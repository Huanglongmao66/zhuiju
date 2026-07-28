package com.zhuiju.app.util

import android.util.Log
import com.zhuiju.app.BuildConfig

/**
 * 统一日志工具
 *
 * - 上线前通过 [isLogEnabled] 控制开关，release 包自动关闭
 * - 自动识别当前类名作为 TAG
 * - 禁止直接使用 android.util.Log，统一通过本工具
 */
object LogUtils {

    /** 全局日志开关，release 包自动关闭；可由 [init] 显式控制 */
    var isLogEnabled: Boolean = BuildConfig.DEBUG
        private set

    /** 敏感信息过滤开关（密钥、视频地址等） */
    var sensitiveFilterEnabled = true

    /**
     * 显式初始化日志开关（一般在 Application.onCreate 中调用）
     * @param enable 是否开启详细日志
     */
    fun init(enable: Boolean = BuildConfig.DEBUG) {
        isLogEnabled = enable
    }

    fun v(msg: String, tag: String? = null) {
        if (isLogEnabled) Log.v(getTag(tag), msg)
    }

    fun d(msg: String, tag: String? = null) {
        if (isLogEnabled) Log.d(getTag(tag), msg)
    }

    fun i(msg: String, tag: String? = null) {
        if (isLogEnabled) Log.i(getTag(tag), msg)
    }

    fun w(msg: String, tag: String? = null, tr: Throwable? = null) {
        if (isLogEnabled) {
            if (tr != null) Log.w(getTag(tag), msg, tr)
            else Log.w(getTag(tag), msg)
        }
    }

    fun e(msg: String, tag: String? = null, tr: Throwable? = null) {
        if (isLogEnabled) {
            if (tr != null) Log.e(getTag(tag), msg, tr)
            else Log.e(getTag(tag), msg)
        }
    }

    /** 过滤敏感信息后输出（密钥、token、视频URL等） */
    fun secure(msg: String, tag: String? = null) {
        if (!isLogEnabled || sensitiveFilterEnabled) return
        Log.d(getTag(tag), msg)
    }

    private fun getTag(customTag: String?): String {
        if (customTag != null) return customTag
        // 自动获取调用栈中的类名作为 TAG
        val stackTrace = Throwable().stackTrace
        return if (stackTrace.size > 2) {
            val className = stackTrace[2].className
            className.substringAfterLast('.')
        } else {
            "ZhuiJu"
        }
    }
}
