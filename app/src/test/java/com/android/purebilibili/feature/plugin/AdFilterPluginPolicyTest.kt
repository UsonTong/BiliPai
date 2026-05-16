package com.android.purebilibili.feature.plugin

import kotlin.test.Test
import kotlin.test.assertEquals

class AdFilterPluginPolicyTest {

    @Test
    fun customListSummary_describesEmptyList() {
        assertEquals(
            AdFilterCustomListSummary(
                countText = "0 个",
                previewText = "暂无拉黑的UP主",
                hiddenCountText = null
            ),
            resolveAdFilterCustomListSummary(
                items = emptyList(),
                emptyText = "暂无拉黑的UP主"
            )
        )
    }

    @Test
    fun customListSummary_showsSmallListInline() {
        assertEquals(
            AdFilterCustomListSummary(
                countText = "2 个",
                previewText = "UP甲、UP乙",
                hiddenCountText = null
            ),
            resolveAdFilterCustomListSummary(
                items = listOf("UP甲", "UP乙"),
                emptyText = "暂无拉黑的UP主"
            )
        )
    }

    @Test
    fun customListSummary_reportsHiddenCountForLongList() {
        assertEquals(
            AdFilterCustomListSummary(
                countText = "5 个",
                previewText = "广告、推广、恰饭",
                hiddenCountText = "还有 2 个，展开查看全部"
            ),
            resolveAdFilterCustomListSummary(
                items = listOf("广告", "推广", "恰饭", "抽奖", "福利"),
                emptyText = "暂无自定义屏蔽词"
            )
        )
    }

    @Test
    fun customListVisibleItems_respectsExpandedState() {
        val items = listOf("广告", "推广", "恰饭", "抽奖", "福利")

        assertEquals(
            listOf("广告", "推广", "恰饭"),
            resolveAdFilterCustomListVisibleItems(items, expanded = false)
        )
        assertEquals(
            items,
            resolveAdFilterCustomListVisibleItems(items, expanded = true)
        )
    }

    @Test
    fun removeCustomListItem_removesOnlyMatchingEntries() {
        assertEquals(
            listOf("广告", "推广"),
            removeAdFilterCustomListItem(
                items = listOf("广告", "恰饭", "推广", "恰饭"),
                item = "恰饭"
            )
        )
    }
}
