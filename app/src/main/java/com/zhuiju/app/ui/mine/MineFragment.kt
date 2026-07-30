package com.zhuiju.app.ui.mine

import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.GridLayoutManager
import com.bumptech.glide.Glide
import com.google.android.material.tabs.TabLayout
import com.zhuiju.app.core.ui.BaseFragment
import com.zhuiju.app.data.Video
import com.zhuiju.app.databinding.FragmentMineBinding
import com.zhuiju.app.ui.discover.DiscoverAdapter
import com.zhuiju.app.ui.player.LongVideoPlayerActivity
import com.zhuiju.app.ui.settings.SettingsActivity
import kotlinx.coroutines.launch

/**
 * 我的 Fragment —— 个人中心
 *
 * - CoordinatorLayout + CollapsingToolbar 折叠头部
 * - 用户数据（获赞、关注、粉丝）
 * - TabLayout 切换 作品/私密/收藏/喜欢
 * - 双列 Grid 视频作品卡片
 */
class MineFragment : BaseFragment<FragmentMineBinding>() {

    private val viewModel: MineViewModel by activityViewModels()
    private lateinit var worksAdapter: DiscoverAdapter

    override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentMineBinding {
        return FragmentMineBinding.inflate(inflater, container, false)
    }

    override fun initViews() {
        // 设置按钮
        binding.btnSettings.setOnClickListener {
            startActivity(Intent(requireContext(), SettingsActivity::class.java))
        }

        // 配置 Tab
        binding.tabWorks.addTab(binding.tabWorks.newTab().setText("作品"))
        binding.tabWorks.addTab(binding.tabWorks.newTab().setText("私密"))
        binding.tabWorks.addTab(binding.tabWorks.newTab().setText("收藏"))
        binding.tabWorks.addTab(binding.tabWorks.newTab().setText("喜欢"))

        binding.tabWorks.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                viewModel.loadTab(tab.position)
            }
            override fun onTabUnselected(tab: TabLayout.Tab) {}
            override fun onTabReselected(tab: TabLayout.Tab) {}
        })

        // 作品列表（双列）
        worksAdapter = DiscoverAdapter { video -> navigateToPlayer(video) }
        binding.rvWorks.apply {
            layoutManager = GridLayoutManager(requireContext(), 2)
            adapter = worksAdapter
        }
    }

    override fun initData() {}

    override fun onLazyInit() {
        viewModel.loadUserData()
    }

    override fun collectState() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.userProfile.collect { profile ->
                        profile?.let { p ->
                            binding.tvNickname.text = p.nickname
                            binding.tvLikeCount.text = formatCount(p.likeCount.toLong())
                            binding.tvFollowingCount.text = formatCount(p.followingCount.toLong())
                            binding.tvFollowerCount.text = formatCount(p.followerCount.toLong())
                            Glide.with(binding.ivAvatar)
                                .load(p.avatarUrl)
                                .circleCrop()
                                .into(binding.ivAvatar)
                        }
                    }
                }
                launch {
                    viewModel.works.collect { works ->
                        worksAdapter.submitList(works)
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

    private fun formatCount(count: Long): String = when {
        count >= 10000 -> String.format("%.1f万", count / 10000.0)
        else -> count.toString()
    }
}
