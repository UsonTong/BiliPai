package com.android.purebilibili.navigation

import com.android.purebilibili.feature.home.components.BottomNavItem
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AppTopLevelNavigationPolicyTest {

    @Test
    fun returnsSkip_whenCurrentRouteAlreadyMatchesTarget() {
        val action = resolveTopLevelNavigationAction(
            currentRoute = ScreenRoutes.Profile.route,
            targetRoute = ScreenRoutes.Profile.route,
            hasTargetInBackStack = true
        )

        assertEquals(TopLevelNavigationAction.SKIP, action)
    }

    @Test
    fun returnsPopExisting_whenTargetExistsInBackStack() {
        val action = resolveTopLevelNavigationAction(
            currentRoute = ScreenRoutes.History.route,
            targetRoute = ScreenRoutes.Profile.route,
            hasTargetInBackStack = true
        )

        assertEquals(TopLevelNavigationAction.POP_EXISTING, action)
    }

    @Test
    fun returnsNavigateWithRestore_whenTargetNotInBackStack() {
        val action = resolveTopLevelNavigationAction(
            currentRoute = ScreenRoutes.History.route,
            targetRoute = ScreenRoutes.Profile.route,
            hasTargetInBackStack = false
        )

        assertEquals(TopLevelNavigationAction.NAVIGATE_WITH_RESTORE, action)
    }

    @Test
    fun selectedBottomBarTap_requestsReselect_insteadOfNavigate() {
        val action = resolveBottomBarSelectionAction(
            currentItem = BottomNavItem.HOME,
            tappedItem = BottomNavItem.HOME
        )

        assertEquals(BottomBarSelectionAction.RESELECT, action)
    }

    @Test
    fun matchingHistoryBottomBarTap_alsoUsesReselectAction() {
        assertEquals(
            BottomBarSelectionAction.RESELECT,
            resolveBottomBarSelectionAction(
                currentItem = BottomNavItem.HISTORY,
                tappedItem = BottomNavItem.HISTORY
            )
        )
    }

    @Test
    fun nonReselectBottomBarTap_keepsNavigateAction() {
        assertEquals(
            BottomBarSelectionAction.NAVIGATE,
            resolveBottomBarSelectionAction(
                currentItem = BottomNavItem.HISTORY,
                tappedItem = BottomNavItem.HOME
            )
        )
        assertEquals(
            BottomBarSelectionAction.NAVIGATE,
            resolveBottomBarSelectionAction(
                currentItem = BottomNavItem.HOME,
                tappedItem = BottomNavItem.DYNAMIC
            )
        )
    }

    @Test
    fun systemBackFromRetainedBottomTab_returnsToHomeBeforeFinishingActivity() {
        assertEquals(
            AppSystemBackAction.RETURN_TO_HOME_TAB,
            resolveAppSystemBackAction(
                currentRoute = ScreenRoutes.Home.route,
                currentBottomItem = BottomNavItem.FAVORITE,
                hasPreviousBackStackEntry = false
            )
        )
        assertEquals(
            AppSystemBackAction.RETURN_TO_HOME_TAB,
            resolveAppSystemBackAction(
                currentRoute = ScreenRoutes.Home.route,
                currentBottomItem = BottomNavItem.HISTORY,
                hasPreviousBackStackEntry = true
            )
        )
    }

    @Test
    fun predictiveBackStillInterceptsRetainedBottomTabReturn() {
        assertTrue(
            shouldInterceptSystemBackForAppAction(
                predictiveBackAnimationEnabled = true,
                action = AppSystemBackAction.RETURN_TO_HOME_TAB
            )
        )
        assertFalse(
            shouldInterceptSystemBackForAppAction(
                predictiveBackAnimationEnabled = true,
                action = AppSystemBackAction.NAVIGATE_UP
            )
        )
        assertTrue(
            shouldInterceptSystemBackForAppAction(
                predictiveBackAnimationEnabled = false,
                action = AppSystemBackAction.NAVIGATE_UP
            )
        )
    }

    @Test
    fun systemBackOnHomeTab_usesBackStackOrFinishesActivity() {
        assertEquals(
            AppSystemBackAction.NAVIGATE_UP,
            resolveAppSystemBackAction(
                currentRoute = ScreenRoutes.Home.route,
                currentBottomItem = BottomNavItem.HOME,
                hasPreviousBackStackEntry = true
            )
        )
        assertEquals(
            AppSystemBackAction.FINISH_ACTIVITY,
            resolveAppSystemBackAction(
                currentRoute = ScreenRoutes.Home.route,
                currentBottomItem = BottomNavItem.HOME,
                hasPreviousBackStackEntry = false
            )
        )
    }

    @Test
    fun visibleBottomTabRoute_mapsToPagerPage() {
        val visibleItems = listOf(
            BottomNavItem.HOME,
            BottomNavItem.DYNAMIC,
            BottomNavItem.HISTORY,
            BottomNavItem.PROFILE
        )

        assertEquals(
            1,
            resolveBottomPagerPageForRoute(
                route = ScreenRoutes.Dynamic.route,
                visibleItems = visibleItems
            )
        )
        assertEquals(
            2,
            resolveBottomPagerPageForRoute(
                route = ScreenRoutes.History.route,
                visibleItems = visibleItems
            )
        )
    }

    @Test
    fun secondaryRoute_doesNotMapToBottomPagerPage() {
        val visibleItems = listOf(
            BottomNavItem.HOME,
            BottomNavItem.DYNAMIC,
            BottomNavItem.HISTORY,
            BottomNavItem.PROFILE
        )

        assertNull(
            resolveBottomPagerPageForRoute(
                route = ScreenRoutes.Search.route,
                visibleItems = visibleItems
            )
        )
        assertNull(
            resolveBottomPagerPageForRoute(
                route = VideoRoute.route,
                visibleItems = visibleItems
            )
        )
    }

    @Test
    fun bottomPagerSelection_clampsInvalidPageToHome() {
        val visibleItems = listOf(
            BottomNavItem.HOME,
            BottomNavItem.DYNAMIC,
            BottomNavItem.HISTORY
        )

        assertEquals(
            BottomNavItem.HOME,
            resolveBottomPagerItemForPage(
                page = -1,
                visibleItems = visibleItems
            )
        )
        assertEquals(
            BottomNavItem.HOME,
            resolveBottomPagerItemForPage(
                page = 99,
                visibleItems = visibleItems
            )
        )
    }

    @Test
    fun bottomPagerNavigationDuration_matchesKernelSuDistanceTiming() {
        assertEquals(
            300,
            resolveBottomPagerNavigationDurationMillis(
                currentPage = 0,
                targetPage = 1
            )
        )
        assertEquals(
            500,
            resolveBottomPagerNavigationDurationMillis(
                currentPage = 0,
                targetPage = 4
            )
        )
    }

    @Test
    fun bottomPagerPreload_waitsUntilContentReady() {
        assertEquals(0, resolveBottomPagerBeyondViewportPageCount(contentReady = false))
        assertEquals(1, resolveBottomPagerBeyondViewportPageCount(contentReady = true))
    }

    @Test
    fun bottomPagerUserScroll_isDisabledToAvoidAccidentalTabSwitch() {
        assertFalse(shouldEnableBottomPagerUserScroll())
    }

    @Test
    fun bottomPagerDuringNavigation_composesOnlyCurrentAndTargetBeforeReady() {
        assertTrue(
            shouldComposeBottomPagerPage(
                page = 0,
                currentPage = 0,
                selectedPage = 2,
                contentReady = false
            )
        )
        assertTrue(
            shouldComposeBottomPagerPage(
                page = 2,
                currentPage = 0,
                selectedPage = 2,
                contentReady = false
            )
        )
        assertFalse(
            shouldComposeBottomPagerPage(
                page = 1,
                currentPage = 0,
                selectedPage = 2,
                contentReady = false
            )
        )
        assertTrue(
            shouldComposeBottomPagerPage(
                page = 1,
                currentPage = 0,
                selectedPage = 2,
                contentReady = true
            )
        )
    }

    @Test
    fun routeMatchingVisibleBottomItem_selectsThatItem() {
        assertEquals(
            BottomNavItem.HISTORY,
            resolveBottomNavItemForRoute(
                currentRoute = ScreenRoutes.History.route,
                retainedItem = BottomNavItem.HOME
            )
        )
        assertEquals(
            BottomNavItem.PROFILE,
            resolveBottomNavItemForRoute(
                currentRoute = ScreenRoutes.Profile.route,
                retainedItem = BottomNavItem.HISTORY
            )
        )
    }

    @Test
    fun secondaryRoute_keepsRetainedBottomItemInsteadOfFallingBackHome() {
        assertEquals(
            BottomNavItem.HISTORY,
            resolveBottomNavItemForRoute(
                currentRoute = VideoRoute.route,
                retainedItem = BottomNavItem.HISTORY
            )
        )
        assertEquals(
            BottomNavItem.PROFILE,
            resolveBottomNavItemForRoute(
                currentRoute = ScreenRoutes.DownloadList.route,
                retainedItem = BottomNavItem.PROFILE
            )
        )
    }

    @Test
    fun unknownRouteWithoutRetainedItem_fallsBackHome() {
        assertEquals(
            BottomNavItem.HOME,
            resolveBottomNavItemForRoute(
                currentRoute = ScreenRoutes.DownloadList.route,
                retainedItem = null
            )
        )
    }

    @Test
    fun bottomTabToBottomTab_usesInstantTransition() {
        val visibleRoutes = setOf(
            ScreenRoutes.Home.route,
            ScreenRoutes.Dynamic.route,
            ScreenRoutes.History.route,
            ScreenRoutes.Profile.route
        )

        assertTrue(
            shouldUseInstantBottomTabTransition(
                fromRoute = ScreenRoutes.Home.route,
                toRoute = ScreenRoutes.History.route,
                visibleBottomBarRoutes = visibleRoutes
            )
        )
        assertTrue(
            shouldUseInstantBottomTabTransition(
                fromRoute = ScreenRoutes.Profile.route,
                toRoute = ScreenRoutes.Dynamic.route,
                visibleBottomBarRoutes = visibleRoutes
            )
        )
    }

    @Test
    fun secondaryRouteTransitions_keepRegularRouteMotion() {
        val visibleRoutes = setOf(
            ScreenRoutes.Home.route,
            ScreenRoutes.Dynamic.route,
            ScreenRoutes.History.route,
            ScreenRoutes.Profile.route
        )

        assertFalse(
            shouldUseInstantBottomTabTransition(
                fromRoute = VideoRoute.route,
                toRoute = ScreenRoutes.History.route,
                visibleBottomBarRoutes = visibleRoutes
            )
        )
        assertFalse(
            shouldUseInstantBottomTabTransition(
                fromRoute = ScreenRoutes.Home.route,
                toRoute = ScreenRoutes.Search.route,
                visibleBottomBarRoutes = visibleRoutes
            )
        )
        assertFalse(
            shouldUseInstantBottomTabTransition(
                fromRoute = ScreenRoutes.History.route,
                toRoute = ScreenRoutes.History.route,
                visibleBottomBarRoutes = visibleRoutes
            )
        )
    }

    @Test
    fun homeRoute_bypassesGlobalNavigationDebounce() {
        assertTrue(
            canProceedWithNavigation(
                currentTimeMillis = 1_000L,
                lastNavigationTimeMillis = 950L,
                debounceWindowMillis = 300L,
                bypassDebounce = shouldBypassNavigationDebounceForRoute(ScreenRoutes.Home.route)
            )
        )
    }

    @Test
    fun dynamicRoute_bypassesGlobalNavigationDebounce() {
        assertTrue(
            canProceedWithNavigation(
                currentTimeMillis = 1_000L,
                lastNavigationTimeMillis = 950L,
                debounceWindowMillis = 300L,
                bypassDebounce = shouldBypassNavigationDebounceForRoute(ScreenRoutes.Dynamic.route)
            )
        )
    }

    @Test
    fun profileRoute_bypassesGlobalNavigationDebounce() {
        assertTrue(
            canProceedWithNavigation(
                currentTimeMillis = 1_000L,
                lastNavigationTimeMillis = 950L,
                debounceWindowMillis = 300L,
                bypassDebounce = shouldBypassNavigationDebounceForRoute(ScreenRoutes.Profile.route)
            )
        )
    }

    @Test
    fun nonHomeRoute_stillRespectsGlobalNavigationDebounce() {
        assertFalse(
            canProceedWithNavigation(
                currentTimeMillis = 1_000L,
                lastNavigationTimeMillis = 950L,
                debounceWindowMillis = 300L,
                bypassDebounce = shouldBypassNavigationDebounceForRoute(ScreenRoutes.Search.route)
            )
        )
    }

    @Test
    fun profileShortcuts_preserveProfileStackSoBackReturnsToProfile() {
        assertTrue(shouldPreserveProfileStackForShortcut(ScreenRoutes.Settings.route))
        assertTrue(shouldPreserveProfileStackForShortcut(ScreenRoutes.History.route))
        assertTrue(shouldPreserveProfileStackForShortcut(ScreenRoutes.Favorite.route))
        assertTrue(shouldPreserveProfileStackForShortcut(ScreenRoutes.WatchLater.route))
        assertTrue(shouldPreserveProfileStackForShortcut(ScreenRoutes.DownloadList.route))
        assertTrue(shouldPreserveProfileStackForShortcut(ScreenRoutes.Inbox.route))
        assertTrue(shouldPreserveProfileStackForShortcut(ScreenRoutes.Following.route))
        assertTrue(shouldPreserveProfileStackForShortcut(ScreenRoutes.Following.createRoute(123L)))
    }
}
