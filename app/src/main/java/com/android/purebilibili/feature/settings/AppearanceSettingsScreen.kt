// 文件路径: feature/settings/AppearanceSettingsScreen.kt
package com.android.purebilibili.feature.settings

import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
// 🍎 Cupertino Icons - iOS SF Symbols 风格图标
import io.github.alexzhirkevich.cupertino.icons.CupertinoIcons
import io.github.alexzhirkevich.cupertino.icons.outlined.*
import io.github.alexzhirkevich.cupertino.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.android.purebilibili.core.theme.iOSBlue
import com.android.purebilibili.core.theme.iOSPink
import com.android.purebilibili.core.theme.iOSPurple
import com.android.purebilibili.core.theme.iOSTeal
import com.android.purebilibili.core.ui.blur.BlurIntensity
import kotlinx.coroutines.launch

/**
 * 🍎 外观设置二级页面
 * iOS 风格设计
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppearanceSettingsScreen(
    viewModel: SettingsViewModel = viewModel(),
    onBack: () -> Unit,
    onNavigateToBottomBarSettings: () -> Unit = {},  // 🔥 底栏设置导航
    onNavigateToThemeSettings: () -> Unit = {},  // 🔥🔥 [新增] 主题设置导航
    onNavigateToIconSettings: () -> Unit = {},  // 🔥🔥 [新增] 图标设置导航
    onNavigateToAnimationSettings: () -> Unit = {}  // 🔥🔥 [新增] 动画设置导航
) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsState()
    
    var showThemeDialog by remember { mutableStateOf(false) }
    val displayLevel = when (state.displayMode) {
        0 -> 0.35f
        1 -> 0.6f
        else -> 0.85f
    }
    val appearanceInteractionLevel = (
        displayLevel +
            if (state.headerBlurEnabled) 0.1f else 0f +
            if (state.isBottomBarFloating) 0.1f else 0f
        ).coerceIn(0f, 1f)
    val appearanceAnimationSpeed = if (state.dynamicColor) 1.1f else 1f
    
    // 🔥🔥 [修复] 设置导航栏透明，确保底部手势栏沉浸式效果
    val view = androidx.compose.ui.platform.LocalView.current
    androidx.compose.runtime.DisposableEffect(Unit) {
        val window = (context as? android.app.Activity)?.window
        val originalNavBarColor = window?.navigationBarColor ?: android.graphics.Color.TRANSPARENT
        
        if (window != null) {
            window.navigationBarColor = android.graphics.Color.TRANSPARENT
        }
        
        onDispose {
            if (window != null) {
                window.navigationBarColor = originalNavBarColor
            }
        }
    }
    
    // 主题模式弹窗
    if (showThemeDialog) {
        AlertDialog(
            onDismissRequest = { showThemeDialog = false },
            title = { Text("外观模式", color = MaterialTheme.colorScheme.onSurface) },
            text = {
                Column {
                    AppThemeMode.entries.forEach { mode ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.setThemeMode(mode)
                                    showThemeDialog = false
                                }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = (state.themeMode == mode),
                                onClick = {
                                    viewModel.setThemeMode(mode)
                                    showThemeDialog = false
                                },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = MaterialTheme.colorScheme.primary,
                                    unselectedColor = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = mode.label, color = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }
            },
            confirmButton = { 
                TextButton(onClick = { showThemeDialog = false }) { 
                    Text("取消", color = MaterialTheme.colorScheme.primary) 
                } 
            },
            containerColor = MaterialTheme.colorScheme.surface,
            textContentColor = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("外观设置", fontWeight = FontWeight.SemiBold) },
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
        // 🔥🔥 [修复] 禁用 Scaffold 默认的 WindowInsets 消耗，避免底部填充
        contentWindowInsets = WindowInsets(0.dp)
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            // 🔥🔥 [修复] 添加底部导航栏内边距，确保沉浸式效果
            contentPadding = WindowInsets.navigationBars.asPaddingValues()
        ) {
            
            // 🔥🔥 [新增] 快速入口
            item { SettingsSectionTitle("快速入口") }
            item {
                SettingsGroup {
                    // 主题设置
                    SettingClickableItem(
                        icon = CupertinoIcons.Default.MoonStars,
                        title = "主题设置",
                        value = state.themeMode.label,
                        onClick = onNavigateToThemeSettings,
                        iconTint = iOSBlue
                    )
                    Divider()
                    // 图标设置
                    SettingClickableItem(
                        icon = CupertinoIcons.Default.SquareStack3dUp,
                        title = "应用图标",
                        value = when(state.appIcon) {
                            "3D" -> "3D立体"
                            "Blue" -> "经典蓝"
                            "Retro" -> "复古怀旧"
                            "Flat" -> "扁平现代"
                            "Flat Material" -> "扁平材质"
                            "Neon" -> "霓虹"
                            "Telegram Blue" -> "纸飞机蓝"
                            "Pink" -> "樱花粉"
                            "Purple" -> "香芋紫"
                            "Green" -> "薄荷绿"
                            "Dark" -> "暗夜蓝"
                            else -> "默认"
                        },
                        onClick = onNavigateToIconSettings,
                        iconTint = iOSPurple
                    )
                    Divider()
                    // 动画设置
                    SettingClickableItem(
                        icon = CupertinoIcons.Default.WandAndStars,
                        title = "动画与效果",
                        value = if (state.cardAnimationEnabled) "已开启" else "已关闭",
                        onClick = onNavigateToAnimationSettings,
                        iconTint = iOSPink
                    )
                }
            }
            
            // 🍎 首页展示 - 抽屉式选择
            item { SettingsSectionTitle("首页展示") }
            item {
                SettingsGroup {
                    val displayMode = state.displayMode
                    var isExpanded by remember { mutableStateOf(false) }
                    
                    // 当前选中模式的名称
                    val currentModeName = DisplayMode.entries.find { it.value == displayMode }?.title ?: "双列网格"
                    
                    Column(modifier = Modifier.padding(16.dp)) {
                        // 标题行 - 可点击展开/收起
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { isExpanded = !isExpanded }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                CupertinoIcons.Default.SquareOnSquare,
                                contentDescription = null,
                                tint = iOSBlue,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "展示样式",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = currentModeName,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Icon(
                                imageVector = if (isExpanded) CupertinoIcons.Default.ChevronUp else CupertinoIcons.Default.ChevronDown,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        
                        // 展开后的选项 - 带动画
                        androidx.compose.animation.AnimatedVisibility(
                            visible = isExpanded,
                            enter = androidx.compose.animation.expandVertically() + androidx.compose.animation.fadeIn(),
                            exit = androidx.compose.animation.shrinkVertically() + androidx.compose.animation.fadeOut()
                        ) {
                            Column(
                                modifier = Modifier.padding(top = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                DisplayMode.entries.forEach { mode ->
                                    val isSelected = displayMode == mode.value
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(
                                                if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                            )
                                            .clickable {
                                                viewModel.setDisplayMode(mode.value)
                                                isExpanded = false
                                            }
                                            .padding(horizontal = 14.dp, vertical = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                mode.title,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                                color = if (isSelected) MaterialTheme.colorScheme.primary 
                                                        else MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                mode.description,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                            )
                                        }
                                        if (isSelected) {
                                            Icon(
                                                CupertinoIcons.Default.Checkmark,
                                                contentDescription = "已选择",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 🍎 界面效果
            item { SettingsSectionTitle("界面效果") }
            item {
                val scope = rememberCoroutineScope()
                val bottomBarVisibilityMode by com.android.purebilibili.core.store.SettingsManager
                    .getBottomBarVisibilityMode(context).collectAsState(
                        initial = com.android.purebilibili.core.store.SettingsManager.BottomBarVisibilityMode.ALWAYS_VISIBLE
                    )
                
                SettingsGroup {
                    // 🔥🔥 [导航入口] 底栏管理
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNavigateToBottomBarSettings() }
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(iOSBlue.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                CupertinoIcons.Default.Menucard,
                                contentDescription = null,
                                tint = iOSBlue,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "底栏管理",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "自定义底栏项目和顺序",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(
                            CupertinoIcons.Default.ChevronForward,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                    }
                    
                    Divider()
                    
                    // ==================== 抽屉类选择器 ====================
                    
                    // 🔥 底栏显示模式选择（抽屉式）
                    var visibilityModeExpanded by remember { mutableStateOf(false) }
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { visibilityModeExpanded = !visibilityModeExpanded }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                CupertinoIcons.Default.Eye,
                                contentDescription = null,
                                tint = com.android.purebilibili.core.theme.iOSOrange,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "底栏显示模式",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = bottomBarVisibilityMode.label,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Icon(
                                imageVector = if (visibilityModeExpanded) CupertinoIcons.Default.ChevronUp else CupertinoIcons.Default.ChevronDown,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        
                        // 展开后的选项
                        androidx.compose.animation.AnimatedVisibility(
                            visible = visibilityModeExpanded,
                            enter = androidx.compose.animation.expandVertically() + androidx.compose.animation.fadeIn(),
                            exit = androidx.compose.animation.shrinkVertically() + androidx.compose.animation.fadeOut()
                        ) {
                            Column(
                                modifier = Modifier.padding(top = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                com.android.purebilibili.core.store.SettingsManager.BottomBarVisibilityMode.entries.forEach { mode ->
                                    val isSelected = mode == bottomBarVisibilityMode
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(
                                                if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                            )
                                            .clickable {
                                                scope.launch {
                                                    com.android.purebilibili.core.store.SettingsManager
                                                        .setBottomBarVisibilityMode(context, mode)
                                                }
                                                visibilityModeExpanded = false
                                            }
                                            .padding(horizontal = 14.dp, vertical = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                mode.label,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                                color = if (isSelected) MaterialTheme.colorScheme.primary 
                                                        else MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                mode.description,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                            )
                                        }
                                        if (isSelected) {
                                            Icon(
                                                CupertinoIcons.Default.Checkmark,
                                                contentDescription = "已选择",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    
                    Divider()
                    
                    // 🔥 底栏标签样式（选择器）
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                CupertinoIcons.Default.Tag,
                                contentDescription = null,
                                tint = iOSPurple,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text(
                                    text = "底栏标签样式",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = when(state.bottomBarLabelMode) {
                                        0 -> "图标 + 文字"
                                        2 -> "仅文字"
                                        else -> "仅图标"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            // 三种模式选择按钮
                            listOf(
                                Triple(0, "图标+文字", CupertinoIcons.Default.House),
                                Triple(1, "仅图标", CupertinoIcons.Default.Heart),
                                Triple(2, "仅文字", CupertinoIcons.Default.Character)
                            ).forEach { (mode, label, icon) ->
                                val isSelected = state.bottomBarLabelMode == mode
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable { viewModel.setBottomBarLabelMode(mode) }
                                        .background(
                                            if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                            else Color.Transparent
                                        )
                                        .padding(horizontal = 16.dp, vertical = 8.dp)
                                ) {
                                    Icon(
                                        icon,
                                        contentDescription = null,
                                        tint = if (isSelected) MaterialTheme.colorScheme.primary
                                               else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary
                                                else MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                                    )
                                }
                            }
                        }
                    }
                    
                    Divider()
                    
                    // ==================== 开关类设置 ====================
                    
                    // 🔥 悬浮底栏开关
                    SettingSwitchItem(
                        icon = CupertinoIcons.Default.RectangleStack,
                        title = "悬浮底栏",
                        subtitle = "关闭后底栏将沉浸式贴底显示",
                        checked = state.isBottomBarFloating,
                        onCheckedChange = { viewModel.toggleBottomBarFloating(it) },
                        iconTint = iOSTeal
                    )
                    
                    Divider()
                    
                    // 🔥 底栏磨砂效果开关
                    SettingSwitchItem(
                        icon = CupertinoIcons.Default.Sparkles,
                        title = "底栏磨砂效果",
                        subtitle = "底部导航栏的毛玻璃模糊",
                        checked = state.bottomBarBlurEnabled,
                        onCheckedChange = { viewModel.toggleBottomBarBlur(it) },
                        iconTint = iOSBlue
                    )
                    
                    // 🔥 模糊强度选择（仅在磨砂开启时显示）
                    if (state.bottomBarBlurEnabled) {
                        Divider()
                        BlurIntensitySelector(
                            selectedIntensity = state.blurIntensity,
                            onIntensityChange = { viewModel.setBlurIntensity(it) }
                        )
                    }
                    
                    Divider()
                    
                    // 🔥 卡片进场动画开关
                    SettingSwitchItem(
                        icon = CupertinoIcons.Default.WandAndStars,
                        title = "卡片进场动画",
                        subtitle = "首页视频卡片的入场动画效果",
                        checked = state.cardAnimationEnabled,
                        onCheckedChange = { viewModel.toggleCardAnimation(it) },
                        iconTint = iOSPink
                    )
                    
                    Divider()
                    
                    // 🔥 卡片过渡动画开关
                    SettingSwitchItem(
                        icon = CupertinoIcons.Default.ArrowLeftArrowRight,
                        title = "卡片过渡动画",
                        subtitle = "点击卡片时的共享元素过渡效果",
                        checked = state.cardTransitionEnabled,
                        onCheckedChange = { viewModel.toggleCardTransition(it) },
                        iconTint = iOSTeal
                    )
                }
            }
        }
    }
}
/**
 * 🔥 模糊强度选择器 (可展开/收起)
 */
@Composable
fun BlurIntensitySelector(
    selectedIntensity: BlurIntensity,
    onIntensityChange: (BlurIntensity) -> Unit,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(false) }
    
    // 获取当前选中项的显示文本
    val currentTitle = when (selectedIntensity) {
        BlurIntensity.THIN -> "标准"
        BlurIntensity.THICK -> "浓郁"
        BlurIntensity.APPLE_DOCK -> "玻璃拟态"
    }
    
    Column(modifier = modifier.padding(horizontal = 16.dp)) {
        // 标题行 - 可点击展开/收起
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .clickable { isExpanded = !isExpanded }
                .padding(vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                CupertinoIcons.Default.Sparkles,
                contentDescription = null,
                tint = iOSBlue,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "模糊强度",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = currentTitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            // 展开/收起箭头
            Icon(
                imageVector = if (isExpanded) CupertinoIcons.Default.ChevronUp else CupertinoIcons.Default.ChevronDown,
                contentDescription = if (isExpanded) "收起" else "展开",
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.size(24.dp)
            )
        }
        
        // 展开后的选项 - 带动画
        androidx.compose.animation.AnimatedVisibility(
            visible = isExpanded,
            enter = androidx.compose.animation.expandVertically() + androidx.compose.animation.fadeIn(),
            exit = androidx.compose.animation.shrinkVertically() + androidx.compose.animation.fadeOut()
        ) {
            Column(modifier = Modifier.padding(start = 40.dp, top = 4.dp, bottom = 8.dp)) {
                // 🔥 [调整] 顺序：标准 → 玻璃拟态 → 浓郁
                BlurIntensityOption(
                    icon = CupertinoIcons.Default.CheckmarkCircle,
                    iconTint = iOSBlue,
                    title = "标准",
                    description = "平衡美观与性能（推荐）",
                    isSelected = selectedIntensity == BlurIntensity.THIN,
                    onClick = { 
                        onIntensityChange(BlurIntensity.THIN)
                        isExpanded = false
                    }
                )
                Spacer(modifier = Modifier.height(8.dp))
                // 🔥 玻璃拟态风格 - 移到中间
                BlurIntensityOption(
                    icon = CupertinoIcons.Default.Desktopcomputer,
                    iconTint = com.android.purebilibili.core.theme.iOSSystemGray,
                    title = "玻璃拟态",
                    description = "强烈模糊，完全遮盖背景",
                    isSelected = selectedIntensity == BlurIntensity.APPLE_DOCK,
                    onClick = { 
                        onIntensityChange(BlurIntensity.APPLE_DOCK)
                        isExpanded = false
                    }
                )
                Spacer(modifier = Modifier.height(8.dp))
                // 🔥 浓郁 - 移到最后，有背景透色
                BlurIntensityOption(
                    icon = CupertinoIcons.Default.Sparkle,
                    iconTint = iOSPurple,
                    title = "浓郁",
                    description = "背景颜色透出 + 磨砂质感",
                    isSelected = selectedIntensity == BlurIntensity.THICK,
                    onClick = { 
                        onIntensityChange(BlurIntensity.THICK)
                        isExpanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun BlurIntensityOption(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    title: String,
    description: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .background(
                if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                else Color.Transparent
            )
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = isSelected,
            onClick = onClick,
            colors = RadioButtonDefaults.colors(
                selectedColor = MaterialTheme.colorScheme.primary
            )
        )
        Spacer(modifier = Modifier.width(8.dp))
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * 🔥🔥 动态取色预览组件
 * 显示从壁纸提取的 Material You 颜色
 */
@Composable
fun DynamicColorPreview() {
    val colorScheme = MaterialTheme.colorScheme
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Text(
            text = "当前取色预览",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Primary
            ColorPreviewItem(
                color = colorScheme.primary,
                label = "主色",
                modifier = Modifier.weight(1f)
            )
            // Secondary
            ColorPreviewItem(
                color = colorScheme.secondary,
                label = "辅色",
                modifier = Modifier.weight(1f)
            )
            // Tertiary
            ColorPreviewItem(
                color = colorScheme.tertiary,
                label = "第三色",
                modifier = Modifier.weight(1f)
            )
            // Primary Container
            ColorPreviewItem(
                color = colorScheme.primaryContainer,
                label = "容器",
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun ColorPreviewItem(
    color: Color,
    label: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(color)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
