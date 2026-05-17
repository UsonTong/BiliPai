// 私信收件箱 ViewModel
package com.android.purebilibili.feature.message

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.purebilibili.core.network.NetworkModule
import com.android.purebilibili.core.network.WbiKeyManager
import com.android.purebilibili.core.network.WbiUtils
import com.android.purebilibili.data.model.response.MessageFeedUnreadData
import com.android.purebilibili.data.model.response.MessageUnreadData
import com.android.purebilibili.data.model.response.SessionItem
import com.android.purebilibili.data.repository.MessageRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 用户简要信息 (用于缓存)
 */
data class UserBasicInfo(
    val mid: Long,
    val name: String,
    val face: String
)

data class InboxUiState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val selectedCategory: MessageSessionCategory = MessageSessionCategory.All,
    val sessions: List<SessionItem> = emptyList(),
    val unreadData: MessageUnreadData? = null,
    val feedUnreadData: MessageFeedUnreadData? = null,
    val hasMore: Boolean = false,
    val isLoadingMore: Boolean = false,
    val isBatchOperating: Boolean = false,
    val error: String? = null,
    val operationError: String? = null,
    val page: Int = 1,
    val endTs: Long = 0, //  游标 (此会话列表中最后一条的 session_ts，微秒级)
    val userInfoMap: Map<Long, UserBasicInfo> = emptyMap()  //  用户信息缓存
)

/**
 * 会话排序：置顶在前（按置顶时间降序），再按最近消息时间降序
 */
private fun List<SessionItem>.sortedByPin(): List<SessionItem> =
    sortedWith(
        compareByDescending<SessionItem> { it.top_ts }
            .thenByDescending { it.session_ts }
    )

private fun resolveSessionEndTs(sessionTs: Long): Long {
    if (sessionTs <= 0L) return 0L
    return when {
        sessionTs >= 1_000_000_000_000_000L -> sessionTs
        sessionTs >= 1_000_000_000_000L -> sessionTs * 1_000L
        else -> sessionTs * 1_000_000L
    }
}

class InboxViewModel : ViewModel() {
    
    private val _uiState = MutableStateFlow(InboxUiState())
    val uiState: StateFlow<InboxUiState> = _uiState.asStateFlow()
    
    // 用户信息缓存 (跨刷新保持)
    private val userCache = mutableMapOf<Long, UserBasicInfo>()
    
    init {
        loadSessions()
    }
    
    /**
     * 加载会话列表
     */
    fun loadSessions() {
        viewModelScope.launch {
            val category = _uiState.value.selectedCategory
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            // 并行加载未读数和会话列表
            val unreadResult = MessageRepository.getUnreadCount()
            val feedUnreadResult = MessageRepository.getFeedUnread()
            // 初始加载，endTs = 0
            val sessionsResult = MessageRepository.getSessions(
                sessionType = category.apiSessionType,
                size = 100,
                endTs = 0
            )
            
            unreadResult.onSuccess { data ->
                _uiState.value = _uiState.value.copy(unreadData = data)
            }

            feedUnreadResult.onSuccess { data ->
                _uiState.value = _uiState.value.copy(feedUnreadData = data)
            }
            
            sessionsResult.fold(
                onSuccess = { data ->
                    val sessions = data.session_list ?: emptyList()
                    
                    //  计算下一次加载的游标
                    val lastSession = sessions.lastOrNull()
                    val nextEndTs = resolveSessionEndTs(lastSession?.session_ts ?: 0L)
                    
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        sessions = sessions.sortedByPin(),
                        hasMore = data.has_more == 1,
                        userInfoMap = primeSessionUserCache(sessions).toMap(),
                        endTs = nextEndTs,
                        page = 1
                    )
                    
                    // 异步加载用户信息
                    loadUserInfosForSessions(sessions)
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = e.message ?: "加载失败"
                    )
                }
            )
        }
    }

    fun selectCategory(category: MessageSessionCategory) {
        if (_uiState.value.selectedCategory == category) return
        _uiState.value = _uiState.value.copy(
            selectedCategory = category,
            sessions = emptyList(),
            hasMore = false,
            page = 1,
            endTs = 0,
            error = null,
            operationError = null
        )
        loadSessions()
    }
    
    private fun loadUserInfosForSessions(sessions: List<SessionItem>) {
        val mids = sessions
            .filter { InboxUserInfoResolver.shouldFetchSessionUserInfo(it, userCache) }
            .map { it.talker_id }
        loadUserInfos(mids)
    }

    /**
     * 异步批量加载用户信息
     */
    private fun loadUserInfos(mids: List<Long>) {
        viewModelScope.launch {
            // 仅拉取缺失或缓存不完整的用户，避免空值缓存导致后续页面显示缺失
            val toFetch = mids
                .distinct()
                .filter { InboxUserInfoResolver.shouldFetchUserInfo(it, userCache) }
            
            toFetch.take(24).forEach { mid ->
                launch {
                    val merged = InboxUserInfoResolver.mergeFetchedUserInfo(
                        existing = userCache[mid],
                        fetched = fetchUserInfo(mid)
                    ) ?: return@launch

                    userCache[mid] = merged
                    // 更新UI状态
                    _uiState.value = _uiState.value.copy(
                        userInfoMap = userCache.toMap()
                    )
                }
            }
        }
    }
    
    /**
     * 获取单个用户信息
     */
    private suspend fun fetchUserInfo(mid: Long): UserBasicInfo? = withContext(Dispatchers.IO) {
        val cardInfo = fetchUserCardInfo(mid)
        if (InboxUserInfoResolver.hasCompleteUserInfo(cardInfo)) return@withContext cardInfo

        // 普通私信会话通常不带 account_info，card 接口失败时用空间 WBI 资料兜底昵称和头像。
        val spaceInfo = fetchSpaceUserInfo(mid)
        InboxUserInfoResolver.mergeFetchedUserInfo(cardInfo, spaceInfo)
    }

    private suspend fun fetchUserCardInfo(mid: Long): UserBasicInfo? {
        return try {
            val response = NetworkModule.api.getUserCard(mid = mid, photo = true)
            val card = response.data?.card
            if (response.code != 0 || card == null) {
                android.util.Log.w("InboxVM", "fetchUserCardInfo failed for $mid: ${response.code}")
                return null
            }

            InboxUserInfoResolver.mergeFetchedUserInfo(
                existing = null,
                fetched = UserBasicInfo(
                    mid = card.mid.toLongOrNull() ?: mid,
                    name = card.name,
                    face = card.face
                )
            )
        } catch (e: Exception) {
            android.util.Log.e("InboxVM", "fetchUserCardInfo exception for $mid", e)
            null
        }
    }

    private suspend fun fetchSpaceUserInfo(mid: Long): UserBasicInfo? {
        return try {
            val keys = WbiKeyManager.getWbiKeys().getOrNull()
                ?: WbiKeyManager.refreshKeys().getOrNull()
                ?: return null
            val params = WbiUtils.sign(mapOf("mid" to mid.toString()), keys.first, keys.second)
            val response = NetworkModule.spaceApi.getSpaceInfo(params)
            val user = response.data
            if (response.code != 0 || user == null) {
                android.util.Log.w("InboxVM", "fetchSpaceUserInfo failed for $mid: ${response.code}")
                return null
            }

            InboxUserInfoResolver.mergeFetchedUserInfo(
                existing = null,
                fetched = UserBasicInfo(
                    mid = user.mid.takeIf { it > 0L } ?: mid,
                    name = user.name,
                    face = user.face
                )
            )
        } catch (e: Exception) {
            android.util.Log.e("InboxVM", "fetchSpaceUserInfo exception for $mid", e)
            null
        }
    }
    
    /**
     * 加载更多会话
     */
    fun loadMoreSessions() {
        if (_uiState.value.isLoadingMore || !_uiState.value.hasMore) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingMore = true)

            val currentState = _uiState.value
            MessageRepository.getSessions(
                sessionType = currentState.selectedCategory.apiSessionType,
                size = 100,
                page = 1,
                endTs = currentState.endTs
            ).fold(
                onSuccess = { data ->
                    val newSessions = data.session_list ?: emptyList()
                    
                    // 去重合并: 过滤掉已存在的会话 (基于 talker_id 和 session_type)
                    val existingKeys = _uiState.value.sessions.map { "${it.talker_id}_${it.session_type}" }.toSet()
                    val filteredNewSessions = newSessions.filter { 
                        "${it.talker_id}_${it.session_type}" !in existingKeys
                    }
                    
                    val allSessions = (_uiState.value.sessions + filteredNewSessions)
                        .distinctBy { "${it.talker_id}_${it.session_type}" }
                        .sortedByPin()
                    val nextEndTs = resolveSessionEndTs(
                        (filteredNewSessions.lastOrNull() ?: newSessions.lastOrNull())?.session_ts ?: 0L
                    )
                    
                    val cursorFetchAddedNewItems = filteredNewSessions.isNotEmpty()
                    if (!cursorFetchAddedNewItems && currentState.page >= 1) {
                        loadMoreSessionsByPageFallback(currentState.page + 1)
                    } else {
                        _uiState.value = _uiState.value.copy(
                            isLoadingMore = false,
                            sessions = allSessions,
                            hasMore = data.has_more == 1 && nextEndTs > 0L,
                            page = currentState.page + 1,
                            userInfoMap = primeSessionUserCache(filteredNewSessions).toMap(),
                            endTs = nextEndTs
                        )

                        // 异步加载用户信息
                        loadUserInfosForSessions(filteredNewSessions)
                    }
                },
                onFailure = {
                    _uiState.value = _uiState.value.copy(isLoadingMore = false)
                }
            )
        }
    }

    /**
     * 下拉刷新
     */
    fun refresh() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isRefreshing = true, error = null)
            
            val unreadResult = MessageRepository.getUnreadCount()
            val feedUnreadResult = MessageRepository.getFeedUnread()
            // 刷新时重置游标
            val sessionsResult = MessageRepository.getSessions(
                sessionType = _uiState.value.selectedCategory.apiSessionType,
                size = 100,
                endTs = 0
            )
            
            unreadResult.onSuccess { data ->
                _uiState.value = _uiState.value.copy(unreadData = data)
            }

            feedUnreadResult.onSuccess { data ->
                _uiState.value = _uiState.value.copy(feedUnreadData = data)
            }
            
            sessionsResult.fold(
                onSuccess = { data ->
                    val sessions = data.session_list ?: emptyList()
                    
                    // 计算 cursor
                    val lastSession = sessions.lastOrNull()
                    val nextEndTs = resolveSessionEndTs(lastSession?.session_ts ?: 0L)
                    
                    _uiState.value = _uiState.value.copy(
                        isRefreshing = false,
                        isBatchOperating = false,
                        sessions = sessions.sortedByPin(),
                        hasMore = data.has_more == 1,
                        userInfoMap = primeSessionUserCache(sessions).toMap(),
                        endTs = nextEndTs
                    )
                    
                    // 异步加载用户信息
                    loadUserInfosForSessions(sessions)
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(
                        isRefreshing = false,
                        isBatchOperating = false,
                        error = e.message ?: "刷新失败"
                    )
                }
            )
        }
    }
    
    /**
     * 移除会话
     */
    fun removeSession(session: SessionItem) {
        viewModelScope.launch {
            MessageRepository.removeSession(session.talker_id, session.session_type)
                .onSuccess {
                    // 从列表中移除
                    val newList = _uiState.value.sessions.filter { 
                        it.talker_id != session.talker_id || it.session_type != session.session_type
                    }
                    _uiState.value = _uiState.value.copy(sessions = newList)
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        operationError = error.message ?: "删除会话失败"
                    )
                }
        }
    }
    
    /**
     * 置顶/取消置顶会话
     */
    fun toggleTop(session: SessionItem) {
        viewModelScope.launch {
            val isCurrentlyTop = session.top_ts > 0
            
            // 乐观更新：立即在本地修改 top_ts 并重排
            val now = System.currentTimeMillis() / 1000
            val updatedSessions = _uiState.value.sessions.map {
                if (it.talker_id == session.talker_id && it.session_type == session.session_type) {
                    it.copy(top_ts = if (isCurrentlyTop) 0 else now)
                } else it
            }.sortedByPin()
            _uiState.value = _uiState.value.copy(sessions = updatedSessions)
            
            MessageRepository.setSessionTop(session.talker_id, session.session_type, !isCurrentlyTop)
                .onSuccess {
                    // 后台同步服务器最新状态
                    refresh()
                }
                .onFailure {
                    // 失败时也刷新，恢复真实状态
                    refresh()
                }
        }
    }

    fun toggleDnd(session: SessionItem) {
        viewModelScope.launch {
            MessageRepository.setSessionDnd(
                talkerId = session.talker_id,
                sessionType = session.session_type,
                enabled = session.is_dnd != 1
            ).onSuccess {
                refresh()
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(
                    operationError = error.message ?: "更新免打扰失败"
                )
            }
        }
    }

    fun toggleIntercept(session: SessionItem) {
        if (session.session_type != 1) return
        viewModelScope.launch {
            MessageRepository.setSessionIntercept(
                talkerId = session.talker_id,
                intercepted = session.is_intercept != 1
            ).onSuccess {
                refresh()
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(
                    operationError = error.message ?: "更新拦截状态失败"
                )
            }
        }
    }

    fun markDustbinRead() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isBatchOperating = true, operationError = null)
            MessageRepository.markDustbinRead()
                .onSuccess { refresh() }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isBatchOperating = false,
                        operationError = error.message ?: "拦截会话已读失败"
                    )
                }
        }
    }

    fun clearDustbinSessions() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isBatchOperating = true, operationError = null)
            MessageRepository.clearDustbinSessions()
                .onSuccess { refresh() }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isBatchOperating = false,
                        operationError = error.message ?: "清空拦截会话失败"
                    )
                }
        }
    }
    
    /**
     * 清除错误信息
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun clearOperationError() {
        _uiState.value = _uiState.value.copy(operationError = null)
    }

    private fun primeSessionUserCache(sessions: List<SessionItem>): Map<Long, UserBasicInfo> {
        sessions.forEach { session ->
            val accountInfo = session.account_info ?: return@forEach
            val merged = InboxUserInfoResolver.mergeFetchedUserInfo(
                existing = userCache[session.talker_id],
                fetched = UserBasicInfo(
                    mid = session.talker_id,
                    name = accountInfo.name,
                    face = accountInfo.avatarUrl
                )
            ) ?: return@forEach
            userCache[session.talker_id] = merged
        }
        return userCache
    }

    private fun loadMoreSessionsByPageFallback(nextPage: Int) {
        viewModelScope.launch {
            MessageRepository.getSessions(
                sessionType = _uiState.value.selectedCategory.apiSessionType,
                size = 100,
                page = nextPage,
                endTs = 0L
            ).fold(
                onSuccess = { data ->
                    val newSessions = data.session_list.orEmpty()
                    val existingKeys = _uiState.value.sessions
                        .map { "${it.talker_id}_${it.session_type}" }
                        .toSet()
                    val filteredNewSessions = newSessions.filter {
                        "${it.talker_id}_${it.session_type}" !in existingKeys
                    }
                    val allSessions = (_uiState.value.sessions + filteredNewSessions)
                        .distinctBy { "${it.talker_id}_${it.session_type}" }
                        .sortedByPin()
                    val nextEndTs = resolveSessionEndTs(newSessions.lastOrNull()?.session_ts ?: 0L)

                    _uiState.value = _uiState.value.copy(
                        isLoadingMore = false,
                        sessions = allSessions,
                        hasMore = data.has_more == 1 || filteredNewSessions.isNotEmpty(),
                        page = nextPage,
                        userInfoMap = primeSessionUserCache(filteredNewSessions).toMap(),
                        endTs = nextEndTs
                    )
                    loadUserInfosForSessions(filteredNewSessions)
                },
                onFailure = {
                    _uiState.value = _uiState.value.copy(isLoadingMore = false)
                }
            )
        }
    }
}
