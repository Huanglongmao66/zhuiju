package com.zhuiju.app.ui.mine

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zhuiju.app.data.MockData
import com.zhuiju.app.data.UserProfile
import com.zhuiju.app.data.Video
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 我的页 ViewModel —— 用户资料 + 作品/收藏列表
 */
class MineViewModel : ViewModel() {

    companion object {
        const val TAB_WORKS = 0
        const val TAB_PRIVATE = 1
        const val TAB_FAVORITES = 2
        const val TAB_LIKES = 3
    }

    private val _userProfile = MutableStateFlow<UserProfile?>(null)
    val userProfile: StateFlow<UserProfile?> = _userProfile.asStateFlow()

    private val _works = MutableStateFlow<List<Video>>(emptyList())
    val works: StateFlow<List<Video>> = _works.asStateFlow()

    init {
        loadUserData()
    }

    fun loadUserData() {
        viewModelScope.launch(Dispatchers.IO) {
            delay(300)
            _userProfile.value = MockData.currentUser
            _works.value = MockData.myWorks
        }
    }

    fun loadTab(tab: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            delay(200)
            _works.value = when (tab) {
                TAB_FAVORITES -> MockData.myFavorites
                TAB_LIKES -> MockData.findSectionVideos
                TAB_PRIVATE -> MockData.myWorks.take(2)
                else -> MockData.myWorks
            }
        }
    }
}
