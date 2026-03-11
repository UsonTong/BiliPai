package com.android.purebilibili.feature.dynamic

import com.android.purebilibili.data.model.response.ReplyData
import com.android.purebilibili.data.model.response.ReplyItem

internal data class DynamicCommentPayload(
    val replies: List<ReplyItem>,
    val totalCount: Int
)

internal data class DynamicCommentLoadAttempt(
    val target: DynamicCommentTarget,
    val replies: List<ReplyItem>,
    val totalCount: Int,
    val candidateIndex: Int
)

internal fun resolveDynamicCommentPayload(
    data: ReplyData,
    fallbackCount: Int
): DynamicCommentPayload {
    val replies = buildList {
        addAll(data.collectTopReplies())
        addAll(data.hots.orEmpty())
        addAll(data.replies.orEmpty())
    }.distinctBy { it.rpid }

    return DynamicCommentPayload(
        replies = replies,
        totalCount = maxOf(
            data.getAllCount(),
            fallbackCount,
            replies.size
        )
    )
}

internal fun selectPreferredDynamicCommentAttempt(
    attempts: List<DynamicCommentLoadAttempt>
): DynamicCommentLoadAttempt? {
    return attempts.minWithOrNull(
        compareBy<DynamicCommentLoadAttempt> {
            when {
                it.replies.isNotEmpty() -> 0
                it.totalCount > 0 -> 1
                else -> 2
            }
        }.thenBy { it.candidateIndex }
            .thenByDescending { it.totalCount }
            .thenByDescending { it.replies.size }
    )
}
