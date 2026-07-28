package com.zhuiju.app.util

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.result.ActivityResultCaller
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

/**
 * 权限统一管理工具
 *
 * - 封装运行时权限申请，使用 ActivityResult API
 * - 支持单/多权限申请
 * - 统一权限检查、申请、结果回调
 *
 * 使用方式：
 * ```
 * val helper = PermissionHelper(activity)
 * helper.requestPermissions(Manifest.permission.WRITE_EXTERNAL_STORAGE) { granted ->
 *     if (granted) { /* 授权成功 */ }
 * }
 * ```
 */
class PermissionHelper(private val caller: ActivityResultCaller) {

    private val launcher = caller.registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        val allGranted = result.values.all { it }
        pendingCallback?.invoke(allGranted, result)
        pendingCallback = null
    }

    private var pendingCallback: ((allGranted: Boolean, result: Map<String, Boolean>) -> Unit)? = null

    /**
     * 检查权限是否已授予
     */
    fun hasPermission(context: Context, permission: String): Boolean {
        return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * 检查多个权限是否全部已授予
     */
    fun hasAllPermissions(context: Context, permissions: Array<String>): Boolean {
        return permissions.all { hasPermission(context, it) }
    }

    /**
     * 申请单个权限
     */
    fun requestPermission(
        permission: String,
        callback: (granted: Boolean) -> Unit
    ) {
        requestPermissions(arrayOf(permission)) { allGranted, _ ->
            callback(allGranted)
        }
    }

    /**
     * 申请多个权限
     */
    fun requestPermissions(
        permissions: Array<String>,
        callback: (allGranted: Boolean, result: Map<String, Boolean>) -> Unit
    ) {
        if (pendingCallback != null) {
            LogUtils.w("已有权限申请进行中，忽略新请求")
            return
        }
        pendingCallback = callback
        launcher.launch(permissions)
    }

    companion object {
        /**
         * 快捷检查 Android 10+ 分区存储权限
         */
        fun needScopedStorage(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q

        /**
         * 快捷检查 Android 12+ 后台服务权限
         */
        fun needForegroundServiceType(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    }
}
