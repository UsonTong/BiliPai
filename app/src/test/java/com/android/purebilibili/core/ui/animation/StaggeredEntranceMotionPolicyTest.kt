package com.android.purebilibili.core.ui.animation

import com.android.purebilibili.core.ui.adaptive.MotionTier
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class StaggeredEntranceMotionPolicyTest {

    @Test
    fun reducedMotion_usesShorterDelayAndSmallerOffset() {
        val policy = resolveStaggeredEntranceMotionPolicy(MotionTier.Reduced)

        assertTrue(policy.delayStepMs <= 12)
        assertTrue(policy.maxDelayMs <= 70)
        assertTrue(policy.translationDurationMs <= 210)
        assertTrue(policy.offsetFactor <= 0.32f)
        assertTrue(policy.initialScale >= 0.985f)
    }

    @Test
    fun normalMotion_shouldReduceQueueingAndTotalDuration() {
        val policy = resolveStaggeredEntranceMotionPolicy(MotionTier.Normal)

        assertTrue(policy.delayStepMs <= 28)
        assertTrue(policy.maxDelayMs <= 200)
        assertTrue(policy.translationDurationMs <= 340)
        assertTrue(policy.scaleDurationMs <= 320)
        assertTrue(policy.initialScale >= 0.95f)
    }

    @Test
    fun enhancedMotion_isExpressiveButScaleIsConstrained() {
        val normal = resolveStaggeredEntranceMotionPolicy(MotionTier.Normal)
        val enhanced = resolveStaggeredEntranceMotionPolicy(MotionTier.Enhanced)

        assertTrue(enhanced.offsetFactor >= normal.offsetFactor)
        assertTrue(enhanced.translationDurationMs > normal.translationDurationMs)
        assertTrue(enhanced.initialScale < normal.initialScale)
        assertTrue(enhanced.initialScale >= 0.92f)
    }

    @Test
    fun initialState_shouldStartAtFinalValuesWhenAlreadyVisible() {
        val policy = resolveStaggeredEntranceMotionPolicy(MotionTier.Normal)

        val state = resolveStaggeredEntranceInitialState(
            visible = true,
            offsetDistance = 50f,
            policy = policy
        )

        assertEquals(1f, state.alpha)
        assertEquals(0f, state.translationY)
        assertEquals(1f, state.scale)
    }

    @Test
    fun shouldAnimate_falseWhenAlreadyAtFinalState() {
        assertFalse(
            shouldRunStaggeredEntranceAnimation(
                visible = true,
                currentAlpha = 1f,
                currentTranslationY = 0f,
                currentScale = 1f
            )
        )
    }
}
