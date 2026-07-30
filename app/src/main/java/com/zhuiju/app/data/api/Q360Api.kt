package com.zhuiju.app.data.api

import com.zhuiju.app.core.network.HttpUtils
import com.zhuiju.app.data.Video
import com.zhuiju.app.util.LogUtils
import kotlinx.serialization.json.Json

/**
 * 360影视 API 封装
 *
 * 数据源: 360影视.js 规则
 * - 排行榜(首页推荐): /v1/rank?cat={cat}&size={size}
 * - 分类列表(一级):   /v1/filter/list?catid={cat}&rank=rankhot&size=35&pageno={page}
 * - 详情(二级):       /v1/detail?cat={cat}&id={id}
 * - 搜索:             /index?force_v=1&kw={kw}&pageno={page}&v_ap=1&tab=all
 *
 * 所有接口返回 JSON，通过 kotlinx.serialization 解析后转换为项目通用 [Video] 模型。
 */
object Q360Api {

    private const val TAG = "Q360Api"

    /** 360影视主机 */
    private const val HOST = "https://api.web.360kan.com"
    private const val SEARCH_HOST = "https://api.so.360kan.com"

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        isLenient = true
    }

    // ==================== 分类 ====================

    /** 获取所有分类 */
    fun getCategories(): List<com.zhuiju.app.data.Category> {
        return Q360Category.values().map { cat ->
            com.zhuiju.app.data.Category(id = cat.id, name = cat.displayName)
        }
    }

    // ==================== 排行榜（首页推荐） ====================

    /**
     * 获取排行榜数据
     * @param cat 分类ID（1=电影 2=电视剧 3=综艺 4=动漫）
     * @param size 返回条数
     */
    suspend fun getRank(cat: String = "2", size: Int = 9): List<Video> {
        return try {
            val url = "$HOST/v1/rank?cat=$cat&size=$size"
            val responseStr = HttpUtils.get(url)
            val response = json.decodeFromString(Q360RankResponse.serializer(), responseStr)
            response.data?.mapNotNull { item ->
                val entId = item.ent_id ?: return@mapNotNull null
                val title = item.title ?: return@mapNotNull null
                Video(
                    id = "${item.cat ?: cat}_$entId",
                    title = title,
                    coverUrl = item.cover ?: "",
                    videoUrl = "",  // 详情页获取
                    description = item.description ?: "",
                    category = Q360Category.fromId(item.cat ?: cat)?.displayName ?: "",
                    playCount = (item.comment?.replace("万", "0000")?.toLongOrNull() ?: 0L) * 100,
                    rating = item.score?.toFloatOrNull() ?: 0f,
                    tags = listOfNotNull(item.year, item.area)
                )
            } ?: emptyList()
        } catch (e: Exception) {
            LogUtils.e("获取排行榜失败: ${e.message}", TAG, e)
            emptyList()
        }
    }

    // ==================== 分类列表（一级） ====================

    /**
     * 获取分类视频列表
     * @param catid 分类ID
     * @param page 页码
     * @param size 每页条数
     */
    suspend fun getCategoryList(catid: String, page: Int = 1, size: Int = 35): List<Video> {
        return try {
            val url = "$HOST/v1/filter/list?catid=$catid&rank=rankhot&cat=&year=&area=&act=&size=$size&pageno=$page&callback="
            val responseStr = HttpUtils.get(url)
            val response = json.decodeFromString(Q360ListResponse.serializer(), responseStr)
            response.data?.movies?.mapNotNull { movie ->
                val id = movie.id ?: return@mapNotNull null
                val title = movie.title ?: return@mapNotNull null
                Video(
                    id = "${catid}_$id",
                    title = title,
                    coverUrl = movie.cover ?: "",
                    videoUrl = "",  // 详情页获取
                    description = movie.description ?: movie.content ?: "",
                    category = Q360Category.fromId(catid)?.displayName ?: movie.catname ?: "",
                    playCount = 0L,
                    rating = movie.score?.toFloatOrNull() ?: 0f,
                    tags = listOfNotNull(movie.year, movie.area)
                )
            } ?: emptyList()
        } catch (e: Exception) {
            LogUtils.e("获取分类列表失败: ${e.message}", TAG, e)
            emptyList()
        }
    }

    // ==================== 详情（二级） ====================

    /**
     * 获取视频详情 + 播放地址
     *
     * 对应 JS 规则的二级解析逻辑：
     * - 读取 playlink_sites（播放源列表）
     * - 读取 playlinksdetail（每个源的默认播放地址）
     * - allupinfo 存在时表示有多集（暂取第一源默认地址）
     *
     * @param cat 分类ID
     * @param id 视频ID
     * @return VideoDetail 包含播放源和地址
     */
    suspend fun getDetail(cat: String, id: String): VideoDetail? {
        return try {
            val url = "$HOST/v1/detail?cat=$cat&id=$id"
            val responseStr = HttpUtils.get(url)
            val response = json.decodeFromString(Q360DetailResponse.serializer(), responseStr)
            val data = response.data ?: return null

            val title = data.title ?: return null
            val coverUrl = data.cdncover ?: ""
            val categoryName = data.moviecategory?.joinToString(",") ?: ""
            val area = data.area?.joinToString(",") ?: ""
            val director = data.director?.joinToString(",") ?: ""
            val actor = data.actor?.joinToString(",") ?: ""
            val description = data.description ?: ""

            // 解析播放源和地址
            val playSources = mutableListOf<PlaySource>()
            data.playlink_sites?.forEach { site ->
                val detail = data.playlinksdetail?.get(site)
                val playUrl = detail?.default_url ?: ""
                if (playUrl.isNotEmpty() && detail != null) {
                    // lazy 规则: input=input.split("?")[0]，去掉查询参数
                    val cleanUrl = playUrl.split("?")[0]
                    playSources.add(PlaySource(
                        siteName = site,
                        episodes = listOf(EpisodeInfo(
                            index = 1,
                            title = detail.sort ?: "正片",
                            url = cleanUrl
                        ))
                    ))
                }
            }

            VideoDetail(
                id = "${cat}_$id",
                title = title,
                coverUrl = coverUrl,
                category = categoryName,
                area = area,
                director = director,
                actor = actor,
                description = description,
                rating = data.score?.toFloatOrNull() ?: 0f,
                year = data.year ?: "",
                playSources = playSources
            )
        } catch (e: Exception) {
            LogUtils.e("获取详情失败: ${e.message}", TAG, e)
            null
        }
    }

    // ==================== 搜索 ====================

    /**
     * 搜索视频
     * @param keyword 搜索关键词
     * @param page 页码
     */
    suspend fun search(keyword: String, page: Int = 1): List<Video> {
        return try {
            val url = "$SEARCH_HOST/index?force_v=1&kw=$keyword&from=&pageno=$page&v_ap=1&tab=all"
            val responseStr = HttpUtils.get(url)
            val response = json.decodeFromString(Q360SearchResponse.serializer(), responseStr)
            response.data?.longData?.rows?.mapNotNull { row ->
                val catId = row.cat_id ?: return@mapNotNull null
                val enId = row.en_id ?: return@mapNotNull null
                val title = row.titleTxt ?: row.titlealias ?: return@mapNotNull null
                Video(
                    id = "${catId}_$enId",
                    title = title,
                    coverUrl = row.cover ?: "",
                    videoUrl = "",  // 详情页获取
                    description = row.description ?: "",
                    category = row.cat_name ?: Q360Category.fromId(catId)?.displayName ?: "",
                    playCount = 0L,
                    rating = row.score?.toFloatOrNull() ?: 0f,
                    tags = listOfNotNull(row.year)
                )
            } ?: emptyList()
        } catch (e: Exception) {
            LogUtils.e("搜索失败: ${e.message}", TAG, e)
            emptyList()
        }
    }
}

// ==================== 详情数据模型 ====================

/** 视频详情（包含播放源） */
data class VideoDetail(
    val id: String,
    val title: String,
    val coverUrl: String,
    val category: String,
    val area: String,
    val director: String,
    val actor: String,
    val description: String,
    val rating: Float,
    val year: String,
    val playSources: List<PlaySource>
)

/** 播放源（如：爱奇艺、优酷、腾讯视频等） */
data class PlaySource(
    val siteName: String,
    val episodes: List<EpisodeInfo>
)

/** 分集信息 */
data class EpisodeInfo(
    val index: Int,
    val title: String,
    val url: String
)
