# 追剧APP ProGuard 规则
-keepattributes *Annotation*, Signature, InnerClasses, EnclosingMethod, SourceFile, LineNumberTable, RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations

# ============== Kotlin 通用保护 ==============
-keep class kotlin.Metadata { *; }
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
# Kotlin data class 保留默认构造、copy、componentN
-keepclassmembers class **$WhenMappings { *; }
-keepclassmembers,allowshrinking,allowoptimization class ** {
    kotlin.Metadata $$kotlinMetadata;
}
-keep class kotlin.reflect.** { *; }
-dontwarn kotlin.reflect.**

# ============== ExoPlayer / Media3 ==============
-keep class androidx.media3.** { *; }
-keep interface androidx.media3.** { *; }
-dontwarn androidx.media3.**

# ============== FFmpeg (stub) ==============
-keep class com.arthenica.ffmpeg.** { *; }
-dontwarn com.arthenica.**

# ============== DanmakuFlameMaster ==============
-keep class master.flame.danmaku.** { *; }
-keep interface master.flame.danmaku.** { *; }
-dontwarn master.flame.danmaku.**

# ============== OkHttp / Okio ==============
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn javax.annotation.**
-dontwarn org.conscrypt.**
-keepnames class okhttp3.internal.publicsuffix.PublicSuffixDatabase
-keep class okhttp3.** { *; }
-keep class okio.** { *; }

# ============== Glide ==============
-keep public class * implements com.bumptech.glide.module.GlideModule
-keep class * extends com.bumptech.glide.module.AppGlideModule { <init>(...); }
-keep class com.bumptech.glide.** { *; }
-dontwarn com.bumptech.glide.**

# ============== 项目自身关键类（禁止混淆） ==============
# 数据模型（MockData.kt 中定义的所有 data class 均在此包）
-keep class com.zhuiju.app.data.** { *; }
-keepclassmembers class com.zhuiju.app.data.** { *; }

# 核心播放/弹幕/网络/安全管理器（反射/单例不允许改名）
-keep class com.zhuiju.app.core.** { *; }
-keepclassmembers class com.zhuiju.app.core.** { *; }

# 工具类（防止内联优化时破坏 CrashHandler 等关键逻辑）
-keep class com.zhuiju.app.util.** { *; }
-keepclassmembers class com.zhuiju.app.util.** { *; }

# Application / Activity / Fragment 必须保留
-keep public class * extends android.app.Application
-keep public class * extends android.app.Activity
-keep public class * extends androidx.fragment.app.Fragment
-keepclassmembers class * extends android.app.Activity {
   public void *(android.view.View);
}

# ViewBinding/DataBinding 生成类保留
-keep class **.*Binding { *; }
-keepclassmembers class **.*Binding { ** inflate(...); ** bind(...); }

# ============== AndroidX / Material ==============
-keep class com.google.android.material.** { *; }
-dontwarn com.google.android.material.**
