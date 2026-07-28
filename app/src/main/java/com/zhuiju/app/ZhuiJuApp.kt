package com.zhuiju.app

import android.app.Application
import android.util.Log
import com.zhuiju.app.core.security.AesKeyManager
import com.zhuiju.app.core.network.NetworkManager
import com.zhuiju.app.util.CrashHandler
import com.zhuiju.app.util.LeakDetector
import com.zhuiju.app.util.LogUtils
import com.zhuiju.app.util.PerformanceMonitor

/**
 * 应用入口 Application
 *
 * 负责全局初始化：崩溃捕获、网络框架、AES密钥管理、日志开关、性能监控、内存泄漏检测
 */
class ZhuiJuApp : Application() {

    override fun onCreate() {
        super.onCreate()
        instance = this

        // 1. 全局异常捕获（最先初始化，确保后续异常可被捕获）
        CrashHandler.init(this)

        // 2. 日志工具初始化（Debug 包开启详细日志）
        LogUtils.init(BuildConfig.DEBUG)

        // 3. 网络框架初始化
        NetworkManager.init(this)

        // 4. AES 密钥管理初始化（异步拉取动态密钥）
        AesKeyManager.init(this)

        // 5. 性能监控（仅 Debug 包启用，帧率/内存/ANR）
        PerformanceMonitor.start()

        // 6. 内存泄漏检测日志回调
        LeakDetector.leakCallback = { report ->
            LogUtils.e("内存泄漏: ${report.message}", "LeakDetector")
        }

        Log.i(TAG, "ZhuiJuApp onCreate, version=${BuildConfig.VERSION_NAME}")
    }

    override fun onTerminate() {
        super.onTerminate()
        PerformanceMonitor.stop()
        LeakDetector.clear()
        com.zhuiju.app.core.player.PowerManager.releaseAll()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        LogUtils.w("系统低内存回调，清理缓存", TAG)
        // 释放非关键资源（图片缓存等），由各模块自行响应
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        LogUtils.w("系统内存压力 level=$level", TAG)
        // 根据压力等级释放资源
        when (level) {
            TRIM_MEMORY_RUNNING_CRITICAL,
            TRIM_MEMORY_RUNNING_LOW,
            TRIM_MEMORY_RUNNING_MODERATE -> {
                // 运行时内存紧张，释放图片缓存
            }
            TRIM_MEMORY_UI_HIDDEN -> {
                // UI 不可见，可释放 UI 相关资源
            }
        }
    }

    companion object {
        private const val TAG = "ZhuiJuApp"

        @Volatile
        lateinit var instance: ZhuiJuApp
            private set
    }
}
