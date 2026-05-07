package com.android.purebilibili.feature.bangumi

import com.android.purebilibili.data.model.response.BangumiDetail
import com.android.purebilibili.data.model.response.BangumiEpisode
import com.android.purebilibili.data.model.response.UserStatus
import com.android.purebilibili.data.model.response.WatchProgress
import com.android.purebilibili.navigation.ScreenRoutes
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class BangumiResumePolicyTest {

    @Test
    fun `auto resume target uses detail watch progress when entering season without explicit episode`() {
        val target = resolveBangumiAutoResumeTarget(
            detail = BangumiDetail(
                seasonId = 100L,
                episodes = listOf(
                    BangumiEpisode(id = 1L),
                    BangumiEpisode(id = 22L)
                ),
                userStatus = UserStatus(
                    progress = WatchProgress(lastEpId = 22L, lastTime = 321L)
                )
            ),
            routeEpId = 0L,
            autoResumeEnabled = true
        )

        assertEquals(BangumiResumeTarget(epId = 22L, resumePositionMs = 321_000L), target)
    }

    @Test
    fun `auto resume does not override an explicit episode route`() {
        val target = resolveBangumiAutoResumeTarget(
            detail = BangumiDetail(
                episodes = listOf(BangumiEpisode(id = 22L)),
                userStatus = UserStatus(
                    progress = WatchProgress(lastEpId = 22L, lastTime = 321L)
                )
            ),
            routeEpId = 11L,
            autoResumeEnabled = true
        )

        assertNull(target)
    }

    @Test
    fun `bangumi player route carries resume position when provided`() {
        assertEquals(
            "bangumi/play/100/22?resumePositionMs=321000",
            ScreenRoutes.BangumiPlayer.createRoute(
                seasonId = 100L,
                epId = 22L,
                resumePositionMs = 321_000L
            )
        )
    }

    @Test
    fun `bangumi heartbeat only sends meaningful playable progress`() {
        assertTrue(
            shouldSendBangumiPlaybackHeartbeat(
                isPlaying = true,
                bvid = "BV1bangumi",
                cid = 123L,
                currentPositionMs = 30_000L
            )
        )
        assertFalse(
            shouldSendBangumiPlaybackHeartbeat(
                isPlaying = true,
                bvid = "BV1bangumi",
                cid = 123L,
                currentPositionMs = 0L
            )
        )
    }
}
