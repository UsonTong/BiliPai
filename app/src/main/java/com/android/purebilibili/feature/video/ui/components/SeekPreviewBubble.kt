// File: feature/video/ui/components/SeekPreviewBubble.kt
package com.android.purebilibili.feature.video.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.android.purebilibili.core.util.FormatUtils
import com.android.purebilibili.data.model.response.VideoshotData

/**
 * 进度条拖动预览气泡
 * 
 * 显示视频缩略图和目标时间，类似 B 站网页版效果
 */
@Composable
fun SeekPreviewBubble(
    videoshotData: VideoshotData?,
    targetPositionMs: Long,
    currentPositionMs: Long,
    durationMs: Long,
    offsetX: Float,            // 水平偏移量 (相对于进度条左端)
    containerWidth: Float,      // 进度条容器宽度
    modifier: Modifier = Modifier
) {
    // 计算气泡位置（限制在容器边界内）
    val bubbleWidth = 160.dp
    val bubbleHeight = 90.dp
    val bubbleWidthPx = with(LocalDensity.current) { bubbleWidth.toPx() }
    val halfBubble = bubbleWidthPx / 2
    
    // 限制气泡水平位置在容器内
    // [修复] 当 containerWidth 小于 bubbleWidth 时（居中显示场景），跳过位置限制
    val clampedOffsetX = if (containerWidth > bubbleWidthPx) {
        offsetX.coerceIn(halfBubble, containerWidth - halfBubble)
    } else {
        halfBubble // 居中显示时，直接使用半宽偏移
    }
    
    val context = LocalContext.current
    val previewInfo = remember(videoshotData, targetPositionMs, durationMs) {
        videoshotData?.getPreviewInfo(targetPositionMs, durationMs)
    }
    
    Box(
        modifier = modifier
            .offset { IntOffset((clampedOffsetX - halfBubble).toInt(), 0) }
            .shadow(6.dp, RoundedCornerShape(8.dp))
            .clip(RoundedCornerShape(8.dp))
            .width(bubbleWidth)
            .height(bubbleHeight)
            .background(Color.Black)
    ) {
        // 1. 视频缩略图 (底层)
        if (previewInfo != null && videoshotData != null) {
            val (rawImageUrl, spriteOffsetX, spriteOffsetY) = previewInfo
            
            // 🔧 修复：B站 URL 可能以 // 开头，需要补全 https:
            val imageUrl = if (rawImageUrl.startsWith("//")) {
                "https:$rawImageUrl"
            } else {
                rawImageUrl
            }
            
            val thumbWidthPx = videoshotData.img_x_size
            val thumbHeightPx = videoshotData.img_y_size
            
            SubcomposeAsyncImage(
                model = ImageRequest.Builder(context)
                    .data(imageUrl)
                    .size(coil.size.Size.ORIGINAL)
                    .transformations(
                        SpriteCropTransformation(
                            offsetX = spriteOffsetX,
                            offsetY = spriteOffsetY,
                            cropWidth = thumbWidthPx,
                            cropHeight = thumbHeightPx
                        )
                    )
                    .crossfade(true)
                    .build(),
                contentDescription = "seek_preview",
                contentScale = ContentScale.Crop, // 确保图片填满
                modifier = Modifier.fillMaxSize(),
                loading = {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("...", color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp)
                    }
                },
                error = {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("×", color = com.android.purebilibili.core.theme.iOSRed, fontSize = 16.sp)
                    }
                }
            )
        } else {
            // 无预览图时显示占位
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.DarkGray),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "预览加载中...",
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 12.sp
                )
            }
        }
        
        // 2. 底部渐变遮罩 (中间层) - 仅在文字区域
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(40.dp)
                .background(
                    androidx.compose.ui.graphics.Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.8f)
                        )
                    )
                )
        )

        // 3. 时间标签 (顶层)
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 目标时间
            Text(
                text = FormatUtils.formatDuration((targetPositionMs / 1000).toInt()),
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                style = androidx.compose.ui.text.TextStyle(
                    shadow = androidx.compose.ui.graphics.Shadow(
                        color = Color.Black.copy(alpha = 0.5f),
                        blurRadius = 4f
                    )
                )
            )
            
            // 时间差
            val deltaSeconds = (targetPositionMs - currentPositionMs) / 1000
            if (deltaSeconds != 0L) {
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (deltaSeconds > 0) "+${deltaSeconds}s" else "${deltaSeconds}s",
                    color = if (deltaSeconds > 0) Color(0xFF81C784) else Color(0xFFE57373), // 稍微调亮一点颜色以在黑底上更清晰
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    style = androidx.compose.ui.text.TextStyle(
                        shadow = androidx.compose.ui.graphics.Shadow(
                            color = Color.Black.copy(alpha = 0.5f),
                            blurRadius = 4f
                        )
                    )
                )
            }
        }
    }
}

/**
 * 简化版预览气泡（仅显示时间，无缩略图）
 * 
 * 用于无 videoshot 数据时的降级显示
 */
@Composable
fun SeekPreviewBubbleSimple(
    targetPositionMs: Long,
    currentPositionMs: Long,
    offsetX: Float,
    containerWidth: Float,
    modifier: Modifier = Modifier
) {
    val bubbleWidth = 100.dp
    val bubbleWidthPx = with(LocalDensity.current) { bubbleWidth.toPx() }
    val halfBubble = bubbleWidthPx / 2
    val clampedOffsetX = offsetX.coerceIn(halfBubble, containerWidth - halfBubble)
    
    Box(
        modifier = modifier
            .offset { IntOffset((clampedOffsetX - halfBubble).toInt(), 0) }
            .shadow(4.dp, RoundedCornerShape(8.dp))
            .clip(RoundedCornerShape(8.dp))
            .background(Color.Black.copy(alpha = 0.85f))
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 目标时间
            Text(
                text = FormatUtils.formatDuration((targetPositionMs / 1000).toInt()),
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            
            // 时间差
            val deltaSeconds = (targetPositionMs - currentPositionMs) / 1000
            if (deltaSeconds != 0L) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = if (deltaSeconds > 0) "+${deltaSeconds}s" else "${deltaSeconds}s",
                    color = if (deltaSeconds > 0) Color(0xFF4CAF50) else Color(0xFFFF5252),
                    fontSize = 12.sp
                )
            }
        }
    }
}

/**
 * 自定义 Coil Transformation - 裁剪雪碧图的特定区域
 */
class SpriteCropTransformation(
    private val offsetX: Int,
    private val offsetY: Int,
    private val cropWidth: Int,
    private val cropHeight: Int
) : coil.transform.Transformation {
    
    override val cacheKey: String
        get() = "sprite_crop_${offsetX}_${offsetY}_${cropWidth}_${cropHeight}"
    
    override suspend fun transform(input: android.graphics.Bitmap, size: coil.size.Size): android.graphics.Bitmap {
        // 确保裁剪区域在图片范围内
        val safeX = offsetX.coerceIn(0, (input.width - cropWidth).coerceAtLeast(0))
        val safeY = offsetY.coerceIn(0, (input.height - cropHeight).coerceAtLeast(0))
        val safeWidth = cropWidth.coerceAtMost(input.width - safeX)
        val safeHeight = cropHeight.coerceAtMost(input.height - safeY)
        
        android.util.Log.d("SpriteCrop", "🔪 Cropping: input=${input.width}x${input.height}, crop=($safeX,$safeY,$safeWidth,$safeHeight)")
        
        return if (safeWidth > 0 && safeHeight > 0) {
            android.graphics.Bitmap.createBitmap(input, safeX, safeY, safeWidth, safeHeight)
        } else {
            input
        }
    }
}
