package com.zhuiju.app.core.player

import android.app.Activity
import android.content.Context
import android.media.AudioManager
import android.provider.Settings
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.WindowManager
import com.zhuiju.app.config.AppConstants
import com.zhuiju.app.util.LogUtils
import kotlin.math.abs

/**
 * 手势控制器
 *
 * - 左侧上下滑动：调节亮度
 * - 右侧上下滑动：调节音量
 * - 左右滑动：拖拽播放进度
 * - 单击：唤起/隐藏控制栏
 * - 长按：触发额外功能（如倍速播放）
 *
 * 手势优先级：上下滑动翻页(短视频) > 单击 > 双击 > 长按 > 侧滑
 *
 * 使用方式：
 * ```
 * val gestureController = GestureController(activity, playerManager)
 * gestureDetector = GestureDetector(context, gestureController)
 * // onTouch 中分发：
 * //   gestureController.onTouchEventStart(event)
 * //   gestureDetector.onTouchEvent(event)
 * //   if (event.action == ACTION_UP) gestureController.onTouchEventEnd()
 * ```
 */
class GestureController(
    private val activity: Activity,
    private val playerManager: PlayerManager,
    private val callback: GestureCallback? = null
) : GestureDetector.SimpleOnGestureListener() {

    private val TAG = "GestureController"

    private val audioManager: AudioManager by lazy {
        activity.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    }

    /** 手势开始时的 X 坐标 */
    private var startX = 0f
    /** 手势开始时的 Y 坐标 */
    private var startY = 0f
    /** 手势类型 */
    private var gestureType = GestureType.NONE

    /** 当前调节的亮度（0~1） */
    private var currentBrightness = 0.5f
    /** 当前音量 */
    private var currentVolume = 0
    /** 最大音量 */
    private val maxVolume by lazy { audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC) }
    /** 拖拽进度时的目标位置 */
    private var seekTargetMs = 0L

    /**
     * 处理手势开始（在 onTouchEvent ACTION_DOWN 时调用）
     */
    fun onTouchEventStart(e: MotionEvent) {
        if (e.action == MotionEvent.ACTION_DOWN) {
            startX = e.x
            startY = e.y
            gestureType = GestureType.NONE
            currentBrightness = activity.window.attributes.screenBrightness.let {
                if (it < 0) getSystemBrightness() else it
            }
            currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        }
    }

    override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
        LogUtils.d("单击：切换控制栏显隐", TAG)
        callback?.onToggleControlBar()
        return true
    }

    override fun onDoubleTap(e: MotionEvent): Boolean {
        LogUtils.d("双击：点赞（短视频）", TAG)
        callback?.onDoubleTap(e.x, e.y)
        return true
    }

    override fun onLongPress(e: MotionEvent) {
        LogUtils.d("长按：触发倍速", TAG)
        callback?.onLongPress()
    }

    override fun onScroll(
        e1: MotionEvent?,
        e2: MotionEvent,
        distanceX: Float,
        distanceY: Float
    ): Boolean {
        if (e1 == null) return false

        val dx = e2.x - e1.x
        val dy = e2.y - e1.y

        // 判断手势类型（仅在未确定时判断）
        if (gestureType == GestureType.NONE) {
            if (abs(dy) > abs(dx) && abs(dy) > AppConstants.GESTURE_MIN_SLIDE_DP) {
                // 垂直滑动
                gestureType = if (e1.x < activity.window.decorView.width / 2) {
                    GestureType.BRIGHTNESS
                } else {
                    GestureType.VOLUME
                }
            } else if (abs(dx) > abs(dy) && abs(dx) > AppConstants.GESTURE_MIN_SLIDE_DP) {
                // 水平滑动
                gestureType = GestureType.SEEK
                seekTargetMs = playerManager.progress.value.current
            }
        }

        when (gestureType) {
            GestureType.BRIGHTNESS -> {
                val delta = -dy / activity.window.decorView.height * 2 * AppConstants.GESTURE_BRIGHTNESS_SENSITIVITY
                currentBrightness = (currentBrightness + delta).coerceIn(0f, 1f)
                setBrightness(currentBrightness)
                callback?.onBrightnessChange(currentBrightness)
            }
            GestureType.VOLUME -> {
                val delta = (-dy / activity.window.decorView.height * maxVolume).toInt()
                val newVolume = (currentVolume + delta).coerceIn(0, maxVolume)
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, newVolume, 0)
                callback?.onVolumeChange(newVolume, maxVolume)
            }
            GestureType.SEEK -> {
                val totalWidth = activity.window.decorView.width.toFloat()
                val percent = dx / totalWidth
                val totalDuration = playerManager.progress.value.total
                if (totalDuration > 0) {
                    seekTargetMs = (seekTargetMs + (percent * totalDuration).toLong()).coerceIn(0L, totalDuration)
                    callback?.onSeekPreview(seekTargetMs, totalDuration)
                }
            }
            else -> {}
        }
        return true
    }

    /**
     * 手势结束（在 onTouchEvent ACTION_UP/CANCEL 时调用）
     */
    fun onTouchEventEnd() {
        when (gestureType) {
            GestureType.SEEK -> {
                if (seekTargetMs > 0) {
                    playerManager.seekTo(seekTargetMs)
                    callback?.onSeekComplete(seekTargetMs)
                }
            }
            else -> {}
        }
        gestureType = GestureType.NONE
    }

    /**
     * 设置屏幕亮度
     */
    private fun setBrightness(brightness: Float) {
        val window = activity.window
        val params = window.attributes
        params.screenBrightness = brightness
        window.attributes = params
    }

    /**
     * 获取系统当前亮度
     */
    private fun getSystemBrightness(): Float {
        return try {
            Settings.System.getInt(activity.contentResolver, Settings.System.SCREEN_BRIGHTNESS) / 255f
        } catch (e: Throwable) {
            0.5f
        }
    }

    /** 手势类型 */
    private enum class GestureType {
        NONE, BRIGHTNESS, VOLUME, SEEK
    }

    /** 手势回调接口 */
    interface GestureCallback {
        /** 切换控制栏显隐 */
        fun onToggleControlBar()
        /** 双击点赞（短视频） */
        fun onDoubleTap(x: Float, y: Float)
        /** 长按（倍速） */
        fun onLongPress()
        /** 亮度变化 */
        fun onBrightnessChange(brightness: Float)
        /** 音量变化 */
        fun onVolumeChange(current: Int, max: Int)
        /** 拖拽进度预览 */
        fun onSeekPreview(positionMs: Long, totalMs: Long)
        /** 拖拽完成 */
        fun onSeekComplete(positionMs: Long)
    }
}
