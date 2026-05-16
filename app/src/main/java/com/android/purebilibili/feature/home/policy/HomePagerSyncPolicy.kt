package com.android.purebilibili.feature.home.policy

import com.android.purebilibili.feature.home.HomeCategory

internal enum class HomePagerSettledAction {
    NONE,
    SWITCH_CATEGORY
}

internal fun shouldDisplayHomeTopCategoryInline(category: HomeCategory?): Boolean {
    return category != null
}

internal fun shouldSwitchHomeCategoryFromPager(
    hasSyncedPagerWithState: Boolean,
    pagerCurrentPage: Int,
    pagerScrolling: Boolean,
    currentCategoryIndex: Int,
    programmaticPageSwitchInProgress: Boolean = false
): Boolean {
    if (!hasSyncedPagerWithState) return false
    if (pagerScrolling) return false
    if (programmaticPageSwitchInProgress) return false
    return pagerCurrentPage != currentCategoryIndex
}

internal fun resolveHomePagerSettledAction(
    hasSyncedPagerWithState: Boolean,
    pagerCurrentPage: Int,
    pagerScrolling: Boolean,
    currentCategoryIndex: Int,
    settledCategory: HomeCategory?,
    programmaticPageSwitchInProgress: Boolean = false
): HomePagerSettledAction {
    if (!shouldSwitchHomeCategoryFromPager(
            hasSyncedPagerWithState = hasSyncedPagerWithState,
            pagerCurrentPage = pagerCurrentPage,
            pagerScrolling = pagerScrolling,
            currentCategoryIndex = currentCategoryIndex,
            programmaticPageSwitchInProgress = programmaticPageSwitchInProgress
        )
    ) {
        return HomePagerSettledAction.NONE
    }

    return if (shouldDisplayHomeTopCategoryInline(settledCategory)) {
        HomePagerSettledAction.SWITCH_CATEGORY
    } else {
        HomePagerSettledAction.NONE
    }
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
