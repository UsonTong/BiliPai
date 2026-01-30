// 文件路径: feature/home/components/LiquidIndicator.kt
package com.android.purebilibili.feature.home.components



import android.os.Build
import androidx.compose.animation.core.*
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
import com.android.purebilibili.core.ui.effect.liquidGlass
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.lens
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas

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
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
    isLiquidGlassEnabled: Boolean = false,
    backdrop: LayerBackdrop? = null // [New] Backdrop for refraction
) {
    val density = LocalDensity.current
    
    // 指示器尺寸 - 增大指示器
    val indicatorWidth = 90.dp
    val indicatorHeight = 52.dp
    
    // [优化] 使用 graphicsLayer 进行位移，避免 Layout 重排
    // 计算位置 (Px)
    val itemWidthPx = with(density) { itemWidth.toPx() }
    val startPaddingPx = with(density) { startPadding.toPx() }
    // 居中偏移：(Item宽度 - 指示器宽度) / 2
    val centerOffsetPx = with(density) { (itemWidth.toPx() - indicatorWidth.toPx()) / 2 }
    
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
    
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.CenterStart
    ) {
         Box(
            modifier = Modifier
                .graphicsLayer {
                    // [核心优化] 在绘制阶段计算位移
                    val currentItemPx = position * itemWidthPx
                    translationX = startPaddingPx + currentItemPx + centerOffsetPx
                    
                    this.scaleX = finalScaleX
                    this.scaleY = finalScaleY
                    shadowElevation = 0f
                }
                .size(indicatorWidth, indicatorHeight)
                .clip(shape)
                .run {
                    if (isLiquidGlassEnabled && backdrop != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        // [Effect] Strong refraction for the indicator (Magnifying Glass effect)
                        this.drawBackdrop(
                            backdrop = backdrop,
                            shape = { shape },
                            effects = {
                                lens(
                                    refractionHeight = 40f, // Stronger bulge
                                    refractionAmount = 30f, // Strong distortion
                                    depthEffect = true,
                                    chromaticAberration = true // Always aberrate for "magic" feel
                                )
                            },
                            onDrawSurface = {
                                // Subtle tint for the indicator
                                drawRect(color.copy(alpha = 0.15f))
                            }
                        )
                    } else {
                        // Fallback
                         this.background(color)
                    }
                }
        )
    }
}


/**
 * 简化版液态指示器（不依赖 Backdrop）
 * 
 * 使用标准 Compose 动画实现类似效果
 */
/**
 * 简化版液态指示器（适用于 TabRow 等变长场景）
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
    
    // [Optimized] Use graphicsLayer for translation to avoid Layout phases
    // We calculate the pixel offset directly in the drawing phase
    val itemWidthPx = with(density) { itemWidth.toPx() }
    
    val scale by animateFloatAsState(
        targetValue = if (isDragging) 1.05f else 1f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 400f),
        label = "scale"
    )
    
    // [Updated] Match BottomBar style: Primary color with alpha
    val indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
    
    Box(
        modifier = modifier
            // [Optimized] Replaced offset with graphicsLayer translation
            .graphicsLayer {
                val currentPosition = positionState.value
                translationX = currentPosition * itemWidthPx
                
                this.scaleX = scale
                this.scaleY = scale
            }
            .size(indicatorWidth, indicatorHeight)
            .clip(RoundedCornerShape(18.dp)) // Half of 36dp
            .background(indicatorColor)
    )
}
