package com.zhuiju.app.core.security

import com.zhuiju.app.core.network.ApiResponse
import com.zhuiju.app.core.network.HttpUtils
import com.zhuiju.app.util.LogUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable

/**
 * 密钥服务
 *
 * - 对接服务端密钥接口，拉取动态 AES 密钥
 * - 密钥 24 小时过期，定时刷新
 * - 替换 [AesKeyManager] 中的临时本地生成逻辑
 *
 * 接口约定：
 * GET /api/security/key
 * Response: { code: 0, data: { key: "base64", iv: "base64", expireAt: 1234567890 } }
 */
object KeyService {

    private const val TAG = "KeyService"

    // TODO: 阶段四对接真实服务端，当前用占位 URL
    private const val KEY_API_URL = "https://api.zhuiju.app/api/security/key"

    /**
     * 从服务端拉取密钥
     *
     * @return [KeyResponse] 密钥数据，失败返回 null
     */
    suspend fun fetchKey(): KeyResponse? = withContext(Dispatchers.IO) {
        try {
            val responseJson = HttpUtils.get(KEY_API_URL)
            val response = HttpUtils.parseApiResponse<KeyResponse>(responseJson)

            if (response.isSuccess && response.data != null) {
                LogUtils.i("密钥拉取成功", TAG)
                response.data
            } else {
                LogUtils.w("密钥拉取业务失败: code=${response.code}, msg=${response.message}", TAG)
                null
            }
        } catch (e: Throwable) {
            LogUtils.e("密钥拉取网络异常: ${e.message}", TAG, e)
            null
        }
    }
}

/**
 * 密钥响应数据
 */
@Serializable
data class KeyResponse(
    val key: String,        // Base64 编码的 AES 密钥
    val iv: String,         // Base64 编码的 IV
    val expireAt: Long      // 过期时间戳（毫秒）
)
