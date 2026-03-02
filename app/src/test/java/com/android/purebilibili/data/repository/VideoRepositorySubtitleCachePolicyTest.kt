package com.android.purebilibili.data.repository

import kotlin.test.Test
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class VideoRepositorySubtitleCachePolicyTest {

    @Test
    fun `subtitle cache key includes bvid cid track identity and url hash`() {
        val key = buildSubtitleCueCacheKey(
            bvid = "BV1abc",
            cid = 2233L,
            subtitleId = 4455L,
            subtitleIdStr = "track-x",
            subtitleLan = "zh-Hans",
            normalizedSubtitleUrl = "https://aisubtitle.hdslb.com/bfs/subtitle/demo.json?auth_key=1"
        )

        assertTrue(key.startsWith("BV1abc:2233:track-x:zh-Hans:"))
    }

    @Test
    fun `subtitle cache key differs when cid or subtitle url differs`() {
        val base = buildSubtitleCueCacheKey(
            bvid = "BV1abc",
            cid = 2233L,
            subtitleId = 4455L,
            subtitleIdStr = "track-x",
            subtitleLan = "zh-Hans",
            normalizedSubtitleUrl = "https://aisubtitle.hdslb.com/bfs/subtitle/demo.json?auth_key=1"
        )
        val cidChanged = buildSubtitleCueCacheKey(
            bvid = "BV1abc",
            cid = 2234L,
            subtitleId = 4455L,
            subtitleIdStr = "track-x",
            subtitleLan = "zh-Hans",
            normalizedSubtitleUrl = "https://aisubtitle.hdslb.com/bfs/subtitle/demo.json?auth_key=1"
        )
        val urlChanged = buildSubtitleCueCacheKey(
            bvid = "BV1abc",
            cid = 2233L,
            subtitleId = 4455L,
            subtitleIdStr = "track-x",
            subtitleLan = "zh-Hans",
            normalizedSubtitleUrl = "https://aisubtitle.hdslb.com/bfs/subtitle/demo.json?auth_key=2"
        )

        assertNotEquals(base, cidChanged)
        assertNotEquals(base, urlChanged)
    }
}
