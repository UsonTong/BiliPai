package com.android.purebilibili.feature.video.ui.section

import kotlin.test.Test
import kotlin.test.assertEquals

class VideoPlayerSeekGesturePolicyTest {

    @Test
    fun `fullscreen uses fixed step when setting enabled`() {
        val delta = resolveHorizontalSeekDeltaMs(
            isFullscreen = true,
            fullscreenSwipeSeekEnabled = true,
            totalDragDistanceX = 110f,
            containerWidthPx = 800f,
            fullscreenSwipeSeekSeconds = 15,
            gestureSensitivity = 1f
        )

        assertEquals(15_000L, delta)
    }

    @Test
    fun `fullscreen falls back to linear when setting disabled`() {
        val delta = resolveHorizontalSeekDeltaMs(
            isFullscreen = true,
            fullscreenSwipeSeekEnabled = false,
            totalDragDistanceX = 110f,
            containerWidthPx = 800f,
            fullscreenSwipeSeekSeconds = 15,
            gestureSensitivity = 1f
        )

        assertEquals(22_000L, delta)
    }

    @Test
    fun `portrait always uses linear seek regardless of fullscreen setting`() {
        val delta = resolveHorizontalSeekDeltaMs(
            isFullscreen = false,
            fullscreenSwipeSeekEnabled = true,
            totalDragDistanceX = 50f,
            containerWidthPx = 800f,
            fullscreenSwipeSeekSeconds = 30,
            gestureSensitivity = 1.2f
        )

        assertEquals(12_000L, delta)
    }
}
