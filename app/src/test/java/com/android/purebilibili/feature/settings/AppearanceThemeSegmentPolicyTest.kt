package com.android.purebilibili.feature.settings

import org.junit.Assert.assertEquals
import org.junit.Test

class AppearanceThemeSegmentPolicyTest {

    @Test
    fun `resolveThemeModeSegmentOptions should keep expected order and labels`() {
        val options = resolveThemeModeSegmentOptions()

        assertEquals(3, options.size)
        assertEquals(AppThemeMode.FOLLOW_SYSTEM, options[0].value)
        assertEquals("跟随系统", options[0].label)
        assertEquals(AppThemeMode.LIGHT, options[1].value)
        assertEquals("浅色模式", options[1].label)
        assertEquals(AppThemeMode.DARK, options[2].value)
        assertEquals("深色模式", options[2].label)
    }
}
