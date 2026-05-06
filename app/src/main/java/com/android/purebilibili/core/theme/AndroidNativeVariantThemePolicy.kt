package com.android.purebilibili.core.theme

import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography

internal const val MD3_CORNER_RADIUS_SCALE = 0.9f
internal const val MD3_EXPRESSIVE_CORNER_RADIUS_SCALE = 1.05f
internal const val MIUIX_CORNER_RADIUS_SCALE = 1.15f

data class AndroidNativeChromeTokens(
    val containerCornerRadiusDp: Int,
    val pillCornerRadiusDp: Int,
    val selectedContainerAlpha: Float,
    val tonalSurfaceElevationDp: Int,
    val denseHorizontalSpacingDp: Int,
    val rowMinTouchTargetDp: Int,
    val expressiveMotionDurationMillis: Int,
    val motionScale: Float
)

fun isMaterial3ExpressiveVariant(
    uiPreset: UiPreset,
    androidNativeVariant: AndroidNativeVariant
): Boolean = uiPreset == UiPreset.MD3 &&
    androidNativeVariant == AndroidNativeVariant.MATERIAL3_EXPRESSIVE

fun resolveAndroidNativeChromeTokens(
    uiPreset: UiPreset,
    androidNativeVariant: AndroidNativeVariant = AndroidNativeVariant.MATERIAL3
): AndroidNativeChromeTokens {
    return when {
        isMaterial3ExpressiveVariant(uiPreset, androidNativeVariant) -> AndroidNativeChromeTokens(
            containerCornerRadiusDp = 30,
            pillCornerRadiusDp = 30,
            selectedContainerAlpha = 0.24f,
            tonalSurfaceElevationDp = 4,
            denseHorizontalSpacingDp = 20,
            rowMinTouchTargetDp = 48,
            expressiveMotionDurationMillis = 260,
            motionScale = 1.12f
        )
        uiPreset == UiPreset.MD3 && androidNativeVariant == AndroidNativeVariant.MIUIX -> AndroidNativeChromeTokens(
            containerCornerRadiusDp = 20,
            pillCornerRadiusDp = 22,
            selectedContainerAlpha = 0.18f,
            tonalSurfaceElevationDp = 0,
            denseHorizontalSpacingDp = 16,
            rowMinTouchTargetDp = 48,
            expressiveMotionDurationMillis = 180,
            motionScale = 1f
        )
        uiPreset == UiPreset.MD3 -> AndroidNativeChromeTokens(
            containerCornerRadiusDp = 24,
            pillCornerRadiusDp = 28,
            selectedContainerAlpha = 0.14f,
            tonalSurfaceElevationDp = 3,
            denseHorizontalSpacingDp = 18,
            rowMinTouchTargetDp = 48,
            expressiveMotionDurationMillis = 200,
            motionScale = 1f
        )
        else -> AndroidNativeChromeTokens(
            containerCornerRadiusDp = 20,
            pillCornerRadiusDp = 10,
            selectedContainerAlpha = 0.12f,
            tonalSurfaceElevationDp = 1,
            denseHorizontalSpacingDp = 16,
            rowMinTouchTargetDp = 44,
            expressiveMotionDurationMillis = 180,
            motionScale = 1f
        )
    }
}

fun resolveMaterialTypography(
    uiPreset: UiPreset,
    androidNativeVariant: AndroidNativeVariant
): Typography {
    return when {
        uiPreset == UiPreset.IOS -> BiliTypography
        androidNativeVariant == AndroidNativeVariant.MIUIX -> BiliMiuixTypography
        else -> BiliTypography
    }
}

fun resolveMaterialShapes(
    uiPreset: UiPreset,
    androidNativeVariant: AndroidNativeVariant
): Shapes {
    return when {
        uiPreset == UiPreset.IOS -> iOSShapes
        androidNativeVariant == AndroidNativeVariant.MATERIAL3_EXPRESSIVE -> Md3ExpressiveShapes
        androidNativeVariant == AndroidNativeVariant.MIUIX -> MiuixAlignedShapes
        else -> Md3Shapes
    }
}

fun resolveCornerRadiusScale(
    uiPreset: UiPreset,
    androidNativeVariant: AndroidNativeVariant
): Float {
    return when {
        uiPreset == UiPreset.IOS -> 1f
        androidNativeVariant == AndroidNativeVariant.MATERIAL3_EXPRESSIVE -> MD3_EXPRESSIVE_CORNER_RADIUS_SCALE
        androidNativeVariant == AndroidNativeVariant.MIUIX -> MIUIX_CORNER_RADIUS_SCALE
        else -> MD3_CORNER_RADIUS_SCALE
    }
}

fun shouldUseMiuixSmoothRounding(
    uiPreset: UiPreset,
    androidNativeVariant: AndroidNativeVariant
): Boolean = uiPreset == UiPreset.MD3 && androidNativeVariant == AndroidNativeVariant.MIUIX

fun shouldUseMaterialExpressiveMotionScheme(
    uiPreset: UiPreset,
    androidNativeVariant: AndroidNativeVariant
): Boolean = isMaterial3ExpressiveVariant(uiPreset, androidNativeVariant)
