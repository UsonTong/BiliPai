package com.android.purebilibili.feature.video.ui.overlay

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MiniPlayerOverlayPollingPolicyTest {

    @Test
    fun pollingEnabled_onlyWhenMiniModeActiveWithPlayer() {
        assertTrue(
            shouldPollMiniPlayerProgress(
                playerExists = true,
                isMiniMode = true,
                isActive = true
            )
        )
    }

    @Test
    fun pollingDisabled_whenAnyRequiredConditionMissing() {
        assertFalse(
            shouldPollMiniPlayerProgress(
                playerExists = false,
                isMiniMode = true,
                isActive = true
            )
        )
        assertFalse(
            shouldPollMiniPlayerProgress(
                playerExists = true,
                isMiniMode = false,
                isActive = true
            )
        )
        assertFalse(
            shouldPollMiniPlayerProgress(
                playerExists = true,
                isMiniMode = true,
                isActive = false
            )
        )
    }

    @Test
    fun pausedPlayback_usesLongerPollingInterval() {
        assertEquals(300L, resolveMiniPlayerPollingIntervalMs(isPlaying = true))
        assertEquals(600L, resolveMiniPlayerPollingIntervalMs(isPlaying = false))
    }
}
