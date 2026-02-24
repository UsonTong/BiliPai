package com.android.purebilibili.feature.video.screen

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VideoDetailEntryVisualPolicyTest {

    @Test
    fun `disabled transition should not apply blur or scrim`() {
        val frame = resolveVideoDetailEntryVisualFrame(
            rawProgress = 0.1f,
            transitionEnabled = false,
            maxBlurRadiusPx = 28f
        )

        assertEquals(1f, frame.contentAlpha)
        assertEquals(0f, frame.scrimAlpha)
        assertEquals(0f, frame.blurRadiusPx)
    }

    @Test
    fun `entry frame should clamp progress and decrease blur as progress increases`() {
        val start = resolveVideoDetailEntryVisualFrame(
            rawProgress = -0.2f,
            transitionEnabled = true,
            maxBlurRadiusPx = 20f
        )
        val mid = resolveVideoDetailEntryVisualFrame(
            rawProgress = 0.5f,
            transitionEnabled = true,
            maxBlurRadiusPx = 20f
        )
        val end = resolveVideoDetailEntryVisualFrame(
            rawProgress = 1.2f,
            transitionEnabled = true,
            maxBlurRadiusPx = 20f
        )

        assertEquals(20f, start.blurRadiusPx)
        assertTrue(mid.blurRadiusPx in 9f..11f)
        assertEquals(0f, end.blurRadiusPx)

        assertTrue(start.scrimAlpha > mid.scrimAlpha)
        assertEquals(0f, end.scrimAlpha)
        assertTrue(start.contentAlpha < mid.contentAlpha)
        assertEquals(1f, end.contentAlpha)
    }
}
