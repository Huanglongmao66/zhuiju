package com.zhuiju.app.core.network

import com.zhuiju.app.util.LogUtils
import okhttp3.Interceptor
import okhttp3.Response

/**
 * 鉴权拦截器
 *
 * - 自动携带 Token（登录后由 [AuthManager] 提供）
 * - 请求签名防篡改（后续阶段接入 AES + 时间戳 + 随机数签名）
 * - 401 响应触发 Token 刷新（后续阶段实现）
 */
class AuthInterceptor : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()

        val token = AuthManager.getToken()
        val requestBuilder = original.newBuilder()
        if (!token.isNullOrEmpty()) {
            requestBuilder.header("Authorization", "Bearer $token")
        }

        // TODO: 阶段四接入请求签名（AES 加密 + 时间戳 + nonce）
        // val signature = SignatureUtils.sign(original.url.toString(), timestamp, nonce)
        // requestBuilder.header("X-Signature", signature)
        // requestBuilder.header("X-Timestamp", timestamp.toString())
        // requestBuilder.header("X-Nonce", nonce)

        val response = chain.proceed(requestBuilder.build())

        // 401 未授权：清除 Token，触发刷新（后续阶段实现）
        if (response.code == 401) {
            LogUtils.w("收到 401 响应，Token 已失效", "AuthInterceptor")
            AuthManager.clearToken()
        }

        return response
    }
}
