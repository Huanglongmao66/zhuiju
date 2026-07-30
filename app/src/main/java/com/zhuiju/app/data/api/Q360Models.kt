package com.zhuiju.app.data.api

import kotlinx.serialization.Serializable

/**
 * 360影视 API 响应模型
 *
 * 对应 360影视.js 规则中的 JSON 接口结构
 */

// ==================== 分类 ====================

/** 360影视分类 */
enum class Q360Category(val id: String, val displayName: String) {
    TV("2", "电视剧"),
    MOVIE("1", "电影"),
    VARIETY("3", "综艺"),
    ANIME("4", "动漫");

    companion object {
        fun fromId(id: String): Q360Category? = values().find { it.id == id }
    }
}

// ==================== 排行榜（首页推荐） ====================

/**
 * 排行榜响应
 * 接口: https://api.web.360kan.com/v1/rank?cat=2&size=9
 */
@Serializable
data class Q360RankResponse(
    val data: List<Q360RankItem>? = null
)

@Serializable
data class Q360RankItem(
    val title: String? = null,
    val cover: String? = null,
    val comment: String? = null,
    val cat: String? = null,
    val ent_id: String? = null,
    val description: String? = null,
    val score: String? = null,
    val year: String? = null,
    val area: String? = null
)

// ==================== 分类列表（一级） ====================

/**
 * 分类列表响应
 * 接口: https://api.web.360kan.com/v1/filter/list?catid={cat}&rank=rankhot&size=35&pageno={page}&callback=
 */
@Serializable
data class Q360ListResponse(
    val data: Q360ListData? = null
)

@Serializable
data class Q360ListData(
    val movies: List<Q360Movie>? = null,
    val totalCount: Int? = null
)

@Serializable
data class Q360Movie(
    val id: String? = null,
    val title: String? = null,
    val cover: String? = null,
    val pubdate: String? = null,
    val description: String? = null,
    val score: String? = null,
    val year: String? = null,
    val area: String? = null,
    val catname: String? = null,
    val content: String? = null
)

// ==================== 详情（二级） ====================

/**
 * 详情响应
 * 接口: https://api.web.360kan.com/v1/detail?cat={cat}&id={id}
 */
@Serializable
data class Q360DetailResponse(
    val data: Q360DetailData? = null
)

@Serializable
data class Q360DetailData(
    val title: String? = null,
    val cdncover: String? = null,
    val moviecategory: List<String>? = null,
    val area: List<String>? = null,
    val director: List<String>? = null,
    val actor: List<String>? = null,
    val description: String? = null,
    val playlink_sites: List<String>? = null,
    val playlinksdetail: Map<String, Q360PlayLinkDetail>? = null,
    val allupinfo: Map<String, String>? = null,
    val score: String? = null,
    val year: String? = null,
    val duration: String? = null
)

@Serializable
data class Q360PlayLinkDetail(
    val default_url: String? = null,
    val sort: String? = null
)

// ==================== 搜索 ====================

/**
 * 搜索响应
 * 接口: https://api.so.360kan.com/index?force_v=1&kw={kw}&pageno={page}&v_ap=1&tab=all
 */
@Serializable
data class Q360SearchResponse(
    val data: Q360SearchData? = null
)

@Serializable
data class Q360SearchData(
    val longData: Q360SearchLongData? = null
)

@Serializable
data class Q360SearchLongData(
    val rows: List<Q360SearchRow>? = null
)

@Serializable
data class Q360SearchRow(
    val titleTxt: String? = null,
    val titlealias: String? = null,
    val cover: String? = null,
    val cat_name: String? = null,
    val cat_id: String? = null,
    val en_id: String? = null,
    val description: String? = null,
    val score: String? = null,
    val year: String? = null
)
