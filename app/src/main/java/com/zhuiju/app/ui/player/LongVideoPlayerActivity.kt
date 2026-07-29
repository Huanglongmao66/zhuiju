package com.zhuiju.app.ui.player

import android.content.pm.ActivityInfo
import android.os.Bundle
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.widget.PopupMenu
import android.widget.SeekBar
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.zhuiju.app.config.AppConstants
import com.zhuiju.app.core.danmaku.DanmakuManager
import com.zhuiju.app.core.danmaku.DanmakuData
import com.zhuiju.app.core.player.GestureController
import com.zhuiju.app.core.player.PlaybackState
import com.zhuiju.app.core.player.PlayerManager
import com.zhuiju.app.core.player.PowerManager
import com.zhuiju.app.core.player.ProgressInfo
import com.zhuiju.app.data.MockData
import com.zhuiju.app.databinding.ActivityLongVideoPlayerBinding
import com.zhuiju.app.util.LogUtils
import com.zhuiju.app.util.ToastUtils
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * 长视频播放页 Activity
 *
 * - 集成 PlayerManager（ExoPlayer 单例）+ DanmakuManager（弹幕）+ GestureController（手势）
 * - 控制栏自动隐藏（3 秒无操作），动画 250ms
 * - 横竖屏适配，全屏切换
 * - 状态遮罩：加载中、错误、重试
 *
 * Intent 参数：
 * - [EXTRA_VIDEO_ID]    视频ID（用于播放历史）
 * - [EXTRA_VIDEO_URL]   视频URL（为空时使用 MockData.longVideoDetail）
 * - [EXTRA_VIDEO_TITLE] 视频标题
 */
class LongVideoPlayerActivity : AppCompatActivity(), GestureController.GestureCallback {

    private lateinit var binding: ActivityLongVideoPlayerBinding
    private lateinit var playerManager: PlayerManager
    private lateinit var danmakuManager: DanmakuManager
    private lateinit var gestureController: GestureController
    private lateinit var gestureDetector: GestureDetector

    private var controlBarHideJob: kotlinx.coroutines.Job? = null
    private var gestureOverlayHideJob: kotlinx.coroutines.Job? = null
    private var isControlBarVisible = true

    /** 当前倍速（点击倍速按钮切换） */
    private var currentSpeed = AppConstants.PLAYBACK_SPEED_DEFAULT

    /** 是否处于长按倍速状态（松手恢复） */
    private var isLongPressSpeeding = false

    /** 当前视频ID */
    private val videoId: String by lazy {
        intent.getStringExtra(EXTRA_VIDEO_ID) ?: "test_001"
    }

    /** 当前视频URL（默认使用 MockData 中的真实可播放视频） */
    private val videoUrl: String by lazy {
        intent.getStringExtra(EXTRA_VIDEO_URL) ?: MockData.longVideoDetail.videoUrl
    }

    /** 当前视频标题 */
    private val videoTitle: String by lazy {
        intent.getStringExtra(EXTRA_VIDEO_TITLE) ?: MockData.longVideoDetail.title
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLongVideoPlayerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.tvTitle.text = videoTitle

        initPlayer()
        initDanmaku()
        initGesture()
        initControlBar()
        collectPlayerState()

        // 播放期间屏幕常亮
        PowerManager.acquireScreenOn()

        // 开始播放（使用续播位置）
        val savedPosition = com.zhuiju.app.core.player.PlayHistoryManager.getPosition(videoId)
        playerManager.play(videoUrl, savedPosition)
        LogUtils.i("开始播放: id=$videoId, url=$videoUrl, savedPos=$savedPosition", "LongVideoPlayer")
    }

    private fun initPlayer() {
        playerManager = PlayerManager.getInstance()
        playerManager.bindLifecycle(this)
        // 将 ExoPlayer 绑定到 TextureView（ExoPlayer 支持 setVideoTextureView）
        val exoPlayer = playerManager.getPlayer()
        // 小米/红米兼容：TextureView 的 SurfaceTexture 若未就绪，需在监听器中二次绑定
        val textureView = binding.textureView
        if (textureView.isAvailable) {
            exoPlayer.setVideoTextureView(textureView)
        } else {
            textureView.surfaceTextureListener = object : android.view.TextureView.SurfaceTextureListener {
                override fun onSurfaceTextureAvailable(surface: android.graphics.SurfaceTexture, width: Int, height: Int) {
                    exoPlayer.setVideoTextureView(textureView)
                    textureView.surfaceTextureListener = null
                }
                override fun onSurfaceTextureSizeChanged(surface: android.graphics.SurfaceTexture, width: Int, height: Int) {}
                override fun onSurfaceTextureDestroyed(surface: android.graphics.SurfaceTexture): Boolean = true
                override fun onSurfaceTextureUpdated(surface: android.graphics.SurfaceTexture) {}
            }
        }
    }

    private fun initDanmaku() {
        danmakuManager = DanmakuManager(binding.danmakuView)
        // 加载 Mock 弹幕数据（init 内部会在 factory 就绪后构建并注入）
        val danmakuDataList = MockData.longVideoDanmakus.map { info ->
            DanmakuData(
                text = info.text,
                timeMs = info.timeMs,
                color = info.color,
                type = info.type
            )
        }
        danmakuManager.init(danmakuDataList)
        danmakuManager.start()
        LogUtils.i("弹幕初始化: ${danmakuDataList.size} 条数据", "LongVideoPlayer")
    }

    private fun initGesture() {
        gestureController = GestureController(this, playerManager, this)
        gestureDetector = GestureDetector(this, gestureController)
        // 绑定到根 View：返回 false 不拦截事件，按钮仍可正常点击；
        // 非按钮区域（TextureView/DanmakuView 不消费 touch）的手势由 GestureDetector 识别
        binding.root.setOnTouchListener { _, event ->
            gestureController.onTouchEventStart(event)
            gestureDetector.onTouchEvent(event)
            if (event.action == MotionEvent.ACTION_UP || event.action == MotionEvent.ACTION_CANCEL) {
                gestureController.onTouchEventEnd()
                // 长按倍速松手后恢复原倍速
                if (isLongPressSpeeding) {
                    isLongPressSpeeding = false
                    playerManager.setPlaybackSpeed(currentSpeed)
                    hideGestureOverlay()
                }
            }
            false
        }
    }

    private fun initControlBar() {
        binding.btnPlayPause.setOnClickListener {
            if (playerManager.isPlaying.value) playerManager.pause() else playerManager.resume()
        }
        binding.btnBack.setOnClickListener { finish() }
        binding.btnRetry.setOnClickListener {
            playerManager.play(videoUrl)
        }
        binding.btnDanmaku.setOnClickListener {
            if (danmakuManager.isVisible.value) danmakuManager.hide() else danmakuManager.show()
        }

        // SeekBar 拖动跳转
        binding.seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    binding.tvTime.text = "${formatTime(progress.toLong())}/${formatTime(seekBar?.max?.toLong() ?: 0)}"
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                controlBarHideJob?.cancel()
            }
            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                val pos = seekBar?.progress?.toLong() ?: return
                playerManager.seekTo(pos)
                danmakuManager.seekTo(pos)
                scheduleHideControlBar()
            }
        })

        // 倍速按钮：弹出选择菜单
        binding.btnSpeed.setOnClickListener {
            showSpeedMenu()
        }

        // 全屏按钮：切换横竖屏
        binding.btnFullscreen.setOnClickListener {
            toggleOrientation()
        }
        // 点击切换控制栏显隐在 GestureCallback 中处理
    }

    /**
     * 倍速选择菜单
     */
    private fun showSpeedMenu() {
        val popup = PopupMenu(this, binding.btnSpeed)
        AppConstants.PLAYBACK_SPEEDS.forEachIndexed { index, speed ->
            val label = if (speed == 1.0f) "1.0x（正常）" else "${speed}x"
            popup.menu.add(0, index, 0, label)
        }
        popup.setOnMenuItemClickListener { item ->
            val speed = AppConstants.PLAYBACK_SPEEDS.getOrNull(item.itemId) ?: return@setOnMenuItemClickListener false
            currentSpeed = speed
            playerManager.setPlaybackSpeed(speed)
            ToastUtils.show("倍速 ${speed}x")
            true
        }
        popup.show()
    }

    /**
     * 切换横竖屏
     */
    private fun toggleOrientation() {
        val newOrientation = if (resources.configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE) {
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        } else {
            ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        }
        requestedOrientation = newOrientation
    }

    private fun collectPlayerState() {
        lifecycleScope.launch {
            playerManager.playbackState.collect { state ->
                when (state) {
                    PlaybackState.Buffering -> showLoading()
                    PlaybackState.Playing -> {
                        hideStatusOverlay()
                        updatePlayPauseButton(true)
                        scheduleHideControlBar()
                        // 播放时启动弹幕
                        danmakuManager.resume()
                    }
                    PlaybackState.Paused -> {
                        updatePlayPauseButton(false)
                        showControlBar()
                        // 暂停时暂停弹幕
                        danmakuManager.pause()
                    }
                    else -> {}
                }
            }
        }

        lifecycleScope.launch {
            playerManager.progress.collect { progress ->
                updateProgress(progress)
            }
        }

        lifecycleScope.launch {
            playerManager.errorEvent.collect { error ->
                showError(error.message)
            }
        }
    }

    private fun updateProgress(progress: ProgressInfo) {
        if (progress.total > 0) {
            binding.seekBar.max = progress.total.toInt()
            binding.seekBar.progress = progress.current.toInt()
            binding.tvTime.text = "${formatTime(progress.current)}/${formatTime(progress.total)}"
        }
    }

    private fun updatePlayPauseButton(isPlaying: Boolean) {
        binding.btnPlayPause.setImageResource(
            if (isPlaying) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play
        )
    }

    private fun showLoading() {
        binding.statusOverlay.visibility = View.VISIBLE
        binding.progressLoading.visibility = View.VISIBLE
        binding.tvError.visibility = View.GONE
        binding.btnRetry.visibility = View.GONE
    }

    private fun showError(message: String) {
        binding.statusOverlay.visibility = View.VISIBLE
        binding.progressLoading.visibility = View.GONE
        binding.tvError.visibility = View.VISIBLE
        binding.tvError.text = message
        binding.btnRetry.visibility = View.VISIBLE
    }

    private fun hideStatusOverlay() {
        binding.statusOverlay.visibility = View.GONE
    }

    /**
     * 显示手势提示浮窗，800ms 后自动隐藏
     */
    private fun showGestureHint(text: String) {
        binding.tvGestureHint.text = text
        binding.gestureOverlay.visibility = View.VISIBLE
        gestureOverlayHideJob?.cancel()
        gestureOverlayHideJob = lifecycleScope.launch {
            delay(800)
            hideGestureOverlay()
        }
    }

    private fun hideGestureOverlay() {
        gestureOverlayHideJob?.cancel()
        binding.gestureOverlay.visibility = View.GONE
    }

    private fun scheduleHideControlBar() {
        controlBarHideJob?.cancel()
        controlBarHideJob = lifecycleScope.launch {
            delay(AppConstants.CONTROL_BAR_HIDE_DELAY_MS)
            hideControlBar()
        }
    }

    private fun showControlBar() {
        controlBarHideJob?.cancel()
        isControlBarVisible = true
        binding.controlBarTop.animate().alpha(1f).translationY(0f).duration = AppConstants.ANIM_NORMAL_MS
        binding.controlBarBottom.animate().alpha(1f).translationY(0f).duration = AppConstants.ANIM_NORMAL_MS
        binding.controlBarTop.visibility = View.VISIBLE
        binding.controlBarBottom.visibility = View.VISIBLE
    }

    private fun hideControlBar() {
        isControlBarVisible = false
        binding.controlBarTop.animate().alpha(0f).translationY(-20f).duration = AppConstants.ANIM_FAST_MS
        binding.controlBarBottom.animate().alpha(0f).translationY(20f).duration = AppConstants.ANIM_FAST_MS
    }

    private fun formatTime(ms: Long): String {
        val totalSec = ms / 1000
        val min = totalSec / 60
        val sec = totalSec % 60
        return String.format("%02d:%02d", min, sec)
    }

    // ==================== GestureCallback 实现 ====================

    override fun onToggleControlBar() {
        if (isControlBarVisible) hideControlBar() else showControlBar()
    }

    override fun onDoubleTap(x: Float, y: Float) {
        // 双击左半屏后退 10s，右半屏快进 10s
        val current = playerManager.progress.value.current
        val total = playerManager.progress.value.total
        val target = if (x < binding.root.width / 2) {
            (current - 10_000).coerceAtLeast(0)
        } else {
            (current + 10_000).coerceAtMost(total)
        }
        playerManager.seekTo(target)
        danmakuManager.seekTo(target)
        showGestureHint(formatTime(target))
    }

    override fun onLongPress() {
        // 长按 2x 倍速播放，松手恢复
        if (isLongPressSpeeding) return
        isLongPressSpeeding = true
        playerManager.setPlaybackSpeed(2.0f)
        showGestureHint("2.0x 倍速播放")
    }

    override fun onBrightnessChange(brightness: Float) {
        showGestureHint("亮度 ${(brightness * 100).toInt()}%")
    }

    override fun onVolumeChange(current: Int, max: Int) {
        showGestureHint("音量 $current/$max")
    }

    override fun onSeekPreview(positionMs: Long, totalMs: Long) {
        showGestureHint(formatTime(positionMs))
    }

    override fun onSeekComplete(positionMs: Long) {
        binding.gestureOverlay.visibility = View.GONE
        // 拖拽后同步弹幕时间轴
        danmakuManager.seekTo(positionMs)
    }

    override fun onDestroy() {
        super.onDestroy()
        // 保存播放位置
        playerManager.progress.value.let { progress ->
            com.zhuiju.app.core.player.PlayHistoryManager.savePosition(videoId, progress.current, progress.total)
        }
        danmakuManager.release()
        // 释放屏幕常亮
        PowerManager.releaseScreenOn()
        LogUtils.i("LongVideoPlayerActivity onDestroy", "LongVideoPlayer")
    }

    companion object {
        const val EXTRA_VIDEO_ID = "extra_video_id"
        const val EXTRA_VIDEO_URL = "extra_video_url"
        const val EXTRA_VIDEO_TITLE = "extra_video_title"
    }
}
