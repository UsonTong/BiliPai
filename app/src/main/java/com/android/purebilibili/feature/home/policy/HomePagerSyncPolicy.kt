package com.android.purebilibili.feature.home.policy

internal fun shouldSwitchHomeCategoryFromPager(
    hasSyncedPagerWithState: Boolean,
    pagerCurrentPage: Int,
    pagerScrolling: Boolean,
    currentCategoryIndex: Int
): Boolean {
    if (!hasSyncedPagerWithState) return false
    if (pagerScrolling) return false
    return pagerCurrentPage != currentCategoryIndex
}

internal fun shouldUseInitialHomePagerSnap(
    hasSyncedPagerWithState: Boolean,
    targetPage: Int
): Boolean {
    return !hasSyncedPagerWithState && targetPage >= 0
}

internal fun shouldAnimateHomePagerToCategory(
    hasSyncedPagerWithState: Boolean,
    targetPage: Int,
    pagerCurrentPage: Int,
    pagerScrolling: Boolean,
    programmaticPageSwitchInProgress: Boolean
): Boolean {
    if (!hasSyncedPagerWithState) return false
    if (targetPage < 0) return false
    if (targetPage == pagerCurrentPage) return false
    if (pagerScrolling) return false
    if (programmaticPageSwitchInProgress) return false
    return true
}
