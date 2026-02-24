package com.android.purebilibili.feature.settings

import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable // [New]
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.Image
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.purebilibili.core.store.SettingsManager
import com.android.purebilibili.core.util.AnalyticsHelper
import com.android.purebilibili.core.util.CacheUtils
import com.android.purebilibili.core.util.CrashReporter
import com.android.purebilibili.core.util.EasterEggs
import com.android.purebilibili.core.util.LocalWindowSizeClass
import com.android.purebilibili.core.util.LogCollector
import com.android.purebilibili.core.ui.adaptive.resolveDeviceUiProfile
import com.android.purebilibili.core.ui.adaptive.resolveEffectiveMotionTier
import com.android.purebilibili.core.plugin.PluginManager

import dev.chrisbanes.haze.hazeSource
import io.github.alexzhirkevich.cupertino.icons.CupertinoIcons
import io.github.alexzhirkevich.cupertino.icons.filled.*
import io.github.alexzhirkevich.cupertino.icons.outlined.*
import kotlinx.coroutines.launch

import com.android.purebilibili.core.ui.components.IOSSectionTitle
import com.android.purebilibili.core.ui.animation.staggeredEntrance

const val GITHUB_URL = OFFICIAL_GITHUB_URL

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
    onWebDavBackupClick: () -> Unit = {},
    onNavigateToBottomBarSettings: () -> Unit = {},
    onTipsClick: () -> Unit = {}, // [Feature] Tips
    onReplayOnboardingClick: () -> Unit = {},
    mainHazeState: dev.chrisbanes.haze.HazeState? = null
) {
    val context = LocalContext.current
    val hapticFeedback = LocalHapticFeedback.current
    val uriHandler = LocalUriHandler.current
    val scope = rememberCoroutineScope()
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val versionClickThreshold = EasterEggs.VERSION_EASTER_EGG_THRESHOLD
    
    // State Collection
    val state by viewModel.state.collectAsState()
    val privacyModeEnabled by SettingsManager.getPrivacyModeEnabled(context).collectAsState(initial = false)
    val crashTrackingEnabled by SettingsManager.getCrashTrackingEnabled(context).collectAsState(initial = true)
    val analyticsEnabled by SettingsManager.getAnalyticsEnabled(context).collectAsState(initial = true)
    val easterEggEnabled by SettingsManager.getEasterEggEnabled(context).collectAsState(initial = true)
    val customDownloadPath by SettingsManager.getDownloadPath(context).collectAsState(initial = null)
    val downloadExportTreeUri by SettingsManager.getDownloadExportTreeUri(context).collectAsState(initial = null)
    val feedApiType by SettingsManager.getFeedApiType(context).collectAsState(
        initial = SettingsManager.FeedApiType.WEB
    )
    val incrementalTimelineRefreshEnabled by SettingsManager.getIncrementalTimelineRefresh(context)
        .collectAsState(initial = false)
    
    // Local UI State
    var showCacheDialog by remember { mutableStateOf(false) }
    var showCacheAnimation by remember { mutableStateOf(false) }
    var cacheProgress by remember { mutableStateOf<CacheClearProgress?>(null) }
    var versionClickCount by remember { mutableIntStateOf(0) }
    var showEasterEggDialog by remember { mutableStateOf(false) }
    var showPathDialog by remember { mutableStateOf(false) }
    // [新增] 打赏对话框
    var showDonateDialog by remember { mutableStateOf(false) }
    var showReleaseDisclaimerDialog by remember { mutableStateOf(false) }
    var isCheckingUpdate by remember { mutableStateOf(false) }
    var updateStatusText by remember { mutableStateOf("点击检查") }
    var updateCheckResult by remember { mutableStateOf<AppUpdateCheckResult?>(null) }
    
    // [新增] 黑名单页面状态
    var showBlockedList by remember { mutableStateOf(false) }

    // Haze State for this screen
    val activeHazeState = mainHazeState ?: remember { dev.chrisbanes.haze.HazeState() }

    // Directory Picker - 使用文件系统 API
    val defaultPath = remember { SettingsManager.getDefaultDownloadPath(context) }
    val downloadFolderPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult

        val takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        runCatching {
            context.contentResolver.takePersistableUriPermission(uri, takeFlags)
        }

        scope.launch {
            SettingsManager.setDownloadExportTreeUri(context, uri.toString())
            SettingsManager.setDownloadPath(context, null)
        }
        Toast.makeText(context, "已设置导出目录", Toast.LENGTH_SHORT).show()
    }

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
        val message = EasterEggs.getVersionClickMessage(
            clickCount = versionClickCount,
            threshold = versionClickThreshold
        )
        val remainingClicks = (versionClickThreshold - versionClickCount).coerceAtLeast(0)
        val hapticType = if (remainingClicks <= 1) {
            HapticFeedbackType.LongPress
        } else {
            HapticFeedbackType.TextHandleMove
        }
        hapticFeedback.performHapticFeedback(hapticType)

        if (EasterEggs.isVersionEasterEggTriggered(versionClickCount, versionClickThreshold)) {
            showEasterEggDialog = true
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        } else if (versionClickCount >= 2 || remainingClicks <= 3) {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }
    
    val onExportLogsAction: () -> Unit = { LogCollector.exportAndShare(context) }
    val onTelegramClick: () -> Unit = { uriHandler.openUri(OFFICIAL_TELEGRAM_URL) }
    val onTwitterClick: () -> Unit = { uriHandler.openUri("https://x.com/YangY_0x00") }
    val onGithubClick: () -> Unit = { uriHandler.openUri(OFFICIAL_GITHUB_URL) }
    val onDisclaimerClick: () -> Unit = { showReleaseDisclaimerDialog = true }
    val onBlockedListClickAction: () -> Unit = { showBlockedList = true }
    val onCheckUpdateAction: () -> Unit = {
        if (isCheckingUpdate) {
            Toast.makeText(context, "正在检查更新，请稍候", Toast.LENGTH_SHORT).show()
        } else {
            scope.launch {
                isCheckingUpdate = true
                updateStatusText = "检查中..."
                val result = AppUpdateChecker.check(com.android.purebilibili.BuildConfig.VERSION_NAME)
                result.onSuccess { info ->
                    updateStatusText = info.message
                    if (info.isUpdateAvailable) {
                        updateCheckResult = info
                    } else {
                        Toast.makeText(context, info.message, Toast.LENGTH_SHORT).show()
                    }
                }.onFailure { error ->
                    updateStatusText = "检查失败"
                    Toast.makeText(context, error.message ?: "更新检查失败，请稍后重试", Toast.LENGTH_SHORT).show()
                }
                isCheckingUpdate = false
            }
        }
    }

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
            val clearResult = viewModel.clearCache()
            if (shouldMarkCacheClearAnimationComplete(clearResult.isSuccess)) {
                cacheProgress = CacheClearProgress(
                    current = totalSize,
                    total = totalSize,
                    isComplete = true,
                    clearedSize = clearedSizeStr
                )
            } else {
                Toast.makeText(
                    context,
                    resolveCacheClearFailureMessage(clearResult.exceptionOrNull()),
                    Toast.LENGTH_SHORT
                ).show()
                showCacheAnimation = false
                cacheProgress = null
            }
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
                    Text(
                        "可选：通过系统文件夹授权设置导出目录（无需“管理所有文件”权限）",
                        style = MaterialTheme.typography.bodySmall,
                        color = com.android.purebilibili.core.theme.iOSOrange
                    )
                    if (!downloadExportTreeUri.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "当前导出目录：已设置",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            confirmButton = {
                com.android.purebilibili.core.ui.IOSDialogAction(onClick = { 
                    showPathDialog = false
                    downloadFolderPicker.launch(null)
                }) { Text("选择导出目录") }
            },
            dismissButton = { 
                com.android.purebilibili.core.ui.IOSDialogAction(onClick = { 
                    scope.launch {
                        SettingsManager.setDownloadPath(context, null)
                        SettingsManager.setDownloadExportTreeUri(context, null)
                    }
                    showPathDialog = false
                    Toast.makeText(context, "已恢复仅应用内存储", Toast.LENGTH_SHORT).show()
                }) { Text("仅使用默认") } 
            }
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

    if (showDonateDialog) {
        DonateDialog(onDismiss = { showDonateDialog = false })
    }

    if (showReleaseDisclaimerDialog) {
        ReleaseChannelDisclaimerDialog(
            onDismiss = { showReleaseDisclaimerDialog = false },
            onOpenGithub = onGithubClick,
            onOpenTelegram = onTelegramClick
        )
    }

    updateCheckResult?.let { info ->
        val notesPreview = info.releaseNotes.trim().let { notes ->
            if (notes.isBlank()) "暂无更新说明" else notes.take(240)
        }
        com.android.purebilibili.core.ui.IOSAlertDialog(
            onDismissRequest = { updateCheckResult = null },
            title = { Text("发现新版本 v${info.latestVersion}") },
            text = {
                Text(
                    "当前版本 v${info.currentVersion}\n\n$notesPreview",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                com.android.purebilibili.core.ui.IOSDialogAction(onClick = {
                    updateCheckResult = null
                    uriHandler.openUri(info.releaseUrl)
                }) { Text("前往下载") }
            },
            dismissButton = {
                com.android.purebilibili.core.ui.IOSDialogAction(onClick = {
                    updateCheckResult = null
                }) { Text("稍后") }
            }
        )
    }

    val onOpenLinksAction: () -> Unit = {
        try {
            val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                Intent(Settings.ACTION_APP_OPEN_BY_DEFAULT_SETTINGS).apply {
                    data = android.net.Uri.parse("package:${context.packageName}")
                }
            } else {
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = android.net.Uri.parse("package:${context.packageName}")
                }
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "无法打开设置", Toast.LENGTH_SHORT).show()
        }
    }

    // 页面跳转逻辑
    if (showBlockedList) {
        BlockedListScreen(onBack = { showBlockedList = false })
    } else {
        // Layout Switching
        Box(
            modifier = Modifier
                .fillMaxSize()
                .hazeSource(state = activeHazeState)
        ) {
            if (shouldUseSettingsSplitLayout(widthDp = configuration.screenWidthDp)) {
                TabletSettingsLayout(
                    onBack = onBack,
                    onAppearanceClick = onAppearanceClick,
                    onPlaybackClick = onPlaybackClick,
                    onPermissionClick = onPermissionClick,
                    onPluginsClick = onPluginsClick,
                    onExportLogsClick = onExportLogsAction,
                    onLicenseClick = onOpenSourceLicensesClick,
                    onDisclaimerClick = onDisclaimerClick,
                    onGithubClick = onGithubClick,
                    onCheckUpdateClick = onCheckUpdateAction,
                    onVersionClick = onVersionClickAction,
                    onReplayOnboardingClick = onReplayOnboardingClick,
                    onTipsClick = onTipsClick, // [Feature]
                    onTelegramClick = onTelegramClick,
                    onTwitterClick = onTwitterClick,
                    onWebDavBackupClick = onWebDavBackupClick,
                    onDownloadPathClick = onDownloadPathAction,
                    onClearCacheClick = onClearCacheAction,
                    onPrivacyModeChange = onPrivacyModeChange,
                    onCrashTrackingChange = onCrashTrackingChange,
                    onAnalyticsChange = onAnalyticsChange,
                    onEasterEggChange = onEasterEggChange,
                    privacyModeEnabled = privacyModeEnabled,
                    customDownloadPath = downloadExportTreeUri ?: customDownloadPath,
                    cacheSize = state.cacheSize,
                    crashTrackingEnabled = crashTrackingEnabled,
                    analyticsEnabled = analyticsEnabled,
                    pluginCount = PluginManager.getEnabledCount(),
                    versionName = com.android.purebilibili.BuildConfig.VERSION_NAME,
                    versionClickCount = versionClickCount,
                    versionClickThreshold = versionClickThreshold,
                    easterEggEnabled = easterEggEnabled,
                    updateStatusText = updateStatusText,
                    isCheckingUpdate = isCheckingUpdate,
                    onDonateClick = { showDonateDialog = true },
                    onOpenLinksClick = onOpenLinksAction,
                    onBlockedListClick = onBlockedListClickAction, // Pass to tablet layout
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
                    },
                    incrementalTimelineRefreshEnabled = incrementalTimelineRefreshEnabled,
                    onIncrementalTimelineRefreshChange = { enabled ->
                        scope.launch {
                            SettingsManager.setIncrementalTimelineRefresh(context, enabled)
                        }
                    }
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
                    onDisclaimerClick = onDisclaimerClick,
                    onGithubClick = onGithubClick,
                    onCheckUpdateClick = onCheckUpdateAction,
                    onVersionClick = onVersionClickAction,
                    onTipsClick = onTipsClick, // [Feature]
                    onReplayOnboardingClick = onReplayOnboardingClick,
                    onTelegramClick = onTelegramClick,
                    onTwitterClick = onTwitterClick,
                    onWebDavBackupClick = onWebDavBackupClick,
                    onDownloadPathClick = onDownloadPathAction,
                    onClearCacheClick = onClearCacheAction,
                    onPrivacyModeChange = onPrivacyModeChange,
                    onCrashTrackingChange = onCrashTrackingChange,
                    onAnalyticsChange = onAnalyticsChange,
                    onEasterEggChange = onEasterEggChange,
                    privacyModeEnabled = privacyModeEnabled,
                    customDownloadPath = downloadExportTreeUri ?: customDownloadPath,
                    cacheSize = state.cacheSize,
                    crashTrackingEnabled = crashTrackingEnabled,
                    analyticsEnabled = analyticsEnabled,
                    pluginCount = PluginManager.getEnabledCount(),
                    versionName = com.android.purebilibili.BuildConfig.VERSION_NAME,
                    versionClickCount = versionClickCount,
                    versionClickThreshold = versionClickThreshold,
                    easterEggEnabled = easterEggEnabled,
                    updateStatusText = updateStatusText,
                    isCheckingUpdate = isCheckingUpdate,
                    cardAnimationEnabled = state.cardAnimationEnabled,
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
                    },
                    incrementalTimelineRefreshEnabled = incrementalTimelineRefreshEnabled,
                    onIncrementalTimelineRefreshChange = { enabled ->
                        scope.launch {
                            SettingsManager.setIncrementalTimelineRefresh(context, enabled)
                        }
                    },
                    onDonateClick = { showDonateDialog = true },
                    onOpenLinksClick = onOpenLinksAction,
                    onBlockedListClick = onBlockedListClickAction // Pass to mobile layout
                )
            }
            
            // Onboarding Bottom Sheet (Shared)
    
        }
    }
}

internal enum class MobileSettingsRootSection {
    FOLLOW_AUTHOR,
    GENERAL,
    PRIVACY,
    STORAGE,
    DEVELOPER,
    FEED,
    ABOUT,
    SUPPORT
}

internal fun resolveMobileSettingsRootSectionOrder(): List<MobileSettingsRootSection> = listOf(
    MobileSettingsRootSection.FOLLOW_AUTHOR,
    MobileSettingsRootSection.GENERAL,
    MobileSettingsRootSection.PRIVACY,
    MobileSettingsRootSection.STORAGE,
    MobileSettingsRootSection.DEVELOPER,
    MobileSettingsRootSection.FEED,
    MobileSettingsRootSection.ABOUT,
    MobileSettingsRootSection.SUPPORT
)

internal fun shouldMarkCacheClearAnimationComplete(clearSucceeded: Boolean): Boolean = clearSucceeded

internal fun resolveCacheClearFailureMessage(error: Throwable?): String {
    return error?.message?.takeIf { it.isNotBlank() } ?: "清理缓存失败，请稍后重试"
}

@Composable
private fun SettingsCategoryHeader(title: String) {
    Text(
        text = title,
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.86f),
        fontSize = 13.sp,
        fontWeight = FontWeight.Medium,
        modifier = Modifier.padding(start = 20.dp, top = 20.dp, bottom = 8.dp)
    )
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
    onTipsClick: () -> Unit, // [Feature]
    onPluginsClick: () -> Unit,
    onExportLogsClick: () -> Unit,
    onLicenseClick: () -> Unit,
    onDisclaimerClick: () -> Unit,
    onGithubClick: () -> Unit,
    onCheckUpdateClick: () -> Unit,
    onVersionClick: () -> Unit,
    onReplayOnboardingClick: () -> Unit,
    onTelegramClick: () -> Unit,
    onTwitterClick: () -> Unit,
    onWebDavBackupClick: () -> Unit,
    onDownloadPathClick: () -> Unit,
    onClearCacheClick: () -> Unit,
    onDonateClick: () -> Unit,
    onOpenLinksClick: () -> Unit, // [New]
    onBlockedListClick: () -> Unit, // [New]
    
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
    versionClickCount: Int,
    versionClickThreshold: Int,
    easterEggEnabled: Boolean,
    updateStatusText: String,
    isCheckingUpdate: Boolean,
    cardAnimationEnabled: Boolean,
    feedApiType: SettingsManager.FeedApiType,
    onFeedApiTypeChange: (SettingsManager.FeedApiType) -> Unit,
    incrementalTimelineRefreshEnabled: Boolean,
    onIncrementalTimelineRefreshChange: (Boolean) -> Unit
) {
    var isVisible by remember { mutableStateOf(false) }
    val windowSizeClass = LocalWindowSizeClass.current
    val deviceUiProfile = remember(windowSizeClass.widthSizeClass) {
        resolveDeviceUiProfile(
            widthSizeClass = windowSizeClass.widthSizeClass
        )
    }
    val effectiveMotionTier = remember(deviceUiProfile.motionTier, cardAnimationEnabled) {
        resolveEffectiveMotionTier(
            baseTier = deviceUiProfile.motionTier,
            animationEnabled = cardAnimationEnabled
        )
    }
    val sectionOrder = remember { resolveMobileSettingsRootSectionOrder() }
    val bottomInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    LaunchedEffect(Unit) { isVisible = true }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("设置", fontWeight = FontWeight.Bold)
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack
                    ) {
                        Icon(
                            CupertinoIcons.Outlined.ChevronBackward,
                            contentDescription = "返回"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
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
            contentPadding = PaddingValues(bottom = bottomInset + 28.dp)
        ) {
            sectionOrder.forEachIndexed { index, section ->
                item {
                    Box(modifier = Modifier.staggeredEntrance(index * 2, isVisible, motionTier = effectiveMotionTier)) {
                        SettingsCategoryHeader(
                            title = when (section) {
                                MobileSettingsRootSection.FOLLOW_AUTHOR -> "关注作者"
                                MobileSettingsRootSection.GENERAL -> "常规"
                                MobileSettingsRootSection.PRIVACY -> "隐私与安全"
                                MobileSettingsRootSection.STORAGE -> "数据与存储"
                                MobileSettingsRootSection.DEVELOPER -> "开发者选项"
                                MobileSettingsRootSection.FEED -> "推荐流"
                                MobileSettingsRootSection.ABOUT -> "关于"
                                MobileSettingsRootSection.SUPPORT -> "帮助与系统"
                            }
                        )
                    }
                }
                item {
                    Box(modifier = Modifier.staggeredEntrance(index * 2 + 1, isVisible, motionTier = effectiveMotionTier)) {
                        when (section) {
                            MobileSettingsRootSection.FOLLOW_AUTHOR -> {
                                FollowAuthorSection(
                                    onTelegramClick = onTelegramClick,
                                    onTwitterClick = onTwitterClick,
                                    onDonateClick = onDonateClick
                                )
                            }
                            MobileSettingsRootSection.GENERAL -> {
                                GeneralSection(
                                    onAppearanceClick = onAppearanceClick,
                                    onPlaybackClick = onPlaybackClick,
                                    onBottomBarClick = onNavigateToBottomBarSettings
                                )
                            }
                            MobileSettingsRootSection.PRIVACY -> {
                                PrivacySection(
                                    privacyModeEnabled = privacyModeEnabled,
                                    onPrivacyModeChange = onPrivacyModeChange,
                                    onPermissionClick = onPermissionClick,
                                    onBlockedListClick = onBlockedListClick
                                )
                            }
                            MobileSettingsRootSection.STORAGE -> {
                                DataStorageSection(
                                    customDownloadPath = customDownloadPath,
                                    cacheSize = cacheSize,
                                    onWebDavBackupClick = onWebDavBackupClick,
                                    onDownloadPathClick = onDownloadPathClick,
                                    onClearCacheClick = onClearCacheClick
                                )
                            }
                            MobileSettingsRootSection.DEVELOPER -> {
                                DeveloperSection(
                                    crashTrackingEnabled = crashTrackingEnabled,
                                    analyticsEnabled = analyticsEnabled,
                                    pluginCount = pluginCount,
                                    onCrashTrackingChange = onCrashTrackingChange,
                                    onAnalyticsChange = onAnalyticsChange,
                                    onPluginsClick = onPluginsClick,
                                    onExportLogsClick = onExportLogsClick
                                )
                            }
                            MobileSettingsRootSection.FEED -> {
                                FeedApiSection(
                                    feedApiType = feedApiType,
                                    onFeedApiTypeChange = onFeedApiTypeChange,
                                    incrementalTimelineRefreshEnabled = incrementalTimelineRefreshEnabled,
                                    onIncrementalTimelineRefreshChange = onIncrementalTimelineRefreshChange
                                )
                            }
                            MobileSettingsRootSection.ABOUT -> {
                                AboutSection(
                                    versionName = versionName,
                                    easterEggEnabled = easterEggEnabled,
                                    onDisclaimerClick = onDisclaimerClick,
                                    onLicenseClick = onLicenseClick,
                                    onGithubClick = onGithubClick,
                                    onCheckUpdateClick = onCheckUpdateClick,
                                    onVersionClick = onVersionClick,
                                    onReplayOnboardingClick = onReplayOnboardingClick,
                                    onEasterEggChange = onEasterEggChange,
                                    updateStatusText = updateStatusText,
                                    isCheckingUpdate = isCheckingUpdate,
                                    versionClickCount = versionClickCount,
                                    versionClickThreshold = versionClickThreshold
                                )
                            }
                            MobileSettingsRootSection.SUPPORT -> {
                                SupportToolsSection(
                                    onTipsClick = onTipsClick,
                                    onOpenLinksClick = onOpenLinksClick
                                )
                            }
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}

// Imports... (Ensure clickable is imported)

@Composable
fun DonateDialog(onDismiss: () -> Unit) {
    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(
            usePlatformDefaultWidth = false, // Full screen
            decorFitsSystemWindows = false
        )
    ) {
        Box(
            modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.9f)),
            contentAlignment = Alignment.Center
        ) {
            // QR Code Container
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(contentAlignment = Alignment.TopStart) {
                    Image(
                        painter = painterResource(id = com.android.purebilibili.R.drawable.author_qr),
                        contentDescription = "Donate QR Code",
                        modifier = Modifier
                            .fillMaxWidth(0.85f)
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { onDismiss() }, // [New] Click to dismiss
                        contentScale = ContentScale.Fit
                    )

                    // Close Button (Top Left of Image)
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .padding(8.dp)
                            .background(Color.Black.copy(alpha = 0.3f), androidx.compose.foundation.shape.CircleShape)
                            .size(32.dp)
                    ) {
                        Icon(
                            imageVector = CupertinoIcons.Default.Xmark, // Fixed: Filled.Xmark -> Default.Xmark or correct path
                            contentDescription = "Close",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Text(
                    "感谢您的支持！",
                    color = Color.White.copy(alpha = 0.9f),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    "点击二维码或关闭按钮退出",
                    color = Color.White.copy(alpha = 0.6f),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}
