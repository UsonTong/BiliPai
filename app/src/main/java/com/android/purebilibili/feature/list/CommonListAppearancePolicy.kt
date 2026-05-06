package com.android.purebilibili.feature.list

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.android.purebilibili.core.store.HomeSettings
import com.android.purebilibili.core.store.resolveEffectiveLiquidGlassEnabled
import com.android.purebilibili.core.store.resolveHomeHeaderBlurEnabled
import com.android.purebilibili.core.theme.AndroidNativeVariant
import com.android.purebilibili.core.theme.UiPreset
import com.android.purebilibili.core.theme.isMaterial3ExpressiveVariant

internal data class CommonListVideoCardAppearance(
    val glassEnabled: Boolean,
    val blurEnabled: Boolean,
    val showCoverGlassBadges: Boolean,
    val showInfoGlassBadges: Boolean
)

internal data class CommonListFavoriteHeaderLayout(
    val searchBarHeightDp: Int,
    val searchBarHorizontalPaddingDp: Int,
    val searchBarVerticalPaddingDp: Int,
    val browseToggleHeightDp: Int,
    val browseToggleIndicatorHeightDp: Int,
    val browseToggleLabelFontSizeSp: Int,
    val browseToggleHorizontalPaddingDp: Int,
    val browseToggleTopPaddingDp: Int,
    val folderChipMinHeightDp: Int,
    val folderChipHorizontalPaddingDp: Int,
    val folderChipRowHorizontalPaddingDp: Int,
    val folderChipRowTopPaddingDp: Int,
    val folderChipSpacingDp: Int,
    val headerBottomPaddingDp: Int,
    val headerBackgroundAlphaMultiplier: Float
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

internal fun shouldUseCommonListHeaderLocalBlur(
    headerBlurEnabled: Boolean,
    globalWallpaperVisible: Boolean
): Boolean = headerBlurEnabled && !globalWallpaperVisible

internal fun resolveCommonListViewportTopPadding(headerHeight: Dp): Dp {
    return headerHeight.coerceAtLeast(0.dp)
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
            uiPreset = uiPreset,
            androidNativeLiquidGlassEnabled = homeSettings.androidNativeLiquidGlassEnabled
        ),
        blurEnabled = headerBlurEnabled || homeSettings.isBottomBarBlurEnabled,
        showCoverGlassBadges = homeSettings.showHomeCoverGlassBadges,
        showInfoGlassBadges = homeSettings.showHomeInfoGlassBadges
    )
}

internal fun resolveCommonListFavoriteHeaderLayout(
    uiPreset: UiPreset,
    androidNativeVariant: AndroidNativeVariant = AndroidNativeVariant.MATERIAL3
): CommonListFavoriteHeaderLayout {
    return when {
        isMaterial3ExpressiveVariant(uiPreset, androidNativeVariant) -> {
            CommonListFavoriteHeaderLayout(
                searchBarHeightDp = 56,
                searchBarHorizontalPaddingDp = 18,
                searchBarVerticalPaddingDp = 8,
                browseToggleHeightDp = 44,
                browseToggleIndicatorHeightDp = 40,
                browseToggleLabelFontSizeSp = 15,
                browseToggleHorizontalPaddingDp = 18,
                browseToggleTopPaddingDp = 4,
                folderChipMinHeightDp = 40,
                folderChipHorizontalPaddingDp = 14,
                folderChipRowHorizontalPaddingDp = 18,
                folderChipRowTopPaddingDp = 8,
                folderChipSpacingDp = 10,
                headerBottomPaddingDp = 8,
                headerBackgroundAlphaMultiplier = 0.9f
            )
        }
        uiPreset == UiPreset.MD3 && androidNativeVariant == AndroidNativeVariant.MIUIX -> {
            CommonListFavoriteHeaderLayout(
                searchBarHeightDp = 46,
                searchBarHorizontalPaddingDp = 16,
                searchBarVerticalPaddingDp = 6,
                browseToggleHeightDp = 38,
                browseToggleIndicatorHeightDp = 34,
                browseToggleLabelFontSizeSp = 14,
                browseToggleHorizontalPaddingDp = 16,
                browseToggleTopPaddingDp = 2,
                folderChipMinHeightDp = 36,
                folderChipHorizontalPaddingDp = 12,
                folderChipRowHorizontalPaddingDp = 16,
                folderChipRowTopPaddingDp = 6,
                folderChipSpacingDp = 8,
                headerBottomPaddingDp = 6,
                headerBackgroundAlphaMultiplier = 0.84f
            )
        }
        uiPreset == UiPreset.MD3 -> {
            CommonListFavoriteHeaderLayout(
                searchBarHeightDp = 48,
                searchBarHorizontalPaddingDp = 16,
                searchBarVerticalPaddingDp = 6,
                browseToggleHeightDp = 38,
                browseToggleIndicatorHeightDp = 34,
                browseToggleLabelFontSizeSp = 14,
                browseToggleHorizontalPaddingDp = 16,
                browseToggleTopPaddingDp = 2,
                folderChipMinHeightDp = 36,
                folderChipHorizontalPaddingDp = 12,
                folderChipRowHorizontalPaddingDp = 16,
                folderChipRowTopPaddingDp = 6,
                folderChipSpacingDp = 8,
                headerBottomPaddingDp = 6,
                headerBackgroundAlphaMultiplier = 0.86f
            )
        }
        else -> {
            CommonListFavoriteHeaderLayout(
                searchBarHeightDp = 36,
                searchBarHorizontalPaddingDp = 16,
                searchBarVerticalPaddingDp = 6,
                browseToggleHeightDp = 34,
                browseToggleIndicatorHeightDp = 30,
                browseToggleLabelFontSizeSp = 14,
                browseToggleHorizontalPaddingDp = 16,
                browseToggleTopPaddingDp = 2,
                folderChipMinHeightDp = 32,
                folderChipHorizontalPaddingDp = 11,
                folderChipRowHorizontalPaddingDp = 12,
                folderChipRowTopPaddingDp = 6,
                folderChipSpacingDp = 8,
                headerBottomPaddingDp = 4,
                headerBackgroundAlphaMultiplier = 0.82f
            )
        }
    }
}
