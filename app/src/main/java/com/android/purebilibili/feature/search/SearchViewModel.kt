package com.android.purebilibili.feature.search

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.android.purebilibili.core.database.AppDatabase
import com.android.purebilibili.core.database.entity.SearchHistory
import com.android.purebilibili.data.model.response.HotItem
import com.android.purebilibili.data.model.response.VideoItem
import com.android.purebilibili.data.model.response.SearchUpItem
import com.android.purebilibili.data.model.response.SearchType
import com.android.purebilibili.data.model.response.BangumiSearchItem
import com.android.purebilibili.data.model.response.LiveRoomSearchItem
import com.android.purebilibili.data.repository.SearchRepository
import com.android.purebilibili.data.repository.SearchOrder
import com.android.purebilibili.data.repository.SearchDuration
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SearchUiState(
    val query: String = "",
    val isSearching: Boolean = false,
    val showResults: Boolean = false,
    //  搜索类型
    val searchType: SearchType = SearchType.VIDEO,
    // 视频结果
    val searchResults: List<VideoItem> = emptyList(),
    //  UP主 结果
    val upResults: List<SearchUpItem> = emptyList(),
    //  [新增] 番剧结果
    val bangumiResults: List<BangumiSearchItem> = emptyList(),
    //  [新增] 直播结果
    val liveResults: List<LiveRoomSearchItem> = emptyList(),
    val hotList: List<HotItem> = emptyList(),
    val historyList: List<SearchHistory> = emptyList(),
    //  搜索建议
    val suggestions: List<String> = emptyList(),
    //  搜索发现 / 猜你想搜
    val discoverList: List<String> = listOf("黑神话悟空", "原神", "初音未来", "JOJO", "罗翔说刑法", "何同学", "毕业季", "猫咪", "我的世界", "战鹰"),
    val discoverTitle: String = "搜索发现",
    val error: String? = null,
    //  搜索过滤条件
    val searchOrder: SearchOrder = SearchOrder.TOTALRANK,
    val searchDuration: SearchDuration = SearchDuration.ALL,
    //  搜索彩蛋消息
    val easterEggMessage: String? = null,
    //  [新增] 分页状态
    val currentPage: Int = 1,
    val totalPages: Int = 1,
    val hasMoreResults: Boolean = false,
    val isLoadingMore: Boolean = false
)

class SearchViewModel(application: Application) : AndroidViewModel(application) {
    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState = _uiState.asStateFlow()

    private val searchDao = AppDatabase.getDatabase(application).searchHistoryDao()
    
    //  防抖任务
    private var suggestJob: Job? = null

    init {
        loadHotSearch()
        loadHistory()
    }

    fun onQueryChange(newQuery: String) {
        _uiState.update { it.copy(query = newQuery) }
        if (newQuery.isEmpty()) {
            _uiState.update { it.copy(showResults = false, suggestions = emptyList(), error = null) }
        } else {
            //  触发搜索建议（防抖 300ms）
            loadSuggestions(newQuery)
        }
    }
    
    //  防抖加载搜索建议
    private fun loadSuggestions(keyword: String) {
        suggestJob?.cancel()
        suggestJob = viewModelScope.launch {
            delay(300) // 防抖 300ms
            val result = SearchRepository.getSuggest(keyword)
            result.onSuccess { suggestions ->
                _uiState.update { it.copy(suggestions = suggestions.take(8)) }
            }
        }
    }
    
    //  切换搜索类型
    fun setSearchType(type: SearchType) {
        _uiState.update { it.copy(searchType = type) }
        // 如果有查询内容，重新搜索
        if (_uiState.value.query.isNotBlank()) {
            search(_uiState.value.query)
        }
    }
    
    //  设置搜索排序
    fun setSearchOrder(order: SearchOrder) {
        _uiState.update { it.copy(searchOrder = order) }
        if (_uiState.value.query.isNotBlank() && _uiState.value.showResults) {
            search(_uiState.value.query)
        }
    }
    
    //  设置时长筛选
    fun setSearchDuration(duration: SearchDuration) {
        _uiState.update { it.copy(searchDuration = duration) }
        if (_uiState.value.query.isNotBlank() && _uiState.value.showResults) {
            search(_uiState.value.query)
        }
    }

    fun search(keyword: String) {
        if (keyword.isBlank()) return

        //  检查搜索彩蛋关键词
        val context = getApplication<android.app.Application>()
        val easterEggEnabled = com.android.purebilibili.core.store.SettingsManager.isEasterEggEnabledSync(context)
        val easterEggMessage = if (easterEggEnabled) {
            com.android.purebilibili.core.util.EasterEggs.checkSearchEasterEgg(keyword)
        } else null

        //  清空建议列表，设置彩蛋消息，重置分页状态
        _uiState.update { 
            it.copy(
                query = keyword, 
                isSearching = true, 
                showResults = true, 
                suggestions = emptyList(), 
                error = null,
                easterEggMessage = easterEggMessage,
                currentPage = 1,  // [新增] 重置分页
                hasMoreResults = false,
                isLoadingMore = false
            ) 
        }
        saveHistory(keyword)
        
        //  记录搜索事件
        com.android.purebilibili.core.util.AnalyticsHelper.logSearch(keyword)

        viewModelScope.launch {
            val searchType = _uiState.value.searchType
            
            when (searchType) {
                SearchType.VIDEO -> {
                    val order = _uiState.value.searchOrder
                    val duration = _uiState.value.searchDuration
                    val result = SearchRepository.search(keyword, order, duration, page = 1)
                    result.onSuccess { (videos, pageInfo) ->
                        //  [修复] 应用插件过滤（UP主拉黑、关键词屏蔽等）
                        val filteredVideos = com.android.purebilibili.core.plugin.PluginManager
                            .filterFeedItems(videos)
                        _uiState.update { 
                            it.copy(
                                isSearching = false, 
                                searchResults = filteredVideos, 
                                upResults = emptyList(),
                                currentPage = pageInfo.currentPage,
                                totalPages = pageInfo.totalPages,
                                hasMoreResults = pageInfo.hasMore
                            ) 
                        }
                    }.onFailure { e ->
                        _uiState.update { it.copy(isSearching = false, error = e.message ?: "搜索失败") }
                    }
                }
                SearchType.UP -> {
                    val result = SearchRepository.searchUp(keyword)
                    result.onSuccess { ups ->
                        _uiState.update { 
                            it.copy(
                                isSearching = false, 
                                upResults = ups, 
                                searchResults = emptyList(),
                                bangumiResults = emptyList(),
                                liveResults = emptyList(),
                                hasMoreResults = false
                            ) 
                        }
                    }.onFailure { e ->
                        _uiState.update { it.copy(isSearching = false, error = e.message ?: "搜索失败") }
                    }
                }
                SearchType.BANGUMI -> {
                    val result = SearchRepository.searchBangumi(keyword, page = 1)
                    result.onSuccess { (bangumis, pageInfo) ->
                        _uiState.update { 
                            it.copy(
                                isSearching = false, 
                                bangumiResults = bangumis,
                                searchResults = emptyList(),
                                upResults = emptyList(),
                                liveResults = emptyList(),
                                currentPage = pageInfo.currentPage,
                                totalPages = pageInfo.totalPages,
                                hasMoreResults = pageInfo.hasMore
                            ) 
                        }
                    }.onFailure { e ->
                        _uiState.update { it.copy(isSearching = false, error = e.message ?: "搜索失败") }
                    }
                }
                SearchType.LIVE -> {
                    val result = SearchRepository.searchLive(keyword, page = 1)
                    result.onSuccess { (liveRooms, pageInfo) ->
                        _uiState.update { 
                            it.copy(
                                isSearching = false, 
                                liveResults = liveRooms,
                                searchResults = emptyList(),
                                upResults = emptyList(),
                                bangumiResults = emptyList(),
                                currentPage = pageInfo.currentPage,
                                totalPages = pageInfo.totalPages,
                                hasMoreResults = pageInfo.hasMore
                            ) 
                        }
                    }.onFailure { e ->
                        _uiState.update { it.copy(isSearching = false, error = e.message ?: "搜索失败") }
                    }
                }
            }
        }
    }
    
    //  [新增] 加载更多搜索结果
    fun loadMoreResults() {
        val state = _uiState.value
        
        // 检查条件：必须是视频搜索、有更多结果、不在加载中
        if (state.searchType != SearchType.VIDEO || !state.hasMoreResults || state.isLoadingMore || state.query.isBlank()) {
            return
        }
        
        _uiState.update { it.copy(isLoadingMore = true) }
        
        val nextPage = state.currentPage + 1
        
        viewModelScope.launch {
            val order = state.searchOrder
            val duration = state.searchDuration
            val result = SearchRepository.search(state.query, order, duration, page = nextPage)
            
            result.onSuccess { (videos, pageInfo) ->
                //  应用插件过滤
                val filteredVideos = com.android.purebilibili.core.plugin.PluginManager
                    .filterFeedItems(videos)
                
                _uiState.update { 
                    it.copy(
                        isLoadingMore = false,
                        searchResults = it.searchResults + filteredVideos,  // 追加新结果
                        currentPage = pageInfo.currentPage,
                        totalPages = pageInfo.totalPages,
                        hasMoreResults = pageInfo.hasMore
                    ) 
                }
            }.onFailure { e ->
                _uiState.update { 
                    it.copy(
                        isLoadingMore = false, 
                        error = "加载更多失败: ${e.message}"
                    ) 
                }
            }
        }
    }

    private fun loadHotSearch() {
        viewModelScope.launch {
            val result = SearchRepository.getHotSearch()
            result.onSuccess { items ->
                _uiState.update { it.copy(hotList = items) }
            }
        }
    }

    private fun loadHistory() {
        viewModelScope.launch {
            searchDao.getAll().collect { history ->
                _uiState.update { it.copy(historyList = history) }
                //  更新搜索发现
                updateDiscover(history)
            }
        }
    }

    //  生成个性化发现内容
    private fun updateDiscover(history: List<SearchHistory>) {
        viewModelScope.launch {
            val historyKeywords = history.map { it.keyword }
            // 使用 Repository 获取 (包含个性化逻辑 + 官方热搜兜底)
            val result = SearchRepository.getSearchDiscover(historyKeywords)
            
            result.onSuccess { (title, list) ->
                _uiState.update { 
                    it.copy(
                        discoverTitle = title,
                        discoverList = list
                    )
                }
            }
        }
    }

    private fun saveHistory(keyword: String) {
        viewModelScope.launch {
            //  隐私无痕模式检查：如果启用则跳过保存搜索历史
            val context = getApplication<android.app.Application>()
            if (com.android.purebilibili.core.store.SettingsManager.isPrivacyModeEnabledSync(context)) {
                com.android.purebilibili.core.util.Logger.d("SearchVM", " Privacy mode enabled, skipping search history save")
                return@launch
            }
            
            //  使用 keyword 主键，重复搜索自动更新时间戳
            searchDao.insert(SearchHistory(keyword = keyword, timestamp = System.currentTimeMillis()))
        }
    }

    fun deleteHistory(history: SearchHistory) {
        viewModelScope.launch {
            searchDao.delete(history)
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            searchDao.clearAll()
        }
    }
    
    //  [新增] 添加到稀后再看
    fun addToWatchLater(bvid: String, aid: Long) {
        viewModelScope.launch {
            val result = com.android.purebilibili.data.repository.ActionRepository.toggleWatchLater(aid, true)
            result.onSuccess {
                android.widget.Toast.makeText(getApplication(), "已添加到稍后再看", android.widget.Toast.LENGTH_SHORT).show()
            }.onFailure { e ->
                android.widget.Toast.makeText(getApplication(), e.message ?: "添加失败", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }
}