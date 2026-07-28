package com.zhuiju.app.core.player

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.zhuiju.app.MainActivity
import com.zhuiju.app.R
import com.zhuiju.app.util.LogUtils
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * 后台播放保活 Service
 *
 * - 前台 Service，保证后台播放不被系统杀死
 * - 通知栏展示当前播放视频标题、播放/暂停按钮
 * - 持有 PowerManager 后台 WakeLock，CPU 不休眠
 * - 绑定 PlayerManager 状态，自动更新通知
 *
 * 必须在 AndroidManifest.xml 中注册：
 * ```xml
 * <service android:name=".core.player.BackgroundPlayService"
 *     android:foregroundServiceType="mediaPlayback"
 *     android:exported="false" />
 * ```
 *
 * 使用方式：
 * ```
 * // 启动后台播放
 * ContextCompat.startForegroundService(context, Intent(context, BackgroundPlayService::class.java))
 * // 停止后台播放
 * context.stopService(Intent(context, BackgroundPlayService::class.java))
 * ```
 */
class BackgroundPlayService : LifecycleService() {

    companion object {
        private const val TAG = "BackgroundPlayService"
        private const val NOTIFICATION_ID = 1002
        private const val CHANNEL_ID = "zhuiju_background_play_channel"
        private const val CHANNEL_NAME = "后台播放"

        const val ACTION_START = "com.zhuiju.app.action.START_BACKGROUND_PLAY"
        const val ACTION_STOP = "com.zhuiju.app.action.STOP_BACKGROUND_PLAY"
        const val ACTION_PAUSE = "com.zhuiju.app.action.PAUSE"
        const val ACTION_RESUME = "com.zhuiju.app.action.RESUME"
        const val EXTRA_TITLE = "extra_title"

        /**
         * 启动后台播放
         */
        fun start(context: Context, title: String = "正在播放") {
            val intent = Intent(context, BackgroundPlayService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_TITLE, title)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        /**
         * 停止后台播放
         */
        fun stop(context: Context) {
            context.stopService(Intent(context, BackgroundPlayService::class.java))
        }
    }

    private var stateCollectorJob: Job? = null
    private var currentTitle: String = "正在播放"

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        LogUtils.i("BackgroundPlayService onCreate", TAG)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        when (intent?.action) {
            ACTION_START -> {
                currentTitle = intent.getStringExtra(EXTRA_TITLE) ?: "正在播放"
                startForeground(NOTIFICATION_ID, buildNotification(currentTitle, true))
                PowerManager.acquireBackground()
                observePlayerState()
            }
            ACTION_STOP -> {
                stopSelf()
            }
            ACTION_PAUSE -> {
                PlayerManager.getInstance().pause()
            }
            ACTION_RESUME -> {
                PlayerManager.getInstance().resume()
            }
        }
        return START_STICKY  // 被杀后自动重启
    }

    /**
     * 监听播放器状态，自动更新通知
     */
    private fun observePlayerState() {
        stateCollectorJob?.cancel()
        stateCollectorJob = lifecycleScope.launch {
            PlayerManager.getInstance().isPlaying.collect { isPlaying ->
                val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
                nm.notify(NOTIFICATION_ID, buildNotification(currentTitle, isPlaying))
                if (!isPlaying && PlayerManager.getInstance().playbackState.value == PlaybackState.Ended) {
                    // 播放结束，停止服务
                    stopSelf()
                }
            }
        }
    }

    /**
     * 构建后台播放通知
     * - 包含播放/暂停、停止按钮
     * - 点击通知跳转 MainActivity
     */
    private fun buildNotification(title: String, isPlaying: Boolean): Notification {
        val contentIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val pauseIntent = PendingIntent.getService(
            this, 1,
            Intent(this, BackgroundPlayService::class.java).apply { action = ACTION_PAUSE },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val resumeIntent = PendingIntent.getService(
            this, 2,
            Intent(this, BackgroundPlayService::class.java).apply { action = ACTION_RESUME },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = PendingIntent.getService(
            this, 3,
            Intent(this, BackgroundPlayService::class.java).apply { action = ACTION_STOP },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("追剧")
            .setContentText(title)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(contentIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .addAction(
                if (isPlaying) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play,
                if (isPlaying) "暂停" else "播放",
                if (isPlaying) pauseIntent else resumeIntent
            )
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "停止", stopIntent)

        return builder.build()
    }

    /**
     * 创建通知渠道（Android 8.0+）
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW  // 低重要性，不发声
            ).apply {
                description = "视频后台播放通知"
                setShowBadge(false)
            }
            val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stateCollectorJob?.cancel()
        PowerManager.releaseBackground()
        LogUtils.i("BackgroundPlayService onDestroy", TAG)
    }
}
