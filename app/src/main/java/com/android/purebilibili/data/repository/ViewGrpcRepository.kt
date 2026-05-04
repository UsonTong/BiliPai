package com.android.purebilibili.data.repository

import com.android.purebilibili.core.network.NetworkModule
import com.android.purebilibili.core.util.Logger
import com.android.purebilibili.data.model.response.BgmInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal object ViewGrpcRepository {
    private const val TAG = "BgmList"

    suspend fun getBgmList(aid: Long, bvid: String, cid: Long): Result<List<BgmInfo>> =
        withContext(Dispatchers.IO) {
            runCatching {
                val response = NetworkModule.api.getBgmMultipleMusic(aid = aid, cid = cid)
                val list = response.data?.list.orEmpty().map { bgm ->
                    // API returns music_id but no jump_url, construct it
                    if (bgm.jumpUrl.isBlank() && bgm.musicId.isNotBlank()) {
                        bgm.copy(
                            jumpUrl = "https://music.bilibili.com/h5-music-detail" +
                                "?music_id=${bgm.musicId}&cid=$cid&aid=$aid&na_close_hide=1"
                        )
                    } else bgm
                }
                Logger.w(TAG, "BGM list API returned ${list.size} entries for aid=$aid")
                list
            }.onFailure { e ->
                Logger.w(TAG, "BGM list API failed: ${e.message}")
            }
        }
}
