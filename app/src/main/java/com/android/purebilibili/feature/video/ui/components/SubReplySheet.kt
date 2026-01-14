// 文件路径: feature/video/SubReplySheet.kt
package com.android.purebilibili.feature.video.ui.components

import com.android.purebilibili.feature.video.viewmodel.SubReplyUiState

import androidx.compose.animation.*
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.purebilibili.core.theme.BiliPink
import com.android.purebilibili.data.model.response.ReplyItem
import io.github.alexzhirkevich.cupertino.CupertinoActivityIndicator

@Composable
fun SubReplySheet(
    state: SubReplyUiState,
    emoteMap: Map<String, String>,
    onDismiss: () -> Unit,
    onLoadMore: () -> Unit,
    onTimestampClick: ((Long) -> Unit)? = null,  //  [新增] 时间戳点击跳转
    onImagePreview: ((List<String>, Int, androidx.compose.ui.geometry.Rect?) -> Unit)? = null  // [问题14修复] 图片预览回调
) {
    //  必须用 Box 包裹，否则 align 报错
    Box(modifier = Modifier.fillMaxSize()) {

        AnimatedVisibility(
            visible = state.visible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.7f))  // [修复] 增加透明度减少背景干扰
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onDismiss() }
            )
        }

        //  iOS 风格弹性滑入动画
        AnimatedVisibility(
            visible = state.visible && state.rootReply != null,
            enter = slideInVertically(
                initialOffsetY = { it },
                animationSpec = spring(
                    dampingRatio = 0.7f,  // 较低阻尼创造弹性效果
                    stiffness = 400f
                )
            ) + fadeIn(animationSpec = tween(200)),
            exit = slideOutVertically(
                targetOffsetY = { it },
                animationSpec = tween(250)
            ) + fadeOut(animationSpec = tween(150)),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.8f)
                    .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {},
                color = MaterialTheme.colorScheme.background
            ) {
                if (state.rootReply != null) {
                    SubReplyList(
                        rootReply = state.rootReply!!,
                        subReplies = state.items,
                        isLoading = state.isLoading,
                        isEnd = state.isEnd,
                        emoteMap = emoteMap,
                        onLoadMore = onLoadMore,
                        onTimestampClick = onTimestampClick,
                        upMid = state.upMid,  // [修复] 使用正确的 UP 主 mid
                        onImagePreview = onImagePreview  // [问题14修复] 传递图片预览回调
                    )
                }
            }
        }
    }
}

@Composable
fun SubReplyList(
    rootReply: ReplyItem,
    subReplies: List<ReplyItem>,
    isLoading: Boolean,
    isEnd: Boolean,
    emoteMap: Map<String, String>,
    onLoadMore: () -> Unit,
    onTimestampClick: ((Long) -> Unit)? = null,
    upMid: Long = 0,  //  UP主 mid 用于 UP 标签
    onImagePreview: ((List<String>, Int, androidx.compose.ui.geometry.Rect?) -> Unit)? = null  // [问题14修复] 图片预览回调
) {
    val listState = rememberLazyListState()
    val shouldLoadMore by remember {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val lastVisible = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            lastVisible >= layoutInfo.totalItemsCount - 2 && !isLoading && !isEnd
        }
    }
    LaunchedEffect(shouldLoadMore) { if (shouldLoadMore) onLoadMore() }

    Column(modifier = Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
            Text(
                "评论详情",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)

        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            item {
                ReplyItemView(
                    item = rootReply,
                    upMid = upMid,  //  传递 UP 主 mid
                    emoteMap = emoteMap, 
                    onClick = {}, 
                    onSubClick = {},
                    onTimestampClick = onTimestampClick,
                    onImagePreview = onImagePreview,  // [问题14修复] 传递图片预览回调
                    hideSubPreview = true  // [修复] 隐藏楼中楼预览，避免重复显示
                )
                HorizontalDivider(thickness = 8.dp, color = MaterialTheme.colorScheme.surfaceContainerHigh)
            }
            items(subReplies) { item ->
                ReplyItemView(
                    item = item,
                    upMid = upMid,  //  传递 UP 主 mid
                    emoteMap = emoteMap, 
                    onClick = {}, 
                    onSubClick = {},
                    onTimestampClick = onTimestampClick,
                    onImagePreview = onImagePreview  // [问题14修复] 传递图片预览回调
                )
            }
            item {
                Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                    when {
                        isLoading -> CupertinoActivityIndicator()
                        isEnd -> Text("没有更多回复了", color = MaterialTheme.colorScheme.outline, fontSize = 12.sp)
                        else -> TextButton(onClick = onLoadMore) { Text("加载更多", color = MaterialTheme.colorScheme.primary) }
                    }
                }
            }
        }
    }
}