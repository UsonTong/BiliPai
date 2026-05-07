package com.android.purebilibili.feature.settings

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
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
    fun liquidGlassPreviewUiState_usesContinuousCopy() {
        val clear = resolveLiquidGlassPreviewUiState(progress = 0.1f)
        val frosted = resolveLiquidGlassPreviewUiState(progress = 0.9f)

        assertEquals("通透", clear.modeLabel)
        assertTrue(clear.subtitle.contains("清晰"))
        assertEquals("磨砂", frosted.modeLabel)
        assertTrue(frosted.subtitle.contains("柔和"))
        assertNotEquals("平衡", clear.modeLabel)
        assertNotEquals("平衡", frosted.modeLabel)
    }

    @Test
    fun liquidGlassPreviewUiState_clampsAndFormatsProgress() {
        val state = resolveLiquidGlassPreviewUiState(progress = 1.4f)

        assertEquals(1f, state.normalizedProgress)
        assertEquals("100%", state.strengthLabel)
    }

    @Test
    fun bottomBarLiquidGlassPresetControl_livesInAnimationSettings() {
        val animationSource = loadSource(
            "app/src/main/java/com/android/purebilibili/feature/settings/screen/AnimationSettingsScreen.kt"
        )
        val bottomBarSource = loadSource(
            "app/src/main/java/com/android/purebilibili/feature/settings/screen/BottomBarSettingsScreen.kt"
        )

        assertTrue(animationSource.contains("底栏液态玻璃预设"))
        assertTrue(animationSource.contains("BottomBarLiquidGlassPreset.entries"))
        assertFalse(bottomBarSource.contains("底栏液态玻璃预设"))
        assertFalse(bottomBarSource.contains("BottomBarLiquidGlassPreset.entries"))
    }

    private fun loadSource(path: String): String {
        val normalizedPath = path.removePrefix("app/")
        val sourceFile = listOf(
            File(path),
            File(normalizedPath)
        ).firstOrNull { it.exists() }
        require(sourceFile != null) { "Cannot locate $path from ${File(".").absolutePath}" }
        return sourceFile.readText()
    }
}
