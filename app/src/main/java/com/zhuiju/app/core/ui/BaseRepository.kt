package com.zhuiju.app.core.ui

import com.zhuiju.app.util.LogUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Repository 基类
 *
 * - 统一管理网络、缓存、本地数据，是唯一数据源
 * - View/ViewModel 禁止直接调用 OkHttp、文件读写
 * - 封装网络请求调度与异常转换
 */
abstract class BaseRepository {

    private val TAG = this.javaClass.simpleName

    /**
     * 在 IO 线程执行网络/磁盘请求，统一异常捕获
     *
     * @param block IO 操作
     * @return Result<T> 成功或失败
     */
    protected suspend fun <T> safeCall(block: suspend () -> T): Result<T> = withContext(Dispatchers.IO) {
        runCatching {
            block()
        }.onFailure { e ->
            LogUtils.e("Repository 请求失败: ${e.message}", TAG, e)
        }
    }

    /**
     * 在 IO 线程执行请求，返回 [UiState]
     *
     * @param block IO 操作
     * @return UiState<T> 状态封装
     */
    protected suspend fun <T> safeCallUiState(block: suspend () -> T): UiState<T> = withContext(Dispatchers.IO) {
        try {
            val data = block()
            if (data == null || (data is Collection<*> && data.isEmpty())) {
                UiState.Empty
            } else {
                UiState.Success(data)
            }
        } catch (e: Throwable) {
            LogUtils.e("Repository 请求失败: ${e.message}", TAG, e)
            UiState.Error(e.message ?: "网络异常", throwable = e)
        }
    }
}
