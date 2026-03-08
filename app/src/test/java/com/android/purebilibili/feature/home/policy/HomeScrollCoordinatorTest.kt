package com.android.purebilibili.feature.home.policy

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class HomeScrollCoordinatorTest {

    @Test
    fun preScroll_updatesHeaderOffsetWithinBounds() {
        val result = reduceHomePreScroll(
            currentHeaderOffsetPx = -40f,
            deltaY = -80f,
            minHeaderOffsetPx = -120f,
            isHeaderCollapseEnabled = true,
            isBottomBarAutoHideEnabled = false,
            useSideNavigation = false,
            liquidGlassEnabled = false,
            currentGlobalScrollOffset = 10f
        )

        assertEquals(-120f, result.headerOffsetPx)
        assertNull(result.bottomBarVisibilityIntent)
        assertNull(result.globalScrollOffset)
    }

    @Test
    fun headerCollapseDisabled_resetsOffsetToZero() {
        val result = reduceHomePreScroll(
            currentHeaderOffsetPx = -64f,
            deltaY = -12f,
            minHeaderOffsetPx = -120f,
            isHeaderCollapseEnabled = false,
            isBottomBarAutoHideEnabled = false,
            useSideNavigation = false,
            liquidGlassEnabled = false,
            currentGlobalScrollOffset = 40f
        )

        assertEquals(0f, result.headerOffsetPx)
    }

    @Test
    fun bottomBarAutoHideDisabled_returnsNoIntent() {
        val result = reduceHomePreScroll(
            currentHeaderOffsetPx = 0f,
            deltaY = -48f,
            minHeaderOffsetPx = -120f,
            isHeaderCollapseEnabled = true,
            isBottomBarAutoHideEnabled = false,
            useSideNavigation = false,
            liquidGlassEnabled = false,
            currentGlobalScrollOffset = 10f
        )

        assertNull(result.bottomBarVisibilityIntent)
    }

    @Test
    fun tinyDelta_doesNotToggleBottomBarVisibility() {
        val result = reduceHomePreScroll(
            currentHeaderOffsetPx = 0f,
            deltaY = -4f,
            minHeaderOffsetPx = -120f,
            isHeaderCollapseEnabled = true,
            isBottomBarAutoHideEnabled = true,
            useSideNavigation = false,
            liquidGlassEnabled = false,
            currentGlobalScrollOffset = 10f,
            bottomBarVisibilityThresholdPx = 10f
        )

        assertNull(result.bottomBarVisibilityIntent)
    }

    @Test
    fun upwardScroll_hidesBottomBarWhenAutoHideEnabled() {
        val result = reduceHomePreScroll(
            currentHeaderOffsetPx = 0f,
            deltaY = -24f,
            minHeaderOffsetPx = -120f,
            isHeaderCollapseEnabled = true,
            isBottomBarAutoHideEnabled = true,
            useSideNavigation = false,
            liquidGlassEnabled = false,
            currentGlobalScrollOffset = 10f
        )

        assertEquals(BottomBarVisibilityIntent.HIDE, result.bottomBarVisibilityIntent)
    }

    @Test
    fun liquidGlassEnabled_updatesGlobalOffset() {
        val result = reduceHomePreScroll(
            currentHeaderOffsetPx = 0f,
            deltaY = -8f,
            minHeaderOffsetPx = -120f,
            isHeaderCollapseEnabled = true,
            isBottomBarAutoHideEnabled = false,
            useSideNavigation = false,
            liquidGlassEnabled = true,
            currentGlobalScrollOffset = 120f
        )

        assertEquals(128f, result.globalScrollOffset)
    }
}
