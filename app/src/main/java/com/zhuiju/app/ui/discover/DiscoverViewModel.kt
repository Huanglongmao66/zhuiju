package com.zhuiju.app.ui.discover

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zhuiju.app.data.Banner
import com.zhuiju.app.data.Category
import com.zhuiju.app.data.MockData
import com.zhuiju.app.data.Video
import com.zhuiju.app.data.api.Q360Api
import com.zhuiju.app.data.api.Q360Category
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 发现页 ViewModel —— Banner + 分类 + 推荐视频
 *
 * 数据源：360影视 API
 * - 分类列表: 电视剧/电影/综艺/动漫
 * - 推荐视频: 360影视排行榜 + 分类列表
 * - 分类切换: 按分类ID请求对应列表
 */
class DiscoverViewModel : ViewModel() {

    private val _banners = MutableStateFlow<List<Banner>>(emptyList())
    val banners: StateFlow<List<Banner>> = _banners.asStateFlow()

    private val _categories = MutableStateFlow<List<Category>>(emptyList())
    val categories: StateFlow<List<Category>> = _categories.asStateFlow()

    private val _videos = MutableStateFlow<List<Video>>(emptyList())
    val videos: StateFlow<List<Video>> = _videos.asStateFlow()

    /** 缓存各分类的视频列表，用于本地筛选 */
    private var allVideosByCategory: Map<String, List<Video>> = emptyMap()

    init {
        loadData()
    }

    fun loadData() {
        viewModelScope.launch(Dispatchers.IO) {
            // 分类（360影视固定4个分类）
            val categories = Q360Api.getCategories()
            _categories.value = categories

            // 并行请求各分类排行榜数据
            val videoMap = mutableMapOf<String, List<Video>>()
            Q360Category.values().forEach { cat ->
                val videos = Q360Api.getRank(cat.id, 9)
                videoMap[cat.id] = videos
            }
            allVideosByCategory = videoMap

            // 默认展示电视剧 + 电影的合集作为"全部"
            val allVideos = videoMap.values.flatten().distinctBy { it.id }
            _videos.value = allVideos

            // Banner 从排行榜取前5条
            _banners.value = allVideos.take(5).mapIndexed { index, video ->
                Banner(
                    id = video.id,
                    title = video.title,
                    imageUrl = video.coverUrl,
                    videoId = video.id
                )
            }
        }
    }

    /**
     * 按分类筛选视频
     * @param tabIndex Tab 索引（0=全部，1..N 对应 categories 列表）
     */
    fun filterByCategory(tabIndex: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            if (tabIndex == 0) {
                // 全部：合并所有分类
                _videos.value = allVideosByCategory.values.flatten().distinctBy { it.id }
            } else {
                val category = _categories.value.getOrNull(tabIndex - 1)
                if (category != null) {
                    // 先用缓存，没有就请求
                    val cached = allVideosByCategory[category.id]
                    if (cached != null) {
                        _videos.value = cached
                    } else {
                        _videos.value = Q360Api.getCategoryList(category.id)
                    }
                }
            }
        }
    }
}
