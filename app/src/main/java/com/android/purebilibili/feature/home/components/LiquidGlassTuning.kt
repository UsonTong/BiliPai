package com.android.purebilibili.feature.home.components

import com.android.purebilibili.core.store.LiquidGlassMode
import com.android.purebilibili.core.store.LiquidGlassStyle
import com.android.purebilibili.core.store.normalizeLiquidGlassStrength
import com.android.purebilibili.core.store.resolveDefaultLiquidGlassStrength
import com.android.purebilibili.core.store.resolveLegacyLiquidGlassMode

data class LiquidGlassTuning(
    val mode: LiquidGlassMode,
    val strength: Float,
    val backdropBlurRadius: Float,
    val surfaceAlpha: Float,
    val whiteOverlayAlpha: Float,
    val refractIntensity: Float,
    val refractionAmount: Float,
    val refractionHeight: Float,
    val indicatorTintAlpha: Float,
    val indicatorLensBoost: Float,
    val indicatorEdgeWarpBoost: Float,
    val indicatorChromaticBoost: Float,
    val chromaticAberrationEnabled: Boolean,
    val scrollCoupledRefraction: Boolean,
    val useNeutralIndicatorTint: Boolean
)

internal fun resolveLiquidGlassTuning(
    mode: LiquidGlassMode,
    strength: Float
): LiquidGlassTuning {
    val normalizedStrength = normalizeLiquidGlassStrength(strength)
    return when (mode) {
        LiquidGlassMode.CLEAR -> LiquidGlassTuning(
            mode = mode,
            strength = normalizedStrength,
            backdropBlurRadius = lerp(14f, 20f, normalizedStrength),
            surfaceAlpha = lerp(0.18f, 0.28f, normalizedStrength),
            whiteOverlayAlpha = lerp(0.03f, 0.06f, normalizedStrength),
            refractIntensity = lerp(0.18f, 0.32f, normalizedStrength),
            refractionAmount = lerp(28f, 42f, normalizedStrength),
            refractionHeight = lerp(124f, 150f, normalizedStrength),
            indicatorTintAlpha = lerp(0.20f, 0.30f, normalizedStrength),
            indicatorLensBoost = lerp(1.18f, 1.42f, normalizedStrength),
            indicatorEdgeWarpBoost = lerp(1.16f, 1.38f, normalizedStrength),
            indicatorChromaticBoost = lerp(0.88f, 1.04f, normalizedStrength),
            chromaticAberrationEnabled = false,
            scrollCoupledRefraction = false,
            useNeutralIndicatorTint = true
        )
        LiquidGlassMode.BALANCED -> LiquidGlassTuning(
            mode = mode,
            strength = normalizedStrength,
            backdropBlurRadius = lerp(18f, 26f, normalizedStrength),
            surfaceAlpha = lerp(0.22f, 0.34f, normalizedStrength),
            whiteOverlayAlpha = lerp(0.04f, 0.08f, normalizedStrength),
            refractIntensity = lerp(0.34f, 0.52f, normalizedStrength),
            refractionAmount = lerp(40f, 60f, normalizedStrength),
            refractionHeight = lerp(142f, 178f, normalizedStrength),
            indicatorTintAlpha = lerp(0.12f, 0.18f, normalizedStrength),
            indicatorLensBoost = lerp(1.42f, 1.72f, normalizedStrength),
            indicatorEdgeWarpBoost = lerp(1.40f, 1.78f, normalizedStrength),
            indicatorChromaticBoost = lerp(1.08f, 1.42f, normalizedStrength),
            chromaticAberrationEnabled = true,
            scrollCoupledRefraction = true,
            useNeutralIndicatorTint = false
        )
        LiquidGlassMode.FROSTED -> LiquidGlassTuning(
            mode = mode,
            strength = normalizedStrength,
            backdropBlurRadius = lerp(24f, 34f, normalizedStrength),
            surfaceAlpha = lerp(0.30f, 0.42f, normalizedStrength),
            whiteOverlayAlpha = lerp(0.07f, 0.12f, normalizedStrength),
            refractIntensity = lerp(0.12f, 0.20f, normalizedStrength),
            refractionAmount = lerp(14f, 24f, normalizedStrength),
            refractionHeight = lerp(96f, 128f, normalizedStrength),
            indicatorTintAlpha = lerp(0.14f, 0.22f, normalizedStrength),
            indicatorLensBoost = lerp(1.00f, 1.16f, normalizedStrength),
            indicatorEdgeWarpBoost = lerp(0.98f, 1.14f, normalizedStrength),
            indicatorChromaticBoost = 0.82f,
            chromaticAberrationEnabled = false,
            scrollCoupledRefraction = false,
            useNeutralIndicatorTint = false
        )
    }
}

internal fun resolveLiquidGlassTuning(style: LiquidGlassStyle): LiquidGlassTuning {
    val mode = resolveLegacyLiquidGlassMode(style)
    return resolveLiquidGlassTuning(
        mode = mode,
        strength = resolveDefaultLiquidGlassStrength(mode)
    )
}

private fun lerp(start: Float, stop: Float, fraction: Float): Float {
    return start + (stop - start) * fraction
}
