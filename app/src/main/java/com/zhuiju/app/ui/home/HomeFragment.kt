package com.zhuiju.app.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.zhuiju.app.R
import com.zhuiju.app.config.AppConstants
import com.zhuiju.app.core.ui.BaseFragment
import com.zhuiju.app.databinding.FragmentHomeBinding
import com.zhuiju.app.ui.player.ShortVideoFeedAdapter
import com.zhuiju.app.ui.player.ShortVideoItem

/**
 * 首页 Fragment —— 抖音式竖屏短视频 Feed 流
 *
 * - ViewPager2 竖直方向，上下滑动切换视频
 * - offscreenPageLimit = 1，配合播放器池预加载
 * - 全屏沉浸式，强制竖屏锁定
 * - Item 布局层级≤3，保证 60 帧滑动
 */
class HomeFragment : BaseFragment<FragmentHomeBinding>() {

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
        loadVideoList()
    }

    override fun collectState() {
        // 收集 ViewModel 状态（后续接入 HomeViewModel）
    }

    private fun loadVideoList() {
        // TODO: 从 Repository 加载短视频列表
        val mockData = listOf(
            ShortVideoItem("1", "", "", "用户A", "", "这是第一条短视频"),
            ShortVideoItem("2", "", "", "用户B", "", "这是第二条短视频"),
            ShortVideoItem("3", "", "", "用户C", "", "这是第三条短视频")
        )
        adapter.submitList(mockData)
    }
}
