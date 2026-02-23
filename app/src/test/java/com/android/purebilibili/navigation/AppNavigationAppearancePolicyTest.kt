package com.android.purebilibili.navigation

import com.android.purebilibili.core.store.HomeSettings
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
                cardTransitionEnabled = false
            )
        )

        assertFalse(appearance.cardTransitionEnabled)
        assertFalse(appearance.bottomBarBlurEnabled)
        assertEquals(2, appearance.bottomBarLabelMode)
        assertFalse(appearance.bottomBarFloating)
    }

    @Test
    fun keepsDefaultsWhenHomeSettingsUseDefaults() {
        val appearance = resolveAppNavigationAppearance(HomeSettings())

        assertTrue(appearance.cardTransitionEnabled)
        assertTrue(appearance.bottomBarBlurEnabled)
        assertEquals(0, appearance.bottomBarLabelMode)
        assertTrue(appearance.bottomBarFloating)
    }
}
