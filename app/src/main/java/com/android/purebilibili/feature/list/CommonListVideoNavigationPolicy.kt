package com.android.purebilibili.feature.list

import com.android.purebilibili.data.model.response.VideoItem

internal data class CommonListVideoNavigationRequest(
    val bvid: String,
    val cid: Long,
    val coverUrl: String
)

internal fun resolveCommonListVideoNavigationRequest(
    video: VideoItem
): CommonListVideoNavigationRequest? {
    val normalizedBvid = video.bvid.trim()
    if (normalizedBvid.isEmpty()) return null

    return CommonListVideoNavigationRequest(
        bvid = normalizedBvid,
        cid = video.cid.takeIf { it > 0L } ?: 0L,
        coverUrl = video.pic
    )
}
