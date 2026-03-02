package com.android.purebilibili.feature.video.screen

import android.content.pm.ActivityInfo
import com.android.purebilibili.core.store.FullscreenMode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class VideoDetailLayoutModePolicyTest {

    @Test
    fun expanded_usesTabletLayout() {
        assertTrue(
            shouldUseTabletVideoLayout(
                isExpandedScreen = true,
                smallestScreenWidthDp = 700
            )
        )
    }

    @Test
    fun compact_doesNotUseTabletLayout() {
        assertFalse(
            shouldUseTabletVideoLayout(
                isExpandedScreen = false,
                smallestScreenWidthDp = 700
            )
        )
    }

    @Test
    fun expandedPhoneLandscape_doesNotUseTabletLayout() {
        assertFalse(
            shouldUseTabletVideoLayout(
                isExpandedScreen = true,
                smallestScreenWidthDp = 411
            )
        )
    }

    @Test
    fun autoRotatePolicy_appliesOnlyOnPhoneLayout() {
        assertTrue(
            shouldApplyPhoneAutoRotatePolicy(
                useTabletLayout = false
            )
        )
        assertFalse(
            shouldApplyPhoneAutoRotatePolicy(
                useTabletLayout = true
            )
        )
        assertFalse(
            shouldApplyPhoneAutoRotatePolicy(
                useTabletLayout = true
            )
        )
    }

    @Test
    fun portraitAndInteractionUi_policiesReflectCurrentBehavior() {
        assertTrue(shouldEnablePortraitExperience())
        assertFalse(shouldShowVideoDetailBottomInteractionBar())
        assertTrue(shouldShowVideoDetailActionButtons())
    }

    @Test
    fun interactionUi_isHiddenOnPhoneToo() {
        assertFalse(shouldShowVideoDetailBottomInteractionBar())
        assertTrue(shouldShowVideoDetailActionButtons())
    }

    @Test
    fun orientationDrivenFullscreen_isPhoneOnly() {
        assertTrue(
            shouldUseOrientationDrivenFullscreen(
                useTabletLayout = false
            )
        )
        assertFalse(
            shouldUseOrientationDrivenFullscreen(
                useTabletLayout = true
            )
        )
        assertFalse(
            shouldUseOrientationDrivenFullscreen(
                useTabletLayout = true
            )
        )
    }

    @Test
    fun sharedCoverTransition_requiresSwitchAndBothScopes() {
        assertTrue(
            shouldEnableVideoCoverSharedTransition(
                transitionEnabled = true,
                hasSharedTransitionScope = true,
                hasAnimatedVisibilityScope = true
            )
        )
        assertFalse(
            shouldEnableVideoCoverSharedTransition(
                transitionEnabled = false,
                hasSharedTransitionScope = true,
                hasAnimatedVisibilityScope = true
            )
        )
        assertFalse(
            shouldEnableVideoCoverSharedTransition(
                transitionEnabled = true,
                hasSharedTransitionScope = false,
                hasAnimatedVisibilityScope = true
            )
        )
        assertFalse(
            shouldEnableVideoCoverSharedTransition(
                transitionEnabled = true,
                hasSharedTransitionScope = true,
                hasAnimatedVisibilityScope = false
            )
        )
    }

    @Test
    fun highRefreshMode_prefersHighestRefreshThenResolution() {
        val selected = resolvePreferredHighRefreshModeId(
            currentModeId = 1,
            supportedModes = listOf(
                RefreshModeCandidate(modeId = 1, refreshRate = 60f, width = 2400, height = 1080),
                RefreshModeCandidate(modeId = 2, refreshRate = 120f, width = 1920, height = 1080),
                RefreshModeCandidate(modeId = 3, refreshRate = 120f, width = 2400, height = 1080)
            )
        )

        assertEquals(3, selected)
    }

    @Test
    fun highRefreshMode_returnsNullWhenNoEligibleHighRefresh() {
        val selected = resolvePreferredHighRefreshModeId(
            currentModeId = 1,
            supportedModes = listOf(
                RefreshModeCandidate(modeId = 1, refreshRate = 60f, width = 2400, height = 1080),
                RefreshModeCandidate(modeId = 2, refreshRate = 75f, width = 2400, height = 1080)
            )
        )

        assertEquals(null, selected)
    }

    @Test
    fun phoneOrientationPolicy_returnsNullOnTabletLayout() {
        assertEquals(
            null,
            resolvePhoneVideoRequestedOrientation(
                autoRotateEnabled = true,
                fullscreenMode = FullscreenMode.AUTO,
                useTabletLayout = true,
                isOrientationDrivenFullscreen = false,
                isFullscreenMode = false
            )
        )
    }

    @Test
    fun phoneOrientationPolicy_autoRotateEnabled_usesSensorToAllowAutoExitFullscreen() {
        assertEquals(
            ActivityInfo.SCREEN_ORIENTATION_SENSOR,
            resolvePhoneVideoRequestedOrientation(
                autoRotateEnabled = true,
                fullscreenMode = FullscreenMode.AUTO,
                useTabletLayout = false,
                isOrientationDrivenFullscreen = true,
                isFullscreenMode = false
            )
        )
        assertEquals(
            ActivityInfo.SCREEN_ORIENTATION_SENSOR,
            resolvePhoneVideoRequestedOrientation(
                autoRotateEnabled = true,
                fullscreenMode = FullscreenMode.AUTO,
                useTabletLayout = false,
                isOrientationDrivenFullscreen = true,
                isFullscreenMode = true
            )
        )
    }

    @Test
    fun phoneOrientationPolicy_autoRotateDisabled_switchesBetweenPortraitAndLandscapeLock() {
        assertEquals(
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT,
            resolvePhoneVideoRequestedOrientation(
                autoRotateEnabled = false,
                fullscreenMode = FullscreenMode.AUTO,
                useTabletLayout = false,
                isOrientationDrivenFullscreen = true,
                isFullscreenMode = false
            )
        )
        assertEquals(
            ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE,
            resolvePhoneVideoRequestedOrientation(
                autoRotateEnabled = false,
                fullscreenMode = FullscreenMode.AUTO,
                useTabletLayout = false,
                isOrientationDrivenFullscreen = true,
                isFullscreenMode = true
            )
        )
    }

    @Test
    fun phoneOrientationPolicy_manualFullscreenRequest_withAutoRotate_forcesLandscape() {
        assertEquals(
            ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE,
            resolvePhoneVideoRequestedOrientation(
                autoRotateEnabled = true,
                fullscreenMode = FullscreenMode.AUTO,
                useTabletLayout = false,
                isOrientationDrivenFullscreen = true,
                isFullscreenMode = false,
                manualFullscreenRequested = true
            )
        )
    }

    @Test
    fun phoneOrientationPolicy_autoRotateHorizontalMode_withoutManualRequest_usesSensor() {
        assertEquals(
            ActivityInfo.SCREEN_ORIENTATION_SENSOR,
            resolvePhoneVideoRequestedOrientation(
                autoRotateEnabled = true,
                fullscreenMode = FullscreenMode.HORIZONTAL,
                useTabletLayout = false,
                isOrientationDrivenFullscreen = true,
                isFullscreenMode = false,
                manualFullscreenRequested = false
            )
        )
    }

    @Test
    fun phoneOrientationPolicy_fullscreenModeNone_keepsCurrentOrientation() {
        assertEquals(
            null,
            resolvePhoneVideoRequestedOrientation(
                autoRotateEnabled = true,
                fullscreenMode = FullscreenMode.NONE,
                useTabletLayout = false,
                isOrientationDrivenFullscreen = false,
                isFullscreenMode = true
            )
        )
    }

    @Test
    fun phoneEnterOrientationPolicy_respectsFullscreenMode() {
        assertEquals(
            ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE,
            resolvePhoneFullscreenEnterOrientation(
                fullscreenMode = FullscreenMode.HORIZONTAL,
                isVerticalVideo = false
            )
        )
        assertEquals(
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT,
            resolvePhoneFullscreenEnterOrientation(
                fullscreenMode = FullscreenMode.VERTICAL,
                isVerticalVideo = true
            )
        )
        assertEquals(
            null,
            resolvePhoneFullscreenEnterOrientation(
                fullscreenMode = FullscreenMode.NONE,
                isVerticalVideo = false
            )
        )
    }

    @Test
    fun phoneEnterOrientationPolicy_autoMode_usesVideoDirection() {
        assertEquals(
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT,
            resolvePhoneFullscreenEnterOrientation(
                fullscreenMode = FullscreenMode.AUTO,
                isVerticalVideo = true
            )
        )
        assertEquals(
            ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE,
            resolvePhoneFullscreenEnterOrientation(
                fullscreenMode = FullscreenMode.AUTO,
                isVerticalVideo = false
            )
        )
    }

    @Test
    fun fullscreenTogglePolicy_entersPortraitFullscreen_whenTargetIsPortraitAndExperienceEnabled() {
        assertTrue(
            shouldEnterPortraitFullscreenOnFullscreenToggle(
                targetOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT,
                portraitExperienceEnabled = true
            )
        )
    }

    @Test
    fun fullscreenTogglePolicy_doesNotEnterPortraitFullscreen_whenExperienceDisabled() {
        assertFalse(
            shouldEnterPortraitFullscreenOnFullscreenToggle(
                targetOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT,
                portraitExperienceEnabled = false
            )
        )
    }

    @Test
    fun fullscreenTogglePolicy_doesNotEnterPortraitFullscreen_whenTargetIsLandscape() {
        assertFalse(
            shouldEnterPortraitFullscreenOnFullscreenToggle(
                targetOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE,
                portraitExperienceEnabled = true
            )
        )
    }
}
