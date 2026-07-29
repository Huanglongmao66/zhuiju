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
import com.zhuiju.app.core.ui.BaseFragment
import com.zhuiju.app.databinding.FragmentHomeBinding
import com.zhuiju.app.ui.player.ShortVideoFeedAdapter
import kotlinx.coroutines.launch

/**
 * 首页 Fragment —— 抖音式竖屏短视频 Feed 流
 *
 * - ViewPager2 竖直方向，上下滑动切换视频
 * - offscreenPageLimit = 1，配合播放器池预加载
 * - 全屏沉浸式，强制竖屏锁定
 * - Item 布局层级≤3，保证 60 帧滑动
 */
class HomeFragment : BaseFragment<FragmentHomeBinding>() {

    private val viewModel: HomeViewModel by activityViewModels()
    private lateinit var adapter: ShortVideoFeedAdapter

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
        }
        adapter = ShortVideoFeedAdapter()
        binding.viewPager.adapter = adapter
    }

    override fun initData() {
        // 页面可见时恢复播放（由 ViewPager2 页面切换回调管理）
    }

    override fun onLazyInit() {
        // 首次可见时加载视频列表
        viewModel.loadShortVideos()
    }

    override fun collectState() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.videos.collect { list ->
                    adapter.submitList(list)
                }
            }
        }
    }
}
