package com.android.purebilibili.feature.home.components

internal const val HOME_HEADER_SECONDARY_BLUR_RESTORE_DELAY_MS = 120L

enum class HomeInteractionMotionBudget {
    FULL,
    REDUCED
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
    if (budget == HomeInteractionMotionBudget.REDUCED) {
        return selectedIndex < firstVisibleIndex || selectedIndex > lastVisibleIndex
    }
    return true
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
    if (!pagerIsScrolling) return selectedIndex
    return pagerTargetPage ?: pagerCurrentPage ?: selectedIndex
}
