// 文件路径: feature/dynamic/DynamicViewModel.kt
package com.android.purebilibili.feature.dynamic

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.purebilibili.data.model.response.DynamicItem
import com.android.purebilibili.data.model.response.FollowedLiveRoom
import com.android.purebilibili.data.repository.DynamicRepository
import com.android.purebilibili.data.repository.VideoRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 🔥 动态页面 ViewModel
 * 支持：动态列表、侧边栏关注用户、在线状态
 */
class DynamicViewModel : ViewModel() {
    
    private val _uiState = MutableStateFlow(DynamicUiState())
    val uiState: StateFlow<DynamicUiState> = _uiState.asStateFlow()
    
    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()
    
    // 🔥 侧边栏相关状态
    private val _followedUsers = MutableStateFlow<List<SidebarUser>>(emptyList())
    val followedUsers: StateFlow<List<SidebarUser>> = _followedUsers.asStateFlow()
    
    private val _selectedUserId = MutableStateFlow<Long?>(null)
    val selectedUserId: StateFlow<Long?> = _selectedUserId.asStateFlow()
    
    private val _isSidebarExpanded = MutableStateFlow(true)
    val isSidebarExpanded: StateFlow<Boolean> = _isSidebarExpanded.asStateFlow()
    
    init {
        loadDynamicFeed(refresh = true)
        loadFollowedUsers()
    }
    
    /**
     * 🔥 加载关注用户列表及其直播状态
     */
    fun loadFollowedUsers() {
        viewModelScope.launch {
            // 获取关注的直播用户（有 liveStatus 字段）
            com.android.purebilibili.data.repository.LiveRepository.getFollowedLive(page = 1).onSuccess { liveRooms ->
                // 提取所有关注用户信息
                val users = extractUsersFromDynamics() + extractUsersFromLive(liveRooms)
                // 🔥🔥 [修复] 过滤无效用户数据，避免真机崩溃
                _followedUsers.value = users
                    .filter { it.uid > 0 && it.name.isNotBlank() }
                    .distinctBy { it.uid }
            }
        }
    }
    
    /**
     * 从动态列表提取用户
     */
    private fun extractUsersFromDynamics(): List<SidebarUser> {
        return _uiState.value.items
            .mapNotNull { it.modules.module_author }
            .map { author ->
                SidebarUser(
                    uid = author.mid,
                    name = author.name,
                    face = author.face,
                    isLive = false
                )
            }
    }
    
    /**
     * 从直播列表提取用户（包含在线状态）
     */
    private fun extractUsersFromLive(rooms: List<com.android.purebilibili.data.model.response.LiveRoom>): List<SidebarUser> {
        return rooms.map { room ->
            SidebarUser(
                uid = room.uid,
                name = room.uname,
                face = room.face,
                isLive = true  // 直播中
            )
        }
    }
    
    /**
     * 选择用户过滤动态
     */
    fun selectUser(uid: Long?) {
        _selectedUserId.value = uid
    }
    
    /**
     * 切换侧边栏展开/收起
     */
    fun toggleSidebar() {
        _isSidebarExpanded.value = !_isSidebarExpanded.value
    }
    
    /**
     * 加载动态列表
     */
    fun loadDynamicFeed(refresh: Boolean = false) {
        if (_uiState.value.isLoading && !refresh) return
        
        viewModelScope.launch {
            if (refresh) {
                _isRefreshing.value = true
            } else {
                _uiState.value = _uiState.value.copy(isLoading = true)
            }
            
            val result = DynamicRepository.getDynamicFeed(refresh)
            
            result.fold(
                onSuccess = { items ->
                    val currentItems = if (refresh) emptyList() else _uiState.value.items
                    _uiState.value = _uiState.value.copy(
                        items = currentItems + items,
                        isLoading = false,
                        error = null,
                        hasMore = DynamicRepository.hasMoreData()
                    )
                    // 刷新后更新关注用户列表
                    if (refresh) loadFollowedUsers()
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = error.message ?: "加载失败"
                    )
                }
            )
            
            _isRefreshing.value = false
        }
    }
    
    fun refresh() {
        loadDynamicFeed(refresh = true)
    }
    
    fun loadMore() {
        if (!_uiState.value.hasMore || _uiState.value.isLoading) return
        loadDynamicFeed(refresh = false)
    }
    
    // ==================== 🔥🔥 动态评论/点赞/转发功能 ====================
    
    // 当前选中的动态（用于评论弹窗）
    private val _selectedDynamic = MutableStateFlow<DynamicItem?>(null)
    val selectedDynamicId: StateFlow<String?> = _selectedDynamic.asStateFlow().let { flow ->
        MutableStateFlow<String?>(null).also { derived ->
            viewModelScope.launch {
                flow.collect { derived.value = it?.id_str }
            }
        }
    }
    
    // 评论列表
    private val _comments = MutableStateFlow<List<com.android.purebilibili.data.model.response.ReplyItem>>(emptyList())
    val comments: StateFlow<List<com.android.purebilibili.data.model.response.ReplyItem>> = _comments.asStateFlow()
    
    private val _commentsLoading = MutableStateFlow(false)
    val commentsLoading: StateFlow<Boolean> = _commentsLoading.asStateFlow()
    
    // 点赞状态缓存 (dynamicId -> isLiked)
    private val _likedDynamics = MutableStateFlow<Set<String>>(emptySet())
    val likedDynamics: StateFlow<Set<String>> = _likedDynamics.asStateFlow()
    
    /**
     * 🔥 根据动态类型获取评论 oid 和 type
     * - 视频动态: type=1, oid=aid
     * - 图片动态: type=11, oid=draw.id
     * - 文字动态: type=17, oid=id_str
     * - 转发动态: 使用原动态的信息
     */
    private fun getCommentParams(item: DynamicItem): Pair<Long, Int>? {
        val dynamicType = com.android.purebilibili.data.model.response.DynamicType.fromApiValue(item.type)
        return when (dynamicType) {
            com.android.purebilibili.data.model.response.DynamicType.VIDEO -> {
                // 视频动态: type=1, oid=aid
                val aid = item.modules.module_dynamic?.major?.archive?.aid?.toLongOrNull()
                if (aid != null) Pair(aid, 1) else null
            }
            com.android.purebilibili.data.model.response.DynamicType.DRAW -> {
                // 图片动态: type=11, oid=draw.id  
                val drawId = item.modules.module_dynamic?.major?.draw?.id
                if (drawId != null && drawId > 0) Pair(drawId, 11) else null
            }
            com.android.purebilibili.data.model.response.DynamicType.WORD -> {
                // 纯文字动态: type=17, oid=id_str
                val oid = item.id_str.toLongOrNull()
                if (oid != null) Pair(oid, 17) else null
            }
            com.android.purebilibili.data.model.response.DynamicType.FORWARD -> {
                // 转发动态: 使用原动态的信息
                item.orig?.let { getCommentParams(it) }
            }
            else -> null
        }
    }
    
    /**
     * 🔥 根据动态ID获取动态对象
     */
    private fun findDynamicById(dynamicId: String): DynamicItem? {
        return _uiState.value.items.find { it.id_str == dynamicId }
    }
    
    /**
     * 🔥 打开评论弹窗
     */
    fun openCommentSheet(dynamicId: String) {
        val item = findDynamicById(dynamicId)
        _selectedDynamic.value = item
        if (item != null) {
            loadCommentsForDynamic(item)
        }
    }
    
    /**
     * 🔥 关闭评论弹窗
     */
    fun closeCommentSheet() {
        _selectedDynamic.value = null
        _comments.value = emptyList()
    }
    
    /**
     * 🔥 加载动态评论 (使用正确的 oid 和 type)
     */
    private fun loadCommentsForDynamic(item: DynamicItem) {
        viewModelScope.launch {
            _commentsLoading.value = true
            try {
                val params = getCommentParams(item)
                if (params == null) {
                    com.android.purebilibili.core.util.Logger.e("DynamicVM", "无法获取评论参数: type=${item.type}")
                    return@launch
                }
                val (oid, type) = params
                com.android.purebilibili.core.util.Logger.d("DynamicVM", "加载评论: oid=$oid, type=$type, dynamicType=${item.type}")
                val response = com.android.purebilibili.core.network.NetworkModule.dynamicApi
                    .getDynamicReplies(oid = oid, type = type)
                com.android.purebilibili.core.util.Logger.d("DynamicVM", "评论响应: code=${response.code}, message=${response.message}, replies=${response.data?.replies?.size}")
                if (response.code == 0 && response.data != null) {
                    _comments.value = response.data.replies ?: emptyList()
                } else {
                    com.android.purebilibili.core.util.Logger.e("DynamicVM", "评论加载失败: code=${response.code}, msg=${response.message}")
                }
            } catch (e: Exception) {
                com.android.purebilibili.core.util.Logger.e("DynamicVM", "加载评论异常: ${e.message}")
                e.printStackTrace()
            } finally {
                _commentsLoading.value = false
            }
        }
    }
    
    /**
     * 🔥 加载评论 (兼容旧调用方式)
     */
    fun loadComments(dynamicId: String) {
        val item = findDynamicById(dynamicId)
        if (item != null) {
            loadCommentsForDynamic(item)
        }
    }
    
    /**
     * 🔥 发表评论
     */
    fun postComment(dynamicId: String, message: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            try {
                val csrf = com.android.purebilibili.core.store.TokenManager.csrfCache
                if (csrf.isNullOrEmpty()) {
                    onResult(false, "请先登录")
                    return@launch
                }
                val oid = dynamicId.toLongOrNull() ?: return@launch
                val response = com.android.purebilibili.core.network.NetworkModule.dynamicApi
                    .addDynamicReply(oid = oid, type = 17, message = message, csrf = csrf)
                if (response.code == 0) {
                    onResult(true, "评论成功")
                    // 刷新评论列表
                    loadComments(dynamicId)
                } else {
                    onResult(false, response.message ?: "评论失败")
                }
            } catch (e: Exception) {
                onResult(false, e.message ?: "网络错误")
            }
        }
    }
    
    /**
     * 🔥 点赞动态
     */
    fun likeDynamic(dynamicId: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            try {
                val csrf = com.android.purebilibili.core.store.TokenManager.csrfCache
                if (csrf.isNullOrEmpty()) {
                    onResult(false, "请先登录")
                    return@launch
                }
                val isLiked = _likedDynamics.value.contains(dynamicId)
                val up = if (isLiked) 2 else 1  // 1=点赞, 2=取消
                
                val response = com.android.purebilibili.core.network.NetworkModule.dynamicApi
                    .likeDynamic(dynamicId = dynamicId, up = up, csrf = csrf)
                if (response.code == 0) {
                    // 更新本地状态
                    _likedDynamics.value = if (isLiked) {
                        _likedDynamics.value - dynamicId
                    } else {
                        _likedDynamics.value + dynamicId
                    }
                    onResult(true, if (isLiked) "已取消" else "已点赞")
                } else {
                    onResult(false, response.message ?: "操作失败")
                }
            } catch (e: Exception) {
                onResult(false, e.message ?: "网络错误")
            }
        }
    }
    
    /**
     * 🔥 转发动态
     */
    fun repostDynamic(dynamicId: String, content: String = "", onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            try {
                val csrf = com.android.purebilibili.core.store.TokenManager.csrfCache
                if (csrf.isNullOrEmpty()) {
                    onResult(false, "请先登录")
                    return@launch
                }
                val response = com.android.purebilibili.core.network.NetworkModule.dynamicApi
                    .repostDynamic(dynIdStr = dynamicId, content = content, csrf = csrf)
                if (response.code == 0) {
                    onResult(true, "转发成功")
                } else {
                    onResult(false, response.message ?: "转发失败")
                }
            } catch (e: Exception) {
                onResult(false, e.message ?: "网络错误")
            }
        }
    }
}

/**
 * 🔥 侧边栏用户数据
 */
data class SidebarUser(
    val uid: Long,
    val name: String,
    val face: String,
    val isLive: Boolean = false
)

/**
 * 动态页面 UI 状态
 */
data class DynamicUiState(
    val items: List<DynamicItem> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val hasMore: Boolean = true
)
