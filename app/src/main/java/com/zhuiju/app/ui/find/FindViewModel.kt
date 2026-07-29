package com.zhuiju.app.ui.find

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zhuiju.app.data.Category
import com.zhuiju.app.data.MockData
import com.zhuiju.app.data.Video
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 找片页 ViewModel —— 热门搜索 + 分类 + 分区视频 + 搜索结果
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
            delay(300)
            _hotSearchWords.value = MockData.hotSearchWords
            _categories.value = MockData.findCategories
            _sectionVideos.value = MockData.findSectionVideos
        }
    }

    fun search(keyword: String) {
        if (keyword.isBlank()) {
            _isSearching.value = false
            _searchResults.value = emptyList()
            return
        }
        _isSearching.value = true
        viewModelScope.launch(Dispatchers.IO) {
            delay(500)
            // 从所有视频中搜索标题/描述包含关键词的
            val allVideos = MockData.discoverVideos + MockData.findSectionVideos + MockData.myWorks
            _searchResults.value = allVideos.distinctBy { it.id }.filter {
                it.title.contains(keyword, true) || it.description.contains(keyword, true) ||
                    it.category.contains(keyword, true) || it.tags.any { t -> t.contains(keyword, true) }
            }
        }
    }

    fun clearSearch() {
        _isSearching.value = false
        _searchResults.value = emptyList()
    }
}
