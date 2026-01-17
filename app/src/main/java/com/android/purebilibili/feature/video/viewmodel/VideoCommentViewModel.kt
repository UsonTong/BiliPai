package com.android.purebilibili.feature.video.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.purebilibili.data.model.response.ReplyItem
import com.android.purebilibili.data.repository.CommentRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

//  [修复] 评论排序模式
// 根据 Bilibili WBI API 文档 (x/v2/reply/wbi/main)：mode=3(按热度), mode=2(按时间)
// 旧版 API (x/v2/reply): sort=0(按时间), sort=1(按点赞), sort=2(按回复数)
enum class CommentSortMode(val apiMode: Int, val label: String) {
    HOT(3, "最热"),       // 按热度排序 (mode=3, 默认)
    NEWEST(2, "最新"),    // 按时间排序（最新优先）(mode=2)
    REPLY(1, "回复最多")  // [新增] 按回复数排序 (使用旧版 API sort=2)
}

// 评论状态
data class CommentUiState(
    val replies: List<ReplyItem> = emptyList(),
    val isRepliesLoading: Boolean = false,
    val replyCount: Int = 0,
    val repliesError: String? = null,
    val isRepliesEnd: Boolean = false,
    val nextPage: Int = 1,
    //  [新增] 排序和筛选状态
    val sortMode: CommentSortMode = CommentSortMode.HOT,
    val upOnlyFilter: Boolean = false,
    val upMid: Long = 0,  // UP主的 mid，用于筛选
    // [新增] 评论交互状态
    val isSending: Boolean = false,
    val sendError: String? = null,
    val replyTarget: ReplyItem? = null,  // 回复目标评论（为空则是一级评论）
    val likedComments: Set<Long> = emptySet(),  // 已点赞的评论 rpid 集合
    val hatedComments: Set<Long> = emptySet()   // 已点踩的评论 rpid 集合
)

// 二级评论状态 (从 PlayerViewModel 移过来)
data class SubReplyUiState(
    val visible: Boolean = false,
    val rootReply: ReplyItem? = null,
    val items: List<ReplyItem> = emptyList(),
    val isLoading: Boolean = false,
    val page: Int = 1,
    val isEnd: Boolean = false,
    val error: String? = null,
    val upMid: Long = 0  // [新增] UP 主 mid，用于显示 UP 标签
)

class VideoCommentViewModel : ViewModel() {
    private val _commentState = MutableStateFlow(CommentUiState())
    val commentState = _commentState.asStateFlow()

    private val _subReplyState = MutableStateFlow(SubReplyUiState())
    val subReplyState = _subReplyState.asStateFlow()

    private var currentAid: Long = 0
    
    //  存储原始评论列表（未经筛选），用于筛选切换
    private var allReplies: List<ReplyItem> = emptyList()

    // 初始化/重置
    fun init(aid: Long, upMid: Long = 0) {
        android.util.Log.d("CommentVM", " init called with aid=$aid, upMid=$upMid, currentAid=$currentAid")
        if (currentAid == aid && _commentState.value.upMid == upMid) return
        currentAid = aid
        allReplies = emptyList()
        _commentState.value = CommentUiState(upMid = upMid)
        loadComments()
    }
    
    //  [新增] 设置 UP 主 mid（用于只看UP主筛选）
    fun setUpMid(mid: Long) {
        if (_commentState.value.upMid != mid) {
            _commentState.value = _commentState.value.copy(upMid = mid)
        }
    }

    //  [修复] 切换排序模式 - 与"只看UP主"互斥
    fun setSortMode(mode: CommentSortMode) {
        val currentState = _commentState.value
        if (currentState.sortMode == mode && !currentState.upOnlyFilter) return
        
        android.util.Log.d("CommentVM", " setSortMode: ${currentState.sortMode} -> $mode, clearing upOnlyFilter")
        
        //  [修复] 切换排序时清除"只看UP主"筛选
        allReplies = emptyList()
        _commentState.value = CommentUiState(
            sortMode = mode,
            upOnlyFilter = false,  //  互斥：清除 UP 筛选
            upMid = currentState.upMid
        )
        loadComments()
    }
    
    //  [修复] 切换只看UP主筛选 - 与"最热/最新"互斥
    fun toggleUpOnly() {
        val currentState = _commentState.value
        val newUpOnly = !currentState.upOnlyFilter
        
        android.util.Log.d("CommentVM", " toggleUpOnly: $newUpOnly, upMid=${currentState.upMid}")
        
        if (newUpOnly) {
            //  [修复] 开启 UP 筛选时，从当前已加载的评论中筛选
            val filteredReplies = if (currentState.upMid > 0) {
                allReplies.filter { it.mid == currentState.upMid }
            } else {
                emptyList()
            }
            
            _commentState.value = currentState.copy(
                upOnlyFilter = true,
                replies = filteredReplies
            )
        } else {
            //  关闭 UP 筛选时，恢复显示所有评论
            _commentState.value = currentState.copy(
                upOnlyFilter = false,
                replies = allReplies
            )
        }
    }

    fun loadComments() {
        val currentState = _commentState.value
        if (currentState.isRepliesEnd || currentState.isRepliesLoading) return

        _commentState.value = currentState.copy(isRepliesLoading = true, repliesError = null)

        viewModelScope.launch {
            val pageToLoad = currentState.nextPage
            //  使用当前排序模式
            val result = CommentRepository.getComments(
                aid = currentAid, 
                page = pageToLoad, 
                ps = 20,
                mode = currentState.sortMode.apiMode
            )

            result.onSuccess { data ->
                val current = _commentState.value
                val newReplies = data.replies ?: emptyList()
                
                //  [新增] 第一页时添加置顶评论到列表开头
                val topReplies = if (pageToLoad == 1) data.collectTopReplies() else emptyList()
                
                //  合并到原始列表（置顶评论在前）
                val combinedReplies = if (pageToLoad == 1) {
                    (topReplies + newReplies).distinctBy { it.rpid }
                } else {
                    (allReplies + newReplies).distinctBy { it.rpid }
                }
                allReplies = combinedReplies
                
                //  [修复] 使用统一方法获取评论总数和结束标志 (兼容 WBI 和旧版 API)
                val totalCount = data.getAllCount()
                val isNoNewReplies = newReplies.isEmpty()
                val isEnd = data.getIsEnd(pageToLoad, combinedReplies.size) || isNoNewReplies
                
                android.util.Log.d("CommentVM", " loadComments result: page=$pageToLoad, new=${newReplies.size}, top=${topReplies.size}, total=${allReplies.size}, allCount=$totalCount, isEnd=$isEnd")
                
                //  [修复] 加载后重新应用筛选（确保排序切换后筛选仍生效）
                val filteredReplies = if (current.upOnlyFilter && current.upMid > 0) {
                    combinedReplies.filter { it.mid == current.upMid }
                } else {
                    combinedReplies
                }
                
                _commentState.value = current.copy(
                    replies = filteredReplies,
                    replyCount = totalCount,
                    isRepliesLoading = false,
                    repliesError = null,
                    isRepliesEnd = isEnd,
                    nextPage = pageToLoad + 1
                )
            }.onFailure { e ->
                android.util.Log.e("CommentVM", " loadComments error: ${e.message}")
                _commentState.value = _commentState.value.copy(
                    isRepliesLoading = false,
                    repliesError = e.message ?: "加载评论失败",
                    isRepliesEnd = true  //  [修复] 出错时也标记为结束，防止无限重试
                )
            }
        }
    }

    // --- 二级评论逻辑 ---

    fun openSubReply(rootReply: ReplyItem) {
        _subReplyState.value = SubReplyUiState(
            visible = true,
            rootReply = rootReply,
            isLoading = true,
            page = 1,
            upMid = _commentState.value.upMid  // [修复] 使用正确的 UP 主 mid
        )
        loadSubReplies(rootReply.oid, rootReply.rpid, 1)
    }

    fun closeSubReply() {
        _subReplyState.value = _subReplyState.value.copy(visible = false)
    }

    fun loadMoreSubReplies() {
        val state = _subReplyState.value
        if (state.isLoading || state.isEnd || state.rootReply == null) return
        val nextPage = state.page + 1
        _subReplyState.value = state.copy(isLoading = true)
        loadSubReplies(state.rootReply.oid, state.rootReply.rpid, nextPage)
    }

    private fun loadSubReplies(oid: Long, rootId: Long, page: Int) {
        viewModelScope.launch {
            val result = CommentRepository.getSubComments(oid, rootId, page)
            result.onSuccess { data ->
                val current = _subReplyState.value
                val newItems = data.replies ?: emptyList()
                val isEnd = data.cursor.isEnd || newItems.isEmpty()

                _subReplyState.value = current.copy(
                    items = if (page == 1) newItems else (current.items + newItems).distinctBy { it.rpid },
                    isLoading = false,
                    page = page,
                    isEnd = isEnd,
                    error = null
                )
            }.onFailure {
                _subReplyState.value = _subReplyState.value.copy(
                    isLoading = false,
                    error = it.message
                )
            }
        }
    }
    
    // --- [新增] 评论交互逻辑 ---
    
    fun sendComment(message: String) {
        if (message.isBlank()) return
        val currentState = _commentState.value
        if (currentState.isSending) return
        
        _commentState.value = currentState.copy(isSending = true, sendError = null)
        
        viewModelScope.launch {
            val replyTarget = currentState.replyTarget
            val root = replyTarget?.rpid ?: 0
            val parent = replyTarget?.rpid ?: 0
            
            val result = CommentRepository.addComment(currentAid, message, root, parent)
            
            result.onSuccess { newReply ->
                val current = _commentState.value
                val updatedReplies = if (newReply != null && replyTarget == null) {
                    listOf(newReply) + allReplies
                } else allReplies
                allReplies = updatedReplies
                
                _commentState.value = current.copy(
                    replies = updatedReplies,
                    isSending = false,
                    sendError = null,
                    replyTarget = null,
                    replyCount = current.replyCount + 1
                )
            }.onFailure { e ->
                _commentState.value = _commentState.value.copy(isSending = false, sendError = e.message)
            }
        }
    }
    
    fun replyTo(reply: ReplyItem) {
        _commentState.value = _commentState.value.copy(replyTarget = reply)
    }
    
    fun cancelReply() {
        _commentState.value = _commentState.value.copy(replyTarget = null)
    }
    
    fun likeComment(rpid: Long) {
        val currentState = _commentState.value
        val isCurrentlyLiked = rpid in currentState.likedComments
        val newLikedComments = if (isCurrentlyLiked) currentState.likedComments - rpid else currentState.likedComments + rpid
        val newHatedComments = currentState.hatedComments - rpid
        _commentState.value = currentState.copy(likedComments = newLikedComments, hatedComments = newHatedComments)
        
        viewModelScope.launch {
            CommentRepository.likeComment(currentAid, rpid, !isCurrentlyLiked).onFailure {
                _commentState.value = _commentState.value.copy(likedComments = currentState.likedComments, hatedComments = currentState.hatedComments)
            }
        }
    }
    
    fun hateComment(rpid: Long) {
        val currentState = _commentState.value
        val isCurrentlyHated = rpid in currentState.hatedComments
        val newHatedComments = if (isCurrentlyHated) currentState.hatedComments - rpid else currentState.hatedComments + rpid
        val newLikedComments = currentState.likedComments - rpid
        _commentState.value = currentState.copy(likedComments = newLikedComments, hatedComments = newHatedComments)
        
        viewModelScope.launch {
            CommentRepository.hateComment(currentAid, rpid, !isCurrentlyHated).onFailure {
                _commentState.value = _commentState.value.copy(likedComments = currentState.likedComments, hatedComments = currentState.hatedComments)
            }
        }
    }
    
    fun deleteComment(rpid: Long) {
        viewModelScope.launch {
            CommentRepository.deleteComment(currentAid, rpid).onSuccess {
                allReplies = allReplies.filter { it.rpid != rpid }
                val current = _commentState.value
                _commentState.value = current.copy(replies = allReplies, replyCount = maxOf(0, current.replyCount - 1))
            }
        }
    }
    
    fun reportComment(rpid: Long, reason: Int, content: String = "") {
        viewModelScope.launch { CommentRepository.reportComment(currentAid, rpid, reason, content) }
    }
}

