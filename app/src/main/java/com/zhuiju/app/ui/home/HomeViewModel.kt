package com.zhuiju.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zhuiju.app.data.MockData
import com.zhuiju.app.data.ShortVideo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 首页 ViewModel —— 短视频 Feed 流
 */
class HomeViewModel : ViewModel() {

    private val _videos = MutableStateFlow<List<ShortVideo>>(emptyList())
    val videos: StateFlow<List<ShortVideo>> = _videos.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        loadShortVideos()
    }

    fun loadShortVideos() {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true
            delay(300) // 模拟网络延迟
            _videos.value = MockData.shortVideos
            _isLoading.value = false
        }
    }
}
