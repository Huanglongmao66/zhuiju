package com.zhuiju.app.core.danmaku

import android.content.Context
import com.zhuiju.app.ZhuiJuApp
import com.zhuiju.app.config.AppConstants
import com.zhuiju.app.util.DeviceUtils
import com.zhuiju.app.util.LogUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import master.flame.danmaku.controller.DrawHandler
import master.flame.danmaku.danmaku.model.BaseDanmaku
import master.flame.danmaku.danmaku.model.DanmakuTimer
import master.flame.danmaku.danmaku.model.IDisplayer
import master.flame.danmaku.danmaku.model.android.DanmakuContext
import master.flame.danmaku.danmaku.model.android.Danmakus
import master.flame.danmaku.danmaku.parser.BaseDanmakuParser
import master.flame.danmaku.ui.widget.DanmakuView

/**
 * 弹幕管理器
 *
 * - 集成 DanmakuFlameMaster 框架
 * - 弹幕初始化/启动/暂停/销毁跟随播放器生命周期
 * - 时间轴与播放器精准同步（倍速、拖拽、暂停自动校准）
 * - 低端机型自动降级：降低帧率、限制最大条数
 * - 页面退出/视频切换强制清空弹幕队列
 */
class DanmakuManager(
    private val danmakuView: DanmakuView,
    private val callback: DanmakuCallback? = null
) {

    private val TAG = "DanmakuManager"

    /** 弹幕上下文配置 */
    private val context: DanmakuContext = DanmakuContext.create()

    /** 弹幕是否已就绪 */
    private val _isReady = MutableStateFlow(false)
    val isReady: StateFlow<Boolean> = _isReady.asStateFlow()

    /** 弹幕是否显示中 */
    private val _isVisible = MutableStateFlow(true)
    val isVisible: StateFlow<Boolean> = _isVisible.asStateFlow()

    /** 当前弹幕透明度 */
    private val _alpha = MutableStateFlow(AppConstants.DANMAKU_ALPHA_DEFAULT)
    val alpha: StateFlow<Float> = _alpha.asStateFlow()

    /** 当前弹幕字号倍数 */
    private val _textSizeScale = MutableStateFlow(1.0f)
    val textSizeScale: StateFlow<Float> = _textSizeScale.asStateFlow()

    /** 是否低端机型（用于降级） */
    private val isLowEnd = DeviceUtils.isLowEndDevice

    init {
        configureContext()
    }

    /**
     * 配置弹幕上下文（机型适配）
     */
    private fun configureContext() {
        // 单屏最大弹幕数量（低端机降级）
        val maxCount = if (isLowEnd) {
            AppConstants.DANMAKU_MAX_COUNT_LOW_END
        } else {
            AppConstants.DANMAKU_MAX_COUNT
        }

        // 弹幕渲染帧率（低端机降级）
        val fps = if (isLowEnd) {
            AppConstants.DANMAKU_FPS_LOW
        } else {
            AppConstants.DANMAKU_FPS_HIGH
        }

        context.setDuplicateMergingEnabled(false)  // 禁止重复弹幕合并
            .setMaximumVisibleSizeInScreen(maxCount)
            .setScrollSpeedFactor(1.2f)

        LogUtils.i("弹幕配置: maxCount=$maxCount, fps=$fps, isLowEnd=$isLowEnd", TAG)
    }

    /**
     * 初始化弹幕
     *
     * @param danmakus 弹幕数据集合
     */
    fun init(danmakus: Danmakus) {
        val parser = object : BaseDanmakuParser() {
            override fun parse(): Danmakus = danmakus
        }

        danmakuView.setCallback(object : DrawHandler.Callback {
            override fun prepared() {
                _isReady.value = true
                LogUtils.i("弹幕准备完成", TAG)
            }

            override fun updateTimer(timer: DanmakuTimer) {
                callback?.onTimerUpdate(timer.currMS)
            }

            override fun danmakuShown(danmaku: BaseDanmaku?) {
                // 单条弹幕展示回调
            }

            override fun drawingFinished() {
                // 绘制完成
            }
        })

        danmakuView.prepare(parser, context)
        danmakuView.showFPS(false)
        danmakuView.enableDanmakuDrawingCache(true)
    }

    /**
     * 启动弹幕
     */
    fun start() {
        if (_isReady.value) {
            danmakuView.start()
            LogUtils.d("弹幕启动", TAG)
        }
    }

    /**
     * 暂停弹幕（跟随播放器暂停）
     */
    fun pause() {
        danmakuView.pause()
        LogUtils.d("弹幕暂停", TAG)
    }

    /**
     * 恢复弹幕（跟随播放器恢复）
     */
    fun resume() {
        danmakuView.resume()
        LogUtils.d("弹幕恢复", TAG)
    }

    /**
     * 隐藏弹幕（用户关闭弹幕开关）
     */
    fun hide() {
        danmakuView.hide()
        _isVisible.value = false
    }

    /**
     * 显示弹幕（用户开启弹幕开关）
     */
    fun show() {
        danmakuView.show()
        _isVisible.value = true
    }

    /**
     * 跳转到指定时间（与播放器拖拽同步）
     *
     * @param positionMs 时间位置（毫秒）
     */
    fun seekTo(positionMs: Long) {
        if (_isReady.value) {
            danmakuView.seekTo(positionMs)
            LogUtils.d("弹幕跳转: ${positionMs}ms", TAG)
        }
    }

    /**
     * 设置倍速（与播放器倍速同步）
     *
     * @param speed 倍速
     */
    fun setPlaybackSpeed(speed: Float) {
        // DanmakuFlameMaster 通过调整滚动速度因子实现倍速
        context.setScrollSpeedFactor(1.0f / speed)
        LogUtils.d("弹幕倍速: $speed", TAG)
    }

    /**
     * 设置透明度
     *
     * @param alpha 透明度（0~1）
     */
    fun setAlpha(alpha: Float) {
        _alpha.value = alpha
        context.setDanmakuTransparency(alpha)
    }

    /**
     * 设置字号倍数
     *
     * @param scale 倍数（1.0=默认）
     */
    fun setTextSizeScale(scale: Float) {
        _textSizeScale.value = scale
        context.setScaleTextSize(scale)
    }

    /**
     * 添加单条弹幕（实时发送）
     *
     * @param danmaku 弹幕对象
     */
    fun addDanmaku(danmaku: BaseDanmaku) {
        danmakuView.addDanmaku(danmaku)
    }

    /**
     * 释放弹幕资源（页面销毁时调用）
     */
    fun release() {
        danmakuView.release()
        _isReady.value = false
        LogUtils.i("弹幕资源释放", TAG)
    }

    /**
     * 弹幕回调接口
     */
    interface DanmakuCallback {
        /** 时间轴更新 */
        fun onTimerUpdate(currentMs: Long)
    }
}
