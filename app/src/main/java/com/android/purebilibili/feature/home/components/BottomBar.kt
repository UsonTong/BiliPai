// 文件路径: feature/home/components/BottomBar.kt
package com.android.purebilibili.feature.home.components

// Duplicate import removed
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.combinedClickable  // [新增] 组合点击支持
import androidx.compose.foundation.ExperimentalFoundationApi // [新增]
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer  //  晃动动画
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.android.purebilibili.feature.home.components.LiquidIndicator
import com.android.purebilibili.navigation.ScreenRoutes
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlin.math.abs
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.draw.alpha
import com.android.purebilibili.core.ui.blur.unifiedBlur
import com.android.purebilibili.core.ui.blur.BlurStyles
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.materials.ExperimentalHazeMaterialsApi
import dev.chrisbanes.haze.materials.HazeMaterials
import com.android.purebilibili.core.util.HapticType
import com.android.purebilibili.core.util.rememberHapticFeedback
import com.android.purebilibili.core.theme.iOSSystemGray
import com.android.purebilibili.core.theme.BottomBarColors  // 统一底栏颜色配置
import com.android.purebilibili.core.theme.BottomBarColorPalette  // 调色板
import com.android.purebilibili.core.theme.LocalCornerRadiusScale
import com.android.purebilibili.core.theme.iOSCornerRadius
import kotlinx.coroutines.launch  //  延迟导航
//  Cupertino Icons - iOS SF Symbols 风格图标
import io.github.alexzhirkevich.cupertino.icons.CupertinoIcons
import io.github.alexzhirkevich.cupertino.icons.outlined.*
import io.github.alexzhirkevich.cupertino.icons.filled.*
import com.android.purebilibili.core.ui.animation.rememberDampedDragAnimationState
import com.android.purebilibili.core.ui.animation.horizontalDragGesture
import com.android.purebilibili.feature.home.components.LiquidIndicator
import com.android.purebilibili.feature.home.components.SimpleLiquidIndicator
// [Removed] internal import for rememberLayerBackdrop
import androidx.compose.ui.Modifier.Companion.then
import dev.chrisbanes.haze.hazeSource

/**
 * 底部导航项枚举 -  使用 iOS SF Symbols 风格图标
 * [HIG] 所有图标包含 contentDescription 用于无障碍访问
 */
enum class BottomNavItem(
    val label: String,
    val selectedIcon: @Composable () -> Unit,
    val unselectedIcon: @Composable () -> Unit,
    val route: String // [新增] 路由地址
) {
    HOME(
        "首页",
        { Icon(CupertinoIcons.Filled.House, contentDescription = "首页") },
        { Icon(CupertinoIcons.Outlined.House, contentDescription = "首页") },
        ScreenRoutes.Home.route
    ),
    DYNAMIC(
        "动态",
        { Icon(CupertinoIcons.Filled.BellBadge, contentDescription = "动态") },
        { Icon(CupertinoIcons.Outlined.Bell, contentDescription = "动态") },
        ScreenRoutes.Dynamic.route
    ),
    STORY(
        "短视频",
        { Icon(CupertinoIcons.Filled.PlayCircle, contentDescription = "短视频") },
        { Icon(CupertinoIcons.Outlined.PlayCircle, contentDescription = "短视频") },
        ScreenRoutes.Story.route
    ),
    HISTORY(
        "历史",
        { Icon(CupertinoIcons.Filled.Clock, contentDescription = "历史记录") },
        { Icon(CupertinoIcons.Outlined.Clock, contentDescription = "历史记录") },
        ScreenRoutes.History.route
    ),
    PROFILE(
        "我的",
        { Icon(CupertinoIcons.Filled.PersonCircle, contentDescription = "个人中心") },
        { Icon(CupertinoIcons.Outlined.Person, contentDescription = "个人中心") },
        ScreenRoutes.Profile.route
    ),
    FAVORITE(
        "收藏",
        { Icon(CupertinoIcons.Filled.Star, contentDescription = "收藏夹") },
        { Icon(CupertinoIcons.Outlined.Star, contentDescription = "收藏夹") },
        ScreenRoutes.Favorite.route
    ),
    LIVE(
        "直播",
        { Icon(CupertinoIcons.Filled.Video, contentDescription = "直播") },
        { Icon(CupertinoIcons.Outlined.Video, contentDescription = "直播") },
        ScreenRoutes.LiveList.route
    ),
    WATCHLATER(
        "稍后看",
        { Icon(CupertinoIcons.Filled.Bookmark, contentDescription = "稀后再看") },
        { Icon(CupertinoIcons.Outlined.Bookmark, contentDescription = "稀后再看") },
        ScreenRoutes.WatchLater.route
    ),
    SETTINGS(
        "设置",
        { Icon(CupertinoIcons.Filled.Gearshape, contentDescription = "设置") },
        { Icon(CupertinoIcons.Default.Gearshape, contentDescription = "设置") },
        ScreenRoutes.Settings.route
    )
}

/**
 *  iOS 风格磨砂玻璃底部导航栏
 * 
 * 特性：
 * - 实时磨砂玻璃效果 (使用 Haze 库)
 * - 悬浮圆角设计
 * - 自动适配深色/浅色模式
 * -  点击触觉反馈
 */
@OptIn(ExperimentalHazeMaterialsApi::class)
@Composable
fun FrostedBottomBar(
    currentItem: BottomNavItem = BottomNavItem.HOME,
    onItemClick: (BottomNavItem) -> Unit,
    modifier: Modifier = Modifier,
    hazeState: HazeState? = null,
    isFloating: Boolean = true,
    labelMode: Int = 1,  //  0=图标+文字, 1=仅图标, 2=仅文字
    onHomeDoubleTap: () -> Unit = {},  //  双击首页回到顶部
    visibleItems: List<BottomNavItem> = listOf(BottomNavItem.HOME, BottomNavItem.DYNAMIC, BottomNavItem.HISTORY, BottomNavItem.PROFILE),  //  [新增] 可配置的可见项目
    itemColorIndices: Map<String, Int> = emptyMap(),  //  [新增] 项目颜色索引映射
    onToggleSidebar: (() -> Unit)? = null  // 📱 [平板适配] 切换到侧边栏
) {
    val isDarkTheme = MaterialTheme.colorScheme.background.red < 0.5f
    val haptic = rememberHapticFeedback()  //  触觉反馈
    
    // 🔒 [防抖] 防止快速点击导致页面重复加载
    var lastClickTime by remember { mutableStateOf(0L) }
    val debounceClick: (BottomNavItem, () -> Unit) -> Unit = remember {
        { item, action ->
            val currentTime = System.currentTimeMillis()
            // 200ms 防抖
            if (currentTime - lastClickTime > 200) {
                lastClickTime = currentTime
                action()
            }
        }
    }
    
    // 📐 [平板适配] 检测屏幕尺寸
    val windowSizeClass = com.android.purebilibili.core.util.LocalWindowSizeClass.current
    val isTablet = windowSizeClass.isTablet
    
    //  读取当前模糊强度以确定背景透明度
    val context = androidx.compose.ui.platform.LocalContext.current
    val blurIntensity by com.android.purebilibili.core.store.SettingsManager.getBlurIntensity(context)
        .collectAsState(initial = com.android.purebilibili.core.ui.blur.BlurIntensity.THIN)
    val barColor = resolveBottomBarSurfaceColor(
        surfaceColor = MaterialTheme.colorScheme.surface,
        blurEnabled = hazeState != null,
        blurIntensity = blurIntensity
    )

    // 📐 [平板适配] 根据 labelMode 和屏幕尺寸动态计算高度
    val floatingHeight = when (labelMode) {
        0 -> if (isTablet) 76.dp else 70.dp   // 图标+文字 (加大: 64->70)
        2 -> if (isTablet) 56.dp else 54.dp   // 仅文字 (加大: 48->54)
        else -> if (isTablet) 68.dp else 62.dp // 仅图标 (加大: 56->62)
    }
    val dockedHeight = when (labelMode) {
        0 -> if (isTablet) 72.dp else 72.dp   // 图标+文字 (66 -> 72)
        2 -> if (isTablet) 52.dp else 56.dp   // 仅文字 (50 -> 56)
        else -> if (isTablet) 64.dp else 64.dp // 仅图标 (58 -> 64)
    }
    
    // 📐 [平板适配] 图标大小
    val iconSize = if (isTablet) 30.dp else 26.dp
    val iconWithTextSize = if (isTablet) 28.dp else 24.dp
    
    //  根据样式计算垂直偏移以确保视觉居中
    //  正值向下偏移，负值向上偏移
    val contentVerticalOffset = when {
        isFloating && labelMode == 0 -> 0.dp   // 悬浮+图标文字：完全居中 (3->0)
        isFloating && labelMode == 1 -> 2.dp   // 悬浮+仅图标：向下偏移
        isFloating && labelMode == 2 -> 2.dp   // 悬浮+仅文字：向下偏移
        !isFloating && labelMode == 0 -> 2.dp  // 贴边+图标文字：微调偏移 (4->2)
        !isFloating && labelMode == 1 -> 0.dp  // 贴边+仅图标：完全居中 (3->0)
        !isFloating && labelMode == 2 -> 0.dp  // 贴边+仅文字：完全居中 (2->0)
        else -> 0.dp
    }
    
    // 📐 [平板适配] 水平间距
    val barHorizontalPadding = if (isFloating) (if (isTablet) 40.dp else 24.dp) else 0.dp
    val barBottomPadding = if (isFloating) (if (isTablet) 20.dp else 16.dp) else 0.dp
    // [新增] 获取圆角缩放比例
    val cornerRadiusScale = LocalCornerRadiusScale.current
    val floatingCornerRadius = iOSCornerRadius.Floating * cornerRadiusScale  // 28.dp * scale + 8
    val barShape = if (isFloating) RoundedCornerShape(floatingCornerRadius + 8.dp) else androidx.compose.ui.graphics.RectangleShape  // iOS 风格动态圆角

    // [Restore] 内部 backdropState，用于折射底栏自身内容（文字/图标）
    // [修改] 使用外部传入的 backdrop （全屏内容折射源）
    
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = barHorizontalPadding)
            .padding(bottom = barBottomPadding)
            .then(if (isFloating) Modifier.navigationBarsPadding() else Modifier),
        contentAlignment = Alignment.BottomCenter // 确保内容居中
    ) {
        //  [修复] hazeEffect 应用于外层 Box，绘制模糊背景
        //  Surface 保持透明作为内容容器，这样模糊效果不会被遮盖
        Box(
            modifier = Modifier
                .then(
                    if (isFloating) {
                         Modifier
                            .widthIn(max = 640.dp) // [平板适配] 限制最大宽度
                            .shadow(
                                elevation = 8.dp,
                                shape = barShape,
                                ambientColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
                                spotColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                            )
                            .height(floatingHeight)
                    } else {
                        Modifier // Docked 高度由内容撑开
                    }
                )
                .fillMaxWidth()
                .clip(barShape)
                .then(if (hazeState != null) Modifier.unifiedBlur(hazeState) else Modifier)
                .background(barColor)
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                //  Surface 透明，让外层 Box 的 hazeEffect 显示
                color = Color.Transparent,
                shape = barShape,
                shadowElevation = 0.dp,
                border = if (hazeState != null) {
                    //  iOS 风格边框
                    if (!isFloating) {
                        androidx.compose.foundation.BorderStroke(
                            width = 0.5.dp,
                            brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f),
                                    Color.Transparent
                                )
                            )
                        )
                    } else {
                        androidx.compose.foundation.BorderStroke(
                            width = 0.5.dp,
                            brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.35f),
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                                )
                            )
                        )
                    }
                } else {
                    androidx.compose.foundation.BorderStroke(
                        width = 0.5.dp,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                    )
                }
            ) {
            //  Telegram 风格滑动指示器
            val itemCount = visibleItems.size  //  [修改] 使用可见项目数
            val selectedIndex = visibleItems.indexOf(currentItem)  //  [修改] 使用可见项目索引
            
            //  iOS 风格：内容区固定高度，导航栏区域作为 padding 包含在 Surface 内
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    // [已移除] 移除 layerBackdrop 防止循环依赖和渲染闪烁
                    // .layerBackdrop(backdropState)
            ) {
                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(
                            if (isFloating) Modifier.fillMaxHeight()
                            else Modifier.height(dockedHeight)
                        )
                ) {
                //  考虑 Row 的 padding 后的实际可用宽度
                //  [调整] 增加 padding 以防止指示器贴边 (12dp -> 20dp)
                //  指示器溢出宽度为 12dp (24dp/2), 所以 20dp padding 会留下 8dp 的间隙
                val rowPadding = 20.dp
                val actualContentWidth = maxWidth - (rowPadding * 2)
                val itemWidth = actualContentWidth / itemCount
                
                //  Telegram 风格滑动指示器
                //  [新增] 阻尼拖拽动画状态
                val dampedDragState = rememberDampedDragAnimationState(
                    initialIndex = if (selectedIndex >= 0) selectedIndex else 0,
                    itemCount = itemCount,
                    onIndexChanged = { index -> 
                        if (index in visibleItems.indices) {
                            onItemClick(visibleItems[index])
                        }
                    }
                )
                
                // [修复] 当选中项不在底栏中时（如设置页面），隐藏指示器
                val isValidSelection = selectedIndex >= 0
                val indicatorAlpha by animateFloatAsState(
                    targetValue = if (isValidSelection) 1f else 0f,
                    label = "indicatorAlpha"
                )
                
                //  同步外部状态变化 (点击切换时)
                LaunchedEffect(selectedIndex) {
                    if (isValidSelection) {
                        dampedDragState.updateIndex(selectedIndex)
                    }
                }

                //  [重构] 布局结构：
                //  1. 内容层 (Row) -> 标记为 backdrop 源 (放在底层)
                //  2. 滤镜层 (LiquidIndicator) -> 使用 backdrop 源进行折射 (放在顶层)
                
                // [新增] 恢复 Backdrop 状态
                
                // [修改] 移除 Haze/Backdrop，使用普通的层级叠加，指示器使用 Primary 颜色半透明
                Box(modifier = Modifier.fillMaxSize()) {
                    // 1. [底层] 内容层
                    BottomBarContent(
                        visibleItems = visibleItems,
                        selectedIndex = selectedIndex,
                        itemColorIndices = itemColorIndices,
                        onItemClick = onItemClick,
                        onToggleSidebar = onToggleSidebar,
                        isTablet = isTablet,
                        labelMode = labelMode,
                        hazeState = hazeState,
                        haptic = haptic,
                        debounceClick = debounceClick,
                        onHomeDoubleTap = onHomeDoubleTap,
                        itemWidth = itemWidth,
                        rowPadding = rowPadding,
                        contentVerticalOffset = contentVerticalOffset,
                        isInteractive = true,
                        currentPosition = dampedDragState.value,
                        dragModifier = Modifier.horizontalDragGesture(
                            dragState = dampedDragState,
                            itemWidthPx = with(LocalDensity.current) { itemWidth.toPx() }
                        )
                   )

                    // 2. [顶层] 液态指示器 (无折射)
                    LiquidIndicator(
                        position = dampedDragState.value,
                        itemWidth = itemWidth,
                        itemCount = itemCount,
                        isDragging = dampedDragState.isDragging,
                        velocity = dampedDragState.velocity,
                        startPadding = rowPadding,
                        modifier = Modifier
                            .fillMaxSize()
                            .offset(y = contentVerticalOffset) 
                            .alpha(indicatorAlpha),
                    )
                }
            } // BoxWithConstraints 闭合
                
                //  iOS 风格：非悬浮模式时，导航栏区域作为 Spacer 包含在 Surface 内
                if (!isFloating) {
                    Spacer(
                        modifier = Modifier
                            .fillMaxWidth()
                            .windowInsetsPadding(WindowInsets.navigationBars)
                    )
                }
            }  //  Column 闭合
        }
    }
    }
}

internal fun resolveBottomBarSurfaceColor(
    surfaceColor: Color,
    blurEnabled: Boolean,
    blurIntensity: com.android.purebilibili.core.ui.blur.BlurIntensity
): Color {
    val alpha = if (blurEnabled) {
        BlurStyles.getBackgroundAlpha(blurIntensity)
    } else {
        1f
    }
    return surfaceColor.copy(alpha = alpha)
}

@Composable
private fun BottomBarContent(
    visibleItems: List<BottomNavItem>,
    selectedIndex: Int,
    itemColorIndices: Map<String, Int>,
    onItemClick: (BottomNavItem) -> Unit,
    onToggleSidebar: (() -> Unit)?,
    isTablet: Boolean,
    labelMode: Int,
    hazeState: HazeState?,
    haptic: (HapticType) -> Unit,
    debounceClick: (BottomNavItem, () -> Unit) -> Unit,
    onHomeDoubleTap: () -> Unit,
    itemWidth: Dp,
    rowPadding: Dp,
    contentVerticalOffset: Dp,
    isInteractive: Boolean,
    currentPosition: Float, // [新增] 当前指示器位置，用于动态插值
    dragModifier: Modifier = Modifier
) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .then(dragModifier)
            .padding(horizontal = rowPadding),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // [平板适配] ... (保持不变，省略以简化 diff，实际需完整保留)
        // 为保持 diff 简洁且正确，这里只修改 visibleItems 循环部分
        // 平板侧边栏按钮逻辑可以保持现状，因为它不参与 currentPosition 计算（它是额外的）
        // 但为了完整性，我们需要确保 BottomBarContent 的完整代码。
        
        // 由于 multi_replace 限制，我必须提供完整的 BottomBarContent。
        // ... (平板按钮代码) 
        if (isTablet && onToggleSidebar != null) {
            // ... (复制原有逻辑)
            // 简单复制：
             var isPending by remember { mutableStateOf(false) }
            val primaryColor = MaterialTheme.colorScheme.primary
            val unselectedColor = if (hazeState != null) {
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            } else {
                BottomBarColors.UNSELECTED
            }
            val iconColor by animateColorAsState(targetValue = if (isPending) primaryColor else unselectedColor, label = "iconColor")

            Column(
                modifier = Modifier.weight(1f).fillMaxHeight().offset(y = contentVerticalOffset)
                    .then(if (isInteractive) Modifier.clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { isPending = true; haptic(HapticType.LIGHT); kotlinx.coroutines.MainScope().launch { kotlinx.coroutines.delay(100); onToggleSidebar(); isPending = false } } else Modifier),
                horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center
            ) {
                Box(modifier = Modifier.size(26.dp)) {
                    Icon(imageVector = CupertinoIcons.Outlined.SidebarLeft, contentDescription = "侧边栏", tint = iconColor, modifier = Modifier.fillMaxSize())
                }
                if (labelMode == 0) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(text = "侧边栏", style = MaterialTheme.typography.labelSmall, color = iconColor, fontWeight = FontWeight.Medium, fontSize = 10.sp)
                }
            }
        }
        
        visibleItems.forEachIndexed { index, item ->
            val isSelected = selectedIndex == index
            val itemColorIndex = itemColorIndices[item.name] ?: 0
            
            // [核心逻辑] 计算每个 Item 的选中分数 (0f..1f)
            // 根据当前位置 currentPosition 和 item index 的距离计算
            // 距离 < 1 时开始变色，距离 0 时完全变色
            val distance = abs(currentPosition - index)
            val selectionFraction = (1f - distance).coerceIn(0f, 1f)
            
            BottomBarItem(
                item = item,
                isSelected = isSelected, // 仅用于点击逻辑判断
                selectionFraction = selectionFraction, // [新增] 用于驱动样式
                onClick = { if (isInteractive) onItemClick(item) },
                labelMode = labelMode,
                colorIndex = itemColorIndex,
                iconSize = if (labelMode == 0) 24.dp else 26.dp,
                contentVerticalOffset = contentVerticalOffset,
                modifier = Modifier.weight(1f),
                hazeState = hazeState,
                haptic = haptic,
                debounceClick = debounceClick,
                onHomeDoubleTap = onHomeDoubleTap
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun BottomBarItem(
    item: BottomNavItem,
    isSelected: Boolean,
    selectionFraction: Float, // [新增] 0f..1f
    onClick: () -> Unit,
    labelMode: Int,
    colorIndex: Int,
    iconSize: androidx.compose.ui.unit.Dp,
    contentVerticalOffset: androidx.compose.ui.unit.Dp,
    modifier: Modifier = Modifier,
    hazeState: HazeState?,
    haptic: (HapticType) -> Unit,
    debounceClick: (BottomNavItem, () -> Unit) -> Unit,
    onHomeDoubleTap: () -> Unit
) {
    var isPending by remember { mutableStateOf(false) }
    
    val primaryColor = MaterialTheme.colorScheme.primary
    val unselectedColor = if (hazeState != null) {
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
    } else {
        BottomBarColors.UNSELECTED
    }
    
    // [修改] 颜色插值：根据 selectionFraction 在 unselected 和 selected 之间混合
    // 还要考虑 isPending (点击态)
    val targetIconColor = androidx.compose.ui.graphics.lerp(
        unselectedColor, 
        primaryColor, 
        if (isPending) 1f else selectionFraction
    )
    
    // 仍然使用 animateColorAsState 但目标值现在是动态插值的
    // 使用较快的动画以跟手，或者直接使用 lerp 结果如果非常平滑
    // 为了平滑过渡，这里使用 FastOutSlowIn 且时间短
    val iconColor by animateColorAsState(
        targetValue = targetIconColor,
        animationSpec = androidx.compose.animation.core.tween(durationMillis = 100), // 快速响应
        label = "iconColor"
    )
    
    // [修改] 缩放插值 - 跃动效果
    // selectionFraction: 0f (未选中) -> 1f (完全选中)
    // 这里的逻辑是：当指示器经过时 (0.5f) 图标最大，两端 (0f/1f) 恢复正常
    // 使用 sin(x * PI) 曲线：sin(0)=0, sin(0.5PI)=1, sin(PI)=0
    // 基础大小 1.0f，最大放大 1.4f (增强版)
    val scaleMultiplier = 0.4f
    val bumpScale = 1.0f + (scaleMultiplier * kotlin.math.sin(selectionFraction * Math.PI)).toFloat()
    
    // 直接使用计算出的 bumpScale 作为 scale，因为 selectionFraction 本身已经是平滑动画的值 (由 dampedDragState 驱动)
    // 这样可以保证图标缩放绝对跟随手指/指示器位置，没有任何滞后
    val scale = bumpScale
    
    // [修改] Y轴位移插值
    val targetBounceY = androidx.compose.ui.util.lerp(0f, 0f, selectionFraction)
    val bounceY by animateFloatAsState(
        targetValue = targetBounceY,
        animationSpec = spring(dampingRatio = 0.4f, stiffness = 400f),
        label = "bounceY"
    )
    
    //  晃动角度 (保持不变)
    var wobbleAngle by remember { mutableFloatStateOf(0f) }
    val animatedWobble by animateFloatAsState(
        targetValue = wobbleAngle,
        animationSpec = spring(dampingRatio = 0.2f, stiffness = 600f),
        label = "wobble"
    )
    
    LaunchedEffect(wobbleAngle) {
        if (wobbleAngle != 0f) {
            kotlinx.coroutines.delay(50)
            wobbleAngle = 0f
        }
    }
    
    Column(
        modifier = modifier
            .fillMaxHeight()
            .offset(y = contentVerticalOffset)
            .then(
                // 保持原样
                if (item == BottomNavItem.HOME) {
                    Modifier.combinedClickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {
                            debounceClick(item) {
                                // 1. 立即响应点击 (Immediate Navigation)
                                onClick()
                                haptic(HapticType.LIGHT)
                                
                                // 2. 视觉反馈 (Visual Feedback)
                                isPending = true
                                kotlinx.coroutines.MainScope().launch {
                                    // 晃动动画与导航并行执行
                                    wobbleAngle = 15f
                                    kotlinx.coroutines.delay(200) // 等待动画完成
                                    isPending = false
                                }
                            }
                        },
                        onDoubleClick = {
                            haptic(HapticType.MEDIUM)
                            onHomeDoubleTap()
                        }
                    )
                } else {
                    Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { 
                        debounceClick(item) {
                            // 1. 立即响应点击 (Immediate Navigation)
                            onClick()
                            haptic(HapticType.LIGHT)
                            
                            // 2. 视觉反馈 (Visual Feedback)
                            isPending = true
                            kotlinx.coroutines.MainScope().launch {
                                // 晃动动画与导航并行执行
                                wobbleAngle = 15f
                                kotlinx.coroutines.delay(200) // 等待动画完成
                                isPending = false
                            }
                        }
                    }
                }
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) { // ... (Icon/Text rendering 保持不变，使用 iconColor/scale 等变量)
        when (labelMode) {
            0 -> { // Icon + Text
                Box(
                    modifier = Modifier
                        .size(iconSize)
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                            rotationZ = animatedWobble
                            translationY = bounceY
                        },
                    contentAlignment = Alignment.Center
                ) {
                    CompositionLocalProvider(LocalContentColor provides iconColor) {
                        if (isSelected) item.selectedIcon() else item.unselectedIcon()
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = item.label,
                    fontSize = 10.sp,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                    color = iconColor
                )
            }
            2 -> { // Text Only
                Text(
                    text = item.label,
                    fontSize = 14.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = iconColor,
                    modifier = Modifier.graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        rotationZ = animatedWobble
                        translationY = bounceY
                    }
                )
            }
            else -> { // Icon Only
                Box(
                    modifier = Modifier
                        .size(iconSize)
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                            rotationZ = animatedWobble
                            translationY = bounceY
                        },
                    contentAlignment = Alignment.Center
                ) {
                    CompositionLocalProvider(LocalContentColor provides iconColor) {
                        if (isSelected) item.selectedIcon() else item.unselectedIcon()
                    }
                }
            }
        }
    }
}
