package com.zhuiju.app.core.player

import android.app.Notification
import androidx.core.app.NotificationCompat
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadManager
import androidx.media3.exoplayer.offline.DownloadService
import androidx.media3.exoplayer.scheduler.PlatformScheduler
import androidx.media3.exoplayer.scheduler.Scheduler
import com.zhuiju.app.R
import com.zhuiju.app.util.LogUtils

/**
 * 视频下载服务
 *
 * - 基于 Media3 DownloadService 实现分片缓存
 * - 前台 Service，适配系统后台权限限制，避免后台被杀
 * - 支持断点续传、下载进度通知
 * - 通知栏显示下载进度，用户可取消
 *
 * 必须在 AndroidManifest.xml 中注册：
 * ```xml
 * <service android:name=".core.player.VideoDownloadService"
 *     android:foregroundServiceType="dataSync"
 *     android:exported="false">
 *     <intent-filter>
 *         <action android:name="androidx.media3.exoplayer.downloadService.action.RESTART" />
 *         <category android:name="android.intent.category.DEFAULT" />
 *     </intent-filter>
 * </service>
 * ```
 */
class VideoDownloadService : DownloadService(
    NOTIFICATION_ID,
    DEFAULT_FOREGROUND_NOTIFICATION_UPDATE_INTERVAL,
    CHANNEL_ID,
    R.string.app_name,
    0
) {
    companion object {
        private const val TAG = "VideoDownloadService"
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "zhuiju_download_channel"
    }

    override fun getDownloadManager(): DownloadManager {
        return DownloadManagerFactory.getDownloadManager(this)
    }

    override fun getScheduler(): Scheduler? {
        return PlatformScheduler(this, 1001)
    }

    override fun getForegroundNotification(
        downloads: List<Download>,
        notMetRequirements: Int
    ): Notification {
        LogUtils.d("构建下载通知: ${downloads.size} 个任务", TAG)

        val totalProgress = if (downloads.isEmpty()) 0 else {
            downloads.sumOf { it.percentDownloaded.toInt() } / downloads.size
        }

        val contentText = when {
            downloads.isEmpty() -> "无下载任务"
            downloads.size == 1 -> "正在下载: ${totalProgress}%"
            else -> "正在下载 ${downloads.size} 个视频: ${totalProgress}%"
        }

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("追剧 - 视频缓存")
            .setContentText(contentText)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setProgress(100, totalProgress, totalProgress == 0)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .build()
    }
}
