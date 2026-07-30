package com.zhuiju.app.ui.find

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zhuiju.app.data.Category
import com.zhuiju.app.data.Video
import com.zhuiju.app.data.api.Q360Api
import com.zhuiju.app.data.api.Q360Category
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 找片页 ViewModel —— 热门搜索 + 分类 + 分区视频 + 搜索结果
 *
 * 数据源：360影视 API
 * - 热门搜索: 固定关键词列表
 * - 分类: 360影视4大分类
 * - 分区视频: 各分类排行榜
 * - 搜索: 360影视搜索接口
 */
class FindViewModel : ViewModel() {

    private val _hotSearchWords = MutableStateFlow<List<String>>(emptyList())
    val hotSearchWords: StateFlow<List<String>> = _hotSearchWords.asStateFlow()

    private val _categories = MutableStateFlow<List<Category>>(emptyList())
    val categories: StateFlow<List<Category>> = _categories.asStateFlow()

    private val _sectionVideos = MutableStateFlow<List<Video>>(emptyList())
    val sectionVideos: StateFlow<List<Video>> = _sectionVideos.asStateFlow()

    private val _searchResults = MutableStateFlow<List<Video>>(emptyList())
    val searchResults: StateFlow<List<Video>> = _searchResults.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    init {
        loadData()
    }

    fun loadData() {
        viewModelScope.launch(Dispatchers.IO) {
            // 热门搜索词
            _hotSearchWords.value = listOf("庆余年", "狂飙", "流浪地球", "繁花", "漫长的季节", "三体", "甄嬛传", "琅琊榜")

            // 分类
            _categories.value = Q360Api.getCategories()

            // 分区视频：取电影排行榜
            _sectionVideos.value = Q360Api.getRank(Q360Category.MOVIE.id, 12)
        }
    }

    /**
     * 搜索视频（调用360影视搜索接口）
     */
    fun search(keyword: String) {
        if (keyword.isBlank()) {
            _isSearching.value = false
            _searchResults.value = emptyList()
            return
        }
        _isSearching.value = true
        viewModelScope.launch(Dispatchers.IO) {
            _searchResults.value = Q360Api.search(keyword)
        }
    }

    fun clearSearch() {
        _isSearching.value = false
        _searchResults.value = emptyList()
    }
}
