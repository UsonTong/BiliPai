package com.android.purebilibili.feature.video.playback.policy

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PlaybackAncillaryTaskPolicyTest {

    @Test
    fun `online count refresh should require foreground and enabled setting`() {
        assertTrue(
            shouldRefreshOnlineCount(
                showOnlineCountEnabled = true,
                isInBackground = false,
                currentBvid = "BV1test",
                currentCid = 123L
            )
        )
        assertFalse(
            shouldRefreshOnlineCount(
                showOnlineCountEnabled = true,
                isInBackground = true,
                currentBvid = "BV1test",
                currentCid = 123L
            )
        )
        assertFalse(
            shouldRefreshOnlineCount(
                showOnlineCountEnabled = false,
                isInBackground = false,
                currentBvid = "BV1test",
                currentCid = 123L
            )
        )
    }

    @Test
    fun `online count polling should back off harder in background`() {
        assertEquals(30_000L, resolveOnlineCountPollingDelayMs(isInBackground = false))
        assertEquals(90_000L, resolveOnlineCountPollingDelayMs(isInBackground = true))
    }

    @Test
    fun `heartbeat should only send for active foreground playback`() {
        assertTrue(
            shouldSendPlaybackHeartbeat(
                isPlaying = true,
                isInBackground = false,
                currentBvid = "BV1test",
                currentCid = 456L
            )
        )
        assertFalse(
            shouldSendPlaybackHeartbeat(
                isPlaying = false,
                isInBackground = false,
                currentBvid = "BV1test",
                currentCid = 456L
            )
        )
        assertFalse(
            shouldSendPlaybackHeartbeat(
                isPlaying = true,
                isInBackground = true,
                currentBvid = "BV1test",
                currentCid = 456L
            )
        )
    }
}
