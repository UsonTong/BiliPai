// 文件路径: feature/dynamic/components/DynamicSidebar.kt
package com.android.purebilibili.feature.dynamic.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
// 🍎 Cupertino Icons - iOS SF Symbols 风格图标
import io.github.alexzhirkevich.cupertino.icons.CupertinoIcons
import io.github.alexzhirkevich.cupertino.icons.outlined.*
import io.github.alexzhirkevich.cupertino.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
// 🔥 已改用 MaterialTheme.colorScheme.primary
import com.android.purebilibili.feature.dynamic.SidebarUser

/**
 * 🔥 动态侧边栏 - 显示关注的UP主（支持展开/收起、在线状态）
 */
@Composable
fun DynamicSidebar(
    users: List<SidebarUser>,
    selectedUserId: Long?,
    isExpanded: Boolean,
    onUserClick: (Long?) -> Unit,
    onToggleExpand: () -> Unit,
    modifier: Modifier = Modifier
) {
    val expandedWidth = 72.dp
    val collapsedWidth = 56.dp
    val animatedWidth by animateFloatAsState(
        targetValue = if (isExpanded) expandedWidth.value else collapsedWidth.value,
        label = "sidebarWidth"
    )
    
    Surface(
        modifier = modifier
            .width(animatedWidth.dp)
            .fillMaxHeight(),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 2.dp
    ) {
        LazyColumn(
            horizontalAlignment = Alignment.CenterHorizontally,
            contentPadding = PaddingValues(vertical = 8.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            // 🔥 展开/收起按钮
            item {
                IconButton(
                    onClick = onToggleExpand,
                    modifier = Modifier.size(40.dp)
                ) {
                    // 🔥 [新增] 旋转动画
                    val rotation by animateFloatAsState(
                        targetValue = if (isExpanded) 0f else 180f,
                        animationSpec = androidx.compose.animation.core.spring(
                            dampingRatio = 0.6f,
                            stiffness = 300f
                        ),
                        label = "chevronRotation"
                    )
                    Icon(
                        CupertinoIcons.Default.ChevronBackward,
                        contentDescription = if (isExpanded) "收起" else "展开",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.graphicsLayer { rotationY = rotation }
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
            
            // 🔥 [简化] 移除「全部」按钮，直接显示 UP 主列表
            // 关注的UP主列表 - 带瀑布入场动画
            itemsIndexed(users, key = { _, u -> "sidebar_${u.uid}" }) { index, user ->
                CascadeSidebarItem(
                    index = index,  // 从 0 开始
                    content = {
                        SidebarUserItem(
                            user = user,
                            isSelected = selectedUserId == user.uid,
                            showLabel = isExpanded,
                            onClick = { onUserClick(user.uid) }
                        )
                    }
                )
            }
        }
    }
}

/**
 * 🔥 [新增] 瀑布入场动画包装器
 * 每个项目有递增的延迟，形成瀑布展开效果
 */
@Composable
private fun CascadeSidebarItem(
    index: Int,
    content: @Composable () -> Unit
) {
    var visible by remember { mutableStateOf(false) }
    val delay = 30 * index  // 每个项目延迟 30ms
    
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(delay.toLong())
        visible = true
    }
    
    val offsetY by animateFloatAsState(
        targetValue = if (visible) 0f else 20f,
        animationSpec = androidx.compose.animation.core.spring(
            dampingRatio = 0.7f,
            stiffness = 400f
        ),
        label = "cascadeOffsetY"
    )
    
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = androidx.compose.animation.core.tween(200),
        label = "cascadeAlpha"
    )
    
    Box(
        modifier = Modifier
            .graphicsLayer {
                translationY = offsetY
                this.alpha = alpha
            }
    ) {
        content()
    }
}

/**
 * 🔥 侧边栏项目（文字图标）
 */
@Composable
fun SidebarItem(
    icon: String,
    label: String?,
    isSelected: Boolean,
    isLive: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(
                    if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = icon,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        if (label != null) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                fontSize = 10.sp,
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * 🔥 侧边栏用户项（头像 + 在线状态）
 */
@Composable
fun SidebarUserItem(
    user: SidebarUser,
    isSelected: Boolean,
    showLabel: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp)
    ) {
        Box {
            // 头像
            val faceUrl = remember(user.face) {
                val raw = user.face.trim()
                when {
                    raw.isEmpty() -> ""
                    raw.startsWith("https://") -> raw
                    raw.startsWith("http://") -> raw.replace("http://", "https://")
                    raw.startsWith("//") -> "https:$raw"
                    else -> "https://$raw"
                }
            }
            
            AsyncImage(
                model = coil.request.ImageRequest.Builder(LocalContext.current)
                    .data(faceUrl.ifEmpty { null })
                    .crossfade(true)
                    .build(),
                contentDescription = user.name,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(
                        if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                        else MaterialTheme.colorScheme.surfaceVariant,
                        CircleShape
                    ),
                contentScale = ContentScale.Crop
            )
            
            // 🔥 在线状态指示器（红点）
            if (user.isLive) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .align(Alignment.TopEnd)
                        .background(Color.Red, CircleShape)
                        .padding(2.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.White, CircleShape)
                            .padding(2.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Red, CircleShape)
                        )
                    }
                }
            }
        }
        
        if (showLabel) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = user.name,
                fontSize = 10.sp,
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }
    }
}
