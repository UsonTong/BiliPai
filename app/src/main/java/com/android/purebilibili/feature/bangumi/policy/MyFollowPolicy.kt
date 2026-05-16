package com.android.purebilibili.feature.bangumi

import com.android.purebilibili.data.model.response.BangumiType
import com.android.purebilibili.data.model.response.FollowBangumiItem

const val MY_FOLLOW_TYPE_BANGUMI = 1
const val MY_FOLLOW_TYPE_CINEMA = 2

fun defaultMyFollowTypeForSeasonType(seasonType: Int): Int {
    return when (seasonType) {
        BangumiType.ANIME.value, BangumiType.GUOCHUANG.value -> MY_FOLLOW_TYPE_BANGUMI
        else -> MY_FOLLOW_TYPE_CINEMA
    }
}

fun resolveMyFollowRequestType(requestedType: Int?, currentType: Int): Int {
    return requestedType ?: currentType
}

fun resolveMyFollowItemLazyKey(
    index: Int,
    item: FollowBangumiItem
): String {
    val businessKey = when {
        item.seasonId > 0L -> "season_${item.seasonId}"
        item.mediaId > 0L -> "media_${item.mediaId}"
        item.firstEp > 0L -> "ep_${item.firstEp}"
        item.url.isNotBlank() -> "url_${item.url.hashCode()}"
        item.title.isNotBlank() -> "title_${item.title.hashCode()}"
        else -> "unknown"
    }
    return "my_follow_${businessKey}_$index"
}
