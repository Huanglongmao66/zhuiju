package com.zhuiju.app.core.player

import android.content.Context
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.NoOpCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadManager
import com.zhuiju.app.config.AppConstants
import com.zhuiju.app.util.LogUtils
import java.io.File

/**
 * DownloadManager 工厂
 *
 * - 单例创建 [DownloadManager]
 * - 配置独立缓存目录、最大缓存空间
 * - 复用 OkHttp 网络栈
 */
object DownloadManagerFactory {

    private const val TAG = "DownloadManagerFactory"

    @Volatile
    private var downloadManager: DownloadManager? = null

    /** 缓存实例 */
    @Volatile
    private var cache: SimpleCache? = null

    /**
     * 获取 DownloadManager 单例
     */
    fun getDownloadManager(context: Context): DownloadManager {
        downloadManager?.let { return it }

        synchronized(this) {
            downloadManager?.let { return it }

            val dbProvider = StandaloneDatabaseProvider(context)
            val cacheDir = File(context.filesDir, AppConstants.CACHE_DIR_NAME).apply { mkdirs() }

            // 创建 SimpleCache，达阈值自动清理
            val cacheEvictor = NoOpCacheEvictor()  // 不自动驱逐，由 VideoCacheManager.checkAndCleanCache 管理
            val simpleCache = SimpleCache(cacheDir, cacheEvictor, dbProvider)
            cache = simpleCache

            // 配置 HttpDataSource 复用 OkHttp
            val httpFactory = DefaultHttpDataSource.Factory()
                .setUserAgent("ZhuiJuApp")
                .setConnectTimeoutMs(AppConstants.NET_TIMEOUT_SLICE_S * 1000)
                .setReadTimeoutMs(AppConstants.NET_TIMEOUT_SLICE_S * 1000)

            val dataSourceFactory = DefaultDataSource.Factory(context, httpFactory)
            val cacheDataSourceFactory = CacheDataSource.Factory()
                .setCache(simpleCache)
                .setUpstreamDataSourceFactory(dataSourceFactory)
                .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)

            val manager = DownloadManager(context, dbProvider, cacheDataSourceFactory)
                .apply {
                    maxParallelDownloads = 3
                    setMinRetryCount(1500)
                    addListener(DownloadListenerImpl())
                }

            downloadManager = manager
            LogUtils.i("DownloadManager 创建完成, cacheDir=${cacheDir.absolutePath}", TAG)
            return manager
        }
    }

    /**
     * 获取缓存实例
     */
    fun getCache(context: Context): SimpleCache {
        cache?.let { return it }
        getDownloadManager(context)  // 触发 cache 初始化
        return cache!!
    }

    /**
     * 释放资源
     */
    fun release() {
        downloadManager?.release()
        cache?.release()
        downloadManager = null
        cache = null
        LogUtils.i("DownloadManager 已释放", TAG)
    }

    /**
     * 下载监听器实现
     */
    private class DownloadListenerImpl : DownloadManager.Listener {
        override fun onDownloadChanged(
            downloadManager: DownloadManager,
            download: Download,
            finalException: Exception?
        ) {
            LogUtils.i("下载状态变化: id=${download.request.id}, state=${download.state}, progress=${download.percentDownloaded}%", TAG)
            if (finalException != null) {
                LogUtils.e("下载异常: ${finalException.message}", "DownloadListener", finalException)
            }
        }

        override fun onDownloadRemoved(downloadManager: DownloadManager, download: Download) {
            LogUtils.i("下载已移除: id=${download.request.id}", TAG)
        }
    }
}
