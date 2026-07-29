package com.zhuiju.app.ui.home

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.zhuiju.app.config.AppConstants
import com.zhuiju.app.core.player.PlayerManager
import com.zhuiju.app.core.ui.BaseFragment
import com.zhuiju.app.databinding.FragmentHomeBinding
import com.zhuiju.app.ui.player.ShortVideoFeedAdapter
import com.zhuiju.app.util.LogUtils
import kotlinx.coroutines.launch

/**
 * 首页 Fragment —— 抖音式竖屏短视频 Feed 流
 *
 * - ViewPager2 竖直方向，上下滑动切换视频
 * - offscreenPageLimit = 1，配合播放器池预加载
 * - 全屏沉浸式，强制竖屏锁定
 * - Item 布局层级≤3，保证 60 帧滑动
 *
 * 播放策略：
 * - 复用全局 [PlayerManager] 单例 ExoPlayer 实例
 * - 页面切换时通过 `setVideoTextureView` 将 ExoPlayer 绑定到当前 ViewHolder 的 TextureView
 * - 切换视频走固定流程：stop → setMediaItem → prepare → play
 */
class HomeFragment : BaseFragment<FragmentHomeBinding>() {

    private val viewModel: HomeViewModel by activityViewModels()
    private lateinit var adapter: ShortVideoFeedAdapter
    private var playerManager: PlayerManager? = null

    /** 当前播放位置（ViewPager2 选中的 page） */
    private var currentPosition = 0

    /** 是否已触发首次播放（防止重复） */
    private var hasInitialPlayed = false

    private val onPageChangeCallback = object : ViewPager2.OnPageChangeCallback() {
        override fun onPageSelected(position: Int) {
            super.onPageSelected(position)
            LogUtils.i("ViewPager2 onPageSelected: $position", "HomeFragment")
            currentPosition = position
            // 切换播放：等待 ViewHolder attach 后绑定 TextureView 并播放
            binding.viewPager.post { playAt(position) }
        }
    }

    override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentHomeBinding {
        return FragmentHomeBinding.inflate(inflater, container, false)
    }

    override fun initViews() {
        // 配置 ViewPager2 竖直滑动
        binding.viewPager.apply {
            orientation = ViewPager2.ORIENTATION_VERTICAL
            offscreenPageLimit = AppConstants.SHORT_VIDEO_OFFSCREEN_PAGE_LIMIT
            // 禁用横向滑动，仅允许上下滑动
            getChildAt(0).overScrollMode = RecyclerView.OVER_SCROLL_NEVER
            registerOnPageChangeCallback(onPageChangeCallback)
        }
        adapter = ShortVideoFeedAdapter()
        binding.viewPager.adapter = adapter
    }

    override fun initData() {
        // 页面可见时恢复播放：
        // - 若 ExoPlayer 仍存活（切 Tab 回来），重新 play 会 stop + 加载当前视频
        // - 若 ExoPlayer 已被长视频播放页 release，play 会自动 ensurePlayer 重建
        if (hasInitialPlayed) {
            binding.viewPager.post { playAt(currentPosition) }
        }
    }

    override fun onLazyInit() {
        // 首次可见时加载视频列表
        viewModel.loadShortVideos()
    }

    override fun collectState() {
        // 初始化 PlayerManager 并绑定生命周期
        playerManager = PlayerManager.getInstance().also {
            it.bindLifecycle(viewLifecycleOwner)
        }

        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.videos.collect { list ->
                    adapter.submitList(list) {
                        // 数据提交完成后触发首次播放
                        if (list.isNotEmpty() && !hasInitialPlayed) {
                            hasInitialPlayed = true
                            binding.viewPager.post { playAt(0) }
                        }
                    }
                }
            }
        }
    }

    /**
     * 播放指定位置的视频
     *
     * - 通过 RecyclerView 找到对应 ViewHolder
     * - 将 ExoPlayer 绑定到其 TextureView
     * - 调用 [PlayerManager.play] 切换 MediaItem
     */
    private fun playAt(position: Int) {
        val pm = playerManager ?: return
        val recyclerView = binding.viewPager.getChildAt(0) as? RecyclerView ?: return
        val holder = adapter.findAttachedViewHolder(recyclerView, position) ?: run {
            LogUtils.w("ViewHolder 未 attach，无法播放: $position", "HomeFragment")
            return
        }
        val item = holder.boundItem ?: return

        // 将 ExoPlayer 绑定到当前页 TextureView（切换视频时重新绑定）
        pm.getPlayer().setVideoTextureView(holder.textureView)
        pm.play(item.videoUrl)
        LogUtils.i("首页播放: pos=$position, url=${item.videoUrl}", "HomeFragment")
    }

    override fun onPause() {
        super.onPause()
        // 离开首页时暂停播放，避免与长视频播放页抢音频焦点
        playerManager?.pause()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.viewPager.unregisterOnPageChangeCallback(onPageChangeCallback)
        // 不调用 playerManager.release()：PlayerManager 已通过 bindLifecycle
        // 跟随 viewLifecycleOwner 自动在 ON_DESTROY 释放
    }
}
