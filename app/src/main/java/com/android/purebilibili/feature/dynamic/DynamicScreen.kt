// 文件路径: feature/dynamic/DynamicScreen.kt
package com.android.purebilibili.feature.dynamic

import android.os.Build
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import com.android.purebilibili.core.ui.BiliGradientButton
import com.android.purebilibili.core.ui.EmptyState
import com.android.purebilibili.core.ui.LoadingAnimation

import com.android.purebilibili.feature.dynamic.components.DynamicCardV2
import com.android.purebilibili.feature.dynamic.components.DynamicSidebar
import com.android.purebilibili.feature.dynamic.components.DynamicTopBarWithTabs
import com.android.purebilibili.feature.dynamic.components.DynamicDisplayMode
import com.android.purebilibili.feature.dynamic.components.DynamicCommentSheet
import com.android.purebilibili.feature.dynamic.components.RepostDialog

/**
 * 🔥 动态页面 - 支持两种布局模式
 * 
 * 1. SIDEBAR 模式：UP 主列表在左侧边栏
 * 2. HORIZONTAL 模式：UP 主列表在顶部横向滚动
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DynamicScreen(
    viewModel: DynamicViewModel = viewModel(),
    onVideoClick: (String) -> Unit,
    onUserClick: (Long) -> Unit = {},
    onLiveClick: (roomId: Long, title: String, uname: String) -> Unit = { _, _, _ -> },
    onBack: () -> Unit,
    onLoginClick: () -> Unit = {},
    onHomeClick: () -> Unit = {}
) {
    val state by viewModel.uiState.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val listState = rememberLazyListState()
    
    // 侧边栏状态
    val followedUsers by viewModel.followedUsers.collectAsState()
    val selectedUserId by viewModel.selectedUserId.collectAsState()
    val isSidebarExpanded by viewModel.isSidebarExpanded.collectAsState()
    
    // 🔥🔥 [新增] 评论/点赞/转发状态
    val selectedDynamicId by viewModel.selectedDynamicId.collectAsState()
    val comments by viewModel.comments.collectAsState()
    val commentsLoading by viewModel.commentsLoading.collectAsState()
    val likedDynamics by viewModel.likedDynamics.collectAsState()
    var showRepostDialog by remember { mutableStateOf<String?>(null) }  // 存储要转发的动态ID
    
    // Tab 选择
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("全部", "视频")
    
    // 🔥 布局模式状态（侧边栏/横向）
    var displayMode by remember { mutableStateOf(DynamicDisplayMode.SIDEBAR) }
    
    val density = LocalDensity.current
    val statusBarHeight = WindowInsets.statusBars.getTop(density).let { with(density) { it.toDp() } }
    val pullRefreshState = rememberPullToRefreshState()
    
    // GIF 图片加载器
    val context = LocalContext.current
    val gifImageLoader = remember {
        ImageLoader.Builder(context)
            .components {
                if (Build.VERSION.SDK_INT >= 28) {
                    add(ImageDecoderDecoder.Factory())
                } else {
                    add(GifDecoder.Factory())
                }
            }
            .crossfade(true)
            .build()
    }
    
    // 过滤动态
    val filteredItems = remember(state.items, selectedTab, selectedUserId) {
        var items = state.items
        if (selectedTab == 1) {
            items = items.filter { it.type == "DYNAMIC_TYPE_AV" }
        }
        selectedUserId?.let { uid ->
            items = items.filter { it.modules.module_author?.mid == uid }
        }
        items.distinctBy { it.id_str }
    }
    
    // 加载更多
    val shouldLoadMore by remember {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val totalItems = layoutInfo.totalItemsCount
            val lastVisibleItemIndex = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            totalItems > 0 && lastVisibleItemIndex >= totalItems - 3 && !state.isLoading && state.hasMore
        }
    }
    LaunchedEffect(shouldLoadMore) { if (shouldLoadMore) viewModel.loadMore() }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        // 🔥 [新增] 模式切换动画
        AnimatedContent(
            targetState = displayMode,
            transitionSpec = {
                // 🔥 根据切换方向使用不同动画
                val slideDirection = if (targetState == DynamicDisplayMode.HORIZONTAL) {
                    // 从侧边栏切换到横向：向左滑出+淡出，向左滑入+淡入
                    (slideInHorizontally { -it / 4 } + fadeIn(animationSpec = tween(300))) togetherWith
                    (slideOutHorizontally { it / 4 } + fadeOut(animationSpec = tween(200)))
                } else {
                    // 从横向切换到侧边栏：向右滑出+淡出，向右滑入+淡入
                    (slideInHorizontally { it / 4 } + fadeIn(animationSpec = tween(300))) togetherWith
                    (slideOutHorizontally { -it / 4 } + fadeOut(animationSpec = tween(200)))
                }
                slideDirection.using(SizeTransform(clip = false))
            },
            label = "displayModeTransition"
        ) { targetMode ->
            // 🔥 根据布局模式选择不同布局
            when (targetMode) {
                DynamicDisplayMode.SIDEBAR -> {
                    // 侧边栏模式
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding)
                    ) {
                        // 左侧边栏
                        DynamicSidebar(
                            users = followedUsers,
                            selectedUserId = selectedUserId,
                            isExpanded = isSidebarExpanded,
                            onUserClick = { viewModel.selectUser(it) },
                            onToggleExpand = { viewModel.toggleSidebar() },
                            modifier = Modifier.padding(top = statusBarHeight)
                        )
                        
                        // 右侧内容区
                        PullToRefreshBox(
                            isRefreshing = isRefreshing,
                            onRefresh = { viewModel.refresh() },
                            state = pullRefreshState,
                            modifier = Modifier.fillMaxSize().weight(1f)
                        ) {
                            DynamicList(
                                state = state,
                                filteredItems = filteredItems,
                                listState = listState,
                                statusBarHeight = statusBarHeight,
                                topPaddingExtra = 100.dp,  // 顶栏高度
                                onVideoClick = onVideoClick,
                                onUserClick = onUserClick,
                                onLiveClick = onLiveClick,
                                onLoginClick = onLoginClick,
                                gifImageLoader = gifImageLoader,
                                onCommentClick = { viewModel.openCommentSheet(it) },
                                onRepostClick = { showRepostDialog = it },
                                onLikeClick = { dynamicId ->
                                    viewModel.likeDynamic(dynamicId) { _, msg ->
                                        android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_SHORT).show()
                                    }
                                },
                                likedDynamics = likedDynamics
                            )
                            
                            // 顶栏
                            DynamicTopBarWithTabs(
                                selectedTab = selectedTab,
                                tabs = tabs,
                                onTabSelected = { selectedTab = it },
                                displayMode = displayMode,
                                onDisplayModeChange = { displayMode = it },
                                onBackClick = onHomeClick,
                                modifier = Modifier.align(Alignment.TopCenter)
                            )
                            
                            // 错误提示
                            ErrorOverlay(state, onLoginClick, { viewModel.refresh() }, Modifier.align(Alignment.Center))
                        }
                    }
                }
                
                DynamicDisplayMode.HORIZONTAL -> {
                    // 横向模式（UP 主列表在顶部）
                    PullToRefreshBox(
                        isRefreshing = isRefreshing,
                        onRefresh = { viewModel.refresh() },
                        state = pullRefreshState,
                        modifier = Modifier.fillMaxSize().padding(padding)
                    ) {
                        DynamicList(
                            state = state,
                            filteredItems = filteredItems,
                            listState = listState,
                            statusBarHeight = statusBarHeight,
                            topPaddingExtra = 220.dp,  // 顶栏 + 横向用户列表高度
                            onVideoClick = onVideoClick,
                            onUserClick = onUserClick,
                            onLiveClick = onLiveClick,
                            onLoginClick = onLoginClick,
                            gifImageLoader = gifImageLoader,
                            onCommentClick = { viewModel.openCommentSheet(it) },
                            onRepostClick = { showRepostDialog = it },
                            onLikeClick = { dynamicId ->
                                viewModel.likeDynamic(dynamicId) { _, msg ->
                                    android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_SHORT).show()
                                }
                            },
                            likedDynamics = likedDynamics
                        )
                        
                        // 顶部区域：顶栏 + 横向用户列表
                        Column(modifier = Modifier.align(Alignment.TopCenter)) {
                            DynamicTopBarWithTabs(
                                selectedTab = selectedTab,
                                tabs = tabs,
                                onTabSelected = { selectedTab = it },
                                displayMode = displayMode,
                                onDisplayModeChange = { displayMode = it },
                                onBackClick = onHomeClick
                            )
                            
                            // 🔥 横向 UP 主列表
                            HorizontalUserList(
                                users = followedUsers,
                                selectedUserId = selectedUserId,
                                onUserClick = { viewModel.selectUser(it) }
                            )
                        }
                        
                        ErrorOverlay(state, onLoginClick, { viewModel.refresh() }, Modifier.align(Alignment.Center))
                    }
                }
            }
        }
    }
    
    // 🔥🔥 [新增] 评论弹窗
    selectedDynamicId?.let { dynamicId ->
        DynamicCommentSheet(
            comments = comments,
            isLoading = commentsLoading,
            onDismiss = { viewModel.closeCommentSheet() },
            onPostComment = { message ->
                viewModel.postComment(dynamicId, message) { success, msg ->
                    android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_SHORT).show()
                }
            }
        )
    }
    
    // 🔥🔥 [新增] 转发弹窗
    showRepostDialog?.let { dynamicId ->
        RepostDialog(
            onDismiss = { showRepostDialog = null },
            onRepost = { content ->
                viewModel.repostDynamic(dynamicId, content) { success, msg ->
                    android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_SHORT).show()
                    if (success) showRepostDialog = null
                }
            }
        )
    }
}

/**
 * 🔥 动态列表内容
 */
@Composable
private fun DynamicList(
    state: DynamicUiState,
    filteredItems: List<com.android.purebilibili.data.model.response.DynamicItem>,
    listState: androidx.compose.foundation.lazy.LazyListState,
    statusBarHeight: androidx.compose.ui.unit.Dp,
    topPaddingExtra: androidx.compose.ui.unit.Dp,
    onVideoClick: (String) -> Unit,
    onUserClick: (Long) -> Unit,
    onLiveClick: (Long, String, String) -> Unit,
    onLoginClick: () -> Unit,
    gifImageLoader: ImageLoader,
    // 🔥🔥 [新增] 动态操作回调
    onCommentClick: (String) -> Unit = {},
    onRepostClick: (String) -> Unit = {},
    onLikeClick: (String) -> Unit = {},
    likedDynamics: Set<String> = emptySet()
) {
    LazyColumn(
        state = listState,
        contentPadding = PaddingValues(
            top = statusBarHeight + topPaddingExtra,
            bottom = 80.dp
        ),
        modifier = Modifier.fillMaxSize()
    ) {
        // 空状态
        if (filteredItems.isEmpty() && !state.isLoading && state.error == null) {
            item {
                EmptyState(
                    message = "暂无动态",
                    actionText = "登录后查看关注 UP主 的动态",
                    modifier = Modifier.height(300.dp)
                )
            }
        }
        
        // 动态卡片列表
        items(filteredItems, key = { "dynamic_${it.id_str}" }) { item ->
            DynamicCardV2(
                item = item,
                onVideoClick = onVideoClick,
                onUserClick = onUserClick,
                onLiveClick = onLiveClick,
                gifImageLoader = gifImageLoader,
                onCommentClick = onCommentClick,
                onRepostClick = onRepostClick,
                onLikeClick = onLikeClick,
                isLiked = likedDynamics.contains(item.id_str)
            )
            // 分隔线 - 增加间距
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                thickness = 0.5.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
            )
        }
        
        // 加载中
        if (state.isLoading && state.items.isNotEmpty()) {
            item {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    LoadingAnimation(size = 40.dp)
                }
            }
        }
        
        // 没有更多
        if (!state.hasMore && filteredItems.isNotEmpty()) {
            item {
                Text(
                    "没有更多了",
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.5f),
                    fontSize = 13.sp
                )
            }
        }
    }
}

/**
 * 🔥 横向 UP 主列表（Telegram 风格）
 */
@Composable
private fun HorizontalUserList(
    users: List<SidebarUser>,
    selectedUserId: Long?,
    onUserClick: (Long?) -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 1.dp
    ) {
        LazyRow(
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 🔥 [简化] 移除「全部」按钮，直接显示 UP 主头像列表
            // UP 主头像列表
            items(users, key = { it.uid }) { user ->
                val isSelected = selectedUserId == user.uid
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clickable { onUserClick(user.uid) }
                        .padding(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .then(
                                if (isSelected) 
                                    Modifier.border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                                else 
                                    Modifier
                            )
                    ) {
                        AsyncImage(
                            model = coil.request.ImageRequest.Builder(LocalContext.current)
                                .data(user.face.let { if (it.startsWith("http://")) it.replace("http://", "https://") else it })
                                .crossfade(true)
                                .build(),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize().clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                        
                        // 🔥 在线状态
                        if (user.isLive) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .size(12.dp)
                                    .background(androidx.compose.ui.graphics.Color.White, CircleShape)
                                    .padding(2.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(androidx.compose.ui.graphics.Color.Red, CircleShape)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        user.name.take(4),  // 最多显示4个字符
                        fontSize = 11.sp,
                        color = if (isSelected) 
                            MaterialTheme.colorScheme.primary 
                        else 
                            MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

/**
 * 错误提示覆盖层
 */
@Composable
private fun ErrorOverlay(
    state: DynamicUiState,
    onLoginClick: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (state.error != null && state.items.isEmpty()) {
        Column(
            modifier = modifier,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(state.error ?: "", color = MaterialTheme.colorScheme.error)
            Spacer(modifier = Modifier.height(16.dp))
            if (state.error?.contains("未登录") == true) {
                BiliGradientButton(text = "去登录", onClick = onLoginClick)
            } else {
                BiliGradientButton(text = "重试", onClick = onRetry)
            }
        }
    }
}
