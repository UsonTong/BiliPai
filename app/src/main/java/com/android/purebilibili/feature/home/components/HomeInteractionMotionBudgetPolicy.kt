package com.android.purebilibili.feature.home.components

internal const val HOME_HEADER_SECONDARY_BLUR_RESTORE_DELAY_MS = 120L

enum class HomeInteractionMotionBudget {
    FULL,
    REDUCED
}

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

internal fun shouldSnapHomeTopTabSelection(
    currentPage: Int,
    targetPage: Int
): Boolean = currentPage != targetPage

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
    // PagerState.targetPage only changes after the drag crosses the settle threshold.
    // currentPageOffsetFraction changes immediately, so deriving the absolute page
    // position from it keeps the top capsule visually attached to the finger.
    return currentPage - offsetFraction
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
