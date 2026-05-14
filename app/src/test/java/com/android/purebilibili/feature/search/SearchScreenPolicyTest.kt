package com.android.purebilibili.feature.search

import com.android.purebilibili.data.model.response.SearchType
import com.android.purebilibili.data.repository.SearchUpOrder
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SearchScreenPolicyTest {

    @Test
    fun resetSearchScroll_onlyWhenShowingNonBlankResults() {
        assertTrue(
            shouldResetSearchResultScroll(
                searchSessionId = 1L,
                showResults = true,
                lastResetSessionId = 0L
            )
        )
        assertFalse(
            shouldResetSearchResultScroll(
                searchSessionId = 0L,
                showResults = true,
                lastResetSessionId = 0L
            )
        )
        assertFalse(
            shouldResetSearchResultScroll(
                searchSessionId = 2L,
                showResults = false,
                lastResetSessionId = 1L
            )
        )
    }

    @Test
    fun backToTopButton_onlyShowsAfterResultListScrollsPastThreshold() {
        assertFalse(
            shouldShowSearchBackToTop(
                firstVisibleItemIndex = 0,
                firstVisibleItemScrollOffset = 180
            )
        )
        assertTrue(
            shouldShowSearchBackToTop(
                firstVisibleItemIndex = 0,
                firstVisibleItemScrollOffset = 320
            )
        )
        assertTrue(
            shouldShowSearchBackToTop(
                firstVisibleItemIndex = 1,
                firstVisibleItemScrollOffset = 0
            )
        )
    }

    @Test
    fun submitKeyword_prefersTypedQuery_thenFallsBackToSuggestedKeyword() {
        assertEquals(
            "黑神话悟空",
            resolveSearchSubmitKeyword(
                query = "  黑神话悟空 ",
                suggestedKeyword = "睡羊妹妹m"
            )
        )
        assertEquals(
            "睡羊妹妹m",
            resolveSearchSubmitKeyword(
                query = " ",
                suggestedKeyword = " 睡羊妹妹m "
            )
        )
        assertEquals(
            "",
            resolveSearchSubmitKeyword(
                query = "",
                suggestedKeyword = " "
            )
        )
    }

    @Test
    fun searchFilterTabs_exposeFullSearchTypesInPlannedOrder() {
        assertEquals(
            listOf(
                SearchType.VIDEO,
                SearchType.UP,
                SearchType.BANGUMI,
                SearchType.MEDIA_FT,
                SearchType.LIVE,
                SearchType.LIVE_USER,
                SearchType.ARTICLE,
                SearchType.TOPIC,
                SearchType.PHOTO
            ),
            resolveSearchFilterTabs()
        )
    }

    @Test
    fun searchFilterControls_matchCurrentSearchType() {
        assertEquals(
            listOf(
                SearchFilterControl.VIDEO_ORDER,
                SearchFilterControl.VIDEO_DURATION,
                SearchFilterControl.VIDEO_TID
            ),
            resolveSearchFilterControls(
                currentType = SearchType.VIDEO,
                currentUpOrder = SearchUpOrder.DEFAULT
            )
        )
        assertEquals(
            listOf(
                SearchFilterControl.UP_ORDER,
                SearchFilterControl.UP_ORDER_SORT,
                SearchFilterControl.UP_USER_TYPE
            ),
            resolveSearchFilterControls(
                currentType = SearchType.UP,
                currentUpOrder = SearchUpOrder.FANS
            )
        )
        assertEquals(
            listOf(SearchFilterControl.LIVE_ORDER),
            resolveSearchFilterControls(
                currentType = SearchType.LIVE,
                currentUpOrder = SearchUpOrder.DEFAULT
            )
        )
        assertEquals(
            emptyList(),
            resolveSearchFilterControls(
                currentType = SearchType.PHOTO,
                currentUpOrder = SearchUpOrder.DEFAULT
            )
        )
    }

    @Test
    fun searchTypeTabs_useCompactDensityOnNarrowScreens() {
        val compact = resolveSearchTypeTabLayoutSpec(widthDp = 360)
        val regular = resolveSearchTypeTabLayoutSpec(widthDp = 412)

        assertEquals(6, compact.horizontalSpacingDp)
        assertEquals(10, compact.horizontalPaddingDp)
        assertEquals(13, compact.fontSizeSp)
        assertEquals(36, compact.minHeightDp)

        assertEquals(8, regular.horizontalSpacingDp)
        assertEquals(16, regular.horizontalPaddingDp)
        assertEquals(14, regular.fontSizeSp)
        assertEquals(40, regular.minHeightDp)
    }

    @Test
    fun bottomBarSearchEntry_usesDedicatedTopBarContinuityMotion() {
        val navigationSource = loadSource("app/src/main/java/com/android/purebilibili/navigation/AppNavigation.kt")
        val searchSource = loadSource("app/src/main/java/com/android/purebilibili/feature/search/SearchScreen.kt")

        assertTrue(navigationSource.contains("fun navigateToSearchFromBottomBar()"))
        assertTrue(navigationSource.contains("fun requestSearchFromBottomBar()"))
        assertTrue(navigationSource.contains("bottomBarSearchLaunchKey += 1"))
        assertTrue(navigationSource.contains("onSearchClick = { requestSearchFromBottomBar() }"))
        assertTrue(navigationSource.contains("searchLaunchKey = bottomBarSearchLaunchKey"))
        assertTrue(navigationSource.contains("onSearchLaunchTransitionFinished = { completedKey ->"))
        assertTrue(navigationSource.contains("searchEntryMotionSource = SearchEntryMotionSource.BOTTOM_BAR"))
        assertTrue(navigationSource.contains("searchEntryMotionKey += 1"))
        assertTrue(navigationSource.contains("entryMotionSource = searchEntryMotionSource"))
        assertTrue(navigationSource.contains("entryMotionKey = searchEntryMotionKey"))

        assertTrue(searchSource.contains("entryMotionSource: SearchEntryMotionSource = SearchEntryMotionSource.NONE"))
        assertTrue(searchSource.contains("entryMotionSpec = resolveSearchEntryMotionSpec("))
        assertTrue(searchSource.contains("entryMotionKey = entryMotionKey"))
        assertTrue(searchSource.contains("graphicsLayer"))
        assertTrue(searchSource.contains("TransformOrigin("))
        assertTrue(searchSource.contains("spec.transformOriginPivotX"))
        assertTrue(searchSource.contains("spec.transformOriginPivotY"))
    }

    @Test
    fun searchEntryMotion_onlyRunsForBottomBarSourceAndRespectsReducedBudget() {
        assertEquals(
            null,
            resolveSearchEntryMotionSpec(
                source = SearchEntryMotionSource.NONE,
                reducedMotionBudget = false
            )
        )

        val bottomBarSpec = requireNotNull(
            resolveSearchEntryMotionSpec(
                source = SearchEntryMotionSource.BOTTOM_BAR,
                reducedMotionBudget = false
            )
        )
        assertEquals(260, bottomBarSpec.durationMillis)
        assertEquals(0.72f, bottomBarSpec.initialAlpha)
        assertEquals(0.92f, bottomBarSpec.initialScale)
        assertEquals(26f, bottomBarSpec.initialTranslationYDp)
        assertEquals(0.88f, bottomBarSpec.transformOriginPivotX)
        assertEquals(1f, bottomBarSpec.transformOriginPivotY)

        val reducedSpec = requireNotNull(
            resolveSearchEntryMotionSpec(
                source = SearchEntryMotionSource.BOTTOM_BAR,
                reducedMotionBudget = true
            )
        )
        assertEquals(0, reducedSpec.durationMillis)
        assertEquals(1f, reducedSpec.initialAlpha)
        assertEquals(1f, reducedSpec.initialScale)
        assertEquals(0f, reducedSpec.initialTranslationYDp)
    }

    private fun loadSource(path: String): String {
        val normalizedPath = path.removePrefix("app/")
        val sourceFile = listOf(
            File(path),
            File(normalizedPath)
        ).firstOrNull { it.exists() }
        require(sourceFile != null) { "Cannot locate $path from ${File(".").absolutePath}" }
        return sourceFile.readText()
    }
}
