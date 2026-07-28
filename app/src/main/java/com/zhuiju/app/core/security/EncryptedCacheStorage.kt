package com.zhuiju.app.core.security

import com.zhuiju.app.config.AppConstants
import com.zhuiju.app.core.network.NetworkManager
import com.zhuiju.app.util.AppExecutors
import com.zhuiju.app.util.LogUtils
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

/**
 * 本地缓存文件加密存储
 *
 * - 视频缓存文件二次 AES 加密存储，防止本地扒资源
 * - 写入时加密，读取时解密，全程内存操作
 * - 禁止解密后明文落地
 */
object EncryptedCacheStorage {

    private const val TAG = "EncryptedCacheStorage"

    /**
     * 加密写入缓存文件
     *
     * @param sourceFile 源文件
     * @param targetFile 目标加密文件
     * @param key        AES 密钥
     * @param iv         IV 偏移量
     */
    suspend fun encryptAndSave(
        sourceFile: File,
        targetFile: File,
        key: ByteArray,
        iv: ByteArray
    ): Boolean = withContext(AppExecutors.heavyIo) {
        try {
            targetFile.parentFile?.mkdirs()
            val bytes = FileInputStream(sourceFile).use { it.readBytes() }
            val encrypted = AesUtils.encrypt(bytes, key, iv)
            FileOutputStream(targetFile).use { it.write(encrypted) }
            LogUtils.i("缓存加密保存成功: ${targetFile.name}", TAG)
            true
        } catch (e: Throwable) {
            LogUtils.e("缓存加密保存失败: ${e.message}", TAG, e)
            false
        }
    }

    /**
     * 解密读取缓存文件
     *
     * @param encryptedFile 加密文件
     * @param key           AES 密钥
     * @param iv            IV 偏移量
     * @return 解密后的字节数组（仅驻留内存，禁止落地）
     */
    suspend fun decrypt(
        encryptedFile: File,
        key: ByteArray,
        iv: ByteArray
    ): ByteArray? = withContext(AppExecutors.heavyIo) {
        try {
            val encrypted = FileInputStream(encryptedFile).use { it.readBytes() }
            val decrypted = AesUtils.decrypt(encrypted, key, iv)
            LogUtils.i("缓存解密读取成功: ${encryptedFile.name}", TAG)
            decrypted
        } catch (e: Throwable) {
            LogUtils.e("缓存解密读取失败: ${e.message}", TAG, e)
            null
        }
    }

    /**
     * 分片加密写入（边下载边加密）
     *
     * @param sliceData  分片数据
     * @param targetFile 目标文件
     * @param key        AES 密钥
     * @param iv         IV 偏移量
     * @param offset     写入偏移量
     */
    suspend fun encryptSliceAndAppend(
        sliceData: ByteArray,
        targetFile: File,
        key: ByteArray,
        iv: ByteArray,
        offset: Long = 0
    ): Boolean = withContext(AppExecutors.heavyIo) {
        try {
            val encrypted = AesUtils.encryptSlice(sliceData, key, iv)
            FileOutputStream(targetFile, true).use {
                it.write(encrypted)
            }
            LogUtils.d("分片加密追加: ${targetFile.name}, offset=$offset", TAG)
            true
        } catch (e: Throwable) {
            LogUtils.e("分片加密追加失败: ${e.message}", TAG, e)
            false
        }
    }
}
