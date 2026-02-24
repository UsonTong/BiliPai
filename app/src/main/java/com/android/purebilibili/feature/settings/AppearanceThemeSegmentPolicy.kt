package com.android.purebilibili.feature.settings

internal fun resolveThemeModeSegmentOptions(): List<PlaybackSegmentOption<AppThemeMode>> {
    return listOf(
        PlaybackSegmentOption(AppThemeMode.FOLLOW_SYSTEM, AppThemeMode.FOLLOW_SYSTEM.label),
        PlaybackSegmentOption(AppThemeMode.LIGHT, AppThemeMode.LIGHT.label),
        PlaybackSegmentOption(AppThemeMode.DARK, AppThemeMode.DARK.label)
    )
}
