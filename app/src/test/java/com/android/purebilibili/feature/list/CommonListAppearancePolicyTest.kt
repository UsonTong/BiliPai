package com.android.purebilibili.feature.list

import com.android.purebilibili.core.store.HomeHeaderBlurMode
import com.android.purebilibili.core.store.HomeSettings
import com.android.purebilibili.core.theme.UiPreset
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CommonListAppearancePolicyTest {

    @Test
    fun md3FollowPreset_keepsHeaderBlurForCommonList() {
        val enabled = resolveCommonListHeaderBlurEnabled(
            homeSettings = HomeSettings(
                headerBlurMode = HomeHeaderBlurMode.FOLLOW_PRESET
            ),
            uiPreset = UiPreset.MD3
        )

        assertTrue(enabled)
    }

    @Test
    fun iosFollowPreset_keepsHeaderBlurForCommonList() {
        val enabled = resolveCommonListHeaderBlurEnabled(
            homeSettings = HomeSettings(
                headerBlurMode = HomeHeaderBlurMode.FOLLOW_PRESET
            ),
            uiPreset = UiPreset.IOS
        )

        assertTrue(enabled)
    }

    @Test
    fun commonListVideoCardAppearance_followsHomeChromeToggles() {
        val appearance = resolveCommonListVideoCardAppearance(
            homeSettings = HomeSettings(
                headerBlurMode = HomeHeaderBlurMode.FOLLOW_PRESET,
                isBottomBarBlurEnabled = false,
                isLiquidGlassEnabled = false,
                showHomeCoverGlassBadges = false,
                showHomeInfoGlassBadges = false
            ),
            uiPreset = UiPreset.MD3
        )

        assertFalse(appearance.glassEnabled)
        assertTrue(appearance.blurEnabled)
        assertFalse(appearance.showCoverGlassBadges)
        assertFalse(appearance.showInfoGlassBadges)
    }
}
