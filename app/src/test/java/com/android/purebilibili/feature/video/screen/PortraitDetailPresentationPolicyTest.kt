package com.android.purebilibili.feature.video.screen

import com.android.purebilibili.core.store.PortraitPlayerCollapseMode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PortraitDetailPresentationPolicyTest {

    @Test
    fun portraitFullscreenPlayback_reusesSharedPlayerForSeamlessSurfaceHandoff() {
        assertTrue(shouldUseSharedPlayerForPortraitFullscreen())
    }

    @Test
    fun officialInlinePortraitMode_enabledForPhoneVerticalVideo() {
        assertTrue(
            shouldUseOfficialInlinePortraitDetailExperience(
                useTabletLayout = false,
                isVerticalVideo = true,
                portraitExperienceEnabled = true
            )
        )
    }

    @Test
    fun officialInlinePortraitMode_disabledForTabletLayout() {
        assertFalse(
            shouldUseOfficialInlinePortraitDetailExperience(
                useTabletLayout = true,
                isVerticalVideo = true,
                portraitExperienceEnabled = true
            )
        )
    }

    @Test
    fun standalonePortraitPager_showsWhenPortraitFullscreenRequestedEvenInInlineMode() {
        assertTrue(
            shouldShowStandalonePortraitPager(
                portraitExperienceEnabled = true,
                isPortraitFullscreen = true,
                useOfficialInlinePortraitDetailExperience = true,
                hasPlayableState = true
            )
        )
    }

    @Test
    fun portraitFullscreenRequest_isAllowedWhenPortraitExperienceEnabled() {
        assertTrue(
            shouldActivatePortraitFullscreenState(
                portraitExperienceEnabled = true
            )
        )
        assertFalse(
            shouldActivatePortraitFullscreenState(
                portraitExperienceEnabled = false
            )
        )
    }

    @Test
    fun inlinePortraitPlayerLayout_usesFullWidthExpandedHeader() {
        val spec = resolvePortraitInlinePlayerLayoutSpec(
            screenWidthDp = 412f,
            screenHeightDp = 915f,
            isCollapsed = false
        )

        assertEquals(412f, spec.widthDp)
        assertTrue(spec.heightDp > spec.widthDp)
        assertEquals(594.75f, spec.heightDp)
    }

    @Test
    fun inlinePortraitPlayerLayout_collapsesToFullWidth16By9Header() {
        val expanded = resolvePortraitInlinePlayerLayoutSpec(
            screenWidthDp = 412f,
            screenHeightDp = 915f,
            isCollapsed = false
        )
        val collapsed = resolvePortraitInlinePlayerLayoutSpec(
            screenWidthDp = 412f,
            screenHeightDp = 915f,
            isCollapsed = true
        )

        assertEquals(412f, collapsed.widthDp)
        assertTrue(collapsed.heightDp < expanded.heightDp)
        assertEquals(231.75f, collapsed.heightDp)
    }

    @Test
    fun inlinePortraitScrollTransform_respectsSettingEvenForOfficialMode() {
        assertFalse(
            shouldEnableInlinePortraitScrollTransform(
                collapseMode = PortraitPlayerCollapseMode.OFF,
                selectedTabIndex = 0
            )
        )
    }

    @Test
    fun portraitButton_entersPortraitFullscreenInOfficialInlineMode() {
        assertEquals(
            PortraitFullscreenButtonAction.ENTER_PORTRAIT_FULLSCREEN,
            resolvePortraitFullscreenButtonAction(
                useOfficialInlinePortraitDetailExperience = true
            )
        )
    }

    @Test
    fun portraitButton_entersPortraitFullscreenInRegularModeToo() {
        assertEquals(
            PortraitFullscreenButtonAction.ENTER_PORTRAIT_FULLSCREEN,
            resolvePortraitFullscreenButtonAction(
                useOfficialInlinePortraitDetailExperience = false
            )
        )
    }

    @Test
    fun inlinePortraitPlayer_keepsExpandedAtCommentTopThenCompactsWhenCommentScrollsDown() {
        assertFalse(
            shouldUseCompactInlinePortraitPlayerForCommentTab(
                useOfficialInlinePortraitDetailExperience = true,
                selectedTabIndex = 1,
                isPortraitFullscreen = false,
                firstVisibleItemIndex = 0,
                firstVisibleItemScrollOffset = 0,
                collapseMode = PortraitPlayerCollapseMode.BOTH
            )
        )
        assertTrue(
            shouldUseCompactInlinePortraitPlayerForCommentTab(
                useOfficialInlinePortraitDetailExperience = true,
                selectedTabIndex = 1,
                isPortraitFullscreen = false,
                firstVisibleItemIndex = 0,
                firstVisibleItemScrollOffset = 80,
                collapseMode = PortraitPlayerCollapseMode.BOTH
            )
        )
        assertFalse(
            shouldUseCompactInlinePortraitPlayerForCommentTab(
                useOfficialInlinePortraitDetailExperience = true,
                selectedTabIndex = 0,
                isPortraitFullscreen = false,
                firstVisibleItemIndex = 1,
                firstVisibleItemScrollOffset = 0,
                collapseMode = PortraitPlayerCollapseMode.BOTH
            )
        )
        assertFalse(
            shouldUseCompactInlinePortraitPlayerForCommentTab(
                useOfficialInlinePortraitDetailExperience = true,
                selectedTabIndex = 1,
                isPortraitFullscreen = true,
                firstVisibleItemIndex = 1,
                firstVisibleItemScrollOffset = 0,
                collapseMode = PortraitPlayerCollapseMode.BOTH
            )
        )
    }

    @Test
    fun inlinePortraitPlayer_compactsWhenCommentThreadDetailIsVisible() {
        assertTrue(
            shouldUseCompactInlinePortraitPlayerForCommentTab(
                useOfficialInlinePortraitDetailExperience = true,
                selectedTabIndex = 0,
                isPortraitFullscreen = false,
                isCommentThreadVisible = true,
                collapseMode = PortraitPlayerCollapseMode.BOTH
            )
        )
        assertFalse(
            shouldUseCompactInlinePortraitPlayerForCommentTab(
                useOfficialInlinePortraitDetailExperience = false,
                selectedTabIndex = 0,
                isPortraitFullscreen = false,
                isCommentThreadVisible = true,
                collapseMode = PortraitPlayerCollapseMode.BOTH
            )
        )
    }

    @Test
    fun inlinePortraitPlayer_compactsWhenIntroHasScrolledDown() {
        assertTrue(
            shouldUseCompactInlinePortraitPlayerForIntroScroll(
                useOfficialInlinePortraitDetailExperience = true,
                selectedTabIndex = 0,
                isPortraitFullscreen = false,
                firstVisibleItemIndex = 0,
                firstVisibleItemScrollOffset = 80,
                collapseMode = PortraitPlayerCollapseMode.BOTH
            )
        )
        assertFalse(
            shouldUseCompactInlinePortraitPlayerForIntroScroll(
                useOfficialInlinePortraitDetailExperience = true,
                selectedTabIndex = 0,
                isPortraitFullscreen = false,
                firstVisibleItemIndex = 0,
                firstVisibleItemScrollOffset = 20,
                collapseMode = PortraitPlayerCollapseMode.BOTH
            )
        )
    }

    @Test
    fun inlinePortraitPlayerCollapseMode_followsPortraitOrientationStrategy() {
        assertTrue(
            shouldUseCompactInlinePortraitPlayerForIntroScroll(
                useOfficialInlinePortraitDetailExperience = true,
                selectedTabIndex = 0,
                isPortraitFullscreen = false,
                firstVisibleItemIndex = 1,
                firstVisibleItemScrollOffset = 0,
                collapseMode = PortraitPlayerCollapseMode.INTRO_ONLY,
                isVerticalVideo = true
            )
        )
        assertTrue(
            shouldUseCompactInlinePortraitPlayerForCommentTab(
                useOfficialInlinePortraitDetailExperience = true,
                selectedTabIndex = 1,
                isPortraitFullscreen = false,
                firstVisibleItemIndex = 1,
                firstVisibleItemScrollOffset = 0,
                collapseMode = PortraitPlayerCollapseMode.INTRO_ONLY,
                isVerticalVideo = true
            )
        )
        assertFalse(
            shouldUseCompactInlinePortraitPlayerForIntroScroll(
                useOfficialInlinePortraitDetailExperience = true,
                selectedTabIndex = 0,
                isPortraitFullscreen = false,
                firstVisibleItemIndex = 1,
                firstVisibleItemScrollOffset = 0,
                collapseMode = PortraitPlayerCollapseMode.INTRO_ONLY,
                isVerticalVideo = false
            )
        )
        assertFalse(
            shouldUseCompactInlinePortraitPlayerForCommentTab(
                useOfficialInlinePortraitDetailExperience = true,
                selectedTabIndex = 1,
                isPortraitFullscreen = false,
                firstVisibleItemIndex = 1,
                firstVisibleItemScrollOffset = 0,
                collapseMode = PortraitPlayerCollapseMode.INTRO_ONLY,
                isVerticalVideo = false
            )
        )
        assertTrue(
            shouldUseCompactInlinePortraitPlayerForIntroScroll(
                useOfficialInlinePortraitDetailExperience = true,
                selectedTabIndex = 0,
                isPortraitFullscreen = false,
                firstVisibleItemIndex = 1,
                firstVisibleItemScrollOffset = 0,
                collapseMode = PortraitPlayerCollapseMode.COMMENT_ONLY,
                isVerticalVideo = false
            )
        )
        assertTrue(
            shouldUseCompactInlinePortraitPlayerForCommentTab(
                useOfficialInlinePortraitDetailExperience = true,
                selectedTabIndex = 1,
                isPortraitFullscreen = false,
                firstVisibleItemIndex = 1,
                firstVisibleItemScrollOffset = 0,
                collapseMode = PortraitPlayerCollapseMode.COMMENT_ONLY,
                isVerticalVideo = false
            )
        )
    }

    @Test
    fun inlinePortraitPlayer_commentTabUsesCollapsedVisualProgressWithoutChangingManualState() {
        assertEquals(
            1f,
            resolveInlinePortraitPlayerCollapseProgress(
                manualCollapseProgress = 0f,
                compactForCommentTabProgress = 1f
            )
        )
        assertEquals(
            0.4f,
            resolveInlinePortraitPlayerCollapseProgress(
                manualCollapseProgress = 0.4f,
                compactForCommentTabProgress = 0f
            )
        )
        assertEquals(
            0.6f,
            resolveInlinePortraitPlayerCollapseProgress(
                manualCollapseProgress = 0.2f,
                compactForCommentTabProgress = 0.6f
            )
        )
    }

    @Test
    fun standalonePortraitPagerMotionSpec_keepsExitTransitionShortAndTight() {
        val spec = resolveStandalonePortraitPagerMotionSpec()

        assertEquals(220, spec.enterDurationMillis)
        assertEquals(180, spec.exitDurationMillis)
        assertEquals(0.98f, spec.exitScaleTarget)
    }

    @Test
    fun sharedPlayerPortraitExit_disablesPagerAnimationToAvoidSurfaceFlicker() {
        assertFalse(shouldAnimateStandalonePortraitPager(useSharedPlayer = true))
        assertTrue(shouldAnimateStandalonePortraitPager(useSharedPlayer = false))
    }
}
