package com.zhuiju.app.core.ffmpeg

import com.arthenica.mobileffmpeg.Config
import com.arthenica.mobileffmpeg.ExecuteCallback
import com.arthenica.mobileffmpeg.FFmpeg
import com.zhuiju.app.util.AppExecutors
import com.zhuiju.app.util.LogUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File
import kotlin.coroutines.resume

/**
 * FFmpeg 辅助工具管理器
 *
 * - 不替换 ExoPlayer 内核，仅做辅助能力补充
 * - 所有 FFmpeg 命令放在 IO 协程子线程，严禁主线程执行，杜绝 ANR
 * - 每次执行完毕必须释放 native 资源、销毁会话
 * - 命令统一封装，禁止业务代码拼接命令
 *
 * 能力：
 * 1. 异常视频格式兼容播放（转码为标准 MP4）
 * 2. 视频帧截图、封面提取
 * 3. 短视频裁剪、片段截取
 * 4. 格式转换
 */
object FFmpegManager {

    private const val TAG = "FFmpegManager"

    /** FFmpeg 执行超时时间（毫秒） */
    private const val EXECUTE_TIMEOUT_MS = 60_000L

    /** 初始化 FFmpeg 环境 */
    fun init() {
        Config.setLogLevel(Config.RETURN_CODE_SUCCESS.let {
            com.arthenica.mobileffmpeg.Config.LOG_LEVEL_ERROR
        })
        LogUtils.i("FFmpegManager 初始化完成", TAG)
    }

    /**
     * 执行 FFmpeg 命令（挂起函数，子线程执行）
     *
     * @param commands FFmpeg 命令数组（如 arrayOf("-i", "input.mp4", "output.mp4")）
     * @return [FFmpegResult] 执行结果
     */
    suspend fun execute(vararg commands: String): FFmpegResult = withContext(AppExecutors.heavyIo) {
        LogUtils.i("执行 FFmpeg 命令: ${commands.joinToString(" ")}", TAG)

        val result = withTimeoutOrNull(EXECUTE_TIMEOUT_MS) {
            suspendCancellableCoroutine<FFmpegResult> { cont ->
                val sessionId = FFmpeg.executeAsync(commands, ExecuteCallback { session ->
                    val returnCode = session.returnCode
                    LogUtils.i("FFmpeg 执行完成 returnCode=$returnCode", TAG)

                    val result = when (returnCode.value) {
                        Config.RETURN_CODE_SUCCESS -> FFmpegResult.Success
                        Config.RETURN_CODE_CANCEL -> FFmpegResult.Cancelled
                        else -> FFmpegResult.Failed(returnCode.value, session.failStackTrace)
                    }

                    // 释放会话资源
                    com.arthenica.mobileffmpeg.FFmpeg.cancel(session.sessionId)
                    if (cont.isActive) cont.resume(result)
                })

                cont.invokeOnCancellation {
                    com.arthenica.mobileffmpeg.FFmpeg.cancel(sessionId)
                }
            }
        }

        result ?: FFmpegResult.Failed(-1, "FFmpeg 执行超时")
    }

    /**
     * 提取视频帧（封面截图）
     *
     * @param videoPath  视频路径
     * @param outputPath 输出图片路径
     * @param timeMs     截图时间点（毫秒）
     * @return [FFmpegResult]
     */
    suspend fun extractFrame(
        videoPath: String,
        outputPath: String,
        timeMs: Long = 1000
    ): FFmpegResult = withContext(AppExecutors.heavyIo) {
        // 确保输出目录存在
        File(outputPath).parentFile?.mkdirs()
        val timeSec = timeMs / 1000.0
        execute("-y", "-i", videoPath, "-ss", timeSec.toString(), "-vframes", "1", "-q:v", "2", outputPath)
    }

    /**
     * 裁剪视频片段
     *
     * @param videoPath  视频路径
     * @param outputPath 输出路径
     * @param startMs    起始时间（毫秒）
     * @param durationMs 时长（毫秒）
     * @return [FFmpegResult]
     */
    suspend fun clipVideo(
        videoPath: String,
        outputPath: String,
        startMs: Long,
        durationMs: Long
    ): FFmpegResult = withContext(AppExecutors.heavyIo) {
        File(outputPath).parentFile?.mkdirs()
        val startSec = startMs / 1000.0
        val durationSec = durationMs / 1000.0
        execute("-y", "-i", videoPath, "-ss", startSec.toString(), "-t", durationSec.toString(),
            "-c", "copy", outputPath)
    }

    /**
     * 转码视频为标准 MP4（异常格式兼容）
     *
     * @param inputPath  原始视频路径
     * @param outputPath 输出 MP4 路径
     * @return [FFmpegResult]
     */
    suspend fun transcodeToMp4(
        inputPath: String,
        outputPath: String
    ): FFmpegResult = withContext(AppExecutors.heavyIo) {
        File(outputPath).parentFile?.mkdirs()
        execute("-y", "-i", inputPath, "-c:v", "libx264", "-c:a", "aac", "-preset", "fast",
            "-movflags", "+faststart", outputPath)
    }

    /**
     * 获取视频信息（时长、分辨率、码率）
     *
     * @param videoPath 视频路径
     * @return [VideoInfo] 视频信息
     */
    suspend fun getVideoInfo(videoPath: String): VideoInfo? = withContext(AppExecutors.heavyIo) {
        val info = com.arthenica.mobileffmpeg.FFprobe.getMediaInformation(videoPath)
        if (info == null) {
            LogUtils.w("无法获取视频信息: $videoPath", TAG)
            return@withContext null
        }

        val duration = info.duration?.toLongOrNull()?.times(1000) ?: 0L
        val width = info.streams?.firstOrNull { it.type == "video" }?.width ?: 0
        val height = info.streams?.firstOrNull { it.type == "video" }?.height ?: 0
        val bitrate = info.bitrate ?: 0

        VideoInfo(
            path = videoPath,
            durationMs = duration,
            width = width,
            height = height,
            bitrate = bitrate
        )
    }
}

/**
 * FFmpeg 执行结果密封类
 */
sealed class FFmpegResult {
    /** 成功 */
    data object Success : FFmpegResult()
    /** 取消 */
    data object Cancelled : FFmpegResult()
    /** 失败 */
    data class Failed(val returnCode: Int, val message: String?) : FFmpegResult()
}

/**
 * 视频信息
 */
data class VideoInfo(
    val path: String,
    val durationMs: Long,
    val width: Int,
    val height: Int,
    val bitrate: Long
)
