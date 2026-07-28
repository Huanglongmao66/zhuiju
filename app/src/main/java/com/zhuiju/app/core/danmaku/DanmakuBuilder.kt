package com.zhuiju.app.core.danmaku

import com.zhuiju.app.config.AppConstants
import master.flame.danmaku.danmaku.model.BaseDanmaku
import master.flame.danmaku.danmaku.model.android.DanmakuContext
import master.flame.danmaku.danmaku.model.android.Danmakus

/**
 * 弹幕对象构建工具
 *
 * 统一构建不同类型弹幕（滚动/顶部/底部），规范弹幕参数
 */
object DanmakuBuilder {

    private val context: DanmakuContext = DanmakuContext.create()

    /**
     * 构建滚动弹幕（最常见）
     *
     * @param text     弹幕文本
     * @param timeMs   出现时间（毫秒）
     * @param color    文字颜色（ARGB）
     * @param textSize 字号（sp）
     */
    fun buildScrollDanmaku(
        text: String,
        timeMs: Long,
        color: Int = 0xFFFFFFFF.toInt(),
        textSize: Float = AppConstants.DANMAKU_TEXT_SIZE_DEFAULT
    ): BaseDanmaku {
        return context.mDanmakuFactory.createDanmaku(BaseDanmaku.TYPE_SCROLL_RL).apply {
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
        text: String,
        timeMs: Long,
        color: Int = 0xFFFFFFFF.toInt(),
        textSize: Float = AppConstants.DANMAKU_TEXT_SIZE_DEFAULT
    ): BaseDanmaku {
        return context.mDanmakuFactory.createDanmaku(BaseDanmaku.TYPE_FIX_TOP).apply {
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
        text: String,
        timeMs: Long,
        color: Int = 0xFFFFFFFF.toInt(),
        textSize: Float = AppConstants.DANMAKU_TEXT_SIZE_DEFAULT
    ): BaseDanmaku {
        return context.mDanmakuFactory.createDanmaku(BaseDanmaku.TYPE_FIX_BOTTOM).apply {
            this.time = timeMs
            this.text = text
            this.textColor = color
            this.textSize = textSize
        }
    }

    /**
     * 批量构建弹幕集合（从原始数据列表转换）
     *
     * @param dataList 弹幕数据列表
     */
    fun buildDanmakus(dataList: List<DanmakuData>): Danmakus {
        val danmakus = Danmakus()
        dataList.forEach { data ->
            val danmaku = when (data.type) {
                DanmakuData.TYPE_TOP -> buildTopDanmaku(data.text, data.timeMs, data.color, data.textSize)
                DanmakuData.TYPE_BOTTOM -> buildBottomDanmaku(data.text, data.timeMs, data.color, data.textSize)
                else -> buildScrollDanmaku(data.text, data.timeMs, data.color, data.textSize)
            }
            danmakus.addItem(danmaku)
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
