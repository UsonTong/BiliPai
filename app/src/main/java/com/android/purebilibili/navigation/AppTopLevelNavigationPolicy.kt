package com.android.purebilibili.navigation

import com.android.purebilibili.feature.home.components.BottomNavItem

internal enum class TopLevelNavigationAction {
    SKIP,
    POP_EXISTING,
    NAVIGATE_WITH_RESTORE
}

internal enum class BottomBarSelectionAction {
    NAVIGATE,
    RESELECT
}

internal fun resolveTopLevelNavigationAction(
    currentRoute: String?,
    targetRoute: String,
    hasTargetInBackStack: Boolean
): TopLevelNavigationAction {
    if (currentRoute == targetRoute) {
        return TopLevelNavigationAction.SKIP
    }

    if (hasTargetInBackStack) {
        return TopLevelNavigationAction.POP_EXISTING
    }

    return TopLevelNavigationAction.NAVIGATE_WITH_RESTORE
}

internal fun resolveBottomBarSelectionAction(
    currentItem: BottomNavItem,
    tappedItem: BottomNavItem
): BottomBarSelectionAction {
    return if (currentItem == tappedItem) {
        BottomBarSelectionAction.RESELECT
    } else {
        BottomBarSelectionAction.NAVIGATE
    }
}

internal fun resolveBottomPagerPageForRoute(
    route: String?,
    visibleItems: List<BottomNavItem>
): Int? {
    val routeBase = route?.substringBefore("?") ?: return null
    return visibleItems.indexOfFirst { item -> item.route == routeBase }
        .takeIf { it >= 0 }
}

internal fun resolveBottomPagerItemForPage(
    page: Int,
    visibleItems: List<BottomNavItem>
): BottomNavItem {
    return visibleItems.getOrNull(page) ?: BottomNavItem.HOME
}

internal fun resolveBottomPagerNavigationDurationMillis(
    currentPage: Int,
    targetPage: Int
): Int {
    val distance = kotlin.math.abs(targetPage - currentPage).coerceAtLeast(2)
    return 100 * distance + 100
}

internal fun resolveBottomNavItemForRoute(
    currentRoute: String?,
    retainedItem: BottomNavItem?,
    visibleItems: List<BottomNavItem> = BottomNavItem.entries
): BottomNavItem {
    val routeBase = currentRoute?.substringBefore("?")
    return visibleItems.firstOrNull { item -> item.route == routeBase }
        ?: retainedItem
        ?: BottomNavItem.HOME
}

internal fun shouldUseInstantBottomTabTransition(
    fromRoute: String?,
    toRoute: String?,
    visibleBottomBarRoutes: Set<String>
): Boolean {
    val fromRouteBase = fromRoute?.substringBefore("?")
    val toRouteBase = toRoute?.substringBefore("?")
    return fromRouteBase != null &&
        toRouteBase != null &&
        fromRouteBase != toRouteBase &&
        fromRouteBase in visibleBottomBarRoutes &&
        toRouteBase in visibleBottomBarRoutes
}

internal fun shouldBypassNavigationDebounceForRoute(targetRoute: String): Boolean {
    return BottomNavItem.entries.any { item -> item.route == targetRoute }
}

internal fun canProceedWithNavigation(
    currentTimeMillis: Long,
    lastNavigationTimeMillis: Long,
    debounceWindowMillis: Long,
    bypassDebounce: Boolean
): Boolean {
    return bypassDebounce || currentTimeMillis - lastNavigationTimeMillis > debounceWindowMillis
}

internal fun shouldPreserveProfileStackForShortcut(targetRoute: String): Boolean {
    return targetRoute == ScreenRoutes.Settings.route ||
        targetRoute == ScreenRoutes.History.route ||
        targetRoute == ScreenRoutes.Favorite.route ||
        targetRoute == ScreenRoutes.WatchLater.route ||
        targetRoute == ScreenRoutes.DownloadList.route ||
        targetRoute == ScreenRoutes.Inbox.route ||
        targetRoute == ScreenRoutes.Following.route ||
        targetRoute.startsWith("following/")
}
