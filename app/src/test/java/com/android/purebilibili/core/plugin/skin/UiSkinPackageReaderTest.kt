package com.android.purebilibili.core.plugin.skin

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.ByteArrayOutputStream
import java.security.MessageDigest
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class UiSkinPackageReaderTest {

    @Test
    fun validPackage_readsManifestHashAndDeclaredImageAssetsWithoutExecutingCode() {
        val manifest = sampleManifest()
        val bytes = skinPackage(
            "skin-manifest.json" to Json.encodeToString(manifest).toByteArray(),
            "assets/bottom_trim.png" to pngBytes(),
            "assets/top_atmosphere.webp" to webpBytes()
        )

        val preview = UiSkinPackageReader.preview(bytes).getOrThrow()

        assertEquals(manifest, preview.manifest)
        assertEquals(sha256Hex(bytes), preview.packageSha256)
        assertEquals(
            listOf(
                UiSkinAssetEntry("assets/bottom_trim.png", UiSkinAssetType.PNG, pngBytes().size.toLong()),
                UiSkinAssetEntry("assets/top_atmosphere.webp", UiSkinAssetType.WEBP, webpBytes().size.toLong())
            ),
            preview.assetEntries
        )
    }

    @Test
    fun missingManifest_rejectsSkinBeforeInstallDecision() {
        val bytes = skinPackage("assets/bottom_trim.png" to pngBytes())

        val error = UiSkinPackageReader.preview(bytes).exceptionOrNull()

        assertNotNull(error)
        assertEquals("皮肤包缺少 skin-manifest.json", error.message)
    }

    @Test
    fun pathTraversalEntry_rejectsSkin() {
        val bytes = skinPackage(
            "skin-manifest.json" to Json.encodeToString(sampleManifest()).toByteArray(),
            "../escape.png" to pngBytes()
        )

        val error = UiSkinPackageReader.preview(bytes).exceptionOrNull()

        assertNotNull(error)
        assertEquals("皮肤包包含非法路径: ../escape.png", error.message)
    }

    @Test
    fun oversizedManifest_rejectsSkin() {
        val bytes = skinPackage(
            "skin-manifest.json" to ByteArray(64 * 1024 + 1) { '{'.code.toByte() }
        )

        val error = UiSkinPackageReader.preview(bytes).exceptionOrNull()

        assertNotNull(error)
        assertEquals("skin-manifest.json 超过 65536 字节", error.message)
    }

    @Test
    fun oversizedUncompressedPackage_rejectsSkin() {
        val bytes = skinPackage(
            "skin-manifest.json" to Json.encodeToString(sampleManifest()).toByteArray(),
            "assets/huge.png" to ByteArray(16 * 1024 * 1024 + 1)
        )

        val error = UiSkinPackageReader.preview(bytes).exceptionOrNull()

        assertNotNull(error)
        assertEquals("皮肤包解压后内容超过 16777216 字节", error.message)
    }

    @Test
    fun unknownSurface_rejectsSkinWithReadableMessage() {
        val manifestJson = """
            {
              "formatVersion": 1,
              "skinId": "dev.example.unknown",
              "displayName": "未知界面",
              "version": "1.0.0",
              "apiVersion": 1,
              "surfaces": ["HOME_BOTTOM_BAR", "PLAYER_OVERLAY"],
              "assets": {"bottomBarTrim": "assets/bottom_trim.png"}
            }
        """.trimIndent().toByteArray()
        val bytes = skinPackage(
            "skin-manifest.json" to manifestJson,
            "assets/bottom_trim.png" to pngBytes()
        )

        val error = UiSkinPackageReader.preview(bytes).exceptionOrNull()

        assertNotNull(error)
        assertEquals("皮肤包声明了未知界面: PLAYER_OVERLAY", error.message)
    }

    @Test
    fun damagedDeclaredImage_rejectsSkin() {
        val bytes = skinPackage(
            "skin-manifest.json" to Json.encodeToString(sampleManifest()).toByteArray(),
            "assets/bottom_trim.png" to byteArrayOf(1, 2, 3),
            "assets/top_atmosphere.webp" to webpBytes()
        )

        val error = UiSkinPackageReader.preview(bytes).exceptionOrNull()

        assertNotNull(error)
        assertEquals("皮肤资源 assets/bottom_trim.png 不是受支持的图片格式", error.message)
    }

    @Test
    fun duplicateAssetPath_rejectsSkin() {
        val manifest = sampleManifest(
            assets = UiSkinAssets(
                bottomBarTrim = "assets/shared.png",
                topAtmosphere = "assets/shared.png"
            )
        )
        val bytes = skinPackage(
            "skin-manifest.json" to Json.encodeToString(manifest).toByteArray(),
            "assets/shared.png" to pngBytes()
        )

        val error = UiSkinPackageReader.preview(bytes).exceptionOrNull()

        assertNotNull(error)
        assertEquals("皮肤包重复声明资源: assets/shared.png", error.message)
    }

    @Test
    fun nonAssetPayload_rejectsSkinCodeLikeEntries() {
        val bytes = skinPackage(
            "skin-manifest.json" to Json.encodeToString(sampleManifest()).toByteArray(),
            "classes.dex" to byteArrayOf(0x64, 0x65, 0x78),
            "assets/bottom_trim.png" to pngBytes()
        )

        val error = UiSkinPackageReader.preview(bytes).exceptionOrNull()

        assertNotNull(error)
        assertEquals("皮肤包只能包含 skin-manifest.json 和 assets/ 下的资源", error.message)
    }

    @Test
    fun builtInWinterSkinDeclaresOnlyDecorativeHomeChromeSurfaces() {
        val skin = BuiltInUiSkins.winterCloud

        assertEquals("builtin.winter_cloud", skin.skinId)
        assertEquals(
            setOf(UiSkinSurface.HOME_BOTTOM_BAR, UiSkinSurface.HOME_TOP_CHROME),
            skin.surfaces
        )
        assertTrue(skin.assets.bottomBarTrim != null)
    }

    private fun sampleManifest(
        assets: UiSkinAssets = UiSkinAssets(
            bottomBarTrim = "assets/bottom_trim.png",
            topAtmosphere = "assets/top_atmosphere.webp"
        )
    ): UiSkinManifest {
        return UiSkinManifest(
            formatVersion = 1,
            skinId = "dev.example.winter_cloud",
            displayName = "冬日云朵",
            version = "1.0.0",
            apiVersion = 1,
            author = "BiliPai",
            surfaces = setOf(UiSkinSurface.HOME_BOTTOM_BAR, UiSkinSurface.HOME_TOP_CHROME),
            assets = assets,
            colors = UiSkinColorTokens(
                bottomBarTrimTint = "#EAF8FF",
                topAtmosphereTint = "#DFF5FF"
            )
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

    private fun pngBytes(): ByteArray {
        return byteArrayOf(
            0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A,
            0x00, 0x00, 0x00, 0x0D
        )
    }

    private fun webpBytes(): ByteArray {
        return byteArrayOf(
            0x52, 0x49, 0x46, 0x46,
            0x04, 0x00, 0x00, 0x00,
            0x57, 0x45, 0x42, 0x50
        )
    }

    private fun sha256Hex(bytes: ByteArray): String {
        return MessageDigest.getInstance("SHA-256")
            .digest(bytes)
            .joinToString("") { "%02x".format(it) }
    }
}
