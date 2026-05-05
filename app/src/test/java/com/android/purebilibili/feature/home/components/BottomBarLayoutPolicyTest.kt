package com.android.purebilibili.feature.home.components

import androidx.compose.ui.unit.dp
import com.android.purebilibili.core.store.BottomBarSearchAutoExpandMode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BottomBarLayoutPolicyTest {

    @Test
    fun `floating five items keeps compact width with safe per-item size`() {
        val policy = resolveBottomBarLayoutPolicy(
            containerWidth = 393.dp,
            itemCount = 5,
            isTablet = false,
            labelMode = 0,
            isFloating = true
        )

        val perItemWidth = (policy.maxBarWidth - (policy.rowPadding * 2)) / 5
        assertTrue(policy.maxBarWidth.value > 340f)
        assertTrue(policy.horizontalPadding.value < 26f)
        assertTrue(perItemWidth.value >= 52f)
    }

    @Test
    fun `floating four items can use wider bar than five items`() {
        val policyForFour = resolveBottomBarLayoutPolicy(
            containerWidth = 393.dp,
            itemCount = 4,
            isTablet = false,
            labelMode = 0,
            isFloating = true
        )
        val policyForFive = resolveBottomBarLayoutPolicy(
            containerWidth = 393.dp,
            itemCount = 5,
            isTablet = false,
            labelMode = 0,
            isFloating = true
        )

        assertTrue(policyForFour.maxBarWidth.value > policyForFive.maxBarWidth.value)
    }

    @Test
    fun `kernelsu floating width uses intrinsic item width when space allows`() {
        val width = resolveKernelSuFloatingBottomBarWidth(
            containerWidth = 393.dp,
            itemCount = 4,
            minEdgePadding = 20.dp
        )

        assertEquals(312.dp, width)
    }

    @Test
    fun `kernelsu floating width keeps safe edge padding on crowded phones`() {
        val width = resolveKernelSuFloatingBottomBarWidth(
            containerWidth = 393.dp,
            itemCount = 5,
            minEdgePadding = 20.dp
        )

        assertEquals(353.dp, width)
    }

    @Test
    fun `kernelsu search entry shares safe floating width while collapsed`() {
        val layout = resolveKernelSuBottomBarSearchLayout(
            containerWidth = 393.dp,
            itemCount = 4,
            minEdgePadding = 20.dp,
            searchEnabled = true,
            searchExpanded = false
        )

        assertEquals(279.dp, layout.dockWidth)
        assertEquals(64.dp, layout.searchWidth)
        assertEquals(10.dp, layout.gap)
    }

    @Test
    fun `kernelsu search entry collapses dock to home capsule when expanded`() {
        val layout = resolveKernelSuBottomBarSearchLayout(
            containerWidth = 393.dp,
            itemCount = 4,
            minEdgePadding = 20.dp,
            searchEnabled = true,
            searchExpanded = true
        )

        assertEquals(64.dp, layout.dockWidth)
        assertEquals(279.dp, layout.searchWidth)
        assertEquals(10.dp, layout.gap)
    }

    @Test
    fun `kernelsu search capsule is slimmer only when expanded`() {
        assertEquals(64.dp, resolveKernelSuBottomBarSearchHeight(searchExpanded = false))
        assertEquals(58.dp, resolveKernelSuBottomBarSearchHeight(searchExpanded = true))
    }

    @Test
    fun `home top automatically expands bottom search`() {
        assertEquals(
            true,
            shouldAutoExpandBottomBarSearch(
                currentItem = BottomNavItem.HOME,
                bottomBarSearchEnabled = true,
                autoExpandMode = BottomBarSearchAutoExpandMode.EXPAND_AT_HOME_TOP,
                homeScrollOffsetPx = 0f
            )
        )
        assertEquals(
            true,
            shouldAutoExpandBottomBarSearch(
                currentItem = BottomNavItem.HOME,
                bottomBarSearchEnabled = true,
                autoExpandMode = BottomBarSearchAutoExpandMode.EXPAND_AT_HOME_TOP,
                homeScrollOffsetPx = 24f
            )
        )
    }

    @Test
    fun `bottom search auto collapses away from home top in top expand mode`() {
        assertEquals(
            false,
            shouldAutoExpandBottomBarSearch(
                currentItem = BottomNavItem.HOME,
                bottomBarSearchEnabled = true,
                autoExpandMode = BottomBarSearchAutoExpandMode.EXPAND_AT_HOME_TOP,
                homeScrollOffsetPx = 96f
            )
        )
        assertEquals(
            false,
            shouldAutoExpandBottomBarSearch(
                currentItem = BottomNavItem.DYNAMIC,
                bottomBarSearchEnabled = true,
                autoExpandMode = BottomBarSearchAutoExpandMode.EXPAND_AT_HOME_TOP,
                homeScrollOffsetPx = 0f
            )
        )
        assertEquals(
            false,
            shouldAutoExpandBottomBarSearch(
                currentItem = BottomNavItem.HOME,
                bottomBarSearchEnabled = false,
                autoExpandMode = BottomBarSearchAutoExpandMode.EXPAND_AT_HOME_TOP,
                homeScrollOffsetPx = 0f
            )
        )
    }

    @Test
    fun `bottom search auto expands away from home top in scroll expand mode`() {
        assertEquals(
            false,
            shouldAutoExpandBottomBarSearch(
                currentItem = BottomNavItem.HOME,
                bottomBarSearchEnabled = true,
                autoExpandMode = BottomBarSearchAutoExpandMode.EXPAND_WHEN_SCROLLING_DOWN,
                homeScrollOffsetPx = 0f
            )
        )
        assertEquals(
            true,
            shouldAutoExpandBottomBarSearch(
                currentItem = BottomNavItem.HOME,
                bottomBarSearchEnabled = true,
                autoExpandMode = BottomBarSearchAutoExpandMode.EXPAND_WHEN_SCROLLING_DOWN,
                homeScrollOffsetPx = 96f
            )
        )
        assertEquals(
            false,
            shouldAutoExpandBottomBarSearch(
                currentItem = BottomNavItem.DYNAMIC,
                bottomBarSearchEnabled = true,
                autoExpandMode = BottomBarSearchAutoExpandMode.EXPAND_WHEN_SCROLLING_DOWN,
                homeScrollOffsetPx = 96f
            )
        )
    }

    @Test
    fun `home icon click expands collapsed search before scrolling to top`() {
        assertEquals(
            true,
            shouldExpandBottomBarSearchOnNavItemClick(
                clickedItem = BottomNavItem.HOME,
                bottomBarSearchEnabled = true,
                searchExpanded = false
            )
        )
        assertEquals(
            false,
            shouldExpandBottomBarSearchOnNavItemClick(
                clickedItem = BottomNavItem.HOME,
                bottomBarSearchEnabled = true,
                searchExpanded = true
            )
        )
        assertEquals(
            false,
            shouldExpandBottomBarSearchOnNavItemClick(
                clickedItem = BottomNavItem.DYNAMIC,
                bottomBarSearchEnabled = true,
                searchExpanded = false
            )
        )
    }

    @Test
    fun `docked mode stays full width with no horizontal inset`() {
        val policy = resolveBottomBarLayoutPolicy(
            containerWidth = 393.dp,
            itemCount = 5,
            isTablet = false,
            labelMode = 0,
            isFloating = false
        )

        assertEquals(0.dp, policy.horizontalPadding)
        assertEquals(393.dp, policy.maxBarWidth)
    }

    @Test
    fun `floating default bar trims height while keeping touch comfort`() {
        assertEquals(58f, resolveBottomBarFloatingHeightDp(labelMode = 1, isTablet = false))
        assertEquals(12f, resolveBottomBarBottomPaddingDp(isFloating = true, isTablet = false))
    }
}
