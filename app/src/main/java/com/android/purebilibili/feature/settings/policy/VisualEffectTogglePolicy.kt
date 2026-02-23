package com.android.purebilibili.feature.settings

internal data class BottomBarVisualEffectState(
    val bottomBarBlurEnabled: Boolean,
    val liquidGlassEnabled: Boolean
)

internal fun resolveBottomBarBlurToggleState(
    enableBottomBarBlur: Boolean
): BottomBarVisualEffectState {
    // Keep visual style stable and avoid ending up in a fully opaque white bar:
    // - turning blur on disables liquid glass
    // - turning blur off restores liquid glass
    val nextLiquidGlassEnabled = if (enableBottomBarBlur) {
        false
    } else {
        true
    }
    return BottomBarVisualEffectState(
        bottomBarBlurEnabled = enableBottomBarBlur,
        liquidGlassEnabled = nextLiquidGlassEnabled
    )
}

internal fun resolveLiquidGlassToggleState(
    enableLiquidGlass: Boolean
): BottomBarVisualEffectState {
    return BottomBarVisualEffectState(
        bottomBarBlurEnabled = !enableLiquidGlass,
        liquidGlassEnabled = enableLiquidGlass
    )
}
