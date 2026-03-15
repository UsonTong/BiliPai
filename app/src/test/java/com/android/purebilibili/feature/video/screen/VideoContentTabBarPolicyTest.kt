package com.android.purebilibili.feature.video.screen

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class VideoContentTabBarPolicyTest {

    @Test
    fun `tab bar layout reserves trailing danmaku action area`() {
        val spec = resolveVideoContentTabBarLayoutSpec()

        assertEquals(1f, spec.tabsRowWeight)
        assertTrue(spec.tabsRowScrollable)
    }

    @Test
    fun `danmaku input visible when player is expanded`() {
        assertTrue(
            shouldShowDanmakuSendInput(
                isPlayerCollapsed = false
            )
        )
    }

    @Test
    fun `danmaku input hidden when player is collapsed`() {
        assertFalse(
            shouldShowDanmakuSendInput(
                isPlayerCollapsed = true
            )
        )
    }

    @Test
    fun `danmaku action layout keeps settings target comfortably tappable`() {
        val policy = resolveVideoContentTabBarDanmakuActionLayoutPolicy()

        assertEquals(40, policy.settingsButtonSizeDp)
        assertEquals(20, policy.settingsIconSizeDp)
        assertEquals(22, policy.sendBadgeSizeDp)
    }
}
