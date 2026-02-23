package com.android.purebilibili.feature.video.ui.overlay

internal fun shouldPollFullscreenPlayerProgress(
    playerExists: Boolean
): Boolean {
    return playerExists
}

internal fun resolveFullscreenPlayerPollingIntervalMs(
    isPlaying: Boolean,
    showControls: Boolean,
    isSeekingGesture: Boolean
): Long {
    if (showControls || isSeekingGesture) return 100L
    return if (isPlaying) 250L else 500L
}
