package com.android.purebilibili.feature.dynamic

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

internal enum class DynamicVideoCardLayoutMode {
    VERTICAL,
    HORIZONTAL
}

internal fun resolveDynamicFeedMaxWidth(): Dp = 480.dp

internal fun resolveDynamicVideoCardLayoutMode(containerWidthDp: Int): DynamicVideoCardLayoutMode {
    return DynamicVideoCardLayoutMode.VERTICAL
}

internal fun resolveDynamicHorizontalUserListHorizontalPadding(): Dp = 10.dp

internal fun resolveDynamicHorizontalUserListSpacing(): Dp = 10.dp

internal fun resolveDynamicTopBarHorizontalPadding(): Dp = 14.dp

internal fun resolveDynamicTopBarTabEndPadding(): Dp = 20.dp

internal fun resolveDynamicSidebarWidth(isExpanded: Boolean): Dp {
    return if (isExpanded) 68.dp else 60.dp
}

internal fun shouldShowDynamicUserLiveBadge(isLive: Boolean): Boolean = isLive

internal fun resolveDynamicUserLiveBadgeLabel(): String = "直播"

internal fun resolveDynamicUserLiveBadgeHeight(): Dp = 16.dp

internal fun resolveDynamicUserLiveBadgeMinWidth(): Dp = 24.dp

internal fun resolveDynamicUserLiveBadgeReservedSpace(): Dp = 8.dp

internal fun resolveDynamicCardOuterPadding(): Dp = 0.dp

internal fun resolveDynamicCardContentPadding(): Dp = 12.dp

internal fun resolveDynamicActionButtonText(label: String, count: Int): String? {
    val countText = if (count > 0) formatDynamicActionCount(count) else null
    return when (label) {
        "转发", "评论" -> listOfNotNull(label, countText).joinToString(separator = " ")
        else -> countText
    }
}

private fun formatDynamicActionCount(count: Int): String {
    return when {
        count >= 10000 -> "${count / 10000}万"
        count >= 1000 -> String.format("%.1fk", count / 1000f)
        else -> count.toString()
    }
}
