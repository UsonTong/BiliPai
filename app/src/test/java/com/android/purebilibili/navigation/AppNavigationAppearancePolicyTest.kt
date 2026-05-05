package com.android.purebilibili.navigation

import com.android.purebilibili.core.store.HomeSettings
import com.android.purebilibili.core.theme.AndroidNativeVariant
import com.android.purebilibili.core.theme.UiPreset
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AppNavigationAppearancePolicyTest {

    @Test
    fun mapsBottomBarAndTransitionFlagsFromHomeSettings() {
        val appearance = resolveAppNavigationAppearance(
            HomeSettings(
                isBottomBarFloating = false,
                bottomBarLabelMode = 2,
                isBottomBarBlurEnabled = false,
                cardTransitionEnabled = false,
                videoTransitionRealtimeBlurEnabled = false,
                predictiveBackAnimationEnabled = false
            )
        )

        assertFalse(appearance.cardTransitionEnabled)
        assertFalse(appearance.videoTransitionRealtimeBlurEnabled)
        assertFalse(appearance.predictiveBackAnimationEnabled)
        assertFalse(appearance.bottomBarBlurEnabled)
        assertEquals(2, appearance.bottomBarLabelMode)
        assertFalse(appearance.bottomBarFloating)
    }

    @Test
    fun keepsDefaultsWhenHomeSettingsUseDefaults() {
        val appearance = resolveAppNavigationAppearance(HomeSettings())

        assertTrue(appearance.cardTransitionEnabled)
        assertTrue(appearance.videoTransitionRealtimeBlurEnabled)
        assertTrue(appearance.predictiveBackAnimationEnabled)
        assertTrue(appearance.bottomBarBlurEnabled)
        assertEquals(0, appearance.bottomBarLabelMode)
        assertTrue(appearance.bottomBarFloating)
    }

    @Test
    fun md3Preset_keepsFloatingBottomBarWhenShellSettingsAreStillDefault() {
        val appearance = resolveAppNavigationAppearance(
            homeSettings = HomeSettings(),
            uiPreset = UiPreset.MD3
        )

        assertTrue(appearance.bottomBarFloating)
        assertTrue(appearance.bottomBarBlurEnabled)
        assertEquals(0, appearance.bottomBarLabelMode)
    }

    @Test
    fun md3Preset_preservesExplicitBottomBarShellCustomization() {
        val appearance = resolveAppNavigationAppearance(
            homeSettings = HomeSettings(
                isBottomBarFloating = true,
                bottomBarLabelMode = 1,
                isBottomBarBlurEnabled = false
            ),
            uiPreset = UiPreset.MD3
        )

        assertTrue(appearance.bottomBarFloating)
        assertFalse(appearance.bottomBarBlurEnabled)
        assertEquals(1, appearance.bottomBarLabelMode)
    }

    @Test
    fun md3MiuixPreset_keepsFloatingBottomBarWhenShellSettingsAreDefault() {
        val appearance = resolveAppNavigationAppearance(
            homeSettings = HomeSettings(),
            uiPreset = UiPreset.MD3,
            androidNativeVariant = AndroidNativeVariant.MIUIX
        )

        assertTrue(appearance.bottomBarFloating)
        assertTrue(appearance.bottomBarBlurEnabled)
        assertEquals(0, appearance.bottomBarLabelMode)
    }

    @Test
    fun bottomBarBackdropCapturesGlobalWallpaperBeforeNavHostContent() {
        val source = loadSource("app/src/main/java/com/android/purebilibili/navigation/AppNavigation.kt")
        val capturedLayerSource = source
            .substringAfter(".layerBackdrop(bottomBarBackdrop)")
            .substringBefore("// ===== 全局底栏")

        val wallpaperIndex = capturedLayerSource.indexOf("HomeWallpaperBackdrop(")
        val navHostIndex = capturedLayerSource.indexOf("NavHost(")

        assertTrue(wallpaperIndex >= 0)
        assertTrue(navHostIndex > wallpaperIndex)
        assertTrue(capturedLayerSource.contains(".then(if (mainHazeState != null) Modifier.hazeSource(mainHazeState) else Modifier)"))
    }

    private fun loadSource(path: String): String {
        val candidates = listOf(
            File(path),
            File("app", path.removePrefix("app/")),
            File(path.removePrefix("app/")),
            File("..", path)
        )
        return candidates.first { it.exists() }.readText()
    }
}
