package com.zhuiju.app.config

/**
 * 项目全局常量定义
 *
 * 统一管理播放器、缓存、弹幕、网络、加密、手势、机型适配、动画等核心参数
 * 禁止业务代码硬编码，必须引用本类常量
 */
object AppConstants {

    // ==================== 播放器核心常量（ExoPlayer） ====================
    /** 播放器倍速档位 */
    val PLAYBACK_SPEEDS = floatArrayOf(0.25f, 0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 1.75f, 2.0f, 2.5f, 3.0f, 3.5f, 4.0f, 5.0f)
    /** 默认倍速 */
    const val PLAYBACK_SPEED_DEFAULT = 1.0f
    /** 自动隐藏控制栏延时（毫秒） */
    const val CONTROL_BAR_HIDE_DELAY_MS = 3000L
    /** 播放异常最大重试次数 */
    const val PLAYER_MAX_RETRY = 3
    /** 初始缓冲阈值（毫秒） */
    const val PLAYER_INITIAL_BUFFER_MS = 1500
    /** 最大缓冲阈值（毫秒） */
    const val PLAYER_MAX_BUFFER_MS = 5000
    /** 进度刷新间隔（毫秒） */
    const val PLAYER_PROGRESS_INTERVAL_MS = 50L
    /** 记忆播放最小记录时长（毫秒） */
    const val PLAYBACK_RECORD_MIN_MS = 3000L

    /** 视频显示模式枚举常量 */
    const val DISPLAY_MODE_ORIGINAL = 0   // 原始比例
    const val DISPLAY_MODE_16_9 = 1       // 16:9
    const val DISPLAY_MODE_4_3 = 2        // 4:3
    const val DISPLAY_MODE_FILL = 3       // 全屏铺满

    // ==================== 离线缓存常量 ====================
    /** 视频缓存根目录名称（独立隔离目录） */
    const val CACHE_DIR_NAME = "video_cache"
    /** 单视频分片大小（2MB，适配AES分片加密） */
    const val CACHE_SLICE_SIZE = 1024 * 1024 * 2
    /** 最大缓存空间（5GB） */
    const val CACHE_MAX_SIZE = 1024L * 1024 * 1024 * 5
    /** 缓存自动清理阈值（4.5GB） */
    const val CACHE_CLEAN_THRESHOLD = 1024L * 1024 * 1024 * 4.5
    /** 断点续传超时时间（秒） */
    const val CACHE_RESUME_TIMEOUT_S = 60L

    // ==================== 弹幕 Danmaku 常量 ====================
    /** 单屏最大弹幕数量 */
    const val DANMAKU_MAX_COUNT = 80
    /** 低端机型单屏最大弹幕数量 */
    const val DANMAKU_MAX_COUNT_LOW_END = 40
    /** 弹幕默认字号（sp） */
    const val DANMAKU_TEXT_SIZE_DEFAULT = 14f
    /** 弹幕大字号 */
    const val DANMAKU_TEXT_SIZE_LARGE = 16f
    /** 弹幕小字号 */
    const val DANMAKU_TEXT_SIZE_SMALL = 12f
    /** 弹幕默认透明度（0~1） */
    const val DANMAKU_ALPHA_DEFAULT = 0.8f
    /** 弹幕行间距（dp） */
    const val DANMAKU_LINE_SPACING_DP = 25
    /** 高端机弹幕渲染帧率 */
    const val DANMAKU_FPS_HIGH = 60
    /** 低端机弹幕渲染帧率 */
    const val DANMAKU_FPS_LOW = 30
    /** 弹幕数据预加载偏移（毫秒） */
    const val DANMAKU_PRELOAD_OFFSET_MS = 5000L

    // ==================== 网络 OkHttp 常量 ====================
    /** 普通接口超时（秒） */
    const val NET_TIMEOUT_NORMAL_S = 15L
    /** 视频分片下载超时（秒） */
    const val NET_TIMEOUT_SLICE_S = 30L
    /** 网络重试间隔（毫秒） */
    const val NET_RETRY_INTERVAL_MS = 2000L
    /** 连接池最大空闲连接 */
    const val NET_CONNECTION_POOL_MAX = 5
    /** 空闲连接存活时间（秒） */
    const val NET_CONNECTION_KEEP_ALIVE_S = 60L

    // ==================== AES 加密安全常量 ====================
    /** 加密模式 */
    const val AES_MODE = "AES/CBC/PKCS5Padding"
    /** 密钥长度（字节） */
    const val AES_KEY_LENGTH = 16
    /** IV 偏移量长度（字节） */
    const val AES_IV_LENGTH = 16
    /** 密钥过期时间（24小时） */
    const val AES_KEY_EXPIRE_MS = 24 * 60 * 60 * 1000L
    /** 加密分片对齐大小（字节） */
    const val AES_BLOCK_SIZE = 16

    // ==================== 手势交互常量 ====================
    /** 手势最小滑动判定距离（dp） */
    const val GESTURE_MIN_SLIDE_DP = 10f
    /** 亮度/音量调节灵敏度（每dp滑动变化量） */
    const val GESTURE_BRIGHTNESS_SENSITIVITY = 0.02f
    /** 点击事件最大判定时长（毫秒） */
    const val GESTURE_TAP_MAX_MS = 200L
    /** 长按事件最大判定时长（毫秒） */
    const val GESTURE_LONG_PRESS_MS = 1000L

    // ==================== 机型适配、权限常量 ====================
    /** 低端机CPU核心数阈值 */
    const val LOW_END_CPU_CORES_THRESHOLD = 4
    /** Android分区存储适配版本（API 29 = Android 10） */
    const val ANDROID_Q_API = 29
    /** Android后台播放权限适配版本（API 31 = Android 12） */
    const val ANDROID_S_API = 31

    // ==================== 动画全局常量 ====================
    /** 动画快速时长（毫秒）—— 退场、点击反馈 */
    const val ANIM_FAST_MS = 200L
    /** 动画标准时长（毫秒）—— 弹窗、控件显隐 */
    const val ANIM_NORMAL_MS = 250L
    /** 动画慢速时长（毫秒）—— 渐变退场、呼吸动画 */
    const val ANIM_SLOW_MS = 300L

    // ==================== UI 统一规范常量 ====================
    /** 卡片圆角（dp） */
    const val CORNER_CARD_DP = 12f
    /** 按钮圆角（dp） */
    const val CORNER_BUTTON_DP = 8f
    /** 标签圆角（dp） */
    const val CORNER_TAG_DP = 4f
    /** 间距 - 小（dp） */
    const val SPACING_SMALL_DP = 10f
    /** 间距 - 中（dp） */
    const val SPACING_MEDIUM_DP = 15f
    /** 间距 - 大（dp） */
    const val SPACING_LARGE_DP = 20f

    // ==================== 短视频专属常量 ====================
    /** 短视频播放器池大小（当前+上一个+下一个） */
    const val SHORT_VIDEO_POOL_SIZE = 3
    /** 短视频首帧渲染目标时长（毫秒） */
    const val SHORT_VIDEO_FIRST_FRAME_MS = 100L
    /** 短视频预加载缓冲（毫秒） */
    const val SHORT_VIDEO_PRELOAD_MS = 2000L
    /** 短视频临时缓存过期时间（30分钟） */
    const val SHORT_VIDEO_CACHE_EXPIRE_MS = 30 * 60 * 1000L
    /** ViewPager2 离屏页面数 */
    const val SHORT_VIDEO_OFFSCREEN_PAGE_LIMIT = 1
}
