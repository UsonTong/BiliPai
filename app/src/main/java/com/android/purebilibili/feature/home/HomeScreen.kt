// 文件路径: feature/home/HomeScreen.kt
package com.android.purebilibili.feature.home

import android.annotation.SuppressLint
import android.content.Context
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.foundation.ExperimentalFoundationApi //  Added
import androidx.compose.foundation.LocalOverscrollFactory // [Fix] Import for disabling overscroll (New API)
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.lazy.staggeredgrid.*  // 🌊 瀑布流布局
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.zIndex
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.graphics.luminance  //  状态栏亮度计算
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.purebilibili.core.theme.BiliPink
import com.android.purebilibili.feature.settings.GITHUB_URL
import com.android.purebilibili.core.store.SettingsManager //  引入 SettingsManager
//  从 components 包导入拆分后的组件
import com.android.purebilibili.feature.home.components.BottomNavItem
import com.android.purebilibili.feature.home.components.FluidHomeTopBar
import com.android.purebilibili.feature.home.components.FrostedSideBar
import com.android.purebilibili.feature.home.components.CategoryTabRow
import com.android.purebilibili.feature.home.components.iOSHomeHeader  //  iOS 大标题头部
import com.android.purebilibili.feature.home.components.iOSRefreshIndicator  //  iOS 下拉刷新指示器
//  从 cards 子包导入卡片组件
import com.android.purebilibili.feature.home.components.cards.ElegantVideoCard
import com.android.purebilibili.feature.home.components.cards.LiveRoomCard
import com.android.purebilibili.feature.home.components.cards.StoryVideoCard   //  故事卡片
import com.android.purebilibili.core.ui.LoadingAnimation
import com.android.purebilibili.core.ui.VideoCardSkeleton
import com.android.purebilibili.core.ui.ErrorState as ModernErrorState
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import com.android.purebilibili.core.ui.shimmer
import com.android.purebilibili.core.ui.LocalSharedTransitionScope  //  共享过渡
import com.android.purebilibili.core.ui.animation.DissolvableVideoCard  //  粒子消散动画
import com.android.purebilibili.core.ui.animation.jiggleOnDissolve      // 📳 iOS 风格抖动效果
import com.android.purebilibili.core.util.responsiveContentWidth
import io.github.alexzhirkevich.cupertino.CupertinoActivityIndicator
import coil.imageLoader
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.distinctUntilChanged  //  性能优化：防止重复触发
import androidx.compose.animation.ExperimentalSharedTransitionApi  //  共享过渡实验API
import com.android.purebilibili.core.ui.LocalSetBottomBarVisible
import com.android.purebilibili.core.ui.LocalBottomBarVisible

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow

// [新增] 全局回顶事件通道
val LocalHomeScrollChannel = compositionLocalOf<Channel<Unit>?> { null }

// [New] Global Scroll Offset for Liquid Glass Effect
// Used to pass scroll position from HomeScreen to BottomBar without causing recomposition
val LocalHomeScrollOffset = compositionLocalOf { androidx.compose.runtime.mutableFloatStateOf(0f) }

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class, ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel = viewModel(),
    onVideoClick: (String, Long, String) -> Unit,
    onAvatarClick: () -> Unit,
    onProfileClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onSearchClick: () -> Unit,
    //  新增：动态页面回调
    onDynamicClick: () -> Unit = {},
    //  新增：历史记录回调
    onHistoryClick: () -> Unit = {},
    //  新增：分区回调
    onPartitionClick: () -> Unit = {},
    //  新增：直播点击回调
    onLiveClick: (Long, String, String) -> Unit = { _, _, _ -> },  // roomId, title, uname
    //  [修复] 番剧/影视回调，接受类型参数 (1=番剧 2=电影 等)
    onBangumiClick: (Int) -> Unit = {},
    //  新增：分类点击回调（用于游戏、知识、科技等分类，传入 tid 和 name）
    onCategoryClick: (Int, String) -> Unit = { _, _ -> },
    //  [新增] 底栏扩展项目导航回调
    onFavoriteClick: () -> Unit = {},  // 收藏页面
    onLiveListClick: () -> Unit = {},  // 直播列表页面
    onWatchLaterClick: () -> Unit = {},  // 稍后再看页面
    onStoryClick: () -> Unit = {},  //  [新增] 竖屏短视频
    globalHazeState: dev.chrisbanes.haze.HazeState? = null  //  [新增] 全局底栏模糊状态
) {
    val state by viewModel.uiState.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
// val pullRefreshState = rememberPullToRefreshState() // [Removed] Moved inside HorizontalPager
    val context = LocalContext.current
    //  [Refactor] Use a map of grid states for each category to support HorizontalPager
    val gridStates = remember { mutableMapOf<HomeCategory, LazyGridState>() }
    HomeCategory.entries.forEach { category ->
        gridStates[category] = rememberLazyGridState()
    }
    val staggeredGridState = rememberLazyStaggeredGridState()  // 🌊 瀑布流状态
    val hazeState = remember { HazeState() }
    val coroutineScope = rememberCoroutineScope()  //  用于双击回顶动画
    
    // [新增] 监听全局回顶事件
    val scrollChannel = LocalHomeScrollChannel.current
    LaunchedEffect(scrollChannel) {
        scrollChannel?.receiveAsFlow()?.collect {
            launch {
                val gridState = gridStates[state.currentCategory]
                val isAtTop = gridState == null || (gridState.firstVisibleItemIndex == 0 && gridState.firstVisibleItemScrollOffset < 50)

                if (isAtTop) {
                    viewModel.refresh()
                } else {
                    gridState?.animateScrollToItem(0)
                }
            }
        }
    }

    // [Refactor] Hoist PagerState to be available for both Content and Header
    // 确保 pagerState 在所有作用域均可见，以便传给 iOSHomeHeader
    val initialPage = HomeCategory.entries.indexOf(state.currentCategory).coerceAtLeast(0)
    val pagerState = androidx.compose.foundation.pager.rememberPagerState(initialPage = initialPage) { HomeCategory.entries.size }
    
    // [修复] 刷新时自动滚回顶部，防止下拉用力过猛导致内容偏移
    LaunchedEffect(isRefreshing) {
        if (isRefreshing) {
            gridStates[state.currentCategory]?.animateScrollToItem(0)
        }
    }
    
    //  [新增] JSON 插件过滤提示
    val snackbarHostState = remember { SnackbarHostState() }
    val lastFilteredCount by com.android.purebilibili.core.plugin.json.JsonPluginManager.lastFilteredCount.collectAsState()
    
    //  当有视频被过滤时显示提示
    LaunchedEffect(lastFilteredCount) {
        if (lastFilteredCount > 0) {
            snackbarHostState.showSnackbar(
                message = " 已过滤 $lastFilteredCount 个视频",
                duration = SnackbarDuration.Short
            )
        }
    }
    
    //  [埋点] 页面浏览追踪
    LaunchedEffect(Unit) {
        com.android.purebilibili.core.util.AnalyticsHelper.logScreenView("HomeScreen")
    }
    
    //  [埋点] 分类切换追踪
    LaunchedEffect(state.currentCategory) {
        com.android.purebilibili.core.util.AnalyticsHelper.logCategoryView(
            categoryName = state.currentCategory.label,
            categoryId = state.currentCategory.tid
        )
    }

    // [New] Broadcast Scroll Offset for Liquid Glass Effect
    val globalScrollOffset = LocalHomeScrollOffset.current
    LaunchedEffect(state.currentCategory, gridStates) { // Re-launch when category changes
        // Use a simple timer loop or snapshotFlow to poll scroll state to avoid heavy recomposition
        // We only need rough updates for the shader wave effect
        val gridState = gridStates[state.currentCategory] ?: return@LaunchedEffect
        snapshotFlow { 
            // Calculate an approximate absolute scroll pixel value
            gridState.firstVisibleItemIndex * 500f + gridState.firstVisibleItemScrollOffset
        }.collect { offset ->
            globalScrollOffset.floatValue = offset
        }
    }
    
    //  [彩蛋] 彩蛋开关设置
    val easterEggEnabled by SettingsManager.getEasterEggEnabled(context).collectAsState(initial = true)
    var showEasterEggDialog by remember { mutableStateOf(false) }
    
    //  [彩蛋] 下拉刷新成功后显示趣味提示（仅在开关开启时）
    LaunchedEffect(state.refreshKey, easterEggEnabled) {
        val message = state.refreshMessage
        if (message != null && state.refreshKey > 0 && easterEggEnabled) {
            val result = snackbarHostState.showSnackbar(
                message = message,
                actionLabel = "关闭彩蛋",
                duration = SnackbarDuration.Short
            )
            if (result == SnackbarResult.ActionPerformed) {
                showEasterEggDialog = true
            }
        }
    }
    
    //  [彩蛋] 关闭确认对话框
    if (showEasterEggDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showEasterEggDialog = false },
            title = { 
                Text(
                    "关闭趣味提示？", 
                    color = MaterialTheme.colorScheme.onSurface
                ) 
            },
            text = { 
                Text(
                    "关闭后下拉刷新将不再显示趣味消息。\n\n你可以在「设置」中随时重新开启。",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                ) 
            },
            confirmButton = {
                androidx.compose.material3.TextButton(
                    onClick = {
                        coroutineScope.launch {
                            SettingsManager.setEasterEggEnabled(context, false)
                        }
                        showEasterEggDialog = false
                    }
                ) { Text("关闭彩蛋", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(
                    onClick = { showEasterEggDialog = false }
                ) { Text("保留彩蛋", color = MaterialTheme.colorScheme.primary) }
            },
            containerColor = MaterialTheme.colorScheme.surface
        )
    }
    
    //  [修复] 确保首页显示时 WindowInsets 配置正确，防止从视频页返回时布局跳动
    val view = androidx.compose.ui.platform.LocalView.current
    SideEffect {
        val window = (view.context as? android.app.Activity)?.window ?: return@SideEffect
        // 保持边到边显示（与 VideoDetailScreen 一致）
        androidx.core.view.WindowCompat.setDecorFitsSystemWindows(window, false)
    }

    //  [性能优化] 合并首页设置为单一 Flow，减少 6 个 collectAsState → 1 个
    val homeSettings by SettingsManager.getHomeSettings(context).collectAsState(
        initial = com.android.purebilibili.core.store.HomeSettings()
    )
    
    // 解构设置值（避免每次访问都触发重组）
    val displayMode = homeSettings.displayMode
    val isBottomBarFloating = homeSettings.isBottomBarFloating
    val bottomBarLabelMode = homeSettings.bottomBarLabelMode
    val isHeaderBlurEnabled = homeSettings.isHeaderBlurEnabled
    val isBottomBarBlurEnabled = homeSettings.isBottomBarBlurEnabled
    val crashTrackingConsentShown = homeSettings.crashTrackingConsentShown
    val cardAnimationEnabled = homeSettings.cardAnimationEnabled      //  卡片进场动画开关
    val cardTransitionEnabled = homeSettings.cardTransitionEnabled    //  卡片过渡动画开关
    
    //  [新增] 底栏可见项目配置
    val orderedVisibleTabIds by SettingsManager.getOrderedVisibleTabs(context).collectAsState(
        initial = listOf("HOME", "DYNAMIC", "HISTORY", "PROFILE")
    )
    // 将字符串 ID 转换为 BottomNavItem 枚举
    val visibleBottomBarItems = remember(orderedVisibleTabIds) {
        orderedVisibleTabIds.mapNotNull { id ->
            try { BottomNavItem.valueOf(id) } catch (e: Exception) { null }
        }
    }
    
    //  [新增] 底栏项目颜色配置
    val bottomBarItemColors by SettingsManager.getBottomBarItemColors(context).collectAsState(initial = emptyMap<String, Int>())

    
    //  📐 [平板适配] 根据屏幕尺寸和展示模式动态设置网格列数
    // 故事卡片需要单列全宽，网格和玻璃使用双列，平板端使用多列
    val windowSizeClass = com.android.purebilibili.core.util.LocalWindowSizeClass.current
    val contentWidth = if (windowSizeClass.isExpandedScreen) {
        minOf(windowSizeClass.widthDp, 1280.dp)
    } else {
        windowSizeClass.widthDp
    }
    val adaptiveColumns = remember(contentWidth, displayMode) {
        val minColumnWidth = if (displayMode == 1) 240.dp else 180.dp
        val maxColumns = if (displayMode == 1) 2 else 6
        val columns = (contentWidth / minColumnWidth).toInt()
        columns.coerceIn(1, maxColumns)
    }
    val gridColumns = if (windowSizeClass.isExpandedScreen) {
        adaptiveColumns
    } else {
        com.android.purebilibili.core.util.rememberResponsiveValue(
            compact = if (displayMode == 1) 1 else 2,  // 手机：故事1列，其他2列
            medium = if (displayMode == 1) 2 else 3    // 中等宽度：故事2列，其它3列
        )
    }
    
    
    //  � [平板导航切换] 用户偏好设置
    val tabletUseSidebar by SettingsManager.getTabletUseSidebar(context).collectAsState(initial = false)
    
    //  �📐 [大屏适配] 平板导航模式：根据用户偏好决定
    // 仅在平板且用户选择了侧边栏时使用侧边导航
    val useSideNavigation = windowSizeClass.isExpandedScreen && tabletUseSidebar
    
    //  📱 [切换导航模式] 处理函数
    val onToggleNavigationMode: () -> Unit = {
        coroutineScope.launch {
            SettingsManager.setTabletUseSidebar(context, !tabletUseSidebar)
        }
    }

    //  [修复] 恢复状态栏样式：确保从视频详情页返回后状态栏正确
    // 当使用滑动动画时，Theme.kt 的 SideEffect 可能不会重新执行
    val backgroundColor = MaterialTheme.colorScheme.background
    val isLightBackground = remember(backgroundColor) { backgroundColor.luminance() > 0.5f }
    
    if (!view.isInEditMode) {
        SideEffect {
            val window = (context as? android.app.Activity)?.window ?: return@SideEffect
            val insetsController = androidx.core.view.WindowCompat.getInsetsController(window, view)
            //  根据背景亮度设置状态栏图标颜色
            insetsController.isAppearanceLightStatusBars = isLightBackground
            //  [修复] 导航栏也需要根据背景亮度设置图标颜色
            insetsController.isAppearanceLightNavigationBars = isLightBackground
            //  确保状态栏可见且透明
            insetsController.show(androidx.core.view.WindowInsetsCompat.Type.statusBars())
            window.statusBarColor = android.graphics.Color.TRANSPARENT
            //  [修复] 导航栏也设为透明，确保底栏隐藏时手势区域沉浸
            window.navigationBarColor = android.graphics.Color.TRANSPARENT
        }
    }

    val density = LocalDensity.current
    val navBarHeight = WindowInsets.navigationBars.getBottom(density).let { with(density) { it.toDp() } }
    
    val bottomBarHeight = if (isBottomBarFloating) {
        84.dp + navBarHeight  // 72dp(栏高度) + 12dp(底部边距)
    } else {
        64.dp + navBarHeight  // 64dp(Docked模式)
    }

    //  [修复] 动态计算内容顶部边距，防止被头部遮挡
    val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val listTopPadding = statusBarHeight + 120.dp  // [调整] 优化顶部间距 (110 -> 120) 增加呼吸感

    val prefs = remember { context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE) }
    
    //  当前选中的导航项
    var currentNavItem by remember { mutableStateOf(BottomNavItem.HOME) }

    // 统一导航点击逻辑（底栏/侧栏复用）
    val handleNavItemClick: (BottomNavItem) -> Unit = { item ->
        currentNavItem = item
        when (item) {
            BottomNavItem.HOME -> {
                coroutineScope.launch { 
                    val gridState = gridStates[state.currentCategory]
                    val isAtTop = gridState == null || (gridState.firstVisibleItemIndex == 0 && gridState.firstVisibleItemScrollOffset < 50)
                    
                    if (isAtTop) {
                        viewModel.refresh()
                    } else {
                        gridState?.animateScrollToItem(0) 
                    } 
                }
            }
            BottomNavItem.DYNAMIC -> onDynamicClick()
            BottomNavItem.HISTORY -> onHistoryClick()
            BottomNavItem.PROFILE -> onProfileClick()
            BottomNavItem.FAVORITE -> onFavoriteClick()
            BottomNavItem.LIVE -> onLiveListClick()
            BottomNavItem.WATCHLATER -> onWatchLaterClick()
            BottomNavItem.STORY -> onStoryClick()
            BottomNavItem.SETTINGS -> onSettingsClick()
        }
    }
    
    //  [新增] 底栏显示模式设置
    val bottomBarVisibilityMode by SettingsManager.getBottomBarVisibilityMode(context).collectAsState(
        initial = SettingsManager.BottomBarVisibilityMode.ALWAYS_VISIBLE
    )
    
    //  [Refactor] 使用全局 CompositionLocal 控制底栏可见性
    val setBottomBarVisible = LocalSetBottomBarVisible.current
    val isGlobalBottomBarVisible = LocalBottomBarVisible.current
    // 兼容代码：为了最小化改动，将 bottomBarVisible 指向全局状态
    // 注意：这里的 bottomBarVisible 现在是只读的，修改必须通过 setBottomBarVisible
    val bottomBarVisible = isGlobalBottomBarVisible
    
    //  [修复] 跟踪是否正在导航到/从视频页 - 必须在 LaunchedEffect 之前声明
    var isVideoNavigating by remember { mutableStateOf(false) }
    
    //  [新增] 滚动方向检测状态（用于上滑隐藏模式）
    var lastScrollOffset by remember { mutableIntStateOf(0) }
    var lastFirstVisibleItem by remember { mutableIntStateOf(0) }
    
    //  [新增] 滚动方向检测逻辑
    LaunchedEffect(state.currentCategory, bottomBarVisibilityMode, useSideNavigation) {
        if (useSideNavigation) {
            setBottomBarVisible(false)
            return@LaunchedEffect
        }
        if (bottomBarVisibilityMode != SettingsManager.BottomBarVisibilityMode.SCROLL_HIDE) {
            // 非滚动隐藏模式时，根据设置决定底栏可见性
            setBottomBarVisible(bottomBarVisibilityMode == SettingsManager.BottomBarVisibilityMode.ALWAYS_VISIBLE)
            return@LaunchedEffect
        }
        
        // 上滑隐藏模式：监听滚动方向
        val currentGridState = gridStates[state.currentCategory] ?: return@LaunchedEffect
        snapshotFlow {
            Pair(currentGridState.firstVisibleItemIndex, currentGridState.firstVisibleItemScrollOffset)
        }
        .distinctUntilChanged()
        .collect { (firstVisibleItem, scrollOffset) ->
            // 视频导航期间不处理滚动隐藏
            if (isVideoNavigating) return@collect
            
            // 滚动到顶部时始终显示
            if (firstVisibleItem == 0 && scrollOffset < 100) {
                setBottomBarVisible(true)
            } else {
                // 计算滚动方向
                val isScrollingDown = when {
                    firstVisibleItem > lastFirstVisibleItem -> true
                    firstVisibleItem < lastFirstVisibleItem -> false
                    else -> scrollOffset > lastScrollOffset + 200 // [UX优化] 增大迟滞阈值 (30 -> 200) 防止微小抖动
                }
                val isScrollingUp = when {
                    firstVisibleItem < lastFirstVisibleItem -> true
                    firstVisibleItem > lastFirstVisibleItem -> false
                    else -> scrollOffset < lastScrollOffset - 200 // [UX优化] 增大迟滞阈值
                }
                
                if (isScrollingDown) setBottomBarVisible(false)
                else if (isScrollingUp) setBottomBarVisible(true)
            }
            
            lastFirstVisibleItem = firstVisibleItem
            lastScrollOffset = scrollOffset
        }
    }
    
    //  [修复] 用于取消延迟协程的 Job 引用
    var bottomBarRestoreJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }
    
    //  包装 onVideoClick：点击视频时先隐藏底栏再导航
    val wrappedOnVideoClick: (String, Long, String) -> Unit = remember(onVideoClick) {
        { bvid, cid, cover ->
            //  取消之前的恢复协程，防止竞态条件
            bottomBarRestoreJob?.cancel()
            bottomBarRestoreJob = null
            
            setBottomBarVisible(false)  //  触发底栏下滑动画
            isVideoNavigating = true  //  标记正在导航到视频
            onVideoClick(bvid, cid, cover)
        }
    }
    
    //  [修复] 使用生命周期事件控制底栏可见性
    // ON_START: 恢复底栏（仅在从视频页返回时）
    // ON_STOP: 隐藏底栏（导航到其他页面时，避免影响导航栏区域）
    val lifecycleOwner = androidx.compose.ui.platform.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, useSideNavigation) {
        if (useSideNavigation) {
            return@DisposableEffect onDispose { }
        }
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            when (event) {
                androidx.lifecycle.Lifecycle.Event.ON_START -> {
                    //  关键修复：只在底栏当前隐藏时才恢复可见
                    if (!bottomBarVisible && isVideoNavigating) {
                        //  [同步动画] 延迟后再显示底栏，让进入动画与卡片返回动画同步
                        //  [优化] 将延迟增加到 360ms (略大于转场动画 350ms)，防止在动画过程中修改 Padding 导致列表重排卡顿
                        bottomBarRestoreJob = kotlinx.coroutines.MainScope().launch {
                            kotlinx.coroutines.delay(360)  // 等待返回动画结束
                            setBottomBarVisible(true)
                            // 延迟重置导航状态，确保进入动画完成
                            kotlinx.coroutines.delay(200)
                            isVideoNavigating = false
                        }
                    } else if (!bottomBarVisible && !isVideoNavigating) {
                        //  [新增] 从设置等非视频页面返回时，立即显示底栏（无延迟）
                        setBottomBarVisible(true)
                    }
                }
                androidx.lifecycle.Lifecycle.Event.ON_STOP -> {
                    //  [修复] 移除此处隐藏底栏的逻辑
                    //  防止切换到其他Tab（如动态/历史）时底栏消失
                    bottomBarRestoreJob?.cancel()
                    bottomBarRestoreJob = null
                    // setBottomBarVisible(false) // REMOVED
                }
                else -> { /* 其他事件不处理 */ }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            bottomBarRestoreJob?.cancel()
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
    
    //  [修复] 使用 ViewModel 中的标签页显示索引（跨导航保持）
    // 当用户滑动到特殊分类时，标签页位置更新，但内容分类保持不变
    val displayedTabIndex = state.displayedTabIndex
    
    //  [修复] 使用 rememberSaveable 记住本次会话中是否已处理过弹窗（防止导航后重新显示）
    var consentDialogHandled by rememberSaveable { mutableStateOf(false) }
    var showConsentDialog by remember { mutableStateOf(false) }
    
    //  检查欢迎弹窗是否已显示过（确保弹窗顺序显示，不会同时出现）
    val welcomePrefs = remember { context.getSharedPreferences("app_welcome", Context.MODE_PRIVATE) }
    val welcomeAlreadyShown = welcomePrefs.getBoolean("first_launch_shown", false)
    
    // 检查是否需要显示弹窗（欢迎弹窗已显示过 且 同意弹窗尚未显示过 且 本次会话未处理过）
    LaunchedEffect(crashTrackingConsentShown) {
        if (welcomeAlreadyShown && !crashTrackingConsentShown && !consentDialogHandled) {
            showConsentDialog = true
        }
    }
    
    // 显示弹窗
    if (showConsentDialog) {
        com.android.purebilibili.feature.home.components.CrashTrackingConsentDialog(
            onDismiss = { 
                showConsentDialog = false
                consentDialogHandled = true  // 标记为已处理
            }
        )
    }
    
    //  计算滚动偏移量用于头部动画 -  优化：量化减少重组
    //  计算滚动偏移量用于头部动画 -  优化：量化减少重组
    val scrollOffset by remember {
        derivedStateOf {
            val currentGridState = gridStates[state.currentCategory]
            if (currentGridState == null) return@derivedStateOf 0f
            
            val firstVisibleItem = currentGridState.firstVisibleItemIndex
            if (firstVisibleItem == 0) {
                //  直接使用原始偏移量，避免量化导致的跳变
                currentGridState.firstVisibleItemScrollOffset.toFloat()
            } else 1000f
        }
    }
    
    //  滚动方向（简化版 - 不再需要复杂检测，因为标签页只在顶部显示）
    val isScrollingUp = true  // 保留参数兼容性

    val shouldLoadMore by remember {
        derivedStateOf {
            val currentGridState = gridStates[state.currentCategory]
            if (currentGridState == null) return@derivedStateOf false

            val layoutInfo = currentGridState.layoutInfo
            val totalItems = layoutInfo.totalItemsCount
            val lastVisibleItemIndex = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            totalItems > 0 && lastVisibleItemIndex >= totalItems - 4 && !state.isLoading && !isRefreshing
        }
    }
    LaunchedEffect(shouldLoadMore) { if (shouldLoadMore) viewModel.loadMore() }
    
    //  [性能优化] 图片预加载 - 提前加载即将显示的视频封面
    // 📉 [省流量] 省流量模式下禁用预加载
    val isDataSaverActive = remember {
        com.android.purebilibili.core.store.SettingsManager.isDataSaverActive(context)
    }
    
    LaunchedEffect(state.currentCategory, isDataSaverActive) {
        // 📉 省流量模式下跳过预加载
        if (isDataSaverActive) return@LaunchedEffect
        
        val currentGridState = gridStates[state.currentCategory] ?: return@LaunchedEffect
        
        snapshotFlow { currentGridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0 }
            .distinctUntilChanged()  //  只在索引变化时触发
            .collect { lastVisibleIndex ->
                val videos = state.videos
                val preloadStart = (lastVisibleIndex + 1).coerceAtMost(videos.size)
                val preloadEnd = (lastVisibleIndex + 6).coerceAtMost(videos.size)  //  减少预加载数量
                
                if (preloadStart < preloadEnd) {
                    for (i in preloadStart until preloadEnd) {
                        val imageUrl = videos.getOrNull(i)?.pic ?: continue
                        val request = coil.request.ImageRequest.Builder(context)
                            .data(com.android.purebilibili.core.util.FormatUtils.fixImageUrl(imageUrl))
                            .size(360, 225)  //  预加载也使用限制尺寸
                            .memoryCachePolicy(coil.request.CachePolicy.ENABLED)
                            .diskCachePolicy(coil.request.CachePolicy.ENABLED)
                            .build()
                        context.imageLoader.enqueue(request)
                    }
                }
            }
    }


    //  PullToRefreshBox 自动处理下拉刷新逻辑
    
    //  [已移除] 特殊分类（ANIME, MOVIE等）不再在首页切换，直接导航到独立页面
    
    //  [修复] 如果当前在直播-关注分类且列表为空，返回时先切换到热门，再切换到推荐
    val isEmptyLiveFollowed = state.currentCategory == HomeCategory.LIVE && 
                               state.liveSubCategory == LiveSubCategory.FOLLOWED &&
                               state.liveRooms.isEmpty() && 
                               !state.isLoading
    androidx.activity.compose.BackHandler(enabled = isEmptyLiveFollowed) {
        // 切换到热门直播
        viewModel.switchLiveSubCategory(LiveSubCategory.POPULAR)
    }

    //  [修复] 如果当前在直播分类（非关注空列表情况），返回时切换到推荐
    val isLiveCategoryNotHome = state.currentCategory == HomeCategory.LIVE && !isEmptyLiveFollowed
    androidx.activity.compose.BackHandler(enabled = isLiveCategoryNotHome) {
        viewModel.switchCategory(HomeCategory.RECOMMEND)
    }
    
// [Removed] Animation logic moved inside HorizontalPager where the active state exists
    
    // 指示器位置逻辑也移入 graphicsLayer
    
    val scaffoldContent: @Composable () -> Unit = {
    Scaffold(
        //  [新增] JSON 插件过滤提示
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.padding(
                    bottom = when {
                        useSideNavigation -> navBarHeight + 8.dp
                        isBottomBarFloating -> 100.dp
                        else -> 80.dp
                    }
                )
            )
        },
        //  [修复] 禁用 Scaffold 默认的 contentWindowInsets，防止底部出现白色填充
        contentWindowInsets = WindowInsets(0),
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        //  [重构] 实现真正的毛玻璃效果
        // 外层 Box 包含：1) hazeSource 内容层  2) Header overlay 层
        // Header 在 hazeSource 外部，可以正确模糊内层内容
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            // ===== 内容层 (hazeSource) =====
            // [修复] 如果有全局 hazeState，同时应用两个 hazeSource
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    // [Fix] Move local hazeSource deeper to avoid drawing hierarchy crash
                    // .hazeSource(state = hazeState) 
                    .then(if (globalHazeState != null) Modifier.hazeSource(state = globalHazeState) else Modifier)  // 全局 hazeSource - 底栏使用
            ) {
            //  [Restored] Always render Pager. Loading/Error states are handled per-page internally.
            //  This prevents the Pager from being destroyed during category switches or loading states.
                
            //  [Best Practice] 使用 InteractionSource 区分用户操作和代码滚动
            //  只有在用户发生过拖拽/按压行为后的 settling 才是用户切换，否则可能是代码触发的滚动
            var isUserAction by remember { mutableStateOf(false) }

                //  监听用户交互
                LaunchedEffect(pagerState) {
                    pagerState.interactionSource.interactions.collect { interaction ->
                        if (interaction is androidx.compose.foundation.interaction.DragInteraction.Start) {
                            isUserAction = true
                        }
                    }
                }

                //  联动 Pager -> ViewModel
                //  仅当 isUserAction 为 true 时才允许 Pager 驱动 ViewModel 变更
                LaunchedEffect(pagerState) {
                    snapshotFlow { pagerState.settledPage }
                        .collect { page ->
                            if (isUserAction) {
                                val category = HomeCategory.entries[page]
                                if (state.currentCategory != category) {
                                    viewModel.switchCategory(category)
                                }
                                // 重置标记，等待下一次手势
                                isUserAction = false
                            }
                        }
                }
                
                //  联动 ViewModel -> Pager
                //  当点击 Tab 时触发，此时 isUserAction 为 false，不会反向触发
                LaunchedEffect(state.currentCategory) {
                    val targetIndex = HomeCategory.entries.indexOf(state.currentCategory)
                    if (targetIndex >= 0 && targetIndex != pagerState.currentPage) {
                        pagerState.animateScrollToPage(targetIndex)
                    }
                }
                
                //  [Refactor] Use Box to allow overlay and proper blur nesting
                Box(modifier = Modifier.fillMaxSize()) {
                    // [Fix] Disable default overscroll (stretch) effect which causes screen shrinking
                    CompositionLocalProvider(LocalOverscrollFactory provides null) {
                        HorizontalPager(
                            state = pagerState,
                            modifier = Modifier
                                .fillMaxSize()
                                .hazeSource(state = hazeState), // [Restored] Always apply hazeSource for consistent blur
                            key = { index -> HomeCategory.entries[index].ordinal }
                        ) { page ->
                        val category = HomeCategory.entries[page]
                        val categoryState = state.categoryStates[category] ?: com.android.purebilibili.feature.home.CategoryContent()
                        
                        //  独立的 PullToRefreshState，避免所有页面共享一个状态导致冲突
                        val pullRefreshState = rememberPullToRefreshState()

                        //  [新增] 下拉回弹物理动画状态 (Moved from outer scope)
                        val targetPullOffset = if (pullRefreshState.distanceFraction > 0) {
                            val fraction = pullRefreshState.distanceFraction.coerceAtMost(2f)
                            fraction * 0.5f 
                        } else 0f
                        
                        //  使用 animateFloatAsState 包装偏移量
                        val animatedDragOffsetFraction by androidx.compose.animation.core.animateFloatAsState(
                            targetValue = targetPullOffset,
                            animationSpec = androidx.compose.animation.core.spring(
                                dampingRatio = 0.5f,  // 0.5 = 明显的弹性 (Bouncy)
                                stiffness = 350f      // 350 = 中等刚度
                            ),
                            label = "pull_bounce"
                        )

                        //  Defers calculation to graphicsLayer
                        val calculateDragOffset: androidx.compose.ui.unit.Density.() -> Float = remember(animatedDragOffsetFraction) {
                            {
                                val maxPx = 140.dp.toPx()
                                maxPx * animatedDragOffsetFraction
                            }
                        }
                        
                        //  每个页面独立的 GridState
                        //  使用 saveable 记住滚动位置
                        val pageGridState = gridStates[category] ?: rememberLazyGridState()
                        
                        //  把 GridState 提升给父级用于控制 Header? 
                        //  暂时简化：Header 固定，内部滚动
                        
                        PullToRefreshBox(
                            isRefreshing = isRefreshing && state.currentCategory == category,
                            onRefresh = { viewModel.refresh() },
                            state = pullRefreshState,
                            modifier = Modifier.fillMaxSize(),
                             //  iOS 风格下拉刷新指示器 (位于内容上方)
                             indicator = {
                                iOSRefreshIndicator(
                                    state = pullRefreshState,
                                    isRefreshing = isRefreshing,
                                     modifier = Modifier
                                         .align(Alignment.TopCenter)
                                         // [物理优化] 指示器跟随拖拽移动，保持在 Gap 中央
                                         .padding(top = listTopPadding) 
                                         .graphicsLayer {
                                            val currentDragOffset = calculateDragOffset()
                                            // [物理优化] 始终保持在 Header (listTopPadding) 和 内容顶部 (listTopPadding + currentDragOffset) 之间
                                            // 公式： (currentDragOffset / 2) - (indicatorHeight / 2)
                                            // 假设指示器高度约 40dp (icon + text spacing)
                                            val indicatorHeight = 40.dp.toPx()
                                            translationY = (currentDragOffset / 2f) - (indicatorHeight / 2f)
                                         }
                                         .fillMaxWidth()
                                 )
                             }
                        ) {
                             // [物理优化] 内容容器应用下沉效果
                             Box(
                                 modifier = Modifier
                                     .fillMaxSize()
                                     .graphicsLayer {
                                         translationY = calculateDragOffset()
                                     }
                             ) {
                             if (categoryState.isLoading && categoryState.videos.isEmpty() && categoryState.liveRooms.isEmpty()) {
                                 // Loading Skeleton per page
                                 LazyVerticalGrid(
                                     columns = GridCells.Fixed(gridColumns),
                                     contentPadding = PaddingValues(
                                         bottom = when {
                                            useSideNavigation -> navBarHeight + 8.dp
                                            isBottomBarFloating -> 100.dp
                                            bottomBarVisible -> 64.dp + navBarHeight + 20.dp
                                            else -> navBarHeight + 8.dp
                                         },
                                         start = 8.dp, end = 8.dp, top = listTopPadding // [Fix] Apply top padding to skeleton grid too
                                     ),
                                     horizontalArrangement = Arrangement.spacedBy(10.dp),
                                     verticalArrangement = Arrangement.spacedBy(10.dp),
                                     modifier = Modifier.fillMaxSize()
                                 ) {
                                     items(10) { index -> VideoCardSkeleton(index = index) }
                                 }
                             } else if (categoryState.error != null && categoryState.videos.isEmpty()) {
                                 // Error State per page
                                 Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                     ModernErrorState(
                                         message = categoryState.error,
                                         onRetry = { viewModel.refresh() }
                                     )
                                 }
                             } else {
                                 // Data Content
                                 HomeCategoryPageContent(
                                     category = category,
                                     categoryState = categoryState,
                                     gridState = pageGridState,
                                     gridColumns = gridColumns,
                                     contentPadding = PaddingValues(
                                         bottom = when {
                                            useSideNavigation -> navBarHeight + 8.dp
                                            isBottomBarFloating -> 100.dp
                                            bottomBarVisible -> 64.dp + navBarHeight + 20.dp
                                            else -> navBarHeight + 8.dp
                                         },
                                         start = 8.dp, end = 8.dp, top = listTopPadding 
                                     ),
                                     dissolvingVideos = state.dissolvingVideos,
                                     followingMids = state.followingMids,
                                     onVideoClick = wrappedOnVideoClick,
                                     onLiveClick = onLiveClick,
                                     onLoadMore = { viewModel.loadMore() },
                                     onDismissVideo = { viewModel.startVideoDissolve(it) },
                                     onWatchLater = { bvid, aid -> viewModel.addToWatchLater(bvid, aid) },
                                     onDissolveComplete = { viewModel.completeVideoDissolve(it) }
                                 )
                             }
                             } // Close Box wrapper
                        }
                    }
                    } // Ends CompositionLocalProvider
                }
            } // 关闭 PullToRefreshBox
        }  // 关闭 hazeSource Box
        
        //  ===== Header Overlay (毛玻璃效果) =====
        //  Header 现在在外层 Box 内、hazeSource 外部，可以正确模糊内层内容
        val isSkeletonState = state.isLoading && state.videos.isEmpty() && state.liveRooms.isEmpty()
        val isErrorState = state.error != null && 
            ((state.currentCategory == HomeCategory.LIVE && state.liveRooms.isEmpty()) ||
             (state.currentCategory != HomeCategory.LIVE && state.videos.isEmpty()))

        //  [Restored] Header 始终显示，不再随 Loading/Error 状态隐藏
        //  这保证了 Tab 指示器状态的连续性，防止消失或重置
        iOSHomeHeader(
            scrollOffset = scrollOffset,
            user = state.user,
            onAvatarClick = { if (state.user.isLogin) onProfileClick() else onAvatarClick() },
            onSettingsClick = onSettingsClick,
            onSearchClick = onSearchClick,
            categoryIndex = displayedTabIndex,
            onCategorySelected = { index ->
                viewModel.updateDisplayedTabIndex(index)
                val category = HomeCategory.entries[index]
                when (category) {
                    HomeCategory.ANIME -> onBangumiClick(1)
                    HomeCategory.MOVIE -> onBangumiClick(2)
                    else -> viewModel.switchCategory(category)
                }
            },
            onPartitionClick = onPartitionClick,
            isScrollingUp = isScrollingUp,
            hazeState = if (isHeaderBlurEnabled) hazeState else null,
            onStatusBarDoubleTap = {
                coroutineScope.launch {
                    gridStates[state.currentCategory]?.animateScrollToItem(0)
                }
            },
            isRefreshing = isRefreshing,
            pullProgress = 0f, // [Fix] Outer header doesn't track inner pull state
            pagerState = pagerState
        )
    }  // 关闭外层 Box
    }  //  关闭 scaffoldContent lambda
    // 📱 [平板适配] 导航模式切换动画
    // 始终使用 Row 布局，通过动画控制侧边栏的显示/隐藏
    Row(modifier = Modifier.fillMaxSize()) {
        AnimatedVisibility(
            visible = useSideNavigation,
            enter = slideInHorizontally(
                initialOffsetX = { -it },
                animationSpec = tween(300, easing = LinearOutSlowInEasing)
            ) + fadeIn(animationSpec = tween(200)),
            exit = slideOutHorizontally(
                targetOffsetX = { -it },
                animationSpec = tween(250, easing = FastOutLinearInEasing)
            ) + fadeOut(animationSpec = tween(200))
        ) {
            FrostedSideBar(
                currentItem = currentNavItem,
                onItemClick = handleNavItemClick,
                onHomeDoubleTap = {
                    coroutineScope.launch { gridStates[state.currentCategory]?.animateScrollToItem(0) }
                },
                hazeState = if (isBottomBarBlurEnabled) hazeState else null,
                visibleItems = visibleBottomBarItems,
                itemColorIndices = bottomBarItemColors,
                onToggleSidebar = onToggleNavigationMode
            )
        }

        Box(
            modifier = Modifier
                .fillMaxHeight()
                .weight(1f)
        ) {
            scaffoldContent()
        }
    }
}
