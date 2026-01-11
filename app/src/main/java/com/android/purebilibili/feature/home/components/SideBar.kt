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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.chrisbanes.haze.HazeState
import com.android.purebilibili.core.ui.blur.unifiedBlur
import com.android.purebilibili.core.util.HapticType
import com.android.purebilibili.core.util.rememberHapticFeedback
import com.android.purebilibili.core.theme.BottomBarColors
import kotlinx.coroutines.launch
import io.github.alexzhirkevich.cupertino.icons.CupertinoIcons
import io.github.alexzhirkevich.cupertino.icons.outlined.*
import io.github.alexzhirkevich.cupertino.icons.filled.*

/**
 * 平板端侧边导航栏 - 垂直版本的 FrostedBottomBar
 */
@Composable
fun FrostedSideBar(
    currentItem: BottomNavItem = BottomNavItem.HOME,
    onItemClick: (BottomNavItem) -> Unit,
    modifier: Modifier = Modifier,
    hazeState: HazeState? = null,
    onHomeDoubleTap: () -> Unit = {},
    visibleItems: List<BottomNavItem> = listOf(BottomNavItem.HOME, BottomNavItem.DYNAMIC, BottomNavItem.HISTORY, BottomNavItem.PROFILE),
    itemColorIndices: Map<String, Int> = emptyMap() // Keep explicit map type to match usage
) {
    val haptic = rememberHapticFeedback()
    
    // 读取模糊设置
    val context = androidx.compose.ui.platform.LocalContext.current
    val blurIntensity by com.android.purebilibili.core.store.SettingsManager.getBlurIntensity(context)
        .collectAsState(initial = com.android.purebilibili.core.ui.blur.BlurIntensity.THIN)
    val backgroundAlpha = com.android.purebilibili.core.ui.blur.BlurStyles.getBackgroundAlpha(blurIntensity)

    val sideBarWidth = 80.dp
    // 垂直胶囊的高度
    val capsuleHeight = 48.dp 
    
    Surface(
        modifier = modifier
            .width(sideBarWidth)
            .fillMaxHeight()
            .then(
                if (hazeState != null) {
                    Modifier.unifiedBlur(hazeState)
                } else {
                    Modifier.background(MaterialTheme.colorScheme.surface)
                }
            ),
        color = if (hazeState != null) {
            MaterialTheme.colorScheme.surface.copy(alpha = backgroundAlpha)
        } else {
            MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
        },
        border = if (hazeState != null) {
             androidx.compose.foundation.BorderStroke(
                width = 0.5.dp,
                brush = androidx.compose.ui.graphics.Brush.horizontalGradient(
                    colors = listOf(
                        Color.Transparent,
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                    )
                )
            )
        } else {
            androidx.compose.foundation.BorderStroke(
                width = 0.5.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top // 从顶部开始排列
        ) {
            // Logo 或 顶部空间 (可选)
            // Spacer(modifier = Modifier.height(20.dp))

            // 导航项列表
            visibleItems.forEach { item ->
                val isSelected = item == currentItem
                
                // 动画状态
                var isPending by remember { mutableStateOf(false) }
                var wobbleAngle by remember { mutableFloatStateOf(0f) }
                
                // 颜色动画
                val primaryColor = MaterialTheme.colorScheme.primary
                val unselectedColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                
                val iconColor by animateColorAsState(
                    targetValue = if (isSelected || isPending) primaryColor else unselectedColor,
                    animationSpec = spring(),
                    label = "iconColor"
                )

                // 缩放动画
                val scale by animateFloatAsState(
                    targetValue = if (isSelected) 1.15f else 1.0f,
                    animationSpec = spring(dampingRatio = 0.35f, stiffness = 300f),
                    label = "scale"
                )

                // 晃动动画
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

                // 交互容器
                Column(
                    modifier = Modifier
                        .size(64.dp) // 增大点击区域
                        .then(
                            if (item == BottomNavItem.HOME) {
                                Modifier.pointerInput(Unit) {
                                    detectTapGestures(
                                        onTap = {
                                            isPending = true
                                            haptic(HapticType.LIGHT)
                                            kotlinx.coroutines.MainScope().launch {
                                                kotlinx.coroutines.delay(100)
                                                wobbleAngle = 15f
                                                kotlinx.coroutines.delay(150)
                                                onItemClick(item)
                                                isPending = false
                                            }
                                        },
                                        onDoubleTap = {
                                            haptic(HapticType.MEDIUM)
                                            onHomeDoubleTap()
                                        }
                                    )
                                }
                            } else {
                                Modifier.clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) {
                                    isPending = true
                                    haptic(HapticType.LIGHT)
                                    kotlinx.coroutines.MainScope().launch {
                                        kotlinx.coroutines.delay(100)
                                        wobbleAngle = 15f
                                        kotlinx.coroutines.delay(150)
                                        onItemClick(item)
                                        isPending = false
                                    }
                                }
                            }
                        ),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .graphicsLayer {
                                scaleX = scale
                                scaleY = scale
                                rotationZ = animatedWobble
                            }
                    ) {
                         CompositionLocalProvider(LocalContentColor provides iconColor) {
                            if (isSelected) item.selectedIcon() else item.unselectedIcon()
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    Text(
                        text = item.label,
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 10.sp,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                        color = iconColor
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp)) // 项目间距
            }
        }
    }
}
