package com.zhuiju.app.ui.discover

import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.GridLayoutManager
import com.google.android.material.tabs.TabLayout
import com.zhuiju.app.core.ui.BaseFragment
import com.zhuiju.app.data.Video
import com.zhuiju.app.databinding.FragmentDiscoverBinding
import com.zhuiju.app.ui.player.LongVideoPlayerActivity
import kotlinx.coroutines.launch

/**
 * 发现页 Fragment —— 热门推荐、合集
 *
 * - 顶部 Banner 轮播
 * - 分类 Tab 栏（可滚动）
 * - 双列 Grid RecyclerView（视频卡片 9:16）
 */
class DiscoverFragment : BaseFragment<FragmentDiscoverBinding>() {

    private val viewModel: DiscoverViewModel by activityViewModels()
    private lateinit var bannerAdapter: BannerAdapter
    private lateinit var videoAdapter: DiscoverAdapter

    override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentDiscoverBinding {
        return FragmentDiscoverBinding.inflate(inflater, container, false)
    }

    override fun initViews() {
        // Banner 配置
        bannerAdapter = BannerAdapter { banner ->
            // 点击 Banner 跳转播放页
            startActivity(
                Intent(requireContext(), LongVideoPlayerActivity::class.java).apply {
                    putExtra(LongVideoPlayerActivity.EXTRA_VIDEO_ID, banner.videoId)
                }
            )
        }
        binding.banner.apply {
            adapter = bannerAdapter
            // Banner 自动轮播：每 3 秒切换
            postDelayed({
                if (isAdded) startAutoScroll()
            }, 3000)
        }

        // 分类 Tab
        binding.tabCategory.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                viewModel.filterByCategory(tab.position)
            }
            override fun onTabUnselected(tab: TabLayout.Tab) {}
            override fun onTabReselected(tab: TabLayout.Tab) {}
        })

        // 双列视频列表
        videoAdapter = DiscoverAdapter { video -> navigateToPlayer(video) }
        binding.recyclerView.apply {
            layoutManager = GridLayoutManager(requireContext(), 2)
            setHasFixedSize(true)
            adapter = videoAdapter
        }
    }

    override fun initData() {}

    override fun onLazyInit() {
        viewModel.loadData()
    }

    override fun collectState() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.banners.collect { banners ->
                        bannerAdapter.submitList(banners)
                    }
                }
                launch {
                    viewModel.categories.collect { categories ->
                        // 首次加载时填充 Tab
                        if (binding.tabCategory.tabCount == 0 && categories.isNotEmpty()) {
                            // 添加"全部"Tab
                            binding.tabCategory.addTab(binding.tabCategory.newTab().setText("全部"))
                            categories.forEach { cat ->
                                binding.tabCategory.addTab(binding.tabCategory.newTab().setText(cat.name))
                            }
                        }
                    }
                }
                launch {
                    viewModel.videos.collect { videos ->
                        videoAdapter.submitList(videos)
                    }
                }
            }
        }
    }

    private fun navigateToPlayer(video: Video) {
        startActivity(
            Intent(requireContext(), LongVideoPlayerActivity::class.java).apply {
                putExtra(LongVideoPlayerActivity.EXTRA_VIDEO_ID, video.id)
                putExtra(LongVideoPlayerActivity.EXTRA_VIDEO_URL, video.videoUrl)
                putExtra(LongVideoPlayerActivity.EXTRA_VIDEO_TITLE, video.title)
            }
        )
    }

    private fun startAutoScroll() {
        // 简单的 Banner 自动轮询
        if (!isAdded) return
        val viewPager = binding.banner
        val count = bannerAdapter.itemCount
        if (count <= 1) return
        val next = (viewPager.currentItem + 1) % count
        viewPager.setCurrentItem(next, true)
        viewPager.postDelayed({ if (isAdded) startAutoScroll() }, 3000)
    }
}
