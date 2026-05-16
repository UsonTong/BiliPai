package com.android.purebilibili.feature.home.components

import kotlin.math.roundToInt

internal const val HOME_HEADER_SECONDARY_BLUR_RESTORE_DELAY_MS = 120L

enum class HomeInteractionMotionBudget {
    FULL,
    REDUCED
}

internal data class TopTabScrollTarget(
    val firstVisibleItemIndex: Int,
    val firstVisibleItemScrollOffsetPx: Int
)

internal fun resolveHomeTopTabViewportSyncEnabled(
    currentTabHeightDp: Float,
    tabAlpha: Float,
    tabContentAlpha: Float,
    minVisibleHeightDp: Float = 1f,
    minVisibleAlpha: Float = 0.01f
): Boolean {
    return currentTabHeightDp > minVisibleHeightDp &&
        tabAlpha > minVisibleAlpha &&
        tabContentAlpha > minVisibleAlpha
}

internal fun resolveHomeInteractionMotionBudget(
    isPagerScrolling: Boolean,
    isProgrammaticPageSwitchInProgress: Boolean,
    isFeedScrolling: Boolean
): HomeInteractionMotionBudget {
    return if (isPagerScrolling || isProgrammaticPageSwitchInProgress || isFeedScrolling) {
        HomeInteractionMotionBudget.REDUCED
    } else {
        HomeInteractionMotionBudget.FULL
    }
}

internal fun shouldAnimateTopTabAutoScroll(
    selectedIndex: Int,
    firstVisibleIndex: Int,
    lastVisibleIndex: Int,
    budget: HomeInteractionMotionBudget
): Boolean {
    if (firstVisibleIndex > lastVisibleIndex) return true
    val isTargetOutsideViewport = selectedIndex < firstVisibleIndex || selectedIndex > lastVisibleIndex
    if (budget == HomeInteractionMotionBudget.REDUCED) {
        return isTargetOutsideViewport
    }
    return isTargetOutsideViewport
}

internal fun shouldSyncHomeTopTabViewport(
    pagerIsScrolling: Boolean,
    targetIsOutsideViewport: Boolean
): Boolean {
    return !pagerIsScrolling || targetIsOutsideViewport
}

internal fun resolveTopTabViewportAnchorIndex(
    selectedIndex: Int,
    pagerCurrentPage: Int?,
    pagerTargetPage: Int?,
    pagerIsScrolling: Boolean
): Int {
    if (!pagerIsScrolling) return pagerCurrentPage ?: selectedIndex
    return pagerTargetPage ?: pagerCurrentPage ?: selectedIndex
}

internal fun resolveTopTabPagerPosition(
    selectedIndex: Int,
    pagerCurrentPage: Int?,
    pagerTargetPage: Int?,
    pagerCurrentPageOffsetFraction: Float?,
    pagerIsScrolling: Boolean
): Float {
    if (!pagerIsScrolling) return (pagerCurrentPage ?: selectedIndex).toFloat()
    val currentPage = pagerCurrentPage ?: return selectedIndex.toFloat()
    val offsetFraction = pagerCurrentPageOffsetFraction ?: 0f
    val targetPage = pagerTargetPage
    if (targetPage != null && targetPage != currentPage) {
        val direction = if (targetPage > currentPage) 1f else -1f
        val progress = kotlin.math.abs(offsetFraction).coerceIn(0f, 1f)
        return currentPage + direction * progress
    }
    // currentPageOffsetFraction changes immediately during a drag. Compose reports
    // forward page movement as a positive offset, so adding it keeps the capsule
    // attached to the finger instead of mirroring the swipe direction.
    return currentPage + offsetFraction
}

internal fun resolveTopTabIndicatorRenderPosition(
    selectedIndex: Int,
    pagerCurrentPage: Int?,
    pagerTargetPage: Int?,
    pagerCurrentPageOffsetFraction: Float?,
    pagerIsScrolling: Boolean
): Float {
    return resolveTopTabPagerPosition(
        selectedIndex = selectedIndex,
        pagerCurrentPage = pagerCurrentPage,
        pagerTargetPage = pagerTargetPage,
        pagerCurrentPageOffsetFraction = pagerCurrentPageOffsetFraction,
        pagerIsScrolling = pagerIsScrolling
    )
}

internal fun resolveTopTabFollowScrollTarget(
    indicatorPosition: Float,
    itemWidthPx: Float,
    itemCount: Int,
    viewportWidthPx: Float,
    currentFirstVisibleItemIndex: Int,
    currentFirstVisibleItemScrollOffsetPx: Int,
    maxScrollPx: Float,
    edgeBufferPx: Float
): TopTabScrollTarget {
    val currentTarget = TopTabScrollTarget(
        firstVisibleItemIndex = currentFirstVisibleItemIndex.coerceAtLeast(0),
        firstVisibleItemScrollOffsetPx = currentFirstVisibleItemScrollOffsetPx.coerceAtLeast(0)
    )
    if (itemWidthPx <= 0f || itemCount <= 0 || viewportWidthPx <= 0f || maxScrollPx <= 0f) {
        return currentTarget
    }

    val clampedPosition = indicatorPosition.coerceIn(0f, (itemCount - 1).toFloat())
    val selectedItemIndex = clampedPosition.roundToInt().coerceIn(0, itemCount - 1)
    val usableViewportWidthPx = (viewportWidthPx - edgeBufferPx.coerceAtLeast(0f) * 2f)
        .coerceAtLeast(itemWidthPx)
    val visibleSlots = (usableViewportWidthPx / itemWidthPx).toInt().coerceAtLeast(1)
    val centerSlotIndex = (visibleSlots / 2).coerceAtLeast(0)
    val maxFirstVisibleByCount = (itemCount - visibleSlots).coerceAtLeast(0)
    val maxFirstVisibleByScroll = (maxScrollPx / itemWidthPx).toInt().coerceAtLeast(0)
    val maxFirstVisibleIndex = minOf(maxFirstVisibleByCount, maxFirstVisibleByScroll)
    val targetIndex = (selectedItemIndex - centerSlotIndex)
        .coerceIn(0, maxFirstVisibleIndex)

    return TopTabScrollTarget(
        firstVisibleItemIndex = targetIndex,
        firstVisibleItemScrollOffsetPx = 0
    )
}

internal fun resolveMd3TopTabViewportPosition(
    visibleIndices: List<Int>,
    absolutePagerPosition: Float
): Float {
    if (visibleIndices.isEmpty()) return 0f
    if (visibleIndices.size == 1) return 0f

    val firstIndex = visibleIndices.first().toFloat()
    val lastIndex = visibleIndices.last().toFloat()
    if (absolutePagerPosition <= firstIndex) return 0f
    if (absolutePagerPosition >= lastIndex) return visibleIndices.lastIndex.toFloat()

    visibleIndices.zipWithNext().forEachIndexed { slotIndex, (start, end) ->
        val startFloat = start.toFloat()
        val endFloat = end.toFloat()
        if (absolutePagerPosition in startFloat..endFloat) {
            val span = (endFloat - startFloat).coerceAtLeast(0.0001f)
            val fraction = (absolutePagerPosition - startFloat) / span
            return slotIndex + fraction
        }
    }

    return visibleIndices.indexOfLast { it.toFloat() <= absolutePagerPosition }
        .coerceAtLeast(0)
        .toFloat()
}

internal fun resolveMd3TopTabIndicatorTranslationPx(
    absolutePagerPosition: Float,
    itemWidthPx: Float,
    rowScrollOffsetPx: Float,
    indicatorWidthPx: Float,
    contentPaddingPx: Float = 0f
): Float {
    if (itemWidthPx <= 0f || indicatorWidthPx <= 0f) return contentPaddingPx
    val indicatorCenterPx = contentPaddingPx + (absolutePagerPosition * itemWidthPx) + (itemWidthPx / 2f)
    return indicatorCenterPx - rowScrollOffsetPx - (indicatorWidthPx / 2f)
}
