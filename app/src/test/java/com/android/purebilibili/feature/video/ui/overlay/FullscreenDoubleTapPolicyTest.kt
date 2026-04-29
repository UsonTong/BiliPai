package com.android.purebilibili.feature.video.ui.overlay

import androidx.media3.common.Player
import kotlin.test.Test
import kotlin.test.assertEquals

class FullscreenDoubleTapPolicyTest {

    @Test
    fun seekDisabled_alwaysTogglePlayPause() {
        assertEquals(
            FullscreenDoubleTapAction.TogglePlayPause,
            resolveFullscreenDoubleTapAction(relativeX = 0.1f, doubleTapSeekEnabled = false)
        )
        assertEquals(
            FullscreenDoubleTapAction.TogglePlayPause,
            resolveFullscreenDoubleTapAction(relativeX = 0.9f, doubleTapSeekEnabled = false)
        )
    }

    @Test
    fun seekEnabled_leftZoneSeekBackward() {
        assertEquals(
            FullscreenDoubleTapAction.SeekBackward,
            resolveFullscreenDoubleTapAction(relativeX = 0.2f, doubleTapSeekEnabled = true)
        )
    }

    @Test
    fun seekEnabled_centerZoneTogglePlayPause() {
        assertEquals(
            FullscreenDoubleTapAction.TogglePlayPause,
            resolveFullscreenDoubleTapAction(relativeX = 0.5f, doubleTapSeekEnabled = true)
        )
    }

    @Test
    fun seekEnabled_rightZoneSeekForward() {
        assertEquals(
            FullscreenDoubleTapAction.SeekForward,
            resolveFullscreenDoubleTapAction(relativeX = 0.8f, doubleTapSeekEnabled = true)
        )
    }

    @Test
    fun seekEnabled_pausedPlaybackAlwaysTogglePlayPause() {
        assertEquals(
            FullscreenDoubleTapAction.TogglePlayPause,
            resolveFullscreenDoubleTapAction(
                relativeX = 0.1f,
                doubleTapSeekEnabled = true,
                playWhenReady = false
            )
        )
        assertEquals(
            FullscreenDoubleTapAction.TogglePlayPause,
            resolveFullscreenDoubleTapAction(
                relativeX = 0.9f,
                doubleTapSeekEnabled = true,
                playWhenReady = false
            )
        )
    }

    @Test
    fun seekEnabled_silentReadyPlaybackAlwaysTogglePlayPause() {
        assertEquals(
            FullscreenDoubleTapAction.TogglePlayPause,
            resolveFullscreenDoubleTapAction(
                relativeX = 0.1f,
                doubleTapSeekEnabled = true,
                playWhenReady = true,
                isPlaying = false,
                playbackState = Player.STATE_READY
            )
        )
        assertEquals(
            FullscreenDoubleTapAction.TogglePlayPause,
            resolveFullscreenDoubleTapAction(
                relativeX = 0.9f,
                doubleTapSeekEnabled = true,
                playWhenReady = true,
                isPlaying = false,
                playbackState = Player.STATE_READY
            )
        )
    }
}
