package com.zhuiju.app.core.security

import com.zhuiju.app.util.LogUtils
import okhttp3.FormBody
import okhttp3.Interceptor
import okhttp3.Response
import java.util.UUID

/**
 * 请求签名拦截器
 *
 * - 所有请求统一携带签名（MD5 + 时间戳 + nonce）
 * - 防篡改、防重放
 * - 接入 [SecurityUtils.sign] 工具
 *
 * 签名规则：MD5(url + timestamp + nonce + secretKey)
 */
class SignatureInterceptor : Interceptor {

    private companion object {
        const val TAG = "SignatureInterceptor"
        // TODO: 密钥应从 KeyService 动态获取，当前占位
        const val SECRET_KEY = "zhuiju_secret_placeholder"
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val timestamp = System.currentTimeMillis()
        val nonce = SecurityUtils.generateNonce()
        val url = original.url.toString()

        // 计算签名
        val signature = SecurityUtils.sign(url, timestamp, nonce, SECRET_KEY)

        val signedRequest = original.newBuilder()
            .header("X-Signature", signature)
            .header("X-Timestamp", timestamp.toString())
            .header("X-Nonce", nonce)
            .build()

        LogUtils.secure("签名请求: url=$url, ts=$timestamp, nonce=$nonce", TAG)

        val response = chain.proceed(signedRequest)

        // 校验响应签名（可选，服务端可返回响应签名）
        // TODO: 校验响应签名

        return response
    }
}
