package com.zhuiju.app.ui.find

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.chip.Chip
import com.zhuiju.app.core.ui.BaseFragment
import com.zhuiju.app.data.Video
import com.zhuiju.app.databinding.FragmentFindBinding
import com.zhuiju.app.ui.discover.DiscoverAdapter
import com.zhuiju.app.ui.player.LongVideoPlayerActivity
import kotlinx.coroutines.launch

/**
 * 找片 Fragment —— 搜索 + 分类
 *
 * - 顶部搜索框（搜索、清空、历史搜索）
 * - 热门搜索标签流式布局
 * - 全部分类横向列表
 * - 多分区横向视频列表（ScrollView 承载，禁止 RecyclerView 嵌套 RecyclerView）
 * - 搜索状态与浏览状态 UI 自动切换
 */
class FindFragment : BaseFragment<FragmentFindBinding>() {

    private val viewModel: FindViewModel by activityViewModels()
    private lateinit var categoryAdapter: FindCategoryAdapter
    private lateinit var sectionAdapter: DiscoverAdapter
    private lateinit var searchResultAdapter: DiscoverAdapter

    override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentFindBinding {
        return FragmentFindBinding.inflate(inflater, container, false)
    }

    override fun initViews() {
        // 搜索框
        binding.etSearch.setOnEditorActionListener { v, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                val keyword = v.text.toString().trim()
                if (keyword.isNotEmpty()) {
                    viewModel.search(keyword)
                    binding.btnClearSearch.visibility = View.VISIBLE
                }
                true
            } else {
                false
            }
        }

        // 清空搜索
        binding.btnClearSearch.setOnClickListener {
            binding.etSearch.text.clear()
            viewModel.clearSearch()
            it.visibility = View.GONE
        }

        // 分类列表（横向）
        categoryAdapter = FindCategoryAdapter { category ->
            // 点击分类直接搜索该分类名称
            binding.etSearch.setText(category.name)
            viewModel.search(category.name)
            binding.btnClearSearch.visibility = View.VISIBLE
        }
        binding.rvCategory.apply {
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
            adapter = categoryAdapter
        }

        // 分区1（推荐视频，双列）
        sectionAdapter = DiscoverAdapter { video -> navigateToPlayer(video) }
        binding.rvSection1.apply {
            layoutManager = GridLayoutManager(requireContext(), 3)
            adapter = sectionAdapter
        }

        // 搜索结果列表（双列）
        searchResultAdapter = DiscoverAdapter { video -> navigateToPlayer(video) }
        binding.rvSearchResult.apply {
            layoutManager = GridLayoutManager(requireContext(), 2)
            adapter = searchResultAdapter
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
                    viewModel.hotSearchWords.collect { words ->
                        binding.chipHotSearch.removeAllViews()
                        words.forEach { word ->
                            val chip = Chip(requireContext()).apply {
                                text = word
                                isCheckable = false
                                isClickable = true
                                setOnClickListener {
                                    binding.etSearch.setText(word)
                                    viewModel.search(word)
                                    binding.btnClearSearch.visibility = View.VISIBLE
                                }
                            }
                            binding.chipHotSearch.addView(chip)
                        }
                    }
                }
                launch {
                    viewModel.categories.collect { categories ->
                        categoryAdapter.submitList(categories)
                    }
                }
                launch {
                    viewModel.sectionVideos.collect { videos ->
                        sectionAdapter.submitList(videos)
                    }
                }
                launch {
                    viewModel.isSearching.collect { isSearching ->
                        binding.scrollBrowse.visibility = if (isSearching) View.GONE else View.VISIBLE
                        binding.rvSearchResult.visibility = if (isSearching) View.VISIBLE else View.GONE
                    }
                }
                launch {
                    viewModel.searchResults.collect { results ->
                        searchResultAdapter.submitList(results)
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
}
