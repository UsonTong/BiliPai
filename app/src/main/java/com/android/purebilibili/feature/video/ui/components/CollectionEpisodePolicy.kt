package com.android.purebilibili.feature.video.ui.components

import com.android.purebilibili.data.model.response.UgcEpisode

internal fun resolveCurrentUgcEpisodeIndex(
    episodes: List<UgcEpisode>,
    currentBvid: String,
    currentCid: Long
): Int {
    if (currentBvid.isBlank()) return -1
    if (currentCid > 0L) {
        val exactIndex = episodes.indexOfFirst { episode ->
            episode.bvid == currentBvid && episode.cid == currentCid
        }
        if (exactIndex >= 0) return exactIndex
    }
    return episodes.indexOfFirst { episode -> episode.bvid == currentBvid }
}

internal fun isCurrentUgcEpisode(
    currentBvid: String,
    currentCid: Long,
    episode: UgcEpisode
): Boolean {
    if (episode.bvid != currentBvid) return false
    return currentCid <= 0L || episode.cid <= 0L || episode.cid == currentCid
}
