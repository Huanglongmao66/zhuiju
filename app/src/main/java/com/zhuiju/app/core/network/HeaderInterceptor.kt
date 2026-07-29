package com.zhuiju.app.core.network

import com.zhuiju.app.BuildConfig
import okhttp3.Interceptor
import okhttp3.Response

/**
 * 请求头统一拦截器
 *
 * 统一配置通用请求头：UA、Content-Type、Accept、应用版本
 *
 * 注意：视频/图片等媒体请求跳过 JSON 专用头，避免 CDN 拒绝或返回错误内容类型。
 */
class HeaderInterceptor : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val builder = original.newBuilder()
            .header("User-Agent", "ZhuiJuApp/${BuildConfig.VERSION_NAME} (Android)")
            .header("App-Version", BuildConfig.VERSION_NAME)
            .header("App-Version-Code", BuildConfig.VERSION_CODE.toString())

        // 仅对 API 业务请求添加 JSON 头；媒体请求（视频/图片/分片）保持中性
        val path = original.url.encodedPath
        val isMediaRequest = path.endsWith(".mp4") ||
            path.endsWith(".m4s") ||
            path.endsWith(".mpd") ||
            path.endsWith(".m3u8") ||
            path.endsWith(".jpg") ||
            path.endsWith(".png") ||
            path.endsWith(".webp") ||
            original.url.host.contains("storage.googleapis.com") ||
            original.url.host.contains("picsum.photos") ||
            original.url.host.contains("pravatar.cc") ||
            original.url.host.contains("i.pravatar.cc")

        if (!isMediaRequest) {
            builder.header("Content-Type", "application/json; charset=utf-8")
            builder.header("Accept", "application/json")
        }

        return chain.proceed(builder.build())
    }
}
