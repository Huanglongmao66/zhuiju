package com.zhuiju.app.ui.discover

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zhuiju.app.data.Banner
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
 * 发现页 ViewModel —— Banner + 分类 + 推荐视频
 */
class DiscoverViewModel : ViewModel() {

    private val _banners = MutableStateFlow<List<Banner>>(emptyList())
    val banners: StateFlow<List<Banner>> = _banners.asStateFlow()

    private val _categories = MutableStateFlow<List<Category>>(emptyList())
    val categories: StateFlow<List<Category>> = _categories.asStateFlow()

    private val _videos = MutableStateFlow<List<Video>>(emptyList())
    val videos: StateFlow<List<Video>> = _videos.asStateFlow()

    init {
        loadData()
    }

    fun loadData() {
        viewModelScope.launch(Dispatchers.IO) {
            delay(300)
            _banners.value = MockData.banners
            _categories.value = MockData.categories
            _videos.value = MockData.discoverVideos
        }
    }

    /**
     * 按分类筛选视频
     * @param tabIndex Tab 索引（0=全部，1..N 对应 categories 列表）
     */
    fun filterByCategory(tabIndex: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            delay(150)
            _videos.value = if (tabIndex == 0) {
                MockData.discoverVideos
            } else {
                val category = MockData.categories.getOrNull(tabIndex - 1)
                MockData.discoverVideos.filter { it.category == category?.name }
            }
        }
    }
}
