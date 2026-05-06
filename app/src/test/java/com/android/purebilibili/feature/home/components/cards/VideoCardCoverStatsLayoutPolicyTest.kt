package com.android.purebilibili.feature.home.components.cards

import org.junit.Assert.assertEquals
import org.junit.Test

class VideoCardCoverStatsLayoutPolicyTest {

    @Test
    fun `primary stat badge reserves enough width for wan count`() {
        val width = resolveVideoCardPrimaryStatBadgeMinWidthDp("6.2万")

        assertEquals(58f, width, 0.0001f)
    }

    @Test
    fun `primary stat badge expands for longer formatted counts`() {
        val width = resolveVideoCardPrimaryStatBadgeMinWidthDp("123.4万")

        assertEquals(70f, width, 0.0001f)
    }
}
