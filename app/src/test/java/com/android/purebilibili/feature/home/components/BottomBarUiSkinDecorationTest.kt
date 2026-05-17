package com.android.purebilibili.feature.home.components

import com.android.purebilibili.core.plugin.skin.InstalledUiSkinPackage
import com.android.purebilibili.core.plugin.skin.UiSkinAssets
import com.android.purebilibili.core.plugin.skin.UiSkinManifest
import com.android.purebilibili.core.plugin.skin.UiSkinState
import com.android.purebilibili.core.plugin.skin.UiSkinSurface
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class BottomBarUiSkinDecorationTest {

    @Test
    fun activeExternalSkinUsesExtractedBottomTrimImagePath() {
        val installed = InstalledUiSkinPackage(
            manifest = UiSkinManifest(
                formatVersion = 1,
                skinId = "dev.example.cloud",
                displayName = "云朵底栏",
                version = "1.0.0",
                apiVersion = 1,
                surfaces = setOf(UiSkinSurface.HOME_BOTTOM_BAR),
                assets = UiSkinAssets(bottomBarTrim = "assets/bottom_trim.png")
            ),
            packageSha256 = "sha",
            packagePath = "/tmp/cloud.bpskin",
            installedAtMillis = 42L,
            assetFiles = mapOf("assets/bottom_trim.png" to "/tmp/bottom_trim.png")
        )

        val decoration = resolveBottomBarUiSkinDecoration(
            UiSkinState(enabled = true, activeSkin = installed)
        )

        assertEquals("dev.example.cloud", decoration?.skinId)
        assertEquals("/tmp/bottom_trim.png", decoration?.bottomTrimImagePath)
    }

    @Test
    fun disabledSkinDoesNotProduceBottomBarDecoration() {
        val installed = InstalledUiSkinPackage(
            manifest = UiSkinManifest(
                formatVersion = 1,
                skinId = "dev.example.cloud",
                displayName = "云朵底栏",
                version = "1.0.0",
                apiVersion = 1,
                surfaces = setOf(UiSkinSurface.HOME_BOTTOM_BAR),
                assets = UiSkinAssets(bottomBarTrim = "assets/bottom_trim.png")
            ),
            packageSha256 = "sha",
            packagePath = "/tmp/cloud.bpskin",
            installedAtMillis = 42L,
            assetFiles = mapOf("assets/bottom_trim.png" to "/tmp/bottom_trim.png")
        )

        assertNull(
            resolveBottomBarUiSkinDecoration(
                UiSkinState(enabled = false, activeSkin = installed)
            )
        )
    }
}
