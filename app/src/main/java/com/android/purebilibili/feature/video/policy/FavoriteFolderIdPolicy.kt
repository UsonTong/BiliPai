package com.android.purebilibili.feature.video.policy

import com.android.purebilibili.data.model.response.FavFolder

internal fun resolveFavoriteFolderMediaId(folder: FavFolder): Long {
    return when {
        folder.fid > 0L -> folder.fid
        folder.id > 0L -> folder.id
        else -> 0L
    }
}
