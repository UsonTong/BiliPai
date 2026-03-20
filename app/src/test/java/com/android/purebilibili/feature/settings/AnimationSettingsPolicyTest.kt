package com.android.purebilibili.feature.settings

import com.android.purebilibili.core.store.LiquidGlassMode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AnimationSettingsPolicyTest {

    @Test
    fun predictiveBackToggle_cardTransitionEnabled_usesPredictiveGestureState() {
        val enabledAndChecked = resolvePredictiveBackToggleUiState(
            cardTransitionEnabled = true,
            predictiveBackAnimationEnabled = true
        )
        assertTrue(enabledAndChecked.enabled)
        assertTrue(enabledAndChecked.checked)
        assertEquals(PREDICTIVE_BACK_TOGGLE_TITLE, enabledAndChecked.title)
        assertEquals(PREDICTIVE_BACK_TOGGLE_ACTIVE_SUBTITLE, enabledAndChecked.subtitle)

        val enabledAndUnchecked = resolvePredictiveBackToggleUiState(
            cardTransitionEnabled = true,
            predictiveBackAnimationEnabled = false
        )
        assertTrue(enabledAndUnchecked.enabled)
        assertFalse(enabledAndUnchecked.checked)
        assertEquals(PREDICTIVE_BACK_TOGGLE_TITLE, enabledAndUnchecked.title)
        assertEquals(PREDICTIVE_BACK_TOGGLE_INACTIVE_SUBTITLE, enabledAndUnchecked.subtitle)
    }

    @Test
    fun predictiveBackToggle_cardTransitionDisabled_forcesDisabledUnchecked() {
        val disabledState = resolvePredictiveBackToggleUiState(
            cardTransitionEnabled = false,
            predictiveBackAnimationEnabled = true
        )
        assertFalse(disabledState.enabled)
        assertFalse(disabledState.checked)
        assertEquals(PREDICTIVE_BACK_TOGGLE_TITLE, disabledState.title)
        assertEquals(PREDICTIVE_BACK_TOGGLE_DEPENDENCY_SUBTITLE, disabledState.subtitle)
    }

    @Test
    fun liquidGlassPreviewUiState_usesSemanticCopy() {
        val clear = resolveLiquidGlassPreviewUiState(
            mode = LiquidGlassMode.CLEAR,
            strength = 0.42f
        )
        val frosted = resolveLiquidGlassPreviewUiState(
            mode = LiquidGlassMode.FROSTED,
            strength = 0.62f
        )

        assertEquals("通透玻璃", clear.modeLabel)
        assertTrue(clear.subtitle.contains("清晰"))
        assertEquals("柔和磨砂", frosted.modeLabel)
        assertTrue(frosted.subtitle.contains("柔和"))
    }

    @Test
    fun liquidGlassPreviewUiState_clampsAndFormatsStrength() {
        val state = resolveLiquidGlassPreviewUiState(
            mode = LiquidGlassMode.BALANCED,
            strength = 1.4f
        )

        assertEquals(1f, state.normalizedStrength)
        assertEquals("100%", state.strengthLabel)
    }
}
