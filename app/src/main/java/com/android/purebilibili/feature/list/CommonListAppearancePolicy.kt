package com.android.purebilibili.feature.list

import com.android.purebilibili.core.store.HomeSettings
import com.android.purebilibili.core.store.resolveEffectiveLiquidGlassEnabled
import com.android.purebilibili.core.store.resolveHomeHeaderBlurEnabled
import com.android.purebilibili.core.theme.UiPreset

internal data class CommonListVideoCardAppearance(
    val glassEnabled: Boolean,
    val blurEnabled: Boolean,
    val showCoverGlassBadges: Boolean,
    val showInfoGlassBadges: Boolean
)

internal fun resolveCommonListHeaderBlurEnabled(
    homeSettings: HomeSettings,
    uiPreset: UiPreset
): Boolean {
    return resolveHomeHeaderBlurEnabled(
        mode = homeSettings.headerBlurMode,
        uiPreset = uiPreset
    )
}

internal fun resolveCommonListVideoCardAppearance(
    homeSettings: HomeSettings,
    uiPreset: UiPreset
): CommonListVideoCardAppearance {
    val headerBlurEnabled = resolveCommonListHeaderBlurEnabled(
        homeSettings = homeSettings,
        uiPreset = uiPreset
    )
    return CommonListVideoCardAppearance(
        glassEnabled = resolveEffectiveLiquidGlassEnabled(
            requestedEnabled = homeSettings.isLiquidGlassEnabled,
            uiPreset = uiPreset
        ),
        blurEnabled = headerBlurEnabled || homeSettings.isBottomBarBlurEnabled,
        showCoverGlassBadges = homeSettings.showHomeCoverGlassBadges,
        showInfoGlassBadges = homeSettings.showHomeInfoGlassBadges
    )
}
