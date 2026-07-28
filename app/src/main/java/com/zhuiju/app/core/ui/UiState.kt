package com.zhuiju.app.core.ui

/**
 * UI 状态密封类
 *
 * 统一封装页面状态，禁止零散变量标记状态
 * - [Loading] 加载中
 * - [Success] 成功，携带数据
 * - [Error]   失败，携带异常信息
 * - [Empty]   空数据
 */
sealed class UiState<out T> {
    data object Loading : UiState<Nothing>()
    data class Success<out T>(val data: T) : UiState<T>()
    data class Error(val message: String, val code: Int = -1, val throwable: Throwable? = null) : UiState<Nothing>()
    data object Empty : UiState<Nothing>()

    /** 是否加载中 */
    val isLoading: Boolean get() = this is Loading

    /** 是否成功 */
    val isSuccess: Boolean get() = this is Success

    /** 是否失败 */
    val isError: Boolean get() = this is Error

    /** 是否空数据 */
    val isEmpty: Boolean get() = this is Empty

    /** 成功时获取数据，否则 null */
    fun getDataOrNull(): T? = (this as? Success<T>)?.data
}

/**
 * 分页 UI 状态
 *
 * 在 [UiState] 基础上增加分页信息：是否还有更多、是否加载更多中
 */
data class PageUiState<T>(
    val state: UiState<List<T>> = UiState.Loading,
    val isRefresh: Boolean = true,
    val hasMore: Boolean = true,
    val isLoadMore: Boolean = false
)
