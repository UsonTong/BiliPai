package com.android.purebilibili.feature.settings

import com.android.purebilibili.core.store.SettingsManager
import org.junit.Assert.assertEquals
import org.junit.Test

class FeedSettingsSelectionPolicyTest {

    @Test
    fun `resolveFeedApiSegmentOptions should preserve enum order and labels`() {
        val options = resolveFeedApiSegmentOptions()

        assertEquals(SettingsManager.FeedApiType.entries.size, options.size)
        assertEquals(SettingsManager.FeedApiType.WEB, options[0].value)
        assertEquals("网页端 (Web)", options[0].label)
        assertEquals(SettingsManager.FeedApiType.MOBILE, options[1].value)
        assertEquals("移动端 (App)", options[1].label)
    }
}
