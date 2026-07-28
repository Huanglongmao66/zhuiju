package com.zhuiju.app.util

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import com.zhuiju.app.ZhuiJuApp

/**
 * 统一吐司工具
 *
 * - 单例 Toast 复用，避免连续弹出多个吐司
 * - 支持子线程调用（内部切回主线程）
 * - 统一通过本工具调用，禁止业务代码直接 new Toast
 */
object ToastUtils {

    private val mainHandler = Handler(Looper.getMainLooper())
    private var toast: Toast? = null

    fun show(msg: String, duration: Int = Toast.LENGTH_SHORT) {
        postMainThread {
            cancel()
            toast = Toast.makeText(ZhuiJuApp.instance, msg, duration).apply { show() }
        }
    }

    fun show(resId: Int, duration: Int = Toast.LENGTH_SHORT) {
        show(ZhuiJuApp.instance.getString(resId), duration)
    }

    fun showLong(msg: String) {
        show(msg, Toast.LENGTH_LONG)
    }

    fun showLong(resId: Int) {
        show(resId, Toast.LENGTH_LONG)
    }

    fun cancel() {
        toast?.cancel()
        toast = null
    }

    private fun postMainThread(action: () -> Unit) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            action()
        } else {
            mainHandler.post(action)
        }
    }
}
