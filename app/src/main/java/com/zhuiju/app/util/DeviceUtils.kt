package com.zhuiju.app.util

import android.app.ActivityManager
import android.os.Build
import com.zhuiju.app.ZhuiJuApp
import com.zhuiju.app.config.AppConstants

/**
 * 设备信息工具
 *
 * - 判断高低端机型（CPU 核心数 + 内存）
 * - 用于弹幕降级、动画降级、预加载策略等性能适配
 */
object DeviceUtils {

    /** CPU 核心数 */
    val cpuCores: Int by lazy {
        Runtime.getRuntime().availableProcessors()
    }

    /** 设备总内存（MB） */
    val totalMemMB: Long by lazy {
        val activityManager = ZhuiJuApp.instance.getSystemService(android.content.Context.ACTIVITY_SERVICE) as? ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        activityManager?.getMemoryInfo(memInfo)
        memInfo.totalMem / (1024 * 1024)
    }

    /**
     * 是否为低端机型
     *
     * 判断标准：CPU 核心数低于阈值 或 内存低于 3GB
     */
    val isLowEndDevice: Boolean by lazy {
        cpuCores < AppConstants.LOW_END_CPU_CORES_THRESHOLD || totalMemMB < 3072L
    }

    /**
     * 是否为高端机型（CPU 核心 >= 8 且内存 >= 6GB）
     */
    val isHighEndDevice: Boolean by lazy {
        cpuCores >= 8 && totalMemMB >= 6144L
    }

    /** Android API 版本 */
    val apiLevel: Int = Build.VERSION.SDK_INT

    /** 是否 Android 10+（分区存储） */
    val isScopedStorage: Boolean = apiLevel >= AppConstants.ANDROID_Q_API

    /** 是否 Android 12+（后台服务类型） */
    val isAndroidS: Boolean = apiLevel >= AppConstants.ANDROID_S_API
}
