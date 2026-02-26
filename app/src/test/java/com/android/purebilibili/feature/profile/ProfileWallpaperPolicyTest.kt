package com.android.purebilibili.feature.profile

import com.android.purebilibili.core.util.WindowWidthSizeClass
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProfileWallpaperPolicyTest {

    @Test
    fun compactProfileTopBanner_usesTallerWallpaperCoverage() {
        assertEquals(420f, resolveProfileTopBannerHeightDp(WindowWidthSizeClass.Compact), 0.001f)
    }

    @Test
    fun compactProfileTopBanner_isTallerThanTabletBannerHeight() {
        val compactHeight = resolveProfileTopBannerHeightDp(WindowWidthSizeClass.Compact)
        val expandedHeight = resolveProfileTopBannerHeightDp(WindowWidthSizeClass.Expanded)
        assertTrue(compactHeight > expandedHeight)
    }
}
