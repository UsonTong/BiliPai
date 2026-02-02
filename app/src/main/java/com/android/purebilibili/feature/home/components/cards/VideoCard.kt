package com.android.purebilibili.feature.home.components.cards

import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
//  Cupertino Icons
import io.github.alexzhirkevich.cupertino.icons.CupertinoIcons
import io.github.alexzhirkevich.cupertino.icons.outlined.*
import io.github.alexzhirkevich.cupertino.icons.filled.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.android.purebilibili.core.util.FormatUtils
import com.android.purebilibili.core.util.rememberHapticFeedback
import com.android.purebilibili.core.util.animateEnter
import com.android.purebilibili.core.util.CardPositionManager
import com.android.purebilibili.data.model.response.VideoItem
import com.android.purebilibili.core.theme.iOSSystemGray
import com.android.purebilibili.core.theme.LocalCornerRadiusScale
import com.android.purebilibili.core.theme.iOSCornerRadius
import com.android.purebilibili.core.util.HapticType
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.core.spring
import com.android.purebilibili.core.ui.LocalSharedTransitionScope
import com.android.purebilibili.core.ui.LocalAnimatedVisibilityScope
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
//  [预览播放] 相关引用已移除

// 显式导入 collectAsState 以避免 ambiguity 或 missing reference
import androidx.compose.runtime.collectAsState

/**
 *  官方 B 站风格视频卡片
 * 采用与 Bilibili 官方 App 一致的设计：
 * - 封面 16:10 比例
 * - 左下角：播放量 + 弹幕数
 * - 右下角：时长
 * - 标题：2行
 * - 底部：「已关注」标签 + UP主名称
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun ElegantVideoCard(
    video: VideoItem,
    index: Int,
    refreshKey: Long = 0L,
    isFollowing: Boolean = false,  //  是否已关注该 UP 主
    animationEnabled: Boolean = true,   //  卡片进场动画开关
    transitionEnabled: Boolean = false, //  卡片过渡动画开关
    showPublishTime: Boolean = false,   //  是否显示发布时间（搜索结果用）
    isDataSaverActive: Boolean = false, // 🚀 [性能优化] 从父级传入，避免每个卡片重复计算
    onDismiss: (() -> Unit)? = null,    //  [新增] 删除/过滤回调（长按触发）
    onWatchLater: (() -> Unit)? = null,  //  [新增] 稍后再看回调
    onUnfavorite: (() -> Unit)? = null,  //  [新增] 取消收藏回调
    onLongClick: ((VideoItem) -> Unit)? = null, // [Feature] Long Press Preview
    onClick: (String, Long) -> Unit
) {
    val haptic = rememberHapticFeedback()
    val scope = rememberCoroutineScope()
    
    //  [HIG] 动态圆角 - 12dp 标准
    val cornerRadiusScale = LocalCornerRadiusScale.current
    val cardCornerRadius = 12.dp * cornerRadiusScale  // HIG 标准圆角
    val smallCornerRadius = iOSCornerRadius.Tiny * cornerRadiusScale  // 4.dp * scale
    
    //  [新增] 长按删除菜单状态
    var showDismissMenu by remember { mutableStateOf(false) }
    //  [新增] 确认对话框状态
    var showUnfavoriteDialog by remember { mutableStateOf(false) }
    
    val coverUrl = remember(video.bvid) {
        FormatUtils.fixImageUrl(if (video.pic.startsWith("//")) "https:${video.pic}" else video.pic)
    }
    
    //  判断是否为竖屏视频（通过封面图 URL 中的尺寸信息或默认不显示）
    // B站封面 URL 通常包含尺寸信息，如 width=X&height=Y
    // 简单方案：暂不显示竖屏标签（因推荐API不提供视频尺寸信息）

    //  获取屏幕尺寸用于计算归一化坐标
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val screenWidthPx = with(density) { configuration.screenWidthDp.dp.toPx() }
    val screenHeightPx = with(density) { configuration.screenHeightDp.dp.toPx() }
    val densityValue = density.density  //  [新增] 屏幕密度值
    
    //  记录卡片位置
    var cardBounds by remember { mutableStateOf<androidx.compose.ui.geometry.Rect?>(null) }
    
    //  [交互优化] 按压缩放动画状态
    var isPressed by remember { mutableStateOf(false) }
    val scale by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f, // [UX优化] 更明显的缩放反馈 (0.96 -> 0.95)
        animationSpec = androidx.compose.animation.core.spring(
            dampingRatio = 0.6f,
            stiffness = 500f // [UX优化] 更快的响应速度 (400 -> 500)
        ),
        label = "cardScale"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)  //  应用全局缩放
            //  [修复] 进场动画 - 使用 Unit 作为 key，只在首次挂载时播放
            // 原问题：使用 video.bvid 作为 key，分类切换时所有卡片重新触发动画（缩放收缩效果）
            .animateEnter(
                index = index, 
                key = Unit, 
                animationEnabled = animationEnabled && !CardPositionManager.isReturningFromDetail && !CardPositionManager.isSwitchingCategory
            )
            //  [新增] 记录卡片位置
            .onGloballyPositioned { coordinates ->
                cardBounds = coordinates.boundsInRoot()
            }
            //  [修改] 父级容器仅处理点击跳转 (或者点击由子 View 分别处理)
            //  为了避免冲突，我们将手势下放到子 View
            .padding(bottom = 12.dp)
    ) {
        //  尝试获取共享元素作用域
        val sharedTransitionScope = LocalSharedTransitionScope.current
        val animatedVisibilityScope = LocalAnimatedVisibilityScope.current
        
        //  封面容器 - 官方 B 站风格，支持共享元素过渡（受开关控制）
        val coverModifier = if (transitionEnabled && sharedTransitionScope != null && animatedVisibilityScope != null) {
            with(sharedTransitionScope) {
                Modifier
                    .sharedBounds(
                        sharedContentState = rememberSharedContentState(key = "video_cover_${video.bvid}"),
                        animatedVisibilityScope = animatedVisibilityScope,
                        //  添加回弹效果的 spring 动画
                        boundsTransform = { _, _ ->
                            spring(
                                dampingRatio = 0.8f,   // [Hero] 高阻尼，类似高级跑车悬挂，稳
                                stiffness = 200f       // [Hero] 低刚度，缓慢展开，营造电影感
                            )
                        },
                        clipInOverlayDuringTransition = OverlayClip(
                            RoundedCornerShape(cardCornerRadius)  //  过渡时保持动态圆角
                        )
                    )
            }
        } else {
            Modifier
        }
        
        //  [性能优化] 封面圆角形状缓存（避免重组时重复创建）
        val coverShape = remember(cardCornerRadius) { RoundedCornerShape(cardCornerRadius) }

        Box(
            modifier = coverModifier
                .fillMaxWidth()
                .aspectRatio(16f / 10f)
                // [性能优化] 使用 shadow(clip = true) 合并裁剪和阴影层，避免创建额外的 GraphicsLayer
                .shadow(
                    elevation = 1.dp,
                    shape = coverShape,
                    ambientColor = androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.08f),
                    spotColor = androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.10f),
                    clip = true
                )
                .background(MaterialTheme.colorScheme.surfaceVariant)
                //  [交互优化] 封面区域：点击跳转 (带按压反馈)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onPress = {
                            isPressed = true
                            tryAwaitRelease()
                            isPressed = false
                        },
                        onLongPress = {
                            if (onLongClick != null) {
                                haptic(HapticType.HEAVY)
                                onLongClick(video)
                            }
                        },
                        onTap = {
                            cardBounds?.let { bounds ->
                                CardPositionManager.recordCardPosition(bounds, screenWidthPx, screenHeightPx, density = densityValue)
                            }
                            onClick(video.bvid, 0)
                        }
                    )
                }
        ) {
            // 🚀 [性能优化] 使用从父级传入的 isDataSaverActive，避免每个卡片重复计算
            val imageWidth = if (isDataSaverActive) 240 else 360
            val imageHeight = if (isDataSaverActive) 150 else 225
            
            // 封面图 -  [性能优化] 降低图片尺寸
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(coverUrl)
                    .size(imageWidth, imageHeight)  // 省流量时使用更小尺寸
                    .crossfade(100)  //  缩短淡入时间
                    .memoryCacheKey("cover_${video.bvid}_${if (isDataSaverActive) "s" else "n"}")
                    .diskCacheKey("cover_${video.bvid}_${if (isDataSaverActive) "s" else "n"}")
                    .build(),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            
            
            //  底部渐变遮罩

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.6f)
                            )
                        )
                    )
            )
            
            //  时长标签 - 右下角 (官方风格)
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(6.dp),
                shape = RoundedCornerShape(smallCornerRadius),
                color = Color.Black.copy(alpha = 0.7f)
            ) {
                Text(
                    text = FormatUtils.formatDuration(video.duration),
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                )
            }
            
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // 标题行：标题 + 更多按钮
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            //  [HIG] 标题 - 15sp Medium, 行高 20sp
            //  共享元素过渡 - 标题
            var titleModifier = Modifier
                .weight(1f)
                .semantics { contentDescription = "视频标题: ${video.title}" }
            
            if (transitionEnabled && sharedTransitionScope != null && animatedVisibilityScope != null) {
                with(sharedTransitionScope) {
                    titleModifier = titleModifier.sharedBounds(
                        sharedContentState = rememberSharedContentState(key = "video_title_${video.bvid}"),
                        animatedVisibilityScope = animatedVisibilityScope,
                        boundsTransform = { _, _ ->
                            spring(dampingRatio = 0.8f, stiffness = 200f)
                        }
                    )
                }
            }

            Text(
                text = video.title,
                maxLines = 2,
                minLines = 2,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Medium,
                    fontSize = 15.sp,  // HIG body 标准
                    lineHeight = 20.sp,  // HIG 行高
                    color = MaterialTheme.colorScheme.onSurface
                ),
                modifier = titleModifier
                    //  [交互优化] 标题区域：长按弹出菜单，点击跳转 (带按压反馈)
                    .pointerInput(onDismiss, onWatchLater, onUnfavorite) {
                        val hasLongPressMenu = onDismiss != null || onWatchLater != null || onUnfavorite != null
                        detectTapGestures(
                            onPress = {
                                isPressed = true
                                tryAwaitRelease()
                                isPressed = false
                            },
                            onLongPress = {
                                if (onLongClick != null) {
                                  haptic(HapticType.HEAVY)
                                  onLongClick(video)
                                } else if (hasLongPressMenu) {
                                    haptic(HapticType.HEAVY)
                                    showDismissMenu = true
                                }
                            },
                            onTap = {
                                cardBounds?.let { bounds ->
                                    CardPositionManager.recordCardPosition(bounds, screenWidthPx, screenHeightPx, density = densityValue)
                                }
                                onClick(video.bvid, 0)
                            }
                        )
                    }
            )

            //  [新增] 更多按钮 / 取消收藏按钮
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 如果提供了取消收藏回调，直接显示取消按钮 (优先于更多菜单显示，或者并存)
                if (onUnfavorite != null) {
                    Box(
                        modifier = Modifier
                            .padding(end = 4.dp, top = 2.dp)
                            .size(24.dp)
                            .clickable { 
                                haptic(HapticType.MEDIUM)
                                // onUnfavorite.invoke() -> 改为弹窗确认
                                showUnfavoriteDialog = true
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = CupertinoIcons.Filled.HeartSlash,
                            contentDescription = "取消收藏",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }

                val hasMenu = onDismiss != null || onWatchLater != null
                if (hasMenu) {
                    Box(
                        modifier = Modifier
                            .padding(start = 4.dp, top = 2.dp) // 微调位置对齐第一行文字
                            .size(20.dp)
                            .clickable { 
                                haptic(HapticType.LIGHT)
                                showDismissMenu = true 
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "⋮",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(6.dp))
        
        //  底部信息行 - 官方 B 站风格
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            //  已关注标签（红色文字，官方风格）
            if (isFollowing) {
                Text(
                    text = "已关注",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary  // 主题色
                )
            }
            
            //  UP主头像（小圆形，官方风格）
            if (video.owner.face.isNotEmpty()) {
                var avatarModifier = Modifier
                    .size(14.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                
                if (transitionEnabled && sharedTransitionScope != null && animatedVisibilityScope != null) {
                    with(sharedTransitionScope) {
                        avatarModifier = avatarModifier.sharedBounds(
                            sharedContentState = rememberSharedContentState(key = "video_avatar_${video.bvid}"),
                            animatedVisibilityScope = animatedVisibilityScope,
                            boundsTransform = { _, _ ->
                                spring(dampingRatio = 0.8f, stiffness = 200f)
                            },
                            clipInOverlayDuringTransition = OverlayClip(CircleShape)
                        )
                    }
                }

                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(FormatUtils.fixImageUrl(video.owner.face))
                        .crossfade(100)
                        .size(32, 32)
                        //  [修复] 使用 face URL hashCode 作为缓存 key
                        // 原因: 历史记录的 owner.mid 可能为空，导致所有头像共享同一缓存
                        .memoryCacheKey("avatar_${video.owner.face.hashCode()}")
                        .build(),
                    contentDescription = null,
                    modifier = avatarModifier,
                    contentScale = ContentScale.Crop
                )
            }
            
            //  [HIG] UP主名称 - 13sp footnote 标准
            //  共享元素过渡 - UP主名称
            var upNameModifier = Modifier.weight(1f, fill = false)
            
            if (transitionEnabled && sharedTransitionScope != null && animatedVisibilityScope != null) {
                with(sharedTransitionScope) {
                    upNameModifier = upNameModifier.sharedBounds(
                        sharedContentState = rememberSharedContentState(key = "video_up_${video.bvid}"),
                        animatedVisibilityScope = animatedVisibilityScope,
                        boundsTransform = { _, _ ->
                            spring(dampingRatio = 0.8f, stiffness = 200f)
                        }
                    )
                }
            }

            Text(
                text = video.owner.name,
                fontSize = 13.sp,  // HIG footnote 标准
                fontWeight = FontWeight.Normal,
                color = iOSSystemGray,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = upNameModifier
            )
            
            //  发布时间（搜索结果显示）
            if (showPublishTime && video.pubdate > 0) {
                Text(
                    text = " · ${FormatUtils.formatPublishTime(video.pubdate)}",
                    fontSize = 11.sp,
                    color = iOSSystemGray.copy(alpha = 0.7f)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(6.dp))
        
        //  [重设计] 播放数据行 - 独立展示，精致风格
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 播放量
            // 🔗 [共享元素] 播放量
            var viewsModifier = Modifier.wrapContentSize()
            if (transitionEnabled && sharedTransitionScope != null && animatedVisibilityScope != null) {
                with(sharedTransitionScope) {
                    viewsModifier = viewsModifier.sharedBounds(
                        sharedContentState = rememberSharedContentState(key = "video_views_${video.bvid}"),
                        animatedVisibilityScope = animatedVisibilityScope,
                        boundsTransform = { _, _ ->
                            spring(dampingRatio = 0.8f, stiffness = 200f)
                        }
                    )
                }
            }

            Box(modifier = viewsModifier) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Icon(
                        imageVector = CupertinoIcons.Outlined.PlayCircle,
                        contentDescription = null,
                        modifier = Modifier.size(12.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = if (video.stat.view > 0) FormatUtils.formatStat(video.stat.view.toLong())
                               else FormatUtils.formatProgress(video.progress, video.duration),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            
            // 弹幕数 (仅当有播放量时显示，保持逻辑一致)
            if (video.stat.view > 0 && video.stat.danmaku > 0) {
                // 🔗 [共享元素] 弹幕数
                var danmakuModifier = Modifier.wrapContentSize()
                if (transitionEnabled && sharedTransitionScope != null && animatedVisibilityScope != null) {
                    with(sharedTransitionScope) {
                        danmakuModifier = danmakuModifier.sharedBounds(
                            sharedContentState = rememberSharedContentState(key = "video_danmaku_${video.bvid}"),
                            animatedVisibilityScope = animatedVisibilityScope,
                            boundsTransform = { _, _ ->
                                spring(dampingRatio = 0.8f, stiffness = 200f)
                            }
                        )
                    }
                }

                Box(modifier = danmakuModifier) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Icon(
                            imageVector = CupertinoIcons.Outlined.BubbleLeft,
                            contentDescription = null,
                            modifier = Modifier.size(12.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = FormatUtils.formatStat(video.stat.danmaku.toLong()),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
    
    
    if (showUnfavoriteDialog) {
        AlertDialog(
            onDismissRequest = { showUnfavoriteDialog = false },
            title = { Text("取消收藏") },
            text = { Text("确定要将此视频从收藏夹中移除吗？") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showUnfavoriteDialog = false
                        onUnfavorite?.invoke()
                    }
                ) {
                    Text("移除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showUnfavoriteDialog = false }) {
                    Text("取消")
                }
            }
        )
    }

    //  [新增] 长按操作菜单
    DropdownMenu(
        expanded = showDismissMenu,
        onDismissRequest = { showDismissMenu = false }
    ) {
        // 稍后再看
        if (onWatchLater != null) {
            DropdownMenuItem(
                text = { 
                    Text(
                        "🕐 稍后再看",
                        color = MaterialTheme.colorScheme.onSurface
                    ) 
                },
                onClick = {
                    showDismissMenu = false
                    onWatchLater.invoke()
                }
            )
        }
        
        
        // 取消收藏 (仅在收藏页显示)
        if (onUnfavorite != null) {
             DropdownMenuItem(
                text = { 
                    Text(
                        "💔 取消收藏",
                        color = MaterialTheme.colorScheme.error  // 使用错误色强调删除操作
                    ) 
                },
                onClick = {
                    showDismissMenu = false
                    // onUnfavorite.invoke() -> 改为弹窗确认
                    showUnfavoriteDialog = true
                }
            )
        }
        
        // 不感兴趣 (放第一位，方便操作) -> 改回下方
        if (onDismiss != null) {
            DropdownMenuItem(
                text = { 
                    Text(
                        "🚫 不感兴趣",
                        color = MaterialTheme.colorScheme.onSurface
                    ) 
                },
                onClick = {
                    showDismissMenu = false
                    onDismiss.invoke()
                }
            )
        }
    }
}

/**
 * 简化版视频网格项 (用于搜索结果等)
 * 注意: onClick 只接收 bvid，不接收 cid
 */
@Composable
fun VideoGridItem(video: VideoItem, index: Int, onClick: (String) -> Unit) {
    ElegantVideoCard(video, index) { bvid, _ -> onClick(bvid) }
}
