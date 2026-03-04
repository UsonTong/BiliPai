package com.android.purebilibili.feature.home

import kotlin.test.Test
import kotlin.test.assertEquals

class HomeReturnAnimationPolicyTest {

    @Test
    fun quickReturn_withTransition_usesLongerSuppressionOnPhone() {
        assertEquals(
            380L,
            resolveReturnAnimationSuppressionDurationMs(
                isTabletLayout = false,
                cardAnimationEnabled = true,
                cardTransitionEnabled = true,
                isQuickReturnFromDetail = true
            )
        )
    }

    @Test
    fun quickReturn_withTransition_usesLongerSuppressionOnTablet() {
        assertEquals(
            500L,
            resolveReturnAnimationSuppressionDurationMs(
                isTabletLayout = true,
                cardAnimationEnabled = true,
                cardTransitionEnabled = true,
                isQuickReturnFromDetail = true
            )
        )
    }

    @Test
    fun normalReturn_usesOriginalDurations() {
        assertEquals(
            360L,
            resolveReturnAnimationSuppressionDurationMs(
                isTabletLayout = false,
                cardAnimationEnabled = true,
                cardTransitionEnabled = true,
                isQuickReturnFromDetail = false
            )
        )
        assertEquals(
            220L,
            resolveReturnAnimationSuppressionDurationMs(
                isTabletLayout = false,
                cardAnimationEnabled = false,
                cardTransitionEnabled = false,
                isQuickReturnFromDetail = false
            )
        )
    }

    @Test
    fun nonSharedReturn_usesShorterSuppressionDurations() {
        assertEquals(
            240L,
            resolveReturnAnimationSuppressionDurationMs(
                isTabletLayout = false,
                cardAnimationEnabled = true,
                cardTransitionEnabled = false,
                isQuickReturnFromDetail = false
            )
        )
        assertEquals(
            220L,
            resolveReturnAnimationSuppressionDurationMs(
                isTabletLayout = true,
                cardAnimationEnabled = true,
                cardTransitionEnabled = false,
                isQuickReturnFromDetail = false
            )
        )
    }

    @Test
    fun bottomBarRestoreDelay_respectsTransitionMode() {
        assertEquals(
            150L,
            resolveBottomBarRestoreDelayMs(
                cardTransitionEnabled = false,
                isQuickReturnFromDetail = false
            )
        )
        assertEquals(
            300L,
            resolveBottomBarRestoreDelayMs(
                cardTransitionEnabled = true,
                isQuickReturnFromDetail = true
            )
        )
    }
}
