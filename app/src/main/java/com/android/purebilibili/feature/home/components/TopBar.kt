// 文件路径: feature/home/components/TopBar.kt
package com.android.purebilibili.feature.home.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState

import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
//  Cupertino Icons - iOS SF Symbols 风格图标
import io.github.alexzhirkevich.cupertino.icons.CupertinoIcons
import io.github.alexzhirkevich.cupertino.icons.outlined.*
import io.github.alexzhirkevich.cupertino.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.*
import androidx.compose.foundation.ExperimentalFoundationApi // [Added]
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.android.purebilibili.core.util.FormatUtils
import com.android.purebilibili.feature.home.UserState
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import androidx.compose.ui.platform.LocalDensity
import kotlinx.coroutines.flow.map
import com.android.purebilibili.core.ui.animation.rememberDampedDragAnimationState
import com.android.purebilibili.core.ui.animation.horizontalDragGesture
import androidx.compose.foundation.combinedClickable // [Added]
import com.android.purebilibili.core.ui.animation.horizontalDragGesture
import com.android.purebilibili.core.ui.animation.rememberDampedDragAnimationState

/**
 * Q弹点击效果
 */
fun Modifier.premiumClickable(onClick: () -> Unit): Modifier = composed {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1f,
        label = "scale"
    )
    this
        .scale(scale)
        .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
}

/**
 *  iOS 风格悬浮顶栏
 * - 不贴边，有水平边距
 * - 圆角 + 毛玻璃效果
 */
@Composable
fun FluidHomeTopBar(
    user: UserState,
    onAvatarClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onSearchClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
    ) {
        //  悬浮式导航栏容器 - 增强视觉层次
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,  //  使用主题色，适配深色模式
            shadowElevation = 6.dp,  // 添加阴影增加层次感
            tonalElevation = 0.dp,
            border = androidx.compose.foundation.BorderStroke(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp) // 稍微减小高度
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                //  左侧：头像
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .premiumClickable { onAvatarClick() }
                        .border(1.dp, MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                ) {
                    if (user.isLogin && user.face.isNotEmpty()) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(FormatUtils.fixImageUrl(user.face))
                                .crossfade(true).build(),
                            contentDescription = "Avatar",
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Box(
                            Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("未", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                //  中间：搜索框
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(36.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .clickable { onSearchClick() }
                        .padding(horizontal = 12.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            CupertinoIcons.Default.MagnifyingGlass,
                            null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.5f),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "搜索视频、UP主...",
                            style = MaterialTheme.typography.bodyMedium,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            maxLines = 1
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))
                
                //  右侧：设置按钮
                IconButton(
                    onClick = onSettingsClick,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        CupertinoIcons.Default.Gear,
                        contentDescription = "Settings",
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    }
}

/**
 *  [HIG] iOS 风格分类标签栏
 * - 限制可见标签为 4 个主要分类 (HIG 建议 3-5 个)
 * - 其余分类收入"更多"下拉菜单
 * - 圆角胶囊选中指示器
 * - 最小触摸目标 44pt
 */
/**
 *  [HIG] iOS 风格可滑动分类标签栏 (Liquid Glass Style)
 * - 移除"更多"菜单，所有分类水平平铺
 * - 支持水平惯性滚动
 * - 液态玻璃选中指示器 (变长胶囊)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryTabRow(
    categories: List<String> = listOf("推荐", "关注", "热门", "直播", "追番", "影视", "游戏", "知识", "科技"),
    selectedIndex: Int = 0,
    onCategorySelected: (Int) -> Unit = {},
    onPartitionClick: () -> Unit = {},
    pagerState: androidx.compose.foundation.pager.PagerState? = null // [New] PagerState for sync
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val unselectedColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)

    //  [交互优化] 触觉反馈
    val haptic = com.android.purebilibili.core.util.rememberHapticFeedback()
    val scrollChannel = com.android.purebilibili.feature.home.LocalHomeScrollChannel.current

    // [Restored] Re-enable custom Damped Drag for better touch feel
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .padding(horizontal = 4.dp), 
        verticalAlignment = Alignment.CenterVertically
    ) {
        // [Refactor] 使用 BoxWithConstraints 动态计算宽度
        BoxWithConstraints(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
        ) {
            // 计算每个 Tab 的宽度
            val visibleTabCount = 5
            val tabWidth = maxWidth / visibleTabCount
            
            // [Restored] 阻尼拖拽状态
            val coroutineScope = rememberCoroutineScope()
            // 使用 DampedDragAnimationState 管理内部状态
            val dampedDragState = rememberDampedDragAnimationState(
                initialIndex = selectedIndex.coerceIn(0, visibleTabCount - 1),
                itemCount = visibleTabCount,
                onIndexChanged = { index ->
                    // 当 Tab 内部拖拽结束时，同步到 Pager
                    if (pagerState != null && pagerState.currentPage != index) {
                        coroutineScope.launch { pagerState.animateScrollToPage(index) }
                    }
                    onCategorySelected(index)
                }
            )
            

            // [Fix] Always update internal state when external selection changes
            // This ensures instant visual feedback on Click, even if Pager is lagging
            LaunchedEffect(selectedIndex) {
                 dampedDragState.updateIndex(selectedIndex)
            }

            // [Restored] Sync Pager -> DragState
            // 当 Pager 被外部（滑动内容区）滚动时，同步更新 DragState
            val isPagerDragging by pagerState?.interactionSource?.collectIsDraggedAsState() ?: remember { mutableStateOf(false) }
            
            // [Fix Issue 2] 区分 "用户滑动内容" (Fling) 和 "代码滚动内容" (Animate)
            // 只有用户交互引起的 Pager 滚动才应该驱动 Indicator
            var isPagerInteracting by remember { mutableStateOf(false) }
            
            LaunchedEffect(isPagerDragging) {
                if (isPagerDragging) isPagerInteracting = true
            }
            LaunchedEffect(pagerState?.isScrollInProgress) {
                if (pagerState?.isScrollInProgress == false) {
                     isPagerInteracting = false
                }
            }
            
            // 监听 Pager 变化，仅在 "用户滑动内容" 期间同步状态
            // [Fix Issue 1] 恢复持续同步，但仅限 isPagerInteracting 为真时
            LaunchedEffect(pagerState?.currentPage, pagerState?.currentPageOffsetFraction, isPagerInteracting) {
                if (pagerState == null) return@LaunchedEffect
                
                // 如果是用户在滑动内容，我们将 DampedState 持续 Snap 到 Pager 位置
                // 这样当滑动停止时(isPagerInteracting -> false)，DampedState 已经在正确位置，无缝切换
                if (isPagerInteracting && !dampedDragState.isDragging && !dampedDragState.isRunning) {
                    val page = pagerState.currentPage
                    val offset = pagerState.currentPageOffsetFraction
                    val targetPosition = page + offset
                    dampedDragState.snapTo(targetPosition)
                }
            }

            // Source of truth Priority:
            // 1. Tab Interaction (Drag/Spring) -> Trusted
            // 2. Content Interaction (Swipe/Fling) -> Trusted
            // 3. Static/Catch-up -> DampedState (Stable)
            
            val currentPositionState = remember(dampedDragState, pagerState, isPagerInteracting) {
                derivedStateOf {
                    val isTabActive = dampedDragState.isDragging || dampedDragState.isRunning
                    
                    if (isTabActive) {
                        dampedDragState.value
                    } else if (isPagerInteracting && pagerState != null) {
                         // Zero Latency Direct Read
                        pagerState.currentPage + pagerState.currentPageOffsetFraction
                    } else {
                        dampedDragState.value
                    }
                }
            }

            // Tabs Container
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    // [Restored] Apply gesture to the bar itself
                    .horizontalDragGesture(
                        dragState = dampedDragState, 
                        itemWidthPx = with(LocalDensity.current) { tabWidth.toPx() }
                    )
            ) {
                 // 1. [Layer] Background Liquid Indicator
                 com.android.purebilibili.feature.home.components.SimpleLiquidIndicator(
                    positionState = currentPositionState,
                    itemWidth = tabWidth,
                    isDragging = dampedDragState.isDragging || (pagerState?.isScrollInProgress == true),
                    modifier = Modifier.align(Alignment.CenterStart)
                 )

                 // 2. [Layer] Content Tabs
                Row(
                   modifier = Modifier.height(48.dp),
                   verticalAlignment = Alignment.CenterVertically
                ) {
                    // 只渲染可见的 5 个标签
                    categories.take(visibleTabCount).forEachIndexed { index, category ->
                        Box(
                            modifier = Modifier.width(tabWidth),
                            contentAlignment = Alignment.Center
                        ) {
                            CategoryTabItem(
                                category = category,
                                index = index,
                                selectedIndex = selectedIndex,
                                currentPositionState = currentPositionState,
                                primaryColor = primaryColor,
                                unselectedColor = unselectedColor,
                                onClick = { 
                                    onCategorySelected(index)
                                    haptic(com.android.purebilibili.core.util.HapticType.LIGHT)
                                    // Click usually triggers Pager scroll via onCategorySelected -> ViewModel -> HomeScreen LaunchedEffect
                                },
                                onDoubleTap = {
                                    if (selectedIndex == index) {
                                        scrollChannel?.trySend(Unit)
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.width(4.dp))
        
        //  分区按钮
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(22.dp))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { onPartitionClick() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                CupertinoIcons.Default.ListBullet,
                contentDescription = "浏览全部分区",
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                modifier = Modifier.size(20.dp)
            )
        }
        
        Spacer(modifier = Modifier.width(8.dp))
    }
}


@Composable
fun CategoryTabItem(
    category: String,
    index: Int,
    selectedIndex: Int,
    currentPositionState: State<Float>,
    primaryColor: Color,
    unselectedColor: Color,
    onClick: () -> Unit,
    onDoubleTap: () -> Unit = {}
) {
     // [Optimized] Calculate fraction from the passed state inside derivedStateOf
     // Defer reading state until drawing phase where possible
     val selectionFraction by remember {
         derivedStateOf {
             val distance = kotlin.math.abs(currentPositionState.value - index)
             (1f - distance).coerceIn(0f, 1f)
         }
     }

     // [Optimized] Removed Color Interpolation to avoid Recomposition
     // val targetTextColor = androidx.compose.ui.graphics.lerp(unselectedColor, primaryColor, selectionFraction)
     
     // [Updated] Louder Scale Effect
     val smoothFraction = androidx.compose.animation.core.FastOutSlowInEasing.transform(selectionFraction)
     val targetScale = androidx.compose.ui.util.lerp(1.0f, 1.25f, smoothFraction)
     
     // Font weight change still triggers relayout, but it's discrete (only happens at 0.6 threshold)
     // This is acceptable as it doesn't happen every frame.
     val fontWeight = if (selectionFraction > 0.6f) FontWeight.SemiBold else FontWeight.Medium

     val haptic = com.android.purebilibili.core.util.rememberHapticFeedback()

     Box(
         modifier = Modifier
             .clip(RoundedCornerShape(16.dp)) 
             .combinedClickable(
                 interactionSource = remember { MutableInteractionSource() },
                 indication = null,
                 onClick = { onClick() },
                 onDoubleClick = onDoubleTap
             )
             .padding(horizontal = 10.dp, vertical = 6.dp), 
         contentAlignment = Alignment.Center
     ) {
         // [Optimization] Double Text Layer with Alpha Cross-fade
         // This avoids creating a new TextLayoutResult every frame due to color change.
         // Layer 1: Unselected Text (Base)
         Text(
             text = category,
             color = unselectedColor,
             fontSize = 15.sp,
             fontWeight = fontWeight,
             modifier = Modifier.graphicsLayer {
                 scaleX = targetScale
                 scaleY = targetScale
                 alpha = 1f - selectionFraction // Fade out as selected
                 transformOrigin = androidx.compose.ui.graphics.TransformOrigin.Center
             }
         )
         
         // Layer 2: Selected Text (Overlay)
         Text(
             text = category,
             color = primaryColor,
             fontSize = 15.sp,
             fontWeight = fontWeight,
             modifier = Modifier.graphicsLayer {
                 scaleX = targetScale
                 scaleY = targetScale
                 alpha = selectionFraction // Fade in as selected
                 transformOrigin = androidx.compose.ui.graphics.TransformOrigin.Center
             }
         )
     }
}
