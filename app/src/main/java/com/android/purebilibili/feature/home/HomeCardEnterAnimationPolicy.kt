package com.android.purebilibili.feature.home

internal fun resolveHomeCardEnterAnimationEnabledAtMount(
    baseAnimationEnabled: Boolean,
    isReturningFromDetail: Boolean,
    isSwitchingCategory: Boolean
): Boolean {
    if (!baseAnimationEnabled) return false
    if (isReturningFromDetail) return false
    if (isSwitchingCategory) return false
    return true
}
