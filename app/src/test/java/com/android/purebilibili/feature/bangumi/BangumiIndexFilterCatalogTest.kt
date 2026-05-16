package com.android.purebilibili.feature.bangumi

import com.android.purebilibili.data.model.response.BangumiFilter
import com.android.purebilibili.data.model.response.BangumiIndexFilterGroupKey
import com.android.purebilibili.data.model.response.BangumiType
import com.android.purebilibili.data.model.response.applyBangumiIndexFilterOption
import com.android.purebilibili.data.model.response.resolveBangumiIndexFilterGroups
import com.android.purebilibili.data.model.response.resolveBangumiIndexFilterKey
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BangumiIndexFilterCatalogTest {

    @Test
    fun `documentary filter catalog should include documented groups`() {
        val groups = resolveBangumiIndexFilterGroups(
            seasonType = BangumiType.DOCUMENTARY.value,
            currentYear = 2026
        )

        assertEquals(
            listOf(
                BangumiIndexFilterGroupKey.ORDER,
                BangumiIndexFilterGroupKey.STYLE,
                BangumiIndexFilterGroupKey.PRODUCER,
                BangumiIndexFilterGroupKey.YEAR,
                BangumiIndexFilterGroupKey.SEASON_STATUS
            ),
            groups.map { it.key }
        )
        assertTrue(groups.first { it.key == BangumiIndexFilterGroupKey.STYLE }.options.any { it.label == "历史" && it.styleId == 25 })
        assertTrue(groups.first { it.key == BangumiIndexFilterGroupKey.PRODUCER }.options.any { it.label == "BBC" && it.producerId == 1 })
        assertTrue(groups.first { it.key == BangumiIndexFilterGroupKey.SEASON_STATUS }.options.any { it.label == "大会员" && it.seasonStatus == "4,6" })
    }

    @Test
    fun `year catalog should start from current year and include range options`() {
        val yearGroup = resolveBangumiIndexFilterGroups(
            seasonType = BangumiType.MOVIE.value,
            currentYear = 2026
        ).first { it.key == BangumiIndexFilterGroupKey.YEAR }

        assertEquals("全部年份", yearGroup.options[0].label)
        assertEquals("2026", yearGroup.options[1].label)
        assertEquals("[2026,2027)", yearGroup.options[1].year)
        assertTrue(yearGroup.options.any { it.label == "2015-2010" && it.year == "[2010,2016)" })
        assertTrue(yearGroup.options.any { it.label == "更早" && it.year == "[,1980)" })
    }

    @Test
    fun `anime filter catalog should keep anime specific groups`() {
        val groups = resolveBangumiIndexFilterGroups(
            seasonType = BangumiType.ANIME.value,
            currentYear = 2026
        )

        assertTrue(groups.any { it.key == BangumiIndexFilterGroupKey.YEAR })
        assertTrue(groups.none { it.key == BangumiIndexFilterGroupKey.PRODUCER })
        assertTrue(groups.none { it.key == BangumiIndexFilterGroupKey.SEASON_STATUS })
    }

    @Test
    fun `applying filter option should update only matching field and change filter key`() {
        val styleOption = resolveBangumiIndexFilterGroups(
            seasonType = BangumiType.DOCUMENTARY.value,
            currentYear = 2026
        )
            .first { it.key == BangumiIndexFilterGroupKey.STYLE }
            .options
            .first { it.label == "历史" }

        val updated = applyBangumiIndexFilterOption(BangumiFilter(producerId = 4), styleOption)

        assertEquals(25, updated.styleId)
        assertEquals(4, updated.producerId)
        assertEquals("3|-1|25|4|-1|-1|3|0", resolveBangumiIndexFilterKey(BangumiType.DOCUMENTARY.value, updated))
    }
}
