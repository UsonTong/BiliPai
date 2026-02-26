package com.android.purebilibili

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SplashExitAnimationPolicyTest {

    @Test
    fun enablesRealtimeBlurOnlyOnAndroid14AndAbove() {
        assertFalse(shouldUseRealtimeSplashBlur(31))
        assertFalse(shouldUseRealtimeSplashBlur(33))
        assertTrue(shouldUseRealtimeSplashBlur(34))
    }

    @Test
    fun disablesRealtimeBlurBelowAndroid14() {
        assertFalse(shouldUseRealtimeSplashBlur(30))
    }

    @Test
    fun allowsCustomSplashOverlayWhenFlyoutEnabledAndDataPresent() {
        assertTrue(
            shouldShowCustomSplashOverlay(
                customSplashEnabled = true,
                splashUri = "content://splash.jpg"
            )
        )
    }

    @Test
    fun allowsCustomSplashOverlayWhenFlyoutDisabledAndDataPresent() {
        assertTrue(
            shouldShowCustomSplashOverlay(
                customSplashEnabled = true,
                splashUri = "content://splash.jpg"
            )
        )
    }

    @Test
    fun appliesRealtimeBlurOnlyAfterAnimationProgressStarts() {
        assertFalse(shouldApplySplashRealtimeBlur(useRealtimeBlur = true, progress = 0f))
        assertTrue(shouldApplySplashRealtimeBlur(useRealtimeBlur = true, progress = 0.12f))
        assertFalse(shouldApplySplashRealtimeBlur(useRealtimeBlur = false, progress = 0.5f))
    }
}
