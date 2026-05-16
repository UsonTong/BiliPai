package com.android.purebilibili.navigation

import androidx.lifecycle.Lifecycle
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AudioModeViewModelOwnerPolicyTest {

    @Test
    fun `shares previous entry when it is an active video route`() {
        assertTrue(
            shouldShareAudioModeViewModelWithPreviousEntry(
                previousRoute = "video/BV1active?cid=1",
                previousLifecycleState = Lifecycle.State.RESUMED
            )
        )
    }

    @Test
    fun `does not share previous entry after it is destroyed`() {
        assertFalse(
            shouldShareAudioModeViewModelWithPreviousEntry(
                previousRoute = "video/BV1destroyed?cid=1",
                previousLifecycleState = Lifecycle.State.DESTROYED
            )
        )
    }

    @Test
    fun `does not share previous entry when route is not video`() {
        assertFalse(
            shouldShareAudioModeViewModelWithPreviousEntry(
                previousRoute = ScreenRoutes.Settings.route,
                previousLifecycleState = Lifecycle.State.STARTED
            )
        )
    }

    @Test
    fun `audio mode returns to previous video when current bvid is unchanged`() {
        assertFalse(
            shouldNavigateAudioModeBackToCurrentVideo(
                previousVideoBvid = "BV1same",
                currentVideoBvid = "BV1same"
            )
        )
    }

    @Test
    fun `audio mode navigates to current video when queue advanced`() {
        assertTrue(
            shouldNavigateAudioModeBackToCurrentVideo(
                previousVideoBvid = "BV1first",
                currentVideoBvid = "BV2current"
            )
        )
    }

    @Test
    fun `audio mode ignores blank current bvid`() {
        assertFalse(
            shouldNavigateAudioModeBackToCurrentVideo(
                previousVideoBvid = "BV1first",
                currentVideoBvid = ""
            )
        )
    }
}
