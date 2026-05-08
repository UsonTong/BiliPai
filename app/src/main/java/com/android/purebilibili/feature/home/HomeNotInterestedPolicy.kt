package com.android.purebilibili.feature.home

import com.android.purebilibili.data.model.response.VideoItem

internal data class HomeNotInterestedAction(
    val bvid: String,
    val shouldBlockCreator: Boolean,
    val creatorMid: Long,
    val creatorName: String,
    val creatorFace: String
)

internal fun resolveHomeNotInterestedAction(video: VideoItem): HomeNotInterestedAction {
    val creatorMid = video.owner.mid
    return HomeNotInterestedAction(
        bvid = video.bvid,
        shouldBlockCreator = creatorMid > 0L,
        creatorMid = creatorMid,
        creatorName = video.owner.name.ifBlank {
            if (creatorMid > 0L) "UP主$creatorMid" else ""
        },
        creatorFace = video.owner.face
    )
}
