package com.zhuiju.app

import android.app.Application
import android.util.Log
import com.zhuiju.app.core.security.AesKeyManager
import com.zhuiju.app.core.network.NetworkManager
import com.zhuiju.app.util.CrashHandler

/**
 * 应用入口 Application
 *
 * 负责全局初始化：崩溃捕获、网络框架、AES密钥管理、日志开关
 */
class ZhuiJuApp : Application() {

    override fun onCreate() {
        super.onCreate()
        instance = this

        // 1. 全局异常捕获（最先初始化，确保后续异常可被捕获）
        CrashHandler.init(this)

        // 2. 网络框架初始化
        NetworkManager.init(this)

        // 3. AES 密钥管理初始化（异步拉取动态密钥）
        AesKeyManager.init(this)

        Log.i(TAG, "ZhuiJuApp onCreate, version=${BuildConfig.VERSION_NAME}")
    }

    companion object {
        private const val TAG = "ZhuiJuApp"

        @Volatile
        lateinit var instance: ZhuiJuApp
            private set
    }
}
