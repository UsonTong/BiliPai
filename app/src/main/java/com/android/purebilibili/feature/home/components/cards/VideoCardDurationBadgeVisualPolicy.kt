package com.android.purebilibili.feature.home.components.cards

internal data class VideoCardDurationBadgeVisualStyle(
    val backgroundAlpha: Float,
    val textShadowAlpha: Float,
    val textShadowBlurRadiusPx: Float
)

internal fun resolveVideoCardDurationBadgeVisualStyle(): VideoCardDurationBadgeVisualStyle {
    return VideoCardDurationBadgeVisualStyle(
        backgroundAlpha = 0f,
        textShadowAlpha = 0.72f,
        textShadowBlurRadiusPx = 4f
    )
}
