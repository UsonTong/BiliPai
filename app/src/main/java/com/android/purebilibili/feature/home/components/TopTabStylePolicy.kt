package com.android.purebilibili.feature.home.components

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.purebilibili.core.theme.UiPreset

enum class TopTabMaterialMode {
    PLAIN,
    BLUR,
    LIQUID_GLASS
}

enum class TopTabIndicatorStyle {
    CAPSULE,
    MATERIAL
}

data class TopTabVisualTuning(
    val nonFloatingIndicatorHeightDp: Float = 31f,
    val nonFloatingIndicatorCornerDp: Float = 14f,
    val nonFloatingIndicatorWidthRatio: Float = 0.72f,
    val nonFloatingIndicatorMinWidthDp: Float = 44f,
    val nonFloatingIndicatorHorizontalInsetDp: Float = 18f,
    val floatingIndicatorWidthMultiplier: Float = 1.34f,
    val floatingIndicatorMinWidthDp: Float = 96f,
    val floatingIndicatorMaxWidthDp: Float = 126f,
    val floatingIndicatorMaxWidthToItemRatio: Float = 1.34f,
    val floatingIndicatorHeightDp: Float = 50f,
    val tabTextSizeSp: Float = 11.6f,
    val tabTextLineHeightSp: Float = 12f,
    val tabContentMinHeightDp: Float = 34f
)

data class TopTabVisualState(
    val floating: Boolean,
    val materialMode: TopTabMaterialMode
)

data class Md3TopTabVisualSpec(
    val rowHeight: Dp,
    val selectedCapsuleHeight: Dp,
    val selectedCapsuleCornerRadius: Dp,
    val selectedCapsuleTonalElevation: Dp,
    val selectedCapsuleShadowElevation: Dp,
    val itemHorizontalPadding: Dp,
    val iconSize: Dp,
    val labelTextSize: TextUnit,
    val labelLineHeight: TextUnit,
    val iconLabelSpacing: Dp
)

fun resolveTopTabVisualTuning(): TopTabVisualTuning = TopTabVisualTuning()

internal fun resolveMd3TopTabVisualSpec(isFloatingStyle: Boolean): Md3TopTabVisualSpec {
    return if (isFloatingStyle) {
        Md3TopTabVisualSpec(
            rowHeight = 48.dp,
            selectedCapsuleHeight = 2.dp,
            selectedCapsuleCornerRadius = 1.dp,
            selectedCapsuleTonalElevation = 0.dp,
            selectedCapsuleShadowElevation = 0.dp,
            itemHorizontalPadding = 14.dp,
            iconSize = 18.dp,
            labelTextSize = 13.sp,
            labelLineHeight = 18.sp,
            iconLabelSpacing = 0.dp
        )
    } else {
        Md3TopTabVisualSpec(
            rowHeight = 44.dp,
            selectedCapsuleHeight = 2.dp,
            selectedCapsuleCornerRadius = 1.dp,
            selectedCapsuleTonalElevation = 0.dp,
            selectedCapsuleShadowElevation = 0.dp,
            itemHorizontalPadding = 12.dp,
            iconSize = 16.dp,
            labelTextSize = 13.sp,
            labelLineHeight = 18.sp,
            iconLabelSpacing = 0.dp
        )
    }
}

internal fun resolveMd3TopTabSelectedContainerColor(
    colorScheme: ColorScheme
): androidx.compose.ui.graphics.Color = colorScheme.primary

internal fun resolveMd3TopTabSelectedIconColor(
    colorScheme: ColorScheme
): androidx.compose.ui.graphics.Color = colorScheme.primary

internal fun resolveMd3TopTabSelectedLabelColor(
    colorScheme: ColorScheme
): androidx.compose.ui.graphics.Color = colorScheme.primary

internal fun resolveMd3TopTabUnselectedIconColor(
    colorScheme: ColorScheme
): androidx.compose.ui.graphics.Color = colorScheme.onSurfaceVariant

internal fun resolveMd3TopTabUnselectedLabelColor(
    colorScheme: ColorScheme
): androidx.compose.ui.graphics.Color = colorScheme.onSurfaceVariant

internal fun resolveMd3TopTabIconTint(
    selectionFraction: Float,
    colorScheme: ColorScheme
) = androidx.compose.ui.graphics.lerp(
    resolveMd3TopTabUnselectedIconColor(colorScheme),
    resolveMd3TopTabSelectedIconColor(colorScheme),
    selectionFraction.coerceIn(0f, 1f)
)

internal fun resolveMd3TopTabLabelTint(
    selectionFraction: Float,
    colorScheme: ColorScheme
) = androidx.compose.ui.graphics.lerp(
    resolveMd3TopTabUnselectedLabelColor(colorScheme),
    resolveMd3TopTabSelectedLabelColor(colorScheme),
    selectionFraction.coerceIn(0f, 1f)
)

internal fun resolveTopTabIndicatorStyle(uiPreset: UiPreset): TopTabIndicatorStyle {
    return if (uiPreset == UiPreset.MD3) {
        TopTabIndicatorStyle.MATERIAL
    } else {
        TopTabIndicatorStyle.CAPSULE
    }
}

fun resolveTopTabLabelTextSizeSp(labelMode: Int): Float {
    val tuning = resolveTopTabVisualTuning()
    return when (normalizeTopTabLabelMode(labelMode)) {
        0 -> resolveMd3TopTabVisualSpec(isFloatingStyle = false).labelTextSize.value
        2 -> tuning.tabTextSizeSp + 0.2f
        else -> tuning.tabTextSizeSp
    }
}

fun resolveTopTabLabelLineHeightSp(labelMode: Int): Float {
    return when (normalizeTopTabLabelMode(labelMode)) {
        0 -> resolveMd3TopTabVisualSpec(isFloatingStyle = false).labelLineHeight.value
        else -> {
            val tuning = resolveTopTabVisualTuning()
            val textSize = resolveTopTabLabelTextSizeSp(labelMode)
            maxOf(tuning.tabTextLineHeightSp, textSize)
        }
    }
}

fun resolveTopTabContentMinHeightDp(): Float {
    return resolveTopTabVisualTuning().tabContentMinHeightDp
}

fun resolveTopTabStyle(
    isBottomBarFloating: Boolean,
    isBottomBarBlurEnabled: Boolean,
    isLiquidGlassEnabled: Boolean
): TopTabVisualState {
    val materialMode = when {
        isBottomBarFloating && isLiquidGlassEnabled -> TopTabMaterialMode.LIQUID_GLASS
        isBottomBarBlurEnabled -> TopTabMaterialMode.BLUR
        else -> TopTabMaterialMode.PLAIN
    }

    return TopTabVisualState(
        floating = isBottomBarFloating,
        materialMode = materialMode
    )
}

internal fun resolveEffectiveHomeHeaderTabMaterialMode(
    materialMode: TopTabMaterialMode,
    interactionBudget: HomeInteractionMotionBudget
): TopTabMaterialMode {
    return materialMode
}

internal fun resolveEffectiveTopTabLiquidGlassEnabled(
    isLiquidGlassEnabled: Boolean,
    interactionBudget: HomeInteractionMotionBudget
): Boolean {
    return isLiquidGlassEnabled
}
