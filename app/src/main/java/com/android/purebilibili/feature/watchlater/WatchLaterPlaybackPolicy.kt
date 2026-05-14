package com.android.purebilibili.feature.watchlater

import com.android.purebilibili.data.model.response.VideoItem
import com.android.purebilibili.feature.video.player.PlaylistItem

data class WatchLaterExternalPlaylist(
    val playlistItems: List<PlaylistItem>,
    val startIndex: Int
)

internal data class WatchLaterPlaybackTarget(
    val bvid: String,
    val cid: Long,
    val resumePositionMs: Long
)

fun buildExternalPlaylistFromWatchLater(
    items: List<VideoItem>,
    clickedBvid: String? = null
): WatchLaterExternalPlaylist? {
    if (items.isEmpty()) return null

    val playlistItems = items.map { video ->
        PlaylistItem(
            bvid = video.bvid,
            title = video.title,
            cover = video.pic,
            owner = video.owner.name,
            duration = video.duration.toLong()
        )
    }

    val startIndex = clickedBvid
        ?.takeIf { it.isNotBlank() }
        ?.let { bvid -> items.indexOfFirst { it.bvid == bvid }.takeIf { it >= 0 } }
        ?: 0

    return WatchLaterExternalPlaylist(
        playlistItems = playlistItems,
        startIndex = startIndex
    )
}

internal fun resolveWatchLaterPlaybackTarget(
    items: List<VideoItem>,
    clickedBvid: String
): WatchLaterPlaybackTarget? {
    if (clickedBvid.isBlank()) return null
    val item = items.firstOrNull { it.bvid == clickedBvid } ?: return null
    return WatchLaterPlaybackTarget(
        bvid = item.bvid,
        cid = item.cid.coerceAtLeast(0L),
        resumePositionMs = item.progress
            .takeIf { it > 0 }
            ?.toLong()
            ?.times(1000L)
            ?: 0L
    )
}
