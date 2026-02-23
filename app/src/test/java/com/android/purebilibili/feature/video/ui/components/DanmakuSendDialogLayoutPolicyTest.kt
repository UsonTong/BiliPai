package com.android.purebilibili.feature.video.ui.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DanmakuSendDialogLayoutPolicyTest {

    @Test
    fun `danmaku dialog should use bottom sheet style to avoid blocking video center`() {
        val policy = resolveDanmakuSendDialogLayoutPolicy()

        assertTrue(policy.bottomAligned)
        assertEquals(1f, policy.fillMaxWidthFraction)
        assertEquals(14, policy.bottomLiftDp)
    }
}
