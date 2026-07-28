package com.zhuiju.app.core.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import com.zhuiju.app.ZhuiJuApp
import com.zhuiju.app.util.LogUtils
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

/**
 * 网络状态监听器
 *
 * - 基于 ConnectivityManager 监听网络变化
 * - 提供 [networkState] Flow，供全局订阅
 * - 监听：无网络、WiFi、流量、弱网切换
 *
 * 使用方式：
 * ```
 * networkMonitor.networkState.collect { state ->
 *     when (state) { ... }
 * }
 * ```
 */
object NetworkMonitor {

    private const val TAG = "NetworkMonitor"

    /** 网络状态密封类 */
    sealed class NetworkState {
        data object Unavailable : NetworkState()
        data object Available : NetworkState()
        data class Wifi(val isWeak: Boolean = false) : NetworkState()
        data class Cellular(val isWeak: Boolean = false) : NetworkState()
    }

    private val connectivityManager: ConnectivityManager by lazy {
        ZhuiJuApp.instance.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    }

    /**
     * 网络状态 Flow
     */
    val networkState: Flow<NetworkState> = callbackFlow {
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                LogUtils.i("网络可用", TAG)
                trySend(NetworkState.Available)
            }

            override fun onLost(network: Network) {
                LogUtils.w("网络丢失", TAG)
                trySend(NetworkState.Unavailable)
            }

            override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
                val hasInternet = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                val isWifi = capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
                val isCellular = capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
                val isWeak = !capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)

                when {
                    !hasInternet -> trySend(NetworkState.Unavailable)
                    isWifi -> trySend(NetworkState.Wifi(isWeak))
                    isCellular -> trySend(NetworkState.Cellular(isWeak))
                }
            }
        }

        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        connectivityManager.registerNetworkCallback(request, callback)

        // 初始状态
        val activeNetwork = connectivityManager.activeNetwork
        val initialState = if (activeNetwork == null) {
            NetworkState.Unavailable
        } else {
            val caps = connectivityManager.getNetworkCapabilities(activeNetwork)
            when {
                caps == null -> NetworkState.Unavailable
                caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> NetworkState.Wifi()
                caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> NetworkState.Cellular()
                else -> NetworkState.Available
            }
        }
        trySend(initialState)

        awaitClose {
            connectivityManager.unregisterNetworkCallback(callback)
        }
    }.distinctUntilChanged()

    /**
     * 当前是否联网
     */
    fun isOnline(): Boolean {
        val network = connectivityManager.activeNetwork ?: return false
        val caps = connectivityManager.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    /**
     * 当前是否 WiFi
     */
    fun isWifi(): Boolean {
        val network = connectivityManager.activeNetwork ?: return false
        val caps = connectivityManager.getNetworkCapabilities(network) ?: return false
        return caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
    }
}
