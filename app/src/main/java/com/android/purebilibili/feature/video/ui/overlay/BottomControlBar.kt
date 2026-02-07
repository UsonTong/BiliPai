// File: feature/video/ui/overlay/BottomControlBar.kt
package com.android.purebilibili.feature.video.ui.overlay

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.graphics.Brush
//  Cupertino Icons
import io.github.alexzhirkevich.cupertino.icons.CupertinoIcons
import io.github.alexzhirkevich.cupertino.icons.filled.*
import io.github.alexzhirkevich.cupertino.icons.outlined.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.purebilibili.core.util.FormatUtils
import com.android.purebilibili.feature.video.ui.components.VideoAspectRatio
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.ui.draw.clip

/**
 * Bottom Control Bar Component
 * 
 * Redesigned Control Bar:
 * [Play/Pause] [Time]  [Danmaku Switch] [       Input Bar       ] [Settings]  [Quality] [Speed] [Fullscreen]
 */

data class PlayerProgress(
    val current: Long = 0L,
    val duration: Long = 0L,
    val buffered: Long = 0L
)

@Composable
fun BottomControlBar(
    isPlaying: Boolean,
    progress: PlayerProgress,
    isFullscreen: Boolean,
    currentSpeed: Float = 1.0f,
    currentRatio: VideoAspectRatio = VideoAspectRatio.FIT,
    onPlayPauseClick: () -> Unit,
    onSeek: (Long) -> Unit,
    onSeekStart: () -> Unit = {},
    onSpeedClick: () -> Unit = {},
    onRatioClick: () -> Unit = {},
    onToggleFullscreen: () -> Unit,
    
    // Danmaku
    danmakuEnabled: Boolean = true,
    onDanmakuToggle: () -> Unit = {},
    onDanmakuSettingsClick: () -> Unit = {},
    
    // Quality
    currentQualityLabel: String = "",
    onQualityClick: () -> Unit = {},
    
    // Features
    videoshotData: com.android.purebilibili.data.model.response.VideoshotData? = null,
    viewPoints: List<com.android.purebilibili.data.model.response.ViewPoint> = emptyList(),
    currentChapter: String? = null,
    onChapterClick: () -> Unit = {},
    
    // Portrait controls (kept for compatibility, though less used in new design)
    isVerticalVideo: Boolean = false,
    onPortraitFullscreen: () -> Unit = {},
    currentPlayMode: com.android.purebilibili.feature.video.player.PlayMode = com.android.purebilibili.feature.video.player.PlayMode.SEQUENTIAL,
    onPlayModeClick: () -> Unit = {},
    onPipClick: () -> Unit = {},
    
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp)
            .let { if (isFullscreen) it.navigationBarsPadding() else it }
    ) {
        // 1. Progress Bar (Top of controls)
        VideoProgressBar(
            currentPosition = progress.current,
            duration = progress.duration,
            bufferedPosition = progress.buffered,
            onSeek = onSeek,
            onSeekStart = onSeekStart,
            videoshotData = videoshotData,
            viewPoints = viewPoints,
            currentChapter = currentChapter,
            onChapterClick = onChapterClick
        )

        Spacer(modifier = Modifier.height(8.dp))

        // 2. Control Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left: Play/Pause
            IconButton(
                onClick = onPlayPauseClick,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = if (isPlaying) CupertinoIcons.Default.Pause else CupertinoIcons.Default.Play,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Time
            Text(
                text = "${FormatUtils.formatDuration((progress.current / 1000).toInt())} / ${FormatUtils.formatDuration((progress.duration / 1000).toInt())}",
                color = Color.White.copy(alpha = 0.9f),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.width(16.dp))

            // Center area: Danmaku Controls (Switch + Input) - Only visible in Fullscreen/Landscape
            if (isFullscreen) {
                // Danmaku Switch
                Icon(
                    imageVector = if (danmakuEnabled) CupertinoIcons.Default.TextBubble else CupertinoIcons.Outlined.TextBubble,
                    contentDescription = "Danmaku Toggle",
                    tint = if (danmakuEnabled) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.7f),
                    modifier = Modifier
                        .size(24.dp)
                        .clickable(onClick = onDanmakuToggle)
                )
                
                Spacer(modifier = Modifier.width(12.dp))
                
                // Danmaku Input Box
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(36.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color.White.copy(alpha = 0.2f))
                        .clickable { /* TODO: Open Input Dialog */ },
                    contentAlignment = Alignment.CenterStart
                ) {
                    Text(
                        text = "发个友善的弹幕见证当下...",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 13.sp,
                        modifier = Modifier.padding(start = 16.dp)
                    )
                    
                    // Settings Icon inside input bar (right)
                    IconButton(
                        onClick = onDanmakuSettingsClick,
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .padding(end = 4.dp)
                            .size(32.dp)
                    ) {
                        Icon(
                            imageVector = CupertinoIcons.Default.Gearshape,
                            contentDescription = "Settings",
                            tint = Color.White.copy(alpha = 0.8f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.width(16.dp))
            } else {
                Spacer(modifier = Modifier.weight(1f))
            }

            // Right: Function Buttons
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Quality
                if (currentQualityLabel.isNotEmpty()) {
                    Text(
                        text = currentQualityLabel,
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.clickable(onClick = onQualityClick)
                    )
                }
                
                // Speed
                Text(
                    text = if (currentSpeed == 1.0f) "倍速" else "${currentSpeed}x",
                    color = if (currentSpeed == 1.0f) Color.White else MaterialTheme.colorScheme.primary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.clickable(onClick = onSpeedClick)
                )

                // 📱 [修复] 竖屏全屏按钮 - 仅在非全屏模式下显示
                if (!isFullscreen) {
                    Text(
                        text = "竖屏",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.clickable(onClick = onPortraitFullscreen)
                    )
                }

                // Fullscreen
                Icon(
                    imageVector = if (isFullscreen) CupertinoIcons.Default.ArrowDownRightAndArrowUpLeft else CupertinoIcons.Default.ArrowUpLeftAndArrowDownRight,
                    contentDescription = "Fullscreen",
                    tint = Color.White,
                    modifier = Modifier
                        .size(20.dp)
                        .clickable(onClick = onToggleFullscreen)
                )
            }
        }
    }
}

/**
 * Reusing existing VideoProgressBar
 */
@Composable
fun VideoProgressBar(
    currentPosition: Long,
    duration: Long,
    bufferedPosition: Long,
    onSeek: (Long) -> Unit,
    onSeekStart: () -> Unit = {},
    videoshotData: com.android.purebilibili.data.model.response.VideoshotData? = null,
    viewPoints: List<com.android.purebilibili.data.model.response.ViewPoint> = emptyList(),
    currentChapter: String? = null,
    onChapterClick: () -> Unit = {}
) {
     val progress = if (duration > 0) currentPosition.toFloat() / duration else 0f
    val bufferedProgress = if (duration > 0) bufferedPosition.toFloat() / duration else 0f
    var tempProgress by remember { mutableFloatStateOf(0f) }
    var isDragging by remember { mutableStateOf(false) }
    var dragOffsetX by remember { mutableFloatStateOf(0f) }
    var containerWidth by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(progress) {
        if (!isDragging) {
            tempProgress = progress
        }
    }
    
    val displayProgress = if (isDragging) tempProgress else progress
    val primaryColor = MaterialTheme.colorScheme.primary
    val targetPositionMs = (tempProgress * duration).toLong()
    val baseHeight = if (currentChapter != null) 32.dp else 20.dp
    val containerHeight = if (isDragging && videoshotData != null) 100.dp else baseHeight

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(containerHeight)
            .pointerInput(Unit) {
                containerWidth = size.width.toFloat()
                detectTapGestures { offset ->
                    val newProgress = (offset.x / size.width).coerceIn(0f, 1f)
                    onSeek((newProgress * duration).toLong())
                }
            }
            .pointerInput(Unit) {
                containerWidth = size.width.toFloat()
                detectDragGestures(
                    onDragStart = { offset ->
                        isDragging = true
                        tempProgress = (offset.x / size.width).coerceIn(0f, 1f)
                        dragOffsetX = offset.x
                        onSeekStart()
                    },
                    onDrag = { change, _ ->
                        change.consume()
                        tempProgress = (change.position.x / size.width).coerceIn(0f, 1f)
                        dragOffsetX = change.position.x
                    },
                    onDragEnd = {
                        isDragging = false
                        onSeek((tempProgress * duration).toLong())
                    },
                    onDragCancel = {
                        isDragging = false
                        tempProgress = progress
                    }
                )
            }
    ) {
         if (isDragging) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(bottom = 24.dp)
            ) {
                if (videoshotData != null && videoshotData.isValid) {
                    com.android.purebilibili.feature.video.ui.components.SeekPreviewBubble(
                        videoshotData = videoshotData,
                        targetPositionMs = targetPositionMs,
                        currentPositionMs = currentPosition,
                        durationMs = duration,
                        offsetX = dragOffsetX,
                        containerWidth = containerWidth
                    )
                } else {
                    com.android.purebilibili.feature.video.ui.components.SeekPreviewBubbleSimple(
                        targetPositionMs = targetPositionMs,
                        currentPositionMs = currentPosition,
                        offsetX = dragOffsetX,
                        containerWidth = containerWidth
                    )
                }
            }
        }
        
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
        ) {
             if (currentChapter != null) {
                Row(
                    modifier = Modifier
                        .clickable(onClick = onChapterClick)
                        .padding(bottom = 4.dp, start = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        CupertinoIcons.Default.ListBullet,
                        contentDescription = "Chapter",
                        tint = Color.White.copy(alpha = 0.8f),
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = currentChapter,
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(20.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                 Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .background(Color.White.copy(alpha = 0.3f), RoundedCornerShape(1.5.dp))
                        .drawWithContent {
                            drawContent()
                            if (duration > 0 && viewPoints.isNotEmpty()) {
                                viewPoints.forEach { point ->
                                    val position = point.fromMs.toFloat() / duration
                                    if (position > 0.01f && position < 0.99f) {
                                        val x = size.width * position
                                        drawLine(
                                            color = Color.White.copy(alpha = 0.8f),
                                            start = Offset(x, 0f),
                                            end = Offset(x, size.height),
                                            strokeWidth = 2f
                                        )
                                    }
                                }
                            }
                        }
                )
                
                Box(
                    modifier = Modifier
                        .fillMaxWidth(bufferedProgress.coerceIn(0f, 1f))
                        .height(3.dp)
                        .background(Color.White.copy(alpha = 0.5f), RoundedCornerShape(1.5.dp))
                )
                
                Box(
                    modifier = Modifier
                        .fillMaxWidth(displayProgress.coerceIn(0f, 1f))
                        .height(3.dp)
                        .background(primaryColor, RoundedCornerShape(1.5.dp))
                )
                
                Box(
                    modifier = Modifier
                        .fillMaxWidth(displayProgress.coerceIn(0f, 1f))
                ) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .size(if (isDragging) 16.dp else 12.dp)
                            .offset(x = if (isDragging) 8.dp else 6.dp)
                            .background(primaryColor, CircleShape)
                    )
                }
            }
        }
    }
}
