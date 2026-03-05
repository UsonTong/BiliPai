package com.android.purebilibili.feature.settings

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AnimationSettingsPolicyTest {

    @Test
    fun predictiveBackToggle_cardTransitionEnabled_usesPredictiveState() {
        val enabledAndChecked = resolvePredictiveBackToggleUiState(
            cardTransitionEnabled = true,
            predictiveBackAnimationEnabled = true
        )
        assertTrue(enabledAndChecked.enabled)
        assertTrue(enabledAndChecked.checked)
        assertEquals(PREDICTIVE_BACK_TOGGLE_ACTIVE_SUBTITLE, enabledAndChecked.subtitle)

        val enabledAndUnchecked = resolvePredictiveBackToggleUiState(
            cardTransitionEnabled = true,
            predictiveBackAnimationEnabled = false
        )
        assertTrue(enabledAndUnchecked.enabled)
        assertFalse(enabledAndUnchecked.checked)
        assertEquals(PREDICTIVE_BACK_TOGGLE_ACTIVE_SUBTITLE, enabledAndUnchecked.subtitle)
    }

    @Test
    fun predictiveBackToggle_cardTransitionDisabled_forcesDisabledUnchecked() {
        val disabledState = resolvePredictiveBackToggleUiState(
            cardTransitionEnabled = false,
            predictiveBackAnimationEnabled = true
        )
        assertFalse(disabledState.enabled)
        assertFalse(disabledState.checked)
        assertEquals(PREDICTIVE_BACK_TOGGLE_DEPENDENCY_SUBTITLE, disabledState.subtitle)
    }
}
