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

    /** 标准移动端 UA（360影视等第三方 API 要求 MOBILE_UA） */
    private val mobileUA =
        "Mozilla/5.0 (Linux; Android 13; Pixel 7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"

    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val host = original.url.host

        // 第三方视频源 API 使用标准移动端 UA，自有 API 使用应用 UA
        val ua = if (host.contains("360kan.com") || host.contains("360kan")) {
            mobileUA
        } else {
            "ZhuiJuApp/${BuildConfig.VERSION_NAME} (Android)"
        }

        val builder = original.newBuilder()
            .header("User-Agent", ua)
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
            host.contains("storage.googleapis.com") ||
            host.contains("picsum.photos") ||
            host.contains("pravatar.cc") ||
            host.contains("i.pravatar.cc")

        if (!isMediaRequest) {
            builder.header("Content-Type", "application/json; charset=utf-8")
            builder.header("Accept", "application/json")
        }

        return chain.proceed(builder.build())
    }
}
