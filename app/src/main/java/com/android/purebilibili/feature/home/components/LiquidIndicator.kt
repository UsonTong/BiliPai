// 文件路径: feature/home/components/LiquidIndicator.kt
package com.android.purebilibili.feature.home.components

import android.os.Build
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.CornerRadius
import kotlin.math.abs

/**
 * 🌊 液态玻璃选中指示器
 * 
 * 实现类似 visionOS 的玻璃折射效果：
 * - 透镜折射效果 (Android 13+ 支持)
 * - 拖拽时放大形变
 * - 高光和内阴影
 * 
 * @param position 当前位置（浮点索引）
 * @param itemWidth 单个项目宽度
 * @param itemCount 项目数量
 * @param isDragging 是否正在拖拽
 * @param velocity 当前速度（用于形变）
 * @param hazeState HazeState 实例（用于模糊效果）
 * @param modifier Modifier
 */
@Composable
fun LiquidIndicator(
    position: Float,
    itemWidth: Dp,
    itemCount: Int,
    isDragging: Boolean,
    velocity: Float = 0f,
    startPadding: Dp = 0.dp,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    
    // 指示器尺寸
    val indicatorWidth = itemWidth + 24.dp
    val indicatorHeight = 55.dp
    
    // 计算位置 (OFFSET 模式)
    val itemStartOffset = with(density) { (position * itemWidth.toPx()).toDp() }
    val currentOffset = startPadding + itemStartOffset + (itemWidth / 2) - (indicatorWidth / 2)
    
    // 速度形变
    val velocityFraction = (velocity / 3000f).coerceIn(-1f, 1f)
    val deformation = abs(velocityFraction) * 0.4f
    
    val targetScaleX = 1f + deformation
    val targetScaleY = 1f - (deformation * 0.6f)
    
    val scaleX by animateFloatAsState(targetValue = targetScaleX, animationSpec = spring(dampingRatio = 0.5f, stiffness = 600f), label = "scaleX")
    val scaleY by animateFloatAsState(targetValue = targetScaleY, animationSpec = spring(dampingRatio = 0.5f, stiffness = 600f), label = "scaleY")
    val dragScale by animateFloatAsState(targetValue = if (isDragging) 1.0f else 1f, animationSpec = spring(dampingRatio = 0.6f, stiffness = 400f), label = "dragScale")

    val finalScaleX = scaleX * dragScale
    val finalScaleY = scaleY * dragScale

    // 指示器形状
    val shape = RoundedCornerShape(indicatorHeight / 2)
    
    // [修改] 颜色：使用 Primary 色调，去除去折射/模糊
    val indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.CenterStart
    ) {
         Box(
            modifier = Modifier
                .offset(x = currentOffset)
                .size(indicatorWidth, indicatorHeight)
                .graphicsLayer {
                    this.scaleX = finalScaleX
                    this.scaleY = finalScaleY
                    shadowElevation = 0f
                }
                .clip(shape)
                .background(indicatorColor)
        )
    }
}

/**
 * 简化版液态指示器（不依赖 Backdrop）
 * 
 * 使用标准 Compose 动画实现类似效果
 */
@Composable
fun SimpleLiquidIndicator(
    positionState: State<Float>, // [Optimized] Pass State to defer reading
    itemWidth: Dp,
    isDragging: Boolean,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    
    // [Updated] Shrink size slightly as requested
    val indicatorWidth = itemWidth
    val indicatorHeight = 36.dp
    
    // [Optimized] Defer calculation to avoiding recomposing parent
    val offsetX by remember {
        derivedStateOf {
            with(density) {
                (positionState.value * itemWidth.toPx()).toDp()
            }
        }
    }
    
    val scale by animateFloatAsState(
        targetValue = if (isDragging) 1.05f else 1f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 400f),
        label = "scale"
    )
    
    // [Updated] Match BottomBar style: Primary color with alpha
    val indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
    
    Box(
        modifier = modifier
            .offset(x = offsetX, y = 0.dp)
            .graphicsLayer {
                this.scaleX = scale
                this.scaleY = scale
            }
            .size(indicatorWidth, indicatorHeight)
            .clip(RoundedCornerShape(18.dp)) // Half of 36dp
            .background(indicatorColor)
    )
}
