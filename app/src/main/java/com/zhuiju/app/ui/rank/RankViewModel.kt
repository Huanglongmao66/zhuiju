package com.zhuiju.app.ui.rank

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zhuiju.app.data.MockData
import com.zhuiju.app.data.RankItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 排行榜 ViewModel —— 日榜/周榜/月榜
 */
class RankViewModel : ViewModel() {

    companion object {
        const val TYPE_DAILY = 0
        const val TYPE_WEEKLY = 1
        const val TYPE_MONTHLY = 2
    }

    private val _rankList = MutableStateFlow<List<RankItem>>(emptyList())
    val rankList: StateFlow<List<RankItem>> = _rankList.asStateFlow()

    private val _currentType = MutableStateFlow(TYPE_DAILY)
    val currentType: StateFlow<Int> = _currentType.asStateFlow()

    init {
        loadRank(TYPE_DAILY)
    }

    fun loadRank(type: Int) {
        _currentType.value = type
        viewModelScope.launch(Dispatchers.IO) {
            delay(300)
            _rankList.value = when (type) {
                TYPE_WEEKLY -> MockData.rankWeekly
                TYPE_MONTHLY -> MockData.rankMonthly
                else -> MockData.rankDaily
            }
        }
    }
}
