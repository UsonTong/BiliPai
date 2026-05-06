package com.android.purebilibili.feature.home.components.cards

internal fun resolveVideoCardPrimaryStatBadgeMinWidthDp(
    statText: String
): Float {
    val normalizedLength = statText.trim().length.coerceAtLeast(3)
    return (34f + normalizedLength * 6f).coerceIn(52f, 72f)
}
