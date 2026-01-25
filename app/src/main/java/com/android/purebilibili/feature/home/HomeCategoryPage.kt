package com.android.purebilibili.feature.home

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.android.purebilibili.core.ui.animation.DissolvableVideoCard
import com.android.purebilibili.core.ui.animation.jiggleOnDissolve
import com.android.purebilibili.core.util.responsiveContentWidth
import com.android.purebilibili.data.model.response.VideoItem
import com.android.purebilibili.feature.home.components.cards.ElegantVideoCard
import com.android.purebilibili.feature.home.components.cards.LiveRoomCard
import com.android.purebilibili.feature.home.components.cards.StoryVideoCard
import com.android.purebilibili.core.store.SettingsManager
import com.android.purebilibili.core.util.CardPositionManager
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.alexzhirkevich.cupertino.CupertinoActivityIndicator
import androidx.compose.ui.Alignment
import kotlinx.coroutines.delay

@Composable
fun HomeCategoryPageContent(
    category: HomeCategory,
    categoryState: CategoryContent,
    gridState: LazyGridState,
    gridColumns: Int,
    contentPadding: PaddingValues,
    dissolvingVideos: Set<String>,
    followingMids: Set<Long>,
    onVideoClick: (String, Long, String) -> Unit,
    onLiveClick: (Long, String, String) -> Unit,
    onLoadMore: () -> Unit,
    onDismissVideo: (String) -> Unit,
    onWatchLater: (String, Long) -> Unit,
    onDissolveComplete: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    // Read necessary settings
    val homeSettings by SettingsManager.getHomeSettings(context).collectAsState(
        initial = com.android.purebilibili.core.store.HomeSettings()
    )
    val displayMode = homeSettings.displayMode
    val cardAnimationEnabled = homeSettings.cardAnimationEnabled
    val cardTransitionEnabled = homeSettings.cardTransitionEnabled
    val isDataSaverActive = remember { SettingsManager.isDataSaverActive(context) }
    
    // Check for load more
    val shouldLoadMore by remember {
        derivedStateOf {
            val layoutInfo = gridState.layoutInfo
            val totalItems = layoutInfo.totalItemsCount
            val lastVisibleItemIndex = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            totalItems > 0 && lastVisibleItemIndex >= totalItems - 4 && !categoryState.isLoading && categoryState.hasMore
        }
    }
    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore) onLoadMore()
    }

    // [新增] 处理从详情页返回时的状态清理 (原逻辑位于 Animations.kt)
    // 确保返回时跳过初始卡片的入场动画，但允许后续加载的卡片动画
    LaunchedEffect(Unit) {
        if (CardPositionManager.isReturningFromDetail) {
             delay(100)
             CardPositionManager.clearReturning()
        }
        if (CardPositionManager.isSwitchingCategory) {
             delay(300)
             CardPositionManager.isSwitchingCategory = false
        }
    }

    LazyVerticalGrid(
        state = gridState,
        columns = GridCells.Fixed(gridColumns),
        contentPadding = contentPadding,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
    ) {
        if (category == HomeCategory.LIVE) {
            // Live Category Content
            
            // 1. Followed Live Rooms
            if (categoryState.followedLiveRooms.isNotEmpty()) {
                item(span = { GridItemSpan(gridColumns) }) {
                    Text(
                        text = "关注",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp)
                    )
                }
                
                itemsIndexed(
                    items = categoryState.followedLiveRooms,
                    key = { _, room -> "followed_${room.roomid}" },
                    contentType = { _, _ -> "live_room" }
                ) { index, room ->
                    LiveRoomCard(
                        room = room,
                        index = index,
                        onClick = { onLiveClick(room.roomid, room.title, room.uname) } 
                    )
                }
            }
            
            // 2. Popular Live Rooms
            if (categoryState.liveRooms.isNotEmpty()) {
                item(span = { GridItemSpan(gridColumns) }) {
                    Text(
                        text = "热门",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp)
                    )
                }
                
                itemsIndexed(
                    items = categoryState.liveRooms,
                    key = { _, room -> "popular_${room.roomid}" },
                    contentType = { _, _ -> "live_room" }
                ) { index, room ->
                    LiveRoomCard(
                        room = room,
                        index = index,
                        onClick = { onLiveClick(room.roomid, room.title, room.uname) } 
                    )
                }
            }
        } else {
            // Video Category Content
            if (categoryState.videos.isNotEmpty()) {
                itemsIndexed(
                    items = categoryState.videos,
                    key = { _, video -> video.bvid },
                    contentType = { _, _ -> "video" }
                ) { index, video ->
                    val isDissolving = video.bvid in dissolvingVideos
                    
                    //  使用可消散卡片容器包装
                    DissolvableVideoCard(
                        isDissolving = isDissolving,
                        onDissolveComplete = { onDissolveComplete(video.bvid) },
                        cardId = video.bvid,
                        modifier = Modifier.jiggleOnDissolve(video.bvid)
                    ) {
                        // Display Mode Logic
                        when (displayMode) {
                            1 -> {
                                StoryVideoCard(
                                    video = video,
                                    index = index,
                                    animationEnabled = cardAnimationEnabled,
                                    transitionEnabled = cardTransitionEnabled,
                                    onDismiss = { onDismissVideo(video.bvid) },
                                    onClick = { bvid, cid -> onVideoClick(bvid, cid, video.pic) }
                                )
                            }
                            else -> {
                                ElegantVideoCard(
                                    video = video,
                                    index = index,
                                    isFollowing = video.owner.mid in followingMids && category != HomeCategory.FOLLOW,
                                    animationEnabled = cardAnimationEnabled,
                                    transitionEnabled = cardTransitionEnabled,
                                    isDataSaverActive = isDataSaverActive,
                                    onDismiss = { onDismissVideo(video.bvid) },
                                    onWatchLater = { onWatchLater(video.bvid, video.id) }, 
                                    onClick = { bvid, cid -> onVideoClick(bvid, cid, video.pic) }
                                )
                            }
                        }
                    }
                }
            }
        }

        // Loading Indicator at bottom
        if (categoryState.isLoading || categoryState.hasMore) {
             item(span = { GridItemSpan(gridColumns) }) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (categoryState.isLoading) {
                        CupertinoActivityIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            }
        }
        
        // Spacer
        item(span = { GridItemSpan(gridColumns) }) {
            Box(modifier = Modifier.fillMaxWidth().height(20.dp))
        }
    }
}
