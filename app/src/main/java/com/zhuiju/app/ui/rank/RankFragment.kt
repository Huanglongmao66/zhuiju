package com.zhuiju.app.ui.rank

import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.tabs.TabLayout
import com.zhuiju.app.core.ui.BaseFragment
import com.zhuiju.app.databinding.FragmentRankBinding
import com.zhuiju.app.ui.player.LongVideoPlayerActivity
import kotlinx.coroutines.launch

/**
 * 排行榜 Fragment —— 日榜/周榜/月榜
 *
 * - 顶部 TabLayout 切换日/周/月榜
 * - 单列 RecyclerView，Item 含排名+头像+昵称+封面+热度+关注按钮
 * - 前三名特殊样式（金/银/铜牌）
 */
class RankFragment : BaseFragment<FragmentRankBinding>() {

    private val viewModel: RankViewModel by activityViewModels()
    private lateinit var adapter: RankAdapter

    override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentRankBinding {
        return FragmentRankBinding.inflate(inflater, container, false)
    }

    override fun initViews() {
        // 配置日/周/月榜 Tab
        binding.tabPeriod.addTab(binding.tabPeriod.newTab().setText("日榜"))
        binding.tabPeriod.addTab(binding.tabPeriod.newTab().setText("周榜"))
        binding.tabPeriod.addTab(binding.tabPeriod.newTab().setText("月榜"))

        binding.tabPeriod.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                viewModel.loadRank(tab.position)
                binding.tvRankTitle.text = tab.text
            }
            override fun onTabUnselected(tab: TabLayout.Tab) {}
            override fun onTabReselected(tab: TabLayout.Tab) {}
        })

        // 配置列表
        adapter = RankAdapter { item ->
            startActivity(
                Intent(requireContext(), LongVideoPlayerActivity::class.java).apply {
                    putExtra(LongVideoPlayerActivity.EXTRA_VIDEO_ID, item.video.id)
                    putExtra(LongVideoPlayerActivity.EXTRA_VIDEO_URL, item.video.videoUrl)
                    putExtra(LongVideoPlayerActivity.EXTRA_VIDEO_TITLE, item.video.title)
                }
            )
        }
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@RankFragment.adapter
        }
    }

    override fun initData() {}

    override fun onLazyInit() {
        viewModel.loadRank(RankViewModel.TYPE_DAILY)
        binding.tvRankTitle.text = "日榜"
    }

    override fun collectState() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.rankList.collect { list ->
                    adapter.submitList(list)
                }
            }
        }
    }
}
