package com.android.purebilibili.feature.video.ui.overlay

internal fun shouldPollFullscreenPlayerProgress(
    playerExists: Boolean,
    hostLifecycleStarted: Boolean
): Boolean {
    return playerExists && hostLifecycleStarted
}

internal fun resolveFullscreenPlayerPollingIntervalMs(
    isPlaying: Boolean,
    showControls: Boolean,
    isSeekingGesture: Boolean
): Long {
    if (showControls || isSeekingGesture) return 100L
    return if (isPlaying) 400L else 800L
}
