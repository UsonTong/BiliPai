package com.android.purebilibili.feature.message

import com.android.purebilibili.data.model.response.SessionItem

internal data class InboxSessionPageMergeResult(
    val sessions: List<SessionItem>,
    val newSessions: List<SessionItem>,
    val nextEndTs: Long,
    val hasMore: Boolean,
    val shouldRetryWithOlderCursor: Boolean
)

internal object InboxSessionPaginationPolicy {

    fun sortSessions(sessions: List<SessionItem>): List<SessionItem> =
        sessions.sortedWith(
            compareByDescending<SessionItem> { it.top_ts }
                .thenByDescending { it.session_ts }
        )

    fun resolveNextEndTs(sessions: List<SessionItem>, requestedEndTs: Long = 0L): Long {
        val normalized = normalizeSessionEndTs(sessions.lastOrNull()?.session_ts ?: 0L)
        if (normalized <= 0L) return 0L

        // get_sessions 的 end_ts 可能返回边界会话，下一次请求必须严格更早，避免重复页卡住。
        val strictOlderCursor = normalized - 1L
        return if (requestedEndTs > 0L && strictOlderCursor >= requestedEndTs) {
            (requestedEndTs - 1L).coerceAtLeast(0L)
        } else {
            strictOlderCursor
        }
    }

    fun mergePage(
        existing: List<SessionItem>,
        incoming: List<SessionItem>,
        responseHasMore: Boolean,
        requestedEndTs: Long,
        canRetryDuplicatePage: Boolean = true
    ): InboxSessionPageMergeResult {
        val existingKeys = existing.map { it.sessionKey }.toSet()
        val newSessions = incoming.filter { it.sessionKey !in existingKeys }
        val nextEndTs = resolveNextEndTs(incoming, requestedEndTs)
        val canAdvanceCursor = nextEndTs > 0L && nextEndTs != requestedEndTs
        val isDuplicatePage = newSessions.isEmpty() && incoming.isNotEmpty()
        val shouldRetry = canRetryDuplicatePage &&
            responseHasMore &&
            isDuplicatePage &&
            canAdvanceCursor
        val hasMore = responseHasMore &&
            nextEndTs > 0L &&
            (newSessions.isNotEmpty() || shouldRetry)

        return InboxSessionPageMergeResult(
            sessions = sortSessions(existing + newSessions).distinctBy { it.sessionKey },
            newSessions = newSessions,
            nextEndTs = nextEndTs,
            hasMore = hasMore,
            shouldRetryWithOlderCursor = shouldRetry
        )
    }

    private val SessionItem.sessionKey: String
        get() = "${talker_id}_${session_type}"

    private fun normalizeSessionEndTs(sessionTs: Long): Long {
        if (sessionTs <= 0L) return 0L
        return when {
            sessionTs >= 1_000_000_000_000_000L -> sessionTs
            sessionTs >= 1_000_000_000_000L -> sessionTs * 1_000L
            else -> sessionTs * 1_000_000L
        }
    }
}
