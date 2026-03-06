package com.android.purebilibili.feature.list

internal enum class CommonListLoadMoreOwner {
    NONE,
    FAVORITE,
    HISTORY,
    SEASON_SERIES_DETAIL
}

internal data class CommonListPaginationSnapshot(
    val hasMore: Boolean,
    val isLoadingMore: Boolean
)

internal fun resolveCommonListLoadMoreOwner(
    isSubscribedBrowse: Boolean,
    hasFavoriteViewModel: Boolean,
    hasHistoryViewModel: Boolean,
    hasSeasonSeriesDetailViewModel: Boolean
): CommonListLoadMoreOwner {
    if (isSubscribedBrowse) return CommonListLoadMoreOwner.NONE
    if (hasFavoriteViewModel) return CommonListLoadMoreOwner.FAVORITE
    if (hasHistoryViewModel) return CommonListLoadMoreOwner.HISTORY
    if (hasSeasonSeriesDetailViewModel) return CommonListLoadMoreOwner.SEASON_SERIES_DETAIL
    return CommonListLoadMoreOwner.NONE
}

internal fun resolveCommonListPaginationSnapshot(
    owner: CommonListLoadMoreOwner,
    favoriteHasMore: Boolean,
    favoriteIsLoadingMore: Boolean,
    historyHasMore: Boolean,
    historyIsLoadingMore: Boolean,
    seasonDetailHasMore: Boolean,
    seasonDetailIsLoadingMore: Boolean
): CommonListPaginationSnapshot {
    return when (owner) {
        CommonListLoadMoreOwner.FAVORITE -> CommonListPaginationSnapshot(
            hasMore = favoriteHasMore,
            isLoadingMore = favoriteIsLoadingMore
        )
        CommonListLoadMoreOwner.HISTORY -> CommonListPaginationSnapshot(
            hasMore = historyHasMore,
            isLoadingMore = historyIsLoadingMore
        )
        CommonListLoadMoreOwner.SEASON_SERIES_DETAIL -> CommonListPaginationSnapshot(
            hasMore = seasonDetailHasMore,
            isLoadingMore = seasonDetailIsLoadingMore
        )
        CommonListLoadMoreOwner.NONE -> CommonListPaginationSnapshot(
            hasMore = false,
            isLoadingMore = false
        )
    }
}
