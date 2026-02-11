package com.android.purebilibili.feature.video.ui.pager

internal fun resolveCommittedPage(
    isScrollInProgress: Boolean,
    currentPage: Int,
    lastCommittedPage: Int
): Int? {
    if (isScrollInProgress) return null
    if (currentPage == lastCommittedPage) return null
    return currentPage
}

internal fun shouldApplyLoadResult(
    requestGeneration: Int,
    activeGeneration: Int,
    expectedBvid: String,
    currentPlayingBvid: String?
): Boolean {
    if (requestGeneration != activeGeneration) return false
    if (expectedBvid != currentPlayingBvid) return false
    return true
}
