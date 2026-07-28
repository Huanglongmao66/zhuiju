package com.zhuiju.app.core.network

import android.content.Context
import com.zhuiju.app.config.AppConstants
import com.zhuiju.app.util.LogUtils
import okhttp3.ConnectionPool
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit

/**
 * 网络管理器
 *
 * - 全局唯一 OkHttpClient 单例，禁止多处 new Client
 * - 区分普通接口超时（[AppConstants.NET_TIMEOUT_NORMAL_S]）与视频分片下载超时（[AppConstants.NET_TIMEOUT_SLICE_S]）
 * - 开启连接池复用，减少连接创建延迟
 * - 统一拦截器：请求头、Token、日志、异常
 *
 * 初始化：在 Application 中调用 [NetworkManager.init]
 */
object NetworkManager {

    private const val TAG = "NetworkManager"

    /** 普通接口客户端（15s 超时） */
    lateinit var client: OkHttpClient
        private set

    /** 视频分片下载客户端（30s 超时） */
    lateinit var downloadClient: OkHttpClient
        private set

    private var initialized = false

    /**
     * 初始化网络框架
     */
    fun init(context: Context) {
        if (initialized) return
        initialized = true

        val connectionPool = ConnectionPool(
            AppConstants.NET_CONNECTION_POOL_MAX,
            AppConstants.NET_CONNECTION_KEEP_ALIVE_S,
            TimeUnit.SECONDS
        )

        val loggingInterceptor = HttpLoggingInterceptor { msg ->
            LogUtils.d(msg, "OkHttp")
        }.apply {
            level = if (com.zhuiju.app.BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BODY
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }

        // 普通接口客户端
        client = OkHttpClient.Builder()
            .connectTimeout(AppConstants.NET_TIMEOUT_NORMAL_S, TimeUnit.SECONDS)
            .readTimeout(AppConstants.NET_TIMEOUT_NORMAL_S, TimeUnit.SECONDS)
            .writeTimeout(AppConstants.NET_TIMEOUT_NORMAL_S, TimeUnit.SECONDS)
            .connectionPool(connectionPool)
            .addInterceptor(HeaderInterceptor())
            .addInterceptor(AuthInterceptor())
            .addInterceptor(loggingInterceptor)
            .retryOnConnectionFailure(true)
            .build()

        // 视频分片下载客户端（独立超时配置）
        downloadClient = client.newBuilder()
            .connectTimeout(AppConstants.NET_TIMEOUT_SLICE_S, TimeUnit.SECONDS)
            .readTimeout(AppConstants.NET_TIMEOUT_SLICE_S, TimeUnit.SECONDS)
            .writeTimeout(AppConstants.NET_TIMEOUT_SLICE_S, TimeUnit.SECONDS)
            .build()

        LogUtils.i("NetworkManager 初始化完成", TAG)
    }
}
