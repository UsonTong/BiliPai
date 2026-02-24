// 文件路径: feature/settings/PlaybackSettingsScreen.kt
package com.android.purebilibili.feature.settings

import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Process
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
//  Cupertino Icons - iOS SF Symbols 风格图标
import io.github.alexzhirkevich.cupertino.icons.CupertinoIcons
import io.github.alexzhirkevich.cupertino.icons.outlined.*
import io.github.alexzhirkevich.cupertino.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.purebilibili.core.store.SettingsManager
import com.android.purebilibili.core.ui.adaptive.resolveDeviceUiProfile
import com.android.purebilibili.core.ui.adaptive.resolveEffectiveMotionTier
import com.android.purebilibili.core.store.PlaybackCompletionBehavior
import com.android.purebilibili.core.theme.iOSGreen
import com.android.purebilibili.core.theme.iOSTeal
import com.android.purebilibili.core.theme.iOSOrange
import com.android.purebilibili.core.theme.iOSSystemGray
import com.android.purebilibili.core.util.LocalWindowSizeClass
import kotlinx.coroutines.launch
import com.android.purebilibili.core.ui.components.*
import com.android.purebilibili.core.ui.animation.staggeredEntrance

/**
 *  播放设置二级页面
 * iOS 风格设计
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaybackSettingsScreen(
    viewModel: SettingsViewModel = viewModel(),
    onBack: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("播放设置", fontWeight = FontWeight.SemiBold) },
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
        Box(modifier = Modifier.padding(padding)) {
             PlaybackSettingsContent(viewModel = viewModel, state = state)
        }
    }
}

/**
 * 播放设置内容 - 可在 BottomSheet 中或分栏布局中复用
 */
@Composable
fun PlaybackSettingsContent(
    viewModel: SettingsViewModel,
    state: SettingsUiState,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val windowSizeClass = LocalWindowSizeClass.current
    // val state by viewModel.state.collectAsState() // Moved to parameter
    val prefs = remember { context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE) }
    var isVisible by remember { mutableStateOf(false) }
    val deviceUiProfile = remember(windowSizeClass.widthSizeClass) {
        resolveDeviceUiProfile(
            widthSizeClass = windowSizeClass.widthSizeClass
        )
    }
    val effectiveMotionTier = remember(deviceUiProfile.motionTier, state.cardAnimationEnabled) {
        resolveEffectiveMotionTier(
            baseTier = deviceUiProfile.motionTier,
            animationEnabled = state.cardAnimationEnabled
        )
    }

    LaunchedEffect(Unit) {
        isVisible = true
    }
    
    var isStatsEnabled by remember { mutableStateOf(prefs.getBoolean("show_stats", false)) }
    var showPipPermissionDialog by remember { mutableStateOf(false) }
    
    // 获取动态圆角用于统一风格
    // 注意：这里需要导入 LocalCornerRadiusScale，如果该文件没有导入，可能需要添加。
    // 假设 iOSCornerRadius 和 LocalCornerRadiusScale 未在此文件导入，先使用硬编码或尝试导入
    // 为了稳妥，这里先检查导入。原文件没有导入这些。
    // 但为了保持原样，我先不做动态圆角修改，或者之后再做。
    
    val miniPlayerMode by com.android.purebilibili.core.store.SettingsManager
        .getMiniPlayerMode(context).collectAsState(
            initial = com.android.purebilibili.core.store.SettingsManager.MiniPlayerMode.OFF
        )
    val stopPlaybackOnExit by com.android.purebilibili.core.store.SettingsManager
        .getStopPlaybackOnExit(context).collectAsState(initial = false)
    val defaultPlaybackSpeed by com.android.purebilibili.core.store.SettingsManager
        .getDefaultPlaybackSpeed(context).collectAsState(initial = 1.0f)
    val rememberLastPlaybackSpeed by com.android.purebilibili.core.store.SettingsManager
        .getRememberLastPlaybackSpeed(context).collectAsState(initial = false)
    val videoCodecPreference by com.android.purebilibili.core.store.SettingsManager
        .getVideoCodec(context).collectAsState(initial = "hev1")
    val videoSecondCodecPreference by com.android.purebilibili.core.store.SettingsManager
        .getVideoSecondCodec(context).collectAsState(initial = "avc1")
    
    // ... [保留原有逻辑: checkPipPermission, gotoPipSettings] ...
    
    // 检查画中画权限
    fun checkPipPermission(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
            val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                appOps.unsafeCheckOpNoThrow(
                    AppOpsManager.OPSTR_PICTURE_IN_PICTURE,
                    Process.myUid(),
                    context.packageName
                )
            } else {
                @Suppress("DEPRECATION")
                appOps.checkOpNoThrow(
                    AppOpsManager.OPSTR_PICTURE_IN_PICTURE,
                    Process.myUid(),
                    context.packageName
                )
            }
            return mode == AppOpsManager.MODE_ALLOWED
        }
        return false
    }
    
    // 跳转到系统设置
    fun gotoPipSettings() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val intent = Intent(
                    "android.settings.PICTURE_IN_PICTURE_SETTINGS",
                    Uri.parse("package:${context.packageName}")
                )
                context.startActivity(intent)
            }
        } catch (e: Exception) {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            intent.data = Uri.parse("package:${context.packageName}")
            context.startActivity(intent)
        }
    }
    
    // 权限弹窗逻辑
    if (showPipPermissionDialog) {
        com.android.purebilibili.core.ui.IOSAlertDialog(
            onDismissRequest = { showPipPermissionDialog = false },
            title = { Text("权限申请", color = MaterialTheme.colorScheme.onSurface) },
            text = { Text("检测到未开启「画中画」权限。请在设置中开启该权限，否则无法使用小窗播放。", color = MaterialTheme.colorScheme.onSurfaceVariant) },
            confirmButton = {
                com.android.purebilibili.core.ui.IOSDialogAction(
                    onClick = {
                        gotoPipSettings()
                        showPipPermissionDialog = false
                    }
                ) { Text("去设置") }
            },
            dismissButton = {
                com.android.purebilibili.core.ui.IOSDialogAction(onClick = { showPipPermissionDialog = false }) {
                    Text("暂不开启", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        )
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = WindowInsets.navigationBars.asPaddingValues()
    ) {
            
            //  解码设置
            //  解码设置
            item {
                Box(modifier = Modifier.staggeredEntrance(0, isVisible, motionTier = effectiveMotionTier)) {
                    IOSSectionTitle("解码")
                }
            }
            item {
                Box(modifier = Modifier.staggeredEntrance(1, isVisible, motionTier = effectiveMotionTier)) {
                    val scope = rememberCoroutineScope()
                    val codecOptions = listOf(
                        PlaybackSegmentOption("avc1", "AVC"),
                        PlaybackSegmentOption("hev1", "HEVC"),
                        PlaybackSegmentOption("av01", "AV1")
                    )
                    fun codecDescription(codec: String): String = when (codec) {
                        "avc1" -> "兼容性最佳"
                        "hev1" -> "推荐，画质与体积更平衡"
                        "av01" -> "高压缩，设备要求更高"
                        else -> "未知"
                    }
                    IOSGroup {
                        IOSSwitchItem(
                            icon = CupertinoIcons.Default.Cpu,
                            title = "启用硬件解码",
                            subtitle = "减少发热和耗电 (推荐开启)",
                            checked = state.hwDecode,
                            onCheckedChange = { 
                                viewModel.toggleHwDecode(it)
                                //  [埋点] 设置变更追踪
                                com.android.purebilibili.core.util.AnalyticsHelper.logSettingChange("hw_decode", it.toString())
                            },
                            iconTint = iOSGreen
                        )
                        Divider()
                        IOSSlidingSegmentedSetting(
                            title = "首选编码：${resolveSelectionLabel(codecOptions, videoCodecPreference, fallbackLabel = "AVC")}",
                            subtitle = codecDescription(videoCodecPreference),
                            options = codecOptions,
                            selectedValue = videoCodecPreference,
                            onSelectionChange = { codec ->
                                scope.launch {
                                    com.android.purebilibili.core.store.SettingsManager
                                        .setVideoCodec(context, codec)
                                }
                            }
                        )
                        Divider()
                        IOSSlidingSegmentedSetting(
                            title = "次选编码：${resolveSelectionLabel(codecOptions, videoSecondCodecPreference, fallbackLabel = "HEVC")}",
                            subtitle = codecDescription(videoSecondCodecPreference),
                            options = codecOptions,
                            selectedValue = videoSecondCodecPreference,
                            onSelectionChange = { codec ->
                                scope.launch {
                                    com.android.purebilibili.core.store.SettingsManager
                                        .setVideoSecondCodec(context, codec)
                                }
                            }
                        )
                    }
                }
            }

            item {
                Box(modifier = Modifier.staggeredEntrance(2, isVisible, motionTier = effectiveMotionTier)) {
                    IOSSectionTitle("播放速度")
                }
            }
            item {
                Box(modifier = Modifier.staggeredEntrance(3, isVisible, motionTier = effectiveMotionTier)) {
                    val scope = rememberCoroutineScope()
                    IOSGroup {
                        IOSSwitchItem(
                            icon = CupertinoIcons.Default.Clock,
                            title = "记忆上次播放速度",
                            subtitle = if (rememberLastPlaybackSpeed) {
                                "新视频将优先使用你最后一次手动设置的速度"
                            } else {
                                "关闭时将使用默认播放速度"
                            },
                            checked = rememberLastPlaybackSpeed,
                            onCheckedChange = {
                                scope.launch {
                                    com.android.purebilibili.core.store.SettingsManager
                                        .setRememberLastPlaybackSpeed(context, it)
                                }
                            },
                            iconTint = com.android.purebilibili.core.theme.iOSBlue
                        )
                        Divider()
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "默认播放速度",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            val speedOptions = listOf(
                                PlaybackSegmentOption(0.5f, "0.5x"),
                                PlaybackSegmentOption(0.75f, "0.75x"),
                                PlaybackSegmentOption(1.0f, "1x"),
                                PlaybackSegmentOption(1.25f, "1.25x"),
                                PlaybackSegmentOption(1.3f, "1.3x"),
                                PlaybackSegmentOption(1.5f, "1.5x"),
                                PlaybackSegmentOption(2.0f, "2x")
                            )
                            IOSSlidingSegmentedControl(
                                options = speedOptions,
                                selectedValue = defaultPlaybackSpeed,
                                onSelectionChange = { speed ->
                                    scope.launch {
                                        com.android.purebilibili.core.store.SettingsManager
                                            .setDefaultPlaybackSpeed(context, speed)
                                    }
                                }
                            )
                        }
                    }
                }
            }
            
            //  小窗播放
            item {
                Box(modifier = Modifier.staggeredEntrance(6, isVisible, motionTier = effectiveMotionTier)) {
                    IOSSectionTitle("小窗播放")
                }
            }
            item {
                Box(modifier = Modifier.staggeredEntrance(7, isVisible, motionTier = effectiveMotionTier)) {
                    val scope = rememberCoroutineScope()
                    val miniPlayerOptions = listOf(
                        PlaybackSegmentOption(com.android.purebilibili.core.store.SettingsManager.MiniPlayerMode.OFF, "默认"),
                        PlaybackSegmentOption(com.android.purebilibili.core.store.SettingsManager.MiniPlayerMode.IN_APP_ONLY, "应用内小窗"),
                        PlaybackSegmentOption(com.android.purebilibili.core.store.SettingsManager.MiniPlayerMode.SYSTEM_PIP, "画中画")
                    )
                    
                    IOSGroup {
                        IOSSwitchItem(
                            icon = CupertinoIcons.Default.Pip,
                            title = "离开播放页后停止",
                            subtitle = "不进入小窗/画中画，也不保留后台播放",
                            checked = stopPlaybackOnExit,
                            onCheckedChange = {
                                scope.launch {
                                    com.android.purebilibili.core.store.SettingsManager
                                        .setStopPlaybackOnExit(context, it)
                                }
                            },
                            iconTint = iOSOrange
                        )
                        Divider()
                        IOSSlidingSegmentedSetting(
                            title = "后台播放模式：${if (stopPlaybackOnExit) "已覆盖" else miniPlayerMode.label}",
                            subtitle = if (stopPlaybackOnExit) {
                                "已由“离开播放页后停止”覆盖，后台模式暂不生效"
                            } else {
                                miniPlayerMode.description
                            },
                            options = miniPlayerOptions,
                            selectedValue = miniPlayerMode,
                            enabled = !stopPlaybackOnExit,
                            onSelectionChange = { mode ->
                                scope.launch {
                                    com.android.purebilibili.core.store.SettingsManager
                                        .setMiniPlayerMode(context, mode)
                                }
                                if (mode == com.android.purebilibili.core.store.SettingsManager.MiniPlayerMode.SYSTEM_PIP &&
                                    !checkPipPermission()
                                ) {
                                    showPipPermissionDialog = true
                                }
                            }
                        )
                        
                        //  权限提示（仅当选择系统PiP且无权限时显示）
                        if (!stopPlaybackOnExit &&
                            miniPlayerMode == com.android.purebilibili.core.store.SettingsManager.MiniPlayerMode.SYSTEM_PIP
                            && !checkPipPermission()) {
                            Divider()
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { showPipPermissionDialog = true }
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    CupertinoIcons.Default.ExclamationmarkTriangle,
                                    contentDescription = null,
                                    tint = iOSOrange,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        "画中画权限未开启",
                                        fontSize = 14.sp,
                                        color = iOSOrange
                                    )
                                    Text(
                                        "点击前往系统设置开启",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                    )
                                }
                                Icon(
                                    CupertinoIcons.Default.ChevronForward,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
            
            //  手势设置
            item {
                Box(modifier = Modifier.staggeredEntrance(4, isVisible, motionTier = effectiveMotionTier)) {
                    IOSSectionTitle("手势控制")
                }
            }
            item {
                Box(modifier = Modifier.staggeredEntrance(5, isVisible, motionTier = effectiveMotionTier)) {
                    IOSGroup {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    CupertinoIcons.Default.HandTap,
                                    contentDescription = null,
                                    tint = iOSOrange,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "手势灵敏度",
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "调整快进/音量/亮度手势响应速度",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Text(
                                    text = "${(state.gestureSensitivity * 100).toInt()}%",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    "较慢",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                //  iOS 风格滑块
                                io.github.alexzhirkevich.cupertino.CupertinoSlider(
                                    value = state.gestureSensitivity,
                                    onValueChange = { viewModel.setGestureSensitivity(it) },
                                    valueRange = 0.5f..2.0f,
                                    steps = 5,
                                    modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
                                )
                                Text(
                                    "较快",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
            
            //  调试选项
            item {
                Box(modifier = Modifier.staggeredEntrance(8, isVisible, motionTier = effectiveMotionTier)) {
                    IOSSectionTitle("调试")
                }
            }
            item {
                Box(modifier = Modifier.staggeredEntrance(9, isVisible, motionTier = effectiveMotionTier)) {
                    IOSGroup {
                        IOSSwitchItem(
                            icon = CupertinoIcons.Default.ChartBar,
                            title = "详细统计信息",
                            subtitle = "显示 Codec、码率等 Geek 信息",
                            checked = isStatsEnabled,
                            onCheckedChange = {
                                isStatsEnabled = it
                                prefs.edit().putBoolean("show_stats", it).apply()
                            },
                            iconTint = iOSSystemGray
                        )
                    }
                }
            }
            
            //  交互设置
            item {
                Box(modifier = Modifier.staggeredEntrance(10, isVisible, motionTier = effectiveMotionTier)) {
                    IOSSectionTitle("交互")
                }
            }
            item {
                Box(modifier = Modifier.staggeredEntrance(11, isVisible, motionTier = effectiveMotionTier)) {
                    val scope = rememberCoroutineScope()
                    val swipeHidePlayerEnabled by com.android.purebilibili.core.store.SettingsManager
                        .getSwipeHidePlayerEnabled(context).collectAsState(initial = false)
                    val portraitSwipeToFullscreenEnabled by com.android.purebilibili.core.store.SettingsManager
                        .getPortraitSwipeToFullscreenEnabled(context).collectAsState(initial = true)
                    val fullscreenSwipeSeekEnabled by com.android.purebilibili.core.store.SettingsManager
                        .getFullscreenSwipeSeekEnabled(context).collectAsState(initial = true)
                    val fullscreenSwipeSeekSeconds by com.android.purebilibili.core.store.SettingsManager
                        .getFullscreenSwipeSeekSeconds(context).collectAsState(initial = 15)
                    
                    //  [新增] 自动播放下一个
                    val autoPlayEnabled by com.android.purebilibili.core.store.SettingsManager
                        .getAutoPlay(context).collectAsState(initial = true)
                    val playbackCompletionBehavior by com.android.purebilibili.core.store.SettingsManager
                        .getPlaybackCompletionBehavior(context)
                        .collectAsState(initial = PlaybackCompletionBehavior.CONTINUE_CURRENT_LOGIC)
                    
                    IOSGroup {
                        // --- Click to Play ---
                        val clickToPlayEnabled by com.android.purebilibili.core.store.SettingsManager
                            .getClickToPlay(context).collectAsState(initial = true)

                        IOSSwitchItem(
                            icon = CupertinoIcons.Default.Play,
                            title = "点击视频直接播放",
                            subtitle = "进入视频详情页时自动开始播放",
                            checked = clickToPlayEnabled,
                            onCheckedChange = {
                                scope.launch {
                                    com.android.purebilibili.core.store.SettingsManager
                                        .setClickToPlay(context, it)
                                }
                            },
                            iconTint = com.android.purebilibili.core.theme.iOSBlue
                        )
                        Divider()
                        //  [新增] 自动播放下一个视频
                        IOSSwitchItem(
                            icon = CupertinoIcons.Default.ForwardEnd,
                            title = "自动播放下一个",
                            subtitle = "视频结束后自动播放推荐视频",
                            checked = autoPlayEnabled,
                            onCheckedChange = { 
                                scope.launch {
                                    com.android.purebilibili.core.store.SettingsManager
                                        .setAutoPlay(context, it)
                                }
                            },
                            iconTint = com.android.purebilibili.core.theme.iOSPurple
                        )
                        Divider()
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val playbackOrderOptions = listOf(
                                PlaybackSegmentOption(PlaybackCompletionBehavior.STOP_AFTER_CURRENT, "暂停"),
                                PlaybackSegmentOption(PlaybackCompletionBehavior.PLAY_IN_ORDER, "顺序"),
                                PlaybackSegmentOption(PlaybackCompletionBehavior.REPEAT_ONE, "单循"),
                                PlaybackSegmentOption(PlaybackCompletionBehavior.LOOP_PLAYLIST, "列表循"),
                                PlaybackSegmentOption(PlaybackCompletionBehavior.CONTINUE_CURRENT_LOGIC, "自动")
                            )
                            IOSSlidingSegmentedSetting(
                                title = "选择播放顺序：${playbackCompletionBehavior.label}",
                                subtitle = "稍后再看推荐选择“顺序播放”",
                                options = playbackOrderOptions,
                                selectedValue = playbackCompletionBehavior,
                                onSelectionChange = { behavior ->
                                    scope.launch {
                                        com.android.purebilibili.core.store.SettingsManager
                                            .setPlaybackCompletionBehavior(context, behavior)
                                    }
                                }
                            )
                            Text(
                                text = "稍后再看推荐选择“顺序播放”即可连续播放下一条，不需要退出重选。",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Divider()
                        IOSSwitchItem(
                            icon = CupertinoIcons.Default.HandThumbsup,
                            title = "双击点赞",
                            subtitle = "双击视频画面快捷点赞",
                            checked = state.doubleTapLike,
                            onCheckedChange = { 
                                viewModel.toggleDoubleTapLike(it)
                                //  [埋点] 设置变更追踪
                                com.android.purebilibili.core.util.AnalyticsHelper.logSettingChange("double_tap_like", it.toString())
                            },
                            iconTint = com.android.purebilibili.core.theme.iOSPink
                        )
                        Divider()
                        IOSSwitchItem(
                            icon = CupertinoIcons.Default.HandDraw,  // 手势图标
                            title = "上滑隐藏播放器",
                            subtitle = "竖屏模式下拉评论区隐藏播放器",
                            checked = swipeHidePlayerEnabled,
                            onCheckedChange = { 
                                scope.launch {
                                    com.android.purebilibili.core.store.SettingsManager
                                        .setSwipeHidePlayerEnabled(context, it)
                                }
                            },
                            iconTint = com.android.purebilibili.core.theme.iOSBlue
                        )

                        Divider()
                        IOSSwitchItem(
                            icon = CupertinoIcons.Default.ArrowLeftArrowRight,
                            title = "竖屏上滑进入全屏",
                            subtitle = if (portraitSwipeToFullscreenEnabled) {
                                "开启后上滑可直接进入横屏全屏"
                            } else {
                                "关闭后上滑不再强制进入全屏"
                            },
                            checked = portraitSwipeToFullscreenEnabled,
                            onCheckedChange = {
                                scope.launch {
                                    com.android.purebilibili.core.store.SettingsManager
                                        .setPortraitSwipeToFullscreenEnabled(context, it)
                                }
                            },
                            iconTint = iOSTeal
                        )

                        Divider()
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "横屏滑动快进/快退步长",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Switch(
                                    checked = fullscreenSwipeSeekEnabled,
                                    onCheckedChange = {
                                        scope.launch {
                                            com.android.purebilibili.core.store.SettingsManager
                                                .setFullscreenSwipeSeekEnabled(context, it)
                                        }
                                    }
                                )
                            }
                            Text(
                                text = if (fullscreenSwipeSeekEnabled) {
                                    "左右滑动时每档跳转秒数：当前 ${fullscreenSwipeSeekSeconds}s"
                                } else {
                                    "已关闭固定步长（当前设定 ${fullscreenSwipeSeekSeconds}s，重新开启后生效）"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            val seekStepOptions = listOf(
                                PlaybackSegmentOption(10, "10s"),
                                PlaybackSegmentOption(15, "15s"),
                                PlaybackSegmentOption(20, "20s"),
                                PlaybackSegmentOption(30, "30s")
                            )
                            IOSSlidingSegmentedControl(
                                options = seekStepOptions,
                                selectedValue = fullscreenSwipeSeekSeconds,
                                enabled = fullscreenSwipeSeekEnabled,
                                onSelectionChange = { seconds ->
                                    if (!fullscreenSwipeSeekEnabled) return@IOSSlidingSegmentedControl
                                    scope.launch {
                                        com.android.purebilibili.core.store.SettingsManager
                                            .setFullscreenSwipeSeekSeconds(context, seconds)
                                    }
                                }
                            )
                        }
                        
                        // 🔄 [新增] 自动横竖屏切换
                        Divider()
                        val autoRotateEnabled by com.android.purebilibili.core.store.SettingsManager
                            .getAutoRotateEnabled(context).collectAsState(initial = false)
                        IOSSwitchItem(
                            icon = CupertinoIcons.Default.ArrowTriangle2CirclepathCamera,  // 旋转图标
                            title = "自动横竖屏切换",
                            subtitle = "跟随手机方向自动进入/退出全屏",
                            checked = autoRotateEnabled,
                            onCheckedChange = { 
                                scope.launch {
                                    com.android.purebilibili.core.store.SettingsManager
                                        .setAutoRotateEnabled(context, it)
                                }
                            },
                            iconTint = iOSTeal
                        )
                    }
                }
            }
            
            //  网络与画质
            item {
                Box(modifier = Modifier.staggeredEntrance(12, isVisible, motionTier = effectiveMotionTier)) {
                    IOSSectionTitle("网络与画质")
                }
            }
            item {
                Box(modifier = Modifier.staggeredEntrance(13, isVisible, motionTier = effectiveMotionTier)) {
                    val scope = rememberCoroutineScope()
                    val wifiQuality by com.android.purebilibili.core.store.SettingsManager
                        .getWifiQuality(context).collectAsState(initial = 80)
                    val mobileQuality by com.android.purebilibili.core.store.SettingsManager
                        .getMobileQuality(context).collectAsState(initial = 64)
                    
                    // 🚀 [新增] 自动最高画质
                    val autoHighestQuality by com.android.purebilibili.core.store.SettingsManager
                        .getAutoHighestQuality(context).collectAsState(initial = false)
                    
                    // 画质选项列表
                    val qualityOptions = listOf(
                        PlaybackSegmentOption(116, "1080P60"),
                        PlaybackSegmentOption(80, "1080P"),
                        PlaybackSegmentOption(64, "720P"),
                        PlaybackSegmentOption(32, "480P"),
                        PlaybackSegmentOption(16, "360P")
                    )
                    
                    fun getQualityLabel(id: Int): String = resolveSelectionLabel(
                        options = qualityOptions,
                        selectedValue = id,
                        fallbackLabel = "720P"
                    )
                    
                    IOSGroup {
                        // 🚀 自动最高画质开关（置顶）
                        IOSSwitchItem(
                            icon = CupertinoIcons.Default.Sparkles,
                            title = "自动最高画质",
                            subtitle = if (autoHighestQuality) "已开启：始终使用视频最高可用画质" else "开启后忽略下方画质设置",
                            checked = autoHighestQuality,
                            onCheckedChange = {
                                scope.launch {
                                    com.android.purebilibili.core.store.SettingsManager
                                        .setAutoHighestQuality(context, it)
                                }
                            },
                            iconTint = com.android.purebilibili.core.theme.iOSPurple
                        )
                        
                        Divider()

                        IOSSlidingSegmentedSetting(
                            title = "WiFi 默认画质：${getQualityLabel(wifiQuality)}",
                            subtitle = if (autoHighestQuality) {
                                "已开启自动最高画质，此选项将暂时不生效"
                            } else {
                                "仅 WiFi 环境生效"
                            },
                            options = qualityOptions,
                            selectedValue = wifiQuality,
                            enabled = !autoHighestQuality,
                            onSelectionChange = { qualityId ->
                                scope.launch {
                                    com.android.purebilibili.core.store.SettingsManager
                                        .setWifiQuality(context, qualityId)
                                }
                            }
                        )
                        
                        Divider()
                        
                        // 📉 读取省流量模式，用于显示提示
                        val dataSaverModeForHint by com.android.purebilibili.core.store.SettingsManager
                            .getDataSaverMode(context).collectAsState(
                                initial = com.android.purebilibili.core.store.SettingsManager.DataSaverMode.MOBILE_ONLY
                            )
                        val isDataSaverActive = dataSaverModeForHint != com.android.purebilibili.core.store.SettingsManager.DataSaverMode.OFF
                        val effectiveQuality = resolveEffectiveMobileQuality(
                            rawMobileQuality = mobileQuality,
                            isDataSaverActive = isDataSaverActive
                        )
                        val effectiveQualityLabel = getQualityLabel(effectiveQuality)
                        
                        IOSSlidingSegmentedSetting(
                            title = "流量 默认画质：${getQualityLabel(mobileQuality)}",
                            subtitle = when {
                                autoHighestQuality -> "已开启自动最高画质，此选项将暂时不生效"
                                isDataSaverActive && mobileQuality > effectiveQuality ->
                                    "省流量模式当前实际最高为 $effectiveQualityLabel"
                                else -> "仅移动网络环境生效"
                            },
                            options = qualityOptions,
                            selectedValue = mobileQuality,
                            enabled = !autoHighestQuality,
                            onSelectionChange = { qualityId ->
                                scope.launch {
                                    com.android.purebilibili.core.store.SettingsManager
                                        .setMobileQuality(context, qualityId)
                                }
                            }
                        )

                        if (isDataSaverActive && mobileQuality > effectiveQuality) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "省流量模式已限制为最高480P",
                                    fontSize = 11.sp,
                                    color = iOSGreen.copy(alpha = 0.8f)
                                )
                            }
                        }
                    }
                }
            }
            
            // 📉 省流量模式
            item {
                Box(modifier = Modifier.staggeredEntrance(14, isVisible, motionTier = effectiveMotionTier)) {
                    IOSSectionTitle("省流量")
                }
            }
            item {
                Box(modifier = Modifier.staggeredEntrance(15, isVisible, motionTier = effectiveMotionTier)) {
                    val scope = rememberCoroutineScope()
                    val dataSaverMode by com.android.purebilibili.core.store.SettingsManager
                        .getDataSaverMode(context).collectAsState(
                            initial = com.android.purebilibili.core.store.SettingsManager.DataSaverMode.MOBILE_ONLY
                        )
                    val dataSaverModeOptions = listOf(
                        PlaybackSegmentOption(com.android.purebilibili.core.store.SettingsManager.DataSaverMode.OFF, "关闭"),
                        PlaybackSegmentOption(com.android.purebilibili.core.store.SettingsManager.DataSaverMode.MOBILE_ONLY, "仅移动数据"),
                        PlaybackSegmentOption(com.android.purebilibili.core.store.SettingsManager.DataSaverMode.ALWAYS, "始终开启")
                    )
                    
                    IOSGroup {
                        IOSSlidingSegmentedSetting(
                            title = "省流量模式：${dataSaverMode.label}",
                            subtitle = dataSaverMode.description,
                            options = dataSaverModeOptions,
                            selectedValue = dataSaverMode,
                            onSelectionChange = { mode ->
                                scope.launch {
                                    com.android.purebilibili.core.store.SettingsManager
                                        .setDataSaverMode(context, mode)
                                }
                            }
                        )
                        
                        //  功能说明
                        Divider()
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(
                                CupertinoIcons.Default.InfoCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                "开启后将自动降低封面图质量、禁用预加载、限制视频最高480P",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                lineHeight = 16.sp
                            )
                        }
                    }
                }
            }
            
            item { Spacer(modifier = Modifier.height(32.dp)) }
}
}
