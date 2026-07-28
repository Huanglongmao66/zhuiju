package com.zhuiju.app.core.security

import com.zhuiju.app.config.AppConstants
import com.zhuiju.app.util.LogUtils
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec
import android.util.Base64

/**
 * AES-128-CBC 加密工具
 *
 * - 严格使用 AES/CBC/PKCS5Padding 模式，禁止 ECB
 * - 密钥与 IV 均 16 字节
 * - 提供字符串、字节数组、分片三种加解密接口
 * - 加解密操作必须放在子线程，禁止主线程调用
 *
 * 安全要点：
 * 1. 密钥由 [AesKeyManager] 动态下发，禁止硬编码
 * 2. 视频分片采用 [encryptSlice] / [decryptSlice]，支持边解边播
 * 3. 解密后数据仅驻留内存，禁止落地明文文件
 */
object AesUtils {

    private const val TAG = "AesUtils"

    /**
     * 字符串加密
     *
     * @param plainText 明文
     * @param key       密钥（16字节）
     * @param iv        IV偏移量（16字节）
     * @return Base64 编码的密文
     */
    fun encryptString(plainText: String, key: ByteArray, iv: ByteArray): String {
        checkKeyAndIv(key, iv)
        val cipher = getCipher(Cipher.ENCRYPT_MODE, key, iv)
        val encrypted = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(encrypted, Base64.NO_WRAP)
    }

    /**
     * 字符串解密
     *
     * @param cipherTextBase64 Base64 编码的密文
     * @param key              密钥（16字节）
     * @param iv               IV偏移量（16字节）
     * @return 明文字符串
     */
    fun decryptString(cipherTextBase64: String, key: ByteArray, iv: ByteArray): String {
        checkKeyAndIv(key, iv)
        val cipher = getCipher(Cipher.DECRYPT_MODE, key, iv)
        val encrypted = Base64.decode(cipherTextBase64, Base64.NO_WRAP)
        val decrypted = cipher.doFinal(encrypted)
        return String(decrypted, Charsets.UTF_8)
    }

    /**
     * 字节数组加密
     */
    fun encrypt(data: ByteArray, key: ByteArray, iv: ByteArray): ByteArray {
        checkKeyAndIv(key, iv)
        val cipher = getCipher(Cipher.ENCRYPT_MODE, key, iv)
        return cipher.doFinal(data)
    }

    /**
     * 字节数组解密
     */
    fun decrypt(data: ByteArray, key: ByteArray, iv: ByteArray): ByteArray {
        checkKeyAndIv(key, iv)
        val cipher = getCipher(Cipher.DECRYPT_MODE, key, iv)
        return cipher.doFinal(data)
    }

    /**
     * 视频分片加密（边播边解场景）
     *
     * - 分片大小必须为 [AppConstants.AES_BLOCK_SIZE] 的整数倍
     * - 自动对齐分片大小
     *
     * @param slice 原始分片数据
     * @param key   密钥
     * @param iv    IV偏移量
     * @return 加密后的分片数据
     */
    fun encryptSlice(slice: ByteArray, key: ByteArray, iv: ByteArray): ByteArray {
        checkKeyAndIv(key, iv)
        // 分片对齐：不足 16 字节整数倍时补齐
        val alignedSize = alignToBlockSize(slice.size)
        val alignedData = if (slice.size == alignedSize) {
            slice
        } else {
            slice.copyOf(alignedSize)
        }
        val cipher = getCipher(Cipher.ENCRYPT_MODE, key, iv)
        return cipher.doFinal(alignedData)
    }

    /**
     * 视频分片解密（边播边解场景）
     *
     * - 解密后数据仅驻留内存，禁止落地
     * - 失败时抛出异常，由调用方统一处理
     *
     * @param encryptedSlice 加密的分片数据
     * @param key            密钥
     * @param iv             IV偏移量
     * @return 解密后的分片数据
     */
    fun decryptSlice(encryptedSlice: ByteArray, key: ByteArray, iv: ByteArray): ByteArray {
        checkKeyAndIv(key, iv)
        // 加密分片必须为 16 字节整数倍
        if (encryptedSlice.size % AppConstants.AES_BLOCK_SIZE != 0) {
            throw IllegalArgumentException("加密分片大小不是 ${AppConstants.AES_BLOCK_SIZE} 的整数倍")
        }
        val cipher = getCipher(Cipher.DECRYPT_MODE, key, iv)
        return cipher.doFinal(encryptedSlice)
    }

    /**
     * 生成随机密钥（仅用于本地测试，生产环境由服务端下发）
     */
    fun generateRandomKey(): ByteArray {
        val key = ByteArray(AppConstants.AES_KEY_LENGTH)
        java.security.SecureRandom().nextBytes(key)
        return key
    }

    /**
     * 生成随机 IV（仅用于本地测试）
     */
    fun generateRandomIv(): ByteArray {
        val iv = ByteArray(AppConstants.AES_IV_LENGTH)
        java.security.SecureRandom().nextBytes(iv)
        return iv
    }

    /**
     * 获取 Cipher 实例
     */
    private fun getCipher(mode: Int, key: ByteArray, iv: ByteArray): Cipher {
        val cipher = Cipher.getInstance(AppConstants.AES_MODE)
        val secretKey = SecretKeySpec(key, "AES")
        val ivSpec = IvParameterSpec(iv)
        cipher.init(mode, secretKey, ivSpec)
        return cipher
    }

    /**
     * 校验密钥与 IV 长度
     */
    private fun checkKeyAndIv(key: ByteArray, iv: ByteArray) {
        if (key.size != AppConstants.AES_KEY_LENGTH) {
            throw IllegalArgumentException("AES 密钥长度必须为 ${AppConstants.AES_KEY_LENGTH} 字节，当前 ${key.size}")
        }
        if (iv.size != AppConstants.AES_IV_LENGTH) {
            throw IllegalArgumentException("AES IV 长度必须为 ${AppConstants.AES_IV_LENGTH} 字节，当前 ${iv.size}")
        }
    }

    /**
     * 将大小对齐到 AES 块大小（16 字节）
     */
    private fun alignToBlockSize(size: Int): Int {
        val blockSize = AppConstants.AES_BLOCK_SIZE
        return if (size % blockSize == 0) size else (size / blockSize + 1) * blockSize
    }
}
