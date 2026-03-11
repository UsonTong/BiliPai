package com.android.purebilibili.feature.video.ui.components

import com.android.purebilibili.data.model.response.UgcEpisode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CollectionEpisodePolicyTest {

    @Test
    fun `resolveCurrentUgcEpisodeIndex prefers exact cid match when bvid repeats`() {
        val episodes = listOf(
            UgcEpisode(bvid = "BV1TEST", cid = 11L, title = "P1"),
            UgcEpisode(bvid = "BV1TEST", cid = 22L, title = "P2"),
            UgcEpisode(bvid = "BV1TEST", cid = 33L, title = "P3")
        )

        val index = resolveCurrentUgcEpisodeIndex(
            episodes = episodes,
            currentBvid = "BV1TEST",
            currentCid = 22L
        )

        assertEquals(1, index)
    }

    @Test
    fun `resolveCurrentUgcEpisodeIndex falls back to bvid match when current cid missing`() {
        val episodes = listOf(
            UgcEpisode(bvid = "BV1TEST", cid = 11L, title = "P1"),
            UgcEpisode(bvid = "BV1TEST", cid = 22L, title = "P2")
        )

        val index = resolveCurrentUgcEpisodeIndex(
            episodes = episodes,
            currentBvid = "BV1TEST",
            currentCid = 0L
        )

        assertEquals(0, index)
    }

    @Test
    fun `isCurrentUgcEpisode requires cid match when current cid is known`() {
        assertFalse(
            isCurrentUgcEpisode(
                currentBvid = "BV1TEST",
                currentCid = 22L,
                episode = UgcEpisode(bvid = "BV1TEST", cid = 11L)
            )
        )
        assertTrue(
            isCurrentUgcEpisode(
                currentBvid = "BV1TEST",
                currentCid = 22L,
                episode = UgcEpisode(bvid = "BV1TEST", cid = 22L)
            )
        )
    }
}
