package com.android.purebilibili.feature.dynamic.components

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ImagePreviewTransitionPolicyTest {

    @Test
    fun resolveImagePreviewTransitionFrame_clampsVisualProgressButKeepsLayoutOvershoot() {
        val frame = resolveImagePreviewTransitionFrame(
            rawProgress = -0.2f,
            hasSourceRect = true,
            sourceCornerRadiusDp = 12f
        )

        assertEquals(-0.08f, frame.layoutProgress)
        assertEquals(0f, frame.visualProgress)
        assertEquals(12f, frame.cornerRadiusDp)
    }

    @Test
    fun resolveImagePreviewTransitionFrame_interpolatesCornerRadiusSmoothly() {
        val frame = resolveImagePreviewTransitionFrame(
            rawProgress = 0.5f,
            hasSourceRect = true,
            sourceCornerRadiusDp = 12f
        )

        assertTrue(abs(frame.cornerRadiusDp - 6f) < 0.001f)
    }

    @Test
    fun imagePreviewDismissMotion_overshootsThenSettlesToSource() {
        val motion = imagePreviewDismissMotion()

        assertTrue(motion.overshootTarget < 0f)
        assertEquals(0f, motion.settleTarget)
    }
}
