package com.android.purebilibili.feature.video.screen

import com.android.purebilibili.core.store.PortraitPlayerCollapseMode
import kotlin.math.max

internal data class PortraitInlinePlayerLayoutSpec(
    val widthDp: Float,
    val heightDp: Float
)

internal data class StandalonePortraitPagerMotionSpec(
    val enterDurationMillis: Int,
    val exitDurationMillis: Int,
    val exitScaleTarget: Float
)

internal enum class PortraitFullscreenButtonAction {
    ENTER_PORTRAIT_FULLSCREEN
}

internal fun shouldUseOfficialInlinePortraitDetailExperience(
    useTabletLayout: Boolean,
    isVerticalVideo: Boolean,
    portraitExperienceEnabled: Boolean
): Boolean {
    return portraitExperienceEnabled && !useTabletLayout && isVerticalVideo
}

internal fun shouldUseSharedPlayerForPortraitFullscreen(): Boolean {
    return true
}

internal fun shouldShowStandalonePortraitPager(
    portraitExperienceEnabled: Boolean,
    isPortraitFullscreen: Boolean,
    useOfficialInlinePortraitDetailExperience: Boolean,
    hasPlayableState: Boolean
): Boolean {
    return portraitExperienceEnabled &&
        isPortraitFullscreen &&
        hasPlayableState
}

internal fun shouldActivatePortraitFullscreenState(
    portraitExperienceEnabled: Boolean
): Boolean {
    return portraitExperienceEnabled
}

internal fun resolveStandalonePortraitPagerMotionSpec(): StandalonePortraitPagerMotionSpec {
    return StandalonePortraitPagerMotionSpec(
        enterDurationMillis = 220,
        exitDurationMillis = 180,
        exitScaleTarget = 0.98f
    )
}

internal fun shouldEnableInlinePortraitScrollTransform(
    collapseMode: PortraitPlayerCollapseMode,
    selectedTabIndex: Int
): Boolean {
    return when (selectedTabIndex) {
        0 -> collapseMode.enablesIntro
        1 -> collapseMode.enablesComment
        else -> collapseMode != PortraitPlayerCollapseMode.OFF
    }
}

internal fun shouldAnimateStandalonePortraitPager(useSharedPlayer: Boolean): Boolean {
    return !useSharedPlayer
}

internal fun resolvePortraitFullscreenButtonAction(
    useOfficialInlinePortraitDetailExperience: Boolean
): PortraitFullscreenButtonAction {
    return PortraitFullscreenButtonAction.ENTER_PORTRAIT_FULLSCREEN
}

internal fun shouldUseCompactInlinePortraitPlayerForCommentTab(
    useOfficialInlinePortraitDetailExperience: Boolean,
    selectedTabIndex: Int,
    isPortraitFullscreen: Boolean,
    isCommentThreadVisible: Boolean = false,
    firstVisibleItemIndex: Int = 0,
    firstVisibleItemScrollOffset: Int = 0,
    collapseMode: PortraitPlayerCollapseMode = PortraitPlayerCollapseMode.BOTH,
    commentScrollThresholdPx: Int = 56
): Boolean {
    if (!useOfficialInlinePortraitDetailExperience || isPortraitFullscreen) return false
    if (!collapseMode.enablesComment) return false
    if (isCommentThreadVisible) return true
    if (selectedTabIndex != 1) return false
    if (firstVisibleItemIndex > 0) return true
    return firstVisibleItemScrollOffset >= commentScrollThresholdPx
}

internal fun shouldUseCompactInlinePortraitPlayerForIntroScroll(
    useOfficialInlinePortraitDetailExperience: Boolean,
    selectedTabIndex: Int,
    isPortraitFullscreen: Boolean,
    firstVisibleItemIndex: Int,
    firstVisibleItemScrollOffset: Int,
    collapseMode: PortraitPlayerCollapseMode = PortraitPlayerCollapseMode.BOTH,
    introScrollThresholdPx: Int = 56
): Boolean {
    if (!useOfficialInlinePortraitDetailExperience || isPortraitFullscreen) return false
    if (!collapseMode.enablesIntro) return false
    if (selectedTabIndex != 0) return false
    if (firstVisibleItemIndex > 0) return true
    return firstVisibleItemScrollOffset >= introScrollThresholdPx
}

internal fun resolveInlinePortraitPlayerCollapseProgress(
    manualCollapseProgress: Float,
    compactForCommentTabProgress: Float
): Float {
    return manualCollapseProgress
        .coerceIn(0f, 1f)
        .coerceAtLeast(compactForCommentTabProgress.coerceIn(0f, 1f))
}

internal fun resolvePortraitInlinePlayerLayoutSpec(
    screenWidthDp: Float,
    screenHeightDp: Float,
    isCollapsed: Boolean
): PortraitInlinePlayerLayoutSpec {
    val width = screenWidthDp
    val collapsedHeight = screenWidthDp * 9f / 16f
    if (isCollapsed) {
        return PortraitInlinePlayerLayoutSpec(
            widthDp = width,
            heightDp = collapsedHeight
        )
    }

    val expandedHeight = max(screenHeightDp * 0.65f, screenWidthDp)
    return PortraitInlinePlayerLayoutSpec(
        widthDp = width,
        heightDp = expandedHeight
    )
}
