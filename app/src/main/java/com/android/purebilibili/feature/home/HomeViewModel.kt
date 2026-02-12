// 文件路径: feature/home/HomeViewModel.kt
package com.android.purebilibili.feature.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.android.purebilibili.core.plugin.PluginManager
import com.android.purebilibili.core.store.SettingsManager
import com.android.purebilibili.core.store.TodayWatchFeedbackStore
import com.android.purebilibili.core.store.TodayWatchProfileStore
import com.android.purebilibili.core.util.appendDistinctByKey
import com.android.purebilibili.core.util.Logger
import com.android.purebilibili.core.util.prependDistinctByKey
import com.android.purebilibili.data.model.response.VideoItem
import com.android.purebilibili.data.repository.HistoryRepository
import com.android.purebilibili.data.repository.VideoRepository
import com.android.purebilibili.data.repository.LiveRepository
import com.android.purebilibili.feature.plugin.EyeProtectionPlugin
import com.android.purebilibili.feature.plugin.TodayWatchPlugin
import com.android.purebilibili.feature.plugin.TodayWatchPluginConfig
import com.android.purebilibili.feature.plugin.TodayWatchPluginMode
import kotlinx.coroutines.delay
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// 状态类已移至 HomeUiState.kt

internal fun trimIncrementalRefreshVideosToEvenCount(videos: List<VideoItem>): List<VideoItem> {
    val size = videos.size
    if (size <= 1 || size % 2 == 0) return videos
    return videos.dropLast(1)
}

private const val HISTORY_SAMPLE_CACHE_TTL_MS = 10 * 60 * 1000L

private fun TodayWatchPluginMode.toUiMode(): TodayWatchMode {
    return when (this) {
        TodayWatchPluginMode.RELAX -> TodayWatchMode.RELAX
        TodayWatchPluginMode.LEARN -> TodayWatchMode.LEARN
    }
}

private fun TodayWatchMode.toPluginMode(): TodayWatchPluginMode {
    return when (this) {
        TodayWatchMode.RELAX -> TodayWatchPluginMode.RELAX
        TodayWatchMode.LEARN -> TodayWatchPluginMode.LEARN
    }
}

private data class TodayWatchRuntimeConfig(
    val enabled: Boolean,
    val mode: TodayWatchMode,
    val upRankLimit: Int,
    val queueBuildLimit: Int,
    val queuePreviewLimit: Int,
    val historySampleLimit: Int,
    val linkEyeCareSignal: Boolean,
    val showUpRank: Boolean,
    val showReasonHint: Boolean,
    val enableWaterfallAnimation: Boolean,
    val waterfallExponent: Float
)

class HomeViewModel(application: Application) : AndroidViewModel(application) {
    private val _uiState = MutableStateFlow(
        HomeUiState(
            isLoading = true,
            // 初始化所有分类的状态
            categoryStates = HomeCategory.entries.associateWith { CategoryContent() }
        )
    )
    val uiState = _uiState.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing = _isRefreshing.asStateFlow()

    private var refreshIdx = 0
    private var popularPage = 1  //  热门视频分页
    private var livePage = 1     //  直播分页
    private var hasMoreLiveData = true  //  是否还有更多直播数据
    private var incrementalTimelineRefreshEnabled = false
    
    //  [新增] 会话级去重集合 (避免重复推荐)
    private val sessionSeenBvids = mutableSetOf<String>()

    // [Feature] Blocked UPs
    private val blockedUpRepository = com.android.purebilibili.data.repository.BlockedUpRepository(application)
    private var blockedMids: Set<Long> = emptySet()
    private var historySampleCache: List<VideoItem> = emptyList()
    private var historySampleLoadedAtMs: Long = 0L
    private val todayDislikedBvids = mutableSetOf<String>()
    private val todayDislikedCreatorMids = mutableSetOf<Long>()
    private val todayDislikedKeywords = linkedSetOf<String>()
    private var todayWatchPluginObserverJob: Job? = null
    private var observedTodayWatchPlugin: TodayWatchPlugin? = null

    init {
        viewModelScope.launch {
            SettingsManager.getIncrementalTimelineRefresh(getApplication()).collect { enabled ->
                incrementalTimelineRefreshEnabled = enabled
            }
        }
        // Monitor blocked list
        viewModelScope.launch {
            blockedUpRepository.getAllBlockedUps().collect { list ->
                blockedMids = list.map { it.mid }.toSet()
                reFilterAllContent()
            }
        }
        syncTodayWatchFeedbackFromStore()
        viewModelScope.launch {
            PluginManager.pluginsFlow.collect { plugins ->
                val plugin = plugins.find { it.plugin.id == TodayWatchPlugin.PLUGIN_ID }?.plugin as? TodayWatchPlugin
                if (plugin !== observedTodayWatchPlugin) {
                    todayWatchPluginObserverJob?.cancel()
                    observedTodayWatchPlugin = plugin
                    if (plugin != null) {
                        todayWatchPluginObserverJob = viewModelScope.launch {
                            plugin.configState.collect {
                                val runtime = syncTodayWatchPluginState(clearWhenDisabled = true)
                                if (runtime.enabled && _uiState.value.currentCategory == HomeCategory.RECOMMEND) {
                                    rebuildTodayWatchPlan()
                                }
                            }
                        }
                    } else {
                        todayWatchPluginObserverJob = null
                    }
                }
                val runtime = syncTodayWatchPluginState(clearWhenDisabled = true)
                if (runtime.enabled && _uiState.value.currentCategory == HomeCategory.RECOMMEND) {
                    rebuildTodayWatchPlan()
                }
            }
        }
        loadData()
    }
    
    // [Feature] Re-filter all content when block list changes
    private fun reFilterAllContent() {
        val oldState = _uiState.value
        val newCategoryStates = oldState.categoryStates.mapValues { (_, content) ->
            content.copy(
                videos = content.videos.filter { it.owner.mid !in blockedMids },
                // Filter live rooms if possible (assuming uid matches mid)
                liveRooms = content.liveRooms.filter { it.uid !in blockedMids },
                followedLiveRooms = content.followedLiveRooms.filter { it.uid !in blockedMids }
            )
        }
        
        var newState = oldState.copy(categoryStates = newCategoryStates)
        
        // Sync legacy fields for current category
        val currentContent = newCategoryStates[newState.currentCategory]
        if (currentContent != null) {
            newState = newState.copy(
                videos = currentContent.videos,
                liveRooms = currentContent.liveRooms,
                followedLiveRooms = currentContent.followedLiveRooms
            )
        }
        
        _uiState.value = newState
        viewModelScope.launch {
            rebuildTodayWatchPlan()
        }
    }

    private fun resolveTodayWatchRuntimeConfig(
        pluginEnabled: Boolean,
        config: TodayWatchPluginConfig
    ): TodayWatchRuntimeConfig {
        return TodayWatchRuntimeConfig(
            enabled = pluginEnabled,
            mode = config.currentMode.toUiMode(),
            upRankLimit = config.upRankLimit,
            queueBuildLimit = config.queueBuildLimit,
            queuePreviewLimit = config.queuePreviewLimit,
            historySampleLimit = config.historySampleLimit,
            linkEyeCareSignal = config.linkEyeCareSignal,
            showUpRank = config.showUpRank,
            showReasonHint = config.showReasonHint,
            enableWaterfallAnimation = config.enableWaterfallAnimation,
            waterfallExponent = config.waterfallExponent
        )
    }

    private fun syncTodayWatchPluginState(clearWhenDisabled: Boolean): TodayWatchRuntimeConfig {
        val info = PluginManager.plugins.find { it.plugin.id == TodayWatchPlugin.PLUGIN_ID }
        val pluginEnabled = info?.enabled == true
        val plugin = info?.plugin as? TodayWatchPlugin
        val config = plugin?.configState?.value ?: TodayWatchPluginConfig()
        val runtime = resolveTodayWatchRuntimeConfig(pluginEnabled = pluginEnabled, config = config)

        val currentState = _uiState.value
        var nextState = currentState.copy(
            todayWatchPluginEnabled = runtime.enabled,
            todayWatchMode = runtime.mode,
            todayWatchCardConfig = TodayWatchCardUiConfig(
                showUpRank = runtime.showUpRank,
                showReasonHint = runtime.showReasonHint,
                queuePreviewLimit = runtime.queuePreviewLimit,
                enableWaterfallAnimation = runtime.enableWaterfallAnimation,
                waterfallExponent = runtime.waterfallExponent
            )
        )

        if (!runtime.enabled && clearWhenDisabled) {
            nextState = nextState.copy(
                todayWatchPlan = null,
                todayWatchLoading = false,
                todayWatchError = null
            )
        }
        if (nextState != currentState) {
            _uiState.value = nextState
        }
        return runtime
    }

    fun switchTodayWatchMode(mode: TodayWatchMode) {
        val info = PluginManager.plugins.find { it.plugin.id == TodayWatchPlugin.PLUGIN_ID }
        if (info?.enabled != true) return

        val plugin = info.plugin as? TodayWatchPlugin
        plugin?.setCurrentMode(mode.toPluginMode())
        _uiState.value = _uiState.value.copy(todayWatchMode = mode)
        viewModelScope.launch {
            rebuildTodayWatchPlan()
        }
    }

    private suspend fun rebuildTodayWatchPlan(forceReloadHistory: Boolean = false) {
        val runtime = syncTodayWatchPluginState(clearWhenDisabled = true)
        if (!runtime.enabled) {
            return
        }
        syncTodayWatchFeedbackFromStore()

        val recommendVideos = getRecommendCandidates()
        if (recommendVideos.isEmpty()) {
            _uiState.value = _uiState.value.copy(
                todayWatchPlan = null,
                todayWatchLoading = false,
                todayWatchError = null
            )
            return
        }

        _uiState.value = _uiState.value.copy(todayWatchLoading = true, todayWatchError = null)

        val historySample = loadHistorySample(
            forceReload = forceReloadHistory,
            sampleLimit = runtime.historySampleLimit
        )
        val creatorSignals = TodayWatchProfileStore.getCreatorSignals(
            context = getApplication(),
            limit = runtime.historySampleLimit / 4
        ).map {
            TodayWatchCreatorSignal(
                mid = it.mid,
                name = it.name,
                score = it.score,
                watchCount = it.watchCount
            )
        }
        val eyeCareNightActive = runtime.linkEyeCareSignal &&
            EyeProtectionPlugin.getInstance()?.isNightModeActive?.value == true

        val plan = buildTodayWatchPlan(
            historyVideos = historySample,
            candidateVideos = recommendVideos,
            mode = runtime.mode,
            eyeCareNightActive = eyeCareNightActive,
            upRankLimit = runtime.upRankLimit,
            queueLimit = runtime.queueBuildLimit,
            creatorSignals = creatorSignals,
            penaltySignals = TodayWatchPenaltySignals(
                dislikedBvids = todayDislikedBvids.toSet(),
                dislikedCreatorMids = todayDislikedCreatorMids.toSet(),
                dislikedKeywords = todayDislikedKeywords.toSet()
            )
        )

        _uiState.value = _uiState.value.copy(
            todayWatchPlan = plan,
            todayWatchMode = runtime.mode,
            todayWatchLoading = false,
            todayWatchError = null
        )
    }

    private suspend fun loadHistorySample(forceReload: Boolean, sampleLimit: Int): List<VideoItem> {
        val now = System.currentTimeMillis()
        if (!forceReload &&
            historySampleCache.isNotEmpty() &&
            now - historySampleLoadedAtMs < HISTORY_SAMPLE_CACHE_TTL_MS
        ) {
            return historySampleCache.take(sampleLimit.coerceIn(20, 120))
        }

        val firstPage = HistoryRepository.getHistoryList(ps = 50, max = 0, viewAt = 0).getOrNull()
        if (firstPage == null) {
            _uiState.value = _uiState.value.copy(
                todayWatchLoading = false,
                todayWatchError = "历史记录不可用，已按当前推荐生成"
            )
            return emptyList()
        }

        val merged = firstPage.list.map { it.toVideoItem() }.toMutableList()
        val cursor = firstPage.cursor
        if (cursor != null && cursor.max > 0 && merged.size < 80) {
            val secondPage = HistoryRepository.getHistoryList(
                ps = 50,
                max = cursor.max,
                viewAt = cursor.view_at
            ).getOrNull()
            if (secondPage != null) {
                merged += secondPage.list.map { it.toVideoItem() }
            }
        }

        historySampleCache = merged
            .filter { it.bvid.isNotBlank() }
            .distinctBy { it.bvid }
        historySampleLoadedAtMs = now
        return historySampleCache.take(sampleLimit.coerceIn(20, 120))
    }

    private fun getRecommendCandidates(): List<VideoItem> {
        val state = _uiState.value
        val recommendVideos = state.categoryStates[HomeCategory.RECOMMEND]?.videos.orEmpty()
        return if (recommendVideos.isNotEmpty()) {
            recommendVideos
        } else if (state.currentCategory == HomeCategory.RECOMMEND) {
            state.videos
        } else {
            emptyList()
        }
    }

    //  [新增] 切换分类
    fun switchCategory(category: HomeCategory) {
        val currentState = _uiState.value
        if (currentState.currentCategory == category) return
        
        //  [修复] 标记正在切换分类，避免入场动画产生收缩效果
        com.android.purebilibili.core.util.CardPositionManager.isSwitchingCategory = true
        
        viewModelScope.launch {
            //  [修复] 如果切换到直播分类，未登录用户默认显示热门
            val liveSubCategory = if (category == HomeCategory.LIVE) {
                val isLoggedIn = !com.android.purebilibili.core.store.TokenManager.sessDataCache.isNullOrEmpty()
                if (isLoggedIn) currentState.liveSubCategory else LiveSubCategory.POPULAR
            } else {
                currentState.liveSubCategory
            }
            
            _uiState.value = currentState.copy(
                currentCategory = category,
                liveSubCategory = liveSubCategory,
                displayedTabIndex = category.ordinal
            )

            //  [修复] 恢复“追番”分类的数据拉取逻辑，确保滑动到这些页面时有内容显示
            /* 之前禁用了此处拉取，导致滑动展示空白页。现在移除提前返回。 */

            val targetCategoryState = _uiState.value.categoryStates[category] ?: CategoryContent()
            val needFetch = targetCategoryState.videos.isEmpty() && 
                           targetCategoryState.liveRooms.isEmpty() && 
                           !targetCategoryState.isLoading && 
                           targetCategoryState.error == null

            // 如果目标分类没有数据，则加载
            if (needFetch) {
                 fetchData(isLoadMore = false)
            } else if (category == HomeCategory.RECOMMEND) {
                rebuildTodayWatchPlan()
            }
        }
    }
    
    //  [新增] 更新显示的标签页索引（用于特殊分类，不改变内容只更新标签高亮）
    fun updateDisplayedTabIndex(index: Int) {
        _uiState.value = _uiState.value.copy(displayedTabIndex = index)
    }
    
    //  [新增] 开始消散动画（触发 UI 播放粒子动画）
    fun startVideoDissolve(bvid: String) {
        _uiState.value = _uiState.value.copy(
            dissolvingVideos = _uiState.value.dissolvingVideos + bvid
        )
    }
    
    //  [新增] 完成消散动画（从列表移除并记录到已过滤集合）
    //  [新增] 完成消散动画（从列表移除并记录到已过滤集合）
    fun completeVideoDissolve(bvid: String) {
        val currentCategory = _uiState.value.currentCategory
        
        // Update global dissolving list
        val newDissolving = _uiState.value.dissolvingVideos - bvid
        
        // Update category state
        updateCategoryState(currentCategory) { oldState ->
            oldState.copy(
                videos = oldState.videos.filterNot { it.bvid == bvid }
            )
        }
        
        // Also update the global dissolving set in UI state
        _uiState.value = _uiState.value.copy(dissolvingVideos = newDissolving)
        if (currentCategory == HomeCategory.RECOMMEND) {
            viewModelScope.launch {
                rebuildTodayWatchPlan()
            }
        }
    }
    
    
    //  [新增] 切换直播子分类
    fun switchLiveSubCategory(subCategory: LiveSubCategory) {
        if (_uiState.value.liveSubCategory == subCategory) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                liveSubCategory = subCategory,
                liveRooms = emptyList(),
                isLoading = true,
                error = null
            )
            livePage = 1
            hasMoreLiveData = true  //  修复：切换分类时重置分页标志
            fetchLiveRooms(isLoadMore = false)
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

    // [New] Mark as Not Interested (Dislike)
    fun markNotInterested(bvid: String) {
        viewModelScope.launch {
            val currentCategory = _uiState.value.currentCategory
            val categoryVideos = _uiState.value.categoryStates[currentCategory]?.videos.orEmpty()
            categoryVideos.firstOrNull { it.bvid == bvid }?.let { video ->
                recordTodayWatchNegativeFeedback(video)
            }
            // Optimistically remove from UI
            completeVideoDissolve(bvid) 
            // TODO: Call API to persist dislike
             com.android.purebilibili.core.util.Logger.d("HomeVM", "Marked as not interested: $bvid")
        }
    }

    private fun recordTodayWatchNegativeFeedback(video: VideoItem) {
        if (video.bvid.isNotBlank()) {
            todayDislikedBvids += video.bvid
        }
        if (video.owner.mid > 0L) {
            todayDislikedCreatorMids += video.owner.mid
        }
        val keywords = extractFeedbackKeywords(video.title)
        keywords.forEach { keyword ->
            if (todayDislikedKeywords.size >= 40) {
                val oldest = todayDislikedKeywords.firstOrNull()
                if (oldest != null) todayDislikedKeywords.remove(oldest)
            }
            todayDislikedKeywords += keyword
        }
        persistTodayWatchFeedback()
    }

    private fun extractFeedbackKeywords(title: String): Set<String> {
        if (title.isBlank()) return emptySet()
        val normalized = title.lowercase()
        val stopWords = setOf("视频", "合集", "最新", "一个", "我们", "你们", "今天", "真的", "这个")

        val zhTokens = Regex("[\\u4e00-\\u9fa5]{2,6}")
            .findAll(normalized)
            .map { it.value }
            .filter { it !in stopWords }
            .take(6)
            .toList()

        val enTokens = Regex("[a-z0-9]{3,}")
            .findAll(normalized)
            .map { it.value }
            .take(4)
            .toList()

        return (zhTokens + enTokens).toSet()
    }

    private fun syncTodayWatchFeedbackFromStore() {
        val snapshot = TodayWatchFeedbackStore.getSnapshot(getApplication())
        todayDislikedBvids.clear()
        todayDislikedBvids.addAll(snapshot.dislikedBvids)
        todayDislikedCreatorMids.clear()
        todayDislikedCreatorMids.addAll(snapshot.dislikedCreatorMids)
        todayDislikedKeywords.clear()
        todayDislikedKeywords.addAll(snapshot.dislikedKeywords)
    }

    private fun persistTodayWatchFeedback() {
        TodayWatchFeedbackStore.saveSnapshot(
            context = getApplication(),
            snapshot = com.android.purebilibili.core.store.TodayWatchFeedbackSnapshot(
                dislikedBvids = todayDislikedBvids.toSet(),
                dislikedCreatorMids = todayDislikedCreatorMids.toSet(),
                dislikedKeywords = todayDislikedKeywords.toSet()
            )
        )
    }

    private fun loadData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            fetchData(isLoadMore = false)
        }
    }

    fun refresh() {
        if (_isRefreshing.value) return
        viewModelScope.launch {
            _isRefreshing.value = true
            val refreshingCategory = _uiState.value.currentCategory
            val previousRecommendTopBvid = if (refreshingCategory == HomeCategory.RECOMMEND) {
                (_uiState.value.categoryStates[HomeCategory.RECOMMEND]?.videos
                    ?: _uiState.value.videos).firstOrNull()?.bvid?.takeIf { it.isNotBlank() }
            } else null
            val newItemsCount = fetchData(isLoadMore = false, isManualRefresh = true)
            
            //  数据加载完成后再更新 refreshKey，避免闪烁
            //  刷新成功后显示趣味提示
            val refreshMessage = com.android.purebilibili.core.util.EasterEggs.getRefreshMessage()
            val oldBoundary = _uiState.value.recommendOldContentStartIndex
            val newBoundary = if (refreshingCategory == HomeCategory.RECOMMEND) {
                if ((newItemsCount ?: 0) > 0) newItemsCount else null
            } else {
                oldBoundary
            }
            val oldAnchor = _uiState.value.recommendOldContentAnchorBvid
            val newAnchor = if (refreshingCategory == HomeCategory.RECOMMEND) {
                if ((newItemsCount ?: 0) > 0) previousRecommendTopBvid else null
            } else {
                oldAnchor
            }
            _uiState.value = _uiState.value.copy(
                refreshKey = System.currentTimeMillis(),
                refreshMessage = refreshMessage,
                refreshNewItemsCount = newItemsCount,
                refreshNewItemsKey = if (newItemsCount != null) System.currentTimeMillis() else _uiState.value.refreshNewItemsKey,
                recommendOldContentAnchorBvid = newAnchor,
                recommendOldContentStartIndex = newBoundary
            )
            _isRefreshing.value = false
        }
    }

    fun markRefreshNewItemsHandled(key: Long) {
        if (key <= 0L) return
        val current = _uiState.value
        if (key != current.refreshNewItemsKey || key <= current.refreshNewItemsHandledKey) return
        _uiState.value = current.copy(refreshNewItemsHandledKey = key)
    }

    fun loadMore() {
        val currentCategory = _uiState.value.currentCategory
        val categoryState = _uiState.value.categoryStates[currentCategory] ?: return
        
        if (categoryState.isLoading || _isRefreshing.value || !categoryState.hasMore) return
        
        //  修复：如果是直播分类且没有更多数据，不再加载
        if (currentCategory == HomeCategory.LIVE && !hasMoreLiveData) {
            com.android.purebilibili.core.util.Logger.d("HomeVM", "🔴 No more live data, skipping loadMore")
            return
        }
        
        viewModelScope.launch {
            fetchData(isLoadMore = true)
        }
    }

    private suspend fun fetchData(isLoadMore: Boolean, isManualRefresh: Boolean = false): Int? {
        val currentCategory = _uiState.value.currentCategory
        var refreshNewItemsCount: Int? = null
        
        // 更新当前分类为加载状态
        updateCategoryState(currentCategory) { it.copy(isLoading = true, error = null) }
        
        //  直播分类单独处理 (TODO: Adapt fetchLiveRooms to use categoryStates)
        if (currentCategory == HomeCategory.LIVE) {
            fetchLiveRooms(isLoadMore)
            return refreshNewItemsCount
        }
        
        //  关注动态分类单独处理 (TODO: Adapt fetchFollowFeed to use categoryStates)
        if (currentCategory == HomeCategory.FOLLOW) {
            fetchFollowFeed(isLoadMore)
            return refreshNewItemsCount
        }
        
        val currentCategoryState = _uiState.value.categoryStates[currentCategory] ?: CategoryContent()
        // 获取当前页码 (如果是刷新则为0/1，加载更多则+1)
        val pageToFetch = if (isLoadMore) currentCategoryState.pageIndex + 1 else 1 // Assuming 1-based pagination for simplicity in general, adjust per API

        //  视频类分类处理
        val videoResult = when (currentCategory) {
            HomeCategory.RECOMMEND -> VideoRepository.getHomeVideos(if (isLoadMore) refreshIdx + 1 else 0) // Recommend uses idx, slightly different
            HomeCategory.POPULAR -> VideoRepository.getPopularVideos(pageToFetch)
            else -> {
                //  Generic categories (Game, Tech, etc.)
                if (currentCategory.tid > 0) {
                     VideoRepository.getRegionVideos(tid = currentCategory.tid, page = pageToFetch)
                } else {
                     Result.failure(Exception("Unknown category"))
                }
            }
        }
        
        // 仅在首次加载或刷新时获取用户信息
        if (!isLoadMore) {
            fetchUserInfo()
        }

        if (isLoadMore) delay(100)

        videoResult.onSuccess { videos ->
            val validVideos = videos.filter { it.bvid.isNotEmpty() && it.title.isNotEmpty() }
            
            //  [Feature] 应用屏蔽 + 原生插件 + JSON 规则插件过滤器
            val blockedFiltered = validVideos.filter { video -> video.owner.mid !in blockedMids }
            val builtinFiltered = PluginManager.filterFeedItems(blockedFiltered)
            val filteredVideos = com.android.purebilibili.core.plugin.json.JsonPluginManager
                .filterVideos(builtinFiltered)
            
            // Global deduplication for RECOMMEND only? Or per category? 
            // Usually Recommend needs global deduplication. Other categories might just need simple append.
            // For now, let's keep sessionSeenBvids for RECOMMEND, or apply globally to avoid seeing same video across tabs?
            // Let's apply globally for now as per existing logic, but maybe we should scope it?
            // Existing logic had a single sessionSeenBvids.
            
            val uniqueNewVideos = if (currentCategory == HomeCategory.RECOMMEND) {
                 filteredVideos.filter { it.bvid !in sessionSeenBvids }
            } else {
                 filteredVideos // Other categories usually have fixed lists, but let's deduplicate against themselves if needed. 
                 // Actually, region videos might have duplicates if pages overlap?
                 // Let's just stick to sessionSeenBvids if we want to avoid seeing same video anywhere.
                 filteredVideos.filter { it.bvid !in sessionSeenBvids }
            }
            
            val useIncrementalRecommendRefresh = !isLoadMore &&
                currentCategory == HomeCategory.RECOMMEND &&
                incrementalTimelineRefreshEnabled

            val incomingVideos = if (useIncrementalRecommendRefresh) {
                trimIncrementalRefreshVideosToEvenCount(uniqueNewVideos)
            } else {
                uniqueNewVideos
            }

            sessionSeenBvids.addAll(incomingVideos.map { it.bvid })
            
            if (incomingVideos.isNotEmpty() || useIncrementalRecommendRefresh) {
                var addedCount = 0
                updateCategoryState(currentCategory) { oldState ->
                    val mergedVideos = when {
                        isLoadMore -> appendDistinctByKey(oldState.videos, incomingVideos, ::videoItemKey)
                        useIncrementalRecommendRefresh -> {
                            val merged = prependDistinctByKey(oldState.videos, incomingVideos, ::videoItemKey)
                            addedCount = (merged.size - oldState.videos.size).coerceAtLeast(0)
                            merged
                        }
                        else -> incomingVideos
                    }

                    oldState.copy(
                        videos = mergedVideos,
                        liveRooms = emptyList(),
                        isLoading = false,
                        error = null,
                        pageIndex = if (isLoadMore) oldState.pageIndex + 1 else if (useIncrementalRecommendRefresh) oldState.pageIndex else 1,
                        hasMore = true
                    )
                }

                if (useIncrementalRecommendRefresh && isManualRefresh) {
                    refreshNewItemsCount = addedCount
                }
                // Update global helper vars if needed for Recommend
                if (currentCategory == HomeCategory.RECOMMEND && isLoadMore) refreshIdx++
            } else {
                 //  全被过滤掉了 OR 空列表
                 updateCategoryState(currentCategory) { oldState ->
                     oldState.copy(
                        isLoading = false,
                        error = if (!isLoadMore && oldState.videos.isEmpty()) "没有更多内容了" else null,
                        hasMore = false
                     )
                 }
            }
            if (currentCategory == HomeCategory.RECOMMEND) {
                viewModelScope.launch {
                    rebuildTodayWatchPlan(forceReloadHistory = !isLoadMore && isManualRefresh)
                }
            }
        }.onFailure { error ->
            updateCategoryState(currentCategory) { oldState ->
                oldState.copy(
                    isLoading = false,
                    error = if (!isLoadMore && oldState.videos.isEmpty()) error.message ?: "网络错误" else null
                )
            }
            if (currentCategory == HomeCategory.RECOMMEND) {
                val runtime = syncTodayWatchPluginState(clearWhenDisabled = true)
                if (runtime.enabled) {
                    _uiState.value = _uiState.value.copy(
                        todayWatchLoading = false,
                        todayWatchError = error.message ?: "今日推荐单生成失败"
                    )
                }
            }
        }
        return refreshNewItemsCount
    }
    
    // Helper to update state for a specific category
    private fun updateCategoryState(category: HomeCategory, update: (CategoryContent) -> CategoryContent) {
        val currentStates = _uiState.value.categoryStates
        val currentCategoryState = currentStates[category] ?: CategoryContent()
        val newCategoryState = update(currentCategoryState)
        val newStates = currentStates.toMutableMap()
        newStates[category] = newCategoryState
        
        // Also update legacy fields if it is current category, to keep UI working until full migration
        // Or if we fully migrated UI, we don't need to update legacy fields 'videos', 'liveRooms' etc in HomeUiState root.
        // But HomeScreen.kt still uses `state.videos`. So we MUST sync variables.
        
        var newState = _uiState.value.copy(categoryStates = newStates)
        
        if (category == newState.currentCategory) {
            newState = newState.copy(
                videos = newCategoryState.videos,
                liveRooms = newCategoryState.liveRooms,
                followedLiveRooms = newCategoryState.followedLiveRooms,
                isLoading = newCategoryState.isLoading,
                error = newCategoryState.error
            )
        }
        _uiState.value = newState
    }

    //  [新增] 获取关注动态列表
    //  [新增] 获取关注动态列表
    private suspend fun fetchFollowFeed(isLoadMore: Boolean) {
        if (com.android.purebilibili.core.store.TokenManager.sessDataCache.isNullOrEmpty()) {
             updateCategoryState(HomeCategory.FOLLOW) { oldState ->
                oldState.copy(
                    isLoading = false,
                    error = "未登录，请先登录以查看关注内容",
                    videos = emptyList() // Ensure empty to trigger error state
                )
            }
            return
        }

        if (!isLoadMore) {
            fetchUserInfo()
            com.android.purebilibili.data.repository.DynamicRepository.resetPagination()
        }
        
        val result = com.android.purebilibili.data.repository.DynamicRepository.getDynamicFeed(!isLoadMore)
        
        if (isLoadMore) delay(100)
        
        result.onSuccess { items ->
            //  将 DynamicItem 转换为 VideoItem（只保留视频类型）
            val videos = items.mapNotNull { item ->
                // Check if author is blocked
                if ((item.modules.module_author?.mid ?: 0) in blockedMids) return@mapNotNull null

                val archive = item.modules.module_dynamic?.major?.archive
                if (archive != null && archive.bvid.isNotEmpty()) {
                    com.android.purebilibili.data.model.response.VideoItem(
                        bvid = archive.bvid,
                        title = archive.title,
                        pic = archive.cover,
                        duration = parseDurationText(archive.duration_text),
                        owner = com.android.purebilibili.data.model.response.Owner(
                            mid = item.modules.module_author?.mid ?: 0,
                            name = item.modules.module_author?.name ?: "",
                            face = item.modules.module_author?.face ?: ""
                        ),
                        stat = com.android.purebilibili.data.model.response.Stat(
                            view = parseStatText(archive.stat.play),
                            danmaku = parseStatText(archive.stat.danmaku)
                        )
                    )
                } else null
            }
            
            updateCategoryState(HomeCategory.FOLLOW) { oldState ->
                val mergedVideos = when {
                    isLoadMore -> appendDistinctByKey(oldState.videos, videos, ::videoItemKey)
                    incrementalTimelineRefreshEnabled -> prependDistinctByKey(oldState.videos, videos, ::videoItemKey)
                    else -> videos
                }
                oldState.copy(
                    videos = mergedVideos,
                    liveRooms = emptyList(),
                    isLoading = false,
                    error = if (!isLoadMore && mergedVideos.isEmpty()) "暂无关注动态，请先关注一些UP主" else null,
                    hasMore = com.android.purebilibili.data.repository.DynamicRepository.hasMoreData()
                )
            }
        }.onFailure { error ->
             updateCategoryState(HomeCategory.FOLLOW) { oldState ->
                oldState.copy(
                    isLoading = false,
                    error = if (!isLoadMore && oldState.videos.isEmpty()) error.message ?: "请先登录" else null
                )
            }
        }
    }

    private fun videoItemKey(item: com.android.purebilibili.data.model.response.VideoItem): String {
        if (item.bvid.isNotBlank()) return "bvid:${item.bvid}"
        if (item.aid > 0) return "aid:${item.aid}"
        if (item.id > 0) return "id:${item.id}"
        return "${item.owner.mid}:${item.title}:${item.pubdate}"
    }
    
    //  解析时长文本 "10:24" -> 624 秒
    private fun parseDurationText(text: String): Int {
        val parts = text.split(":")
        return try {
            when (parts.size) {
                2 -> parts[0].toInt() * 60 + parts[1].toInt()
                3 -> parts[0].toInt() * 3600 + parts[1].toInt() * 60 + parts[2].toInt()
                else -> 0
            }
        } catch (e: Exception) { 0 }
    }
    
    //  解析统计文本 "123.4万" -> 1234000
    private fun parseStatText(text: String): Int {
        return try {
            if (text.contains("万")) {
                (text.replace("万", "").toFloat() * 10000).toInt()
            } else if (text.contains("亿")) {
                (text.replace("亿", "").toFloat() * 100000000).toInt()
            } else {
                text.toIntOrNull() ?: 0
            }
        } catch (e: Exception) { 0 }
    }
    
    //  🔴 [改进] 获取直播间列表（同时获取关注和热门）
    private suspend fun fetchLiveRooms(isLoadMore: Boolean) {
        val page = if (isLoadMore) livePage else 1
        
        com.android.purebilibili.core.util.Logger.d("HomeVM", "🔴 fetchLiveRooms: isLoadMore=$isLoadMore, page=$page")
        
        if (!isLoadMore) {
            fetchUserInfo()
            
            // 🔴 [改进] 首次加载时同时获取关注和热门直播
            val isLoggedIn = !com.android.purebilibili.core.store.TokenManager.sessDataCache.isNullOrEmpty()
            
            // 并行获取关注和热门直播
            val followedResult = if (isLoggedIn) LiveRepository.getFollowedLive(1) else Result.success(emptyList())
            val popularResult = LiveRepository.getLiveRooms(1)
            
            // 处理关注直播结果
            val followedRooms = followedResult.getOrDefault(emptyList())
            
            // 处理热门直播结果
            popularResult.onSuccess { rooms ->
                if (rooms.isNotEmpty() || followedRooms.isNotEmpty()) {
                    updateCategoryState(HomeCategory.LIVE) { oldState ->
                        oldState.copy(
                            followedLiveRooms = followedRooms,
                            liveRooms = rooms,
                            videos = emptyList(),
                            isLoading = false,
                            error = null,
                            hasMore = true
                        )
                    }
                } else {
                     updateCategoryState(HomeCategory.LIVE) { oldState ->
                        oldState.copy(
                            isLoading = false,
                            error = "暂无直播",
                            hasMore = false
                        )
                    }
                }
            }.onFailure { e ->
                 updateCategoryState(HomeCategory.LIVE) { oldState ->
                    oldState.copy(
                        followedLiveRooms = followedRooms,
                        isLoading = false,
                        error = if (followedRooms.isEmpty()) e.message ?: "网络错误" else null
                    )
                }
            }
        } else {
            // 加载更多时只加载热门直播（关注的主播数量有限，不需要分页）
            val result = LiveRepository.getLiveRooms(page)
            delay(100)
            
            result.onSuccess { rooms ->
                if (rooms.isNotEmpty()) {
                    val currentLiveRooms = _uiState.value.categoryStates[HomeCategory.LIVE]?.liveRooms ?: emptyList()
                    val existingRoomIds = currentLiveRooms.map { it.roomid }.toSet()
                    // [Feature] Block Filter
                    val newRooms = rooms.filter { it.roomid !in existingRoomIds && it.uid !in blockedMids }
                    
                    if (newRooms.isEmpty()) {
                        hasMoreLiveData = false
                        updateCategoryState(HomeCategory.LIVE) { it.copy(isLoading = false, hasMore = false) }
                        return@onSuccess
                    }
                    
                    updateCategoryState(HomeCategory.LIVE) { oldState ->
                        oldState.copy(
                            liveRooms = oldState.liveRooms + newRooms,
                            isLoading = false,
                            error = null,
                            hasMore = true
                        )
                    }
                } else {
                    hasMoreLiveData = false
                    updateCategoryState(HomeCategory.LIVE) { it.copy(isLoading = false, hasMore = false) }
                }
            }.onFailure { e ->
                updateCategoryState(HomeCategory.LIVE) { it.copy(isLoading = false) }
            }
        }
    }
    
    //  提取用户信息获取逻辑
    private suspend fun fetchUserInfo() {
        val navResult = VideoRepository.getNavInfo()
        navResult.onSuccess { navData ->
            if (navData.isLogin) {
                val isVip = navData.vip.status == 1
                com.android.purebilibili.core.store.TokenManager.isVipCache = isVip
                com.android.purebilibili.core.store.TokenManager.midCache = navData.mid
                com.android.purebilibili.core.util.AnalyticsHelper.syncUserContext(
                    mid = navData.mid,
                    isVip = isVip,
                    privacyModeEnabled = com.android.purebilibili.core.store.SettingsManager
                        .isPrivacyModeEnabledSync(getApplication())
                )
                _uiState.value = _uiState.value.copy(
                    user = UserState(
                        isLogin = true,
                        face = navData.face,
                        name = navData.uname,
                        mid = navData.mid,
                        level = navData.level_info.current_level,
                        coin = navData.money,
                        bcoin = navData.wallet.bcoin_balance,
                        isVip = isVip
                    )
                )
                
                //  获取关注列表（异步，不阻塞主流程）
                fetchFollowingList(navData.mid)
            } else {
                com.android.purebilibili.core.store.TokenManager.isVipCache = false
                com.android.purebilibili.core.store.TokenManager.midCache = null
                com.android.purebilibili.core.util.AnalyticsHelper.syncUserContext(
                    mid = null,
                    isVip = false,
                    privacyModeEnabled = com.android.purebilibili.core.store.SettingsManager
                        .isPrivacyModeEnabledSync(getApplication())
                )
                _uiState.value = _uiState.value.copy(
                    user = UserState(isLogin = false),
                    followingMids = emptySet()
                )
            }
        }
    }
    
    //  获取关注列表（并行分页获取，支持更多关注，带本地缓存）
    private suspend fun fetchFollowingList(mid: Long) {
        val context = getApplication<android.app.Application>()
        val prefs = context.getSharedPreferences("following_cache", android.content.Context.MODE_PRIVATE)
        val cacheKey = "following_mids_$mid"
        val cacheTimeKey = "following_time_$mid"
        
        //  检查缓存（1小时内有效）
        val cachedTime = prefs.getLong(cacheTimeKey, 0)
        val cacheValidDuration = 60 * 60 * 1000L  // 1小时
        if (System.currentTimeMillis() - cachedTime < cacheValidDuration) {
            val cachedMids = prefs.getStringSet(cacheKey, null)
            if (!cachedMids.isNullOrEmpty()) {
                val mids = cachedMids.mapNotNull { it.toLongOrNull() }.toSet()
                _uiState.value = _uiState.value.copy(followingMids = mids)
                com.android.purebilibili.core.util.Logger.d("HomeVM", " Loaded ${mids.size} following mids from cache")
                return
            }
        }
        
        //  动态获取所有关注列表（无上限）
        try {
            val allMids = mutableSetOf<Long>()
            
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                var page = 1
                while (true) {  //  无限循环，直到获取完所有关注
                    try {
                        val result = com.android.purebilibili.core.network.NetworkModule.api.getFollowings(mid, page, 50)
                        if (result.code == 0 && result.data != null) {
                            val list = result.data.list ?: break
                            if (list.isEmpty()) break
                            
                            list.forEach { user -> allMids.add(user.mid) }
                            
                            // 如果这一页不满50，说明已经获取完所有关注
                            if (list.size < 50) {
                                com.android.purebilibili.core.util.Logger.d("HomeVM", " Reached end at page $page, total: ${allMids.size}")
                                break
                            }
                            page++
                        } else {
                            break
                        }
                    } catch (e: Exception) {
                        com.android.purebilibili.core.util.Logger.e("HomeVM", " Error at page $page", e)
                        break
                    }
                }
            }
            
            //  保存到本地缓存
            prefs.edit()
                .putStringSet(cacheKey, allMids.map { it.toString() }.toSet())
                .putLong(cacheTimeKey, System.currentTimeMillis())
                .apply()
            
            _uiState.value = _uiState.value.copy(followingMids = allMids.toSet())
            com.android.purebilibili.core.util.Logger.d("HomeVM", " Total following mids fetched and cached: ${allMids.size}")
        } catch (e: Exception) {
            com.android.purebilibili.core.util.Logger.e("HomeVM", " Error fetching following list", e)
        }
    }

    // [Feature] Preview Video URL logic
    suspend fun getPreviewVideoUrl(bvid: String, cid: Long): String? {
        return try {
            com.android.purebilibili.data.repository.VideoRepository.getPreviewVideoUrl(bvid, cid)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
