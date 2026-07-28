package com.zhuiju.app.core.network

import com.zhuiju.app.util.LogUtils
import com.zhuiju.app.util.ToastUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLException

/**
 * HTTP 工具
 *
 * - 封装 GET/POST 请求，基于协程
 * - 统一异常转换（[toNetworkException]）
 * - 统一 JSON 解析
 */
object HttpUtils {

    internal val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        encodeDefaults = true
    }

    internal const val MEDIA_TYPE_JSON = "application/json; charset=utf-8"

    /**
     * GET 请求
     *
     * @param url 完整 URL
     * @param params 查询参数
     * @return 响应字符串
     */
    suspend fun get(
        url: String,
        params: Map<String, Any?> = emptyMap()
    ): String = withContext(Dispatchers.IO) {
        val fullUrl = buildUrl(url, params)
        val request = Request.Builder().url(fullUrl).get().build()
        execute(NetworkManager.client, request)
    }

    /**
     * POST JSON 请求
     */
    suspend inline fun <reified T> postJson(
        url: String,
        body: T
    ): String = withContext(Dispatchers.IO) {
        val jsonStr = json.encodeToString(kotlinx.serialization.serializer<T>(), body)
        val request = Request.Builder()
            .url(url)
            .post(jsonStr.toRequestBody(MEDIA_TYPE_JSON.toMediaType()))
            .build()
        execute(NetworkManager.client, request)
    }

    /**
     * 执行请求并转换异常
     */
    fun execute(client: OkHttpClient, request: Request): String {
        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw when {
                        response.code in 400..499 -> NetworkException.ClientError(response.code, response.message)
                        response.code in 500..599 -> NetworkException.ServerError(response.code, response.message)
                        else -> NetworkException.BusinessError(response.code, response.message)
                    }
                }
                return response.body?.string() ?: throw NetworkException.ParseError(IllegalStateException("响应体为空"))
            }
        } catch (e: SocketTimeoutException) {
            throw NetworkException.Timeout(e)
        } catch (e: UnknownHostException) {
            throw NetworkException.NoNetwork()
        } catch (e: SSLException) {
            throw NetworkException.SslError(e)
        } catch (e: IOException) {
            throw NetworkException.NoNetwork()
        }
    }

    /**
     * 解析 API 响应
     */
    inline fun <reified T> parseApiResponse(jsonStr: String): ApiResponse<T> {
        return try {
            json.decodeFromString(ApiResponse.serializer(kotlinx.serialization.serializer()), jsonStr)
        } catch (e: Throwable) {
            LogUtils.e("解析 API 响应失败: ${e.message}", "HttpUtils", e)
            throw NetworkException.ParseError(e)
        }
    }

    /**
     * 统一处理网络异常（弹 Toast）
     */
    fun handleException(throwable: Throwable) {
        val message = when (throwable) {
            is NetworkException.NoNetwork -> throwable.message
            is NetworkException.Timeout -> throwable.message
            is NetworkException.ClientError -> throwable.message
            is NetworkException.ServerError -> throwable.message
            is NetworkException.BusinessError -> throwable.message
            is NetworkException.ParseError -> throwable.message
            is NetworkException.SslError -> throwable.message
            else -> "网络异常，请稍后重试"
        }
        ToastUtils.show(message ?: "网络异常")
    }

    /**
     * 构建带查询参数的 URL
     */
    private fun buildUrl(baseUrl: String, params: Map<String, Any?>): String {
        if (params.isEmpty()) return baseUrl
        val sb = StringBuilder(baseUrl)
        if (!baseUrl.contains("?")) sb.append("?") else sb.append("&")
        params.entries.forEachIndexed { index, (key, value) ->
            if (value == null) return@forEachIndexed
            if (index > 0) sb.append("&")
            sb.append(key).append("=").append(value.toString())
        }
        return sb.toString()
    }
}
