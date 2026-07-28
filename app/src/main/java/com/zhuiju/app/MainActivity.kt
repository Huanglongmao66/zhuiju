package com.zhuiju.app

import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.zhuiju.app.core.ui.BaseActivity
import com.zhuiju.app.databinding.ActivityMainBinding
import com.zhuiju.app.ui.discover.DiscoverFragment
import com.zhuiju.app.ui.find.FindFragment
import com.zhuiju.app.ui.home.HomeFragment
import com.zhuiju.app.ui.mine.MineFragment
import com.zhuiju.app.ui.rank.RankFragment

/**
 * 主 Activity —— 承载五大主页 Fragment 的容器
 *
 * - ViewPager2 + BottomNavigationView 双向联动
 * - 5 大主页：首页（短视频Feed）/发现/排行榜/找片/我的
 * - 默认首页为短视频 Feed，沉浸式体验
 * - 禁止页面重建时状态丢失，配置 configChanges 适配横竖屏
 */
class MainActivity : BaseActivity<ActivityMainBinding>() {

    override fun inflateBinding(): ActivityMainBinding = ActivityMainBinding.inflate(layoutInflater)

    override fun initViews() {
        setupViewPager()
        setupBottomNav()
    }

    override fun initData() {
        // 默认选中首页（短视频 Feed）
        binding.viewPager.currentItem = 0
    }

    override fun collectState() {
        // 暂无全局状态
    }

    /**
     * 配置 ViewPager2：禁用左右滑动（仅由底部导航切换），
     * 但首页短视频 Feed 内部自身是竖直 ViewPager2，由 Fragment 内部处理
     */
    private fun setupViewPager() {
        binding.viewPager.apply {
            adapter = MainPagerAdapter(this@MainActivity)
            // 禁用横向滑动平滑动画，避免与短视频 Feed 内部竖直滑动冲突
            isUserInputEnabled = false
            offscreenPageLimit = 1
            registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    binding.bottomNav.menu.getItem(position).isChecked = true
                }
            })
        }
    }

    /**
     * 配置底部导航：点击切换 ViewPager2 页面
     */
    private fun setupBottomNav() {
        binding.bottomNav.setOnItemSelectedListener { item ->
            val index = when (item.itemId) {
                R.id.nav_home -> 0
                R.id.nav_discover -> 1
                R.id.nav_rank -> 2
                R.id.nav_find -> 3
                R.id.nav_mine -> 4
                else -> 0
            }
            binding.viewPager.currentItem = index
            true
        }
        // 禁用底部导航长按菜单提示（避免长按卡顿）
        binding.bottomNav.labelVisibilityMode = BottomNavigationView.LABEL_VISIBILITY_LABELED
    }

    /**
     * 主页 ViewPager2 适配器
     * - 使用 FragmentStateAdapter 自动管理 Fragment 生命周期
     * - 5 个页面常驻，避免频繁创建销毁
     */
    private inner class MainPagerAdapter(activity: androidx.fragment.app.FragmentActivity) :
        FragmentStateAdapter(activity) {

        override fun getItemCount(): Int = 5

        override fun createFragment(position: Int): Fragment {
            return when (position) {
                0 -> HomeFragment()
                1 -> DiscoverFragment()
                2 -> RankFragment()
                3 -> FindFragment()
                else -> MineFragment()
            }
        }
    }
}
