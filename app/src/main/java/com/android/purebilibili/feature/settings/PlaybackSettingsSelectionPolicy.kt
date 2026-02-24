package com.android.purebilibili.feature.settings

import com.android.purebilibili.core.store.SettingsManager

internal data class PlaybackSegmentOption<T>(
    val value: T,
    val label: String
)

internal fun <T> resolveSelectionIndex(
    options: List<PlaybackSegmentOption<T>>,
    selectedValue: T
): Int {
    if (options.isEmpty()) return 0
    val index = options.indexOfFirst { it.value == selectedValue }
    return if (index >= 0) index else 0
}

internal fun <T> resolveSelectionLabel(
    options: List<PlaybackSegmentOption<T>>,
    selectedValue: T,
    fallbackLabel: String
): String {
    return options.find { it.value == selectedValue }?.label ?: fallbackLabel
}

internal fun resolveEffectiveMobileQuality(
    rawMobileQuality: Int,
    isDataSaverActive: Boolean,
    maxQualityWhenSaverActive: Int = 32
): Int {
    if (!isDataSaverActive) return rawMobileQuality
    return rawMobileQuality.coerceAtMost(maxQualityWhenSaverActive)
}

internal fun resolveSegmentedSwipeTargetIndex(
    currentIndex: Int,
    totalDragPx: Float,
    optionCount: Int,
    thresholdPx: Float = 30f
): Int {
    if (optionCount <= 0) return 0
    val boundedCurrent = currentIndex.coerceIn(0, optionCount - 1)
    return when {
        totalDragPx >= thresholdPx -> (boundedCurrent + 1).coerceAtMost(optionCount - 1)
        totalDragPx <= -thresholdPx -> (boundedCurrent - 1).coerceAtLeast(0)
        else -> boundedCurrent
    }
}

internal fun resolveFeedApiSegmentOptions(
    entries: List<SettingsManager.FeedApiType> = SettingsManager.FeedApiType.entries
): List<PlaybackSegmentOption<SettingsManager.FeedApiType>> {
    return entries.map { type ->
        PlaybackSegmentOption(
            value = type,
            label = type.label
        )
    }
}
