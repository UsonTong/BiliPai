package com.android.purebilibili.feature.home

import com.android.purebilibili.core.store.resolveEffectiveLiquidGlassEnabled
import com.android.purebilibili.core.theme.UiPreset

internal data class HomePerformanceConfig(
    val headerBlurEnabled: Boolean,
    val bottomBarBlurEnabled: Boolean,
    val topBarLiquidGlassEnabled: Boolean,
    val bottomBarLiquidGlassEnabled: Boolean,
    val cardAnimationEnabled: Boolean,
    val cardTransitionEnabled: Boolean,
    val isDataSaverActive: Boolean,
    val preloadAheadCount: Int
) {
    val isAnyLiquidGlassEnabled: Boolean
        get() = topBarLiquidGlassEnabled || bottomBarLiquidGlassEnabled
}

internal fun resolveHomePreloadAheadCount(
    isDataSaverActive: Boolean,
    normalPreloadAheadCount: Int
): Int {
    if (isDataSaverActive) return 0
    return normalPreloadAheadCount.coerceAtLeast(0).coerceAtMost(3)
}

internal fun resolveHomePerformanceConfig(
    uiPreset: UiPreset = UiPreset.IOS,
    headerBlurEnabled: Boolean,
    bottomBarBlurEnabled: Boolean,
    topBarLiquidGlassEnabled: Boolean,
    bottomBarLiquidGlassEnabled: Boolean,
    androidNativeLiquidGlassEnabled: Boolean = false,
    cardAnimationEnabled: Boolean,
    cardTransitionEnabled: Boolean,
    isDataSaverActive: Boolean,
    smartVisualGuardEnabled: Boolean,
    normalPreloadAheadCount: Int = 5
): HomePerformanceConfig {
    // Feature retired: keep parameter for compatibility, but never apply runtime smoothness downgrade.
    val shouldPrioritizeSmoothness = false
    val effectiveDataSaver = isDataSaverActive
    val effectiveTopBarLiquidGlass = resolveEffectiveLiquidGlassEnabled(
        requestedEnabled = topBarLiquidGlassEnabled,
        uiPreset = uiPreset,
        androidNativeLiquidGlassEnabled = androidNativeLiquidGlassEnabled
    ) && !shouldPrioritizeSmoothness
    val effectiveBottomBarLiquidGlass = resolveEffectiveLiquidGlassEnabled(
        requestedEnabled = bottomBarLiquidGlassEnabled,
        uiPreset = uiPreset,
        androidNativeLiquidGlassEnabled = androidNativeLiquidGlassEnabled
    ) && !shouldPrioritizeSmoothness
    val effectivePreloadAheadCount = when {
        shouldPrioritizeSmoothness -> normalPreloadAheadCount.coerceAtLeast(0).coerceAtMost(2)
        else -> resolveHomePreloadAheadCount(
            isDataSaverActive = effectiveDataSaver,
            normalPreloadAheadCount = normalPreloadAheadCount
        )
    }

    return HomePerformanceConfig(
        headerBlurEnabled = headerBlurEnabled,
        bottomBarBlurEnabled = bottomBarBlurEnabled,
        topBarLiquidGlassEnabled = effectiveTopBarLiquidGlass,
        bottomBarLiquidGlassEnabled = effectiveBottomBarLiquidGlass,
        cardAnimationEnabled = cardAnimationEnabled,
        cardTransitionEnabled = cardTransitionEnabled,
        isDataSaverActive = effectiveDataSaver,
        preloadAheadCount = effectivePreloadAheadCount
    )
}
