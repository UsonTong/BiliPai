package com.android.purebilibili.navigation

import org.junit.Assert.assertEquals
import org.junit.Test

class AppNavigationMotionSpecTest {

    @Test
    fun `disabled card transition should use reduced spec`() {
        val spec = resolveAppNavigationMotionSpec(
            isTabletLayout = true,
            cardTransitionEnabled = false
        )

        assertEquals(220, spec.slideDurationMillis)
        assertEquals(120, spec.fastFadeDurationMillis)
        assertEquals(160, spec.backdropBlurDurationMillis)
        assertEquals(8f, spec.maxBackdropBlurRadius)
        assertEquals(120, spec.fallbackFadeDurationMillis)
        assertEquals(170, spec.quickReturnFadeDurationMillis)
        assertEquals(180, spec.seamlessFadeDurationMillis)
        assertEquals(180, spec.cardTargetFallbackSlideMaxDurationMillis)
    }

    @Test
    fun `tablet layout should use enhanced spec`() {
        val spec = resolveAppNavigationMotionSpec(
            isTabletLayout = true,
            cardTransitionEnabled = true
        )

        assertEquals(310, spec.slideDurationMillis)
        assertEquals(170, spec.fastFadeDurationMillis)
        assertEquals(250, spec.slowFadeDurationMillis)
        assertEquals(22f, spec.maxBackdropBlurRadius)
        assertEquals(120, spec.fallbackFadeDurationMillis)
        assertEquals(170, spec.quickReturnFadeDurationMillis)
        assertEquals(180, spec.seamlessFadeDurationMillis)
        assertEquals(180, spec.cardTargetFallbackSlideMaxDurationMillis)
    }

    @Test
    fun `compact layout should use normal spec`() {
        val spec = resolveAppNavigationMotionSpec(
            isTabletLayout = false,
            cardTransitionEnabled = true
        )

        assertEquals(260, spec.slideDurationMillis)
        assertEquals(145, spec.fastFadeDurationMillis)
        assertEquals(220, spec.slowFadeDurationMillis)
        assertEquals(16f, spec.maxBackdropBlurRadius)
        assertEquals(120, spec.fallbackFadeDurationMillis)
        assertEquals(170, spec.quickReturnFadeDurationMillis)
        assertEquals(180, spec.seamlessFadeDurationMillis)
        assertEquals(180, spec.cardTargetFallbackSlideMaxDurationMillis)
    }
}
