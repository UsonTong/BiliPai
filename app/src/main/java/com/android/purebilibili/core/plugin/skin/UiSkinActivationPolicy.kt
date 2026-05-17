package com.android.purebilibili.core.plugin.skin

import com.android.purebilibili.core.store.HomeSettings

fun resolveUiSkinState(
    selection: UiSkinSelection,
    installedSkins: List<InstalledUiSkinPackage>
): UiSkinState {
    if (!selection.enabled || selection.selectedSkinId.isNullOrBlank()) {
        return UiSkinState()
    }
    val activeSkin = installedSkins.firstOrNull { it.manifest.skinId == selection.selectedSkinId }
        ?: return UiSkinState()
    return UiSkinState(
        enabled = true,
        activeSkin = activeSkin
    )
}

fun resolveUiSkinHomeSettings(
    homeSettings: HomeSettings,
    uiSkinState: UiSkinState
): HomeSettings {
    return homeSettings
}
