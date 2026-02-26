package com.android.purebilibili.core.ui.wallpaper

import com.android.purebilibili.core.util.WindowWidthSizeClass

enum class SplashWallpaperLayout {
    FULL_CROP,
    POSTER_CARD_BLUR_BG
}

enum class ProfileWallpaperLayout {
    TOP_BANNER_BLUR_BG,
    POSTER_CARD_BLUR_BG
}

fun resolveSplashWallpaperLayout(widthSizeClass: WindowWidthSizeClass): SplashWallpaperLayout {
    return when (widthSizeClass) {
        WindowWidthSizeClass.Compact -> SplashWallpaperLayout.FULL_CROP
        WindowWidthSizeClass.Medium,
        WindowWidthSizeClass.Expanded -> SplashWallpaperLayout.POSTER_CARD_BLUR_BG
    }
}

fun resolveProfileWallpaperLayout(widthSizeClass: WindowWidthSizeClass): ProfileWallpaperLayout {
    return when (widthSizeClass) {
        WindowWidthSizeClass.Compact -> ProfileWallpaperLayout.TOP_BANNER_BLUR_BG
        WindowWidthSizeClass.Medium,
        WindowWidthSizeClass.Expanded -> ProfileWallpaperLayout.POSTER_CARD_BLUR_BG
    }
}
