// File: feature/video/ui/section/VideoInfoSection.kt
package com.android.purebilibili.feature.video.ui.section

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
//  已改用 MaterialTheme.colorScheme.primary
import com.android.purebilibili.core.util.FormatUtils
import com.android.purebilibili.data.model.response.UgcSeason
import com.android.purebilibili.data.model.response.VideoStaff
import com.android.purebilibili.data.model.response.ViewInfo
import com.android.purebilibili.data.model.response.VideoTag
import com.android.purebilibili.core.ui.common.copyOnLongPress
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.ui.draw.rotate
import com.android.purebilibili.core.ui.common.copyOnClick
import com.android.purebilibili.core.ui.components.resolveUpStatsText
import com.android.purebilibili.core.ui.components.UserUpBadge
import com.android.purebilibili.core.ui.transition.shouldEnableVideoCoverSharedTransition
import com.android.purebilibili.core.ui.transition.shouldEnableVideoMetadataSharedTransition
import com.android.purebilibili.core.util.CardPositionManager
import com.android.purebilibili.data.model.response.BgmInfo
import com.android.purebilibili.data.model.response.AiSummaryData
import com.android.purebilibili.feature.video.ui.FollowButtonTone
import com.android.purebilibili.feature.video.ui.FollowTextTone
import com.android.purebilibili.feature.video.ui.resolveVideoFollowVisualPolicy

internal const val VIDEO_DESCRIPTION_URL_TAG = "VIDEO_DESCRIPTION_URL"
private val VIDEO_DESCRIPTION_URL_PATTERN =
    """((https?|ftp|file)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|])""".toRegex()
private val VIDEO_DESCRIPTION_INLINE_BVID_PATTERN =
    Regex("""(?<![A-Za-z0-9])BV[a-zA-Z0-9]{10}(?![A-Za-z0-9])""", RegexOption.IGNORE_CASE)

internal fun buildVideoDescriptionAnnotatedString(
    desc: String,
    urlColor: Color
): AnnotatedString {
    data class LinkMatch(
        val range: IntRange,
        val annotation: String,
        val displayText: String,
        val priority: Int
    )

    val matches = mutableListOf<LinkMatch>()
    VIDEO_DESCRIPTION_URL_PATTERN.findAll(desc).forEach { match ->
        matches += LinkMatch(
            range = match.range,
            annotation = match.value,
            displayText = match.value,
            priority = 0
        )
    }
    VIDEO_DESCRIPTION_INLINE_BVID_PATTERN.findAll(desc).forEach { match ->
        val overlapsUrl = matches.any { existing ->
            match.range.first <= existing.range.last && match.range.last >= existing.range.first
        }
        if (!overlapsUrl) {
            matches += LinkMatch(
                range = match.range,
                annotation = "https://www.bilibili.com/video/${match.value}",
                displayText = match.value,
                priority = 1
            )
        }
    }
    matches.sortWith(compareBy<LinkMatch> { it.range.first }.thenBy { it.priority })

    return buildAnnotatedString {
        var lastIndex = 0
        matches.forEach { match ->
            if (lastIndex < match.range.first) {
                append(desc.substring(lastIndex, match.range.first))
            }
            pushStringAnnotation(tag = VIDEO_DESCRIPTION_URL_TAG, annotation = match.annotation)
            withStyle(SpanStyle(color = urlColor, textDecoration = TextDecoration.Underline)) {
                append(match.displayText)
            }
            pop()
            lastIndex = match.range.last + 1
        }
        if (lastIndex < desc.length) {
            append(desc.substring(lastIndex))
        }
    }
}

/**
 * Video Info Section Components
 * 
 * Contains components for displaying video information:
 * - VideoTitleSection: Video title with expand/collapse
 * - VideoTitleWithDesc: Title + stats + description
 * - UpInfoSection: UP owner info with follow button
 * - DescriptionSection: Video description
 * 
 * Requirement Reference: AC3.1 - Video info components in dedicated file
 */

internal fun resolveVideoInfoInitialExpandedState(
    hasDescription: Boolean,
    hasTags: Boolean,
    defaultExpanded: Boolean = true
): Boolean = defaultExpanded && (hasDescription || hasTags)

/**
 * Video Title Section (Bilibili official style: compact layout)
 */
@Composable
fun VideoTitleSection(
    info: ViewInfo,
    animateLayout: Boolean = true,
    onUpClick: (Long) -> Unit = {}
) {
    var expanded by remember { mutableStateOf(false) }
    val publishTimeRowText = remember(info.pubdate, info.tname, info.title) {
        resolvePublishTimeRowText(
            pubdate = info.pubdate,
            partitionName = info.tname,
            title = info.title
        )
    }
    val emphasizePublishTime = remember(info.tname, info.title) {
        shouldEmphasizePrecisePublishTime(
            partitionName = info.tname,
            title = info.title
        )
    }
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .clickable { expanded = !expanded }
            .padding(horizontal = 12.dp, vertical = 4.dp)
    ) {
        // Title row (expandable)
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            Text(
                text = info.title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontSize = 16.sp,
                    lineHeight = 22.sp,
                    fontWeight = FontWeight.SemiBold
                ),
                maxLines = if (expanded) Int.MAX_VALUE else 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .weight(1f)
                    .then(if (animateLayout) Modifier.animateContentSize() else Modifier)
                    .copyOnLongPress(info.title, "视频标题")
            )
            Spacer(Modifier.width(4.dp))
            Icon(
                imageVector = if (expanded) CupertinoIcons.Default.ChevronUp else CupertinoIcons.Default.ChevronDown,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(18.dp)
            )
        }
        
        Spacer(Modifier.height(2.dp))
        
        // Stats row (views, danmaku)
        Text(
            text = "${FormatUtils.formatStat(info.stat.view.toLong())}  \u2022  ${FormatUtils.formatStat(info.stat.danmaku.toLong())}\u5f39\u5e55",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            maxLines = 1
        )

        if (publishTimeRowText.isNotBlank()) {
            Spacer(Modifier.height(6.dp))
            if (emphasizePublishTime) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.78f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        text = publishTimeRowText,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.92f),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            } else {
                Text(
                    text = publishTimeRowText,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f),
                    maxLines = 1
                )
            }
        }
    }
}

/**
 * Video Title with Description (Official layout: title + stats + description)
 *  Description and tags hidden by default, shown on expand
 */


@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class, androidx.compose.animation.ExperimentalSharedTransitionApi::class)
@Composable
fun VideoTitleWithDesc(
    info: ViewInfo,
    videoTags: List<VideoTag> = emptyList(),  //  视频标签
    bgmInfo: BgmInfo? = null, // [新增] BGM 信息
    bgmInfoList: List<BgmInfo> = emptyList(),
    onlineCount: String = "",
    showOnlineCount: Boolean = true,
    transitionEnabled: Boolean = false,  // 🔗 共享元素过渡开关
    animateLayout: Boolean = true,
    onDescriptionUrlClick: ((String) -> Unit)? = null,
    onBgmClick: (BgmInfo) -> Unit = {}
) {
    val context = LocalContext.current
    val defaultExpanded by com.android.purebilibili.core.store.SettingsManager
        .getVideoInfoDefaultExpanded(context)
        .collectAsState(initial = true)
    var expanded by remember(info.bvid, info.desc, videoTags.size, defaultExpanded) {
        mutableStateOf(
            resolveVideoInfoInitialExpandedState(
                hasDescription = info.desc.isNotBlank(),
                hasTags = videoTags.isNotEmpty(),
                defaultExpanded = defaultExpanded
            )
        )
    }
    val publishTimeRowText = remember(info.pubdate, info.tname, info.title) {
        resolvePublishTimeRowText(
            pubdate = info.pubdate,
            partitionName = info.tname,
            title = info.title
        )
    }
    val emphasizePublishTime = remember(info.tname, info.title) {
        shouldEmphasizePrecisePublishTime(
            partitionName = info.tname,
            title = info.title
        )
    }
    val onlineCountText = remember(showOnlineCount, onlineCount) {
        resolveVideoDetailOnlineCountText(
            showOnlineCount = showOnlineCount,
            onlineCount = onlineCount
        )
    }
    val videoBadges = remember(info.isUpowerExclusive, info.isUpowerPreview, info.isCooperation) {
        resolveVideoDetailBadges(info)
    }
    
    //  尝试获取共享元素作用域
    val sharedTransitionScope = com.android.purebilibili.core.ui.LocalSharedTransitionScope.current
    val animatedVisibilityScope = com.android.purebilibili.core.ui.LocalAnimatedVisibilityScope.current
    val coverSharedEnabled = shouldEnableVideoCoverSharedTransition(
        transitionEnabled = transitionEnabled,
        hasSharedTransitionScope = sharedTransitionScope != null,
        hasAnimatedVisibilityScope = animatedVisibilityScope != null
    )
    val metadataSharedEnabled = shouldEnableVideoMetadataSharedTransition(
        coverSharedEnabled = coverSharedEnabled,
        isQuickReturnLimited = CardPositionManager.shouldLimitSharedElementsForQuickReturn()
    )
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .clickable { expanded = !expanded }
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        // Title row (expandable)
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            //  共享元素过渡 - 标题
            var titleModifier = if (animateLayout) Modifier.animateContentSize() else Modifier

            //  注意：使用 ExperimentalSharedTransitionApi 注解需要上下文
            if (metadataSharedEnabled) {
                with(requireNotNull(sharedTransitionScope)) {
                     titleModifier = titleModifier.sharedBounds(
                        sharedContentState = rememberSharedContentState(key = "video_title_${info.bvid}"),
                        animatedVisibilityScope = requireNotNull(animatedVisibilityScope),
                        boundsTransform = { _, _ ->
                            androidx.compose.animation.core.spring(dampingRatio = 0.8f, stiffness = 200f)
                        }
                    )
                }
            }

            Text(
                text = info.title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontSize = 16.sp,
                    lineHeight = 21.sp,
                    fontWeight = FontWeight.SemiBold
                ),
                maxLines = if (expanded) Int.MAX_VALUE else 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = titleModifier.weight(1f)
            )

            val rotateAngle by animateFloatAsState(
                targetValue = if (expanded) 180f else 0f, // 展开时旋转180度
                animationSpec = tween(durationMillis = 300), // 设置动画时长和曲线
                label = "IconRotation"
            )
            Icon(
                imageVector = CupertinoIcons.Default.ChevronDown,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier
                    .rotate(rotateAngle)
                    .size(20.dp)
                    .padding(4.dp)
            )
        }
        
        Spacer(Modifier.height(4.dp))
        
        // Stats row
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Stats Row split for shared element transitions
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Views
                var viewsModifier = Modifier.wrapContentSize()
                if (metadataSharedEnabled) {
                    with(requireNotNull(sharedTransitionScope)) {
                        viewsModifier = viewsModifier.sharedBounds(
                            sharedContentState = rememberSharedContentState(key = "video_views_${info.bvid}"),
                            animatedVisibilityScope = requireNotNull(animatedVisibilityScope),
                            boundsTransform = { _, _ ->
                                androidx.compose.animation.core.spring(dampingRatio = 0.8f, stiffness = 200f)
                            }
                        )
                    }
                }
                Text(
                    text = "${FormatUtils.formatStat(info.stat.view.toLong())}播放",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = viewsModifier
                )

                Text(
                    text = "  •  ",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )

                // Danmaku
                var danmakuModifier = Modifier.wrapContentSize()
                if (metadataSharedEnabled) {
                    with(requireNotNull(sharedTransitionScope)) {
                        danmakuModifier = danmakuModifier.sharedBounds(
                            sharedContentState = rememberSharedContentState(key = "video_danmaku_${info.bvid}"),
                            animatedVisibilityScope = requireNotNull(animatedVisibilityScope),
                            boundsTransform = { _, _ ->
                                androidx.compose.animation.core.spring(dampingRatio = 0.8f, stiffness = 200f)
                            }
                        )
                    }
                }
                Text(
                    text = "${FormatUtils.formatStat(info.stat.danmaku.toLong())}弹幕",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = danmakuModifier
                )

            }
            if (onlineCountText.isNotBlank()) {
                Text(
                    text = "  •  ",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
                Text(
                    text = onlineCountText,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.82f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            // [新增] 显示 BVID 并支持点击复制
            Spacer(Modifier.width(8.dp))
            Text(
                text = info.bvid,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                modifier = Modifier.copyOnClick(info.bvid, "BV号")
            )
        }

        if (videoBadges.isNotEmpty()) {
            Spacer(Modifier.height(6.dp))
            androidx.compose.foundation.layout.FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                videoBadges.forEach { badge ->
                    VideoDetailBadgeChip(
                        text = badge,
                        emphasized = badge.startsWith("充电专属")
                    )
                }
            }
        }

        if (publishTimeRowText.isNotBlank()) {
            Spacer(Modifier.height(6.dp))
            if (emphasizePublishTime) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.78f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        text = publishTimeRowText,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.92f),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            } else {
                Text(
                    text = publishTimeRowText,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f),
                    maxLines = 1
                )
            }
        }

        // [新增] BGM Info Row
        val displayBgmList = remember(bgmInfo, bgmInfoList) {
            if (bgmInfoList.size > 1) bgmInfoList
            else if (bgmInfo != null) listOf(bgmInfo)
            else emptyList()
        }
        if (displayBgmList.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            InlineBgmSection(
                bgmList = displayBgmList,
                onBgmClick = onBgmClick
            )
        }
        
        //  Description - 默认隐藏，展开后显示
        androidx.compose.animation.AnimatedVisibility(
            visible = expanded && info.desc.isNotBlank(),
            enter = if (animateLayout) {
                androidx.compose.animation.expandVertically() + androidx.compose.animation.fadeIn()
            } else {
                androidx.compose.animation.EnterTransition.None
            },
            exit = if (animateLayout) {
                androidx.compose.animation.shrinkVertically() + androidx.compose.animation.fadeOut()
            } else {
                androidx.compose.animation.ExitTransition.None
            }
        ) {
            Column {
                Spacer(Modifier.height(6.dp))
                val descriptionUrlColor = MaterialTheme.colorScheme.primary
                val descriptionText = remember(info.desc, descriptionUrlColor) {
                    buildVideoDescriptionAnnotatedString(
                        desc = info.desc,
                        urlColor = descriptionUrlColor
                    )
                }
                var descriptionTextLayout by remember { mutableStateOf<TextLayoutResult?>(null) }
                val descriptionModifier = if (onDescriptionUrlClick != null) {
                    Modifier.pointerInput(descriptionText, info.desc, onDescriptionUrlClick) {
                        detectTapGestures { offset ->
                            val layoutResult = descriptionTextLayout ?: return@detectTapGestures
                            val position = layoutResult.getOffsetForPosition(offset)
                            val searchStart = maxOf(0, position - 1)
                            val searchEnd = minOf(descriptionText.length, position + 1)
                            descriptionText.getStringAnnotations(
                                tag = VIDEO_DESCRIPTION_URL_TAG,
                                start = searchStart,
                                end = searchEnd
                            ).firstOrNull()?.let { annotation ->
                                onDescriptionUrlClick(annotation.item)
                            }
                        }
                    }
                } else {
                    Modifier
                }
                // [新增] 使用 SelectionContainer 支持滑动复制
                SelectionContainer {
                    Text(
                        text = descriptionText,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 12.sp,
                            lineHeight = 17.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                        onTextLayout = { descriptionTextLayout = it },
                        modifier = (if (animateLayout) Modifier.animateContentSize() else Modifier)
                            .then(descriptionModifier)
                    )
                }
            }
        }
        
        //  Tags - 默认隐藏，展开后显示
        androidx.compose.animation.AnimatedVisibility(
            visible = expanded && videoTags.isNotEmpty(),
            enter = if (animateLayout) {
                androidx.compose.animation.expandVertically() + androidx.compose.animation.fadeIn()
            } else {
                androidx.compose.animation.EnterTransition.None
            },
            exit = if (animateLayout) {
                androidx.compose.animation.shrinkVertically() + androidx.compose.animation.fadeOut()
            } else {
                androidx.compose.animation.ExitTransition.None
            }
        ) {
            Column {
                Spacer(Modifier.height(8.dp))
                androidx.compose.foundation.layout.FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    videoTags.take(10).forEach { tag ->
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text(
                                text = tag.tag_name,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                                    .copyOnLongPress(tag.tag_name, "标签")
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun VideoDetailBadgeChip(
    text: String,
    emphasized: Boolean
) {
    val containerColor = if (emphasized) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.82f)
    }
    val contentColor = if (emphasized) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    Surface(
        color = containerColor,
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(
            text = text,
            fontSize = 11.sp,
            lineHeight = 14.sp,
            fontWeight = FontWeight.Medium,
            color = contentColor,
            maxLines = 1,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

/**
 * UP Owner Info Section (Bilibili official style: blue UP tag)
 */
@OptIn(androidx.compose.animation.ExperimentalSharedTransitionApi::class)
@Composable
fun UpInfoSection(
    info: ViewInfo,
    isFollowing: Boolean = false,
    onFollowClick: () -> Unit = {},
    onUpClick: (Long) -> Unit = {},
    showOwnerAvatar: Boolean = true,
    followerCount: Int? = null,
    videoCount: Int? = null,
    transitionEnabled: Boolean = false,  // 🔗 共享元素过渡开关
    modifier: Modifier = Modifier
) {
    //  尝试获取共享元素作用域
    val sharedTransitionScope = com.android.purebilibili.core.ui.LocalSharedTransitionScope.current
    val animatedVisibilityScope = com.android.purebilibili.core.ui.LocalAnimatedVisibilityScope.current
    val coverSharedEnabled = shouldEnableVideoCoverSharedTransition(
        transitionEnabled = transitionEnabled,
        hasSharedTransitionScope = sharedTransitionScope != null,
        hasAnimatedVisibilityScope = animatedVisibilityScope != null
    )
    val metadataSharedEnabled = shouldEnableVideoMetadataSharedTransition(
        coverSharedEnabled = coverSharedEnabled,
        isQuickReturnLimited = CardPositionManager.shouldLimitSharedElementsForQuickReturn()
    )
    val upStatsText = resolveUpStatsText(
        followerCount = followerCount,
        videoCount = videoCount
    )
    val showInlineOwnerIdentity = shouldShowInlineOwnerIdentity(showOwnerAvatar = showOwnerAvatar)
    Column(
        modifier = modifier
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onUpClick(info.owner.mid) }
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (showOwnerAvatar) {
                var avatarModifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)

                if (metadataSharedEnabled) {
                    with(requireNotNull(sharedTransitionScope)) {
                        avatarModifier = avatarModifier.sharedBounds(
                            sharedContentState = rememberSharedContentState(key = "video_avatar_${info.bvid}"),
                            animatedVisibilityScope = requireNotNull(animatedVisibilityScope),
                            boundsTransform = { _, _ ->
                                androidx.compose.animation.core.spring(dampingRatio = 0.8f, stiffness = 200f)
                            },
                            clipInOverlayDuringTransition = OverlayClip(CircleShape)
                        )
                    }
                }

                if (info.owner.face.isNotBlank()) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(FormatUtils.fixImageUrl(info.owner.face))
                            .crossfade(true)
                            .build(),
                        contentDescription = "UP主头像",
                        modifier = avatarModifier,
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = avatarModifier,
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = CupertinoIcons.Default.PersonCropCircle,
                            contentDescription = "UP主标识",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            } else {
                UserUpBadge()
            }

            Spacer(Modifier.width(10.dp))

            // UP owner name row
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    //  共享元素过渡 - UP主名称
                    //  [调整] 确保 sharedBounds 在交互修饰符之前应用
                    var upNameModifier: Modifier = Modifier

                    if (metadataSharedEnabled) {
                        with(requireNotNull(sharedTransitionScope)) {
                            upNameModifier = upNameModifier.sharedBounds(
                                sharedContentState = rememberSharedContentState(key = "video_up_${info.bvid}"),
                                animatedVisibilityScope = requireNotNull(animatedVisibilityScope),
                                boundsTransform = { _, _ ->
                                    androidx.compose.animation.core.spring(dampingRatio = 0.8f, stiffness = 200f)
                                }
                            )
                        }
                    }

                    //  添加交互修饰符 (放在 sharedBounds 之后，使其包含在 sharedBounds 内部)
                    upNameModifier = upNameModifier.copyOnLongPress(info.owner.name, "UP主名称")

                    if (showInlineOwnerIdentity) {
                        if (info.owner.face.isNotBlank()) {
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(FormatUtils.fixImageUrl(info.owner.face))
                                    .crossfade(true)
                                    .build(),
                                contentDescription = "UP主头像",
                                modifier = Modifier
                                    .padding(horizontal = 2.dp)
                                    .size(18.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceVariant),
                                contentScale = androidx.compose.ui.layout.ContentScale.Crop
                            )
                        } else {
                            UserUpBadge(modifier = Modifier.padding(horizontal = 2.dp))
                        }
                        Spacer(Modifier.width(4.dp))
                    }
                    Text(
                        text = info.owner.name,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = upNameModifier
                    )
                }
                if (!upStatsText.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = upStatsText,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.82f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            var followActionModifier = Modifier.height(36.dp)
            if (metadataSharedEnabled) {
                with(requireNotNull(sharedTransitionScope)) {
                    followActionModifier = followActionModifier.sharedBounds(
                        sharedContentState = rememberSharedContentState(key = "video_up_action_${info.bvid}"),
                        animatedVisibilityScope = requireNotNull(animatedVisibilityScope),
                        boundsTransform = { _, _ ->
                            androidx.compose.animation.core.spring(dampingRatio = 0.8f, stiffness = 200f)
                        },
                        clipInOverlayDuringTransition = OverlayClip(RoundedCornerShape(16.dp))
                    )
                }
            }

            // Follow button
            val followVisualPolicy = remember(isFollowing) {
                resolveVideoFollowVisualPolicy(isFollowing = isFollowing)
            }
            Surface(
                onClick = onFollowClick,
                color = when (followVisualPolicy.detailButtonTone) {
                    FollowButtonTone.PRIMARY -> MaterialTheme.colorScheme.primary
                    FollowButtonTone.PRIMARY_CONTAINER -> MaterialTheme.colorScheme.primaryContainer
                },
                shape = RoundedCornerShape(18.dp),
                modifier = followActionModifier
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 16.dp)
                ) {
                    if (!isFollowing) {
                        Icon(
                            CupertinoIcons.Default.Plus,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(2.dp))
                    }
                    Text(
                        text = if (isFollowing) "\u5df2\u5173\u6ce8" else "\u5173\u6ce8",
                        fontSize = 13.sp,
                        color = when (followVisualPolicy.detailTextTone) {
                            FollowTextTone.ON_PRIMARY -> MaterialTheme.colorScheme.onPrimary
                            FollowTextTone.ON_PRIMARY_CONTAINER -> MaterialTheme.colorScheme.onPrimaryContainer
                        },
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
        if (shouldShowCreatorTeamSection(info)) {
            CreatorTeamSection(
                staff = info.staff,
                onMemberClick = onUpClick
            )
        }
    }
}

@Composable
private fun CreatorTeamSection(
    staff: List<VideoStaff>,
    onMemberClick: (Long) -> Unit
) {
    if (staff.isEmpty()) return
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 12.dp, end = 12.dp, bottom = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "创作团队",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = "共 ${staff.size} 位",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            staff.forEach { member ->
                CreatorTeamMemberChip(
                    member = member,
                    onClick = { onMemberClick(member.mid) }
                )
            }
        }
    }
}

@Composable
private fun CreatorTeamMemberChip(
    member: VideoStaff,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(enabled = member.mid > 0L, onClick = onClick)
            .padding(end = 4.dp)
            .heightIn(min = 48.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(FormatUtils.fixImageUrl(member.face))
                .crossfade(true)
                .build(),
            contentDescription = "${member.name} 头像",
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentScale = androidx.compose.ui.layout.ContentScale.Crop
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column(
            modifier = Modifier.widthIn(min = 64.dp, max = 112.dp)
        ) {
            Text(
                text = member.name,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (member.title.isNotBlank()) {
                Text(
                    text = member.title,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/**
 * Description Section (optimized style)
 */
@Composable
fun DescriptionSection(desc: String) {
    var expanded by remember { mutableStateOf(false) }

    if (desc.isBlank()) return

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .animateContentSize()
        ) {
            Text(
                text = desc,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.9f),
                maxLines = if (expanded) Int.MAX_VALUE else 3,
                overflow = TextOverflow.Ellipsis
            )

            if (desc.length > 100 || desc.lines().size > 3) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { expanded = !expanded }
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (expanded) "\u6536\u8d77" else "\u5c55\u5f00\u66f4\u591a",
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Icon(
                        imageVector = if (expanded) CupertinoIcons.Default.ChevronUp else CupertinoIcons.Default.ChevronDown,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun InlineBgmSection(
    bgmList: List<BgmInfo>,
    onBgmClick: (BgmInfo) -> Unit = {}
) {
    if (bgmList.isEmpty()) return

    if (bgmList.size == 1) {
        BgmInfoRow(
            bgmInfo = bgmList.first(),
            onBgmClick = onBgmClick
        )
    } else {
        BgmInfoListRow(
            bgmList = bgmList,
            onBgmClick = onBgmClick
        )
    }
}

/**
 * [新增] 背景音乐信息行
 */
@Composable
fun BgmInfoRow(
    bgmInfo: BgmInfo,
    onBgmClick: (BgmInfo) -> Unit = {}
) {
    Surface(
        // Optimization: Use primary color with very low alpha for a subtle, branded look
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onBgmClick(bgmInfo) }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = CupertinoIcons.Default.MusicNote,
                contentDescription = "BGM",
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = buildString {
                    append(bgmInfo.musicTitle.ifEmpty { "发现音乐" })
                    if (bgmInfo.actor.isNotBlank()) {
                        append(" - ")
                        append(bgmInfo.actor)
                    }
                },
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Medium
                ),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.9f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun BgmInfoListRow(
    bgmList: List<BgmInfo>,
    onBgmClick: (BgmInfo) -> Unit = {}
) {
    var expanded by remember { mutableStateOf(false) }
    val displayList = if (expanded) bgmList else bgmList.take(2)

    Surface(
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = CupertinoIcons.Default.MusicNote,
                    contentDescription = "BGM",
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "发现 ${bgmList.size} 首音乐",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Medium
                    ),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.9f),
                    modifier = Modifier.weight(1f)
                )
                if (bgmList.size > 2) {
                    Text(
                        text = if (expanded) "收起" else "展开",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                        modifier = Modifier.clickable { expanded = !expanded }
                    )
                }
            }
            displayList.forEach { bgm ->
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onBgmClick(bgm) }
                        .padding(start = 22.dp, top = 2.dp, bottom = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = buildString {
                            append(bgm.musicTitle.ifEmpty { "未知音乐" })
                            if (bgm.actor.isNotBlank()) {
                                append(" - ")
                                append(bgm.actor)
                            }
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}
