package com.zhuiju.app.core.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zhuiju.app.util.CrashHandler
import com.zhuiju.app.util.LogUtils
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel 基类
 *
 * - 封装状态管理（StateFlow / SharedFlow）
 * - 统一协程作用域与异常处理
 * - 不持有 View 引用，不操作控件
 * - 通过 [UiState] 分发页面状态
 */
abstract class BaseViewModel : ViewModel() {

    private val TAG = this.javaClass.simpleName

    /** 协程异常处理器 */
    private val exceptionHandler = CrashHandler.coroutineExceptionHandler

    /**
     * 全局事件 SharedFlow（一次性事件：Toast、跳转、弹窗等）
     */
    private val _commonEvent = MutableSharedFlow<CommonEvent>()
    val commonEvent: SharedFlow<CommonEvent> = _commonEvent.asSharedFlow()

    /**
     * 启动协程（IO 线程执行），自动异常捕获
     *
     * @param block IO 操作
     * @param onSuccess 成功回调（主线程）
     * @param onError 失败回调（主线程）
     */
    protected fun <T> launchIO(
        block: suspend CoroutineScope.() -> T,
        onSuccess: (T) -> Unit = {},
        onError: (Throwable) -> Unit = { handleError(it) }
    ) {
        viewModelScope.launch(exceptionHandler + Dispatchers.IO) {
            try {
                val result = block()
                launch(Dispatchers.Main) { onSuccess(result) }
            } catch (e: Throwable) {
                LogUtils.e("协程执行失败", TAG, e)
                launch(Dispatchers.Main) { onError(e) }
            }
        }
    }

    /**
     * 启动协程（主线程执行）
     */
    protected fun launchMain(
        block: suspend CoroutineScope.() -> Unit,
        onError: (Throwable) -> Unit = { handleError(it) }
    ) {
        viewModelScope.launch(exceptionHandler + Dispatchers.Main) {
            try {
                block()
            } catch (e: Throwable) {
                LogUtils.e("协程执行失败", TAG, e)
                onError(e)
            }
        }
    }

    /**
     * 收集 Flow 数据（主线程处理）
     */
    protected fun <T> collectFlow(
        flow: Flow<T>,
        collector: suspend (T) -> Unit
    ) {
        viewModelScope.launch(exceptionHandler + Dispatchers.Main) {
            flow.collect { collector(it) }
        }
    }

    /**
     * 发送全局事件（Toast、跳转等）
     */
    protected fun sendEvent(event: CommonEvent) {
        viewModelScope.launch { _commonEvent.emit(event) }
    }

    /**
     * 统一错误处理
     */
    protected open fun handleError(throwable: Throwable) {
        LogUtils.e("ViewModel 错误: ${throwable.message}", TAG, throwable)
        sendEvent(CommonEvent.Toast(throwable.message ?: "未知错误"))
    }

    override fun onCleared() {
        super.onCleared()
        LogUtils.i("ViewModel onCleared: $TAG", TAG)
    }
}

/**
 * 全局通用事件（一次性消费）
 */
sealed class CommonEvent {
    /** 显示 Toast */
    data class Toast(val message: String, val duration: Int = 0) : CommonEvent()

    /** 跳转页面 */
    data class Navigate(val route: String, val args: Map<String, Any?> = emptyMap()) : CommonEvent()

    /** 关闭当前页面 */
    data object Finish : CommonEvent()

    /** 显示加载框 */
    data class ShowLoading(val message: String = "加载中...") : CommonEvent()

    /** 隐藏加载框 */
    data object HideLoading : CommonEvent()
}
