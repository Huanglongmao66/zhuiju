package com.zhuiju.app.ui.player

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.zhuiju.app.config.AppConstants
import com.zhuiju.app.core.danmaku.DanmakuBuilder
import com.zhuiju.app.core.danmaku.DanmakuManager
import com.zhuiju.app.core.danmaku.DanmakuSyncController
import com.zhuiju.app.core.danmaku.DanmakuData
import com.zhuiju.app.core.player.GestureController
import com.zhuiju.app.core.player.PlaybackState
import com.zhuiju.app.core.player.PlayerManager
import com.zhuiju.app.core.player.PowerManager
import com.zhuiju.app.core.player.ProgressInfo
import com.zhuiju.app.data.MockData
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
    private lateinit var danmakuSyncController: DanmakuSyncController
    private lateinit var gestureController: GestureController

    private var controlBarHideJob: kotlinx.coroutines.Job? = null
    private var isControlBarVisible = true

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
        // 绑定 TextureView（ExoPlayer 1.2.1 使用 PlayerView，这里用 TextureView 自定义）
        // 实际可替换为 androidx.media3.ui.PlayerView
    }

    private fun initDanmaku() {
        danmakuManager = DanmakuManager(binding.danmakuView)
        // 加载 Mock 弹幕数据
        val danmakuDataList = MockData.longVideoDanmakus.map { info ->
            DanmakuData(
                text = info.text,
                timeMs = info.timeMs,
                color = info.color,
                type = info.type
            )
        }
        val danmakus = DanmakuBuilder.buildDanmakus(danmakuDataList)
        danmakuManager.init(danmakus)
        danmakuManager.start()

        danmakuSyncController = DanmakuSyncController(danmakuManager)
        danmakuSyncController.bindPlayer(
            positionProvider = { playerManager.progress.value.current },
            speedProvider = { playerManager.playbackState.value.let { 1.0f } },  // TODO: 获取实际倍速
            isPlayingProvider = { playerManager.isPlaying.value }
        )
        danmakuSyncController.start()
        LogUtils.i("弹幕加载完成: ${danmakuDataList.size} 条", "LongVideoPlayer")
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
            playerManager.play(videoUrl)
        }
        binding.btnDanmaku.setOnClickListener {
            if (danmakuManager.isVisible.value) danmakuManager.hide() else danmakuManager.show()
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
        // 拖拽后同步弹幕时间轴
        danmakuManager.seekTo(positionMs)
    }

    override fun onDestroy() {
        super.onDestroy()
        // 保存播放位置
        playerManager.progress.value.let { progress ->
            com.zhuiju.app.core.player.PlayHistoryManager.savePosition(videoId, progress.current, progress.total)
        }
        danmakuSyncController.release()
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
