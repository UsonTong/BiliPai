package com.android.purebilibili.feature.settings

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class VisualEffectTogglePolicyTest {

    @Test
    fun `enabling bottom bar blur disables liquid glass`() {
        val result = resolveBottomBarBlurToggleState(
            enableBottomBarBlur = true,
            currentLiquidGlassEnabled = true
        )

        assertTrue(result.bottomBarBlurEnabled)
        assertFalse(result.liquidGlassEnabled)
    }

    @Test
    fun `disabling bottom bar blur keeps liquid glass disabled when user already turned it off`() {
        val result = resolveBottomBarBlurToggleState(
            enableBottomBarBlur = false,
            currentLiquidGlassEnabled = false
        )

        assertFalse(result.bottomBarBlurEnabled)
        assertFalse(result.liquidGlassEnabled)
    }

    @Test
    fun `disabling bottom bar blur keeps liquid glass enabled when it was already on`() {
        val result = resolveBottomBarBlurToggleState(
            enableBottomBarBlur = false,
            currentLiquidGlassEnabled = true
        )

        assertFalse(result.bottomBarBlurEnabled)
        assertTrue(result.liquidGlassEnabled)
    }

    @Test
    fun `enabling liquid glass disables bottom bar blur`() {
        val result = resolveLiquidGlassToggleState(
            enableLiquidGlass = true,
            currentBottomBarBlurEnabled = true
        )
        assertTrue(result.liquidGlassEnabled)
        assertFalse(result.bottomBarBlurEnabled)
    }

    @Test
    fun `disabling liquid glass keeps bottom bar blur disabled when user already turned it off`() {
        val result = resolveLiquidGlassToggleState(
            enableLiquidGlass = false,
            currentBottomBarBlurEnabled = false
        )
        assertFalse(result.liquidGlassEnabled)
        assertFalse(result.bottomBarBlurEnabled)
    }

    @Test
    fun `disabling liquid glass keeps bottom bar blur enabled when it was already on`() {
        val result = resolveLiquidGlassToggleState(
            enableLiquidGlass = false,
            currentBottomBarBlurEnabled = true
        )
        assertFalse(result.liquidGlassEnabled)
        assertTrue(result.bottomBarBlurEnabled)
    }
}
