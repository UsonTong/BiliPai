// 文件路径: feature/home/components/BottomBar.kt
package com.android.purebilibili.feature.home.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer  // 🔥 晃动动画
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeChild
import com.android.purebilibili.core.ui.blur.unifiedBlur  // 🔥 统一模糊API
import dev.chrisbanes.haze.materials.ExperimentalHazeMaterialsApi
import dev.chrisbanes.haze.materials.HazeMaterials
import com.android.purebilibili.core.util.HapticType
import com.android.purebilibili.core.util.rememberHapticFeedback
import com.android.purebilibili.core.theme.iOSSystemGray
import com.android.purebilibili.core.theme.BottomBarColors  // 🔥 统一底栏颜色配置
import com.android.purebilibili.core.theme.BottomBarColorPalette  // 🔥 调色板
import kotlinx.coroutines.launch  // 🔥 延迟导航
// 🍎 Cupertino Icons - iOS SF Symbols 风格图标
import io.github.alexzhirkevich.cupertino.icons.CupertinoIcons
import io.github.alexzhirkevich.cupertino.icons.outlined.*
import io.github.alexzhirkevich.cupertino.icons.filled.*

/**
 * 底部导航项枚举 - 🍎 使用 iOS SF Symbols 风格图标
 */
enum class BottomNavItem(
    val label: String,
    val selectedIcon: @Composable () -> Unit,
    val unselectedIcon: @Composable () -> Unit
) {
    HOME(
        "首页",
        { Icon(CupertinoIcons.Filled.House, null) },
        { Icon(CupertinoIcons.Outlined.House, null) }
    ),
    DYNAMIC(
        "动态",
        { Icon(CupertinoIcons.Filled.BellBadge, null) },
        { Icon(CupertinoIcons.Outlined.Bell, null) }
    ),
    STORY(
        "短视频",
        { Icon(CupertinoIcons.Filled.PlayCircle, null) },
        { Icon(CupertinoIcons.Outlined.PlayCircle, null) }
    ),
    HISTORY(
        "历史",
        { Icon(CupertinoIcons.Filled.Clock, null) },
        { Icon(CupertinoIcons.Outlined.Clock, null) }
    ),
    PROFILE(
        "我的",
        { Icon(CupertinoIcons.Filled.PersonCircle, null) },
        { Icon(CupertinoIcons.Outlined.Person, null) }
    ),
    FAVORITE(
        "收藏",
        { Icon(CupertinoIcons.Filled.Star, null) },
        { Icon(CupertinoIcons.Outlined.Star, null) }
    ),
    LIVE(
        "直播",
        { Icon(CupertinoIcons.Filled.Video, null) },
        { Icon(CupertinoIcons.Outlined.Video, null) }
    ),
    WATCHLATER(
        "稍后看",
        { Icon(CupertinoIcons.Filled.Bookmark, null) },
        { Icon(CupertinoIcons.Outlined.Bookmark, null) }
    )
}

/**
 * 🔥 iOS 风格磨砂玻璃底部导航栏
 * 
 * 特性：
 * - 实时磨砂玻璃效果 (使用 Haze 库)
 * - 悬浮圆角设计
 * - 自动适配深色/浅色模式
 * - 🍎 点击触觉反馈
 */
@OptIn(ExperimentalHazeMaterialsApi::class)
@Composable
fun FrostedBottomBar(
    currentItem: BottomNavItem = BottomNavItem.HOME,
    onItemClick: (BottomNavItem) -> Unit,
    modifier: Modifier = Modifier,
    hazeState: HazeState? = null,
    isFloating: Boolean = true,
    labelMode: Int = 1,  // 🔥 0=图标+文字, 1=仅图标, 2=仅文字
    onHomeDoubleTap: () -> Unit = {},  // 🍎 双击首页回到顶部
    visibleItems: List<BottomNavItem> = listOf(BottomNavItem.HOME, BottomNavItem.DYNAMIC, BottomNavItem.HISTORY, BottomNavItem.PROFILE),  // 🔥🔥 [新增] 可配置的可见项目
    itemColorIndices: Map<String, Int> = emptyMap()  // 🔥🔥 [新增] 项目颜色索引映射
) {
    val isDarkTheme = MaterialTheme.colorScheme.background.red < 0.5f
    val haptic = rememberHapticFeedback()  // 🍎 触觉反馈
    
    // 🔥🔥 读取当前模糊强度以确定背景透明度
    val context = androidx.compose.ui.platform.LocalContext.current
    val blurIntensity by com.android.purebilibili.core.store.SettingsManager.getBlurIntensity(context)
        .collectAsState(initial = com.android.purebilibili.core.ui.blur.BlurIntensity.THIN)
    val backgroundAlpha = com.android.purebilibili.core.ui.blur.BlurStyles.getBackgroundAlpha(blurIntensity)

    // 🔥 根据 labelMode 动态计算高度
    val floatingHeight = when (labelMode) {
        0 -> 64.dp   // 图标+文字
        2 -> 48.dp   // 仅文字
        else -> 56.dp // 仅图标
    }
    val dockedHeight = when (labelMode) {
        0 -> 56.dp   // 图标+文字
        2 -> 44.dp   // 仅文字
        else -> 52.dp // 仅图标
    }
    
    val barHorizontalPadding = if (isFloating) 24.dp else 0.dp
    val barBottomPadding = if (isFloating) 16.dp else 0.dp
    val barShape = if (isFloating) RoundedCornerShape(36.dp) else androidx.compose.ui.graphics.RectangleShape  // 🍎 iOS 风格：紧贴底部无圆角
    
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = barHorizontalPadding)
            .padding(bottom = barBottomPadding)
            .then(if (isFloating) Modifier.navigationBarsPadding() else Modifier)
    ) {
        // 🔥 主内容层
        Surface(
            modifier = Modifier
                .then(
                    if (isFloating) {
                         Modifier
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
                .then(
                    if (hazeState != null) {
                        Modifier.unifiedBlur(hazeState)  // 🔥 版本自适应模糊
                    } else {
                        Modifier
                    }
                ),
            // 🔥🔥 [修复] 根据模糊强度动态调整背景透明度
            color = if (hazeState != null) {
                MaterialTheme.colorScheme.surface.copy(alpha = backgroundAlpha)
            } else {
                // 无模糊时使用实心背景
                MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
            },
            shape = barShape,
            shadowElevation = 0.dp,
            border = if (hazeState != null) {
                // 🍎 iOS 风格：非悬浮模式只显示顶部边框
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
                    // 🍎 [优化] 悬浮模式边框 0.5dp - 更精致的玻璃拟态风格
                    androidx.compose.foundation.BorderStroke(
                        width = 0.5.dp,  // 🔥 从 1dp 改为 0.5dp
                        brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.35f),  // 🔥 顶部高光增强
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                            )
                        )
                    )
                }
            } else {
                // 无模糊时使用更淡的边框
                androidx.compose.foundation.BorderStroke(
                    width = 0.5.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                )
            }
        ) {
            // 📱 Telegram 风格滑动指示器
            val itemCount = visibleItems.size  // 🔥🔥 [修改] 使用可见项目数
            val selectedIndex = visibleItems.indexOf(currentItem)  // 🔥🔥 [修改] 使用可见项目索引
            
            // 🍎 iOS 风格：内容区固定高度，导航栏区域作为 padding 包含在 Surface 内
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(
                            if (isFloating) Modifier.fillMaxHeight()
                            else Modifier.height(dockedHeight)
                        )
                ) {
                // 🔥 考虑 Row 的 padding 后的实际可用宽度
                val rowPadding = 12.dp
                val actualContentWidth = maxWidth - (rowPadding * 2)
                val itemWidth = actualContentWidth / itemCount
                
                // 🔥 Telegram 风格滑动胶囊指示器
                val indicatorOffset by animateDpAsState(
                    targetValue = rowPadding + (itemWidth * selectedIndex) + (itemWidth - 48.dp) / 2,  // 🍎 适配 48dp 胶囊
                    animationSpec = spring(
                        dampingRatio = 0.7f,  // 柔和阻尼
                        stiffness = 400f       // 较快响应
                    ),
                    label = "indicator_offset"
                )
                
                // 🔥 [已移除] 指示器胶囊背景 - 用户要求去掉圆圈
                
                // 导航项 Row
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = rowPadding),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
            ) {
                visibleItems.forEach { item ->  // 🔥🔥 [修改] 使用可配置的项目列表
                    val isSelected = item == currentItem
                    
                    // 🔥🔥 [新增] 追踪是否正在点击此项（动画播放中）
                    var isPending by remember { mutableStateOf(false) }
                    
                    // 🍎 跟随主题色：选中时使用主题色，未选中时根据模糊状态调整颜色
                    val primaryColor = MaterialTheme.colorScheme.primary
                    // 🔥🔥 [优化] 模糊模式下使用 onSurface 自适应深浅模式
                    // 深色模式 -> onSurface 为浅色（白色系）；浅色模式 -> onSurface 为深色（黑色系）
                    val unselectedColor = if (hazeState != null) {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    } else {
                        BottomBarColors.UNSELECTED
                    }
                    
                    val iconColor by animateColorAsState(
                        targetValue = if (isSelected || isPending) primaryColor else unselectedColor,
                        animationSpec = spring(),
                        label = "iconColor"
                    )
                    
                    // 🔥🔥 [新增] Telegram 风格晃动动画状态
                    var triggerWobble by remember { mutableStateOf(0) }
                    
                    // 🍎 晃动角度动画
                    val rotation by animateFloatAsState(
                        targetValue = 0f,
                        animationSpec = spring(
                            dampingRatio = 0.35f,  // 更低阻尼 = 更多晃动
                            stiffness = 600f
                        ),
                        label = "rotation"
                    )
                    
                    // 🔥 点击时触发晃动效果
                    LaunchedEffect(triggerWobble) {
                        if (triggerWobble > 0) {
                            // 无需额外操作，rotation 动画会自动处理
                        }
                    }
                    
                    // 🍎 弹性缩放动画 (选中时放大并弹跳)
                    val scale by animateFloatAsState(
                        targetValue = if (isSelected) 1.15f else 1.0f,
                        animationSpec = spring(
                            dampingRatio = 0.4f,  // 🍎 更低阻尼创造明显弹跳
                            stiffness = 350f
                        ),
                        label = "scale"
                    )
                    
                    // 🔥🔥 [新增] 点击时的晃动角度
                    var wobbleAngle by remember { mutableFloatStateOf(0f) }
                    val scope = rememberCoroutineScope()  // 🔥 用于延迟导航
                    
                    val animatedWobble by animateFloatAsState(
                        targetValue = wobbleAngle,
                        animationSpec = spring(
                            dampingRatio = 0.25f,  // 非常低的阻尼 = 多次晃动
                            stiffness = 800f       // 高刚度 = 快速响应
                        ),
                        label = "wobble"
                    )
                    
                    // 🔥 晃动完成后重置角度
                    LaunchedEffect(wobbleAngle) {
                        if (wobbleAngle != 0f) {
                            kotlinx.coroutines.delay(50)  // 短暂保持
                            wobbleAngle = 0f  // 重置触发弹回晃动
                        }
                    }
                    
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .then(
                                if (item == BottomNavItem.HOME) {
                                    // 🍎 HOME 项支持双击回到顶部
                                    Modifier.pointerInput(Unit) {
                                        detectTapGestures(
                                            onTap = {
                                                isPending = true  // 🔥 立即变色
                                                haptic(HapticType.LIGHT)
                                                // 🔥 颜色切换完成后再播放晃动动画，然后切换页面
                                                kotlinx.coroutines.MainScope().launch {
                                                    kotlinx.coroutines.delay(100)  // 等待颜色动画
                                                    wobbleAngle = 15f  // 🔥 触发晃动
                                                    kotlinx.coroutines.delay(150)  // 等待晃动动画
                                                    onItemClick(item)
                                                }
                                            },
                                            onDoubleTap = {
                                                haptic(HapticType.MEDIUM)  // 双击用更强反馈
                                                onHomeDoubleTap()
                                            }
                                        )
                                    }
                                } else {
                                    // 其他项保持普通点击
                                    Modifier.clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null
                                    ) { 
                                        isPending = true  // 🔥 立即变色
                                        haptic(HapticType.LIGHT)
                                        // 🔥 颜色切换完成后再播放晃动动画，然后切换页面
                                        kotlinx.coroutines.MainScope().launch {
                                            kotlinx.coroutines.delay(100)  // 等待颜色动画
                                            wobbleAngle = 15f  // 🔥 触发晃动
                                            kotlinx.coroutines.delay(150)  // 等待晃动动画
                                            onItemClick(item)
                                        }
                                    }
                                }
                            ),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        // 🔥 根据 labelMode 显示不同组合
                        when (labelMode) {
                            0 -> {
                                // 图标 + 文字
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .scale(scale)
                                        .graphicsLayer { rotationZ = animatedWobble },  // 🔥 晃动效果
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
                            2 -> {
                                // 仅文字
                                Text(
                                    text = item.label,
                                    fontSize = 14.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = iconColor,
                                    modifier = Modifier
                                        .scale(scale)
                                        .graphicsLayer { rotationZ = animatedWobble }  // 🔥 晃动效果
                                )
                            }
                            else -> {
                                // 仅图标 (默认)
                                Box(
                                    modifier = Modifier
                                        .size(26.dp)
                                        .scale(scale)
                                        .graphicsLayer { rotationZ = animatedWobble },  // 🔥 晃动效果
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
            }
            }  // 🔥 BoxWithConstraints 闭合
                
                // 🍎 iOS 风格：非悬浮模式时，导航栏区域作为 Spacer 包含在 Surface 内
                if (!isFloating) {
                    Spacer(
                        modifier = Modifier
                            .fillMaxWidth()
                            .windowInsetsPadding(WindowInsets.navigationBars)
                    )
                }
            }  // 🔥 Column 闭合
        }
    }
}
