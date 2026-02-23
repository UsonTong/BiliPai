package com.android.purebilibili.data.repository

import com.android.purebilibili.data.model.response.Dash
import com.android.purebilibili.data.model.response.DashVideo
import com.android.purebilibili.data.model.response.Durl
import com.android.purebilibili.data.model.response.PlayUrlData
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class VideoCastPolicyTest {

    @Test
    fun `buildTvCastPlayUrlParams includes projection fields and token`() {
        val params = buildTvCastPlayUrlParams(
            aid = 1234L,
            cid = 5678L,
            qn = 120,
            accessToken = "token-abc"
        )

        assertEquals("1234", params["object_id"])
        assertEquals("5678", params["cid"])
        assertEquals("1", params["is_proj"])
        assertEquals("1", params["playurl_type"])
        assertEquals("120", params["qn"])
        assertEquals("token-abc", params["access_key"])
        assertEquals("token-abc", params["mobile_access_key"])
        assertNotNull(params["ts"])
        assertEquals("appkey", params["actionKey"])
    }

    @Test
    fun `buildTvCastPlayUrlParams falls back to default quality and omits blank token`() {
        val params = buildTvCastPlayUrlParams(
            aid = 99L,
            cid = 88L,
            qn = 0,
            accessToken = "  "
        )

        assertEquals("80", params["qn"])
        assertFalse(params.containsKey("access_key"))
        assertFalse(params.containsKey("mobile_access_key"))
    }

    @Test
    fun `extractTvCastPlayableUrl prefers durl url then backup then dash`() {
        val durlPrimary = PlayUrlData(
            durl = listOf(Durl(url = "https://cdn.example.com/main.mp4"))
        )
        val durlBackup = PlayUrlData(
            durl = listOf(Durl(url = "", backupUrl = listOf("https://cdn.example.com/backup.mp4")))
        )
        val dashFallback = PlayUrlData(
            durl = emptyList(),
            dash = Dash(video = listOf(DashVideo(baseUrl = "https://cdn.example.com/video.m4s")))
        )

        assertEquals("https://cdn.example.com/main.mp4", extractTvCastPlayableUrl(durlPrimary))
        assertEquals("https://cdn.example.com/backup.mp4", extractTvCastPlayableUrl(durlBackup))
        assertEquals("https://cdn.example.com/video.m4s", extractTvCastPlayableUrl(dashFallback))
    }

    @Test
    fun `extractTvCastPlayableUrl returns null when payload has no playable stream`() {
        val emptyPayload = PlayUrlData(durl = emptyList(), dash = Dash(video = emptyList()))

        assertNull(extractTvCastPlayableUrl(emptyPayload))
    }
}
