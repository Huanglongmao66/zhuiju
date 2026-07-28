package com.zhuiju.app.util

import android.content.Context
import android.os.Build
import android.os.Looper
import android.widget.Toast
import com.zhuiju.app.ZhuiJuApp
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlin.system.exitProcess

/**
 * 全局异常捕获处理器
 *
 * - 捕获主线程未处理异常，防止白屏/ANR
 * - 捕获子线程未处理异常，记录日志
 * - 提供协程异常处理器 [coroutineExceptionHandler]
 * - 异常时弹出友好提示，避免生硬崩溃
 *
 * 使用方式：[CrashHandler.init] 在 Application 中初始化
 */
object CrashHandler {

    private const val CRASH_TAG = "CrashHandler"
    private var initialized = false

    /**
     * 协程全局异常处理器，配合协程作用域使用
     */
    val coroutineExceptionHandler = CoroutineExceptionHandler { _, throwable ->
        LogUtils.e("协程异常: ${throwable.message}", CRASH_TAG, throwable)
    }

    /**
     * 初始化全局异常捕获
     */
    fun init(context: Context) {
        if (initialized) return
        initialized = true

        // 主线程异常捕获
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            LogUtils.e("未捕获异常 thread=${thread.name}", CRASH_TAG, throwable)
            handleCrash(context, throwable)
            defaultHandler?.uncaughtException(thread, throwable)
        }

        LogUtils.i("CrashHandler 初始化完成", CRASH_TAG)
    }

    /**
     * 处理崩溃：主线程弹提示，子线程记录日志
     */
    private fun handleCrash(context: Context, throwable: Throwable) {
        if (Thread.currentThread() == Looper.getMainLooper().thread) {
            // 主线程异常，弹吐司提示
            Toast.makeText(context, "应用出现异常，正在恢复...", Toast.LENGTH_SHORT).show()
        }
        // 记录设备信息便于排查
        LogUtils.e(buildCrashReport(throwable), CRASH_TAG)
    }

    /**
     * 构建崩溃报告（设备信息+异常堆栈）
     */
    private fun buildCrashReport(throwable: Throwable): String {
        return buildString {
            appendLine("=== 追剧APP 崩溃报告 ===")
            appendLine("时间: ${System.currentTimeMillis()}")
            appendLine("设备: ${Build.MANUFACTURER} ${Build.MODEL}")
            appendLine("系统: Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
            appendLine("应用版本: ${com.zhuiju.app.BuildConfig.VERSION_NAME} (${com.zhuiju.app.BuildConfig.VERSION_CODE})")
            appendLine("异常: ${throwable.javaClass.name}: ${throwable.message}")
            appendLine("堆栈:")
            throwable.stackTrace.take(20).forEach { appendLine("    at $it") }
            appendLine("========================")
        }
    }
}
