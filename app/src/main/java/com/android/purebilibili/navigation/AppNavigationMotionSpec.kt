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
            slideDurationMillis = 260,
            fastFadeDurationMillis = 160,
            mediumFadeDurationMillis = 200,
            slowFadeDurationMillis = 240,
            backdropBlurDurationMillis = 220,
            maxBackdropBlurRadius = 10f
        )
    }

    return if (isTabletLayout) {
        AppNavigationMotionSpec(
            slideDurationMillis = 380,
            fastFadeDurationMillis = 210,
            mediumFadeDurationMillis = 260,
            slowFadeDurationMillis = 320,
            backdropBlurDurationMillis = 280,
            maxBackdropBlurRadius = 28f
        )
    } else {
        AppNavigationMotionSpec(
            slideDurationMillis = 340,
            fastFadeDurationMillis = 180,
            mediumFadeDurationMillis = 230,
            slowFadeDurationMillis = 300,
            backdropBlurDurationMillis = 240,
            maxBackdropBlurRadius = 20f
        )
    }
}
