package com.android.purebilibili.feature.search

enum class SearchEntryMotionSource {
    NONE,
    BOTTOM_BAR
}

data class SearchEntryMotionSpec(
    val durationMillis: Int,
    val initialAlpha: Float,
    val initialScale: Float,
    val initialTranslationYDp: Float,
    val transformOriginPivotX: Float,
    val transformOriginPivotY: Float
)

internal fun resolveSearchEntryMotionSpec(
    source: SearchEntryMotionSource,
    reducedMotionBudget: Boolean
): SearchEntryMotionSpec? {
    if (source != SearchEntryMotionSource.BOTTOM_BAR) return null
    return if (reducedMotionBudget) {
        SearchEntryMotionSpec(
            durationMillis = 0,
            initialAlpha = 1f,
            initialScale = 1f,
            initialTranslationYDp = 0f,
            transformOriginPivotX = 0.88f,
            transformOriginPivotY = 1f
        )
    } else {
        SearchEntryMotionSpec(
            durationMillis = 260,
            initialAlpha = 0.72f,
            initialScale = 0.92f,
            initialTranslationYDp = 26f,
            transformOriginPivotX = 0.88f,
            transformOriginPivotY = 1f
        )
    }
}
