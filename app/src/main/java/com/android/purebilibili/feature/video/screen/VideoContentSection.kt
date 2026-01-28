// 文件路径: feature/video/screen/VideoContentSection.kt
package com.android.purebilibili.feature.video.screen

import androidx.compose.ui.geometry.Rect
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.positionChangeIgnoreConsumed
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.purebilibili.core.ui.common.copyOnLongPress
import com.android.purebilibili.data.model.response.RelatedVideo
import com.android.purebilibili.data.model.response.ReplyItem
import com.android.purebilibili.data.model.response.VideoTag
import com.android.purebilibili.data.model.response.ViewInfo
import com.android.purebilibili.feature.video.ui.section.VideoTitleWithDesc
import com.android.purebilibili.feature.video.ui.section.UpInfoSection
import com.android.purebilibili.feature.video.ui.section.ActionButtonsRow
import com.android.purebilibili.feature.video.ui.components.RelatedVideoItem
import com.android.purebilibili.feature.video.ui.components.CollectionRow
import com.android.purebilibili.feature.video.ui.components.CollectionSheet
import com.android.purebilibili.feature.video.ui.components.PagesSelector
import com.android.purebilibili.feature.video.ui.components.CommentSortFilterBar
import com.android.purebilibili.feature.video.ui.components.ReplyItemView
import com.android.purebilibili.feature.video.viewmodel.CommentSortMode
import com.android.purebilibili.feature.dynamic.components.ImagePreviewDialog
import io.github.alexzhirkevich.cupertino.CupertinoActivityIndicator
import io.github.alexzhirkevich.cupertino.icons.CupertinoIcons
import io.github.alexzhirkevich.cupertino.icons.outlined.*
import kotlin.math.abs

/**
 * 视频详情内容区域
 * 从 VideoDetailScreen.kt 提取出来，提高代码可维护性
 */
@Composable
fun VideoContentSection(
    info: ViewInfo,
    relatedVideos: List<RelatedVideo>,
    replies: List<ReplyItem>,
    replyCount: Int,
    emoteMap: Map<String, String>,
    isRepliesLoading: Boolean,
    isRepliesEnd: Boolean = false,
    isFollowing: Boolean,
    isFavorited: Boolean,
    isLiked: Boolean,
    coinCount: Int,
    currentPageIndex: Int,
    downloadProgress: Float = -1f,
    isInWatchLater: Boolean = false,
    followingMids: Set<Long> = emptySet(),
    videoTags: List<VideoTag> = emptyList(),
    sortMode: CommentSortMode = CommentSortMode.HOT,
    upOnlyFilter: Boolean = false,
    onSortModeChange: (CommentSortMode) -> Unit = {},
    onUpOnlyToggle: () -> Unit = {},
    onFollowClick: () -> Unit,
    onFavoriteClick: () -> Unit,
    onLikeClick: () -> Unit,
    onCoinClick: () -> Unit,
    onTripleClick: () -> Unit,
    onPageSelect: (Int) -> Unit,
    onUpClick: (Long) -> Unit,
    onRelatedVideoClick: (String) -> Unit,
    onSubReplyClick: (ReplyItem) -> Unit,
    onLoadMoreReplies: () -> Unit,
    onDownloadClick: () -> Unit = {},
    onWatchLaterClick: () -> Unit = {},
    onTimestampClick: ((Long) -> Unit)? = null,
    onDanmakuSendClick: () -> Unit = {},
    // [新增] 删除与动画参数
    currentMid: Long = 0,
    dissolvingIds: Set<Long> = emptySet(),
    onDeleteComment: (Long) -> Unit = {},
    onDissolveStart: (Long) -> Unit = {},
    // [新增] 点赞回调
    onCommentLike: (Long) -> Unit = {},
    // [新增] 已点赞的评论 ID 集合
    likedComments: Set<Long> = emptySet(),
    // 🔗 [新增] 共享元素过渡开关
    transitionEnabled: Boolean = false
) {
    val tabs = listOf("简介", "评论 $replyCount")
    val pagerState = rememberPagerState(pageCount = { tabs.size })
    val scope = rememberCoroutineScope()
    val introListState = rememberLazyListState()
    val commentListState = rememberLazyListState()
    
    // 评论图片预览状态
    var showImagePreview by remember { mutableStateOf(false) }
    var previewImages by remember { mutableStateOf<List<String>>(emptyList()) }
    var previewInitialIndex by remember { mutableIntStateOf(0) }
    var sourceRect by remember { mutableStateOf<Rect?>(null) }
    
    // 合集展开状态
    var showCollectionSheet by remember { mutableStateOf(false) }

    // 图片预览对话框
    if (showImagePreview && previewImages.isNotEmpty()) {
        ImagePreviewDialog(
            images = previewImages,
            initialIndex = previewInitialIndex,
            sourceRect = sourceRect,
            onDismiss = { showImagePreview = false }
        )
    }
    
    // 合集底部弹窗
    info.ugc_season?.let { season ->
        if (showCollectionSheet) {
            CollectionSheet(
                ugcSeason = season,
                currentBvid = info.bvid,
                onDismiss = { showCollectionSheet = false },
                onEpisodeClick = { episode ->
                    showCollectionSheet = false
                    onRelatedVideoClick(episode.bvid)
                }
            )
        }
    }

    val onTabSelected: (Int) -> Unit = { index ->
        scope.launch { pagerState.animateScrollToPage(index) }
    }

    // 💡 [重构] 使用简单的 Column 布局代替复杂的嵌套滚动
    // 头部和 TabBar 固定在顶部，HorizontalPager 占据剩余空间
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // 头部区域 (Header + TabBar)


        VideoContentTabBar(
            tabs = tabs,
            selectedTabIndex = pagerState.currentPage,
            onTabSelected = onTabSelected,
            onDanmakuSendClick = onDanmakuSendClick,
            modifier = Modifier
        )

        // 内容区域
        HorizontalPager(
            state = pagerState,
            userScrollEnabled = true,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f) // 占据剩余空间
        ) { page ->
            when (page) {
                0 -> VideoIntroTab(
                    listState = introListState,
                    modifier = Modifier,
                    info = info,
                    relatedVideos = relatedVideos,
                    currentPageIndex = currentPageIndex,
                    followingMids = followingMids,
                    videoTags = videoTags,
                    isFollowing = isFollowing,
                    isFavorited = isFavorited,
                    isLiked = isLiked,
                    coinCount = coinCount,
                    downloadProgress = downloadProgress,
                    isInWatchLater = isInWatchLater,
                    onFollowClick = onFollowClick,
                    onFavoriteClick = onFavoriteClick,
                    onLikeClick = onLikeClick,
                    onCoinClick = onCoinClick,
                    onTripleClick = onTripleClick,
                    onPageSelect = onPageSelect,
                    onUpClick = onUpClick,
                    onRelatedVideoClick = onRelatedVideoClick,
                    onOpenCollectionSheet = { showCollectionSheet = true },
                    onDownloadClick = onDownloadClick,
                    onWatchLaterClick = onWatchLaterClick,
                    contentPadding = PaddingValues(bottom = 84.dp), // 适配底部输入栏
                    transitionEnabled = transitionEnabled  // 🔗 传递共享元素开关
                )
                1 -> VideoCommentTab(
                    listState = commentListState,
                    modifier = Modifier,
                    info = info,
                    replies = replies,
                    replyCount = replyCount,
                    emoteMap = emoteMap,
                    isRepliesLoading = isRepliesLoading,
                    isRepliesEnd = isRepliesEnd,
                    videoTags = videoTags,
                    sortMode = sortMode,
                    upOnlyFilter = upOnlyFilter,
                    onSortModeChange = onSortModeChange,
                    onUpOnlyToggle = onUpOnlyToggle,
                    onUpClick = onUpClick,
                    onSubReplyClick = onSubReplyClick,
                    onLoadMoreReplies = onLoadMoreReplies,
                    
                    // [新增] 传递删除相关参数
                    currentMid = currentMid,
                    dissolvingIds = dissolvingIds,
                    onDeleteComment = onDeleteComment,
                    onDissolveStart = onDissolveStart,
                    // [新增] 传递点赞回调
                    onCommentLike = onCommentLike,
                    likedComments = likedComments,

                    onImagePreview = { images, index, rect ->
                        previewImages = images
                        previewInitialIndex = index
                        sourceRect = rect
                        showImagePreview = true
                    },
                    onTimestampClick = onTimestampClick,
                    contentPadding = PaddingValues(bottom = 84.dp) // 适配底部输入栏
                )
            }
        }
    }
}

// ... VideoIntroTab signature ...
@Composable
private fun VideoIntroTab(
    listState: LazyListState,
    modifier: Modifier,
    info: ViewInfo,
    relatedVideos: List<RelatedVideo>,
    currentPageIndex: Int,
    followingMids: Set<Long>,
    videoTags: List<VideoTag>,
    isFollowing: Boolean,
    isFavorited: Boolean,
    isLiked: Boolean,
    coinCount: Int,
    downloadProgress: Float,
    isInWatchLater: Boolean,
    onFollowClick: () -> Unit,
    onFavoriteClick: () -> Unit,
    onLikeClick: () -> Unit,
    onCoinClick: () -> Unit,
    onTripleClick: () -> Unit,
    onPageSelect: (Int) -> Unit,
    onUpClick: (Long) -> Unit,
    onRelatedVideoClick: (String) -> Unit,
    onOpenCollectionSheet: () -> Unit,
    onDownloadClick: () -> Unit,
    onWatchLaterClick: () -> Unit,
    contentPadding: PaddingValues,
    transitionEnabled: Boolean = false  // 🔗 共享元素过渡开关
) {
    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxSize(),
        contentPadding = contentPadding
    ) {
        // 1. 移入的 Header 区域
        item {
            VideoHeaderContent(
                info = info,
                videoTags = videoTags,
                isFollowing = isFollowing,
                isFavorited = isFavorited,
                isLiked = isLiked,
                coinCount = coinCount,
                downloadProgress = downloadProgress,
                isInWatchLater = isInWatchLater,
                onFollowClick = onFollowClick,
                onFavoriteClick = onFavoriteClick,
                onLikeClick = onLikeClick,
                onCoinClick = onCoinClick,
                onTripleClick = onTripleClick,
                onUpClick = onUpClick,
                onOpenCollectionSheet = onOpenCollectionSheet,
                onDownloadClick = onDownloadClick,
                onWatchLaterClick = onWatchLaterClick,
                onGloballyPositioned = { }
            )
        }
        if (info.pages.size > 1) {
            item {
                PagesSelector(
                    pages = info.pages,
                    currentPageIndex = currentPageIndex,
                    onPageSelect = onPageSelect
                )
            }
        }

        item {
            VideoRecommendationHeader()
        }

        items(relatedVideos, key = { it.bvid }) { video ->
            RelatedVideoItem(
                video = video,
                isFollowed = video.owner.mid in followingMids,
                transitionEnabled = transitionEnabled,  // 🔗 传递共享元素开关
                onClick = { onRelatedVideoClick(video.bvid) }
            )
        }
    }
}

// ... VideoCommentTab signature ...
@Composable
private fun VideoCommentTab(
    listState: LazyListState,
    modifier: Modifier,
    info: ViewInfo,
    replies: List<ReplyItem>,
    replyCount: Int,
    emoteMap: Map<String, String>,
    isRepliesLoading: Boolean,
    isRepliesEnd: Boolean,
    videoTags: List<VideoTag>,
    sortMode: CommentSortMode,
    upOnlyFilter: Boolean,
    onSortModeChange: (CommentSortMode) -> Unit,
    onUpOnlyToggle: () -> Unit,
    onUpClick: (Long) -> Unit,
    onSubReplyClick: (ReplyItem) -> Unit,
    onLoadMoreReplies: () -> Unit,
    onImagePreview: (List<String>, Int, Rect?) -> Unit,
    onTimestampClick: ((Long) -> Unit)?,
    contentPadding: PaddingValues,
    // [新增] 参数
    currentMid: Long,
    dissolvingIds: Set<Long>,
    onDeleteComment: (Long) -> Unit,
    onDissolveStart: (Long) -> Unit,
    // [新增] 点赞回调
    onCommentLike: (Long) -> Unit,
    likedComments: Set<Long>
) {
    Box(
        modifier = modifier.fillMaxSize()
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = contentPadding
        ) {
            item {
                CommentSortFilterBar(
                    count = replyCount,
                    sortMode = sortMode,
                    onSortModeChange = onSortModeChange,
                    upOnly = upOnlyFilter,
                    onUpOnlyToggle = onUpOnlyToggle
                )
            }

            if (isRepliesLoading && replies.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                        CupertinoActivityIndicator()
                    }
                }
            } else if (replies.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text(
                            text = if (upOnlyFilter) "这个视频没有 UP 主的评论" else "暂无评论",
                            color = Color.Gray
                        )
                    }
                }
            } else {
                items(items = replies, key = { it.rpid }) { reply ->
                    // [新增] 使用 DissolvableVideoCard 包裹
                    com.android.purebilibili.core.ui.animation.DissolvableVideoCard(
                        isDissolving = reply.rpid in dissolvingIds,
                        onDissolveComplete = { onDeleteComment(reply.rpid) },
                        cardId = "comment_${reply.rpid}",
                        modifier = Modifier.padding(bottom = 1.dp) // 小间距防止裁剪
                    ) {
                        ReplyItemView(
                            item = reply,
                            upMid = info.owner.mid,
                            emoteMap = emoteMap,
                            onClick = {},
                            onSubClick = { onSubReplyClick(reply) },
                            onTimestampClick = onTimestampClick,
                            onImagePreview = { images, index, rect ->
                                onImagePreview(images, index, rect)
                            },
                            // [新增] 点赞事件
                            onLikeClick = { onCommentLike(reply.rpid) },
                            // [修复] 正确传递点赞状态 (API数据 或 本地乐观更新)
                            isLiked = reply.action == 1 || reply.rpid in likedComments,
                            // [新增] 仅当评论 mid 与当前登录用户 mid 一致时显示删除按钮
                            onDeleteClick = if (currentMid > 0 && reply.mid == currentMid) {
                                { onDissolveStart(reply.rpid) }
                            } else null
                        )
                    }
                }

                // 加载更多
                item {
                    val shouldLoadMore by remember(replies.size, replyCount, isRepliesLoading) {
                        derivedStateOf {
                            !isRepliesLoading &&
                                replies.isNotEmpty() &&
                                replies.size < replyCount &&
                                replyCount > 0
                        }
                    }

                    LaunchedEffect(shouldLoadMore) {
                        if (shouldLoadMore) {
                            onLoadMoreReplies()
                        }
                    }

                    Box(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        when {
                            isRepliesLoading -> CupertinoActivityIndicator()
                            isRepliesEnd || replies.size >= replyCount -> {
                                Text("—— end ——", color = Color.Gray, fontSize = 12.sp)
                            }
                            // 当 shouldLoadMore 为 true 时才显示加载指示器
                            shouldLoadMore -> CupertinoActivityIndicator()
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun VideoHeaderContent(
    info: ViewInfo,
    videoTags: List<VideoTag>,
    isFollowing: Boolean,
    isFavorited: Boolean,
    isLiked: Boolean,
    coinCount: Int,
    downloadProgress: Float,
    isInWatchLater: Boolean,
    onFollowClick: () -> Unit,
    onFavoriteClick: () -> Unit,
    onLikeClick: () -> Unit,
    onCoinClick: () -> Unit,
    onTripleClick: () -> Unit,
    onUpClick: (Long) -> Unit,
    onOpenCollectionSheet: () -> Unit,
    onDownloadClick: () -> Unit,
    onWatchLaterClick: () -> Unit,
    onGloballyPositioned: (Float) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface) // 🎨 [修复] 与 TabBar 统一使用 Surface (通常为白色/深灰色)，消除割裂感
            .onGloballyPositioned { coordinates ->
                onGloballyPositioned(coordinates.size.height.toFloat())
            }
    ) {
        UpInfoSection(
            info = info,
            isFollowing = isFollowing,
            onFollowClick = onFollowClick,
            onUpClick = onUpClick
        )

        VideoTitleWithDesc(
            info = info,
            videoTags = videoTags
        )

        ActionButtonsRow(
            info = info,
            isFavorited = isFavorited,
            isLiked = isLiked,
            coinCount = coinCount,
            downloadProgress = downloadProgress,
            isInWatchLater = isInWatchLater,
            onFavoriteClick = onFavoriteClick,
            onLikeClick = onLikeClick,
            onCoinClick = onCoinClick,
            onTripleClick = onTripleClick,
            onCommentClick = {},
            onDownloadClick = onDownloadClick,
            onWatchLaterClick = onWatchLaterClick
        )

        info.ugc_season?.let { season ->
            CollectionRow(
                ugcSeason = season,
                currentBvid = info.bvid,
                onClick = onOpenCollectionSheet
            )
        }
    }
}

/**
 * Tab 栏组件
 */
@Composable
private fun VideoContentTabBar(
    tabs: List<String>,
    selectedTabIndex: Int,
    onTabSelected: (Int) -> Unit,
    onDanmakuSendClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            tabs.forEachIndexed { index, title ->
                val isSelected = selectedTabIndex == index
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clickable { onTabSelected(index) }
                        .padding(vertical = 6.dp, horizontal = 6.dp)
                ) {
                    Text(
                        text = title,
                        fontSize = 14.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) MaterialTheme.colorScheme.primary 
                               else MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        softWrap = false
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Box(
                        modifier = Modifier
                            .width(24.dp)
                            .height(2.dp)
                            .clip(RoundedCornerShape(1.dp))
                            .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
                    )
                }
                if (index < tabs.lastIndex) {
                    Spacer(modifier = Modifier.width(16.dp))
                }
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            // 发弹幕入口
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .clickable { 
                        android.util.Log.d("VideoContentSection", "📤 点我发弹幕 clicked!")
                        onDanmakuSendClick() 
                    }
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "点我发弹幕",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(4.dp))
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "弹",
                        fontSize = 10.sp,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
    }
}

/**
 * 推荐视频标题
 */
@Composable
private fun VideoRecommendationHeader() {
    Row(
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 4.dp), // 优化：减少底部间距，使视频卡片更紧凑
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "相关推荐",
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

/**
 * 视频标签行
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun VideoTagsRow(tags: List<VideoTag>) {
    FlowRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        tags.take(10).forEach { tag ->
            VideoTagChip(tagName = tag.tag_name)
        }
    }
}

/**
 * 视频标签芯片
 */
@Composable
fun VideoTagChip(tagName: String) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f),
        shape = RoundedCornerShape(14.dp)
    ) {
        Text(
            text = tagName,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .padding(horizontal = 12.dp, vertical = 6.dp)
                .copyOnLongPress(tagName, "标签")
        )
    }
}
