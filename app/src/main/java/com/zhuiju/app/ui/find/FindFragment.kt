package com.zhuiju.app.ui.find

import android.view.LayoutInflater
import android.view.ViewGroup
import com.zhuiju.app.core.ui.BaseFragment
import com.zhuiju.app.databinding.FragmentFindBinding
import com.zhuiju.app.util.LogUtils

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

    override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentFindBinding {
        return FragmentFindBinding.inflate(inflater, container, false)
    }

    override fun initViews() {
        binding.etSearch.setOnEditorActionListener { v, _, _ ->
            val keyword = v.text.toString().trim()
            if (keyword.isNotEmpty()) {
                search(keyword)
            }
            true
        }
    }

    override fun initData() {}

    override fun onLazyInit() {
        LogUtils.i("FindFragment 懒加载", "Find")
        // TODO: 加载热门搜索、分类、分区数据
    }

    override fun collectState() {}

    private fun search(keyword: String) {
        LogUtils.i("搜索: $keyword", "Find")
        // TODO: 调用 Repository 搜索，切换 UI 到搜索结果状态
    }
}
