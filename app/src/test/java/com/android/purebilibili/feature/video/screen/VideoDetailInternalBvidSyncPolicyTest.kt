package com.android.purebilibili.feature.video.screen

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class VideoDetailInternalBvidSyncPolicyTest {

    @Test
    fun nonPortraitRegularPlaybackSwitch_shouldNotForceReloadFromRouteBvid() {
        assertFalse(
            shouldSyncMainPlayerToInternalBvid(
                isPortraitFullscreen = false,
                routeBvid = "BV_ROUTE",
                currentBvid = "BV_ROUTE",
                loadedBvid = "BV_NEXT"
            )
        )
    }

    @Test
    fun portraitExitInternalTarget_shouldSyncMainPlayer() {
        assertTrue(
            shouldSyncMainPlayerToInternalBvid(
                isPortraitFullscreen = false,
                routeBvid = "BV_ROUTE",
                currentBvid = "BV_TARGET",
                loadedBvid = "BV_ROUTE"
            )
        )
    }

    @Test
    fun alreadySyncedTarget_shouldNotReloadAgain() {
        assertFalse(
            shouldSyncMainPlayerToInternalBvid(
                isPortraitFullscreen = false,
                routeBvid = "BV_ROUTE",
                currentBvid = "BV_TARGET",
                loadedBvid = "BV_TARGET"
            )
        )
    }

    @Test
    fun portraitFullscreenActive_shouldNotSyncMainPlayer() {
        assertFalse(
            shouldSyncMainPlayerToInternalBvid(
                isPortraitFullscreen = true,
                routeBvid = "BV_ROUTE",
                currentBvid = "BV_TARGET",
                loadedBvid = "BV_ROUTE"
            )
        )
    }
}
