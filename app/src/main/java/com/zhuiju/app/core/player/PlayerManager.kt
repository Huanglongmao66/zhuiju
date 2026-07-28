package com.zhuiju.app.core.player

import android.content.Context
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.RenderersFactory
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.upstream.DefaultBandwidthMeter
import com.zhuiju.app.ZhuiJuApp
import com.zhuiju.app.config.AppConstants
import com.zhuiju.app.util.LogUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ExoPlayer 全局单例管理器
 *
 * - 全局唯一 ExoPlayer 实例，页面复用，切换视频只切换 MediaItem
 * - 绑定 Lifecycle，自动暂停/释放，杜绝后台音频残留、内存泄漏
 * - 视频切换固定流程：reset → setMediaItem → prepare → play
 * - 播放状态、缓冲、进度、错误统一通过 StateFlow / SharedFlow 分发
 * - 进度更新基于协程 Flow 定时推送，禁止 Handler 轮询
 * - 异常统一拦截，自动重试 [AppConstants.PLAYER_MAX_RETRY] 次
 *
 * 使用方式：
 * ```
 * val playerManager = PlayerManager.getInstance()
 * playerManager.bindLifecycle(lifecycleOwner)
 * playerView.player = playerManager.getPlayer()
 * playerManager.play(url, historyPosition)
 * lifecycleScope.launch {
 *     playerManager.playbackState.collect { /* 渲染 UI */ }
 * }
 * ```
 */
class PlayerManager private constructor() {

    companion object {
        private const val TAG = "PlayerManager"

        @Volatile
        private var instance: PlayerManager? = null

        fun getInstance(): PlayerManager {
            return instance ?: synchronized(this) {
                instance ?: PlayerManager().also { instance = it }
            }
        }
    }

    /** ExoPlayer 实例 */
    private var player: ExoPlayer? = null

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var progressJob: Job? = null

    // ==================== 状态分发 ====================

    /** 播放器状态 */
    private val _playbackState = MutableStateFlow<PlaybackState>(PlaybackState.Idle)
    val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()

    /** 播放进度（毫秒） */
    private val _progress = MutableStateFlow(ProgressInfo(0, 0, 0))
    val progress: StateFlow<ProgressInfo> = _progress.asStateFlow()

    /** 是否正在播放 */
    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    /** 错误事件（一次性） */
    private val _errorEvent = MutableSharedFlow<PlayerError>()
    val errorEvent: SharedFlow<PlayerError> = _errorEvent.asSharedFlow()

    /** 视频尺寸变化 */
    private val _videoSize = MutableStateFlow<VideoSize?>(null)
    val videoSize: StateFlow<VideoSize?> = _videoSize.asStateFlow()

    /** 当前播放视频 URL */
    private var currentUrl: String? = null

    /** 当前重试次数 */
    private var retryCount = 0

    // ==================== 初始化 ====================

    /**
     * 初始化 ExoPlayer（懒加载）
     *
     * - 使用 DefaultRenderersFactory 开启硬解
     * - 配置音频属性（自动处理音频焦点）
     * - 配置 OkHttpDataSource 复用 NetworkManager 连接池
     */
    private fun ensurePlayer(): ExoPlayer {
        player?.let { return it }

        val context = ZhuiJuApp.instance
        val renderersFactory = DefaultRenderersFactory(context)
            .setEnableDecoderFallback(true)  // 硬解失败自动降级软解
            .setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER)

        val httpFactory = OkHttpDataSource.Factory(com.zhuiju.app.core.network.NetworkManager.client)
        val mediaSourceFactory = DefaultMediaSourceFactory(context).setDataSourceFactory(httpFactory)

        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
            .build()

        val newPlayer = ExoPlayer.Builder(context, renderersFactory)
            .setMediaSourceFactory(mediaSourceFactory)
            .setAudioAttributes(audioAttributes, true)  // true=自动处理音频焦点
            .setHandleAudioBecomingNoisy(true)          // 拔耳机自动暂停
            .setSeekBackIncrementMs(10_000)             // 回退 10s
            .setSeekForwardIncrementMs(10_000)          // 前进 10s
            .build()

        newPlayer.addListener(playerListener)
        player = newPlayer
        LogUtils.i("ExoPlayer 初始化完成", TAG)
        return newPlayer
    }

    /**
     * 获取底层 ExoPlayer 实例（供 UI 层绑定 PlayerView）
     */
    fun getPlayer(): ExoPlayer = ensurePlayer()

    // ==================== 播放控制 ====================

    /**
     * 播放视频
     *
     * 固定流程：reset → setMediaItem → prepare → play
     *
     * @param url      视频 URL
     * @param position 起始播放位置（毫秒），用于记忆播放
     */
    fun play(url: String, position: Long = 0L) {
        LogUtils.i("播放视频: url=$url, position=$position", TAG)
        currentUrl = url
        retryCount = 0

        val exoPlayer = ensurePlayer()
        exoPlayer.stop()  // reset 等价于 stop + clearMediaItems
        exoPlayer.clearMediaItems()

        val mediaItem = MediaItem.fromUri(url)
        exoPlayer.setMediaItem(mediaItem, position)
        exoPlayer.prepare()
        exoPlayer.play()
        startProgressUpdate()
    }

    /**
     * 继续播放
     */
    fun resume() {
        player?.play()
    }

    /**
     * 暂停
     */
    fun pause() {
        player?.pause()
    }

    /**
     * 跳转到指定位置（毫秒）
     */
    fun seekTo(positionMs: Long) {
        player?.seekTo(positionMs)
    }

    /**
     * 设置倍速
     */
    fun setPlaybackSpeed(speed: Float) {
        player?.setPlaybackParameters(player!!.playbackParameters.withSpeed(speed))
    }

    /**
     * 设置静音
     */
    fun setVolume(volume: Float) {
        player?.volume = volume
    }

    /**
     * 设置循环模式
     * @param mode 0=不循环 1=单曲循环 2=列表循环
     */
    fun setRepeatMode(mode: Int) {
        player?.repeatMode = when (mode) {
            1 -> Player.REPEAT_MODE_ONE
            2 -> Player.REPEAT_MODE_ALL
            else -> Player.REPEAT_MODE_OFF
        }
    }

    /**
     * 释放播放器（页面彻底销毁时调用）
     */
    fun release() {
        progressJob?.cancel()
        progressJob = null
        player?.removeListener(playerListener)
        player?.release()
        player = null
        _playbackState.value = PlaybackState.Idle
        _isPlaying.value = false
        LogUtils.i("ExoPlayer 已释放", TAG)
    }

    // ==================== 进度更新 ====================

    /**
     * 启动进度定时推送（基于协程，禁止 Handler 轮询）
     */
    private fun startProgressUpdate() {
        progressJob?.cancel()
        progressJob = scope.launch {
            while (true) {
                player?.let { p ->
                    val current = p.currentPosition
                    val total = p.duration
                    val buffered = p.bufferedPosition
                    _progress.value = ProgressInfo(current, total, buffered)
                }
                delay(AppConstants.PLAYER_PROGRESS_INTERVAL_MS)
            }
        }
    }

    // ==================== 生命周期绑定 ====================

    /**
     * 绑定 Lifecycle
     *
     * - ON_PAUSE: 暂停播放
     * - ON_STOP: 暂停播放（后台不释放，保留实例供快速恢复）
     * - ON_DESTROY: 释放播放器
     */
    fun bindLifecycle(lifecycleOwner: LifecycleOwner) {
        lifecycleOwner.lifecycle.addObserver(LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> {
                    LogUtils.i("ON_PAUSE 暂停播放", TAG)
                    pause()
                }
                Lifecycle.Event.ON_STOP -> {
                    LogUtils.i("ON_STOP 暂停播放", TAG)
                    pause()
                }
                Lifecycle.Event.ON_DESTROY -> {
                    LogUtils.i("ON_DESTROY 释放播放器", TAG)
                    release()
                }
                else -> {}
            }
        })
    }

    // ==================== 播放监听 ====================

    private val playerListener = object : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) {
            _playbackState.value = when (playbackState) {
                Player.STATE_IDLE -> PlaybackState.Idle
                Player.STATE_BUFFERING -> PlaybackState.Buffering
                Player.STATE_READY -> if (player?.playWhenReady == true) PlaybackState.Playing else PlaybackState.Paused
                Player.STATE_ENDED -> PlaybackState.Ended
                else -> PlaybackState.Idle
            }
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _isPlaying.value = isPlaying
        }

        override fun onVideoSizeChanged(videoSize: VideoSize) {
            _videoSize.value = videoSize
        }

        override fun onPlayerErrorChanged(error: PlaybackException?) {
            if (error != null) {
                LogUtils.e("播放错误: ${error.message}", TAG, error)
                handlePlayError(error)
            }
        }
    }

    /**
     * 处理播放错误：自动重试
     */
    private fun handlePlayError(error: PlaybackException) {
        if (retryCount < AppConstants.PLAYER_MAX_RETRY) {
            retryCount++
            LogUtils.w("播放错误，第 $retryCount 次重试", TAG)
            scope.launch {
                delay(AppConstants.NET_RETRY_INTERVAL_MS)
                currentUrl?.let { play(it, _progress.value.current) }
            }
        } else {
            LogUtils.e("播放错误，已达最大重试次数", TAG)
            scope.launch {
                _errorEvent.emit(
                    PlayerError(
                        code = error.errorCodeName,
                        message = error.message ?: "播放失败",
                        throwable = error
                    )
                )
            }
        }
    }
}

// ==================== 状态模型 ====================

/** 播放器状态密封类 */
sealed class PlaybackState {
    data object Idle : PlaybackState()       // 空闲
    data object Buffering : PlaybackState()  // 缓冲中
    data object Playing : PlaybackState()    // 播放中
    data object Paused : PlaybackState()     // 暂停
    data object Ended : PlaybackState()      // 播放结束
}

/** 进度信息 */
data class ProgressInfo(
    val current: Long,    // 当前播放位置（毫秒）
    val total: Long,      // 总时长（毫秒）
    val buffered: Long    // 缓冲位置（毫秒）
)

/** 播放错误 */
data class PlayerError(
    val code: String,     // 错误码
    val message: String,  // 错误信息
    val throwable: Throwable? = null
)
