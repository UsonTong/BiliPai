package com.android.purebilibili.feature.home.components

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.purebilibili.core.theme.AndroidNativeVariant
import com.android.purebilibili.core.theme.UiPreset
import com.android.purebilibili.core.theme.isMaterial3ExpressiveVariant

enum class TopTabMaterialMode {
    PLAIN,
    BLUR,
    LIQUID_GLASS
}

enum class TopTabIndicatorStyle {
    CAPSULE,
    MATERIAL
}

internal const val CompactTopTabIndicatorHeightDp = 40f
internal const val CompactTopTabIndicatorCornerDp = CompactTopTabIndicatorHeightDp / 2f

data class TopTabVisualTuning(
    val nonFloatingIndicatorHeightDp: Float = CompactTopTabIndicatorHeightDp,
    val nonFloatingIndicatorCornerDp: Float = CompactTopTabIndicatorCornerDp,
    val nonFloatingIndicatorWidthRatio: Float = 0.72f,
    val nonFloatingIndicatorMinWidthDp: Float = 44f,
    val nonFloatingIndicatorHorizontalInsetDp: Float = 18f,
    val floatingIndicatorWidthMultiplier: Float = 1.18f,
    val floatingIndicatorMinWidthDp: Float = 82f,
    val floatingIndicatorMaxWidthDp: Float = 112f,
    val floatingIndicatorMaxWidthToItemRatio: Float = 1.18f,
    val floatingIndicatorHeightDp: Float = CompactTopTabIndicatorHeightDp,
    val tabTextSizeSp: Float = 13f,
    val tabTextLineHeightSp: Float = 17f,
    val tabContentMinHeightDp: Float = 36f,
    val tabIconWithTextSizeDp: Float = 18f,
    val tabIconOnlySizeDp: Float = 22f,
    val tabIconTextSpacingDp: Float = 2f
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

fun resolveTopTabVisualTuning(uiPreset: UiPreset): TopTabVisualTuning {
    return when (uiPreset) {
        UiPreset.IOS -> TopTabVisualTuning(
            nonFloatingIndicatorHeightDp = CompactTopTabIndicatorHeightDp,
            nonFloatingIndicatorCornerDp = CompactTopTabIndicatorCornerDp,
            nonFloatingIndicatorWidthRatio = 1.18f,
            nonFloatingIndicatorMinWidthDp = 78f,
            nonFloatingIndicatorHorizontalInsetDp = 0f,
            floatingIndicatorWidthMultiplier = 1.18f,
            floatingIndicatorMinWidthDp = 82f,
            floatingIndicatorMaxWidthDp = 112f,
            floatingIndicatorMaxWidthToItemRatio = 1.18f,
            floatingIndicatorHeightDp = CompactTopTabIndicatorHeightDp,
            tabTextSizeSp = 13f,
            tabTextLineHeightSp = 17f,
            tabContentMinHeightDp = 36f,
            tabIconWithTextSizeDp = 18f,
            tabIconOnlySizeDp = 22f,
            tabIconTextSpacingDp = 2f
        )
        UiPreset.MD3 -> resolveTopTabVisualTuning()
    }
}

internal fun resolveTopTabContentScale(
    selectionFraction: Float,
    showIcon: Boolean,
    showText: Boolean,
    uiPreset: UiPreset
): Float {
    if (showIcon && showText) return 1f

    val clampedFraction = selectionFraction.coerceIn(0f, 1f)
    val maxScale = when (uiPreset) {
        UiPreset.IOS -> 1.03f
        UiPreset.MD3 -> 1.04f
    }
    return 1f + ((maxScale - 1f) * clampedFraction)
}

internal fun resolveMd3TopTabVisualSpec(
    isFloatingStyle: Boolean,
    androidNativeVariant: AndroidNativeVariant = AndroidNativeVariant.MATERIAL3,
    labelMode: Int = 2
): Md3TopTabVisualSpec {
    val normalizedLabelMode = normalizeTopTabLabelMode(labelMode)
    val showIconAndText = normalizedLabelMode == 0
    if (isMaterial3ExpressiveVariant(UiPreset.MD3, androidNativeVariant)) {
        return if (isFloatingStyle) {
            Md3TopTabVisualSpec(
                rowHeight = if (showIconAndText) 62.dp else 56.dp,
                selectedCapsuleHeight = 36.dp,
                selectedCapsuleCornerRadius = 18.dp,
                selectedCapsuleTonalElevation = 2.dp,
                selectedCapsuleShadowElevation = 0.dp,
                itemHorizontalPadding = if (showIconAndText) 10.dp else 14.dp,
                iconSize = 22.dp,
                labelTextSize = if (showIconAndText) 14.sp else 15.sp,
                labelLineHeight = if (showIconAndText) 18.sp else 20.sp,
                iconLabelSpacing = if (showIconAndText) 3.dp else 0.dp
            )
        } else {
            Md3TopTabVisualSpec(
                rowHeight = if (showIconAndText) 60.dp else 52.dp,
                selectedCapsuleHeight = 34.dp,
                selectedCapsuleCornerRadius = 17.dp,
                selectedCapsuleTonalElevation = 2.dp,
                selectedCapsuleShadowElevation = 0.dp,
                itemHorizontalPadding = if (showIconAndText) 10.dp else 12.dp,
                iconSize = 21.dp,
                labelTextSize = if (showIconAndText) 14.sp else 15.sp,
                labelLineHeight = if (showIconAndText) 18.sp else 20.sp,
                iconLabelSpacing = if (showIconAndText) 3.dp else 0.dp
            )
        }
    }
    if (androidNativeVariant == AndroidNativeVariant.MIUIX) {
        return if (isFloatingStyle) {
            Md3TopTabVisualSpec(
                rowHeight = if (showIconAndText) 60.dp else 54.dp,
                selectedCapsuleHeight = 30.dp,
                selectedCapsuleCornerRadius = 15.dp,
                selectedCapsuleTonalElevation = 0.dp,
                selectedCapsuleShadowElevation = 0.dp,
                itemHorizontalPadding = 12.dp,
                iconSize = 22.dp,
                labelTextSize = 14.sp,
                labelLineHeight = 18.sp,
                iconLabelSpacing = 3.dp
            )
        } else {
            Md3TopTabVisualSpec(
                rowHeight = if (showIconAndText) 56.dp else 48.dp,
                selectedCapsuleHeight = 30.dp,
                selectedCapsuleCornerRadius = 15.dp,
                selectedCapsuleTonalElevation = 0.dp,
                selectedCapsuleShadowElevation = 0.dp,
                itemHorizontalPadding = 12.dp,
                iconSize = 20.dp,
                labelTextSize = 15.sp,
                labelLineHeight = 20.sp,
                iconLabelSpacing = 2.dp
            )
        }
    }

    return if (isFloatingStyle) {
        Md3TopTabVisualSpec(
            rowHeight = if (showIconAndText) 62.dp else 52.dp,
            selectedCapsuleHeight = 2.dp,
            selectedCapsuleCornerRadius = 1.dp,
            selectedCapsuleTonalElevation = 0.dp,
            selectedCapsuleShadowElevation = 0.dp,
            itemHorizontalPadding = if (showIconAndText) 8.dp else 14.dp,
            iconSize = 22.dp,
            labelTextSize = if (showIconAndText) 14.sp else 15.sp,
            labelLineHeight = if (showIconAndText) 18.sp else 20.sp,
            iconLabelSpacing = if (showIconAndText) 3.dp else 0.dp
        )
    } else {
        Md3TopTabVisualSpec(
            rowHeight = if (showIconAndText) 60.dp else 48.dp,
            selectedCapsuleHeight = 2.dp,
            selectedCapsuleCornerRadius = 1.dp,
            selectedCapsuleTonalElevation = 0.dp,
            selectedCapsuleShadowElevation = 0.dp,
            itemHorizontalPadding = if (showIconAndText) 8.dp else 12.dp,
            iconSize = 20.dp,
            labelTextSize = if (showIconAndText) 14.sp else 15.sp,
            labelLineHeight = if (showIconAndText) 18.sp else 20.sp,
            iconLabelSpacing = if (showIconAndText) 3.dp else 0.dp
        )
    }
}

internal fun resolveMd3TopTabSelectedContainerColor(
    colorScheme: ColorScheme,
    androidNativeVariant: AndroidNativeVariant = AndroidNativeVariant.MATERIAL3
): androidx.compose.ui.graphics.Color = when {
    isMaterial3ExpressiveVariant(UiPreset.MD3, androidNativeVariant) -> colorScheme.primaryContainer
    androidNativeVariant == AndroidNativeVariant.MIUIX -> colorScheme.secondaryContainer
    else -> colorScheme.primary
}

internal fun resolveMd3TopTabSelectedIconColor(
    colorScheme: ColorScheme,
    androidNativeVariant: AndroidNativeVariant = AndroidNativeVariant.MATERIAL3
): androidx.compose.ui.graphics.Color = when {
    isMaterial3ExpressiveVariant(UiPreset.MD3, androidNativeVariant) -> colorScheme.onPrimaryContainer
    androidNativeVariant == AndroidNativeVariant.MIUIX -> colorScheme.onSecondaryContainer
    else -> colorScheme.primary
}

internal fun resolveMd3TopTabSelectedLabelColor(
    colorScheme: ColorScheme,
    androidNativeVariant: AndroidNativeVariant = AndroidNativeVariant.MATERIAL3
): androidx.compose.ui.graphics.Color = when {
    isMaterial3ExpressiveVariant(UiPreset.MD3, androidNativeVariant) -> colorScheme.onPrimaryContainer
    androidNativeVariant == AndroidNativeVariant.MIUIX -> colorScheme.onSecondaryContainer
    else -> colorScheme.primary
}

internal fun resolveMd3TopTabUnselectedIconColor(
    colorScheme: ColorScheme
): androidx.compose.ui.graphics.Color = colorScheme.onSurfaceVariant

internal fun resolveMd3TopTabUnselectedLabelColor(
    colorScheme: ColorScheme
): androidx.compose.ui.graphics.Color = colorScheme.onSurfaceVariant

internal fun resolveMd3TopTabIconTint(
    selectionFraction: Float,
    colorScheme: ColorScheme,
    androidNativeVariant: AndroidNativeVariant = AndroidNativeVariant.MATERIAL3
) = androidx.compose.ui.graphics.lerp(
    resolveMd3TopTabUnselectedIconColor(colorScheme),
    resolveMd3TopTabSelectedIconColor(colorScheme, androidNativeVariant),
    selectionFraction.coerceIn(0f, 1f)
)

internal fun resolveMd3TopTabLabelTint(
    selectionFraction: Float,
    colorScheme: ColorScheme,
    androidNativeVariant: AndroidNativeVariant = AndroidNativeVariant.MATERIAL3
) = androidx.compose.ui.graphics.lerp(
    resolveMd3TopTabUnselectedLabelColor(colorScheme),
    resolveMd3TopTabSelectedLabelColor(colorScheme, androidNativeVariant),
    selectionFraction.coerceIn(0f, 1f)
)

internal fun resolveTopTabIndicatorStyle(uiPreset: UiPreset): TopTabIndicatorStyle {
    return if (uiPreset == UiPreset.MD3) {
        TopTabIndicatorStyle.MATERIAL
    } else {
        TopTabIndicatorStyle.CAPSULE
    }
}

internal fun shouldUseMd3TopTabMaterialIndicator(
    uiPreset: UiPreset,
    liquidGlassEnabled: Boolean
): Boolean {
    return resolveTopTabIndicatorStyle(uiPreset) == TopTabIndicatorStyle.MATERIAL &&
        !liquidGlassEnabled
}

fun resolveTopTabLabelTextSizeSp(labelMode: Int): Float {
    val tuning = resolveTopTabVisualTuning()
    return when (normalizeTopTabLabelMode(labelMode)) {
        0 -> resolveMd3TopTabVisualSpec(isFloatingStyle = false, labelMode = labelMode).labelTextSize.value
        2 -> tuning.tabTextSizeSp
        else -> tuning.tabTextSizeSp
    }
}

fun resolveTopTabLabelLineHeightSp(labelMode: Int): Float {
    return when (normalizeTopTabLabelMode(labelMode)) {
        0 -> resolveMd3TopTabVisualSpec(isFloatingStyle = false, labelMode = labelMode).labelLineHeight.value
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

fun resolveTopTabIconSizeDp(labelMode: Int): Float {
    val tuning = resolveTopTabVisualTuning()
    return when (normalizeTopTabLabelMode(labelMode)) {
        0 -> tuning.tabIconWithTextSizeDp
        1 -> tuning.tabIconOnlySizeDp
        else -> 0f
    }
}

fun resolveTopTabIconTextSpacingDp(labelMode: Int): Float {
    return if (normalizeTopTabLabelMode(labelMode) == 0) {
        resolveTopTabVisualTuning().tabIconTextSpacingDp
    } else {
        0f
    }
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
