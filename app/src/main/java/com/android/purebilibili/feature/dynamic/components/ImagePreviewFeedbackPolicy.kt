package com.android.purebilibili.feature.dynamic.components

import com.android.purebilibili.core.util.HapticType

internal fun resolveImagePreviewSaveFeedback(success: Boolean): HapticType {
    return if (success) HapticType.LIGHT else HapticType.HEAVY
}
