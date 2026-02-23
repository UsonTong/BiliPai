package com.android.purebilibili.data.repository

import com.android.purebilibili.core.network.AppSignUtils
import com.android.purebilibili.data.model.response.PlayUrlData

internal const val TV_CAST_DEFAULT_QUALITY = 80

internal fun buildTvCastPlayUrlParams(
    aid: Long,
    cid: Long,
    qn: Int,
    accessToken: String?
): MutableMap<String, String> {
    val targetQn = qn.takeIf { it > 0 } ?: TV_CAST_DEFAULT_QUALITY
    val params = mutableMapOf(
        "actionKey" to "appkey",
        "cid" to cid.toString(),
        "fourk" to "1",
        "is_proj" to "1",
        "object_id" to aid.toString(),
        "mobi_app" to "android",
        "platform" to "android",
        "playurl_type" to "1",
        "protocol" to "0",
        "qn" to targetQn.toString(),
        "appkey" to AppSignUtils.TV_APP_KEY,
        "ts" to AppSignUtils.getTimestamp().toString()
    )

    if (!accessToken.isNullOrBlank()) {
        params["access_key"] = accessToken
        params["mobile_access_key"] = accessToken
    }
    return params
}

internal fun extractTvCastPlayableUrl(data: PlayUrlData?): String? {
    if (data == null) return null
    data.durl.orEmpty().forEach { segment ->
        if (segment.url.isNotBlank()) {
            return segment.url
        }
        val backup = segment.backupUrl?.firstOrNull { it.isNotBlank() }
        if (!backup.isNullOrBlank()) {
            return backup
        }
    }

    val dashVideo = data.dash?.video
        ?.firstOrNull { it.getValidUrl().isNotBlank() }
        ?.getValidUrl()
    if (!dashVideo.isNullOrBlank()) {
        return dashVideo
    }
    return null
}
