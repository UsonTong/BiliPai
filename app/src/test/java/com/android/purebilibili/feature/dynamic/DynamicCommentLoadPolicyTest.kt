package com.android.purebilibili.feature.dynamic

import com.android.purebilibili.data.model.response.ReplyCursor
import com.android.purebilibili.data.model.response.ReplyData
import com.android.purebilibili.data.model.response.ReplyItem
import com.android.purebilibili.data.model.response.ReplyPage
import com.android.purebilibili.data.model.response.ReplyTop
import kotlin.test.Test
import kotlin.test.assertEquals

class DynamicCommentLoadPolicyTest {

    @Test
    fun `dynamic comment payload merges top hot and main replies with stable count`() {
        val data = ReplyData(
            cursor = ReplyCursor(allCount = 0, isEnd = false, next = 2),
            page = ReplyPage(count = 0, acount = 0),
            top = ReplyTop(upper = ReplyItem(rpid = 100L)),
            hots = listOf(ReplyItem(rpid = 101L)),
            replies = listOf(ReplyItem(rpid = 102L), ReplyItem(rpid = 101L))
        )

        val resolved = resolveDynamicCommentPayload(
            data = data,
            fallbackCount = 29
        )

        assertEquals(listOf(100L, 101L, 102L), resolved.replies.map { it.rpid })
        assertEquals(29, resolved.totalCount)
    }

    @Test
    fun `dynamic comment selection prefers candidate closer to expected count`() {
        val attempts = listOf(
            DynamicCommentLoadAttempt(
                target = DynamicCommentTarget(oid = 326122895L, type = 11),
                replies = listOf(ReplyItem(rpid = 1L)),
                totalCount = 122,
                candidateIndex = 1
            ),
            DynamicCommentLoadAttempt(
                target = DynamicCommentTarget(oid = 967717348014293017L, type = 17),
                replies = listOf(ReplyItem(rpid = 2L)),
                totalCount = 3,
                candidateIndex = 0
            )
        )

        val selected = selectPreferredDynamicCommentAttempt(attempts = attempts)

        assertEquals(DynamicCommentTarget(oid = 967717348014293017L, type = 17), selected?.target)
    }
}
