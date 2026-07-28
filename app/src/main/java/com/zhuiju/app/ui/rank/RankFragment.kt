package com.zhuiju.app.ui.rank

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import com.zhuiju.app.core.ui.BaseFragment
import com.zhuiju.app.databinding.FragmentRankBinding
import com.zhuiju.app.util.LogUtils

/**
 * 排行榜 Fragment —— 日榜/周榜/月榜
 *
 * - 顶部 TabLayout 切换日/周/月榜
 * - 单列 RecyclerView，Item 含排名+头像+昵称+封面+热度+关注按钮
 * - 前三名特殊样式（金/银/铜牌）
 */
class RankFragment : BaseFragment<FragmentRankBinding>() {

    override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentRankBinding {
        return FragmentRankBinding.inflate(inflater, container, false)
    }

    override fun initViews() {
        // 配置日/周/月榜 Tab
        binding.tabPeriod.addTab(binding.tabPeriod.newTab().setText("日榜"))
        binding.tabPeriod.addTab(binding.tabPeriod.newTab().setText("周榜"))
        binding.tabPeriod.addTab(binding.tabPeriod.newTab().setText("月榜"))

        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
    }

    override fun initData() {}

    override fun onLazyInit() {
        LogUtils.i("RankFragment 懒加载", "Rank")
        // TODO: 加载榜单数据
    }

    override fun collectState() {}
}
