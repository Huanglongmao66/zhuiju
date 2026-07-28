package com.zhuiju.app.util

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

/**
 * 协程调度器统一管理
 *
 * - 全局统一调度器入口，禁止业务代码直接使用 Dispatchers
 * - 便于后续替换/测试/监控
 * - 遵循项目规范：禁止原生 Thread/Handler，强制使用协程
 */
object AppExecutors {

    /** 主线程（UI 操作） */
    val main: CoroutineDispatcher = Dispatchers.Main

    /** IO 线程（网络、磁盘、FFmpeg、加解密） */
    val io: CoroutineDispatcher = Dispatchers.IO

    /** 默认计算线程（CPU 密集型） */
    val default: CoroutineDispatcher = Dispatchers.Default

    /** 低并发 IO 调度器（用于 FFmpeg 等重型任务，避免占满 IO 池） */
    val heavyIo: CoroutineDispatcher = Dispatchers.IO.limitedParallelism(2)
}
