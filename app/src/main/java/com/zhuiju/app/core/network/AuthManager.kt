package com.zhuiju.app.core.network

import android.content.Context
import android.content.SharedPreferences
import com.zhuiju.app.ZhuiJuApp

/**
 * 鉴权管理器
 *
 * - 管理 Token 的存取（登录态）
 * - 当前阶段基于 SharedPreferences，阶段四升级为 EncryptedSharedPreferences
 * - 提供 Token 状态查询
 */
object AuthManager {

    private const val PREFS_NAME = "auth_prefs"
    private const val KEY_TOKEN = "key_token"
    private const val KEY_USER_ID = "key_user_id"

    private val prefs: SharedPreferences by lazy {
        ZhuiJuApp.instance.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    /** 获取当前 Token */
    fun getToken(): String? = prefs.getString(KEY_TOKEN, null)

    /** 保存 Token（登录成功后调用） */
    fun saveToken(token: String, userId: String? = null) {
        prefs.edit().apply {
            putString(KEY_TOKEN, token)
            if (userId != null) putString(KEY_USER_ID, userId)
        }.apply()
    }

    /** 清除 Token（退出登录或 401 时调用） */
    fun clearToken() {
        prefs.edit().clear().apply()
    }

    /** 是否已登录 */
    fun isLoggedIn(): Boolean = !getToken().isNullOrEmpty()

    /** 获取用户 ID */
    fun getUserId(): String? = prefs.getString(KEY_USER_ID, null)
}
