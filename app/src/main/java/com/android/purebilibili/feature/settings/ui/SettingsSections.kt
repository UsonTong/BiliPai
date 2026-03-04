package com.android.purebilibili.feature.settings

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.purebilibili.core.theme.*
import com.android.purebilibili.core.util.EasterEggs
import io.github.alexzhirkevich.cupertino.CupertinoSwitch
import io.github.alexzhirkevich.cupertino.CupertinoSwitchDefaults
import io.github.alexzhirkevich.cupertino.icons.CupertinoIcons
import io.github.alexzhirkevich.cupertino.icons.filled.*
import io.github.alexzhirkevich.cupertino.icons.outlined.*
import com.android.purebilibili.core.ui.common.copyOnLongPress

// ═══════════════════════════════════════════════════
//  UI 组件 (Stateless Components)
// ═══════════════════════════════════════════════════

// ═══════════════════════════════════════════════════
//  UI 组件 (Stateless Components)
// ═══════════════════════════════════════════════════

// Delegated to core/ui/components/iOSListComponents.kt
import com.android.purebilibili.core.ui.components.IOSSectionTitle as SettingsSectionTitle
import com.android.purebilibili.core.ui.components.IOSGroup as SettingsGroup
import com.android.purebilibili.core.ui.components.IOSSwitchItem as SettingSwitchItem
import com.android.purebilibili.core.ui.components.IOSClickableItem as SettingClickableItem
import com.android.purebilibili.core.ui.components.IOSDivider as SettingsDivider



// ═══════════════════════════════════════════════════
//  业务板块 (Business Sections)
// ═══════════════════════════════════════════════════

@Composable
private fun SettingsCardGroup(
    content: @Composable ColumnScope.() -> Unit
) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.45f
    val darkTintBase = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
    val containerColor = if (isDark) {
        darkTintBase.compositeOver(MaterialTheme.colorScheme.surface).copy(alpha = 0.96f)
    } else {
        MaterialTheme.colorScheme.surface
    }
    val borderColor = if (isDark) {
        Color.White.copy(alpha = 0.06f)
    } else {
        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.32f)
    }

    SettingsGroup(
        containerColor = containerColor,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(0.6.dp, borderColor)
    ) {
        content()
    }
}

@Composable
fun FollowAuthorSection(
    onTelegramClick: () -> Unit,
    onTwitterClick: () -> Unit,
    onDonateClick: () -> Unit
) {
    val telegramVisual = resolveSettingsEntryVisual(SettingsSearchTarget.TELEGRAM)
    val twitterVisual = resolveSettingsEntryVisual(SettingsSearchTarget.TWITTER)
    val donateVisual = resolveSettingsEntryVisual(SettingsSearchTarget.DONATE)

    SettingsCardGroup {
        SettingClickableItem(
            icon = telegramVisual.icon,
            iconPainter = telegramVisual.iconResId?.let { painterResource(id = it) },
            title = "Telegram 频道",
            value = "@BiliPai",
            onClick = onTelegramClick,
            iconTint = telegramVisual.iconTint,
            enableCopy = true
        )
        SettingsDivider(startIndent = 66.dp)
        SettingClickableItem(
            icon = twitterVisual.icon,
            iconPainter = twitterVisual.iconResId?.let { painterResource(id = it) },
            title = "Twitter / X",
            value = "@YangY_0x00",
            onClick = onTwitterClick,
            iconTint = twitterVisual.iconTint,
            enableCopy = true
        )
        SettingsDivider(startIndent = 66.dp)
        SettingClickableItem(
            icon = donateVisual.icon,
            iconPainter = donateVisual.iconResId?.let { painterResource(id = it) },
            title = "打赏作者",
            value = "支持开发",
            onClick = onDonateClick,
            iconTint = donateVisual.iconTint,
            enableCopy = false
        )
    }
}

@Composable
fun GeneralSection(
    onAppearanceClick: () -> Unit,
    onPlaybackClick: () -> Unit,
    onBottomBarClick: () -> Unit
) {
    val appearanceVisual = resolveSettingsEntryVisual(SettingsSearchTarget.APPEARANCE)
    val playbackVisual = resolveSettingsEntryVisual(SettingsSearchTarget.PLAYBACK)
    val bottomBarVisual = resolveSettingsEntryVisual(SettingsSearchTarget.BOTTOM_BAR)

    SettingsCardGroup {
        SettingClickableItem(
            icon = appearanceVisual.icon,
            iconPainter = appearanceVisual.iconResId?.let { painterResource(id = it) },
            title = "外观设置",
            value = "主题、图标、模糊效果",
            onClick = onAppearanceClick,
            iconTint = appearanceVisual.iconTint
        )
        SettingsDivider(startIndent = 66.dp)
        SettingClickableItem(
            icon = playbackVisual.icon,
            iconPainter = playbackVisual.iconResId?.let { painterResource(id = it) },
            title = "播放设置",
            value = "解码、手势、后台播放",
            onClick = onPlaybackClick,
            iconTint = playbackVisual.iconTint
        )
        SettingsDivider(startIndent = 66.dp)
        SettingClickableItem(
            icon = bottomBarVisual.icon,
            iconPainter = bottomBarVisual.iconResId?.let { painterResource(id = it) },
            title = "底栏设置",
            value = "自定义底栏项目",
            onClick = onBottomBarClick,
            iconTint = bottomBarVisual.iconTint
        )
    }
}

@Composable
fun SupportToolsSection(
    onTipsClick: () -> Unit,
    onOpenLinksClick: () -> Unit
) {
    val tipsVisual = resolveSettingsEntryVisual(SettingsSearchTarget.TIPS)
    val openLinksVisual = resolveSettingsEntryVisual(SettingsSearchTarget.OPEN_LINKS)

    SettingsCardGroup {
        SettingClickableItem(
            icon = tipsVisual.icon,
            iconPainter = tipsVisual.iconResId?.let { painterResource(id = it) },
            title = "小贴士 & 隐藏操作",
            value = "探索更多功能",
            onClick = onTipsClick,
            iconTint = tipsVisual.iconTint
        )
        SettingsDivider(startIndent = 66.dp)
        SettingClickableItem(
            icon = openLinksVisual.icon,
            iconPainter = openLinksVisual.iconResId?.let { painterResource(id = it) },
            title = "默认打开链接",
            value = "设置应用链接支持",
            onClick = onOpenLinksClick,
            iconTint = openLinksVisual.iconTint
        )
    }
}

@Composable
fun ReleaseChannelPinnedCard(
    onGithubClick: () -> Unit,
    onTelegramClick: () -> Unit,
    onDisclaimerClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        )
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = CupertinoIcons.Default.Link,
                    contentDescription = null,
                    tint = iOSBlue,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "官方发布渠道仅限 GitHub / Telegram",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "不存在其他官方发布渠道，请注意安装来源安全。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onGithubClick) {
                    Text("GitHub")
                }
                OutlinedButton(onClick = onTelegramClick) {
                    Text("Telegram")
                }
                TextButton(onClick = onDisclaimerClick) {
                    Text("完整声明")
                }
            }
        }
    }
}

@Composable
fun SettingsSubpageEntrySection(
    onContentAndStorageClick: () -> Unit,
    onPrivacyAndSecurityClick: () -> Unit,
    onExtensionsAndDebugClick: () -> Unit,
    onAboutAndSupportClick: () -> Unit
) {
    SettingsCardGroup {
        SettingClickableItem(
            icon = CupertinoIcons.Default.Folder,
            title = "内容与存储",
            value = "推荐流、下载与缓存",
            onClick = onContentAndStorageClick,
            iconTint = iOSBlue
        )
        SettingsDivider(startIndent = 66.dp)
        SettingClickableItem(
            icon = CupertinoIcons.Default.Lock,
            title = "隐私与安全",
            value = "无痕模式、权限与黑名单",
            onClick = onPrivacyAndSecurityClick,
            iconTint = iOSPurple
        )
        SettingsDivider(startIndent = 66.dp)
        SettingClickableItem(
            icon = CupertinoIcons.Default.PuzzlepieceExtension,
            title = "扩展与调试",
            value = "插件、日志与数据采集",
            onClick = onExtensionsAndDebugClick,
            iconTint = iOSTeal
        )
        SettingsDivider(startIndent = 66.dp)
        SettingClickableItem(
            icon = CupertinoIcons.Default.InfoCircle,
            title = "关于与支持",
            value = "版本、开源、帮助与作者",
            onClick = onAboutAndSupportClick,
            iconTint = iOSOrange
        )
    }
}

@Composable
fun FeedApiSection(
    feedApiType: com.android.purebilibili.core.store.SettingsManager.FeedApiType,
    onFeedApiTypeChange: (com.android.purebilibili.core.store.SettingsManager.FeedApiType) -> Unit,
    incrementalTimelineRefreshEnabled: Boolean,
    onIncrementalTimelineRefreshChange: (Boolean) -> Unit
) {
    SettingsCardGroup {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = CupertinoIcons.Default.RectangleStack,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = iOSOrange
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "推荐流类型",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = feedApiType.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))

            IOSSlidingSegmentedControl(
                options = resolveFeedApiSegmentOptions(),
                selectedValue = feedApiType,
                onSelectionChange = onFeedApiTypeChange
            )
        }
        SettingsDivider(startIndent = 66.dp)
        FeedSwitchItem(
            icon = CupertinoIcons.Default.ArrowTriangle2Circlepath,
            title = "动态增量刷新",
            subtitle = "下拉刷新时不重置列表，仅在顶部插入新内容",
            checked = incrementalTimelineRefreshEnabled,
            onCheckedChange = onIncrementalTimelineRefreshChange,
            iconTint = iOSGreen
        )
    }
}

@Composable
private fun FeedSwitchItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    iconTint: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(iconTint.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(20.dp))
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                CupertinoSwitch(
                    checked = checked,
                    onCheckedChange = onCheckedChange,
                    colors = CupertinoSwitchDefaults.colors(
                        thumbColor = Color.White,
                        checkedTrackColor = MaterialTheme.colorScheme.primary,
                        uncheckedTrackColor = Color(0xFFE9E9EA)
                    )
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
    }
}

@Composable
fun PrivacySection(
    privacyModeEnabled: Boolean,
    onPrivacyModeChange: (Boolean) -> Unit,
    onPermissionClick: () -> Unit,
    onBlockedListClick: () -> Unit // [New]
) {
    val permissionVisual = resolveSettingsEntryVisual(SettingsSearchTarget.PERMISSION)
    val blockedListVisual = resolveSettingsEntryVisual(SettingsSearchTarget.BLOCKED_LIST)

    SettingsCardGroup {
        SettingSwitchItem(
            icon = CupertinoIcons.Default.EyeSlash,
            title = "隐私无痕模式",
            subtitle = "启用后不记录播放历史和搜索历史",
            checked = privacyModeEnabled,
            onCheckedChange = onPrivacyModeChange,
            iconTint = iOSPurple
        )
        SettingsDivider(startIndent = 66.dp)
        SettingClickableItem(
            icon = permissionVisual.icon,
            iconPainter = permissionVisual.iconResId?.let { painterResource(id = it) },
            title = "权限管理",
            value = "查看应用权限",
            onClick = onPermissionClick,
            iconTint = permissionVisual.iconTint
        )
         SettingsDivider(startIndent = 66.dp)
        SettingClickableItem(
            icon = blockedListVisual.icon,
            iconPainter = blockedListVisual.iconResId?.let { painterResource(id = it) },
            title = "黑名单管理",
            value = "管理已屏蔽的 UP 主",
            onClick = onBlockedListClick,
            iconTint = blockedListVisual.iconTint
        )
    }
}

@Composable
fun DataStorageSection(
    customDownloadPath: String?,
    cacheSize: String,
    onWebDavBackupClick: () -> Unit,
    onDownloadPathClick: () -> Unit,
    onClearCacheClick: () -> Unit
) {
    val webDavVisual = resolveSettingsEntryVisual(SettingsSearchTarget.WEBDAV_BACKUP)
    val downloadPathVisual = resolveSettingsEntryVisual(SettingsSearchTarget.DOWNLOAD_PATH)
    val clearCacheVisual = resolveSettingsEntryVisual(SettingsSearchTarget.CLEAR_CACHE)

    SettingsCardGroup {
        // WebDAV 是“备份副本”场景，使用双文档图标比链路图标更贴合语义。
        SettingClickableItem(
            icon = webDavVisual.icon,
            iconPainter = webDavVisual.iconResId?.let { painterResource(id = it) },
            title = "WebDAV 云备份",
            value = "备份与恢复设置/插件",
            onClick = onWebDavBackupClick,
            iconTint = webDavVisual.iconTint
        )
        SettingsDivider(startIndent = 66.dp)
        SettingClickableItem(
            icon = downloadPathVisual.icon,
            iconPainter = downloadPathVisual.iconResId?.let { painterResource(id = it) },
            title = "下载位置",
            value = if (customDownloadPath != null) "自定义" else "默认",
            onClick = onDownloadPathClick,
            iconTint = downloadPathVisual.iconTint
        )
        SettingsDivider(startIndent = 66.dp)
        SettingClickableItem(
            icon = clearCacheVisual.icon,
            iconPainter = clearCacheVisual.iconResId?.let { painterResource(id = it) },
            title = "清除缓存",
            value = cacheSize,
            onClick = onClearCacheClick,
            iconTint = clearCacheVisual.iconTint
        )
    }
}

@Composable
fun DeveloperSection(
    crashTrackingEnabled: Boolean,
    analyticsEnabled: Boolean,
    pluginCount: Int,
    onCrashTrackingChange: (Boolean) -> Unit,
    onAnalyticsChange: (Boolean) -> Unit,
    onPluginsClick: () -> Unit,
    onExportLogsClick: () -> Unit
) {
    val pluginsVisual = resolveSettingsEntryVisual(SettingsSearchTarget.PLUGINS)
    val exportLogsVisual = resolveSettingsEntryVisual(SettingsSearchTarget.EXPORT_LOGS)

    SettingsCardGroup {
        SettingSwitchItem(
            icon = CupertinoIcons.Default.ExclamationmarkTriangle,
            title = "崩溃追踪",
            subtitle = "帮助开发者发现和修复问题",
            checked = crashTrackingEnabled,
            onCheckedChange = onCrashTrackingChange,
            iconTint = iOSTeal
        )
        SettingsDivider(startIndent = 66.dp)
        SettingSwitchItem(
            icon = CupertinoIcons.Default.ChartBar,
            title = "使用情况统计",
            subtitle = "帮助改进应用体验，不收集个人信息",
            checked = analyticsEnabled,
            onCheckedChange = onAnalyticsChange,
            iconTint = iOSBlue
        )
        SettingsDivider(startIndent = 66.dp)
        SettingClickableItem(
            icon = pluginsVisual.icon,
            iconPainter = pluginsVisual.iconResId?.let { painterResource(id = it) },
            title = "插件中心",
            value = "$pluginCount 个已启用",
            onClick = onPluginsClick,
            iconTint = pluginsVisual.iconTint
        )
        SettingsDivider(startIndent = 66.dp)
        SettingClickableItem(
            icon = exportLogsVisual.icon,
            iconPainter = exportLogsVisual.iconResId?.let { painterResource(id = it) },
            title = "导出日志",
            value = "用于反馈问题",
            onClick = onExportLogsClick,
            iconTint = exportLogsVisual.iconTint
        )
    }
}

@Composable
fun AboutSection(
    versionName: String,
    easterEggEnabled: Boolean,
    onDisclaimerClick: () -> Unit,
    onLicenseClick: () -> Unit,
    onGithubClick: () -> Unit,
    onCheckUpdateClick: () -> Unit,
    onViewReleaseNotesClick: () -> Unit,
    autoCheckUpdateEnabled: Boolean,
    onAutoCheckUpdateChange: (Boolean) -> Unit,
    onVersionClick: () -> Unit,
    onReplayOnboardingClick: () -> Unit,
    onEasterEggChange: (Boolean) -> Unit,
    updateStatusText: String = "点击检查",
    isCheckingUpdate: Boolean = false,
    versionClickCount: Int = 0,
    versionClickThreshold: Int = EasterEggs.VERSION_EASTER_EGG_THRESHOLD
) {
    val disclaimerVisual = resolveSettingsEntryVisual(SettingsSearchTarget.DISCLAIMER)
    val licensesVisual = resolveSettingsEntryVisual(SettingsSearchTarget.OPEN_SOURCE_LICENSES)
    val openSourceHomeVisual = resolveSettingsEntryVisual(SettingsSearchTarget.OPEN_SOURCE_HOME)
    val checkUpdateVisual = resolveSettingsEntryVisual(SettingsSearchTarget.CHECK_UPDATE)
    val releaseNotesVisual = resolveSettingsEntryVisual(SettingsSearchTarget.VIEW_RELEASE_NOTES)
    val replayOnboardingVisual = resolveSettingsEntryVisual(SettingsSearchTarget.REPLAY_ONBOARDING)

    val safeThreshold = versionClickThreshold.coerceAtLeast(1)
    val normalizedClickCount = versionClickCount.coerceAtLeast(0)
    val versionProgress = normalizedClickCount.coerceAtMost(safeThreshold).toFloat() / safeThreshold
    val versionIconTint = animateColorAsState(
        targetValue = when {
            normalizedClickCount >= safeThreshold -> iOSGreen
            versionProgress >= 0.85f -> iOSOrange
            versionProgress >= 0.5f -> iOSYellow
            normalizedClickCount > 0 -> iOSBlue
            else -> iOSTeal
        },
        label = "versionIconTint"
    ).value
    val versionHint = when {
        normalizedClickCount <= 0 -> null
        normalizedClickCount >= safeThreshold -> "彩蛋已解锁"
        else -> "还差 ${safeThreshold - normalizedClickCount} 次"
    }
    val versionValue = buildString {
        append("v$versionName")
        versionHint?.let {
            append(" · ")
            append(it)
        }
    }

    SettingsCardGroup {
        SettingClickableItem(
            icon = disclaimerVisual.icon,
            iconPainter = disclaimerVisual.iconResId?.let { painterResource(id = it) },
            title = "发布渠道声明",
            value = "仅 GitHub / Telegram",
            onClick = onDisclaimerClick,
            iconTint = disclaimerVisual.iconTint
        )
        SettingsDivider(startIndent = 66.dp)
        SettingClickableItem(
            icon = licensesVisual.icon,
            iconPainter = licensesVisual.iconResId?.let { painterResource(id = it) },
            title = "开源许可证",
            value = "License",
            onClick = onLicenseClick,
            iconTint = licensesVisual.iconTint
        )
        SettingsDivider(startIndent = 66.dp)
        SettingClickableItem(
            icon = openSourceHomeVisual.icon,
            iconPainter = openSourceHomeVisual.iconResId?.let { painterResource(id = it) },
            title = "开源主页",
            value = "GitHub",
            onClick = onGithubClick,
            iconTint = openSourceHomeVisual.iconTint,
            enableCopy = true
        )
        SettingsDivider(startIndent = 66.dp)
        SettingClickableItem(
            icon = checkUpdateVisual.icon,
            iconPainter = checkUpdateVisual.iconResId?.let { painterResource(id = it) },
            title = "检查更新",
            value = if (isCheckingUpdate) "检查中..." else updateStatusText,
            onClick = onCheckUpdateClick,
            iconTint = checkUpdateVisual.iconTint
        )
        SettingsDivider(startIndent = 66.dp)
        SettingClickableItem(
            icon = releaseNotesVisual.icon,
            iconPainter = releaseNotesVisual.iconResId?.let { painterResource(id = it) },
            title = "查看更新日志",
            value = "最新版本说明",
            onClick = onViewReleaseNotesClick,
            iconTint = releaseNotesVisual.iconTint
        )
        SettingsDivider(startIndent = 66.dp)
        SettingSwitchItem(
            icon = CupertinoIcons.Default.BellBadge,
            title = "自动检查更新",
            subtitle = resolveAutoCheckUpdateSubtitle(autoCheckEnabled = autoCheckUpdateEnabled),
            checked = autoCheckUpdateEnabled,
            onCheckedChange = onAutoCheckUpdateChange,
            iconTint = iOSBlue
        )
        SettingsDivider(startIndent = 66.dp)
        SettingClickableItem(
            icon = CupertinoIcons.Default.Tag,
            title = "版本",
            value = versionValue,
            onClick = onVersionClick,
            iconTint = versionIconTint,
            enableCopy = true
        )
        SettingsDivider(startIndent = 66.dp)
        SettingClickableItem(
            icon = replayOnboardingVisual.icon,
            iconPainter = replayOnboardingVisual.iconResId?.let { painterResource(id = it) },
            title = "重播新手引导",
            value = "了解应用功能",
            onClick = onReplayOnboardingClick,
            iconTint = replayOnboardingVisual.iconTint
        )
        SettingsDivider(startIndent = 66.dp)
        SettingSwitchItem(
            icon = CupertinoIcons.Default.Gift,
            title = "趣味彩蛋",
            subtitle = "刷新、点赞、投币、搜索时显示趣味提示",
            checked = easterEggEnabled,
            onCheckedChange = onEasterEggChange,
            iconTint = iOSYellow
        )
    }
}
