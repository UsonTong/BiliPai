package com.android.purebilibili.core.ui.wallpaper

import com.android.purebilibili.core.util.WindowWidthSizeClass
import org.junit.Assert.assertEquals
import org.junit.Test

class WallpaperPresentationPolicyTest {

    @Test
    fun splash_compactWidth_usesFullCrop() {
        assertEquals(
            SplashWallpaperLayout.FULL_CROP,
            resolveSplashWallpaperLayout(WindowWidthSizeClass.Compact)
        )
    }

    @Test
    fun splash_tabletWidth_usesPosterCardWithBlurBackground() {
        assertEquals(
            SplashWallpaperLayout.POSTER_CARD_BLUR_BG,
            resolveSplashWallpaperLayout(WindowWidthSizeClass.Medium)
        )
        assertEquals(
            SplashWallpaperLayout.POSTER_CARD_BLUR_BG,
            resolveSplashWallpaperLayout(WindowWidthSizeClass.Expanded)
        )
    }

    @Test
    fun profile_compactWidth_usesTopBannerWithBlurBackground() {
        assertEquals(
            ProfileWallpaperLayout.TOP_BANNER_BLUR_BG,
            resolveProfileWallpaperLayout(WindowWidthSizeClass.Compact)
        )
    }

    @Test
    fun profile_tabletWidth_usesPosterCardWithBlurBackground() {
        assertEquals(
            ProfileWallpaperLayout.POSTER_CARD_BLUR_BG,
            resolveProfileWallpaperLayout(WindowWidthSizeClass.Medium)
        )
        assertEquals(
            ProfileWallpaperLayout.POSTER_CARD_BLUR_BG,
            resolveProfileWallpaperLayout(WindowWidthSizeClass.Expanded)
        )
    }
}
