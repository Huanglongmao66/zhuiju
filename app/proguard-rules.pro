# 追剧APP ProGuard 规则
-keepattributes *Annotation*, Signature, InnerClasses, EnclosingMethod, SourceFile, LineNumberTable

# Kotlin 协程
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

# ExoPlayer / Media3
-keep class androidx.media3.** { *; }
-dontwarn androidx.media3.**

# FFmpeg
-keep class com.arthenica.ffmpeg.** { *; }
-dontwarn com.arthenica.**

# DanmakuFlameMaster
-keep class master.flame.danmaku.** { *; }
-dontwarn master.flame.danmaku.**

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }
-keep class okio.** { *; }

# Glide
-keep public class * implements com.bumptech.glide.module.GlideModule
-keep class * extends com.bumptech.glide.module.AppGlideModule { <init>(...); }
-keep class com.bumptech.glide.** { *; }
-dontwarn com.bumptech.glide.**

# 项目自身模型类（序列化）
-keep class com.zhuiju.app.data.model.** { *; }
-keepclassmembers class com.zhuiju.app.data.model.** { *; }

# AES 加密相关类禁止混淆
-keep class com.zhuiju.app.core.security.** { *; }
