package com.android.purebilibili.feature.settings

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.android.purebilibili.core.ui.AdaptiveSplitLayout
import dev.chrisbanes.haze.HazeState
import com.android.purebilibili.core.theme.iOSBlue
import com.android.purebilibili.core.theme.iOSGreen
import com.android.purebilibili.core.theme.iOSOrange
import com.android.purebilibili.core.theme.iOSPink
import com.android.purebilibili.core.theme.iOSPurple
import com.android.purebilibili.core.theme.iOSTeal
import io.github.alexzhirkevich.cupertino.icons.CupertinoIcons
import io.github.alexzhirkevich.cupertino.icons.filled.*
import io.github.alexzhirkevich.cupertino.icons.outlined.*

enum class SettingsCategory(
    val title: String, 
    val icon: ImageVector,
    val color: Color
) {
    GENERAL("常规", CupertinoIcons.Filled.Gearshape, iOSPink),
    PRIVACY("隐私与安全", CupertinoIcons.Filled.Lock, iOSPurple),
    STORAGE("数据与存储", CupertinoIcons.Filled.Folder, iOSBlue),
    DEVELOPER("开发者选项", CupertinoIcons.Filled.Hammer, iOSTeal),
    ABOUT("关于", CupertinoIcons.Filled.InfoCircle, iOSOrange)
}

@Composable
fun TabletSettingsLayout(
    // Callbacks
    onAppearanceClick: () -> Unit,
    onPlaybackClick: () -> Unit,
    onPermissionClick: () -> Unit,
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
    
    modifier: Modifier = Modifier
) {
    var selectedCategory by remember { mutableStateOf(SettingsCategory.GENERAL) }

    AdaptiveSplitLayout(
        modifier = modifier,
        primaryRatio = 0.35f, // Left pane narrower
        primaryContent = {
            // Master List
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(16.dp)
            ) {
                Text(
                    text = "设置",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp, start = 8.dp)
                )
                
                // Author Section in Sidebar
                FollowAuthorSection(
                    onTelegramClick = onTelegramClick,
                    onTwitterClick = onTwitterClick
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                SettingsCategory.entries.forEach { category ->
                    val isSelected = category == selectedCategory
                    NavigationDrawerItem(
                        label = { Text(category.title) },
                        selected = isSelected,
                        onClick = { selectedCategory = category },
                        icon = { 
                            Icon(
                                category.icon, 
                                contentDescription = null,
                                tint = if (isSelected) MaterialTheme.colorScheme.onSecondaryContainer else category.color
                            ) 
                        },
                        modifier = Modifier.padding(vertical = 4.dp),
                        colors = NavigationDrawerItemDefaults.colors(
                            selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                            unselectedContainerColor = Color.Transparent
                        )
                    )
                }
            }
        },
        secondaryContent = {
            // Detail Content
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(24.dp),
                contentAlignment = Alignment.TopCenter
            ) {
                AnimatedContent(
                    targetState = selectedCategory,
                    transitionSpec = {
                        (slideInVertically { height -> height } + fadeIn()).togetherWith(
                            slideOutVertically { height -> -height } + fadeOut())
                    },
                    label = "SettingsDetailTransition"
                ) { category ->
                    Column(modifier = Modifier.widthIn(max = 600.dp)) {
                        Text(
                            text = category.title,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 24.dp, start = 16.dp)
                        )
                        
                        when (category) {
                            SettingsCategory.GENERAL -> GeneralSection(
                                onAppearanceClick = onAppearanceClick,
                                onPlaybackClick = onPlaybackClick
                            )
                            SettingsCategory.PRIVACY -> PrivacySection(
                                privacyModeEnabled = privacyModeEnabled,
                                onPrivacyModeChange = onPrivacyModeChange,
                                onPermissionClick = onPermissionClick
                            )
                            SettingsCategory.STORAGE -> DataStorageSection(
                                customDownloadPath = customDownloadPath,
                                cacheSize = cacheSize,
                                onDownloadPathClick = onDownloadPathClick,
                                onClearCacheClick = onClearCacheClick
                            )
                            SettingsCategory.DEVELOPER -> DeveloperSection(
                                crashTrackingEnabled = crashTrackingEnabled,
                                analyticsEnabled = analyticsEnabled,
                                pluginCount = pluginCount,
                                onCrashTrackingChange = onCrashTrackingChange,
                                onAnalyticsChange = onAnalyticsChange,
                                onPluginsClick = onPluginsClick,
                                onExportLogsClick = onExportLogsClick
                            )
                            SettingsCategory.ABOUT -> AboutSection(
                                versionName = versionName,
                                easterEggEnabled = easterEggEnabled,
                                onLicenseClick = onLicenseClick,
                                onGithubClick = onGithubClick,
                                onVersionClick = onVersionClick,
                                onReplayOnboardingClick = onReplayOnboardingClick,
                                onEasterEggChange = onEasterEggChange
                            )
                        }
                    }
                }
            }
        }
    )
}
