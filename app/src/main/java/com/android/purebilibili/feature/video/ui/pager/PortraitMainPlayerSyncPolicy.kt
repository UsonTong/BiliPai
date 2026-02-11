package com.android.purebilibili.feature.video.ui.pager

internal fun shouldReloadMainPlayerAfterPortraitExit(
    snapshotBvid: String?,
    currentBvid: String?
): Boolean {
    if (snapshotBvid.isNullOrBlank()) return false
    if (currentBvid.isNullOrBlank()) return true
    return snapshotBvid != currentBvid
}
