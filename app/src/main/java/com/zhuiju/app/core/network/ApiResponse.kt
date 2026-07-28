package com.zhuiju.app.core.network

import kotlinx.serialization.Serializable

/**
 * 统一 API 响应体
 *
 * 后端约定格式：
 * ```json
 * { "code": 0, "message": "success", "data": {...} }
 * ```
 * code = 0 表示成功，非 0 表示业务错误
 */
@Serializable
data class ApiResponse<T>(
    val code: Int,
    val message: String?,
    val data: T?
) {
    /** 是否成功 */
    val isSuccess: Boolean get() = code == 0
}

/**
 * 网络异常统一封装
 */
sealed class NetworkException(message: String, cause: Throwable? = null) : Exception(message, cause) {
    /** 无网络 */
    class NoNetwork : NetworkException("网络未连接，请检查网络设置")

    /** 连接超时 */
    class Timeout(cause: Throwable? = null) : NetworkException("网络连接超时，请稍后重试", cause)

    /** 客户端错误 4xx */
    class ClientError(val code: Int, message: String) : NetworkException("请求错误($code): $message")

    /** 服务端错误 5xx */
    class ServerError(val code: Int, message: String) : NetworkException("服务器异常($code)，请稍后重试")

    /** 解析失败 */
    class ParseError(cause: Throwable) : NetworkException("数据解析失败", cause)

    /** 业务错误（code 非 0） */
    class BusinessError(val code: Int, message: String?) : NetworkException(message ?: "业务异常($code)")

    /** SSL 证书错误 */
    class SslError(cause: Throwable) : NetworkException("SSL 证书验证失败", cause)
}
