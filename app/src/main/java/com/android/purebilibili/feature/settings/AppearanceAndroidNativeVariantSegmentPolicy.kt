package com.android.purebilibili.feature.settings

import com.android.purebilibili.core.theme.AndroidNativeVariant

internal fun resolveAndroidNativeVariantSegmentOptions(
    material3Label: String,
    material3ExpressiveLabel: String,
    miuixLabel: String
): List<PlaybackSegmentOption<AndroidNativeVariant>> {
    return listOf(
        PlaybackSegmentOption(AndroidNativeVariant.MATERIAL3, material3Label),
        PlaybackSegmentOption(AndroidNativeVariant.MATERIAL3_EXPRESSIVE, material3ExpressiveLabel),
        PlaybackSegmentOption(AndroidNativeVariant.MIUIX, miuixLabel)
    )
}
