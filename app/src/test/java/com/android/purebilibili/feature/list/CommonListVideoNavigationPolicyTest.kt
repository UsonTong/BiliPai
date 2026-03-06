package com.android.purebilibili.feature.list

import com.android.purebilibili.data.model.response.VideoItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CommonListVideoNavigationPolicyTest {

    @Test
    fun `returns normalized request when video has bvid`() {
        val request = resolveCommonListVideoNavigationRequest(
            VideoItem(
                bvid = "  BV1abc411c7m  ",
                cid = 1234L,
                pic = "//i0.hdslb.com/test.jpg"
            )
        )

        assertEquals(
            CommonListVideoNavigationRequest(
                bvid = "BV1abc411c7m",
                cid = 1234L,
                coverUrl = "//i0.hdslb.com/test.jpg"
            ),
            request
        )
    }

    @Test
    fun `returns null when video bvid is blank`() {
        val request = resolveCommonListVideoNavigationRequest(
            VideoItem(
                bvid = "   ",
                cid = 1234L,
                pic = "https://example.com/cover.jpg"
            )
        )

        assertNull(request)
    }

    @Test
    fun `falls back to zero cid when cid is invalid`() {
        val request = resolveCommonListVideoNavigationRequest(
            VideoItem(
                bvid = "BV1abc411c7m",
                cid = -1L,
                pic = ""
            )
        )

        assertEquals(0L, request?.cid)
    }
}
