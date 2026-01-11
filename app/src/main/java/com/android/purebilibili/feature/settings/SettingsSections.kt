package com.android.purebilibili.feature.settings

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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.purebilibili.R
import com.android.purebilibili.core.theme.*
import com.android.purebilibili.core.ui.AppIcons
import io.github.alexzhirkevich.cupertino.CupertinoSwitch
import io.github.alexzhirkevich.cupertino.CupertinoSwitchDefaults
import io.github.alexzhirkevich.cupertino.icons.CupertinoIcons
import io.github.alexzhirkevich.cupertino.icons.filled.*
import io.github.alexzhirkevich.cupertino.icons.outlined.*

// ═══════════════════════════════════════════════════
//  UI 组件 (Stateless Components)
// ═══════════════════════════════════════════════════

@Composable
fun SettingsSectionTitle(title: String) {
    Text(
        text = title.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        letterSpacing = 0.5.sp,
        modifier = Modifier.padding(start = 32.dp, top = 24.dp, bottom = 8.dp)
    )
}

@Composable
fun SettingsGroup(content: @Composable ColumnScope.() -> Unit) {
    val cornerRadiusScale = LocalCornerRadiusScale.current
    val groupCornerRadius = iOSCornerRadius.Medium * cornerRadiusScale
    
    Surface(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(groupCornerRadius)),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 0.dp,
        tonalElevation = 1.dp
    ) {
        Column(content = content)
    }
}

@Composable
fun SettingSwitchItem(
    icon: ImageVector? = null,
    title: String,
    subtitle: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    iconTint: Color = MaterialTheme.colorScheme.primary
) {
    val cornerRadiusScale = LocalCornerRadiusScale.current
    val iconCornerRadius = iOSCornerRadius.Small * cornerRadiusScale
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(iconCornerRadius))
                    .background(iconTint.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(14.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
            if (subtitle != null) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        val primaryColor = MaterialTheme.colorScheme.primary
        CupertinoSwitch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = CupertinoSwitchDefaults.colors(
                thumbColor = Color.White,
                checkedTrackColor = primaryColor,
                uncheckedTrackColor = Color(0xFFE9E9EA)
            )
        )
    }
}

@Composable
fun SettingClickableItem(
    icon: ImageVector? = null,
    iconPainter: androidx.compose.ui.graphics.painter.Painter? = null,
    title: String,
    value: String? = null,
    onClick: (() -> Unit)? = null,
    iconTint: Color = MaterialTheme.colorScheme.primary
) {
    val cornerRadiusScale = LocalCornerRadiusScale.current
    val iconCornerRadius = iOSCornerRadius.Small * cornerRadiusScale
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = onClick != null) { onClick?.invoke() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null || iconPainter != null) {
            if (iconTint != Color.Unspecified) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(iconCornerRadius))
                        .background(iconTint.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    if (icon != null) {
                        Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(20.dp))
                    } else if (iconPainter != null) {
                        Icon(painter = iconPainter, contentDescription = null, tint = iconTint, modifier = Modifier.size(20.dp))
                    }
                }
            } else {
                Box(
                    modifier = Modifier.size(36.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (icon != null) {
                        Icon(icon, contentDescription = null, tint = Color.Unspecified, modifier = Modifier.size(36.dp))
                    } else if (iconPainter != null) {
                        Icon(painter = iconPainter, contentDescription = null, tint = Color.Unspecified, modifier = Modifier.size(36.dp))
                    }
                }
            }
            Spacer(modifier = Modifier.width(14.dp))
        }
        Text(text = title, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f), maxLines = 1)
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (value != null) {
                Text(
                    text = value, 
                    style = MaterialTheme.typography.bodyMedium, 
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
            }
            if (onClick != null) {
                Spacer(modifier = Modifier.width(6.dp))
                Icon(CupertinoIcons.Default.ChevronForward, null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.5f), modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
fun Divider() {
    Box(modifier = Modifier.fillMaxWidth().height(0.5.dp).background(MaterialTheme.colorScheme.surfaceVariant))
}

// ═══════════════════════════════════════════════════
//  业务板块 (Business Sections)
// ═══════════════════════════════════════════════════

@Composable
fun FollowAuthorSection(
    onTelegramClick: () -> Unit,
    onTwitterClick: () -> Unit
) {
    SettingsGroup {
        SettingClickableItem(
            iconPainter = painterResource(R.drawable.ic_telegram_mono),
            title = "Telegram 频道",
            value = "@BiliPai",
            onClick = onTelegramClick,
            iconTint = Color(0xFF0088CC)
        )
        Divider()
        SettingClickableItem(
            icon = AppIcons.Twitter,
            title = "Twitter / X",
            value = "@YangY_0x00",
            onClick = onTwitterClick,
            iconTint = Color(0xFF1DA1F2)
        )
    }
}

@Composable
fun GeneralSection(
    onAppearanceClick: () -> Unit,
    onPlaybackClick: () -> Unit
) {
    SettingsGroup {
        SettingClickableItem(
            icon = CupertinoIcons.Default.Paintpalette,
            title = "外观设置",
            value = "主题、图标、模糊效果",
            onClick = onAppearanceClick,
            iconTint = iOSPink
        )
        Divider()
        SettingClickableItem(
            icon = CupertinoIcons.Default.Play,
            title = "播放设置",
            value = "解码、手势、后台播放",
            onClick = onPlaybackClick,
            iconTint = iOSGreen
        )
    }
}

@Composable
fun PrivacySection(
    privacyModeEnabled: Boolean,
    onPrivacyModeChange: (Boolean) -> Unit,
    onPermissionClick: () -> Unit
) {
    SettingsGroup {
        SettingSwitchItem(
            icon = CupertinoIcons.Default.EyeSlash,
            title = "隐私无痕模式",
            subtitle = "启用后不记录播放历史和搜索历史",
            checked = privacyModeEnabled,
            onCheckedChange = onPrivacyModeChange,
            iconTint = iOSPurple
        )
        Divider()
        SettingClickableItem(
            icon = CupertinoIcons.Default.Lock,
            title = "权限管理",
            value = "查看应用权限",
            onClick = onPermissionClick,
            iconTint = iOSTeal
        )
    }
}

@Composable
fun DataStorageSection(
    customDownloadPath: String?,
    cacheSize: String,
    onDownloadPathClick: () -> Unit,
    onClearCacheClick: () -> Unit
) {
    SettingsGroup {
        SettingClickableItem(
            icon = CupertinoIcons.Default.FolderBadgePlus,
            title = "下载位置",
            value = if (customDownloadPath != null) "自定义" else "默认",
            onClick = onDownloadPathClick,
            iconTint = iOSBlue
        )
        Divider()
        SettingClickableItem(
            icon = CupertinoIcons.Default.Trash,
            title = "清除缓存",
            value = cacheSize,
            onClick = onClearCacheClick,
            iconTint = iOSPink
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
    SettingsGroup {
        SettingSwitchItem(
            icon = CupertinoIcons.Default.ExclamationmarkTriangle,
            title = "崩溃追踪",
            subtitle = "帮助开发者发现和修复问题",
            checked = crashTrackingEnabled,
            onCheckedChange = onCrashTrackingChange,
            iconTint = iOSTeal
        )
        Divider()
        SettingSwitchItem(
            icon = CupertinoIcons.Default.ChartBar,
            title = "使用情况统计",
            subtitle = "帮助改进应用体验，不收集个人信息",
            checked = analyticsEnabled,
            onCheckedChange = onAnalyticsChange,
            iconTint = iOSBlue
        )
        Divider()
        SettingClickableItem(
            icon = CupertinoIcons.Default.PuzzlepieceExtension,
            title = "插件中心",
            value = "$pluginCount 个已启用",
            onClick = onPluginsClick,
            iconTint = iOSPurple
        )
        Divider()
        SettingClickableItem(
            icon = CupertinoIcons.Default.DocTextMagnifyingglass,
            title = "导出日志",
            value = "用于反馈问题",
            onClick = onExportLogsClick,
            iconTint = iOSTeal
        )
    }
}

@Composable
fun AboutSection(
    versionName: String,
    easterEggEnabled: Boolean,
    onLicenseClick: () -> Unit,
    onGithubClick: () -> Unit,
    onVersionClick: () -> Unit,
    onReplayOnboardingClick: () -> Unit,
    onEasterEggChange: (Boolean) -> Unit
) {
    SettingsGroup {
        SettingClickableItem(
            icon = CupertinoIcons.Default.DocText,
            title = "开源许可证",
            value = "License",
            onClick = onLicenseClick,
            iconTint = iOSOrange
        )
        Divider()
        SettingClickableItem(
            icon = CupertinoIcons.Default.Link,
            title = "开源主页",
            value = "GitHub",
            onClick = onGithubClick,
            iconTint = iOSPurple
        )
        Divider()
        SettingClickableItem(
            icon = CupertinoIcons.Default.InfoCircle,
            title = "版本",
            value = "v$versionName",
            onClick = onVersionClick,
            iconTint = iOSTeal
        )
        Divider()
        SettingClickableItem(
            icon = CupertinoIcons.Default.BookCircle,
            title = "重播新手引导",
            value = "了解应用功能",
            onClick = onReplayOnboardingClick,
            iconTint = iOSPink
        )
        Divider()
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
