// 文件路径: feature/settings/AnimationSettingsScreen.kt
package com.android.purebilibili.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable // [Fix] Missing import
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.purebilibili.core.theme.*
import com.android.purebilibili.core.ui.blur.BlurIntensity
import io.github.alexzhirkevich.cupertino.icons.CupertinoIcons
import io.github.alexzhirkevich.cupertino.icons.outlined.*
import kotlinx.coroutines.launch
import com.android.purebilibili.core.ui.components.*
import com.android.purebilibili.core.ui.animation.staggeredEntrance
import kotlinx.coroutines.delay
import android.os.Build

/**
 *  动画与效果设置二级页面
 * 管理卡片动画、过渡效果、磨砂效果等
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnimationSettingsScreen(
    viewModel: SettingsViewModel = viewModel(),
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsState()
    val scope = rememberCoroutineScope()
    val blurLevel = when (state.blurIntensity) {
        BlurIntensity.THIN -> 0.5f
        BlurIntensity.THICK -> 0.8f
        BlurIntensity.APPLE_DOCK -> 1.0f  //  玻璃拟态风格
    }
    val animationInteractionLevel = (
        0.2f +
            if (state.cardAnimationEnabled) 0.25f else 0f +
            if (state.cardTransitionEnabled) 0.25f else 0f +
            if (state.bottomBarBlurEnabled) 0.2f else 0f +
            blurLevel * 0.2f
        ).coerceIn(0f, 1f)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("动画与效果", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(CupertinoIcons.Default.ChevronBackward, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0.dp)
    ) { padding ->
        AnimationSettingsContent(
            modifier = Modifier.padding(padding),
            state = state,
            viewModel = viewModel
        )
    }
}

@Composable
fun AnimationSettingsContent(
    modifier: Modifier = Modifier,
    state: SettingsUiState,
    viewModel: SettingsViewModel
) {
    var isVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { isVisible = true }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = WindowInsets.navigationBars.asPaddingValues()
    ) {
            
            //  卡片动画
            //  卡片动画
            item {
                Box(modifier = Modifier.staggeredEntrance(0, isVisible)) {
                    IOSSectionTitle("卡片动画")
                }
            }
            item {
                Box(modifier = Modifier.staggeredEntrance(1, isVisible)) {
                    IOSGroup {
                        IOSSwitchItem(
                            icon = CupertinoIcons.Default.WandAndStars,
                            title = "进场动画",
                            subtitle = "首页视频卡片的入场动画效果",
                            checked = state.cardAnimationEnabled,
                            onCheckedChange = { viewModel.toggleCardAnimation(it) },
                            iconTint = iOSPink
                        )
                        Divider()
                        IOSSwitchItem(
                            icon = CupertinoIcons.Default.ArrowLeftArrowRight,
                            title = "过渡动画",
                            subtitle = "点击卡片时的共享元素过渡效果",
                            checked = state.cardTransitionEnabled,
                            onCheckedChange = { viewModel.toggleCardTransition(it) },
                            iconTint = iOSTeal
                        )
                    }
                }
            }
            
            // ✨ 视觉效果
            item {
                Box(modifier = Modifier.staggeredEntrance(2, isVisible)) {
                    IOSSectionTitle("视觉效果")
                }
            }
            item {
                Box(modifier = Modifier.staggeredEntrance(3, isVisible)) {
                    IOSGroup {
                        // Android 13+ 显示液态玻璃
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                             IOSSwitchItem(
                                icon = CupertinoIcons.Default.Drop, 
                                title = "液态玻璃", 
                                subtitle = "底栏指示器的实时折射效果",
                                checked = state.isLiquidGlassEnabled, 
                                onCheckedChange = { viewModel.toggleLiquidGlass(it) },
                                iconTint = iOSBlue
                            )
                            // Style Selector (Only visible when enabled)
                            androidx.compose.animation.AnimatedVisibility(
                                visible = state.isLiquidGlassEnabled,
                                enter = androidx.compose.animation.expandVertically() + androidx.compose.animation.fadeIn(),
                                exit = androidx.compose.animation.shrinkVertically() + androidx.compose.animation.fadeOut()
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        "风格选择", 
                                        style = MaterialTheme.typography.labelSmall, 
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        // Classic
                                        LiquidGlassStyleCard(
                                            title = "Classic",
                                            subtitle = "流体波纹",
                                            isSelected = state.liquidGlassStyle == com.android.purebilibili.core.store.LiquidGlassStyle.CLASSIC,
                                            onClick = { viewModel.setLiquidGlassStyle(com.android.purebilibili.core.store.LiquidGlassStyle.CLASSIC) },
                                            modifier = Modifier.weight(1f)
                                        )
                                        // SimpMusic
                                        LiquidGlassStyleCard(
                                            title = "SimpMusic",
                                            subtitle = "自适应透镜",
                                            isSelected = state.liquidGlassStyle == com.android.purebilibili.core.store.LiquidGlassStyle.SIMP_MUSIC,
                                            onClick = { viewModel.setLiquidGlassStyle(com.android.purebilibili.core.store.LiquidGlassStyle.SIMP_MUSIC) },
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            }
                            Divider()
                        }

                        // 磨砂效果 (始终显示)
                        IOSSwitchItem(
                            icon = CupertinoIcons.Default.Sparkles,
                            title = "顶部栏磨砂",
                            subtitle = "顶部导航栏的毛玻璃模糊效果",
                            checked = state.headerBlurEnabled,
                            onCheckedChange = { viewModel.toggleHeaderBlur(it) },
                            iconTint = iOSBlue
                        )
                        Divider()
                        IOSSwitchItem(
                            icon = CupertinoIcons.Default.Sparkles,
                            title = "底栏磨砂",
                            subtitle = "底部导航栏的毛玻璃模糊效果",
                            checked = state.bottomBarBlurEnabled,
                            onCheckedChange = { viewModel.toggleBottomBarBlur(it) },
                            iconTint = iOSBlue
                        )
                        
                        // 模糊强度（仅在任意模糊开启时显示）
                        if (state.headerBlurEnabled || state.bottomBarBlurEnabled) {
                            Divider()
                            BlurIntensitySelector(
                                selectedIntensity = state.blurIntensity,
                                onIntensityChange = { viewModel.setBlurIntensity(it) }
                            )
                        }
                    }
                }
            }
            
            // 📐 底栏样式
            // 📐 底栏样式
            item {
                Box(modifier = Modifier.staggeredEntrance(4, isVisible)) {
                    IOSSectionTitle("底栏样式")
                }
            }
            item {
                Box(modifier = Modifier.staggeredEntrance(5, isVisible)) {
                    IOSGroup {
                        IOSSwitchItem(
                            icon = CupertinoIcons.Default.RectangleStack,
                            title = "悬浮底栏",
                            subtitle = "关闭后底栏将沉浸式贴底显示",
                            checked = state.isBottomBarFloating,
                            onCheckedChange = { viewModel.toggleBottomBarFloating(it) },
                            iconTint = iOSPurple
                        )
                    }
                }
            }
            
            //  提示
            //  提示
            item {
                Box(modifier = Modifier.staggeredEntrance(6, isVisible)) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                CupertinoIcons.Default.Lightbulb,
                                contentDescription = null,
                                tint = iOSOrange,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "关闭动画可以减少电量消耗，提升流畅度",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
            
            item { Spacer(modifier = Modifier.height(32.dp)) }
        }
    }


@Composable
private fun LiquidGlassStyleCard(
    title: String,
    subtitle: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha=0.5f)
    val contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
    
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(color)
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = contentColor
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = contentColor.copy(alpha = 0.8f)
            )
        }
    }
}
