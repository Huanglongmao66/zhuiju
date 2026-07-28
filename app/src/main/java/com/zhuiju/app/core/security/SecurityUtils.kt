package com.zhuiju.app.core.security

import com.zhuiju.app.config.AppConstants
import com.zhuiju.app.util.LogUtils
import java.security.MessageDigest

/**
 * 安全工具类
 *
 * - 请求签名（MD5 + 时间戳 + nonce），防篡改
 * - 字符串 MD5/SHA-256 摘要
 * - 本地缓存文件名加密（避免明文路径暴露）
 */
object SecurityUtils {

    private const val TAG = "SecurityUtils"

    /**
     * 生成请求签名
     *
     * 签名规则：MD5(url + timestamp + nonce + secretKey)
     *
     * @param url       请求 URL
     * @param timestamp 时间戳
     * @param nonce     随机数
     * @param secretKey 密钥
     * @return 32位 MD5 签名
     */
    fun sign(url: String, timestamp: Long, nonce: String, secretKey: String): String {
        val raw = "$url$timestamp$nonce$secretKey"
        return md5(raw)
    }

    /**
     * MD5 摘要
     */
    fun md5(input: String): String {
        return hash(input, "MD5")
    }

    /**
     * SHA-256 摘要
     */
    fun sha256(input: String): String {
        return hash(input, "SHA-256")
    }

    /**
     * 生成随机 nonce
     */
    fun generateNonce(): String {
        val random = java.security.SecureRandom()
        val bytes = ByteArray(16)
        random.nextBytes(bytes)
        return bytes.joinToString("") { "%02x".format(it) }
    }

    /**
     * 加密本地缓存文件名
     *
     * 将视频 ID 加密为文件名，避免明文暴露资源信息
     *
     * @param videoId 视频 ID
     * @return 加密后的文件名（不含扩展名）
     */
    fun encryptCacheFileName(videoId: String): String {
        return md5("zhuiju_cache_$videoId").substring(0, 16)
    }

    /**
     * 通用哈希
     */
    private fun hash(input: String, algorithm: String): String {
        return try {
            val md = MessageDigest.getInstance(algorithm)
            val digest = md.digest(input.toByteArray(Charsets.UTF_8))
            digest.joinToString("") { "%02x".format(it) }
        } catch (e: Throwable) {
            LogUtils.e("哈希计算失败: ${e.message}", TAG, e)
            ""
        }
    }
}
