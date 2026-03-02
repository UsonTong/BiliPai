package com.android.purebilibili.feature.video.ui.overlay

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import com.android.purebilibili.core.store.BottomProgressBehavior

class VideoPlayerOverlayPolicyTest {

    @Test
    fun episodeEntryShownWhenRelatedVideosExist() {
        assertTrue(
            shouldShowEpisodeEntryFromVideoData(
                relatedVideosCount = 1,
                hasSeasonEpisodes = false,
                pagesCount = 1
            )
        )
    }

    @Test
    fun episodeEntryShownWhenSeasonEpisodesExist() {
        assertTrue(
            shouldShowEpisodeEntryFromVideoData(
                relatedVideosCount = 0,
                hasSeasonEpisodes = true,
                pagesCount = 1
            )
        )
    }

    @Test
    fun episodeEntryShownWhenPagesExist() {
        assertTrue(
            shouldShowEpisodeEntryFromVideoData(
                relatedVideosCount = 0,
                hasSeasonEpisodes = false,
                pagesCount = 3
            )
        )
    }

    @Test
    fun episodeEntryHiddenWhenNoEpisodeData() {
        assertFalse(
            shouldShowEpisodeEntryFromVideoData(
                relatedVideosCount = 0,
                hasSeasonEpisodes = false,
                pagesCount = 1
            )
        )
    }

    @Test
    fun nextEpisodeTargetPrefersPageNext() {
        val target = resolveNextEpisodeTarget(
            pagesCount = 3,
            currentPageIndex = 0,
            seasonEpisodeBvids = listOf("BV1", "BV2"),
            currentBvid = "BV1",
            relatedBvids = listOf("BV3")
        )
        assertTrue(target?.nextPageIndex == 1)
    }

    @Test
    fun nextEpisodeTargetFallsBackToSeasonThenRelated() {
        val seasonTarget = resolveNextEpisodeTarget(
            pagesCount = 1,
            currentPageIndex = 0,
            seasonEpisodeBvids = listOf("BV1", "BV2"),
            currentBvid = "BV1",
            relatedBvids = listOf("BV3")
        )
        assertTrue(seasonTarget?.nextBvid == "BV2")

        val relatedTarget = resolveNextEpisodeTarget(
            pagesCount = 1,
            currentPageIndex = 0,
            seasonEpisodeBvids = listOf("BV1"),
            currentBvid = "BV1",
            relatedBvids = listOf("BV1", "BV3")
        )
        assertTrue(relatedTarget?.nextBvid == "BV3")
    }

    @Test
    fun nextEpisodeTargetReturnsNullWhenNoCandidate() {
        assertTrue(
            resolveNextEpisodeTarget(
                pagesCount = 1,
                currentPageIndex = 0,
                seasonEpisodeBvids = emptyList(),
                currentBvid = "BV1",
                relatedBvids = emptyList()
            )
                == null
        )
    }

    @Test
    fun drawerVisibleShouldConsumeBackgroundGestures() {
        assertTrue(
            shouldConsumeBackgroundGesturesForEndDrawer(
                endDrawerVisible = true
            )
        )
    }

    @Test
    fun drawerHiddenShouldNotConsumeBackgroundGestures() {
        assertFalse(
            shouldConsumeBackgroundGesturesForEndDrawer(
                endDrawerVisible = false
            )
        )
    }

    @Test
    fun centerPlayButtonHiddenWhenScrubbingOrBuffering() {
        assertFalse(
            shouldShowCenterPlayButton(
                isVisible = true,
                isPlaying = false,
                isQualitySwitching = false,
                isFullscreen = true,
                isBuffering = true,
                isScrubbing = false
            )
        )
        assertFalse(
            shouldShowCenterPlayButton(
                isVisible = true,
                isPlaying = false,
                isQualitySwitching = false,
                isFullscreen = true,
                isBuffering = false,
                isScrubbing = true
            )
        )
        assertTrue(
            shouldShowCenterPlayButton(
                isVisible = true,
                isPlaying = false,
                isQualitySwitching = false,
                isFullscreen = true,
                isBuffering = false,
                isScrubbing = false
            )
        )
    }

    @Test
    fun bufferingIndicatorCanShowDuringScrubbingEvenWhenControlsVisible() {
        assertTrue(
            shouldShowBufferingIndicator(
                isBuffering = true,
                isQualitySwitching = false,
                isVisible = true,
                isScrubbing = true
            )
        )
        assertFalse(
            shouldShowBufferingIndicator(
                isBuffering = true,
                isQualitySwitching = false,
                isVisible = true,
                isScrubbing = false
            )
        )
    }

    @Test
    fun pageSelectorSheetOuterBottomPadding_isZeroInFullscreen() {
        assertEquals(0, resolvePageSelectorSheetOuterBottomPaddingDp(isFullscreen = true))
        assertEquals(8, resolvePageSelectorSheetOuterBottomPaddingDp(isFullscreen = false))
    }

    @Test
    fun persistentBottomProgressBarRespectsAlwaysShowBehavior() {
        assertTrue(
            shouldShowPersistentBottomProgressBar(
                controlsVisible = false,
                isFullscreen = false,
                behavior = BottomProgressBehavior.ALWAYS_SHOW
            )
        )
        assertFalse(
            shouldShowPersistentBottomProgressBar(
                controlsVisible = true,
                isFullscreen = true,
                behavior = BottomProgressBehavior.ALWAYS_SHOW
            )
        )
    }

    @Test
    fun persistentBottomProgressBarRespectsAlwaysHideBehavior() {
        assertFalse(
            shouldShowPersistentBottomProgressBar(
                controlsVisible = false,
                isFullscreen = true,
                behavior = BottomProgressBehavior.ALWAYS_HIDE
            )
        )
    }

    @Test
    fun persistentBottomProgressBarRespectsOnlyShowFullscreenBehavior() {
        assertTrue(
            shouldShowPersistentBottomProgressBar(
                controlsVisible = false,
                isFullscreen = true,
                behavior = BottomProgressBehavior.ONLY_SHOW_FULLSCREEN
            )
        )
        assertFalse(
            shouldShowPersistentBottomProgressBar(
                controlsVisible = false,
                isFullscreen = false,
                behavior = BottomProgressBehavior.ONLY_SHOW_FULLSCREEN
            )
        )
    }

    @Test
    fun persistentBottomProgressBarRespectsOnlyHideFullscreenBehavior() {
        assertTrue(
            shouldShowPersistentBottomProgressBar(
                controlsVisible = false,
                isFullscreen = false,
                behavior = BottomProgressBehavior.ONLY_HIDE_FULLSCREEN
            )
        )
        assertFalse(
            shouldShowPersistentBottomProgressBar(
                controlsVisible = false,
                isFullscreen = true,
                behavior = BottomProgressBehavior.ONLY_HIDE_FULLSCREEN
            )
        )
    }

    @Test
    fun resolveDisplayedOnlineCountRespectsSetting() {
        assertEquals(
            "123人正在看",
            resolveDisplayedOnlineCount(
                onlineCount = "123人正在看",
                showOnlineCount = true
            )
        )
        assertEquals(
            "",
            resolveDisplayedOnlineCount(
                onlineCount = "123人正在看",
                showOnlineCount = false
            )
        )
    }
}
