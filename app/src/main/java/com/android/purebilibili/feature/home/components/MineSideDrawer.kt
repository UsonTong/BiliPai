package com.android.purebilibili.feature.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.android.purebilibili.core.theme.iOSBlue
import com.android.purebilibili.core.theme.iOSGreen
import com.android.purebilibili.core.theme.iOSPink
import com.android.purebilibili.core.theme.iOSYellow
import com.android.purebilibili.core.ui.components.IOSClickableItem
import com.android.purebilibili.feature.home.UserState
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.materials.ExperimentalHazeMaterialsApi
import dev.chrisbanes.haze.materials.HazeMaterials
import io.github.alexzhirkevich.cupertino.icons.CupertinoIcons
import io.github.alexzhirkevich.cupertino.icons.outlined.ArrowDownCircle
import io.github.alexzhirkevich.cupertino.icons.outlined.Bookmark
import io.github.alexzhirkevich.cupertino.icons.outlined.ChevronForward
import io.github.alexzhirkevich.cupertino.icons.outlined.Clock
import io.github.alexzhirkevich.cupertino.icons.outlined.Envelope
import io.github.alexzhirkevich.cupertino.icons.outlined.RectanglePortraitAndArrowForward
import io.github.alexzhirkevich.cupertino.icons.outlined.Drop
import com.android.purebilibili.core.ui.components.IOSSwitchItem
import kotlinx.coroutines.launch

/**
 * 首页侧边栏 - 优化版 (带毛玻璃效果)
 * 采用更紧凑的布局和更现代的视觉风格
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalHazeMaterialsApi::class)
@Composable
fun MineSideDrawer(
    drawerState: DrawerState,
    user: UserState,
    onLogout: () -> Unit,
    onHistoryClick: () -> Unit,
    onFavoriteClick: () -> Unit,
    onDownloadClick: () -> Unit,
    onWatchLaterClick: () -> Unit,
    onInboxClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onProfileClick: () -> Unit,
    hazeState: HazeState? = null, // 毛玻璃效果状态
    isBlurEnabled: Boolean = true // [新增] 模糊开关状态
) {
    val scope = rememberCoroutineScope()
    
    // 辅助函数：关闭侧边栏并执行回调
    fun closeAndRun(action: () -> Unit) {
        scope.launch {
            drawerState.close()
            action()
        }
    }
    
    // 检测深色模式
    val isDark = androidx.compose.foundation.isSystemInDarkTheme()

    // 动态调整毛玻璃样式以适配深浅色模式
    val hazeStyle = if (isDark) {
        // 深色模式：使用深色背景和模糊
        HazeStyle(
            backgroundColor = Color.Black.copy(alpha = 0.6f),
            tint = HazeTint(Color.Black.copy(alpha = 0.4f)),
            blurRadius = 30.dp,
            noiseFactor = 0f
        )
    } else {
        // 浅色模式：使用白色背景和模糊 (iOS 风格)
        HazeStyle(
            backgroundColor = Color.White.copy(alpha = 0.65f),
            tint = HazeTint(Color.Unspecified),
            blurRadius = 30.dp,
            noiseFactor = 0f
        )
    }
    
    // 动态文字颜色
    val activeContentColor = if (isDark) Color(0xFFE5E5EA) else Color(0xFF1C1C1E)
    // 动态次级文字/图标颜色
    val secondaryContentColor = if (isDark) Color(0xFF8E8E93) else Color(0xFF3C3C43).copy(alpha = 0.6f)
    // 动态分割线颜色
    val dividerColor = if (isDark) Color.White.copy(alpha = 0.15f) else Color.Black.copy(alpha = 0.1f)

    // 使用 Surface 替代 ModalDrawerSheet 以绕过最小宽度限制 (240dp)
    Surface(
        color = if (hazeState != null && isBlurEnabled) 
            Color.Transparent  // 透明背景以显示毛玻璃
        else 
            MaterialTheme.colorScheme.surface,
        contentColor = activeContentColor,
        shape = RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp), // 保持抽屉的右侧圆角
        modifier = Modifier
            .fillMaxHeight()
            .fillMaxWidth(0.5f) // 宽度设为屏幕的一半，刚好卡在两列视频中间
            .then(
                if (hazeState != null && isBlurEnabled) {
                    Modifier.hazeEffect(
                        state = hazeState,
                        style = hazeStyle
                    )
                } else Modifier
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .statusBarsPadding()
                .padding(vertical = 12.dp)
        ) {
            // 1. 用户信息区域 - 可点击进入个人主页
            // 移除 Surface 背景，只保留点击区域和内容
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { closeAndRun(onProfileClick) }
                    // 背景完全透明，依靠下方毛玻璃效果
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp), 
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 头像 (尺寸再次微调，适应更窄的栏宽)
                    AsyncImage(
                        model = user.face,
                        contentDescription = "用户头像",
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    )
                    
                    Spacer(modifier = Modifier.width(12.dp))
                    
                    // 用户名和等级
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = user.name.ifEmpty { "未登录" },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = activeContentColor,
                            maxLines = 1
                        )
                        
                        if (user.isLogin) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                // 等级徽章
                                if (user.level > 0) {
                                    Surface(
                                        color = when (user.level) {
                                            6 -> Color(0xFFFF6B9D) // LV6 粉色
                                            5 -> Color(0xFFFF8A65) // LV5 橙色
                                            4 -> Color(0xFFFFB74D) // LV4 黄色
                                            else -> MaterialTheme.colorScheme.primary
                                        },
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            text = "LV${user.level}",
                                            color = Color.White,
                                            fontSize = 9.sp, // 字体微调
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                
                                // VIP 徽章
                                if (user.isVip) {
                                    Surface(
                                        color = Color(0xFFFB7299),
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            text = "大会员",
                                            color = Color.White,
                                            fontSize = 9.sp, // 字体微调
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                    
                    // 右箭头
                    Icon(
                        imageVector = CupertinoIcons.Outlined.ChevronForward,
                        contentDescription = null,
                        tint = secondaryContentColor,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            
            // 分割线样式
            val dividerThickness = 0.5.dp
            
            // 组间分割线 (全宽带padding)
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                thickness = dividerThickness,
                color = dividerColor
            )

            // 2. 常用服务 - iOS 风格列表
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
            ) {
                IOSClickableItem(
                    icon = CupertinoIcons.Outlined.ArrowDownCircle,
                    title = "离线缓存",
                    onClick = { closeAndRun(onDownloadClick) },
                    iconTint = MaterialTheme.colorScheme.primary,
                    textColor = activeContentColor
                )
                
                // 列表内分割线 (左侧留出图标空隙)
                HorizontalDivider(modifier = Modifier.padding(start = 48.dp), thickness = dividerThickness, color = dividerColor)
                
                IOSClickableItem(
                    icon = CupertinoIcons.Outlined.Clock,
                    title = "历史记录",
                    onClick = { closeAndRun(onHistoryClick) },
                    iconTint = iOSBlue,
                    textColor = activeContentColor
                )
                
                HorizontalDivider(modifier = Modifier.padding(start = 48.dp), thickness = dividerThickness, color = dividerColor)
                
                IOSClickableItem(
                    icon = CupertinoIcons.Outlined.Bookmark,
                    title = "我的收藏",
                    onClick = { closeAndRun(onFavoriteClick) },
                    iconTint = iOSYellow,
                    textColor = activeContentColor
                )
                
                HorizontalDivider(modifier = Modifier.padding(start = 48.dp), thickness = dividerThickness, color = dividerColor)
                
                IOSClickableItem(
                    icon = CupertinoIcons.Outlined.Bookmark,
                    title = "稀后再看",
                    onClick = { closeAndRun(onWatchLaterClick) },
                    iconTint = iOSGreen,
                    textColor = activeContentColor
                )
                
                HorizontalDivider(modifier = Modifier.padding(start = 48.dp), thickness = dividerThickness, color = dividerColor)
                
                IOSClickableItem(
                    icon = CupertinoIcons.Outlined.Envelope,
                    title = "我的私信",
                    onClick = { closeAndRun(onInboxClick) },
                    iconTint = iOSPink,
                    textColor = activeContentColor
                )

                HorizontalDivider(modifier = Modifier.padding(start = 48.dp), thickness = dividerThickness, color = dividerColor)
            }
            
            // 组间分割线
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                thickness = dividerThickness,
                color = dividerColor
            )
            
            // 3. 退出登录按钮
            if (user.isLogin) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp)
                ) {
                    IOSClickableItem(
                        icon = CupertinoIcons.Outlined.RectanglePortraitAndArrowForward,
                        title = "退出登录",
                        onClick = { closeAndRun(onLogout) },
                        iconTint = Color(0xFFFF453A), // iOS 红色
                        textColor = Color(0xFFFF453A)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
