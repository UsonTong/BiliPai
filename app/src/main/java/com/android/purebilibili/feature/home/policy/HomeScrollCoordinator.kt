package com.android.purebilibili.feature.home.policy

import com.android.purebilibili.feature.home.resolveNextHomeGlobalScrollOffset

internal enum class BottomBarVisibilityIntent {
    SHOW,
    HIDE
}

internal data class HomeScrollUpdate(
    val headerOffsetPx: Float,
    val bottomBarVisibilityIntent: BottomBarVisibilityIntent?,
    val globalScrollOffset: Float?
)

internal fun reduceHomePreScroll(
    currentHeaderOffsetPx: Float,
    deltaY: Float,
    minHeaderOffsetPx: Float,
    isHeaderCollapseEnabled: Boolean,
    isBottomBarAutoHideEnabled: Boolean,
    useSideNavigation: Boolean,
    liquidGlassEnabled: Boolean,
    currentGlobalScrollOffset: Float,
    bottomBarVisibilityThresholdPx: Float = 10f
): HomeScrollUpdate {
    val nextHeaderOffset = if (isHeaderCollapseEnabled) {
        (currentHeaderOffsetPx + deltaY).coerceIn(minHeaderOffsetPx, 0f)
    } else {
        0f
    }

    val nextBottomBarIntent = when {
        !isBottomBarAutoHideEnabled || useSideNavigation -> null
        deltaY <= -bottomBarVisibilityThresholdPx -> BottomBarVisibilityIntent.HIDE
        deltaY >= bottomBarVisibilityThresholdPx -> BottomBarVisibilityIntent.SHOW
        else -> null
    }

    return HomeScrollUpdate(
        headerOffsetPx = nextHeaderOffset,
        bottomBarVisibilityIntent = nextBottomBarIntent,
        globalScrollOffset = resolveNextHomeGlobalScrollOffset(
            currentOffset = currentGlobalScrollOffset,
            scrollDeltaY = deltaY,
            liquidGlassEnabled = liquidGlassEnabled
        )
    )
}
