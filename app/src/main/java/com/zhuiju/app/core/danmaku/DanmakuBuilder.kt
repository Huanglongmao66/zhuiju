package com.zhuiju.app.core.danmaku

import com.zhuiju.app.config.AppConstants
import master.flame.danmaku.danmaku.model.BaseDanmaku
import master.flame.danmaku.danmaku.model.android.DanmakuContext
import master.flame.danmaku.danmaku.model.android.Danmakus

/**
 * 弹幕对象构建工具
 *
 * 统一构建不同类型弹幕（滚动/顶部/底部），规范弹幕参数
 *
 * 注意：DanmakuFlameMaster 的 `DanmakuContext.create()` 在某些环境下
 * `mDanmakuFactory.createDanmaku()` 会返回 null（factory/pool 未就绪）。
 * 因此本类仅提供工厂方法，**实际调用必须在 DanmakuView prepare 之后**，
 * 或由 DanmakuManager 在 init 流程中按需构建并容忍 null。
 */
object DanmakuBuilder {

    /**
     * 使用指定 context 构建滚动弹幕（推荐用法）
     *
     * @param context 已就绪的 DanmakuContext（DanmakuManager 持有）
     */
    fun buildScrollDanmaku(
        context: DanmakuContext,
        text: String,
        timeMs: Long,
        color: Int = 0xFFFFFFFF.toInt(),
        textSize: Float = AppConstants.DANMAKU_TEXT_SIZE_DEFAULT
    ): BaseDanmaku? {
        val danmaku = context.mDanmakuFactory.createDanmaku(BaseDanmaku.TYPE_SCROLL_RL) ?: return null
        return danmaku.apply {
            this.time = timeMs
            this.text = text
            this.textColor = color
            this.textSize = textSize
            this.borderColor = 0x33000000  // 轻微描边
        }
    }

    /**
     * 构建顶部固定弹幕
     */
    fun buildTopDanmaku(
        context: DanmakuContext,
        text: String,
        timeMs: Long,
        color: Int = 0xFFFFFFFF.toInt(),
        textSize: Float = AppConstants.DANMAKU_TEXT_SIZE_DEFAULT
    ): BaseDanmaku? {
        val danmaku = context.mDanmakuFactory.createDanmaku(BaseDanmaku.TYPE_FIX_TOP) ?: return null
        return danmaku.apply {
            this.time = timeMs
            this.text = text
            this.textColor = color
            this.textSize = textSize
        }
    }

    /**
     * 构建底部固定弹幕
     */
    fun buildBottomDanmaku(
        context: DanmakuContext,
        text: String,
        timeMs: Long,
        color: Int = 0xFFFFFFFF.toInt(),
        textSize: Float = AppConstants.DANMAKU_TEXT_SIZE_DEFAULT
    ): BaseDanmaku? {
        val danmaku = context.mDanmakuFactory.createDanmaku(BaseDanmaku.TYPE_FIX_BOTTOM) ?: return null
        return danmaku.apply {
            this.time = timeMs
            this.text = text
            this.textColor = color
            this.textSize = textSize
        }
    }

    /**
     * 批量构建弹幕集合（从原始数据列表转换）
     *
     * @param context 已就绪的 DanmakuContext
     * @param dataList 弹幕数据列表
     */
    fun buildDanmakus(context: DanmakuContext, dataList: List<DanmakuData>): Danmakus {
        val danmakus = Danmakus()
        var successCount = 0
        dataList.forEach { data ->
            val danmaku = when (data.type) {
                DanmakuData.TYPE_TOP -> buildTopDanmaku(context, data.text, data.timeMs, data.color, data.textSize)
                DanmakuData.TYPE_BOTTOM -> buildBottomDanmaku(context, data.text, data.timeMs, data.color, data.textSize)
                else -> buildScrollDanmaku(context, data.text, data.timeMs, data.color, data.textSize)
            }
            if (danmaku != null) {
                danmakus.addItem(danmaku)
                successCount++
            }
        }
        return danmakus
    }
}

/**
 * 弹幕原始数据模型
 */
data class DanmakuData(
    val text: String,
    val timeMs: Long,
    val color: Int = 0xFFFFFFFF.toInt(),
    val textSize: Float = AppConstants.DANMAKU_TEXT_SIZE_DEFAULT,
    val type: Int = TYPE_SCROLL
) {
    companion object {
        const val TYPE_SCROLL = 0  // 滚动弹幕
        const val TYPE_TOP = 1     // 顶部弹幕
        const val TYPE_BOTTOM = 2  // 底部弹幕
    }
}
