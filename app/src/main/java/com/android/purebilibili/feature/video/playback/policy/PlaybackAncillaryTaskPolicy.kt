package com.android.purebilibili.feature.video.playback.policy

internal fun shouldRefreshOnlineCount(
    showOnlineCountEnabled: Boolean,
    isInBackground: Boolean,
    currentBvid: String,
    currentCid: Long
): Boolean {
    return showOnlineCountEnabled &&
        !isInBackground &&
        currentBvid.isNotBlank() &&
        currentCid > 0L
}

internal fun resolveOnlineCountPollingDelayMs(isInBackground: Boolean): Long {
    return if (isInBackground) 90_000L else 30_000L
}

internal fun shouldSendPlaybackHeartbeat(
    isPlaying: Boolean,
    isInBackground: Boolean,
    currentBvid: String,
    currentCid: Long
): Boolean {
    return isPlaying &&
        !isInBackground &&
        currentBvid.isNotBlank() &&
        currentCid > 0L
}
