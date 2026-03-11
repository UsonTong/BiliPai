package com.android.purebilibili.feature.dynamic

import com.android.purebilibili.data.model.response.ReplyItem

internal fun canOpenDynamicSubReplies(reply: ReplyItem): Boolean {
    return resolveDynamicSubReplyCount(reply) > 0
}

internal fun resolveDynamicSubReplyCount(reply: ReplyItem): Int {
    return maxOf(
        reply.rcount,
        reply.replies.orEmpty().size
    )
}
