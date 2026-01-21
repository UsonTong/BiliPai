// 文件路径: feature/home/components/TopBar.kt
package com.android.purebilibili.feature.home.components

import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
//  Cupertino Icons - iOS SF Symbols 风格图标
import io.github.alexzhirkevich.cupertino.icons.CupertinoIcons
import io.github.alexzhirkevich.cupertino.icons.outlined.*
import io.github.alexzhirkevich.cupertino.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.android.purebilibili.core.util.FormatUtils
import com.android.purebilibili.feature.home.UserState

/**
 * Q弹点击效果
 */
fun Modifier.premiumClickable(onClick: () -> Unit): Modifier = composed {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1f,
        label = "scale"
    )
    this
        .scale(scale)
        .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
}

/**
 *  iOS 风格悬浮顶栏
 * - 不贴边，有水平边距
 * - 圆角 + 毛玻璃效果
 */
@Composable
fun FluidHomeTopBar(
    user: UserState,
    onAvatarClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onSearchClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
    ) {
        //  悬浮式导航栏容器 - 增强视觉层次
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,  //  使用主题色，适配深色模式
            shadowElevation = 6.dp,  // 添加阴影增加层次感
            tonalElevation = 0.dp,
            border = androidx.compose.foundation.BorderStroke(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp) // 稍微减小高度
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                //  左侧：头像
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .premiumClickable { onAvatarClick() }
                        .border(1.dp, MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                ) {
                    if (user.isLogin && user.face.isNotEmpty()) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(FormatUtils.fixImageUrl(user.face))
                                .crossfade(true).build(),
                            contentDescription = "Avatar",
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Box(
                            Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("未", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                //  中间：搜索框
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(36.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .clickable { onSearchClick() }
                        .padding(horizontal = 12.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            CupertinoIcons.Default.MagnifyingGlass,
                            null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.5f),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "搜索视频、UP主...",
                            style = MaterialTheme.typography.bodyMedium,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            maxLines = 1
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))
                
                //  右侧：设置按钮
                IconButton(
                    onClick = onSettingsClick,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        CupertinoIcons.Default.Gear,
                        contentDescription = "Settings",
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    }
}

/**
 *  [HIG] iOS 风格分类标签栏
 * - 限制可见标签为 4 个主要分类 (HIG 建议 3-5 个)
 * - 其余分类收入"更多"下拉菜单
 * - 圆角胶囊选中指示器
 * - 最小触摸目标 44pt
 */
/**
 *  [HIG] iOS 风格可滑动分类标签栏 (Liquid Glass Style)
 * - 移除"更多"菜单，所有分类水平平铺
 * - 支持水平惯性滚动
 * - 液态玻璃选中指示器 (变长胶囊)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryTabRow(
    categories: List<String> = listOf("推荐", "关注", "热门", "直播", "追番", "影视", "游戏", "知识", "科技"),
    selectedIndex: Int = 0,
    onCategorySelected: (Int) -> Unit = {},
    onPartitionClick: () -> Unit = {}
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val unselectedColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
    
    // [Refactor] 回退到 Row 布局，增加间距以避免"露半字"的尴尬截断
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp) // Maintain height
            .padding(horizontal = 4.dp), // Minimal horizontal padding
        verticalAlignment = Alignment.CenterVertically
    ) {
        val listState = rememberLazyListState()
        
        // 自动滚动到选中项
        LaunchedEffect(selectedIndex) {
            listState.animateScrollToItem(selectedIndex)
        }

        // [Refactor] 使用 BoxWithConstraints 动态计算宽度，实现"固定显示5个"
        BoxWithConstraints(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
        ) {
            // 计算每个 Tab 的宽度：可用宽度 / 5
            val tabWidth = maxWidth / 5

            LazyRow(
                modifier = Modifier.fillMaxSize(),
                state = listState,
                verticalAlignment = Alignment.CenterVertically
            ) {
                itemsIndexed(categories) { index, category ->
                    Box(
                        modifier = Modifier.width(tabWidth),
                        contentAlignment = Alignment.Center
                    ) {
                        CategoryTabItem(
                            category = category,
                            isSelected = index == selectedIndex,
                            primaryColor = primaryColor,
                            unselectedColor = unselectedColor,
                            onClick = { onCategorySelected(index) }
                        )
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.width(4.dp))
        
        //  分区按钮
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(22.dp))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { onPartitionClick() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                CupertinoIcons.Default.ListBullet,
                contentDescription = "浏览全部分区",
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                modifier = Modifier.size(20.dp)
            )
        }
        
        Spacer(modifier = Modifier.width(8.dp))
    }
}


@Composable
fun CategoryTabItem(
    category: String,
    isSelected: Boolean,
    primaryColor: Color,
    unselectedColor: Color,
    onClick: () -> Unit
) {
     // 文字颜色动画
     val textColor by animateColorAsState(
         targetValue = if (isSelected) primaryColor else unselectedColor,
         animationSpec = spring(dampingRatio = 0.7f, stiffness = 400f),
         label = "textColor"
     )
     
     // [新增] 背景动画 (替代 Indicator)
     // 选中时显示半透明背景，未选中透明
     val backgroundColor by animateColorAsState(
         targetValue = if (isSelected) primaryColor.copy(alpha = 0.12f) else Color.Transparent,
         animationSpec = spring(dampingRatio = 0.7f, stiffness = 400f),
         label = "bgColor"
     )

     val fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal

     Box(
         modifier = Modifier
             .clip(RoundedCornerShape(16.dp)) // 圆角适配 Dock
             .background(backgroundColor) // 应用背景
             .clickable(
                 interactionSource = remember { MutableInteractionSource() },
                 indication = null
             ) { onClick() }
             .padding(horizontal = 10.dp, vertical = 6.dp), // 调整内边距适应 1/5 宽度
         contentAlignment = Alignment.Center
     ) {
         Text(
             text = category,
             color = textColor,
             fontSize = if (isSelected) 16.sp else 15.sp,
             fontWeight = fontWeight
         )
     }
}
