package com.zhuiju.app.core.player

import android.content.Context
import android.os.PowerManager
import com.zhuiju.app.ZhuiJuApp
import com.zhuiju.app.util.LogUtils

/**
 * 功耗与 WakeLock 管理器
 *
 * - 播放时保持屏幕常亮，暂停后关闭常亮，节省电量
 * - 使用 PARTIAL_WAKE_LOCK 保证后台播放不被 CPU 休眠打断
 * - 引用计数，避免重复 acquire/release
 * - 全局单例，禁止业务层直接操作 WakeLock
 *
 * 使用方式：
 * ```
 * PowerManager.acquireScreenOn()      // 进入播放页
 * PowerManager.releaseScreenOn()      // 退出播放页
 * PowerManager.acquireBackground()    // 启动后台播放
 * PowerManager.releaseBackground()    // 停止后台播放
 * ```
 */
object PowerManager {

    private const val TAG = "PowerManager"

    private const val SCREEN_ON_TAG = "zhuiju:screen_on"
    private const val BACKGROUND_TAG = "zhuiju:background_play"

    /** 屏幕常亮 WakeLock（播放期间） */
    private var screenOnWakeLock: PowerManager.WakeLock? = null

    /** 后台播放 WakeLock（保证 CPU 不休眠） */
    private var backgroundWakeLock: PowerManager.WakeLock? = null

    private val powerManager: PowerManager by lazy {
        ZhuiJuApp.instance.getSystemService(Context.POWER_SERVICE) as PowerManager
    }

    /**
     * 获取屏幕常亮 WakeLock
     * - SCREEN_BRIGHT_WAKE_LOCK 已废弃，使用 FLAG_KEEP_SCREEN_ON 替代
     * - 这里仅作为兼容封装，实际屏幕常亮通过 Window Flag 实现
     */
    fun acquireScreenOn() {
        synchronized(this) {
            if (screenOnWakeLock?.isHeld == true) {
                LogUtils.w("screenOnWakeLock 已持有，跳过 acquire", TAG)
                return
            }
            screenOnWakeLock = powerManager.newWakeLock(
                PowerManager.SCREEN_BRIGHT_WAKE_LOCK or PowerManager.ON_AFTER_RELEASE,
                SCREEN_ON_TAG
            ).apply {
                setReferenceCounted(false)
                acquire(10 * 60 * 1000L)  // 10 分钟超时，避免泄漏
            }
            LogUtils.i("屏幕常亮已开启", TAG)
        }
    }

    /**
     * 释放屏幕常亮
     */
    fun releaseScreenOn() {
        synchronized(this) {
            screenOnWakeLock?.let {
                if (it.isHeld) {
                    it.release()
                    LogUtils.i("屏幕常亮已关闭", TAG)
                }
            }
            screenOnWakeLock = null
        }
    }

    /**
     * 获取后台播放 WakeLock（PARTIAL_WAKE_LOCK，仅 CPU）
     * - 保证视频在后台不被系统休眠打断
     * - 仅在播放状态下持有，避免电量浪费
     */
    fun acquireBackground() {
        synchronized(this) {
            if (backgroundWakeLock?.isHeld == true) {
                LogUtils.w("backgroundWakeLock 已持有，跳过 acquire", TAG)
                return
            }
            backgroundWakeLock = powerManager.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                BACKGROUND_TAG
            ).apply {
                setReferenceCounted(false)
                acquire()
            }
            LogUtils.i("后台播放 WakeLock 已开启", TAG)
        }
    }

    /**
     * 释放后台播放 WakeLock
     */
    fun releaseBackground() {
        synchronized(this) {
            backgroundWakeLock?.let {
                if (it.isHeld) {
                    it.release()
                    LogUtils.i("后台播放 WakeLock 已关闭", TAG)
                }
            }
            backgroundWakeLock = null
        }
    }

    /**
     * 全部释放（应用退出时调用）
     */
    fun releaseAll() {
        releaseScreenOn()
        releaseBackground()
    }

    /**
     * 当前是否处于省电模式
     * - 用于播放器自动降级（关闭高帧率、降低分辨率）
     */
    val isPowerSaveMode: Boolean
        get() = try {
            powerManager.isPowerSaveMode
        } catch (e: Exception) {
            false
        }
}
