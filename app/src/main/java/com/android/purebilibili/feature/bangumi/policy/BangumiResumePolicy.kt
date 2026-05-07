package com.android.purebilibili.feature.bangumi

import com.android.purebilibili.data.model.response.BangumiDetail

internal data class BangumiResumeTarget(
    val epId: Long,
    val resumePositionMs: Long
)

internal fun resolveBangumiAutoResumeTarget(
    detail: BangumiDetail,
    routeEpId: Long,
    autoResumeEnabled: Boolean
): BangumiResumeTarget? {
    if (!autoResumeEnabled || routeEpId > 0L) return null

    val progress = detail.userStatus?.progress ?: return null
    val lastEpId = progress.lastEpId.takeIf { it > 0L } ?: return null
    val episodes = detail.episodes.orEmpty()
    if (episodes.isNotEmpty() && episodes.none { it.id == lastEpId }) return null

    return BangumiResumeTarget(
        epId = lastEpId,
        resumePositionMs = resolveBangumiResumePositionMs(progress.lastTime)
    )
}

internal fun resolveBangumiResumePositionMs(lastTimeSec: Long): Long {
    return lastTimeSec.coerceAtLeast(0L) * 1000L
}

internal fun shouldSendBangumiPlaybackHeartbeat(
    isPlaying: Boolean,
    bvid: String,
    cid: Long,
    currentPositionMs: Long
): Boolean {
    return isPlaying &&
        bvid.isNotBlank() &&
        cid > 0L &&
        currentPositionMs > 0L
}
