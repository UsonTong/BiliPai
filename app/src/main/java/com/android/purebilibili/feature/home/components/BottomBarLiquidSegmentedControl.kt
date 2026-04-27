package com.android.purebilibili.feature.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.purebilibili.core.store.LiquidGlassStyle
import com.android.purebilibili.core.store.SettingsManager
import com.android.purebilibili.core.ui.animation.horizontalDragGesture
import com.android.purebilibili.core.ui.animation.rememberDampedDragAnimationState
import com.android.purebilibili.core.ui.blur.currentUnifiedBlurIntensity
import com.android.purebilibili.core.ui.motion.BottomBarMotionProfile
import com.android.purebilibili.core.ui.motion.resolveBottomBarMotionSpec
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberCombinedBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.InnerShadow
import com.kyant.backdrop.shadow.Shadow
import kotlin.math.abs
import kotlin.math.sign

@Composable
fun BottomBarLiquidSegmentedControl(
    items: List<String>,
    selectedIndex: Int,
    onSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    itemWidth: Dp? = null,
    height: Dp = 42.dp,
    indicatorHeight: Dp = 34.dp,
    labelFontSize: TextUnit = 14.sp,
    containerHorizontalPadding: Dp = 3.dp,
    containerVerticalPadding: Dp = 3.dp
) {
    if (items.isEmpty()) return

    val context = LocalContext.current
    val liquidGlassEnabled by SettingsManager
        .getBottomBarLiquidGlassEnabled(context)
        .collectAsState(initial = true)
    val liquidGlassStyle by SettingsManager
        .getLiquidGlassStyle(context)
        .collectAsState(initial = LiquidGlassStyle.SUKISU)
    val blurIntensity = currentUnifiedBlurIntensity()
    val density = LocalDensity.current
    val itemCount = items.size
    val safeSelectedIndex = selectedIndex.coerceIn(0, itemCount - 1)
    val motionSpec = remember {
        resolveBottomBarMotionSpec(profile = BottomBarMotionProfile.ANDROID_NATIVE_FLOATING)
    }
    val dragState = rememberDampedDragAnimationState(
        initialIndex = safeSelectedIndex,
        itemCount = itemCount,
        motionSpec = motionSpec,
        onIndexChanged = { index ->
            if (enabled && index in items.indices) {
                onSelected(index)
            }
        }
    )
    val liquidGlassTuning = remember(liquidGlassStyle) {
        resolveLiquidGlassTuning(liquidGlassStyle)
    }
    val containerShape = RoundedCornerShape(height / 2)
    val indicatorCorner = indicatorHeight / 2
    val indicatorShape = RoundedCornerShape(indicatorCorner)
    val isDarkTheme = isSystemInDarkTheme()
    val surfaceColor = MaterialTheme.colorScheme.surface
    val isLightMode = surfaceColor.luminance() > 0.5f
    val containerColor = resolveBottomBarContainerColor(
        surfaceColor = surfaceColor,
        blurEnabled = liquidGlassEnabled,
        blurIntensity = blurIntensity,
        liquidGlassProgress = liquidGlassTuning.progress,
        isGlassEffectEnabled = liquidGlassEnabled
    )
    val selectedTextColor = MaterialTheme.colorScheme.primary
    val unselectedTextColor = resolveBottomBarReadableContentColor(
        isLightMode = isLightMode,
        liquidGlassProgress = liquidGlassTuning.progress,
        contentLuminance = if (liquidGlassEnabled && isDarkTheme) 0.18f else 0f
    ).copy(alpha = if (enabled) 0.78f else 0.42f)
    val shellBackdrop = rememberLayerBackdrop()

    LaunchedEffect(safeSelectedIndex) {
        dragState.updateIndex(safeSelectedIndex)
    }

    BoxWithConstraints(
        modifier = modifier
            .then(
                if (itemWidth != null) {
                    Modifier.width((itemWidth.value * itemCount).dp + containerHorizontalPadding * 2)
                } else {
                    Modifier.fillMaxWidth()
                }
            )
            .height(height)
            .clip(containerShape)
            .background(containerColor)
            .border(
                width = 0.6.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(
                    alpha = if (liquidGlassEnabled) 0.34f else 0.22f
                ),
                shape = containerShape
            )
            .padding(horizontal = containerHorizontalPadding, vertical = containerVerticalPadding)
    ) {
        val segmentWidth = maxWidth / itemCount
        val itemWidthPx = with(density) { segmentWidth.toPx() }
        val dragModifier = if (enabled && itemCount > 1) {
            Modifier.horizontalDragGesture(
                dragState = dragState,
                itemWidthPx = itemWidthPx
            )
        } else {
            Modifier
        }
        val indicatorPosition = dragState.value
        val pressMotionProgress by remember {
            derivedStateOf { dragState.pressProgress }
        }
        val refractionMotionProfile = resolveBottomBarRefractionMotionProfile(
            position = indicatorPosition,
            velocity = dragState.velocityPxPerSecond,
            isDragging = dragState.isDragging,
            motionSpec = motionSpec
        )
        val motionProgress = maxOf(pressMotionProgress, refractionMotionProfile.progress)
        val contentBackdrop = rememberLayerBackdrop()
        val combinedIndicatorBackdrop = rememberCombinedBackdrop(shellBackdrop, contentBackdrop)
        val shouldRefractContent = dragState.isDragging
        val indicatorEffectProgress = if (shouldRefractContent) motionProgress else 0f
        val panelOffsetPx by remember(density, itemWidthPx) {
            derivedStateOf {
                val fraction = (dragState.dragOffset / itemWidthPx).coerceIn(-1f, 1f)
                with(density) {
                    4.dp.toPx() * fraction.sign * androidx.compose.animation.core.EaseOut.transform(abs(fraction))
                }
            }
        }

        Box(
            modifier = Modifier
                .matchParentSize()
                .layerBackdrop(shellBackdrop)
                .background(containerColor)
        )

        BottomBarLiquidSegmentedLabels(
            items = items,
            selectedIndex = safeSelectedIndex,
            indicatorPosition = indicatorPosition,
            motionProgress = motionProgress,
            selectionEmphasis = refractionMotionProfile.visibleSelectionEmphasis,
            selectedTextColor = selectedTextColor,
            unselectedTextColor = unselectedTextColor,
            enabled = enabled,
            labelFontSize = labelFontSize,
            indicatorCorner = indicatorCorner,
            onSelected = onSelected,
            interactive = false,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { translationX = panelOffsetPx }
        )

        Box(
            modifier = Modifier
                .matchParentSize()
                .clearAndSetSemantics {}
                .alpha(0f)
                .layerBackdrop(contentBackdrop)
                .graphicsLayer { translationX = panelOffsetPx }
        ) {
            BottomBarLiquidSegmentedLabels(
                items = items,
                selectedIndex = safeSelectedIndex,
                indicatorPosition = indicatorPosition,
                motionProgress = motionProgress,
                selectionEmphasis = refractionMotionProfile.exportSelectionEmphasis,
                selectedTextColor = selectedTextColor,
                unselectedTextColor = selectedTextColor,
                enabled = enabled,
                labelFontSize = labelFontSize,
                indicatorCorner = indicatorCorner,
                onSelected = onSelected,
                interactive = false,
                modifier = Modifier.fillMaxSize()
            )
        }

        Box(
            modifier = Modifier
                .offset(x = segmentWidth * dragState.value)
                .graphicsLayer {
                    translationX = panelOffsetPx
                }
                .width(segmentWidth)
                .height(indicatorHeight)
                .align(Alignment.CenterStart)
                .run {
                    if (liquidGlassEnabled) {
                        drawBackdrop(
                            backdrop = if (shouldRefractContent) {
                                combinedIndicatorBackdrop
                            } else {
                                shellBackdrop
                            },
                            shape = { indicatorShape },
                            effects = {
                                lens(
                                    refractionHeight = 12.dp.toPx() *
                                        indicatorEffectProgress *
                                        refractionMotionProfile.indicatorLensHeightScale,
                                    refractionAmount = 18.dp.toPx() *
                                        indicatorEffectProgress *
                                        refractionMotionProfile.indicatorLensAmountScale,
                                    depthEffect = true,
                                    chromaticAberration = shouldRefractContent
                                )
                            },
                            highlight = {
                                Highlight.Default.copy(alpha = indicatorEffectProgress)
                            },
                            shadow = {
                                Shadow(alpha = indicatorEffectProgress)
                            },
                            innerShadow = {
                                InnerShadow(
                                    radius = 8.dp * indicatorEffectProgress,
                                    alpha = indicatorEffectProgress
                                )
                            },
                            layerBlock = {
                                val indicatorScale = androidx.compose.ui.util.lerp(
                                    1f,
                                    78f / 56f,
                                    indicatorEffectProgress
                                )
                                val velocity = if (shouldRefractContent) {
                                    dragState.velocity / 10f
                                } else {
                                    0f
                                }
                                scaleX = indicatorScale / (
                                    1f - (
                                        velocity * 0.75f
                                    ).coerceIn(-0.2f, 0.2f)
                                )
                                scaleY = indicatorScale * (
                                    1f - (
                                        velocity * 0.25f
                                    ).coerceIn(-0.2f, 0.2f)
                                )
                            },
                            onDrawSurface = {
                                drawRect(
                                    color = if (isDarkTheme) {
                                        Color.White.copy(0.1f)
                                    } else {
                                        Color.Black.copy(0.1f)
                                    },
                                    alpha = 1f - indicatorEffectProgress
                                )
                                drawRect(Color.Black.copy(alpha = 0.03f * indicatorEffectProgress))
                            }
                        )
                    } else {
                        background(
                            if (isDarkTheme) Color.White.copy(0.1f) else Color.Black.copy(0.1f),
                            indicatorShape
                        )
                    }
                }
        )

        BottomBarLiquidSegmentedLabels(
            items = items,
            selectedIndex = safeSelectedIndex,
            indicatorPosition = indicatorPosition,
            motionProgress = motionProgress,
            selectionEmphasis = refractionMotionProfile.visibleSelectionEmphasis,
            selectedTextColor = selectedTextColor,
            unselectedTextColor = unselectedTextColor,
            enabled = enabled,
            labelFontSize = labelFontSize,
            indicatorCorner = indicatorCorner,
            onSelected = onSelected,
            interactive = true,
            modifier = Modifier
                .fillMaxSize()
                .alpha(0f)
                .graphicsLayer { translationX = panelOffsetPx }
                .then(dragModifier)
        )
    }
}

@Composable
private fun BottomBarLiquidSegmentedLabels(
    items: List<String>,
    selectedIndex: Int,
    indicatorPosition: Float,
    motionProgress: Float,
    selectionEmphasis: Float,
    selectedTextColor: Color,
    unselectedTextColor: Color,
    enabled: Boolean,
    labelFontSize: TextUnit,
    indicatorCorner: Dp,
    onSelected: (Int) -> Unit,
    interactive: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        items.forEachIndexed { index, label ->
            val visual = resolveBottomBarItemMotionVisual(
                itemIndex = index,
                indicatorPosition = indicatorPosition,
                currentSelectedIndex = selectedIndex,
                motionProgress = motionProgress,
                selectionEmphasis = selectionEmphasis
            )
            val textColor = if (enabled) {
                androidx.compose.ui.graphics.lerp(
                    start = unselectedTextColor,
                    stop = selectedTextColor,
                    fraction = visual.themeWeight
                )
            } else {
                unselectedTextColor.copy(alpha = 0.44f)
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(indicatorCorner))
                    .then(
                        if (interactive) {
                            Modifier.clickable(
                                enabled = enabled,
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                onSelected(index)
                            }
                        } else {
                            Modifier
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    color = textColor,
                    fontSize = labelFontSize,
                    fontWeight = if (visual.themeWeight > 0.5f) {
                        FontWeight.SemiBold
                    } else {
                        FontWeight.Medium
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.graphicsLayer {
                        scaleX = 1f
                        scaleY = 1f
                    }
                )
            }
        }
    }
}
