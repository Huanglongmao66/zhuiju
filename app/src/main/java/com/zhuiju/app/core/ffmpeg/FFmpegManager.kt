package com.zhuiju.app.core.ffmpeg

import com.zhuiju.app.util.AppExecutors
import com.zhuiju.app.util.LogUtils
import kotlinx.coroutines.withContext
import java.io.File

/**
 * FFmpeg 辅助工具管理器（已降级为 stub）
 *
 * mobile-ffmpeg 已下线、ffmpeg-kit 需 JitPack 编译原生库易超时，
 * 此处保留对外 API，内部返回失败/空结果。核心视频播放走 ExoPlayer 硬解，不依赖本类。
 *
 * 待后续接入 ffmpeg-kit 社区版时恢复实现：
 *  - implementation("com.github.ffmpegkit-community:ffmpeg-kit-android:6.0.1")
 *  - import com.arthenica.ffmpegkit.*
 */
object FFmpegManager {

    private const val TAG = "FFmpegManager"

    /** 初始化 FFmpeg 环境（当前为 no-op） */
    fun init() {
        LogUtils.i("FFmpegManager stub 初始化（FFmpeg 未接入）", TAG)
    }

    /**
     * 执行 FFmpeg 命令（当前未接入，直接返回失败）
     *
     * @param commands FFmpeg 命令数组
     * @return [FFmpegResult] 执行结果
     */
    suspend fun execute(vararg commands: String): FFmpegResult = withContext(AppExecutors.heavyIo) {
        LogUtils.w("FFmpeg 未接入，命令被拒绝: ${commands.joinToString(" ")}", TAG)
        FFmpegResult.Failed(-1, "FFmpeg 未接入")
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
        File(outputPath).parentFile?.mkdirs()
        execute("-y", "-i", videoPath, "-ss", (timeMs / 1000.0).toString(), "-vframes", "1", "-q:v", "2", outputPath)
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
        execute("-y", "-i", videoPath, "-ss", (startMs / 1000.0).toString(), "-t", (durationMs / 1000.0).toString(),
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
     * @return [VideoInfo] 视频信息，FFmpeg 未接入时返回 null
     */
    suspend fun getVideoInfo(videoPath: String): VideoInfo? = withContext(AppExecutors.heavyIo) {
        LogUtils.w("FFmpeg 未接入，无法获取视频信息: $videoPath", TAG)
        null
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
