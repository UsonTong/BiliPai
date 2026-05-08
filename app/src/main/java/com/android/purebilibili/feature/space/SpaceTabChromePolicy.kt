package com.android.purebilibili.feature.space

import kotlin.math.roundToInt

internal data class SpaceSegmentedTabChromeSpec(
    val selectedIndex: Int,
    val heightDp: Int,
    val indicatorHeightDp: Int,
    val horizontalPaddingDp: Int,
    val itemWidthDp: Int?,
    val scrollable: Boolean,
    val liquidGlassEffectsEnabled: Boolean,
    val dragSelectionEnabled: Boolean
)

private const val SPACE_SEGMENTED_TAB_HEIGHT_DP = 58
private const val SPACE_SEGMENTED_TAB_INDICATOR_HEIGHT_DP = 56
private const val SPACE_CONTRIBUTION_TAB_HEIGHT_DP = 48
private const val SPACE_CONTRIBUTION_TAB_INDICATOR_HEIGHT_DP = 38
private const val SPACE_SEGMENTED_TAB_HORIZONTAL_PADDING_DP = 16
private const val SPACE_SCROLLABLE_CONTRIBUTION_ITEM_MIN_WIDTH_DP = 104
private const val SPACE_SCROLLABLE_CONTRIBUTION_ITEM_TEXT_PADDING_DP = 44
private const val SPACE_SCROLLABLE_CONTRIBUTION_CJK_CHAR_WIDTH_DP = 15
private const val SPACE_SCROLLABLE_CONTRIBUTION_ASCII_CHAR_WIDTH_DP = 8

internal fun resolveSpaceMainTabChromeSpec(
    tabs: List<SpaceMainTabItem>,
    selectedTab: SpaceMainTab
): SpaceSegmentedTabChromeSpec {
    val selectedIndex = tabs.indexOfFirst { it.tab == selectedTab }.coerceAtLeast(0)
    return SpaceSegmentedTabChromeSpec(
        selectedIndex = selectedIndex,
        heightDp = SPACE_SEGMENTED_TAB_HEIGHT_DP,
        indicatorHeightDp = SPACE_SEGMENTED_TAB_INDICATOR_HEIGHT_DP,
        horizontalPaddingDp = SPACE_SEGMENTED_TAB_HORIZONTAL_PADDING_DP,
        itemWidthDp = null,
        scrollable = false,
        liquidGlassEffectsEnabled = true,
        dragSelectionEnabled = true
    )
}

internal fun resolveSpaceContributionTabChromeSpec(
    tabs: List<SpaceContributionTab>,
    selectedTabId: String,
    selectedSubTab: SpaceSubTab
): SpaceSegmentedTabChromeSpec {
    val selectedIndex = tabs.indexOfFirst { it.id == selectedTabId }
        .takeIf { it >= 0 }
        ?: tabs.indexOfFirst { it.subTab == selectedSubTab }.coerceAtLeast(0)
    val scrollable = shouldScrollSpaceContributionTabs(tabs)
    return SpaceSegmentedTabChromeSpec(
        selectedIndex = selectedIndex,
        heightDp = SPACE_CONTRIBUTION_TAB_HEIGHT_DP,
        indicatorHeightDp = SPACE_CONTRIBUTION_TAB_INDICATOR_HEIGHT_DP,
        horizontalPaddingDp = SPACE_SEGMENTED_TAB_HORIZONTAL_PADDING_DP,
        itemWidthDp = resolveSpaceContributionTabItemWidthDp(tabs),
        scrollable = scrollable,
        liquidGlassEffectsEnabled = true,
        dragSelectionEnabled = !scrollable
    )
}

private fun shouldScrollSpaceContributionTabs(tabs: List<SpaceContributionTab>): Boolean {
    return tabs.size > 3
}

internal fun resolveSpaceContributionTabItemWidthDp(tabs: List<SpaceContributionTab>): Int {
    val widestTitle = tabs.maxOfOrNull { estimateSpaceContributionTabTitleWidthDp(it.title) } ?: 0
    return widestTitle.coerceAtLeast(SPACE_SCROLLABLE_CONTRIBUTION_ITEM_MIN_WIDTH_DP)
}

internal fun resolveSpaceContributionTabCenteredScrollOffsetPx(
    selectedIndex: Int,
    itemWidthPx: Float,
    viewportWidthPx: Float
): Int {
    if (selectedIndex <= 0 || itemWidthPx <= 0f || viewportWidthPx <= 0f) return 0
    val itemStartPx = selectedIndex * itemWidthPx
    return (itemStartPx - (viewportWidthPx - itemWidthPx) / 2f)
        .roundToInt()
        .coerceAtLeast(0)
}

private fun estimateSpaceContributionTabTitleWidthDp(title: String): Int {
    val textWidth = title.sumOf { char ->
        if (char.code in 0..127) {
            SPACE_SCROLLABLE_CONTRIBUTION_ASCII_CHAR_WIDTH_DP
        } else {
            SPACE_SCROLLABLE_CONTRIBUTION_CJK_CHAR_WIDTH_DP
        }
    }
    return textWidth + SPACE_SCROLLABLE_CONTRIBUTION_ITEM_TEXT_PADDING_DP
}
