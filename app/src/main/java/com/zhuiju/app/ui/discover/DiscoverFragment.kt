package com.zhuiju.app.ui.discover

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.GridLayoutManager
import com.zhuiju.app.core.ui.BaseFragment
import com.zhuiju.app.databinding.FragmentDiscoverBinding
import com.zhuiju.app.util.LogUtils

/**
 * 发现页 Fragment —— 热门推荐、合集
 *
 * - 顶部 Banner 轮播
 * - 分类 Tab 栏（可滚动）
 * - 双列 Grid RecyclerView（视频卡片 9:16）
 */
class DiscoverFragment : BaseFragment<FragmentDiscoverBinding>() {

    override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentDiscoverBinding {
        return FragmentDiscoverBinding.inflate(inflater, container, false)
    }

    override fun initViews() {
        binding.recyclerView.layoutManager = GridLayoutManager(requireContext(), 2)
        binding.recyclerView.setHasFixedSize(true)
    }

    override fun initData() {}

    override fun onLazyInit() {
        LogUtils.i("DiscoverFragment 懒加载", "Discover")
        // TODO: 加载热门推荐数据
    }

    override fun collectState() {}
}
