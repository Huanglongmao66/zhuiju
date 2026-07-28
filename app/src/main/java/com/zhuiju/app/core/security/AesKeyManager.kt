package com.zhuiju.app.core.security

import android.content.Context
import com.zhuiju.app.ZhuiJuApp
import com.zhuiju.app.config.AppConstants
import com.zhuiju.app.util.LogUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * AES 密钥管理器
 *
 * - 服务端动态下发密钥，24 小时自动过期更新
 * - 本地加密缓存（EncryptedSharedPreferences，后续阶段接入）
 * - 提供 [currentKey] / [currentIv] 供播放器分片解密使用
 * - 密钥拉取失败时使用兜底策略（重试 3 次）
 * - 禁止硬编码密钥
 *
 * 初始化：在 Application 中调用 [AesKeyManager.init]
 */
object AesKeyManager {

    private const val TAG = "AesKeyManager"

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val keyMutex = Mutex()

    /** 当前密钥状态 */
    sealed class KeyState {
        data object Loading : KeyState()
        data class Ready(val key: ByteArray, val iv: ByteArray, val expireAt: Long) : KeyState()
        data class Error(val message: String) : KeyState()
    }

    private val _keyState = MutableStateFlow<KeyState>(KeyState.Loading)
    val keyState: StateFlow<KeyState> = _keyState.asStateFlow()

    /** 密钥过期定时任务 */
    private var refreshJob: Job? = null

    /**
     * 初始化密钥管理器
     *
     * - 优先读取本地缓存密钥
     * - 若无缓存或已过期，从服务端拉取
     */
    fun init(context: Context) {
        LogUtils.i("AesKeyManager 初始化", TAG)
        fetchKeyFromServer()
    }

    /**
     * 从服务端拉取密钥
     *
     * - 失败自动重试 [AppConstants.PLAYER_MAX_RETRY] 次
     * - 成功后启动定时刷新任务
     */
    private fun fetchKeyFromServer() {
        scope.launch {
            keyMutex.withLock {
                _keyState.value = KeyState.Loading
                var retryCount = 0
                while (retryCount < AppConstants.PLAYER_MAX_RETRY) {
                    try {
                        // TODO: 阶段四对接真实服务端接口
                        // val response = HttpUtils.get("${BuildConfig.BASE_URL}/api/security/key")
                        // val keyData = HttpUtils.parseApiResponse<KeyResponse>(response).data

                        // 阶段一：临时使用本地生成的密钥（仅开发调试，生产环境必须服务端下发）
                        LogUtils.w("临时使用本地密钥，生产环境必须服务端下发", TAG)
                        val key = AesUtils.generateRandomKey()
                        val iv = AesUtils.generateRandomIv()
                        val expireAt = System.currentTimeMillis() + AppConstants.AES_KEY_EXPIRE_MS

                        _keyState.value = KeyState.Ready(key, iv, expireAt)
                        LogUtils.i("密钥拉取成功，过期时间: $expireAt", TAG)

                        scheduleRefresh(expireAt)
                        return@withLock
                    } catch (e: Throwable) {
                        retryCount++
                        LogUtils.e("密钥拉取失败(第 $retryCount 次): ${e.message}", TAG, e)
                        delay(AppConstants.NET_RETRY_INTERVAL_MS)
                    }
                }

                _keyState.value = KeyState.Error("密钥拉取失败，已重试 $retryCount 次")
                LogUtils.e("密钥拉取失败，已达最大重试次数", TAG)
            }
        }
    }

    /**
     * 定时刷新密钥（过期前提前刷新）
     */
    private fun scheduleRefresh(expireAt: Long) {
        refreshJob?.cancel()
        // 过期前 1 小时刷新
        val delayMs = (expireAt - System.currentTimeMillis()) - 60 * 60 * 1000L
        if (delayMs <= 0) {
            fetchKeyFromServer()
            return
        }
        refreshJob = scope.launch {
            delay(delayMs)
            LogUtils.i("密钥即将过期，开始刷新", TAG)
            fetchKeyFromServer()
        }
    }

    /**
     * 获取当前密钥（阻塞直到密钥就绪）
     *
     * 供播放器分片解密使用，密钥未就绪时返回 null
     */
    fun getCurrentKey(): KeyState = _keyState.value

    /**
     * 强制刷新密钥
     */
    fun forceRefresh() {
        LogUtils.i("手动触发密钥刷新", TAG)
        fetchKeyFromServer()
    }
}
