package com.android.purebilibili.feature.home

import com.android.purebilibili.core.theme.AndroidNativeVariant
import com.android.purebilibili.core.theme.UiPreset
import kotlin.math.min
import kotlin.math.max

internal fun resolvePullRefreshThresholdDp(): Float = 56f

enum class HomePullRefreshMotionStyle {
    IOS,
    MD3
}

enum class HomePullRefreshIndicatorStyle {
    IOS,
    MATERIAL_DEFAULT,
    MD3_SCREENSHOT_HANDLE
}

internal fun resolveHomePullRefreshMotionStyle(uiPreset: UiPreset): HomePullRefreshMotionStyle {
    return resolveHomePullRefreshMotionStyle(
        uiPreset = uiPreset,
        androidNativeVariant = AndroidNativeVariant.MATERIAL3
    )
}

internal fun resolveHomePullRefreshMotionStyle(
    uiPreset: UiPreset,
    androidNativeVariant: AndroidNativeVariant
): HomePullRefreshMotionStyle {
    return when {
        uiPreset == UiPreset.MD3 -> HomePullRefreshMotionStyle.MD3
        else -> HomePullRefreshMotionStyle.IOS
    }
}

internal fun resolveHomePullRefreshIndicatorStyle(
    uiPreset: UiPreset,
    androidNativeVariant: AndroidNativeVariant
): HomePullRefreshIndicatorStyle {
    return when {
        uiPreset == UiPreset.MD3 &&
            androidNativeVariant == AndroidNativeVariant.MATERIAL3 -> {
            HomePullRefreshIndicatorStyle.MD3_SCREENSHOT_HANDLE
        }
        uiPreset == UiPreset.MD3 &&
            androidNativeVariant == AndroidNativeVariant.MIUIX -> {
            HomePullRefreshIndicatorStyle.MATERIAL_DEFAULT
        }
        else -> HomePullRefreshIndicatorStyle.IOS
    }
}

internal fun resolveRequiredPullDistanceDp(
    thresholdDp: Float,
    dragMultiplier: Float
): Float {
    if (dragMultiplier <= 0f) return Float.POSITIVE_INFINITY
    return thresholdDp / dragMultiplier
}

internal fun shouldResetToTopOnRefreshStart(
    firstVisibleItemIndex: Int,
    firstVisibleItemScrollOffset: Int
): Boolean {
    return firstVisibleItemIndex > 0 || firstVisibleItemScrollOffset > 0
}

internal fun shouldResetToTopAfterIncrementalRefresh(
    currentCategory: HomeCategory,
    newItemsCount: Int?,
    isRefreshing: Boolean,
    firstVisibleItemIndex: Int,
    firstVisibleItemScrollOffset: Int
): Boolean {
    if (currentCategory != HomeCategory.RECOMMEND) return false
    if ((newItemsCount ?: 0) <= 0) return false
    if (isRefreshing) return false
    return shouldResetToTopOnRefreshStart(
        firstVisibleItemIndex = firstVisibleItemIndex,
        firstVisibleItemScrollOffset = firstVisibleItemScrollOffset
    )
}

internal fun shouldShowReleaseToRefreshHint(
    progress: Float,
    isRefreshing: Boolean,
    isStateAnimating: Boolean
): Boolean {
    if (isRefreshing) return false
    if (progress < 1f) return false
    return !isStateAnimating
}

internal fun resolvePullRefreshHintText(
    progress: Float,
    isRefreshing: Boolean,
    isStateAnimating: Boolean
): String {
    return when {
        isRefreshing -> "正在刷新..."
        shouldShowReleaseToRefreshHint(
            progress = progress,
            isRefreshing = isRefreshing,
            isStateAnimating = isStateAnimating
        ) -> "松手刷新"
        progress > 0f -> "下拉刷新..."
        else -> ""
    }
}

internal fun resolvePullIndicatorTranslationY(
    dragOffsetPx: Float,
    indicatorHeightPx: Float,
    minGapPx: Float,
    isRefreshing: Boolean
): Float {
    if (isRefreshing) return 0f
    if (dragOffsetPx <= 0f) return -indicatorHeightPx
    val centeredY = (dragOffsetPx / 2f) - (indicatorHeightPx / 2f)
    val maxAllowedY = dragOffsetPx - indicatorHeightPx - minGapPx
    return min(centeredY, maxAllowedY)
}

internal fun resolvePullContentMaxOffsetDp(
    indicatorStyle: HomePullRefreshIndicatorStyle
): Float {
    return when (indicatorStyle) {
        HomePullRefreshIndicatorStyle.MD3_SCREENSHOT_HANDLE -> 168f
        else -> 140f
    }
}

internal fun resolvePullContentOffsetFraction(
    distanceFraction: Float,
    isRefreshing: Boolean,
    motionStyle: HomePullRefreshMotionStyle = HomePullRefreshMotionStyle.IOS,
    indicatorStyle: HomePullRefreshIndicatorStyle = HomePullRefreshIndicatorStyle.IOS
): Float {
    if (indicatorStyle == HomePullRefreshIndicatorStyle.MD3_SCREENSHOT_HANDLE) {
        if (isRefreshing) return 0f
        val clampedDistance = distanceFraction.coerceIn(0f, 1.32f)
        return (clampedDistance * 0.82f).coerceAtMost(1.08f)
    }
    if (motionStyle == HomePullRefreshMotionStyle.MD3) return 0f
    val clampedDistance = distanceFraction.coerceAtMost(2f).coerceAtLeast(0f)
    return clampedDistance * 0.5f
}

internal fun resolveStablePullContentOffsetFraction(
    distanceFraction: Float,
    isRefreshing: Boolean,
    isStateAnimating: Boolean,
    previousOffsetFraction: Float,
    motionStyle: HomePullRefreshMotionStyle = HomePullRefreshMotionStyle.IOS,
    indicatorStyle: HomePullRefreshIndicatorStyle = HomePullRefreshIndicatorStyle.IOS
): Float {
    if (indicatorStyle == HomePullRefreshIndicatorStyle.MD3_SCREENSHOT_HANDLE) {
        if (isRefreshing) return 0f
        return resolvePullContentOffsetFraction(
            distanceFraction = distanceFraction,
            isRefreshing = isRefreshing,
            motionStyle = motionStyle,
            indicatorStyle = indicatorStyle
        )
    }
    if (motionStyle == HomePullRefreshMotionStyle.MD3) return 0f
    val currentOffset = resolvePullContentOffsetFraction(
        distanceFraction = distanceFraction,
        isRefreshing = isRefreshing,
        motionStyle = motionStyle,
        indicatorStyle = indicatorStyle
    )
    if (!isRefreshing && !isStateAnimating && distanceFraction <= 0f) return 0f
    return max(previousOffsetFraction, currentOffset)
}

internal fun resolveMd3ScreenshotRefreshIndicatorHeightDp(
    progress: Float,
    isRefreshing: Boolean
): Float {
    if (isRefreshing) return 42f
    val clampedProgress = progress.coerceIn(0f, 1.35f)
    return 44f + (clampedProgress * 42f)
}

internal fun resolveMd3ScreenshotRefreshIndicatorTranslationY(
    dragOffsetPx: Float,
    indicatorTotalHeightPx: Float,
    minGapPx: Float
): Float {
    if (dragOffsetPx <= 0f || indicatorTotalHeightPx <= 0f) return 0f
    val centeredInGap = (dragOffsetPx - indicatorTotalHeightPx) / 2f
    val maxTop = (dragOffsetPx - indicatorTotalHeightPx - minGapPx).coerceAtLeast(0f)
    return centeredInGap.coerceIn(0f, maxTop)
}
