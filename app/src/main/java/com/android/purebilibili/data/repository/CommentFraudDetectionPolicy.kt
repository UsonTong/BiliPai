package com.android.purebilibili.data.repository

import com.android.purebilibili.data.model.CommentFraudStatus

internal data class CommentPresenceProbe(
    val requestSucceeded: Boolean,
    val found: Boolean,
    val deletedHint: Boolean = false
)

internal fun shouldStartCommentFraudDetection(
    enabled: Boolean,
    rpid: Long
): Boolean = enabled && rpid > 0L

internal fun shouldShowCommentFraudResultDialog(status: CommentFraudStatus): Boolean {
    return status != CommentFraudStatus.NORMAL
}

internal fun resolveCommentFraudLightMessage(status: CommentFraudStatus): String? {
    return if (status == CommentFraudStatus.NORMAL) {
        "评论已正常显示"
    } else {
        null
    }
}

internal fun resolveReplyFraudStatus(
    guestProbe: CommentPresenceProbe,
    authProbe: CommentPresenceProbe,
    confirmedNotFoundAfterRetry: Boolean
): CommentFraudStatus {
    if (guestProbe.requestSucceeded && guestProbe.found) {
        return CommentFraudStatus.NORMAL
    }

    if (authProbe.requestSucceeded && authProbe.found) {
        return if (guestProbe.requestSucceeded) {
            CommentFraudStatus.SHADOW_BANNED
        } else {
            CommentFraudStatus.UNKNOWN
        }
    }

    if (!authProbe.requestSucceeded) {
        return CommentFraudStatus.UNKNOWN
    }

    if (authProbe.deletedHint || confirmedNotFoundAfterRetry) {
        return CommentFraudStatus.DELETED
    }

    return CommentFraudStatus.UNKNOWN
}

internal fun resolveRootFraudStatus(
    guestSeekProbe: CommentPresenceProbe,
    authSeekProbe: CommentPresenceProbe,
    guestReplyPageVisible: Boolean?,
    confirmedNotFoundAfterRetry: Boolean
): CommentFraudStatus {
    if (guestSeekProbe.requestSucceeded && guestSeekProbe.found) {
        return CommentFraudStatus.NORMAL
    }

    if (authSeekProbe.requestSucceeded && authSeekProbe.found) {
        if (!guestSeekProbe.requestSucceeded) return CommentFraudStatus.UNKNOWN
        return when (guestReplyPageVisible) {
            true -> CommentFraudStatus.UNDER_REVIEW
            false -> CommentFraudStatus.SHADOW_BANNED
            null -> CommentFraudStatus.UNKNOWN
        }
    }

    if (!authSeekProbe.requestSucceeded) {
        return CommentFraudStatus.UNKNOWN
    }

    if (authSeekProbe.deletedHint || confirmedNotFoundAfterRetry) {
        return CommentFraudStatus.DELETED
    }

    return CommentFraudStatus.UNKNOWN
}
