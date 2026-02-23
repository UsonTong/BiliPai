package com.android.purebilibili.feature.video.ui.overlay

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FullscreenPlayerOverlayPollingPolicyTest {

    @Test
    fun pollingDisabled_withoutPlayer() {
        assertFalse(
            shouldPollFullscreenPlayerProgress(
                playerExists = false
            )
        )
    }

    @Test
    fun pollingEnabled_withPlayer() {
        assertTrue(
            shouldPollFullscreenPlayerProgress(
                playerExists = true
            )
        )
    }

    @Test
    fun pausedPlayback_hiddenControls_usesLongerInterval() {
        assertEquals(
            500L,
            resolveFullscreenPlayerPollingIntervalMs(
                isPlaying = false,
                showControls = false,
                isSeekingGesture = false
            )
        )
    }

    @Test
    fun visibleControlsOrSeeking_keepsFastInterval() {
        assertEquals(
            100L,
            resolveFullscreenPlayerPollingIntervalMs(
                isPlaying = true,
                showControls = true,
                isSeekingGesture = false
            )
        )
        assertEquals(
            100L,
            resolveFullscreenPlayerPollingIntervalMs(
                isPlaying = false,
                showControls = false,
                isSeekingGesture = true
            )
        )
    }
}
