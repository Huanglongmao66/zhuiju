package com.zhuiju.app.core.danmaku

import com.zhuiju.app.util.LogUtils

/**
 * 弹幕数据过滤器
 *
 * 统一预处理弹幕数据：
 * - 过滤异常时间戳（负数、超大）
 * - 过滤重复弹幕
 * - 过滤空文本
 * - 屏蔽规则过滤（用户屏蔽词、用户屏蔽类型）
 */
object DanmakuFilter {

    private const val TAG = "DanmakuFilter"

    /** 最大合理时间戳（24小时，毫秒） */
    private const val MAX_REASONABLE_TIME_MS = 24 * 60 * 60 * 1000L

    /**
     * 过滤弹幕数据
     *
     * @param rawData     原始弹幕列表
     * @param blockWords  用户屏蔽词列表
     * @param blockTypes  用户屏蔽的弹幕类型
     * @return 过滤后的弹幕列表
     */
    fun filter(
        rawData: List<DanmakuData>,
        blockWords: List<String> = emptyList(),
        blockTypes: Set<Int> = emptySet()
    ): List<DanmakuData> {
        val seenTexts = mutableSetOf<String>()
        val result = mutableListOf<DanmakuData>()
        var filteredCount = 0

        for (data in rawData) {
            // 1. 过滤空文本
            if (data.text.isBlank()) {
                filteredCount++
                continue
            }

            // 2. 过滤异常时间戳
            if (data.timeMs < 0 || data.timeMs > MAX_REASONABLE_TIME_MS) {
                filteredCount++
                LogUtils.w("异常时间戳弹幕: ${data.timeMs}ms, text=${data.text}", TAG)
                continue
            }

            // 3. 过滤屏蔽类型
            if (data.type in blockTypes) {
                filteredCount++
                continue
            }

            // 4. 过滤屏蔽词
            if (blockWords.any { data.text.contains(it) }) {
                filteredCount++
                continue
            }

            // 5. 过滤重复弹幕（相同文本+相近时间，1秒内）
            val dedupKey = "${data.text}_${data.timeMs / 1000}"
            if (!seenTexts.add(dedupKey)) {
                filteredCount++
                continue
            }

            result.add(data)
        }

        LogUtils.i("弹幕过滤完成: 原始=${rawData.size}, 过滤=$filteredCount, 保留=${result.size}", TAG)
        return result
    }
}
