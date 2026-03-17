package com.android.purebilibili.feature.space

import com.android.purebilibili.data.model.response.*

enum class SpaceMainTab {
    HOME,
    DYNAMIC,
    CONTRIBUTION,
    COLLECTIONS
}

data class SpaceTabContentState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val hasLoaded: Boolean = false
)

data class SpaceTabShellState(
    val selectedTab: SpaceMainTab,
    val tabStates: Map<SpaceMainTab, SpaceTabContentState>
) {
    fun withUpdatedTab(tab: SpaceMainTab, transform: (SpaceTabContentState) -> SpaceTabContentState): SpaceTabShellState {
        val current = tabStates[tab] ?: SpaceTabContentState()
        return copy(
            tabStates = tabStates + (tab to transform(current))
        )
    }

    fun withSelectedTab(tab: SpaceMainTab): SpaceTabShellState {
        return if (tab == selectedTab) this else copy(selectedTab = tab)
    }
}

data class SpaceHeaderState(
    val userInfo: SpaceUserInfo?,
    val relationStat: RelationStatData?,
    val upStat: UpStatData?,
    val topVideo: SpaceTopArcData?,
    val notice: String,
    val createdFavorites: List<FavFolder>,
    val collectedFavorites: List<FavFolder>
)

fun buildInitialTabShellState(selectedTab: SpaceMainTab = SpaceMainTab.HOME): SpaceTabShellState {
    val tabs = SpaceMainTab.values()
    return SpaceTabShellState(
        selectedTab = selectedTab,
        tabStates = tabs.associateWith { SpaceTabContentState() }
    )
}

fun tabIndexToMainTab(index: Int): SpaceMainTab {
    return when (index) {
        0 -> SpaceMainTab.HOME
        1 -> SpaceMainTab.DYNAMIC
        2 -> SpaceMainTab.CONTRIBUTION
        3 -> SpaceMainTab.COLLECTIONS
        else -> SpaceMainTab.HOME
    }
}

fun mainTabToTabIndex(tab: SpaceMainTab): Int {
    return when (tab) {
        SpaceMainTab.HOME -> 0
        SpaceMainTab.DYNAMIC -> 1
        SpaceMainTab.CONTRIBUTION -> 2
        SpaceMainTab.COLLECTIONS -> 3
    }
}

fun buildHeaderState(
    userInfo: SpaceUserInfo?,
    relationStat: RelationStatData?,
    upStat: UpStatData?,
    topVideo: SpaceTopArcData?,
    notice: String,
    createdFavorites: List<FavFolder>,
    collectedFavorites: List<FavFolder>
): SpaceHeaderState {
    return SpaceHeaderState(
        userInfo = userInfo,
        relationStat = relationStat,
        upStat = upStat,
        topVideo = topVideo,
        notice = notice,
        createdFavorites = createdFavorites,
        collectedFavorites = collectedFavorites
    )
}

internal fun shouldEnableSpaceTopPhotoPreview(topPhotoUrl: String): Boolean {
    return normalizeSpaceTopPhotoUrl(topPhotoUrl).isNotBlank()
}

internal fun resolveSpaceTopPhoto(
    topPhoto: String,
    cardLargePhoto: String,
    cardSmallPhoto: String
): String {
    return sequenceOf(topPhoto, cardLargePhoto, cardSmallPhoto)
        .map { normalizeSpaceTopPhotoUrl(it) }
        .firstOrNull { it.isNotEmpty() }
        .orEmpty()
}

internal fun normalizeSpaceTopPhotoUrl(url: String): String {
    val candidate = url.trim()
    if (candidate.isEmpty()) return ""
    val lower = candidate.lowercase()
    if (
        lower == "null" ||
        lower == "nil" ||
        lower == "none" ||
        lower == "undefined" ||
        lower == "[]" ||
        lower == "{}" ||
        lower == "n/a" ||
        lower == "about:blank"
    ) {
        return ""
    }
    return when {
        candidate.startsWith("//") -> "https:$candidate"
        candidate.startsWith("http://", ignoreCase = true) -> {
            "https://${candidate.substring(startIndex = "http://".length)}"
        }
        else -> candidate
    }
}

internal fun resolveSpaceFavoriteFoldersForDisplay(folders: List<FavFolder>): List<FavFolder> {
    if (folders.isEmpty()) return emptyList()
    val seenIds = HashSet<Long>()
    return folders.filter { folder ->
        val valid = folder.id > 0L &&
            folder.title.isNotBlank() &&
            folder.media_count > 0
        valid && seenIds.add(folder.id)
    }
}

internal fun resolveSpaceCollectionTabCount(
    seasonCount: Int,
    seriesCount: Int,
    createdFavoriteCount: Int,
    collectedFavoriteCount: Int
): Int {
    return seasonCount.coerceAtLeast(0) +
        seriesCount.coerceAtLeast(0) +
        createdFavoriteCount.coerceAtLeast(0) +
        collectedFavoriteCount.coerceAtLeast(0)
}
