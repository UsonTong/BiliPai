package com.android.purebilibili.core.plugin.skin

import android.content.Context
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

class UiSkinInstallStore(
    private val rootDir: File,
    private val clock: () -> Long = { System.currentTimeMillis() }
) {
    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
    }

    fun previewPackage(packageBytes: ByteArray): Result<UiSkinPackagePreview> {
        return UiSkinPackageReader.preview(packageBytes)
    }

    fun installPreview(
        preview: UiSkinPackagePreview,
        packageBytes: ByteArray
    ): Result<InstalledUiSkinPackage> {
        return runCatching {
            val verifiedPreview = previewPackage(packageBytes).getOrThrow()
            if (verifiedPreview.packageSha256 != preview.packageSha256) {
                throw IllegalArgumentException("皮肤包内容与预览 SHA-256 不一致")
            }
            val packageFile = packageFile(preview.manifest.skinId, preview.packageSha256)
            packageFile.parentFile?.mkdirs()
            packageFile.writeBytes(packageBytes)
            val installed = InstalledUiSkinPackage(
                manifest = preview.manifest,
                packageSha256 = preview.packageSha256,
                packagePath = packageFile.absolutePath,
                installedAtMillis = clock(),
                enabled = false
            )
            writeJson(installedFile(preview.manifest.skinId), installed)
            installed
        }
    }

    fun listInstalledPackages(): List<InstalledUiSkinPackage> {
        val external = installedDir()
            .listFiles { file -> file.extension == "json" }
            ?.sortedBy { it.name }
            ?.mapNotNull { file ->
                runCatching {
                    json.decodeFromString(InstalledUiSkinPackage.serializer(), file.readText())
                }.getOrNull()
            }
            ?: emptyList()
        return listOf(BuiltInUiSkins.winterCloudInstallRecord) + external
    }

    companion object {
        fun createDefault(context: Context): UiSkinInstallStore {
            return UiSkinInstallStore(
                rootDir = File(context.filesDir, "ui_skins")
            )
        }
    }

    private fun packageFile(skinId: String, packageSha256: String): File {
        return File(packageDir(skinId), "$packageSha256.bpskin")
    }

    private fun packageDir(skinId: String): File {
        return File(packagesDir(), skinId.safeFileSegment())
    }

    private fun packagesDir(): File = File(rootDir, "packages")

    private fun installedDir(): File = File(rootDir, "installed").also { it.mkdirs() }

    private fun installedFile(skinId: String): File {
        return File(installedDir(), "${skinId.safeFileSegment()}.json")
    }

    private inline fun <reified T> writeJson(file: File, value: T) {
        file.parentFile?.mkdirs()
        file.writeText(json.encodeToString(value))
    }
}

private fun String.safeFileSegment(): String {
    return replace(Regex("[^A-Za-z0-9_.-]"), "_")
}
