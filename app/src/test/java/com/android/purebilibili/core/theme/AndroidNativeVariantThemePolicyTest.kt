package com.android.purebilibili.core.theme

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AndroidNativeVariantThemePolicyTest {

    @Test
    fun miuixVariant_usesMiuixAlignedTypography() {
        val typography = resolveMaterialTypography(
            uiPreset = UiPreset.MD3,
            androidNativeVariant = AndroidNativeVariant.MIUIX
        )

        assertEquals(BiliMiuixTypography.bodyMedium.fontSize, typography.bodyMedium.fontSize)
        assertEquals(BiliMiuixTypography.titleMedium.letterSpacing, typography.titleMedium.letterSpacing)
    }

    @Test
    fun material3Variant_keepsExistingTypography() {
        val typography = resolveMaterialTypography(
            uiPreset = UiPreset.MD3,
            androidNativeVariant = AndroidNativeVariant.MATERIAL3
        )

        assertEquals(BiliTypography.bodyMedium.fontSize, typography.bodyMedium.fontSize)
        assertEquals(BiliTypography.titleMedium.letterSpacing, typography.titleMedium.letterSpacing)
    }

    @Test
    fun material3ExpressiveVariant_keepsMaterialTypography() {
        val typography = resolveMaterialTypography(
            uiPreset = UiPreset.MD3,
            androidNativeVariant = AndroidNativeVariant.MATERIAL3_EXPRESSIVE
        )

        assertEquals(BiliTypography.bodyMedium.fontSize, typography.bodyMedium.fontSize)
        assertEquals(BiliTypography.titleMedium.letterSpacing, typography.titleMedium.letterSpacing)
    }

    @Test
    fun miuixVariant_enablesSmoothRoundingAndLargerCornerScale() {
        assertTrue(
            shouldUseMiuixSmoothRounding(
                uiPreset = UiPreset.MD3,
                androidNativeVariant = AndroidNativeVariant.MIUIX
            )
        )
        assertEquals(
            MIUIX_CORNER_RADIUS_SCALE,
            resolveCornerRadiusScale(
                uiPreset = UiPreset.MD3,
                androidNativeVariant = AndroidNativeVariant.MIUIX
            )
        )
    }

    @Test
    fun material3Variant_keepsCompactCornerScaleWithoutSmoothRounding() {
        assertFalse(
            shouldUseMiuixSmoothRounding(
                uiPreset = UiPreset.MD3,
                androidNativeVariant = AndroidNativeVariant.MATERIAL3
            )
        )
        assertEquals(
            MD3_CORNER_RADIUS_SCALE,
            resolveCornerRadiusScale(
                uiPreset = UiPreset.MD3,
                androidNativeVariant = AndroidNativeVariant.MATERIAL3
            )
        )
    }

    @Test
    fun material3ExpressiveVariant_usesExpressiveMotionAndLargerMaterialShape() {
        assertTrue(
            shouldUseMaterialExpressiveMotionScheme(
                uiPreset = UiPreset.MD3,
                androidNativeVariant = AndroidNativeVariant.MATERIAL3_EXPRESSIVE
            )
        )
        assertFalse(
            shouldUseMaterialExpressiveMotionScheme(
                uiPreset = UiPreset.MD3,
                androidNativeVariant = AndroidNativeVariant.MATERIAL3
            )
        )
        assertFalse(
            shouldUseMiuixSmoothRounding(
                uiPreset = UiPreset.MD3,
                androidNativeVariant = AndroidNativeVariant.MATERIAL3_EXPRESSIVE
            )
        )
        assertEquals(
            MD3_EXPRESSIVE_CORNER_RADIUS_SCALE,
            resolveCornerRadiusScale(
                uiPreset = UiPreset.MD3,
                androidNativeVariant = AndroidNativeVariant.MATERIAL3_EXPRESSIVE
            )
        )
        assertEquals(
            Md3ExpressiveShapes.extraLarge,
            resolveMaterialShapes(
                uiPreset = UiPreset.MD3,
                androidNativeVariant = AndroidNativeVariant.MATERIAL3_EXPRESSIVE
            ).extraLarge
        )
    }

    @Test
    fun material3ExpressiveVariant_exposesDistinctChromeTokens() {
        assertTrue(
            isMaterial3ExpressiveVariant(
                uiPreset = UiPreset.MD3,
                androidNativeVariant = AndroidNativeVariant.MATERIAL3_EXPRESSIVE
            )
        )
        assertFalse(
            isMaterial3ExpressiveVariant(
                uiPreset = UiPreset.MD3,
                androidNativeVariant = AndroidNativeVariant.MATERIAL3
            )
        )

        val material = resolveAndroidNativeChromeTokens(
            uiPreset = UiPreset.MD3,
            androidNativeVariant = AndroidNativeVariant.MATERIAL3
        )
        val expressive = resolveAndroidNativeChromeTokens(
            uiPreset = UiPreset.MD3,
            androidNativeVariant = AndroidNativeVariant.MATERIAL3_EXPRESSIVE
        )
        val miuix = resolveAndroidNativeChromeTokens(
            uiPreset = UiPreset.MD3,
            androidNativeVariant = AndroidNativeVariant.MIUIX
        )

        assertEquals(24, material.containerCornerRadiusDp)
        assertEquals(30, expressive.containerCornerRadiusDp)
        assertEquals(20, miuix.containerCornerRadiusDp)
        assertTrue(expressive.pillCornerRadiusDp > material.pillCornerRadiusDp)
        assertTrue(expressive.selectedContainerAlpha > material.selectedContainerAlpha)
        assertTrue(expressive.motionScale > material.motionScale)
        assertEquals(48, expressive.rowMinTouchTargetDp)
    }
}
