package com.android.purebilibili.feature.home

import kotlin.math.abs

internal fun resolveNextHomeGlobalScrollOffset(
    currentOffset: Float,
    scrollDeltaY: Float,
    liquidGlassEnabled: Boolean,
    minUpdateDeltaPx: Float = 0.5f
): Float? {
    if (!liquidGlassEnabled) return null
    if (abs(scrollDeltaY) < minUpdateDeltaPx) return null
    return currentOffset - scrollDeltaY
}
