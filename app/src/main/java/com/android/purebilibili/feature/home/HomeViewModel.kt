// 文件路径: feature/home/HomeViewModel.kt
package com.android.purebilibili.feature.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.android.purebilibili.core.plugin.PluginManager
import com.android.purebilibili.core.util.Logger
import com.android.purebilibili.data.repository.VideoRepository
import com.android.purebilibili.data.repository.LiveRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// 状态类已移至 HomeUiState.kt

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
    
    //  [新增] 会话级去重集合 (避免重复推荐)
    private val sessionSeenBvids = mutableSetOf<String>()

    init {
        loadData()
    }

    //  [新增] 切换分类
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
            
            val targetCategoryState = currentState.categoryStates[category] ?: CategoryContent()
            val needFetch = targetCategoryState.videos.isEmpty() && targetCategoryState.liveRooms.isEmpty() && !targetCategoryState.isLoading && targetCategoryState.error == null

            _uiState.value = currentState.copy(
                currentCategory = category,
                liveSubCategory = liveSubCategory,
                displayedTabIndex = category.ordinal
            )
            
            // 如果目标分类没有数据，则加载
            if (needFetch) {
                 fetchData(isLoadMore = false)
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
            // Optimistically remove from UI
            completeVideoDissolve(bvid) 
            // TODO: Call API to persist dislike
             com.android.purebilibili.core.util.Logger.d("HomeVM", "Marked as not interested: $bvid")
        }
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
            fetchData(isLoadMore = false)
            
            //  数据加载完成后再更新 refreshKey，避免闪烁
            //  刷新成功后显示趣味提示
            val refreshMessage = com.android.purebilibili.core.util.EasterEggs.getRefreshMessage()
            _uiState.value = _uiState.value.copy(
                refreshKey = System.currentTimeMillis(),
                refreshMessage = refreshMessage
            )
            _isRefreshing.value = false
        }
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

    private suspend fun fetchData(isLoadMore: Boolean) {
        val currentCategory = _uiState.value.currentCategory
        
        // 更新当前分类为加载状态
        updateCategoryState(currentCategory) { it.copy(isLoading = true, error = null) }
        
        //  直播分类单独处理 (TODO: Adapt fetchLiveRooms to use categoryStates)
        if (currentCategory == HomeCategory.LIVE) {
            fetchLiveRooms(isLoadMore)
            return
        }
        
        //  关注动态分类单独处理 (TODO: Adapt fetchFollowFeed to use categoryStates)
        if (currentCategory == HomeCategory.FOLLOW) {
            fetchFollowFeed(isLoadMore)
            return
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
            
            //  应用原生 FeedPlugin 过滤器
            val nativeFiltered = validVideos.filter { video ->
                val plugins = PluginManager.getEnabledFeedPlugins()
                if (plugins.isEmpty()) return@filter true
                
                plugins.all { plugin ->
                    try {
                        plugin.shouldShowItem(video)
                    } catch (e: Exception) {
                        Logger.e("HomeVM", "Plugin ${plugin.name} filter failed", e)
                        true  // 过滤器失败时默认显示
                    }
                }
            }
            
            //  [新增] 应用 JSON 规则插件过滤器
            val filteredVideos = com.android.purebilibili.core.plugin.json.JsonPluginManager.filterVideos(nativeFiltered)
            
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
                
            sessionSeenBvids.addAll(uniqueNewVideos.map { it.bvid })
            
            if (uniqueNewVideos.isNotEmpty()) {
                updateCategoryState(currentCategory) { oldState ->
                    oldState.copy(
                        videos = if (isLoadMore) oldState.videos + uniqueNewVideos else uniqueNewVideos,
                        liveRooms = emptyList(),
                        isLoading = false,
                        error = null,
                        pageIndex = if (isLoadMore) oldState.pageIndex + 1 else 1,
                        hasMore = true // Assuming if we got data, there might be more
                    )
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
        }.onFailure { error ->
            updateCategoryState(currentCategory) { oldState ->
                oldState.copy(
                    isLoading = false,
                    error = if (!isLoadMore && oldState.videos.isEmpty()) error.message ?: "网络错误" else null
                )
            }
        }
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
            
            if (videos.isNotEmpty()) {
                updateCategoryState(HomeCategory.FOLLOW) { oldState ->
                    oldState.copy(
                        videos = if (isLoadMore) oldState.videos + videos else videos,
                        liveRooms = emptyList(),
                        isLoading = false,
                        error = null,
                        hasMore = true // Assume more unless empty
                    )
                }
            } else {
                 updateCategoryState(HomeCategory.FOLLOW) { oldState ->
                    oldState.copy(
                        isLoading = false,
                        error = if (!isLoadMore && oldState.videos.isEmpty()) "暂无关注动态，请先关注一些UP主" else null,
                        hasMore = false
                    )
                }
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
                    val newRooms = rooms.filter { it.roomid !in existingRoomIds }
                    
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