package com.zhuiju.app.data

/**
 * 模拟数据层 —— 数据模型 + Mock 数据提供者
 *
 * 使用公开测试视频源（Google ExoPlayer 测试媒体）保证播放器可真实播放验证。
 * 后续接入真实后端时，仅需替换 Repository 层，数据模型保持不变。
 */

// ==================== 数据模型 ====================

/** 长视频/剧集主体 */
data class Video(
    val id: String,
    val title: String,
    val coverUrl: String,
    val videoUrl: String,
    val durationMs: Long = 0L,
    val description: String = "",
    val category: String = "",
    val tags: List<String> = emptyList(),
    val playCount: Long = 0L,
    val likeCount: Int = 0,
    val episodeCount: Int = 1,
    val currentEpisode: Int = 1,
    val isVip: Boolean = false,
    val rating: Float = 0f
)

/** 剧集分集 */
data class Episode(
    val index: Int,
    val title: String,
    val videoUrl: String,
    val durationMs: Long = 0L
)

/** Banner 轮播图 */
data class Banner(
    val id: String,
    val title: String,
    val imageUrl: String,
    val videoId: String
)

/** 分类 */
data class Category(
    val id: String,
    val name: String,
    val iconRes: Int = android.R.drawable.ic_menu_gallery
)

/** 排行榜条目 */
data class RankItem(
    val rank: Int,
    val video: Video,
    val hotValue: Long
)

/** 用户资料 */
data class UserProfile(
    val userId: String,
    val nickname: String,
    val avatarUrl: String,
    val likeCount: Int = 0,
    val followingCount: Int = 0,
    val followerCount: Int = 0,
    val signature: String = "",
    val isVip: Boolean = false
)

/** 短视频条目（复用现有模型，扩展视频URL） */
data class ShortVideo(
    val id: String,
    val videoUrl: String,
    val coverUrl: String,
    val author: String,
    val avatarUrl: String,
    val description: String,
    val likeCount: Int = 0,
    val commentCount: Int = 0,
    val shareCount: Int = 0,
    val isLiked: Boolean = false
)

/** 弹幕数据 */
data class DanmakuInfo(
    val text: String,
    val timeMs: Long,
    val color: Int = 0xFFFFFFFF.toInt(),
    val type: Int = 0  // 0=滚动 1=顶部 2=底部
)

// ==================== Mock 数据提供者 ====================

/**
 * Mock 数据单例
 *
 * 所有页面模拟数据集中管理，使用真实可播放的测试视频 URL。
 */
object MockData {

    // 公开测试视频源（Google ExoPlayer 官方测试媒体，稳定可访问）
    private const val URL_BIG_BUCK_BUNNY = "https://storage.googleapis.com/exoplayer-test-media-0/BigBuckBunny_320x180.mp4"
    private const val URL_ELEPHANT_DREAM = "https://storage.googleapis.com/exoplayer-test-media-0/ElephantsDream_480p.mp4"
    private const val URL_SINTEL = "https://storage.googleapis.com/exoplayer-test-media-0/Sintel_480p.mp4"
    private const val URL_Tears_OF_Steel = "https://storage.googleapis.com/exoplayer-test-media-0/TearsOfSteel_480p.mp4"
    private const val URL_DASH = "https://storage.googleapis.com/wvmedia/cenc/h264/tears/tears_sd.mp4"

    // 封面图（使用 picsum 占位图，稳定可访问）
    private fun cover(seed: Int) = "https://picsum.photos/seed/zhuiju$seed/300/450"
    private fun bannerImg(seed: Int) = "https://picsum.photos/seed/banner$seed/750/300"
    private fun avatar(seed: Int) = "https://i.pravatar.cc/150?img=$seed"

    // ============== 构建辅助属性：必须在业务属性之前定义（object 按代码顺序初始化） ==============

    private val videoUrls = listOf(URL_BIG_BUCK_BUNNY, URL_ELEPHANT_DREAM, URL_SINTEL, URL_Tears_OF_Steel)
    private val titles = listOf(
        "钢铁之泪" to "科幻动作短片，特效炸裂",
        "大象之梦" to "奇幻冒险，开源动画先驱",
        "辛特尔" to "龙女孩的寻剑之旅",
        "大雄兔" to "搞笑动画，治愈日常",
        "深海迷踪" to "悬疑惊悚，海底秘境",
        "星空彼岸" to "浪漫爱情，跨越时空",
        "古城秘闻" to "考古探险，历史悬案",
        "极速追击" to "动作警匪，肾上腺素飙升"
    )
    private val mockCategories: List<Category> = listOf(
        Category("c1", "电影", android.R.drawable.ic_menu_camera),
        Category("c2", "电视剧", android.R.drawable.ic_menu_view),
        Category("c3", "综艺", android.R.drawable.ic_menu_sort_by_size),
        Category("c4", "动画", android.R.drawable.ic_menu_gallery),
        Category("c5", "纪录片", android.R.drawable.ic_menu_my_calendar),
        Category("c6", "短视频", android.R.drawable.ic_menu_send)
    )

    /** 首页短视频 Feed */
    val shortVideos: List<ShortVideo> = listOf(
        ShortVideo("sv1", URL_BIG_BUCK_BUNNY, cover(1), "追剧小达人", avatar(1),
            "笑死！这只兔子太搞笑了 #搞笑 #动画", 12500, 328, 156, true),
        ShortVideo("sv2", URL_ELEPHANT_DREAM, cover(2), "影视剪辑师", avatar(2),
            "一部被名字耽误的科幻神作 #科幻 #烧脑", 8900, 210, 89),
        ShortVideo("sv3", URL_SINTEL, cover(3), "电影安利官", avatar(3),
            "龙女孩的寻剑之旅，画风绝美 #奇幻 #动画", 23000, 567, 340, true),
        ShortVideo("sv4", URL_Tears_OF_Steel, cover(4), "追剧日记", avatar(4),
            "钢铁之泪，这部短片特效炸裂 #科幻 #动作", 6700, 145, 67),
        ShortVideo("sv5", URL_BIG_BUCK_BUNNY, cover(5), "深夜看片", avatar(5),
            "三只小兔子的冒险日常，治愈系 #治愈 #动画", 15600, 423, 198, true)
    )

    /** 发现页 Banner */
    val banners: List<Banner> = listOf(
        Banner("b1", "年度科幻巨制《钢铁之泪》上线", bannerImg(1), "v1"),
        Banner("b2", "治愈动画合集，温暖你的冬天", bannerImg(2), "v2"),
        Banner("b3", "高分悬疑片单，烧脑不停", bannerImg(3), "v3")
    )

    /** 发现页分类 */
    val categories: List<Category> = mockCategories

    /** 发现页推荐视频 */
    val discoverVideos: List<Video> = buildVideoList(8)

    /** 排行榜 - 日榜 */
    val rankDaily: List<RankItem> = buildRankList(10, 1000)

    /** 排行榜 - 周榜 */
    val rankWeekly: List<RankItem> = buildRankList(10, 5000)

    /** 排行榜 - 月榜 */
    val rankMonthly: List<RankItem> = buildRankList(10, 20000)

    /** 找片页 - 热门搜索词 */
    val hotSearchWords: List<String> = listOf(
        "钢铁之泪", "大象之梦", "辛特尔", "大雄兔", "科幻", "悬疑", "治愈", "高分电影"
    )

    /** 找片页 - 分类区 */
    val findCategories: List<Category> = categories

    /** 找片页 - 分区1（热门推荐） */
    val findSectionVideos: List<Video> = buildVideoList(6)

    /** 我的页 - 当前用户 */
    val currentUser: UserProfile = UserProfile(
        userId = "u001",
        nickname = "追剧达人",
        avatarUrl = avatar(12),
        likeCount = 12800,
        followingCount = 156,
        followerCount = 2300,
        signature = "分享每一部值得追的好剧 🎬",
        isVip = true
    )

    /** 我的页 - 作品列表 */
    val myWorks: List<Video> = buildVideoList(4)

    /** 我的页 - 收藏列表 */
    val myFavorites: List<Video> = buildVideoList(3)

    /** 长视频详情（播放页用） */
    val longVideoDetail: Video = Video(
        id = "v1",
        title = "钢铁之泪 Tears of Steel",
        coverUrl = cover(10),
        videoUrl = URL_Tears_OF_Steel,
        durationMs = 734_000L,
        description = "在阿姆斯特丹的科幻背景下，一群战士和科学家试图从机器人军团手中拯救世界。这部短片由 Blender 基金会制作，展示了开源电影的特效实力。",
        category = "科幻",
        tags = listOf("科幻", "动作", "短片", "特效"),
        playCount = 1_250_000L,
        likeCount = 8900,
        episodeCount = 1,
        rating = 8.5f
    )

    /** 长视频弹幕数据 */
    val longVideoDanmakus: List<DanmakuInfo> = listOf(
        DanmakuInfo("开头好震撼！", 3_000, 0xFFFFFFFF.toInt(), 0),
        DanmakuInfo("这特效绝了", 5_500, 0xFFFFDD00.toInt(), 0),
        DanmakuInfo("前排打卡", 8_000, 0xFFFF5555.toInt(), 0),
        DanmakuInfo("Blender出品必属精品", 12_000, 0xFF55FF55.toInt(), 0),
        DanmakuInfo("这配乐太燃了", 15_000, 0xFF55AAFF.toInt(), 0),
        DanmakuInfo("机器人好逼真", 20_000, 0xFFFFFFFF.toInt(), 0),
        DanmakuInfo("看到这里泪目了", 25_000, 0xFFFFDD00.toInt(), 0),
        DanmakuInfo("2026年还有人看吗", 30_000, 0xFFFFFFFF.toInt(), 0),
        DanmakuInfo("1", 35_000, 0xFFFFFFFF.toInt(), 0),
        DanmakuInfo("经典永流传", 40_000, 0xFFFF5555.toInt(), 0),
        DanmakuInfo("开源电影的巅峰", 45_000, 0xFF55FF55.toInt(), 0),
        DanmakuInfo("这画面放今天也不过时", 50_000, 0xFFFFFFFF.toInt(), 0),
        DanmakuInfo("顶顶顶", 55_000, 0xFF55AAFF.toInt(), 0),
        DanmakuInfo("阿姆斯特丹好美", 60_000, 0xFFFFFFFF.toInt(), 1),
        DanmakuInfo("续集呢？", 65_000, 0xFFFFFFFF.toInt(), 2)
    )

    // ==================== 构建辅助 ====================

    private fun buildVideoList(count: Int): List<Video> =
        (1..count).map { i ->
            val (title, desc) = titles[(i - 1) % titles.size]
            Video(
                id = "v$i",
                title = "$title 第${i}集",
                coverUrl = cover(i + 10),
                videoUrl = videoUrls[(i - 1) % videoUrls.size],
                durationMs = (45 * 60_000L + i * 60_000L),
                description = desc,
                category = mockCategories[(i - 1) % mockCategories.size].name,
                tags = listOf("热门", "推荐"),
                playCount = (10_000L..200_000L).random(),
                likeCount = (500..9999).random(),
                rating = kotlin.random.Random.nextDouble(7.0, 9.5).toFloat()
            )
        }

    private fun buildRankList(count: Int, baseHot: Long): List<RankItem> =
        (1..count).map { i ->
            val (title, desc) = titles[(i - 1) % titles.size]
            val video = Video(
                id = "rank_v$i",
                title = title,
                coverUrl = cover(i + 20),
                videoUrl = videoUrls[(i - 1) % videoUrls.size],
                durationMs = 90 * 60_000L,
                description = desc,
                category = mockCategories[(i - 1) % mockCategories.size].name,
                playCount = baseHot * (count - i + 1).toLong(),
                likeCount = (baseHot / 10).toInt() * (count - i + 1),
                rating = kotlin.random.Random.nextDouble(7.5, 9.8).toFloat()
            )
            RankItem(
                rank = i,
                video = video,
                hotValue = baseHot * (count - i + 1).toLong()
            )
        }
}
