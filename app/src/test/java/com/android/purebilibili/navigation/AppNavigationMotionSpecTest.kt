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

        assertEquals(260, spec.slideDurationMillis)
        assertEquals(160, spec.fastFadeDurationMillis)
        assertEquals(220, spec.backdropBlurDurationMillis)
        assertEquals(10f, spec.maxBackdropBlurRadius)
    }

    @Test
    fun `tablet layout should use enhanced spec`() {
        val spec = resolveAppNavigationMotionSpec(
            isTabletLayout = true,
            cardTransitionEnabled = true
        )

        assertEquals(380, spec.slideDurationMillis)
        assertEquals(210, spec.fastFadeDurationMillis)
        assertEquals(320, spec.slowFadeDurationMillis)
        assertEquals(28f, spec.maxBackdropBlurRadius)
    }

    @Test
    fun `compact layout should use normal spec`() {
        val spec = resolveAppNavigationMotionSpec(
            isTabletLayout = false,
            cardTransitionEnabled = true
        )

        assertEquals(340, spec.slideDurationMillis)
        assertEquals(180, spec.fastFadeDurationMillis)
        assertEquals(300, spec.slowFadeDurationMillis)
        assertEquals(20f, spec.maxBackdropBlurRadius)
    }
}
