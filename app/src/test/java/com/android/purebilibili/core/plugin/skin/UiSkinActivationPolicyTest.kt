package com.android.purebilibili.core.plugin.skin

import com.android.purebilibili.core.store.BottomBarLiquidGlassPreset
import com.android.purebilibili.core.store.HomeSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class UiSkinActivationPolicyTest {

    @Test
    fun defaultSelection_keepsSkinDisabled() {
        val state = resolveUiSkinState(
            selection = UiSkinSelection(),
            installedSkins = listOf(BuiltInUiSkins.winterCloudInstallRecord)
        )

        assertFalse(state.enabled)
        assertNull(state.activeSkin)
    }

    @Test
    fun enabledSelectionActivatesOnlyMatchingInstalledSkin() {
        val state = resolveUiSkinState(
            selection = UiSkinSelection(
                enabled = true,
                selectedSkinId = BuiltInUiSkins.winterCloud.skinId
            ),
            installedSkins = listOf(BuiltInUiSkins.winterCloudInstallRecord)
        )

        assertTrue(state.enabled)
        assertEquals(BuiltInUiSkins.winterCloud.skinId, state.activeSkin?.skinId)
    }

    @Test
    fun missingSelectedSkinFallsBackToNoActiveSkin() {
        val state = resolveUiSkinState(
            selection = UiSkinSelection(enabled = true, selectedSkinId = "missing"),
            installedSkins = listOf(BuiltInUiSkins.winterCloudInstallRecord)
        )

        assertFalse(state.enabled)
        assertNull(state.activeSkin)
    }

    @Test
    fun skinStateDoesNotMutateLiquidGlassSettings() {
        val homeSettings = HomeSettings(
            isBottomBarLiquidGlassEnabled = true,
            bottomBarLiquidGlassPreset = BottomBarLiquidGlassPreset.BILIPAI_TUNED
        )
        val state = resolveUiSkinState(
            selection = UiSkinSelection(
                enabled = true,
                selectedSkinId = BuiltInUiSkins.winterCloud.skinId
            ),
            installedSkins = listOf(BuiltInUiSkins.winterCloudInstallRecord)
        )

        val resolved = resolveUiSkinHomeSettings(
            homeSettings = homeSettings,
            uiSkinState = state
        )

        assertEquals(homeSettings, resolved)
        assertTrue(resolved.isBottomBarLiquidGlassEnabled)
        assertEquals(BottomBarLiquidGlassPreset.BILIPAI_TUNED, resolved.bottomBarLiquidGlassPreset)
    }
}
