package com.android.purebilibili.feature.profile

import com.android.purebilibili.data.model.response.SplashItem
import org.junit.Assert.assertEquals
import org.junit.Test

class SplashWallpaperRandomPoolPolicyTest {

    @Test
    fun `resolveVisibleSplashWallpaperPool uses thumb first then image with https normalization`() {
        val pool = resolveVisibleSplashWallpaperPool(
            listOf(
                SplashItem(id = 1L, thumb = "//a.jpg", image = ""),
                SplashItem(id = 2L, thumb = "", image = "http://b.jpg"),
                SplashItem(id = 3L, thumb = "https://c.jpg", image = "https://ignored.jpg")
            )
        )

        assertEquals(
            listOf("https://a.jpg", "https://b.jpg", "https://c.jpg"),
            pool
        )
    }

    @Test
    fun `resolveVisibleSplashWallpaperPool removes blanks and duplicates while keeping order`() {
        val pool = resolveVisibleSplashWallpaperPool(
            listOf(
                SplashItem(id = 1L, thumb = "", image = ""),
                SplashItem(id = 2L, thumb = "https://a.jpg", image = ""),
                SplashItem(id = 3L, thumb = "https://a.jpg", image = ""),
                SplashItem(id = 4L, thumb = "", image = "https://b.jpg")
            )
        )

        assertEquals(listOf("https://a.jpg", "https://b.jpg"), pool)
    }
}
