package com.android.purebilibili.data.repository

import com.android.purebilibili.data.model.response.FollowingUser
import kotlin.test.Test
import kotlin.test.assertEquals

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
    fun `remote blacklist users map to import items with names and faces`() {
        val items = buildBlockedUpImportItemsFromRemoteBlacks(
            listOf(
                FollowingUser(mid = 7L, uname = "测试UP", face = "https://i0.hdslb.com/test.jpg"),
                FollowingUser(mid = 8L, uname = "   ", face = "")
            )
        )

        assertEquals(
            listOf(
                BlockedUpImportItem(mid = 7L, name = "测试UP", face = "https://i0.hdslb.com/test.jpg"),
                BlockedUpImportItem(mid = 8L, name = "UP主8", face = "")
            ),
            items
        )
    }

    @Test
    fun `remote blacklist mapping drops invalid mids before import plan`() {
        val items = buildBlockedUpImportItemsFromRemoteBlacks(
            listOf(
                FollowingUser(mid = 0L, uname = "无效"),
                FollowingUser(mid = -1L, uname = "无效"),
                FollowingUser(mid = 9L, uname = "有效")
            )
        )

        assertEquals(listOf(9L), items.map { it.mid })
    }

    @Test
    fun `blocked list write message keeps local success when remote is skipped`() {
        val message = buildBlockedUpWriteMessage(
            blocked = true,
            remoteStatus = BilibiliBlockedListRemoteStatus.SKIPPED_NOT_LOGGED_IN
        )

        assertEquals("已屏蔽该 UP 主；未登录，未同步 B站黑名单", message)
    }

    @Test
    fun `blocked list write message reports remote failure without rolling back local result`() {
        val message = buildBlockedUpWriteMessage(
            blocked = false,
            remoteStatus = BilibiliBlockedListRemoteStatus.FAILED,
            remoteMessage = "csrf 校验失败"
        )

        assertEquals("已解除屏蔽；B站黑名单同步失败：csrf 校验失败", message)
    }
}
