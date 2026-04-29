package com.android.purebilibili.feature.live

import java.io.File
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LiveCategorySegmentedControlStructureTest {

    @Test
    fun `live home category row delegates to bottom bar segmented control`() {
        val source = loadSource(
            "app/src/main/java/com/android/purebilibili/feature/live/LiveListScreen.kt"
        )

        assertTrue(source.contains("BottomBarLiquidSegmentedControl("))
        assertTrue(source.contains("resolveLiveHomeCategorySegmentedControlSpec()"))
        assertTrue(source.contains(".horizontalScroll(scrollState, enabled = false)"))
        assertTrue(source.contains("resolveLiveHomeCategoryFollowScrollTarget("))
        assertTrue(source.contains("scrollState.scrollTo(targetScroll)"))
        assertTrue(source.contains("onIndicatorPositionChanged = { indicatorPosition = it }"))
        assertFalse(source.contains("dragSelectionEnabled = false"))
        assertFalse(source.contains("liquidGlassEffectsEnabled = false"))
        assertFalse(source.contains("SimpleLiquidIndicator"))
        assertFalse(source.contains("shouldUseLiveHomeCategoryLiquidIndicator("))
    }

    @Test
    fun `all tags parent row uses shared segmented control dimensions`() {
        val source = loadSource(
            "app/src/main/java/com/android/purebilibili/feature/live/LiveAreaScreen.kt"
        )

        assertTrue(source.contains("rememberPagerState"))
        assertTrue(source.contains("HorizontalPager("))
        assertTrue(source.contains("pagerState.animateScrollToPage"))
        assertTrue(source.contains("selectedTab = pagerState.currentPage"))
        assertTrue(source.contains("BottomBarLiquidSegmentedControl("))
        assertTrue(source.contains("resolveLiveAreaParentSegmentedControlSpec()"))
        assertTrue(source.contains(".horizontalScroll(scrollState, enabled = false)"))
        assertTrue(source.contains("resolveLiveHomeCategoryFollowScrollTarget("))
        assertTrue(source.contains("scrollState.scrollTo(targetScroll)"))
        assertTrue(source.contains("onIndicatorPositionChanged = { indicatorPosition = it }"))
        assertFalse(source.contains("dragSelectionEnabled = false"))
        assertFalse(source.contains("liquidGlassEffectsEnabled = false"))
    }

    @Test
    fun `live room interaction panel supports horizontal pager gestures`() {
        val source = loadSource(
            "app/src/main/java/com/android/purebilibili/feature/live/LivePlayerScreen.kt"
        )

        assertTrue(source.contains("rememberPagerState"))
        assertTrue(source.contains("HorizontalPager("))
        assertTrue(source.contains("pagerState.animateScrollToPage"))
        assertTrue(source.contains("selectedIndex = pagerState.currentPage"))
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
