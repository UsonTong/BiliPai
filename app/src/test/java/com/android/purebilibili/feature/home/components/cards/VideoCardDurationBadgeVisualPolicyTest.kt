package com.android.purebilibili.feature.home.components.cards

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VideoCardDurationBadgeVisualPolicyTest {

    @Test
    fun `duration badge style should use transparent background to avoid black border`() {
        val style = resolveVideoCardDurationBadgeVisualStyle()

        assertEquals(0f, style.backgroundAlpha, 0.0001f)
        assertTrue(style.textShadowAlpha > 0f)
        assertTrue(style.textShadowBlurRadiusPx > 0f)
    }
}
