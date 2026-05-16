package com.android.purebilibili.feature.home.components

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class HomeInteractionMotionBudgetPolicyTest {

    @Test
    fun activePagerOrFeedMotion_reducesHomeBudget() {
        assertEquals(
            HomeInteractionMotionBudget.REDUCED,
            resolveHomeInteractionMotionBudget(
                isPagerScrolling = true,
                isProgrammaticPageSwitchInProgress = false,
                isFeedScrolling = false
            )
        )
        assertEquals(
            HomeInteractionMotionBudget.REDUCED,
            resolveHomeInteractionMotionBudget(
                isPagerScrolling = false,
                isProgrammaticPageSwitchInProgress = true,
                isFeedScrolling = false
            )
        )
        assertEquals(
            HomeInteractionMotionBudget.REDUCED,
            resolveHomeInteractionMotionBudget(
                isPagerScrolling = false,
                isProgrammaticPageSwitchInProgress = false,
                isFeedScrolling = true
            )
        )
    }

    @Test
    fun idleHomeState_keepsFullBudget() {
        assertEquals(
            HomeInteractionMotionBudget.FULL,
            resolveHomeInteractionMotionBudget(
                isPagerScrolling = false,
                isProgrammaticPageSwitchInProgress = false,
                isFeedScrolling = false
            )
        )
    }

    @Test
    fun reducedBudget_onlyAutoScrollsTabsWhenTargetIsOutOfViewport() {
        assertFalse(
            shouldAnimateTopTabAutoScroll(
                selectedIndex = 2,
                firstVisibleIndex = 0,
                lastVisibleIndex = 4,
                budget = HomeInteractionMotionBudget.REDUCED
            )
        )
        assertTrue(
            shouldAnimateTopTabAutoScroll(
                selectedIndex = 5,
                firstVisibleIndex = 0,
                lastVisibleIndex = 4,
                budget = HomeInteractionMotionBudget.REDUCED
            )
        )
    }

    @Test
    fun fullBudget_keepsLeadingTabsVisibleWhenSelectionAlreadyInViewport() {
        assertFalse(
            shouldAnimateTopTabAutoScroll(
                selectedIndex = 1,
                firstVisibleIndex = 0,
                lastVisibleIndex = 3,
                budget = HomeInteractionMotionBudget.FULL
            )
        )
    }

    @Test
    fun collapsedTopTabs_disableViewportSync() {
        assertFalse(
            resolveHomeTopTabViewportSyncEnabled(
                currentTabHeightDp = 0f,
                tabAlpha = 1f,
                tabContentAlpha = 1f
            )
        )
        assertFalse(
            resolveHomeTopTabViewportSyncEnabled(
                currentTabHeightDp = 52f,
                tabAlpha = 0f,
                tabContentAlpha = 1f
            )
        )
    }

    @Test
    fun visibleTopTabs_keepViewportSyncEnabled() {
        assertTrue(
            resolveHomeTopTabViewportSyncEnabled(
                currentTabHeightDp = 52f,
                tabAlpha = 1f,
                tabContentAlpha = 1f
            )
        )
    }

    @Test
    fun pagerSwipe_skipsTopTabViewportSyncWhenTargetRemainsVisible() {
        assertFalse(
            shouldSyncHomeTopTabViewport(
                pagerIsScrolling = true,
                targetIsOutsideViewport = false
            )
        )
        assertTrue(
            shouldSyncHomeTopTabViewport(
                pagerIsScrolling = true,
                targetIsOutsideViewport = true
            )
        )
    }

    @Test
    fun idlePager_allowsTopTabViewportSettleCorrection() {
        assertTrue(
            shouldSyncHomeTopTabViewport(
                pagerIsScrolling = false,
                targetIsOutsideViewport = false
            )
        )
    }

    @Test
    fun activePagerSwipe_prefersPagerTargetForTopTabViewportAnchor() {
        assertEquals(
            4,
            resolveTopTabViewportAnchorIndex(
                selectedIndex = 2,
                pagerCurrentPage = 2,
                pagerTargetPage = 4,
                pagerIsScrolling = true
            )
        )
    }

    @Test
    fun idlePagerViewportAnchor_prefersSettledPagerPage() {
        assertEquals(
            3,
            resolveTopTabViewportAnchorIndex(
                selectedIndex = 1,
                pagerCurrentPage = 3,
                pagerTargetPage = 4,
                pagerIsScrolling = false
            )
        )
    }

    @Test
    fun pagerSwipePosition_tracksTargetPageContinuously() {
        assertEquals(
            0.35f,
            resolveTopTabPagerPosition(
                selectedIndex = 0,
                pagerCurrentPage = 0,
                pagerTargetPage = 1,
                pagerCurrentPageOffsetFraction = 0.35f,
                pagerIsScrolling = true
            )
        )
        assertEquals(
            0.65f,
            resolveTopTabPagerPosition(
                selectedIndex = 1,
                pagerCurrentPage = 1,
                pagerTargetPage = 0,
                pagerCurrentPageOffsetFraction = -0.35f,
                pagerIsScrolling = true
            )
        )
    }

    @Test
    fun pagerSwipePosition_usesTargetDirectionWhenPagerOffsetSignIsOpposite() {
        assertEquals(
            3.35f,
            resolveTopTabPagerPosition(
                selectedIndex = 3,
                pagerCurrentPage = 3,
                pagerTargetPage = 4,
                pagerCurrentPageOffsetFraction = -0.35f,
                pagerIsScrolling = true
            )
        )
        assertEquals(
            3.65f,
            resolveTopTabPagerPosition(
                selectedIndex = 4,
                pagerCurrentPage = 4,
                pagerTargetPage = 3,
                pagerCurrentPageOffsetFraction = 0.35f,
                pagerIsScrolling = true
            )
        )
    }

    @Test
    fun pagerSwipePosition_tracksOffsetBeforeTargetPageChanges() {
        assertEquals(
            0.35f,
            resolveTopTabPagerPosition(
                selectedIndex = 0,
                pagerCurrentPage = 0,
                pagerTargetPage = 0,
                pagerCurrentPageOffsetFraction = 0.35f,
                pagerIsScrolling = true
            )
        )
        assertEquals(
            0.65f,
            resolveTopTabPagerPosition(
                selectedIndex = 1,
                pagerCurrentPage = 1,
                pagerTargetPage = 1,
                pagerCurrentPageOffsetFraction = -0.35f,
                pagerIsScrolling = true
            )
        )
    }

    @Test
    fun idlePagerPosition_prefersSettledPagerPage() {
        assertEquals(
            1f,
            resolveTopTabPagerPosition(
                selectedIndex = 2,
                pagerCurrentPage = 1,
                pagerTargetPage = 3,
                pagerCurrentPageOffsetFraction = 0.4f,
                pagerIsScrolling = false
            )
        )
    }

    @Test
    fun topTabIndicatorRenderPosition_tracksPagerOffsetWhileUserSwipesContent() {
        assertEquals(
            0.35f,
            resolveTopTabIndicatorRenderPosition(
                selectedIndex = 0,
                pagerCurrentPage = 0,
                pagerTargetPage = 1,
                pagerCurrentPageOffsetFraction = 0.35f,
                pagerIsScrolling = true
            )
        )
    }

    @Test
    fun topTabIndicatorRenderPosition_prefersSettledPagerPageWhenIdle() {
        assertEquals(
            2f,
            resolveTopTabIndicatorRenderPosition(
                selectedIndex = 0,
                pagerCurrentPage = 2,
                pagerTargetPage = 3,
                pagerCurrentPageOffsetFraction = 0.4f,
                pagerIsScrolling = false
            )
        )
    }

    @Test
    fun topTabSweepSelection_usesReleasePositionWithoutMovingIndicatorLive() {
        assertEquals(
            2,
            resolveTopTabSweepSelectionIndex(
                pointerX = 132f,
                rowScrollOffsetPx = 0f,
                itemWidthPx = 64f,
                itemCount = 5
            )
        )
        assertEquals(
            4,
            resolveTopTabSweepSelectionIndex(
                pointerX = 196f,
                rowScrollOffsetPx = 96f,
                itemWidthPx = 64f,
                itemCount = 5
            )
        )
        assertEquals(
            0,
            resolveTopTabSweepSelectionIndex(
                pointerX = -16f,
                rowScrollOffsetPx = 0f,
                itemWidthPx = 64f,
                itemCount = 5
            )
        )
    }

    @Test
    fun md3TopTabViewportPosition_matchesPagerProgressWithinVisibleSlots() {
        assertEquals(
            1.35f,
            resolveMd3TopTabViewportPosition(
                visibleIndices = listOf(0, 1, 2, 3),
                absolutePagerPosition = 1.35f
            ),
            0.001f
        )
    }

    @Test
    fun md3TopTabViewportPosition_interpolatesAcrossPinnedTailSlot() {
        assertEquals(
            2.6f,
            resolveMd3TopTabViewportPosition(
                visibleIndices = listOf(0, 1, 2, 4),
                absolutePagerPosition = 3.2f
            ),
            0.001f
        )
    }
}
