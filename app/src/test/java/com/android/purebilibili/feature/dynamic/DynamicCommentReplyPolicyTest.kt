package com.android.purebilibili.feature.dynamic

import com.android.purebilibili.data.model.response.ReplyItem
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DynamicCommentReplyPolicyTest {

    @Test
    fun `dynamic comment exposes sub reply entry when server says replies exist`() {
        val reply = ReplyItem(rpid = 1L, rcount = 4)

        assertTrue(canOpenDynamicSubReplies(reply))
        assertEquals(4, resolveDynamicSubReplyCount(reply))
    }

    @Test
    fun `dynamic comment falls back to inline reply preview count`() {
        val reply = ReplyItem(
            rpid = 1L,
            replies = listOf(
                ReplyItem(rpid = 2L),
                ReplyItem(rpid = 3L)
            )
        )

        assertTrue(canOpenDynamicSubReplies(reply))
        assertEquals(2, resolveDynamicSubReplyCount(reply))
    }

    @Test
    fun `dynamic comment hides sub reply entry when no replies exist`() {
        val reply = ReplyItem(rpid = 1L)

        assertFalse(canOpenDynamicSubReplies(reply))
        assertEquals(0, resolveDynamicSubReplyCount(reply))
    }
}
