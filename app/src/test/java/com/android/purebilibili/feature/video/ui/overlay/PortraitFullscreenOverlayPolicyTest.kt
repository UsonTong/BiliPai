package com.android.purebilibili.feature.video.ui.overlay

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PortraitFullscreenOverlayPolicyTest {

    @Test
    fun useCompactTopBarOnNarrowScreen() {
        assertTrue(shouldUseCompactPortraitTopBar(screenWidthDp = 360))
        assertTrue(shouldUseCompactPortraitTopBar(screenWidthDp = 320))
    }

    @Test
    fun keepNormalTopBarOnWideScreen() {
        assertFalse(shouldUseCompactPortraitTopBar(screenWidthDp = 411))
    }

    @Test
    fun hideViewCountInCompactMode() {
        assertFalse(shouldShowPortraitViewCount(viewCount = 12345, compactMode = true))
    }

    @Test
    fun showViewCountInNormalModeWhenHasData() {
        assertTrue(shouldShowPortraitViewCount(viewCount = 12345, compactMode = false))
    }

    @Test
    fun hideViewCountWhenNoData() {
        assertFalse(shouldShowPortraitViewCount(viewCount = 0, compactMode = false))
    }
}
