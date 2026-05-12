package com.android.purebilibili.feature.profile

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProfileServicesVisibilityPolicyTest {

    @Test
    fun `history service hides when history is already visible in bottom bar`() {
        assertFalse(
            shouldShowProfileHistoryService(
                bottomBarVisibleTabIds = listOf("HOME", "DYNAMIC", "HISTORY", "PROFILE")
            )
        )
    }

    @Test
    fun `history service stays visible when history is not in bottom bar`() {
        assertTrue(
            shouldShowProfileHistoryService(
                bottomBarVisibleTabIds = listOf("HOME", "DYNAMIC", "PROFILE")
            )
        )
    }
}
