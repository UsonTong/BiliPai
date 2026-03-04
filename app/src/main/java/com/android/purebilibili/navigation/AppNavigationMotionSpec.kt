package com.android.purebilibili.navigation

internal data class AppNavigationMotionSpec(
    val slideDurationMillis: Int,
    val fastFadeDurationMillis: Int,
    val mediumFadeDurationMillis: Int,
    val slowFadeDurationMillis: Int,
    val backdropBlurDurationMillis: Int,
    val maxBackdropBlurRadius: Float
)

internal fun resolveAppNavigationMotionSpec(
    isTabletLayout: Boolean,
    cardTransitionEnabled: Boolean
): AppNavigationMotionSpec {
    if (!cardTransitionEnabled) {
        return AppNavigationMotionSpec(
            slideDurationMillis = 220,
            fastFadeDurationMillis = 120,
            mediumFadeDurationMillis = 160,
            slowFadeDurationMillis = 190,
            backdropBlurDurationMillis = 160,
            maxBackdropBlurRadius = 8f
        )
    }

    return if (isTabletLayout) {
        AppNavigationMotionSpec(
            slideDurationMillis = 350,
            fastFadeDurationMillis = 190,
            mediumFadeDurationMillis = 240,
            slowFadeDurationMillis = 290,
            backdropBlurDurationMillis = 240,
            maxBackdropBlurRadius = 24f
        )
    } else {
        AppNavigationMotionSpec(
            slideDurationMillis = 300,
            fastFadeDurationMillis = 160,
            mediumFadeDurationMillis = 200,
            slowFadeDurationMillis = 255,
            backdropBlurDurationMillis = 200,
            maxBackdropBlurRadius = 16f
        )
    }
}
