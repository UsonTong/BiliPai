package com.android.purebilibili.core.plugin.skin

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class UiSkinInstallStoreTest {

    @Test
    fun installPreview_persistsBpskinRecordButKeepsItDisabledByDefault() {
        val rootDir = createTempDirectory("ui-skin-store").toFile()
        val store = UiSkinInstallStore(
            rootDir = rootDir,
            clock = { 42L }
        )
        val manifest = UiSkinManifest(
            formatVersion = 1,
            skinId = "dev.example.cloud",
            displayName = "云朵底栏",
            version = "1.0.0",
            apiVersion = 1,
            surfaces = setOf(UiSkinSurface.HOME_BOTTOM_BAR),
            assets = UiSkinAssets(bottomBarTrim = "assets/bottom_trim.png")
        )
        val bytes = skinPackage(
            "skin-manifest.json" to Json.encodeToString(manifest).toByteArray(),
            "assets/bottom_trim.png" to pngBytes()
        )
        val preview = store.previewPackage(bytes).getOrThrow()

        val installed = store.installPreview(preview, bytes).getOrThrow()
        val installedPackages = store.listInstalledPackages()

        assertEquals(manifest, installed.manifest)
        assertFalse(installed.enabled)
        assertEquals(42L, installed.installedAtMillis)
        assertTrue(installed.packagePath.endsWith(".bpskin"))
        val bottomTrimPath = assertNotNull(installed.assetFiles["assets/bottom_trim.png"])
        assertTrue(File(bottomTrimPath).exists())
        assertEquals(pngBytes().toList(), File(bottomTrimPath).readBytes().toList())
        assertTrue(installedPackages.any { it.skinId == BuiltInUiSkins.winterCloud.skinId })
        assertTrue(installedPackages.any { it.skinId == manifest.skinId && !it.enabled })
    }

    @Test
    fun installPreview_keepsDifferentPackagesWithSameSkinId() {
        val rootDir = createTempDirectory("ui-skin-store").toFile()
        var now = 100L
        val store = UiSkinInstallStore(
            rootDir = rootDir,
            clock = { now++ }
        )
        val firstBytes = skinPackage(
            "skin-manifest.json" to Json.encodeToString(
                manifest(
                    displayName = "本地装扮资源包",
                    version = "1.0.0",
                    bottomBarTrim = "assets/first.png"
                )
            ).toByteArray(),
            "assets/first.png" to pngBytes()
        )
        val secondBytes = skinPackage(
            "skin-manifest.json" to Json.encodeToString(
                manifest(
                    displayName = "本地装扮资源包",
                    version = "1.0.1",
                    bottomBarTrim = "assets/second.png"
                )
            ).toByteArray(),
            "assets/second.png" to pngBytes(0x01)
        )

        val firstPreview = store.previewPackage(firstBytes).getOrThrow()
        val secondPreview = store.previewPackage(secondBytes).getOrThrow()
        val firstInstalled = store.installPreview(firstPreview, firstBytes).getOrThrow()
        val secondInstalled = store.installPreview(secondPreview, secondBytes).getOrThrow()
        val installedPackages = store.listInstalledPackages()
            .filter { it.skinId == "local.bilibili_skin.local_package" }

        assertEquals(2, installedPackages.size)
        assertTrue(installedPackages.any { it.installId == firstInstalled.installId })
        assertTrue(installedPackages.any { it.installId == secondInstalled.installId })
        assertTrue(firstInstalled.installId != secondInstalled.installId)
        assertTrue(File(checkNotNull(firstInstalled.assetFiles["assets/first.png"])).exists())
        assertTrue(File(checkNotNull(secondInstalled.assetFiles["assets/second.png"])).exists())
    }

    private fun manifest(
        displayName: String,
        version: String,
        bottomBarTrim: String
    ): UiSkinManifest {
        return UiSkinManifest(
            formatVersion = 1,
            skinId = "local.bilibili_skin.local_package",
            displayName = displayName,
            version = version,
            apiVersion = 1,
            surfaces = setOf(UiSkinSurface.HOME_BOTTOM_BAR),
            assets = UiSkinAssets(bottomBarTrim = bottomBarTrim)
        )
    }

    private fun skinPackage(vararg entries: Pair<String, ByteArray>): ByteArray {
        return ByteArrayOutputStream().use { output ->
            ZipOutputStream(output).use { zip ->
                entries.forEach { (name, bytes) ->
                    zip.putNextEntry(ZipEntry(name))
                    zip.write(bytes)
                    zip.closeEntry()
                }
            }
            output.toByteArray()
        }
    }

    private fun pngBytes(seed: Int = 0x00): ByteArray {
        return byteArrayOf(
            0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A,
            seed.toByte(), 0x00, 0x00, 0x0D
        )
    }
}
