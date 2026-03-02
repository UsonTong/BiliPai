package com.android.purebilibili.feature.home

import com.android.purebilibili.data.model.response.VideoItem
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class HomeRefreshPolicyTest {

    @Test
    fun trimIncrementalRefreshVideosToEvenCount_keepsSingleItem() {
        val input = listOf(VideoItem(bvid = "BV1"))

        val output = trimIncrementalRefreshVideosToEvenCount(input)

        assertEquals(listOf("BV1"), output.map { it.bvid })
    }

    @Test
    fun trimIncrementalRefreshVideosToEvenCount_dropsLastWhenOddAndMoreThanOne() {
        val input = listOf(
            VideoItem(bvid = "BV1"),
            VideoItem(bvid = "BV2"),
            VideoItem(bvid = "BV3")
        )

        val output = trimIncrementalRefreshVideosToEvenCount(input)

        assertEquals(listOf("BV1", "BV2"), output.map { it.bvid })
    }

    @Test
    fun trimIncrementalRefreshVideosToEvenCount_keepsAllWhenEven() {
        val input = listOf(
            VideoItem(bvid = "BV1"),
            VideoItem(bvid = "BV2"),
            VideoItem(bvid = "BV3"),
            VideoItem(bvid = "BV4")
        )

        val output = trimIncrementalRefreshVideosToEvenCount(input)

        assertEquals(listOf("BV1", "BV2", "BV3", "BV4"), output.map { it.bvid })
    }

    @Test
    fun shouldHandleRefreshNewItemsEvent_requiresPositiveAndGreaterKey() {
        assertFalse(shouldHandleRefreshNewItemsEvent(refreshKey = 0L, handledKey = 0L))
        assertFalse(shouldHandleRefreshNewItemsEvent(refreshKey = 10L, handledKey = 10L))
        assertFalse(shouldHandleRefreshNewItemsEvent(refreshKey = 9L, handledKey = 10L))
        assertTrue(shouldHandleRefreshNewItemsEvent(refreshKey = 11L, handledKey = 10L))
    }

    @Test
    fun buildHomeRefreshUndoSnapshot_returnsNull_forNonRecommendCategory() {
        val snapshot = buildHomeRefreshUndoSnapshot(
            refreshingCategory = HomeCategory.POPULAR,
            recommendCategoryState = CategoryContent(
                videos = listOf(VideoItem(bvid = "BV1")),
                pageIndex = 3,
                hasMore = false
            ),
            fallbackVideos = listOf(VideoItem(bvid = "BV2"))
        )

        assertEquals(null, snapshot)
    }

    @Test
    fun buildHomeRefreshUndoSnapshot_returnsNull_whenRecommendListEmpty() {
        val snapshot = buildHomeRefreshUndoSnapshot(
            refreshingCategory = HomeCategory.RECOMMEND,
            recommendCategoryState = CategoryContent(videos = emptyList()),
            fallbackVideos = emptyList()
        )

        assertEquals(null, snapshot)
    }

    @Test
    fun buildHomeRefreshUndoSnapshot_limitsItemsAndPreservesPagingState() {
        val videos = (1..25).map { index -> VideoItem(bvid = "BV$index") }
        val snapshot = buildHomeRefreshUndoSnapshot(
            refreshingCategory = HomeCategory.RECOMMEND,
            recommendCategoryState = CategoryContent(
                videos = videos,
                pageIndex = 4,
                hasMore = false
            ),
            fallbackVideos = emptyList()
        )

        assertEquals(20, snapshot?.videos?.size)
        assertEquals("BV1", snapshot?.videos?.firstOrNull()?.bvid)
        assertEquals("BV20", snapshot?.videos?.lastOrNull()?.bvid)
        assertEquals(4, snapshot?.pageIndex)
        assertFalse(snapshot?.hasMore ?: true)
    }

    @Test
    fun applyHomeRefreshUndoSnapshot_restoresVideosAndPagingState() {
        val oldState = CategoryContent(
            videos = listOf(VideoItem(bvid = "NEW")),
            pageIndex = 8,
            hasMore = true
        )
        val snapshot = HomeRefreshUndoSnapshot(
            videos = listOf(VideoItem(bvid = "OLD-1"), VideoItem(bvid = "OLD-2")),
            pageIndex = 2,
            hasMore = false
        )

        val restored = applyHomeRefreshUndoSnapshot(oldState, snapshot)

        assertEquals(listOf("OLD-1", "OLD-2"), restored.videos.map { it.bvid })
        assertEquals(2, restored.pageIndex)
        assertFalse(restored.hasMore)
    }

    @Test
    fun shouldExposeHomeRefreshUndo_onlyWhenRecommendAndSnapshotExists() {
        assertFalse(
            shouldExposeHomeRefreshUndo(
                refreshingCategory = HomeCategory.POPULAR,
                snapshot = HomeRefreshUndoSnapshot(videos = listOf(VideoItem(bvid = "BV1")), pageIndex = 1, hasMore = true)
            )
        )
        assertFalse(
            shouldExposeHomeRefreshUndo(
                refreshingCategory = HomeCategory.RECOMMEND,
                snapshot = null
            )
        )
        assertTrue(
            shouldExposeHomeRefreshUndo(
                refreshingCategory = HomeCategory.RECOMMEND,
                snapshot = HomeRefreshUndoSnapshot(videos = listOf(VideoItem(bvid = "BV1")), pageIndex = 1, hasMore = true)
            )
        )
    }
}
