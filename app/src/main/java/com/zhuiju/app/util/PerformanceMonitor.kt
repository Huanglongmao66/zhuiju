package com.zhuiju.app.util

import android.os.Debug
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.Choreographer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * 性能监控统一管理器
 *
 * 集成三大核心能力：
 * 1. 帧率监控 —— 基于 Choreographer，检测掉帧、卡顿
 * 2. 内存监控 —— 基于 Debug.MemoryInfo，检测内存压力、内存泄漏
 * 3. ANR 检测 —— 主线程心跳监测，超过阈值触发告警
 *
 * - 仅在 Debug 包开启，正式包自动关闭，避免性能损耗
 * - 所有告警通过 [PerformanceAlert] 回调分发
 * - 监控数据通过 [metrics] StateFlow 暴露，便于开发者面板消费
 *
 * 使用方式：
 * ```
 * PerformanceMonitor.start()  // Application.onCreate 中
 * PerformanceMonitor.stop()   // 应用退出时
 * ```
 */
object PerformanceMonitor {

    private const val TAG = "PerformanceMonitor"

    /** ANR 心跳超时阈值（毫秒），主线程超过该时间无响应判定为 ANR */
    private const val ANR_TIMEOUT_MS = 5000L

    /** ANR 检测心跳间隔（毫秒） */
    private const val ANR_CHECK_INTERVAL_MS = 2000L

    /** 帧率告警阈值（FPS），低于该值判定为卡顿 */
    private const val FPS_WARN_THRESHOLD = 40

    /** 内存告警阈值（已用内存占总内存比例 0~1） */
    private const val MEM_WARN_RATIO = 0.8f

    /** 监控采样间隔（毫秒） */
    private const val SAMPLE_INTERVAL_MS = 5000L

    /** 历史数据最大保留条数 */
    private const val MAX_HISTORY_SIZE = 60

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var monitorJob: Job? = null
    private var anrThread: Thread? = null
    @Volatile private var isRunning = false

    /** 性能告警回调 */
    var alertCallback: ((PerformanceAlert) -> Unit)? = null

    /** 历史帧率记录（最近 N 条） */
    private val fpsHistory = ConcurrentLinkedQueue<Float>()

    /** 历史内存记录（MB，最近 N 条） */
    private val memHistory = ConcurrentLinkedQueue<Long>()

    // ==================== 启停 ====================

    /**
     * 启动性能监控（仅 Debug 包生效）
     */
    fun start() {
        if (isRunning) return
        if (!com.zhuiju.app.BuildConfig.DEBUG) {
            LogUtils.i("非 Debug 包，性能监控不启动", TAG)
            return
        }
        isRunning = true
        startFrameMonitor()
        startMemoryMonitor()
        startAnrWatchDog()
        LogUtils.i("性能监控已启动", TAG)
    }

    /**
     * 停止性能监控
     */
    fun stop() {
        if (!isRunning) return
        isRunning = false
        monitorJob?.cancel()
        monitorJob = null
        anrThread?.interrupt()
        anrThread = null
        stopFrameMonitor()
        LogUtils.i("性能监控已停止", TAG)
    }

    // ==================== 帧率监控 ====================

    private val frameCallback = object : Choreographer.FrameCallback {
        private var lastFrameTimeNanos = 0L
        private var frameCount = 0

        override fun doFrame(frameTimeNanos: Long) {
            if (lastFrameTimeNanos == 0L) {
                lastFrameTimeNanos = frameTimeNanos
            } else {
                frameCount++
                val elapsedMs = (frameTimeNanos - lastFrameTimeNanos) / 1_000_000
                // 每秒统计一次 FPS
                if (elapsedMs >= 1000) {
                    val fps = frameCount * 1000f / elapsedMs
                    recordFps(fps)
                    if (fps < FPS_WARN_THRESHOLD) {
                        emitAlert(PerformanceAlert.FrameDrop(fps))
                    }
                    lastFrameTimeNanos = frameTimeNanos
                    frameCount = 0
                }
            }
            if (isRunning) {
                Choreographer.getInstance().postFrameCallback(this)
            }
        }
    }

    private fun startFrameMonitor() {
        Choreographer.getInstance().postFrameCallback(frameCallback)
    }

    private fun stopFrameMonitor() {
        Choreographer.getInstance().removeFrameCallback(frameCallback)
    }

    private fun recordFps(fps: Float) {
        fpsHistory.offer(fps)
        while (fpsHistory.size > MAX_HISTORY_SIZE) {
            fpsHistory.poll()
        }
    }

    // ==================== 内存监控 ====================

    private fun startMemoryMonitor() {
        monitorJob = scope.launch {
            while (isRunning) {
                val usedMemMB = getUsedMemoryMB()
                val totalMemMB = DeviceUtils.totalMemMB
                memHistory.offer(usedMemMB)
                while (memHistory.size > MAX_HISTORY_SIZE) {
                    memHistory.poll()
                }
                if (totalMemMB > 0 && usedMemMB.toFloat() / totalMemMB >= MEM_WARN_RATIO) {
                    emitAlert(PerformanceAlert.MemoryPressure(usedMemMB, totalMemMB))
                }
                delay(SAMPLE_INTERVAL_MS)
            }
        }
    }

    /**
     * 获取当前应用已用内存（MB）
     * - 包含 Native 内存，更准确反映视频 APP 内存压力
     */
    private fun getUsedMemoryMB(): Long {
        val memoryInfo = Debug.MemoryInfo()
        Debug.getMemoryInfo(memoryInfo)
        return memoryInfo.totalPss / 1024L
    }

    // ==================== ANR 检测 ====================

    /**
     * ANR 看门狗：子线程定期检测主线程心跳
     * - 主线程在 ANR_CHECK_INTERVAL_MS 内未更新心跳时间戳，触发 ANR 告警
     */
    private fun startAnrWatchDog() {
        val mainHandler = Handler(Looper.getMainLooper())
        var lastHeartbeat = 0L
        anrThread = Thread {
            while (isRunning && !Thread.currentThread().isInterrupted) {
                try {
                    Thread.sleep(ANR_CHECK_INTERVAL_MS)
                } catch (e: InterruptedException) {
                    break
                }
                val currentHeartbeat = SystemClock.uptimeMillis()
                mainHandler.post { lastHeartbeat = SystemClock.uptimeMillis() }
                // 等待主线程响应
                Thread.sleep(ANR_CHECK_INTERVAL_MS)
                if (lastHeartbeat == 0L || currentHeartbeat - lastHeartbeat > ANR_TIMEOUT_MS) {
                    val stackTrace = Looper.getMainLooper().thread.stackTrace
                    emitAlert(PerformanceAlert.AnrDetected(ANR_TIMEOUT_MS, stackTrace))
                }
            }
        }.apply {
            name = "AnrWatchDog"
            isDaemon = true
            start()
        }
    }

    // ==================== 告警分发 ====================

    private fun emitAlert(alert: PerformanceAlert) {
        when (alert) {
            is PerformanceAlert.FrameDrop -> LogUtils.w("卡顿告警 FPS=${alert.fps}", TAG)
            is PerformanceAlert.MemoryPressure -> LogUtils.w("内存告警 used=${alert.usedMB}MB total=${alert.totalMB}MB", TAG)
            is PerformanceAlert.AnrDetected -> LogUtils.e("ANR 告警 timeout=${alert.timeoutMs}ms", TAG)
        }
        alertCallback?.invoke(alert)
    }

    // ==================== 指标查询 ====================

    /**
     * 获取当前性能指标快照
     */
    fun getMetricsSnapshot(): PerformanceMetrics {
        val fpsList = fpsHistory.toList()
        val memList = memHistory.toList()
        return PerformanceMetrics(
            currentFps = fpsList.lastOrNull() ?: 0f,
            avgFps = if (fpsList.isEmpty()) 0f else fpsList.average().toFloat(),
            minFps = fpsList.minOrNull() ?: 0f,
            currentMemMB = memList.lastOrNull() ?: 0L,
            avgMemMB = if (memList.isEmpty()) 0L else memList.average().toLong(),
            maxMemMB = memList.maxOrNull() ?: 0L,
            cpuCores = DeviceUtils.cpuCores,
            isLowEnd = DeviceUtils.isLowEndDevice
        )
    }

    /**
     * 主动触发 GC 并打印内存信息（调试用）
     */
    fun dumpMemory() {
        System.gc()
        val usedMB = getUsedMemoryMB()
        val totalMB = DeviceUtils.totalMemMB
        LogUtils.i("内存快照: used=${usedMB}MB, total=${totalMB}MB, native=${Debug.getNativeHeapAllocatedSize() / 1024 / 1024}MB", TAG)
    }
}

// ==================== 性能告警模型 ====================

/** 性能告警密封类 */
sealed class PerformanceAlert {
    /** 掉帧告警 */
    data class FrameDrop(val fps: Float) : PerformanceAlert()
    /** 内存压力告警 */
    data class MemoryPressure(val usedMB: Long, val totalMB: Long) : PerformanceAlert()
    /** ANR 告警 */
    data class AnrDetected(val timeoutMs: Long, val stackTrace: Array<StackTraceElement>) : PerformanceAlert()
}

/** 性能指标快照 */
data class PerformanceMetrics(
    val currentFps: Float,
    val avgFps: Float,
    val minFps: Float,
    val currentMemMB: Long,
    val avgMemMB: Long,
    val maxMemMB: Long,
    val cpuCores: Int,
    val isLowEnd: Boolean
)
