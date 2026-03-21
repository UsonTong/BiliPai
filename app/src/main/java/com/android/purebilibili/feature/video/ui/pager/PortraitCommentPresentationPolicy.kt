package com.android.purebilibili.feature.video.ui.pager

internal fun shouldUseEmbeddedVideoSubReplyPresentation(): Boolean = true

private const val DEFAULT_VIDEO_SUB_REPLY_SHEET_MAX_HEIGHT_FRACTION = 0.78f

internal fun shouldShowDetachedVideoSubReplySheet(
    useEmbeddedPresentation: Boolean
): Boolean = !useEmbeddedPresentation

internal fun shouldOpenPortraitCommentReplyComposer(): Boolean = true

internal fun shouldOpenPortraitCommentThreadDetail(
    useEmbeddedPresentation: Boolean
): Boolean = true

internal fun resolveVideoSubReplySheetMaxHeightFraction(
    screenHeightPx: Int = 0,
    topReservedPx: Int = 0
): Float {
    if (screenHeightPx <= 0 || topReservedPx <= 0) {
        return DEFAULT_VIDEO_SUB_REPLY_SHEET_MAX_HEIGHT_FRACTION
    }
    val availableHeightPx = (screenHeightPx - topReservedPx).coerceAtLeast(1)
    return (availableHeightPx.toFloat() / screenHeightPx.toFloat())
        .coerceIn(0.3f, DEFAULT_VIDEO_SUB_REPLY_SHEET_MAX_HEIGHT_FRACTION)
}

internal fun resolveVideoSubReplySheetScrimAlpha(): Float = 0f
