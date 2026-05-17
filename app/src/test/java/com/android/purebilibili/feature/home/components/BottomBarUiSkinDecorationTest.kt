package com.android.purebilibili.feature.home.components

import com.android.purebilibili.core.plugin.skin.InstalledUiSkinPackage
import com.android.purebilibili.core.plugin.skin.UiSkinAssets
import com.android.purebilibili.core.plugin.skin.UiSkinColorTokens
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

    @Test
    fun activeExternalSkinUsesExtractedHomeAtmosphereImagePath() {
        val installed = InstalledUiSkinPackage(
            manifest = UiSkinManifest(
                formatVersion = 1,
                skinId = "dev.example.atmosphere",
                displayName = "顶部氛围",
                version = "1.0.0",
                apiVersion = 1,
                surfaces = setOf(UiSkinSurface.HOME_TOP_CHROME),
                assets = UiSkinAssets(topAtmosphere = "assets/head_bg.jpg"),
                colors = UiSkinColorTokens(
                    topAtmosphereTint = "#DFF5FF",
                    searchCapsuleTint = "#FFFFFF"
                )
            ),
            packageSha256 = "sha",
            packagePath = "/tmp/atmosphere.bpskin",
            installedAtMillis = 42L,
            assetFiles = mapOf("assets/head_bg.jpg" to "/tmp/head_bg.jpg")
        )

        val decoration = resolveHomeUiSkinDecoration(
            UiSkinState(enabled = true, activeSkin = installed)
        )

        assertEquals("dev.example.atmosphere", decoration?.skinId)
        assertEquals("/tmp/head_bg.jpg", decoration?.topAtmosphereImagePath)
    }

    @Test
    fun bottomOnlySkinDoesNotProduceHomeAtmosphereDecoration() {
        val installed = InstalledUiSkinPackage(
            manifest = UiSkinManifest(
                formatVersion = 1,
                skinId = "dev.example.bottom",
                displayName = "仅底栏",
                version = "1.0.0",
                apiVersion = 1,
                surfaces = setOf(UiSkinSurface.HOME_BOTTOM_BAR),
                assets = UiSkinAssets(bottomBarTrim = "assets/bottom_trim.png")
            ),
            packageSha256 = "sha",
            packagePath = "/tmp/bottom.bpskin",
            installedAtMillis = 42L,
            assetFiles = mapOf("assets/bottom_trim.png" to "/tmp/bottom_trim.png")
        )

        assertNull(
            resolveHomeUiSkinDecoration(
                UiSkinState(enabled = true, activeSkin = installed)
            )
        )
    }
}
