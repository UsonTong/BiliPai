package com.android.purebilibili.feature.list

internal data class FavoriteProgressBadge(
    val primaryText: String,
    val secondaryText: String,
    val footnoteText: String? = null
)

internal fun resolveFavoriteDetailProgressBadge(
    loadedCount: Int,
    expectedCount: Int,
    currentPage: Int,
    lastAddedCount: Int,
    invalidCount: Int,
    hasMore: Boolean
): FavoriteProgressBadge {
    val safeLoaded = loadedCount.coerceAtLeast(0)
    val safeExpected = expectedCount.coerceAtLeast(0)
    val primary = if (safeExpected > 0) {
        "$safeLoaded / $safeExpected"
    } else {
        safeLoaded.toString()
    }
    val secondary = buildString {
        append("P${currentPage.coerceAtLeast(1)}")
        append("  +${lastAddedCount.coerceAtLeast(0)}")
        if (invalidCount > 0) {
            append("  异常$invalidCount")
        }
    }
    val gapCount = (safeExpected - safeLoaded).coerceAtLeast(0)
    val footnote = if (!hasMore && gapCount > 0) {
        "差额 $gapCount，可能含失效/隐藏/已删除"
    } else {
        null
    }
    return FavoriteProgressBadge(
        primaryText = primary,
        secondaryText = secondary,
        footnoteText = footnote
    )
}

internal fun resolveSubscribedFolderProgressBadge(
    loadedCount: Int,
    totalCount: Int,
    currentPage: Int,
    lastAddedCount: Int,
    hasMore: Boolean
): FavoriteProgressBadge {
    val safeLoaded = loadedCount.coerceAtLeast(0)
    val safeTotal = totalCount.coerceAtLeast(0)
    val primary = if (safeTotal > 0) {
        "$safeLoaded / $safeTotal"
    } else {
        safeLoaded.toString()
    }
    val secondary = buildString {
        append("P${currentPage.coerceAtLeast(1)}")
        append("  +${lastAddedCount.coerceAtLeast(0)}")
    }
    val footnote = if (!hasMore && safeTotal > 0 && safeLoaded < safeTotal) {
        "差额 ${(safeTotal - safeLoaded).coerceAtLeast(0)}"
    } else {
        null
    }
    return FavoriteProgressBadge(
        primaryText = primary,
        secondaryText = secondary,
        footnoteText = footnote
    )
}
