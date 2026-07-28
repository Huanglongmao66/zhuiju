package com.zhuiju.app.core.network

import com.zhuiju.app.BuildConfig
import okhttp3.Interceptor
import okhttp3.Response

/**
 * 请求头统一拦截器
 *
 * 统一配置通用请求头：UA、Content-Type、Accept、应用版本
 */
class HeaderInterceptor : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request().newBuilder()
            .header("User-Agent", "ZhuiJuApp/${BuildConfig.VERSION_NAME} (Android)")
            .header("Content-Type", "application/json; charset=utf-8")
            .header("Accept", "application/json")
            .header("App-Version", BuildConfig.VERSION_NAME)
            .header("App-Version-Code", BuildConfig.VERSION_CODE.toString())
            .build()
        return chain.proceed(request)
    }
}
