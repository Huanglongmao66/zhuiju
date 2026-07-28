package com.zhuiju.app.ui.mine

import android.view.LayoutInflater
import android.view.ViewGroup
import com.zhuiju.app.core.ui.BaseFragment
import com.zhuiju.app.databinding.FragmentMineBinding
import com.zhuiju.app.util.LogUtils

/**
 * 我的 Fragment —— 个人中心
 *
 * - CoordinatorLayout + CollapsingToolbar 折叠头部
 * - 用户数据（获赞、关注、粉丝）
 * - TabLayout + ViewPager2（作品/私密/收藏/喜欢）
 * - 双列 Grid 视频作品卡片
 */
class MineFragment : BaseFragment<FragmentMineBinding>() {

    override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentMineBinding {
        return FragmentMineBinding.inflate(inflater, container, false)
    }

    override fun initViews() {
        // 配置 Tab
        binding.tabWorks.addTab(binding.tabWorks.newTab().setText("作品"))
        binding.tabWorks.addTab(binding.tabWorks.newTab().setText("私密"))
        binding.tabWorks.addTab(binding.tabWorks.newTab().setText("收藏"))
        binding.tabWorks.addTab(binding.tabWorks.newTab().setText("喜欢"))
    }

    override fun initData() {}

    override fun onLazyInit() {
        LogUtils.i("MineFragment 懒加载", "Mine")
        // TODO: 加载用户数据、作品列表
    }

    override fun collectState() {}
}
