package com.android.purebilibili.feature.video.screen

import android.os.Bundle
import com.android.purebilibili.data.model.response.RelatedVideo
import com.android.purebilibili.data.model.response.UgcSeason

internal const val VIDEO_NAV_TARGET_CID_KEY = "video_nav_target_cid"

internal fun buildVideoNavigationOptions(
    base: Bundle? = null,
    targetCid: Long
): Bundle? {
    if (targetCid <= 0L) return base
    return Bundle().apply {
        if (base != null) {
            putAll(base)
        }
        putLong(VIDEO_NAV_TARGET_CID_KEY, targetCid)
    }
}

internal fun resolveNavigationTargetCid(
    targetBvid: String,
    explicitCid: Long,
    relatedVideos: List<RelatedVideo> = emptyList(),
    ugcSeason: UgcSeason?
): Long {
    if (explicitCid > 0L) return explicitCid
    if (targetBvid.isBlank()) return 0L
    relatedVideos.firstOrNull { video -> video.bvid == targetBvid }
        ?.cid
        ?.takeIf { it > 0L }
        ?.let { return it }
    return ugcSeason
        ?.sections
        ?.asSequence()
        ?.flatMap { section -> section.episodes.asSequence() }
        ?.firstOrNull { episode -> episode.bvid == targetBvid }
        ?.cid
        ?.takeIf { it > 0L }
        ?: 0L
}
