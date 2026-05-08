package com.android.purebilibili.data.repository

import com.android.purebilibili.data.model.response.RelationTagItem
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class BlockedUpImportPolicyTest {

    @Test
    fun `import plan skips existing mids and duplicate incoming rows`() {
        val plan = buildBlockedUpImportPlan(
            existingMids = setOf(42L),
            items = listOf(
                BlockedUpImportItem(mid = 42L, name = "已存在"),
                BlockedUpImportItem(mid = 7L, name = ""),
                BlockedUpImportItem(mid = 7L, name = "重复"),
                BlockedUpImportItem(mid = 0L, name = "无效")
            )
        )

        assertEquals(listOf(7L), plan.itemsToInsert.map { it.mid })
        assertEquals("UP主7", plan.itemsToInsert.single().name)
        assertEquals(1, plan.existingCount)
        assertEquals(2, plan.failedCount)
    }

    @Test
    fun `blocked list tag finder recognizes blacklist names without hardcoded ids`() {
        val tag = findBilibiliBlockedListTag(
            listOf(
                RelationTagItem(tagid = 1L, name = "特别关注"),
                RelationTagItem(tagid = 9L, name = "黑名单")
            )
        )

        assertEquals(9L, tag?.tagid)
    }

    @Test
    fun `blocked list tag finder returns null when server exposes no blacklist group`() {
        val tag = findBilibiliBlockedListTag(
            listOf(
                RelationTagItem(tagid = 1L, name = "默认分组"),
                RelationTagItem(tagid = 2L, name = "游戏区")
            )
        )

        assertNull(tag)
    }
}
