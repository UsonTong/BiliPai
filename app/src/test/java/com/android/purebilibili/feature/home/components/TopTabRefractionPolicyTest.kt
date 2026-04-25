package com.android.purebilibili.feature.home.components

import com.android.purebilibili.core.ui.motion.BottomBarMotionProfile
import com.android.purebilibili.core.ui.motion.resolveBottomBarMotionSpec
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TopTabRefractionPolicyTest {

    @Test
    fun `indicator should not refract when stationary on integer page`() {
        assertFalse(
            shouldTopTabIndicatorUseRefraction(
                position = 1.0f,
                interacting = false,
                velocityPxPerSecond = 0f
            )
        )
    }

    @Test
    fun `indicator should refract while dragging`() {
        assertTrue(
            shouldTopTabIndicatorUseRefraction(
                position = 1.0f,
                interacting = true,
                velocityPxPerSecond = 0f
            )
        )
    }

    @Test
    fun `indicator should refract during settle phase`() {
        assertTrue(
            shouldTopTabIndicatorUseRefraction(
                position = 1.18f,
                interacting = false,
                velocityPxPerSecond = 0f
            )
        )
    }

    @Test
    fun `liquid top tab refraction forces chromatic aberration during motion`() {
        val profile = resolveTopTabRefractionMotionProfile(
            shouldRefract = true,
            velocityPxPerSecond = 1800f,
            liquidGlassEnabled = true
        )

        assertTrue(profile.forceChromaticAberration)
        assertTrue(profile.chromaticBoostScale > 1f)
        assertTrue(profile.lensAmountScale > 1f)
        assertTrue(profile.lensHeightScale > 1f)
    }

    @Test
    fun `top tab refraction profile stays neutral when liquid glass is disabled`() {
        val profile = resolveTopTabRefractionMotionProfile(
            shouldRefract = true,
            velocityPxPerSecond = 1800f,
            liquidGlassEnabled = false
        )

        assertFalse(profile.forceChromaticAberration)
        assertEquals(1f, profile.chromaticBoostScale, 0.001f)
        assertEquals(1f, profile.lensAmountScale, 0.001f)
        assertEquals(1f, profile.lensHeightScale, 0.001f)
    }

    @Test
    fun `stationary top tab indicator follows bottom bar static tint policy`() {
        val policy = resolveTopTabIndicatorVisualPolicy(
            position = 2f,
            interacting = false,
            velocityPxPerSecond = 0f,
            useNeutralIndicatorTint = true
        )

        assertFalse(policy.isInMotion)
        assertFalse(policy.shouldRefract)
        assertFalse(policy.useNeutralTint)
    }

    @Test
    fun `moving top tab indicator follows bottom bar refraction tint policy`() {
        val policy = resolveTopTabIndicatorVisualPolicy(
            position = 2.18f,
            interacting = false,
            velocityPxPerSecond = 0f,
            useNeutralIndicatorTint = true
        )

        assertTrue(policy.isInMotion)
        assertTrue(policy.shouldRefract)
        assertTrue(policy.useNeutralTint)
    }

    @Test
    fun `moving top tab item emphasis follows bottom bar text rendering policy`() {
        val top = resolveTopTabItemMotionVisual(
            itemIndex = 1,
            indicatorPosition = 0.8f,
            currentSelectedIndex = 0,
            isInMotion = true,
            selectionEmphasis = 0.28f
        )
        val bottom = resolveBottomBarItemMotionVisual(
            itemIndex = 1,
            indicatorPosition = 0.8f,
            currentSelectedIndex = 0,
            motionProgress = 1f,
            selectionEmphasis = 0.28f
        )

        assertEquals(bottom.themeWeight, top.themeWeight, 0.001f)
        assertEquals(bottom.useSelectedIcon, top.useSelectedIcon)
    }

    @Test
    fun `moving top tab refraction profile follows bottom bar chromatic policy`() {
        val top = resolveTopTabRefractionMotionProfile(
            position = 1.32f,
            shouldRefract = true,
            velocityPxPerSecond = 860f,
            liquidGlassEnabled = true
        )
        val bottom = resolveBottomBarRefractionMotionProfile(
            position = 1.32f,
            velocity = 860f,
            isDragging = true,
            motionSpec = resolveBottomBarMotionSpec(BottomBarMotionProfile.IOS_FLOATING)
        )

        assertEquals(bottom.indicatorLensAmountScale, top.lensAmountScale, 0.001f)
        assertEquals(bottom.indicatorLensHeightScale, top.lensHeightScale, 0.001f)
        assertEquals(bottom.chromaticBoostScale, top.chromaticBoostScale, 0.001f)
        assertEquals(bottom.forceChromaticAberration, top.forceChromaticAberration)
        assertEquals(bottom.visibleSelectionEmphasis, top.visibleSelectionEmphasis, 0.001f)
        assertEquals(bottom.exportSelectionEmphasis, top.exportSelectionEmphasis, 0.001f)
    }

    @Test
    fun `top tab indicator only uses combined backdrop while refracting`() {
        val idle = resolveTopTabIndicatorBackdropPolicy(
            effectiveLiquidGlassEnabled = true,
            hasBackdrop = true,
            indicatorVisualPolicy = BottomBarIndicatorVisualPolicy(
                isInMotion = false,
                shouldRefract = false,
                useNeutralTint = false
            )
        )
        val moving = resolveTopTabIndicatorBackdropPolicy(
            effectiveLiquidGlassEnabled = true,
            hasBackdrop = true,
            indicatorVisualPolicy = BottomBarIndicatorVisualPolicy(
                isInMotion = true,
                shouldRefract = true,
                useNeutralTint = true
            )
        )

        assertFalse(idle.useCombinedBackdrop)
        assertFalse(idle.useIndicatorBackdrop)
        assertTrue(moving.useCombinedBackdrop)
        assertTrue(moving.useIndicatorBackdrop)
    }

    @Test
    fun `liquid top tab keeps content sampling layer when page backdrop is unavailable`() {
        val moving = resolveTopTabIndicatorBackdropPolicy(
            effectiveLiquidGlassEnabled = true,
            hasBackdrop = false,
            indicatorVisualPolicy = BottomBarIndicatorVisualPolicy(
                isInMotion = true,
                shouldRefract = true,
                useNeutralTint = true
            )
        )

        assertTrue(moving.useIndicatorBackdrop)
        assertFalse(moving.useCombinedBackdrop)
    }
}
