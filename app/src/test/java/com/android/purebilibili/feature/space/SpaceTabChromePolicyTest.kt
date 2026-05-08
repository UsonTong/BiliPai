package com.android.purebilibili.feature.space

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SpaceTabChromePolicyTest {

    @Test
    fun `main tab chrome resolves selected index and keeps touch target height`() {
        val tabs = listOf(
            SpaceMainTabItem(SpaceMainTab.HOME, "主页"),
            SpaceMainTabItem(SpaceMainTab.DYNAMIC, "动态"),
            SpaceMainTabItem(SpaceMainTab.CONTRIBUTION, "投稿")
        )

        val spec = resolveSpaceMainTabChromeSpec(
            tabs = tabs,
            selectedTab = SpaceMainTab.DYNAMIC
        )

        assertEquals(1, spec.selectedIndex)
        assertFalse(spec.scrollable)
        assertTrue(spec.heightDp >= 48)
        assertTrue(spec.indicatorHeightDp < spec.heightDp)
        assertTrue(spec.liquidGlassEffectsEnabled)
    }

    @Test
    fun `main tab chrome falls back to first item when selected tab is hidden`() {
        val spec = resolveSpaceMainTabChromeSpec(
            tabs = listOf(SpaceMainTabItem(SpaceMainTab.HOME, "主页")),
            selectedTab = SpaceMainTab.COLLECTIONS
        )

        assertEquals(0, spec.selectedIndex)
    }

    @Test
    fun `contribution tab chrome prefers selected id and uses compact secondary chrome`() {
        val tabs = listOf(
            SpaceContributionTab(
                id = "video",
                title = "视频",
                subTab = SpaceSubTab.VIDEO,
                param = "video"
            ),
            SpaceContributionTab(
                id = "article",
                title = "图文",
                subTab = SpaceSubTab.ARTICLE,
                param = "article"
            ),
            SpaceContributionTab(
                id = "season_video:season:1",
                title = "很长的合集标题",
                subTab = SpaceSubTab.SEASON_VIDEO,
                param = "season_video",
                seasonId = 1L
            ),
            SpaceContributionTab(
                id = "series:series:2",
                title = "系列",
                subTab = SpaceSubTab.SERIES,
                param = "series",
                seriesId = 2L
            )
        )
        val mainSpec = resolveSpaceMainTabChromeSpec(
            tabs = buildDefaultSpaceMainTabs().take(3),
            selectedTab = SpaceMainTab.DYNAMIC
        )

        val spec = resolveSpaceContributionTabChromeSpec(
            tabs = tabs,
            selectedTabId = "season_video:season:1",
            selectedSubTab = SpaceSubTab.VIDEO
        )

        assertEquals(2, spec.selectedIndex)
        assertTrue(spec.scrollable)
        assertTrue((spec.itemWidthDp ?: 0) > 104)
        assertTrue(spec.heightDp < mainSpec.heightDp)
        assertTrue(spec.heightDp >= 48)
        assertTrue(spec.indicatorHeightDp < mainSpec.indicatorHeightDp)
        assertTrue(spec.indicatorHeightDp < spec.heightDp)
        assertEquals(mainSpec.horizontalPaddingDp, spec.horizontalPaddingDp)
        assertTrue(spec.liquidGlassEffectsEnabled)
        assertFalse(spec.dragSelectionEnabled)
    }

    @Test
    fun `single contribution video tab uses compact item width instead of full row`() {
        val spec = resolveSpaceContributionTabChromeSpec(
            tabs = listOf(
                SpaceContributionTab(
                    id = "video",
                    title = "视频",
                    subTab = SpaceSubTab.VIDEO,
                    param = "video"
                )
            ),
            selectedTabId = "video",
            selectedSubTab = SpaceSubTab.VIDEO
        )

        assertFalse(spec.scrollable)
        assertEquals(104, spec.itemWidthDp)
        assertTrue(spec.dragSelectionEnabled)
    }

    @Test
    fun `contribution tab chrome falls back to selected sub tab`() {
        val tabs = buildDefaultSpaceContributionTabs()

        val spec = resolveSpaceContributionTabChromeSpec(
            tabs = tabs,
            selectedTabId = "missing",
            selectedSubTab = SpaceSubTab.AUDIO
        )

        assertEquals(tabs.indexOfFirst { it.subTab == SpaceSubTab.AUDIO }, spec.selectedIndex)
    }

    @Test
    fun `contribution tab chrome keeps drag selection for three contribution entries`() {
        val tabs = listOf(
            SpaceContributionTab(
                id = "video",
                title = "视频",
                subTab = SpaceSubTab.VIDEO,
                param = "video"
            ),
            SpaceContributionTab(
                id = "article",
                title = "图文",
                subTab = SpaceSubTab.ARTICLE,
                param = "article"
            ),
            SpaceContributionTab(
                id = "season_video:season:1",
                title = "合集 · 美食对决合集视频",
                subTab = SpaceSubTab.SEASON_VIDEO,
                param = "season_video",
                seasonId = 1L
            )
        )

        val spec = resolveSpaceContributionTabChromeSpec(
            tabs = tabs,
            selectedTabId = "season_video:season:1",
            selectedSubTab = SpaceSubTab.VIDEO
        )

        assertFalse(spec.scrollable)
        assertTrue((spec.itemWidthDp ?: 0) > 104)
        assertTrue(spec.dragSelectionEnabled)
    }

    @Test
    fun `contribution tab scroll offset centers selected item like main tab indicator`() {
        assertEquals(
            0,
            resolveSpaceContributionTabCenteredScrollOffsetPx(
                selectedIndex = 0,
                itemWidthPx = 160f,
                viewportWidthPx = 360f
            )
        )
        assertEquals(
            220,
            resolveSpaceContributionTabCenteredScrollOffsetPx(
                selectedIndex = 2,
                itemWidthPx = 160f,
                viewportWidthPx = 360f
            )
        )
    }
}
