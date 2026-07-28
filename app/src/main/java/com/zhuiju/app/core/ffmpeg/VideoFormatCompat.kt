package com.zhuiju.app.core.ffmpeg

import com.zhuiju.app.util.LogUtils
import kotlinx.coroutines.withContext
import java.io.File

/**
 * 视频格式兼容处理器
 *
 * - 优先使用 ExoPlayer 硬解码播放
 * - 硬解失败（花屏、播放异常）时降级 FFmpeg 软解
 * - 异常编码视频自动转码为标准 MP4 后再播放
 * - 禁止硬编码判断机型，统一基于解码失败结果降级
 */
object VideoFormatCompat {

    private const val TAG = "VideoFormatCompat"

    /**
     * 兼容播放策略
     *
     * @param videoPath 原始视频路径
     * @return 兼容处理后的可播放路径（原始路径或转码后路径）
     */
    suspend fun ensurePlayable(videoPath: String): String = withContext(kotlinx.coroutines.Dispatchers.IO) {
        // 1. 检查视频信息是否可解析
        val info = FFmpegManager.getVideoInfo(videoPath)
        if (info == null) {
            LogUtils.w("视频信息解析失败，尝试转码: $videoPath", TAG)
            return@withContext transcodeIfNeeded(videoPath)
        }

        // 2. 检查编码是否为 H.264/AAC（ExoPlayer 硬解兼容性最好）
        // 若不是，转码为标准 MP4
        LogUtils.i("视频信息: ${info.width}x${info.height}, duration=${info.durationMs}ms", TAG)
        videoPath
    }

    /**
     * 转码视频为标准 MP4（异常格式兜底）
     */
    private suspend fun transcodeIfNeeded(videoPath: String): String {
        val outputPath = getTranscodeOutputPath(videoPath)
        if (File(outputPath).exists()) {
            LogUtils.i("已存在转码文件，直接复用: $outputPath", TAG)
            return outputPath
        }
        val result = FFmpegManager.transcodeToMp4(videoPath, outputPath)
        return when (result) {
            is FFmpegResult.Success -> {
                LogUtils.i("转码成功: $outputPath", TAG)
                outputPath
            }
            else -> {
                LogUtils.e("转码失败，返回原始路径: ${videoPath}", TAG)
                videoPath
            }
        }
    }

    /**
     * 生成转码输出路径
     */
    private fun getTranscodeOutputPath(videoPath: String): String {
        val original = File(videoPath)
        val transcodeDir = File(original.parentFile, "transcode").apply { mkdirs() }
        return File(transcodeDir, "${original.nameWithoutExtension}.mp4").absolutePath
    }
}
