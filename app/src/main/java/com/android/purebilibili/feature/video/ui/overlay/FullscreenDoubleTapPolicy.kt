package com.android.purebilibili.feature.video.ui.overlay

import androidx.media3.common.Player

internal enum class FullscreenDoubleTapAction {
    SeekBackward,
    TogglePlayPause,
    SeekForward
}

internal fun resolveFullscreenDoubleTapAction(
    relativeX: Float,
    doubleTapSeekEnabled: Boolean,
    playWhenReady: Boolean = true,
    isPlaying: Boolean = playWhenReady,
    playbackState: Int = Player.STATE_READY
): FullscreenDoubleTapAction {
    if (!doubleTapSeekEnabled) return FullscreenDoubleTapAction.TogglePlayPause
    if (!shouldRouteDoubleTapToSeek(
            playWhenReady = playWhenReady,
            isPlaying = isPlaying,
            playbackState = playbackState
        )
    ) {
        return FullscreenDoubleTapAction.TogglePlayPause
    }

    return when {
        relativeX < 0.3f -> FullscreenDoubleTapAction.SeekBackward
        relativeX > 0.7f -> FullscreenDoubleTapAction.SeekForward
        else -> FullscreenDoubleTapAction.TogglePlayPause
    }
}

private fun shouldRouteDoubleTapToSeek(
    playWhenReady: Boolean,
    isPlaying: Boolean,
    playbackState: Int
): Boolean {
    return playWhenReady && (isPlaying || playbackState == Player.STATE_BUFFERING)
}
