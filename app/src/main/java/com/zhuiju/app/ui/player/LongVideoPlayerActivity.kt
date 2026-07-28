package com.zhuiju.app.ui.player

import android.os.Bundle
import android.view.TextureView
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.zhuiju.app.R
import com.zhuiju.app.config.AppConstants
import com.zhuiju.app.core.danmaku.DanmakuManager
import com.zhuiju.app.core.danmaku.DanmakuSyncController
import com.zhuiju.app.core.player.GestureController
import com.zhuiju.app.core.player.PlaybackState
import com.zhuiju.app.core.player.PlayerManager
import com.zhuiju.app.core.player.ProgressInfo
import com.zhuiju.app.databinding.ActivityLongVideoPlayerBinding
import com.zhuiju.app.util.LogUtils
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * 长视频播放页 Activity
 *
 * - 集成 PlayerManager（ExoPlayer 单例）+ DanmakuManager（弹幕）+ GestureController（手势）
 * - 控制栏自动隐藏（3 秒无操作），动画 250ms
 * - 横竖屏适配，全屏切换
 * - 状态遮罩：加载中、错误、重试
 */
class LongVideoPlayerActivity : AppCompatActivity(), GestureController.GestureCallback {

    private lateinit var binding: ActivityLongVideoPlayerBinding
    private lateinit var playerManager: PlayerManager
    private lateinit var danmakuManager: DanmakuManager
    private lateinit var danmakuSyncController: DanmakuSyncController
    private lateinit var gestureController: GestureController

    private var controlBarHideJob: kotlinx.coroutines.Job? = null
    private var isControlBarVisible = true

    private val testVideoUrl = "https://test.zhuiju.app/sample.mp4"
    private val testVideoId = "test_001"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLongVideoPlayerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initPlayer()
        initDanmaku()
        initGesture()
        initControlBar()
        collectPlayerState()

        // 开始播放
        val savedPosition = com.zhuiju.app.core.player.PlayHistoryManager.getPosition(testVideoId)
        playerManager.play(testVideoUrl, savedPosition)
    }

    private fun initPlayer() {
        playerManager = PlayerManager.getInstance()
        playerManager.bindLifecycle(this)
        // 绑定 TextureView（ExoPlayer 1.2.1 使用 PlayerView，这里用 TextureView 自定义）
        // 实际可替换为 androidx.media3.ui.PlayerView
    }

    private fun initDanmaku() {
        danmakuManager = DanmakuManager(binding.danmakuView)
        // TODO: 从网络加载弹幕数据后调用 danmakuManager.init(danmakus)
        danmakuSyncController = DanmakuSyncController(danmakuManager)
        danmakuSyncController.bindPlayer(
            positionProvider = { playerManager.progress.value.current },
            speedProvider = { playerManager.playbackState.value.let { 1.0f } },  // TODO: 获取实际倍速
            isPlayingProvider = { playerManager.isPlaying.value }
        )
        danmakuSyncController.start()
    }

    private fun initGesture() {
        gestureController = GestureController(this, playerManager, this)
    }

    private fun initControlBar() {
        binding.btnPlayPause.setOnClickListener {
            if (playerManager.isPlaying.value) playerManager.pause() else playerManager.resume()
        }
        binding.btnBack.setOnClickListener { finish() }
        binding.btnRetry.setOnClickListener {
            playerManager.play(testVideoUrl)
        }
        // 点击切换控制栏显隐在 GestureCallback 中处理
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
                    }
                    PlaybackState.Paused -> {
                        updatePlayPauseButton(false)
                        showControlBar()
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
        // 长视频双击可点赞或快进/后退
    }

    override fun onLongPress() {
        // 长按倍速
    }

    override fun onBrightnessChange(brightness: Float) {
        binding.gestureOverlay.visibility = View.VISIBLE
        binding.tvGestureHint.text = "亮度 ${(brightness * 100).toInt()}%"
    }

    override fun onVolumeChange(current: Int, max: Int) {
        binding.gestureOverlay.visibility = View.VISIBLE
        binding.tvGestureHint.text = "音量 $current/$max"
    }

    override fun onSeekPreview(positionMs: Long, totalMs: Long) {
        binding.gestureOverlay.visibility = View.VISIBLE
        binding.tvGestureHint.text = formatTime(positionMs)
    }

    override fun onSeekComplete(positionMs: Long) {
        binding.gestureOverlay.visibility = View.GONE
    }

    override fun onDestroy() {
        super.onDestroy()
        // 保存播放位置
        playerManager.progress.value.let { progress ->
            com.zhuiju.app.core.player.PlayHistoryManager.savePosition(testVideoId, progress.current, progress.total)
        }
        danmakuSyncController.release()
        LogUtils.i("LongVideoPlayerActivity onDestroy", "LongVideoPlayer")
    }
}
