package com.zhuiju.app.core.player

import android.content.Context
import com.zhuiju.app.ZhuiJuApp
import com.zhuiju.app.config.AppConstants
import com.zhuiju.app.util.LogUtils

/**
 * 播放历史与记忆播放位置管理器
 *
 * - 记录视频播放位置，下次打开自动恢复
 * - 播放超过 [AppConstants.PLAYBACK_RECORD_MIN_MS] 才记录，避免误记录
 * - 接近结尾（剩余 < 5s）视为已看完，记录为 0
 * - 基于 SharedPreferences，后续阶段可替换为 Room
 *
 * 使用方式：
 * ```
 * // 播放前恢复
 * val history = PlayHistoryManager.getPosition(videoId)
 * playerManager.play(url, history)
 * // 播放中定时保存
 * PlayHistoryManager.savePosition(videoId, progress.current, progress.total)
 * ```
 */
object PlayHistoryManager {

    private const val TAG = "PlayHistoryManager"
    private const val PREFS_NAME = "play_history_prefs"

    private val prefs by lazy {
        ZhuiJuApp.instance.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    /**
     * 记录播放位置
     *
     * @param videoId  视频 ID
     * @param position 当前播放位置（毫秒）
     * @param duration 总时长（毫秒）
     */
    fun savePosition(videoId: String, position: Long, duration: Long) {
        if (position < AppConstants.PLAYBACK_RECORD_MIN_MS) {
            return  // 播放不足 3 秒不记录
        }
        // 接近结尾（剩余 < 5s）视为已看完，记录为 0
        val recordPosition = if (duration - position < 5000) 0L else position
        prefs.edit().putLong("pos_$videoId", recordPosition).apply()
        LogUtils.d("记录播放位置: videoId=$videoId, position=$recordPosition", TAG)
    }

    /**
     * 获取记忆播放位置
     *
     * @return 上次播放位置（毫秒），无记录返回 0
     */
    fun getPosition(videoId: String): Long {
        return prefs.getLong("pos_$videoId", 0L)
    }

    /**
     * 清除指定视频的播放记录
     */
    fun clearPosition(videoId: String) {
        prefs.edit().remove("pos_$videoId").apply()
    }

    /**
     * 清除所有播放记录
     */
    fun clearAll() {
        prefs.edit().clear().apply()
    }
}
