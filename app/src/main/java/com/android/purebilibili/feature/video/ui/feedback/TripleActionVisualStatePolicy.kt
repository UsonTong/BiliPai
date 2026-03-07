package com.android.purebilibili.feature.video.ui.feedback

data class TripleActionVisualState(
    val isLiked: Boolean,
    val coinCount: Int,
    val isFavorited: Boolean
)

fun resolveTripleActionVisualState(
    currentLiked: Boolean,
    currentCoinCount: Int,
    currentFavorited: Boolean,
    likeSuccess: Boolean,
    coinSuccess: Boolean,
    favoriteSuccess: Boolean
): TripleActionVisualState {
    return TripleActionVisualState(
        isLiked = currentLiked || likeSuccess,
        coinCount = if (coinSuccess) maxOf(currentCoinCount, 2) else currentCoinCount,
        isFavorited = currentFavorited || favoriteSuccess
    )
}
