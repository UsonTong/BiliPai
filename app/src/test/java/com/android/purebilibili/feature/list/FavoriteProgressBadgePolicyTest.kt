package com.android.purebilibili.feature.list

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FavoriteProgressBadgePolicyTest {

    @Test
    fun `favorite detail badge exposes remaining gap after all pages loaded`() {
        val badge = resolveFavoriteDetailProgressBadge(
            loadedCount = 180,
            expectedCount = 227,
            currentPage = 9,
            lastAddedCount = 7,
            invalidCount = 0,
            hasMore = false
        )

        assertEquals("180 / 227", badge.primaryText)
        assertEquals("P9  +7", badge.secondaryText)
        assertTrue(badge.footnoteText?.contains("差额 47") == true)
    }

    @Test
    fun `favorite detail badge shows invalid items when present`() {
        val badge = resolveFavoriteDetailProgressBadge(
            loadedCount = 44,
            expectedCount = 44,
            currentPage = 2,
            lastAddedCount = 4,
            invalidCount = 3,
            hasMore = false
        )

        assertEquals("44 / 44", badge.primaryText)
        assertEquals("P2  +4  异常3", badge.secondaryText)
    }

    @Test
    fun `subscribed folder badge uses total count when known`() {
        val badge = resolveSubscribedFolderProgressBadge(
            loadedCount = 40,
            totalCount = 73,
            currentPage = 1,
            lastAddedCount = 40,
            hasMore = true
        )

        assertEquals("40 / 73", badge.primaryText)
        assertEquals("P1  +40", badge.secondaryText)
        assertEquals(null, badge.footnoteText)
    }
}
