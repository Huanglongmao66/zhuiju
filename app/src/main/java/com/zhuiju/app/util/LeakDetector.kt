package com.zhuiju.app.util

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import java.lang.ref.WeakReference
import java.util.concurrent.ConcurrentHashMap

/**
 * 内存泄漏检测工具
 *
 * - 监控 Activity/Fragment 销毁后是否仍被持有
 * - 销毁后延迟 5 秒检查弱引用，若仍存活则判定为泄漏
 * - 仅在 Debug 包启用，正式包自动关闭
 * - 检测结果通过 [LeakReport] 回调分发
 *
 * 使用方式：
 * ```
 * class MyActivity : AppCompatActivity() {
 *     override fun onCreate(savedInstanceState: Bundle?) {
 *         super.onCreate(savedInstanceState)
 *         LeakDetector.watch(this, "MyActivity")
 *     }
 * }
 * ```
 */
object LeakDetector {

    private const val TAG = "LeakDetector"

    /** 销毁后检测延迟（毫秒），等待 GC 后再判定 */
    private const val DETECT_DELAY_MS = 5000L

    /** 已监控的对象：key=对象身份 hash，value=弱引用+标签 */
    private val watchedObjects = ConcurrentHashMap<Int, WatchEntry>()

    /** 泄漏回调 */
    var leakCallback: ((LeakReport) -> Unit)? = null

    private data class WatchEntry(
        val reference: WeakReference<Any>,
        val tag: String,
        val watchTime: Long
    )

    /**
     * 监控指定对象（绑定 Lifecycle，自动在销毁后触发检测）
     *
     * @param target       被监控对象（Activity/Fragment）
     * @param tag          对象标签，便于日志识别
     * @param lifecycle    生命周期所有者，默认为 target
     */
    fun watch(target: Any, tag: String, lifecycle: Lifecycle? = null) {
        if (!com.zhuiju.app.BuildConfig.DEBUG) return

        val identityHash = System.identityHashCode(target)
        watchedObjects[identityHash] = WatchEntry(
            reference = WeakReference(target),
            tag = tag,
            watchTime = System.currentTimeMillis()
        )

        // 绑定 Lifecycle，ON_DESTROY 后延迟检测
        (target as? LifecycleOwner)?.let { owner ->
            owner.lifecycle.addObserver(LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_DESTROY) {
                    scheduleCheck(identityHash, tag)
                }
            })
        } ?: lifecycle?.addObserver(LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_DESTROY) {
                scheduleCheck(identityHash, tag)
            }
        })

        LogUtils.d("监控对象: $tag (hash=$identityHash)", TAG)
    }

    /**
     * 延迟检测弱引用是否仍存活
     */
    private fun scheduleCheck(identityHash: Int, tag: String) {
        Thread {
            try {
                Thread.sleep(DETECT_DELAY_MS)
            } catch (e: InterruptedException) {
                return@Thread
            }
            // 主动触发 GC
            System.gc()
            Thread.sleep(100)

            val entry = watchedObjects[identityHash] ?: return@Thread
            val leaked = entry.reference.get()
            if (leaked != null) {
                val report = LeakReport(
                    tag = tag,
                    watchTimeMs = entry.watchTime,
                    detectTimeMs = System.currentTimeMillis(),
                    message = "$tag 销毁后 ${DETECT_DELAY_MS}ms 仍被持有，疑似内存泄漏"
                )
                LogUtils.e(report.message, TAG)
                leakCallback?.invoke(report)
            } else {
                LogUtils.d("$tag 已正常回收", TAG)
            }
            watchedObjects.remove(identityHash)
        }.apply {
            name = "LeakDetector-$tag"
            isDaemon = true
            start()
        }
    }

    /**
     * 强制检查所有监控对象（手动触发）
     */
    fun checkAll() {
        System.gc()
        try {
            Thread.sleep(200)
        } catch (e: InterruptedException) {
            return
        }
        val iterator = watchedObjects.entries.iterator()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            val obj = entry.value.reference.get()
            if (obj == null) {
                iterator.remove()
            } else {
                val report = LeakReport(
                    tag = entry.value.tag,
                    watchTimeMs = entry.value.watchTime,
                    detectTimeMs = System.currentTimeMillis(),
                    message = "${entry.value.tag} 仍被持有（手动检查）"
                )
                LogUtils.w(report.message, TAG)
                leakCallback?.invoke(report)
            }
        }
    }

    /**
     * 清空所有监控记录（应用退出时调用）
     */
    fun clear() {
        watchedObjects.clear()
        LogUtils.i("LeakDetector 已清空监控记录", TAG)
    }
}

/** 内存泄漏报告 */
data class LeakReport(
    val tag: String,
    val watchTimeMs: Long,
    val detectTimeMs: Long,
    val message: String
)
