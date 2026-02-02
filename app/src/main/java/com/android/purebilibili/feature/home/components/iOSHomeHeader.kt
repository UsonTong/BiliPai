// 文件路径: feature/home/components/iOSHomeHeader.kt
package com.android.purebilibili.feature.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
//  Cupertino Icons - iOS SF Symbols 风格图标
import io.github.alexzhirkevich.cupertino.icons.CupertinoIcons
import io.github.alexzhirkevich.cupertino.icons.outlined.*
import io.github.alexzhirkevich.cupertino.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance  //  状态栏亮度计算
import androidx.compose.ui.zIndex
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import com.kyant.backdrop.drawBackdrop 
import com.kyant.backdrop.effects.lens 
import com.android.purebilibili.core.ui.effect.simpMusicLiquidGlass 
import com.kyant.backdrop.backdrops.LayerBackdrop
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.android.purebilibili.core.util.FormatUtils
import com.android.purebilibili.core.util.HapticType
import com.android.purebilibili.core.util.iOSTapEffect
import com.android.purebilibili.core.util.rememberHapticFeedback
import com.android.purebilibili.feature.home.UserState
import com.android.purebilibili.core.theme.iOSSystemGray
import dev.chrisbanes.haze.HazeState
import com.android.purebilibili.core.store.SettingsManager
import com.android.purebilibili.core.ui.blur.unifiedBlur
import com.android.purebilibili.core.ui.blur.BlurStyles
import com.android.purebilibili.core.ui.blur.BlurIntensity

/**
 *  简洁版首页头部 (带滚动隐藏/显示动画)
 * 
 *  [Refactor] 现在改为由外部通过 NestedScrollConnection 直接控制高度和透明度，
 *  实现了 1:1 的物理跟手效果，消除了漂浮感。
 */
@Composable
fun iOSHomeHeader(
    searchBarHeight: Dp,
    searchBarAlpha: Float,
    user: UserState,
    onAvatarClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onSearchClick: () -> Unit,
    categoryIndex: Int,
    onCategorySelected: (Int) -> Unit,
    onPartitionClick: () -> Unit = {},  //  新增：分区按钮回调
    hazeState: HazeState? = null,  // 保留参数兼容性，但不用于模糊
    onStatusBarDoubleTap: () -> Unit = {},
    //  [新增] 下拉刷新状态
    isRefreshing: Boolean = false,
    pullProgress: Float = 0f,  // 0.0 ~ 1.0+ 下拉进度
    pagerState: androidx.compose.foundation.pager.PagerState? = null, // [New] PagerState for sync
    // [New] LayerBackdrop for liquid glass effect
    backdrop: com.kyant.backdrop.backdrops.LayerBackdrop? = null,
    homeSettings: com.android.purebilibili.core.store.HomeSettings? = null,
    // [Feature] Tab Collapse Control
    tabContainerHeight: Dp = 44.dp,
    tabAlpha: Float = 1f
) {
    val haptic = rememberHapticFeedback()
    val density = LocalDensity.current

    // 状态栏高度
    val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    
    // [Feature] Liquid Glass Logic
    val isGlassEnabled = homeSettings?.isLiquidGlassEnabled == true

    //  读取当前模糊强度以确定背景透明度
    val blurIntensity by SettingsManager.getBlurIntensity(LocalContext.current)
        .collectAsState(initial = BlurIntensity.THIN)
    val backgroundAlpha = BlurStyles.getBackgroundAlpha(blurIntensity) * 0.8f // Slightly more transparent for glass
    
    val targetHeaderColor = if (isGlassEnabled) {
         MaterialTheme.colorScheme.surface.copy(alpha = 0.01f) // Almost transparent
    } else {
         MaterialTheme.colorScheme.surface.copy(alpha = if (hazeState != null) backgroundAlpha else 1f)
    }
    
    // [UX优化] 平滑过渡顶部栏背景色 (Smooth Header Color Transition)
    // 注意：这里保留颜色动画是没问题的，因为它不影响布局
    val animatedHeaderColor by animateColorAsState(
        targetValue = targetHeaderColor,
        animationSpec = androidx.compose.animation.core.tween<androidx.compose.ui.graphics.Color>(300),
        label = "headerColor"
    )
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .zIndex(10f) // Ensure high z-index for the whole header
            // [Revert] Removed Liquid Glass Effect due to performance issues
             .run {
                  this.then(if (hazeState != null) Modifier.unifiedBlur(hazeState) else Modifier)
                      .background(animatedHeaderColor)
             }
            .padding(bottom = 0.dp) // Reset padding, controlled by spacer
    ) {
        // 1. Status Bar Placeholder
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(statusBarHeight)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = {
                            haptic(HapticType.LIGHT)
                            onStatusBarDoubleTap()
                        }
                    )
                }
        )

        // 2. Search Bar + Avatar + Settings
        // 高度和透明度由外部直接控制，实现物理跟手
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(searchBarHeight)
                .graphicsLayer { alpha = searchBarAlpha }
                .clip(androidx.compose.ui.graphics.RectangleShape) // Ensure content is clipped when shrinking
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp) // 内部内容保持原始高度，通过父容器裁剪实现收缩
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Avatar
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .iOSTapEffect { onAvatarClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .border(1.dp, MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                    ) {
                        if (user.isLogin && user.face.isNotEmpty()) {
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(FormatUtils.fixImageUrl(user.face))
                                    .crossfade(true).build(),
                                contentDescription = "用户头像",
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Box(
                                Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceVariant),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("未", fontSize = 11.sp, fontWeight = FontWeight.Bold, 
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.width(8.dp))
                
                // Search Box
                // [优化] 外层容器用于居中，内层容器限制最大宽度 (640dp)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(36.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .widthIn(max = 640.dp)
                            .fillMaxWidth()
                            .height(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                            .clickable { 
                                haptic(HapticType.LIGHT)
                                onSearchClick() 
                            }
                            .padding(horizontal = 12.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                CupertinoIcons.Default.MagnifyingGlass,
                                contentDescription = "搜索",
                                tint = iOSSystemGray,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            // [优化] 响应式字体大小
                            val isTablet = com.android.purebilibili.core.util.LocalWindowSizeClass.current.isTablet
                            Text(
                                text = "搜索视频、UP主...",
                                style = MaterialTheme.typography.bodyMedium,
                                fontSize = if (isTablet) 16.sp else 15.sp,
                                fontWeight = FontWeight.Normal,
                                color = iOSSystemGray,
                                maxLines = 1
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.width(8.dp))
                
                // Settings Button
                IconButton(
                    onClick = { 
                        haptic(HapticType.LIGHT)
                        onSettingsClick() 
                    },
                    modifier = Modifier.size(44.dp)
                ) {
                    Icon(
                        CupertinoIcons.Default.Gear,
                        contentDescription = "设置",
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
        
        // 3. Category Tabs (Merged directly below Search Bar)
        // Tabs 始终显示，但会随 SearchBar 收缩而上移
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .zIndex(-1f) // Slide behind search bar
                .height(tabContainerHeight) // [Feature] Collapse Tabs
                .graphicsLayer { alpha = tabAlpha }
                .clip(RoundedCornerShape(bottomStart = 0.dp, bottomEnd = 0.dp))
        ) {
            CategoryTabRow(
                selectedIndex = categoryIndex,
                onCategorySelected = onCategorySelected,
                onPartitionClick = onPartitionClick,
                pagerState = pagerState
            )
        }
    }
}
