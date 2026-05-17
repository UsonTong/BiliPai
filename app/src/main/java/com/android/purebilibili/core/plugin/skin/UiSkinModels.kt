package com.android.purebilibili.core.plugin.skin

import kotlinx.serialization.Serializable

enum class UiSkinSurface {
    HOME_BOTTOM_BAR,
    HOME_TOP_CHROME
}

enum class UiSkinAssetType {
    PNG,
    WEBP,
    JPEG
}

@Serializable
data class UiSkinManifest(
    val formatVersion: Int,
    val skinId: String,
    val displayName: String,
    val version: String,
    val apiVersion: Int,
    val author: String? = null,
    val surfaces: Set<UiSkinSurface>,
    val assets: UiSkinAssets = UiSkinAssets(),
    val colors: UiSkinColorTokens = UiSkinColorTokens(),
    val styleSourceName: String? = null,
    val styleSourceUrl: String? = null,
    val licenseNote: String? = null,
    val communityShareable: Boolean = false,
    val containsOfficialAssets: Boolean = false
)

@Serializable
data class UiSkinAssets(
    val bottomBarTrim: String? = null,
    val topAtmosphere: String? = null,
    val searchCapsuleBackground: String? = null,
    val bottomBarIcons: Map<String, String> = emptyMap()
) {
    fun declaredPaths(): List<String> {
        return buildList {
            bottomBarTrim?.let(::add)
            topAtmosphere?.let(::add)
            searchCapsuleBackground?.let(::add)
            addAll(bottomBarIcons.values)
        }
    }
}

@Serializable
data class UiSkinColorTokens(
    val bottomBarTrimTint: String? = null,
    val topAtmosphereTint: String? = null,
    val searchCapsuleTint: String? = null
)

data class UiSkinAssetEntry(
    val path: String,
    val type: UiSkinAssetType,
    val sizeBytes: Long
)

data class UiSkinPackagePreview(
    val manifest: UiSkinManifest,
    val packageSha256: String,
    val assetEntries: List<UiSkinAssetEntry>
)

@Serializable
data class InstalledUiSkinPackage(
    val manifest: UiSkinManifest,
    val packageSha256: String,
    val packagePath: String,
    val installedAtMillis: Long,
    val enabled: Boolean = false,
    val assetFiles: Map<String, String> = emptyMap()
) {
    val skinId: String
        get() = manifest.skinId

    val displayName: String
        get() = manifest.displayName

    fun assetFilePath(assetPath: String?): String? {
        return assetPath?.let(assetFiles::get)
    }
}

data class UiSkinSelection(
    val enabled: Boolean = false,
    val selectedSkinId: String? = null
)

data class UiSkinState(
    val enabled: Boolean = false,
    val activeSkin: InstalledUiSkinPackage? = null
)

object BuiltInUiSkins {
    val winterCloud = UiSkinManifest(
        formatVersion = 1,
        skinId = "builtin.winter_cloud",
        displayName = "冬日云朵",
        version = "1.0.0",
        apiVersion = 1,
        author = "BiliPai",
        surfaces = setOf(UiSkinSurface.HOME_BOTTOM_BAR, UiSkinSurface.HOME_TOP_CHROME),
        assets = UiSkinAssets(
            bottomBarTrim = "builtin://winter-cloud/bottom-trim",
            topAtmosphere = "builtin://winter-cloud/top-atmosphere"
        ),
        colors = UiSkinColorTokens(
            bottomBarTrimTint = "#EAF8FF",
            topAtmosphereTint = "#DFF5FF",
            searchCapsuleTint = "#FFFFFF"
        ),
        styleSourceName = "BiliPai",
        licenseNote = "BiliPai 原创内置资源",
        communityShareable = false,
        containsOfficialAssets = false
    )

    val winterCloudInstallRecord = InstalledUiSkinPackage(
        manifest = winterCloud,
        packageSha256 = "builtin",
        packagePath = "builtin://winter-cloud",
        installedAtMillis = 0L,
        enabled = false
    )
}
