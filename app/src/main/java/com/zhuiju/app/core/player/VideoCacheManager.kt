package com.zhuiju.app.core.player

import android.content.Context
import android.net.Uri
import androidx.media3.common.util.Util
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadManager
import androidx.media3.exoplayer.offline.DownloadRequest
import androidx.media3.exoplayer.offline.DownloadService
import com.zhuiju.app.ZhuiJuApp
import com.zhuiju.app.config.AppConstants
import com.zhuiju.app.util.LogUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

/**
 * 视频缓存管理器
 *
 * - 基于 ExoPlayer DownloadManager 实现分片缓存
 * - 支持断点续传、缓存进度展示、缓存清理
 * - 缓存目录独立隔离，避免被系统清理
 * - 达到 [AppConstants.CACHE_CLEAN_THRESHOLD] 自动清理最早缓存
 *
 * 使用方式：
 * ```
 * VideoCacheManager.addDownload(videoId, url)
 * VideoCacheManager.downloadProgress.collect { /* 渲染进度 */ }
 * VideoCacheManager.checkAndCleanCache()
 * ```
 *
 * 注意：DownloadService 实际启动逻辑待阶段三实现，需在 Manifest 注册。
 */
object VideoCacheManager {

    private const val TAG = "VideoCacheManager"

    /** 缓存根目录 */
    val cacheDir: File by lazy {
        File(ZhuiJuApp.instance.filesDir, AppConstants.CACHE_DIR_NAME).apply {
            if (!exists()) mkdirs()
        }
    }

    private val _downloadProgress = MutableStateFlow<Map<String, Float>>(emptyMap())
    /** 下载进度（videoId -> 进度 0~1） */
    val downloadProgress: StateFlow<Map<String, Float>> = _downloadProgress.asStateFlow()

    /**
     * 添加缓存任务
     *
     * @param videoId 视频 ID
     * @param url     视频 URL
     */
    fun addDownload(videoId: String, url: String) {
        LogUtils.i("添加缓存任务: videoId=$videoId, url=$url", TAG)
        val request = DownloadRequest.Builder(videoId, Uri.parse(url)).build()
        DownloadService.sendAddDownload(ZhuiJuApp.instance, VideoDownloadService::class.java, request, false)
    }

    /**
     * 移除缓存
     */
    fun removeDownload(videoId: String) {
        LogUtils.i("移除缓存: videoId=$videoId", TAG)
        DownloadService.sendRemoveDownload(ZhuiJuApp.instance, VideoDownloadService::class.java, videoId, false)
    }

    /**
     * 暂停缓存
     */
    fun pauseDownload(videoId: String) {
        LogUtils.i("暂停缓存: videoId=$videoId", TAG)
        DownloadService.sendSetStopReason(ZhuiJuApp.instance, VideoDownloadService::class.java, videoId, "paused", false)
    }

    /**
     * 继续缓存（断点续传）
     */
    fun resumeDownload(videoId: String) {
        LogUtils.i("继续缓存（断点续传）: videoId=$videoId", TAG)
        DownloadService.sendSetStopReason(ZhuiJuApp.instance, VideoDownloadService::class.java, videoId, 0, false)
    }

    /**
     * 清理所有缓存
     */
    fun clearAllCache() {
        LogUtils.i("清理所有缓存", TAG)
        cacheDir.listFiles()?.forEach { it.delete() }
    }

    /**
     * 检查缓存空间是否达阈值，自动清理最早缓存
     */
    fun checkAndCleanCache() {
        val totalSize = cacheDir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
        if (totalSize >= AppConstants.CACHE_CLEAN_THRESHOLD) {
            LogUtils.w("缓存达阈值(${totalSize / 1024 / 1024}MB)，开始清理", TAG)
            // 按最后修改时间排序，删除最早的
            cacheDir.listFiles()
                ?.sortedBy { it.lastModified() }
                ?.forEach { file ->
                    if (cacheDir.walkTopDown().filter { it.isFile }.sumOf { it.length() } < AppConstants.CACHE_CLEAN_THRESHOLD) {
                        return
                    }
                    file.delete()
                }
        }
    }

    /**
     * 获取缓存总大小（字节）
     */
    fun getCacheSize(): Long {
        return cacheDir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
    }

    /**
     * 获取缓存大小（MB，格式化字符串）
     */
    fun getCacheSizeMB(): String {
        val sizeMB = getCacheSize() / 1024.0 / 1024.0
        return String.format("%.1f MB", sizeMB)
    }
}
