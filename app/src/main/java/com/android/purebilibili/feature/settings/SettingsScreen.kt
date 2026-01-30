package com.android.purebilibili.feature.settings

import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Process
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.purebilibili.core.store.SettingsManager
import com.android.purebilibili.core.util.AnalyticsHelper
import com.android.purebilibili.core.util.CacheUtils
import com.android.purebilibili.core.util.CrashReporter
import com.android.purebilibili.core.util.EasterEggs
import com.android.purebilibili.core.util.LocalWindowSizeClass
import com.android.purebilibili.core.util.LogCollector
import com.android.purebilibili.core.plugin.PluginManager

import dev.chrisbanes.haze.hazeSource
import io.github.alexzhirkevich.cupertino.icons.CupertinoIcons
import io.github.alexzhirkevich.cupertino.icons.filled.*
import io.github.alexzhirkevich.cupertino.icons.outlined.*
import kotlinx.coroutines.launch

import com.android.purebilibili.core.ui.components.IOSSectionTitle
import com.android.purebilibili.core.ui.animation.staggeredEntrance

const val GITHUB_URL = "https://github.com/jay3-yy/BiliPai/"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = viewModel(),
    onBack: () -> Unit,
    onOpenSourceLicensesClick: () -> Unit,
    onAppearanceClick: () -> Unit = {},
    onPlaybackClick: () -> Unit = {},
    onPermissionClick: () -> Unit = {},
    onPluginsClick: () -> Unit = {},
    onNavigateToBottomBarSettings: () -> Unit = {},
    onReplayOnboardingClick: () -> Unit = {},
    mainHazeState: dev.chrisbanes.haze.HazeState? = null
) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val scope = rememberCoroutineScope()
    val windowSizeClass = LocalWindowSizeClass.current
    
    // State Collection
    val state by viewModel.state.collectAsState()
    val privacyModeEnabled by SettingsManager.getPrivacyModeEnabled(context).collectAsState(initial = false)
    val crashTrackingEnabled by SettingsManager.getCrashTrackingEnabled(context).collectAsState(initial = true)
    val analyticsEnabled by SettingsManager.getAnalyticsEnabled(context).collectAsState(initial = true)
    val easterEggEnabled by SettingsManager.getEasterEggEnabled(context).collectAsState(initial = true)
    val customDownloadPath by SettingsManager.getDownloadPath(context).collectAsState(initial = null)
    val feedApiType by SettingsManager.getFeedApiType(context).collectAsState(
        initial = SettingsManager.FeedApiType.WEB
    )
    
    // Local UI State
    var showCacheDialog by remember { mutableStateOf(false) }
    var showCacheAnimation by remember { mutableStateOf(false) }
    var cacheProgress by remember { mutableStateOf<CacheClearProgress?>(null) }
    var versionClickCount by remember { mutableIntStateOf(0) }
    var showEasterEggDialog by remember { mutableStateOf(false) }
    var showPathDialog by remember { mutableStateOf(false) }
    // [新增] 权限引导弹窗状态
    var showPermissionDialog by remember { mutableStateOf(false) }
    // [新增] 目录选择对话框状态
    var showDirectoryPicker by remember { mutableStateOf(false) }
    
    // Haze State for this screen
    val activeHazeState = mainHazeState ?: remember { dev.chrisbanes.haze.HazeState() }

    // Directory Picker - 使用文件系统 API
    val defaultPath = remember { SettingsManager.getDefaultDownloadPath(context) }

    // Callbacks
    val onClearCacheAction = { showCacheDialog = true }
    val onDownloadPathAction = { showPathDialog = true }
    
    // Logic Callbacks
    val onPrivacyModeChange: (Boolean) -> Unit = { enabled ->
        scope.launch { SettingsManager.setPrivacyModeEnabled(context, enabled) }
    }
    val onCrashTrackingChange: (Boolean) -> Unit = { enabled ->
        scope.launch {
            SettingsManager.setCrashTrackingEnabled(context, enabled)
            CrashReporter.setEnabled(enabled)
        }
    }
    val onAnalyticsChange: (Boolean) -> Unit = { enabled ->
        scope.launch {
            SettingsManager.setAnalyticsEnabled(context, enabled)
            AnalyticsHelper.setEnabled(enabled)
        }
    }
    val onEasterEggChange: (Boolean) -> Unit = { enabled ->
        scope.launch { SettingsManager.setEasterEggEnabled(context, enabled) }
    }
    
    val onVersionClickAction: () -> Unit = {
        versionClickCount++
        val message = EasterEggs.getVersionClickMessage(versionClickCount)
        if (EasterEggs.isVersionEasterEggTriggered(versionClickCount)) {
            showEasterEggDialog = true
        } else if (versionClickCount >= 3) {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }
    
    val onExportLogsAction: () -> Unit = { LogCollector.exportAndShare(context) }
    val onTelegramClick: () -> Unit = { uriHandler.openUri("https://t.me/BiliPai") }
    val onTwitterClick: () -> Unit = { uriHandler.openUri("https://x.com/YangY_0x00") }
    val onGithubClick: () -> Unit = { uriHandler.openUri(GITHUB_URL) }

    // Effects
    LaunchedEffect(showCacheAnimation) {
        if (showCacheAnimation) {
            val breakdown = CacheUtils.getCacheBreakdown(context)
            val totalSize = breakdown.totalSize
            val clearedSizeStr = breakdown.format()
            for (i in 0..100 step 10) {
                cacheProgress = CacheClearProgress(
                    current = (totalSize * i / 100),
                    total = totalSize,
                    isComplete = false,
                    clearedSize = clearedSizeStr
                )
                kotlinx.coroutines.delay(150)
            }
            viewModel.clearCache()
            cacheProgress = CacheClearProgress(
                current = totalSize,
                total = totalSize,
                isComplete = true,
                clearedSize = clearedSizeStr
            )
        }
    }

    LaunchedEffect(Unit) {
        viewModel.refreshCacheSize()
        AnalyticsHelper.logScreenView("SettingsScreen")
    }
    
    //  Transparent Navigation Bar
    val view = androidx.compose.ui.platform.LocalView.current
    DisposableEffect(Unit) {
        val window = (context as? android.app.Activity)?.window
        val originalNavBarColor = window?.navigationBarColor ?: android.graphics.Color.TRANSPARENT
        if (window != null) window.navigationBarColor = android.graphics.Color.TRANSPARENT
        onDispose { if (window != null) window.navigationBarColor = originalNavBarColor }
    }

    // Dialogs
    if (showCacheDialog) {
        CacheClearConfirmDialog(
            cacheSize = state.cacheSize,
            onConfirm = { showCacheDialog = false; showCacheAnimation = true },
            onDismiss = { showCacheDialog = false }
        )
    }
    
    if (showCacheAnimation && cacheProgress != null) {
        CacheClearAnimationDialog(progress = cacheProgress!!, onDismiss = { showCacheAnimation = false; cacheProgress = null })
    }
    
    if (showPathDialog) {
        com.android.purebilibili.core.ui.IOSAlertDialog(
            onDismissRequest = { showPathDialog = false },
            title = { Text("下载位置", color = MaterialTheme.colorScheme.onSurface) },
            text = { 
                Column {
                    Text("默认位置（应用私有目录）：", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(defaultPath.substringAfterLast("Android/"), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(" 默认位置随应用卸载而删除，选择自定义位置可保留下载文件", style = MaterialTheme.typography.bodySmall, color = com.android.purebilibili.core.theme.iOSOrange)
                }
            },
            confirmButton = {
                com.android.purebilibili.core.ui.IOSDialogAction(onClick = { 
                    // [修复] 检查 MANAGE_EXTERNAL_STORAGE 权限
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !android.os.Environment.isExternalStorageManager()) {
                        showPathDialog = false
                        showPermissionDialog = true
                    } else {
                        showPathDialog = false
                        showDirectoryPicker = true  // [修复] 使用 DirectorySelectionDialog
                    }
                }) { Text("选择自定义目录") }
            },
            dismissButton = { 
                com.android.purebilibili.core.ui.IOSDialogAction(onClick = { 
                    scope.launch { SettingsManager.setDownloadPath(context, null) }
                    showPathDialog = false
                    Toast.makeText(context, "已重置为默认路径", Toast.LENGTH_SHORT).show()
                }) { Text("使用默认") } 
            }
        )
    }
    
    // [新增] 权限引导弹窗
    if (showPermissionDialog) {
        com.android.purebilibili.core.ui.IOSAlertDialog(
            onDismissRequest = { showPermissionDialog = false },
            title = { Text("需要权限", fontWeight = FontWeight.Bold) },
            text = { 
                Text("为了将视频下载到自定义目录（如 /Download/BiliPai），应用需要“管理所有文件”的权限。\n\n请在接下来的系统设置中允许此权限。")
            },
            confirmButton = {
                com.android.purebilibili.core.ui.IOSDialogAction(onClick = {
                    showPermissionDialog = false
                    try {
                        val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                            data = android.net.Uri.parse("package:${context.packageName}")
                        }
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        try {
                            val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            Toast.makeText(context, "无法打开设置页面，请手动授权", Toast.LENGTH_LONG).show()
                        }
                    }
                }) { Text("去授权", color = MaterialTheme.colorScheme.primary) }
            },
            dismissButton = {
                com.android.purebilibili.core.ui.IOSDialogAction(onClick = { showPermissionDialog = false }) { Text("取消") }
            }
        )
    }
    
    // [新增] 目录选择对话框（使用 File API）
    if (showDirectoryPicker) {
        com.android.purebilibili.feature.download.DirectorySelectionDialog(
            initialPath = android.os.Environment.getExternalStorageDirectory().absolutePath,
            onPathSelected = { path ->
                scope.launch { SettingsManager.setDownloadPath(context, path) }
                Toast.makeText(context, "下载路径已更新: $path", Toast.LENGTH_SHORT).show()
                showDirectoryPicker = false
            },
            onDismiss = { showDirectoryPicker = false }
        )
    }
    
    if (showEasterEggDialog) {
        com.android.purebilibili.core.ui.IOSAlertDialog(
            onDismissRequest = { showEasterEggDialog = false; versionClickCount = 0 },
            title = { Text(" 你发现了彩蛋！", fontWeight = FontWeight.Bold) },
            text = { Text("感谢你使用 BiliPai！这是一个用爱发电的开源项目。") },
            confirmButton = { com.android.purebilibili.core.ui.IOSDialogAction(onClick = { showEasterEggDialog = false; versionClickCount = 0 }) { Text("我知道了！") } }
        )
    }

    // Layout Switching
    Box(
        modifier = Modifier
            .fillMaxSize()
            .hazeSource(state = activeHazeState)
    ) {
        if (windowSizeClass.shouldUseSplitLayout) {
            TabletSettingsLayout(
                onBack = onBack,
                onAppearanceClick = onAppearanceClick,
                onPlaybackClick = onPlaybackClick,
                onPermissionClick = onPermissionClick,
                onPluginsClick = onPluginsClick,
                onExportLogsClick = onExportLogsAction,
                onLicenseClick = onOpenSourceLicensesClick,
                onGithubClick = onGithubClick,
                onVersionClick = onVersionClickAction,
                onReplayOnboardingClick = onReplayOnboardingClick,
                onTelegramClick = onTelegramClick,
                onTwitterClick = onTwitterClick,
                onDownloadPathClick = onDownloadPathAction,
                onClearCacheClick = onClearCacheAction,
                onPrivacyModeChange = onPrivacyModeChange,
                onCrashTrackingChange = onCrashTrackingChange,
                onAnalyticsChange = onAnalyticsChange,
                onEasterEggChange = onEasterEggChange,
                privacyModeEnabled = privacyModeEnabled,
                customDownloadPath = customDownloadPath,
                cacheSize = state.cacheSize,
                crashTrackingEnabled = crashTrackingEnabled,
                analyticsEnabled = analyticsEnabled,
                pluginCount = PluginManager.getEnabledCount(),
                versionName = com.android.purebilibili.BuildConfig.VERSION_NAME,
                easterEggEnabled = easterEggEnabled
            )
        } else {
            MobileSettingsLayout(
                onBack = onBack,
                onAppearanceClick = onAppearanceClick,
                onPlaybackClick = onPlaybackClick,
                onPermissionClick = onPermissionClick,
                onNavigateToBottomBarSettings = onNavigateToBottomBarSettings,
                onPluginsClick = onPluginsClick,
                onExportLogsClick = onExportLogsAction,
                onLicenseClick = onOpenSourceLicensesClick,
                onGithubClick = onGithubClick,
                onVersionClick = onVersionClickAction,
                onReplayOnboardingClick = onReplayOnboardingClick,
                onTelegramClick = onTelegramClick,
                onTwitterClick = onTwitterClick,
                onDownloadPathClick = onDownloadPathAction,
                onClearCacheClick = onClearCacheAction,
                onPrivacyModeChange = onPrivacyModeChange,
                onCrashTrackingChange = onCrashTrackingChange,
                onAnalyticsChange = onAnalyticsChange,
                onEasterEggChange = onEasterEggChange,
                privacyModeEnabled = privacyModeEnabled,
                customDownloadPath = customDownloadPath,
                cacheSize = state.cacheSize,
                crashTrackingEnabled = crashTrackingEnabled,
                analyticsEnabled = analyticsEnabled,
                pluginCount = PluginManager.getEnabledCount(),
                versionName = com.android.purebilibili.BuildConfig.VERSION_NAME,
                easterEggEnabled = easterEggEnabled,
                feedApiType = feedApiType,
                onFeedApiTypeChange = { type ->
                    scope.launch {
                        SettingsManager.setFeedApiType(context, type)
                        android.widget.Toast.makeText(
                            context, 
                            "已切换为${type.label}，下拉刷新生效", 
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            )
        }
        
        // Onboarding Bottom Sheet (Shared)

    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MobileSettingsLayout(
    onBack: () -> Unit,
    // Callbacks
    onAppearanceClick: () -> Unit,
    onPlaybackClick: () -> Unit,
    onPermissionClick: () -> Unit,
    onNavigateToBottomBarSettings: () -> Unit,
    onPluginsClick: () -> Unit,
    onExportLogsClick: () -> Unit,
    onLicenseClick: () -> Unit,
    onGithubClick: () -> Unit,
    onVersionClick: () -> Unit,
    onReplayOnboardingClick: () -> Unit,
    onTelegramClick: () -> Unit,
    onTwitterClick: () -> Unit,
    onDownloadPathClick: () -> Unit,
    onClearCacheClick: () -> Unit,
    
    // Logic Callbacks
    onPrivacyModeChange: (Boolean) -> Unit,
    onCrashTrackingChange: (Boolean) -> Unit,
    onAnalyticsChange: (Boolean) -> Unit,
    onEasterEggChange: (Boolean) -> Unit,
    
    // State
    privacyModeEnabled: Boolean,
    customDownloadPath: String?,
    cacheSize: String,
    crashTrackingEnabled: Boolean,
    analyticsEnabled: Boolean,
    pluginCount: Int,
    versionName: String,
    easterEggEnabled: Boolean,
    feedApiType: SettingsManager.FeedApiType,
    onFeedApiTypeChange: (SettingsManager.FeedApiType) -> Unit
) {
    val context = LocalContext.current
    
    // Animation Trigger
    var isVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        isVisible = true
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("设置", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(CupertinoIcons.Outlined.ChevronBackward, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                    actionIconContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0.dp)
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            // [修复] 增加底部内边距以适配自定义底栏 (80dp + systemBars)
            contentPadding = PaddingValues(
                bottom = 100.dp 
            )
        ) {
            item { 
                Box(modifier = Modifier.staggeredEntrance(0, isVisible)) {
                    IOSSectionTitle("关注作者") 
                }
            }
            item { 
                Box(modifier = Modifier.staggeredEntrance(1, isVisible)) {
                    FollowAuthorSection(onTelegramClick, onTwitterClick)  
                }
            }
            
            item { 
                Box(modifier = Modifier.staggeredEntrance(2, isVisible)) {
                    IOSSectionTitle("常规") 
                }
            }
            item { 
                Box(modifier = Modifier.staggeredEntrance(3, isVisible)) {
                    GeneralSection(
                        onAppearanceClick = onAppearanceClick,
                        onPlaybackClick = onPlaybackClick,
                        onBottomBarClick = onNavigateToBottomBarSettings
                    )
                }
            }
            
            item { 
                Box(modifier = Modifier.staggeredEntrance(4, isVisible)) {
                    IOSSectionTitle("推荐流") 
                }
            }
            item { 
                Box(modifier = Modifier.staggeredEntrance(5, isVisible)) {
                    FeedApiSection(
                        feedApiType = feedApiType,
                        onFeedApiTypeChange = onFeedApiTypeChange
                    )
                }
            }
            
            item { 
                Box(modifier = Modifier.staggeredEntrance(6, isVisible)) {
                    IOSSectionTitle("隐私与安全") 
                }
            }
            item { 
                Box(modifier = Modifier.staggeredEntrance(7, isVisible)) {
                    PrivacySection(privacyModeEnabled, onPrivacyModeChange, onPermissionClick)
                }
            }
            
            item { 
                Box(modifier = Modifier.staggeredEntrance(8, isVisible)) {
                    IOSSectionTitle("数据与存储") 
                }
            }
            item { 
                Box(modifier = Modifier.staggeredEntrance(9, isVisible)) {
                    DataStorageSection(customDownloadPath, cacheSize, onDownloadPathClick, onClearCacheClick)
                }
            }
            
            item { 
                Box(modifier = Modifier.staggeredEntrance(10, isVisible)) {
                    IOSSectionTitle("开发者选项") 
                }
            }
            item {
                Box(modifier = Modifier.staggeredEntrance(11, isVisible)) {
                    DeveloperSection(
                        crashTrackingEnabled, analyticsEnabled, pluginCount,
                        onCrashTrackingChange, onAnalyticsChange,
                        onPluginsClick, onExportLogsClick
                    )
                }
            }
            
            item { 
                Box(modifier = Modifier.staggeredEntrance(12, isVisible)) {
                    IOSSectionTitle("关于") 
                }
            }
            item {
                Box(modifier = Modifier.staggeredEntrance(13, isVisible)) {
                    AboutSection(
                        versionName, easterEggEnabled,
                        onLicenseClick, onGithubClick,
                        onVersionClick, onReplayOnboardingClick,
                        onEasterEggChange
                    )
                }
            }
            
            item { Spacer(modifier = Modifier.height(32.dp)) }
        }
    }
}