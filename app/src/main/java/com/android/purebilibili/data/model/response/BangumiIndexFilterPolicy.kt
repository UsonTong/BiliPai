package com.android.purebilibili.data.model.response

enum class BangumiIndexFilterGroupKey {
    ORDER,
    STYLE,
    PRODUCER,
    YEAR,
    SEASON_STATUS
}

data class BangumiIndexFilterOption(
    val label: String,
    val order: Int? = null,
    val sortDirection: Int? = null,
    val styleId: Int? = null,
    val producerId: Int? = null,
    val year: String? = null,
    val seasonStatus: String? = null
)

data class BangumiIndexFilterGroup(
    val key: BangumiIndexFilterGroupKey,
    val title: String,
    val options: List<BangumiIndexFilterOption>
)

data class BangumiIndexRequestFilter(
    val order: Int,
    val sortDirection: Int,
    val area: Int,
    val isFinish: Int,
    val year: String,
    val releaseDate: String,
    val styleId: Int,
    val producerId: Int,
    val seasonStatus: String
)

fun buildBangumiIndexRequestFilter(
    filter: BangumiFilter,
    seasonType: Int
): BangumiIndexRequestFilter {
    return BangumiIndexRequestFilter(
        order = filter.order,
        sortDirection = filter.sortDirection,
        area = filter.area,
        isFinish = filter.isFinish,
        year = filter.toApiYear(seasonType),
        releaseDate = filter.toApiReleaseDate(seasonType),
        styleId = filter.styleId,
        producerId = filter.producerId,
        seasonStatus = filter.seasonStatus
    )
}

fun resolveBangumiIndexFilterGroups(
    seasonType: Int,
    currentYear: Int
): List<BangumiIndexFilterGroup> {
    val groups = mutableListOf<BangumiIndexFilterGroup>()
    groups += BangumiIndexFilterGroup(
        key = BangumiIndexFilterGroupKey.ORDER,
        title = "综合排序",
        options = listOf(
            BangumiIndexFilterOption(label = "综合排序", order = 3, sortDirection = 0),
            BangumiIndexFilterOption(label = "最多播放", order = 2, sortDirection = 0),
            BangumiIndexFilterOption(label = "最近更新", order = 0, sortDirection = 0),
            BangumiIndexFilterOption(label = "最高评分", order = 4, sortDirection = 0)
        )
    )

    groups += BangumiIndexFilterGroup(
        key = BangumiIndexFilterGroupKey.STYLE,
        title = "全部风格",
        options = resolveBangumiStyleOptions(seasonType)
    )

    if (seasonType == BangumiType.DOCUMENTARY.value) {
        groups += BangumiIndexFilterGroup(
            key = BangumiIndexFilterGroupKey.PRODUCER,
            title = "全部出品",
            options = DOCUMENTARY_PRODUCER_OPTIONS
        )
    }

    groups += BangumiIndexFilterGroup(
        key = BangumiIndexFilterGroupKey.YEAR,
        title = "全部年份",
        options = resolveBangumiYearOptions(currentYear)
    )

    if (seasonType == BangumiType.MOVIE.value ||
        seasonType == BangumiType.DOCUMENTARY.value ||
        seasonType == BangumiType.TV_SHOW.value ||
        seasonType == BangumiType.VARIETY.value
    ) {
        groups += BangumiIndexFilterGroup(
            key = BangumiIndexFilterGroupKey.SEASON_STATUS,
            title = "付费类型",
            options = SEASON_STATUS_OPTIONS
        )
    }

    return groups
}

fun applyBangumiIndexFilterOption(
    filter: BangumiFilter,
    option: BangumiIndexFilterOption
): BangumiFilter {
    return filter.copy(
        order = option.order ?: filter.order,
        sortDirection = option.sortDirection ?: filter.sortDirection,
        styleId = option.styleId ?: filter.styleId,
        producerId = option.producerId ?: filter.producerId,
        year = option.year ?: filter.year,
        seasonStatus = option.seasonStatus ?: filter.seasonStatus
    )
}

fun resolveBangumiIndexSelectedOption(
    filter: BangumiFilter,
    group: BangumiIndexFilterGroup
): BangumiIndexFilterOption {
    return group.options.firstOrNull { option ->
        when (group.key) {
            BangumiIndexFilterGroupKey.ORDER -> option.order == filter.order &&
                option.sortDirection == filter.sortDirection
            BangumiIndexFilterGroupKey.STYLE -> option.styleId == filter.styleId
            BangumiIndexFilterGroupKey.PRODUCER -> option.producerId == filter.producerId
            BangumiIndexFilterGroupKey.YEAR -> option.year == filter.year
            BangumiIndexFilterGroupKey.SEASON_STATUS -> option.seasonStatus == filter.seasonStatus
        }
    } ?: group.options.first()
}

fun resolveBangumiIndexFilterKey(
    seasonType: Int,
    filter: BangumiFilter
): String {
    return listOf(
        seasonType,
        filter.area,
        filter.styleId,
        filter.producerId,
        filter.year,
        filter.seasonStatus,
        filter.order,
        filter.sortDirection
    ).joinToString("|")
}

private fun resolveBangumiStyleOptions(seasonType: Int): List<BangumiIndexFilterOption> {
    return when (seasonType) {
        BangumiType.DOCUMENTARY.value -> DOCUMENTARY_STYLE_OPTIONS
        else -> listOf(BangumiIndexFilterOption(label = "全部风格", styleId = -1))
    }
}

private fun resolveBangumiYearOptions(currentYear: Int): List<BangumiIndexFilterOption> {
    val safeCurrentYear = currentYear.coerceAtLeast(1980)
    val recentYears = (safeCurrentYear downTo 2016).map { year ->
        BangumiIndexFilterOption(
            label = year.toString(),
            year = "[$year,${year + 1})"
        )
    }
    return listOf(BangumiIndexFilterOption(label = "全部年份", year = "-1")) +
        recentYears +
        listOf(
            BangumiIndexFilterOption(label = "2015-2010", year = "[2010,2016)"),
            BangumiIndexFilterOption(label = "2009-2005", year = "[2005,2010)"),
            BangumiIndexFilterOption(label = "2004-2000", year = "[2000,2005)"),
            BangumiIndexFilterOption(label = "90年代", year = "[1990,2000)"),
            BangumiIndexFilterOption(label = "80年代", year = "[1980,1990)"),
            BangumiIndexFilterOption(label = "更早", year = "[,1980)")
        )
}

private val DOCUMENTARY_STYLE_OPTIONS = listOf(
    BangumiIndexFilterOption(label = "全部风格", styleId = -1),
    BangumiIndexFilterOption(label = "历史", styleId = 25),
    BangumiIndexFilterOption(label = "美食", styleId = 39),
    BangumiIndexFilterOption(label = "人文", styleId = 19),
    BangumiIndexFilterOption(label = "科技", styleId = 27),
    BangumiIndexFilterOption(label = "探险", styleId = 29),
    BangumiIndexFilterOption(label = "宇宙", styleId = 1201),
    BangumiIndexFilterOption(label = "萌宠", styleId = 1202),
    BangumiIndexFilterOption(label = "社会", styleId = 1203),
    BangumiIndexFilterOption(label = "动物", styleId = 989),
    BangumiIndexFilterOption(label = "自然", styleId = 34),
    BangumiIndexFilterOption(label = "医疗", styleId = 1204),
    BangumiIndexFilterOption(label = "军事", styleId = 988),
    BangumiIndexFilterOption(label = "灾难", styleId = 1205),
    BangumiIndexFilterOption(label = "旅行", styleId = 31)
)

private val DOCUMENTARY_PRODUCER_OPTIONS = listOf(
    BangumiIndexFilterOption(label = "全部出品", producerId = -1),
    BangumiIndexFilterOption(label = "央视", producerId = 4),
    BangumiIndexFilterOption(label = "BBC", producerId = 1),
    BangumiIndexFilterOption(label = "探索频道", producerId = 7),
    BangumiIndexFilterOption(label = "NHK", producerId = 2),
    BangumiIndexFilterOption(label = "历史频道", producerId = 6),
    BangumiIndexFilterOption(label = "卫视", producerId = 8),
    BangumiIndexFilterOption(label = "自制", producerId = 9),
    BangumiIndexFilterOption(label = "ITV", producerId = 5),
    BangumiIndexFilterOption(label = "SKY", producerId = 3),
    BangumiIndexFilterOption(label = "ZDF", producerId = 10),
    BangumiIndexFilterOption(label = "合作机构", producerId = 11),
    BangumiIndexFilterOption(label = "国内其他", producerId = 12),
    BangumiIndexFilterOption(label = "国外其他", producerId = 13)
)

private val SEASON_STATUS_OPTIONS = listOf(
    BangumiIndexFilterOption(label = "全部", seasonStatus = "-1"),
    BangumiIndexFilterOption(label = "免费", seasonStatus = "1"),
    BangumiIndexFilterOption(label = "大会员", seasonStatus = "4,6")
)
