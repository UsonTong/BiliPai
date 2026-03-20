package com.android.purebilibili.feature.home.components

import com.android.purebilibili.core.store.LiquidGlassMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LiquidGlassTuningTest {

    @Test
    fun `clear mode stays more transparent than frosted at same strength`() {
        val clear = resolveLiquidGlassTuning(
            mode = LiquidGlassMode.CLEAR,
            strength = 0.5f
        )
        val frosted = resolveLiquidGlassTuning(
            mode = LiquidGlassMode.FROSTED,
            strength = 0.5f
        )

        assertTrue(clear.backdropBlurRadius < frosted.backdropBlurRadius)
        assertTrue(clear.surfaceAlpha < frosted.surfaceAlpha)
        assertTrue(clear.refractionAmount > frosted.refractionAmount)
    }

    @Test
    fun `strength is clamped into safe range`() {
        val low = resolveLiquidGlassTuning(
            mode = LiquidGlassMode.BALANCED,
            strength = -1f
        )
        val high = resolveLiquidGlassTuning(
            mode = LiquidGlassMode.BALANCED,
            strength = 3f
        )

        assertEquals(0f, low.strength, 0.0001f)
        assertEquals(1f, high.strength, 0.0001f)
        assertTrue(high.backdropBlurRadius >= low.backdropBlurRadius)
    }

    @Test
    fun `clear mode damps chromatic split and scroll coupling`() {
        val clear = resolveLiquidGlassTuning(
            mode = LiquidGlassMode.CLEAR,
            strength = 0.42f
        )
        val balanced = resolveLiquidGlassTuning(
            mode = LiquidGlassMode.BALANCED,
            strength = 0.52f
        )

        assertTrue(clear.indicatorChromaticBoost < balanced.indicatorChromaticBoost)
        assertFalse(clear.scrollCoupledRefraction)
    }
}
