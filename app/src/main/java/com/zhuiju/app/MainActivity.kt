package com.zhuiju.app

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

/**
 * 主 Activity —— 承载五大主页 Fragment 的容器
 *
 * 后续阶段将集成：ViewPager2 + 底部导航栏 + 5大Fragment（首页/发现/排行榜/找片/我的）
 */
class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }
}
