package com.zhuiju.app.core.danmaku

import com.zhuiju.app.config.AppConstants
import com.zhuiju.app.util.LogUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * 弹幕同步控制器
 *
 * - 与播放器进度精准同步：拖拽、倍速、暂停自动校准
 * - 统一封装同步逻辑，禁止零散同步代码
 * - 预加载偏移：提前 [AppConstants.DANMAKU_PRELOAD_OFFSET_MS] 加载后续弹幕
 */
class DanmakuSyncController(
    private val danmakuManager: DanmakuManager
) {

    private const val TAG = "DanmakuSyncController"

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var syncJob: Job? = null

    /** 上次同步的播放位置 */
    private var lastSyncPosition = 0L

    /** 当前播放器位置 Flow（由外部注入） */
    private var positionProvider: (() -> Long)? = null
    private var speedProvider: (() -> Float)? = null
    private var isPlayingProvider: (() -> Boolean)? = null

    /**
     * 绑定播放器状态提供者
     */
    fun bindPlayer(
        positionProvider: () -> Long,
        speedProvider: () -> Float,
        isPlayingProvider: () -> Boolean
    ) {
        this.positionProvider = positionProvider
        this.speedProvider = speedProvider
        this.isPlayingProvider = isPlayingProvider
    }

    /**
     * 启动同步
     */
    fun start() {
        syncJob?.cancel()
        syncJob = scope.launch {
            while (true) {
                val position = positionProvider?.invoke() ?: 0L
                val isPlaying = isPlayingProvider?.invoke() ?: false
                val speed = speedProvider?.invoke() ?: 1.0f

                // 检测是否发生 Seek（位置跳变 > 1s）
                if (kotlin.math.abs(position - lastSyncPosition) > 1000) {
                    LogUtils.d("检测到 Seek，同步弹幕: ${position}ms", TAG)
                    danmakuManager.seekTo(position)
                }
                lastSyncPosition = position

                // 播放/暂停状态联动
                if (isPlaying) {
                    if (!danmakuManager.isReady.value) {
                        danmakuManager.start()
                    }
                } else {
                    danmakuManager.pause()
                }

                // 倍速同步
                danmakuManager.setPlaybackSpeed(speed)

                delay(AppConstants.PLAYER_PROGRESS_INTERVAL_MS)
            }
        }
    }

    /**
     * 停止同步
     */
    fun stop() {
        syncJob?.cancel()
        syncJob = null
        LogUtils.i("弹幕同步停止", TAG)
    }

    /**
     * 释放资源
     */
    fun release() {
        stop()
        danmakuManager.release()
    }
}
