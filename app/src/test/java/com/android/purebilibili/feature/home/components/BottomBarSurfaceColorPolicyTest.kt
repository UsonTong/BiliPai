package com.android.purebilibili.feature.home.components

import androidx.compose.ui.graphics.Color
import com.android.purebilibili.core.theme.UiPreset
import com.android.purebilibili.core.ui.blur.BlurIntensity
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BottomBarSurfaceColorPolicyTest {

    @Test
    fun `blur enabled follows blur style alpha`() {
        val color = resolveBottomBarSurfaceColor(
            surfaceColor = Color.White,
            blurEnabled = true,
            blurIntensity = BlurIntensity.THIN
        )

        assertEquals(0.4f, color.alpha, 0.001f)
    }

    @Test
    fun `blur disabled keeps light theme surface color`() {
        val color = resolveBottomBarSurfaceColor(
            surfaceColor = Color.White,
            blurEnabled = false,
            blurIntensity = BlurIntensity.THIN
        )

        assertEquals(Color.White, color)
    }

    @Test
    fun `blur disabled keeps dark theme surface color`() {
        val color = resolveBottomBarSurfaceColor(
            surfaceColor = Color(0xFF121212),
            blurEnabled = false,
            blurIntensity = BlurIntensity.THIN
        )

        assertEquals(Color(0xFF121212), color)
    }

    @Test
    fun `android native floating shell is opaque when blur is disabled`() {
        val tuning = resolveAndroidNativeBottomBarTuning(
            blurEnabled = false,
            darkTheme = false
        )

        assertEquals(1f, tuning.shellSurfaceAlpha, 0.001f)
    }

    @Test
    fun `android native floating shell is translucent when ordinary blur is enabled`() {
        val tuning = resolveAndroidNativeBottomBarTuning(
            blurEnabled = true,
            darkTheme = false
        )
        val color = resolveAndroidNativeFloatingBottomBarContainerColor(
            surfaceColor = Color.White,
            tuning = tuning,
            glassEnabled = false,
            blurEnabled = true,
            blurIntensity = BlurIntensity.THIN
        )

        assertEquals(0.4f, tuning.shellSurfaceAlpha, 0.001f)
        assertEquals(0.4f, color.alpha, 0.001f)
    }

    @Test
    fun `android native floating shell follows ordinary blur intensity when glass is off`() {
        val tuning = resolveAndroidNativeBottomBarTuning(
            blurEnabled = true,
            darkTheme = false
        )
        val color = resolveAndroidNativeFloatingBottomBarContainerColor(
            surfaceColor = Color.White,
            tuning = tuning,
            glassEnabled = false,
            blurEnabled = true,
            blurIntensity = BlurIntensity.THICK
        )

        assertEquals(0.6f, color.alpha, 0.001f)
    }

    @Test
    fun `android native glass stays enabled when liquid glass is on even if blur toggle is off`() {
        assertTrue(
            resolveAndroidNativeBottomBarGlassEnabled(
                liquidGlassEnabled = true,
                blurEnabled = false
            )
        )
        assertFalse(
            resolveAndroidNativeBottomBarGlassEnabled(
                liquidGlassEnabled = false,
                blurEnabled = false
            )
        )
    }

    @Test
    fun `android native liquid glass stays enabled when blur toggle is also on`() {
        assertFalse(
            resolveAndroidNativeBottomBarGlassEnabled(
                liquidGlassEnabled = false,
                blurEnabled = true
            )
        )
        assertTrue(
            resolveAndroidNativeBottomBarGlassEnabled(
                liquidGlassEnabled = true,
                blurEnabled = true
            )
        )
    }

    @Test
    fun `android native floating blur uses haze when available`() {
        assertTrue(
            shouldUseAndroidNativeFloatingHazeBlur(
                blurEnabled = true,
                glassEnabled = false,
                hasHazeState = true
            )
        )
        assertFalse(
            shouldUseAndroidNativeFloatingHazeBlur(
                blurEnabled = true,
                glassEnabled = true,
                hasHazeState = true
            )
        )
    }

    @Test
    fun `android native indicator keeps capsule static when idle`() {
        val spec = resolveAndroidNativeIndicatorSpec(isMoving = false)

        assertFalse(spec.usesLens)
        assertFalse(spec.captureTintedContentLayer)
    }

    @Test
    fun `android native indicator enables lens and tinted export while moving`() {
        val spec = resolveAndroidNativeIndicatorSpec(isMoving = true)

        assertTrue(spec.usesLens)
        assertTrue(spec.captureTintedContentLayer)
    }

    @Test
    fun `android native indicator color softens primary tint in light theme`() {
        val color = resolveAndroidNativeIndicatorColor(
            themeColor = Color(0xFF4F7CFF),
            darkTheme = false
        )

        assertTrue(color.alpha > 0.7f)
        assertTrue(color.red > 0.7f)
        assertTrue(color.green > 0.8f)
    }

    @Test
    fun `settled segmented indicator uses bottom bar surface tint`() {
        val color = resolveIosFloatingBottomIndicatorColor(
            isDarkTheme = true,
            visualPolicy = BottomBarIndicatorVisualPolicy(
                isInMotion = false,
                shouldRefract = false,
                useNeutralTint = false
            ),
            liquidGlassTuning = resolveLiquidGlassTuning(progress = 0.5f)
        )

        assertEquals(resolveBottomBarMovingIndicatorSurfaceColor(isDarkTheme = true).red, color.red, 0.001f)
        assertEquals(resolveBottomBarMovingIndicatorSurfaceColor(isDarkTheme = true).green, color.green, 0.001f)
        assertEquals(resolveBottomBarMovingIndicatorSurfaceColor(isDarkTheme = true).blue, color.blue, 0.001f)
        assertTrue(color.alpha < 1f)
    }

    @Test
    fun `light segmented indicator uses gray white bottom bar surface tint`() {
        val color = resolveIosFloatingBottomIndicatorColor(
            isDarkTheme = false,
            visualPolicy = BottomBarIndicatorVisualPolicy(
                isInMotion = false,
                shouldRefract = false,
                useNeutralTint = false
            ),
            liquidGlassTuning = resolveLiquidGlassTuning(progress = 0.5f)
        )

        assertEquals(resolveBottomBarMovingIndicatorSurfaceColor(isDarkTheme = false).red, color.red, 0.001f)
        assertEquals(resolveBottomBarMovingIndicatorSurfaceColor(isDarkTheme = false).green, color.green, 0.001f)
        assertEquals(resolveBottomBarMovingIndicatorSurfaceColor(isDarkTheme = false).blue, color.blue, 0.001f)
    }

    @Test
    fun `moving segmented indicator uses bottom bar moving alpha floor`() {
        val visualPolicy = BottomBarIndicatorVisualPolicy(
            isInMotion = true,
            shouldRefract = true,
            useNeutralTint = true
        )
        val alpha = resolveIosFloatingBottomIndicatorTintAlpha(
            visualPolicy = visualPolicy,
            isDarkTheme = false,
            liquidGlassProgress = 0.5f,
            configuredAlpha = 0.12f
        )

        assertTrue(alpha >= 0.4f)
    }

    @Test
    fun `md3 segmented control falls back to android underline when global liquid glass is disabled`() {
        assertEquals(
            SegmentedControlChromeStyle.ANDROID_NATIVE_UNDERLINE,
            resolveSegmentedControlChromeStyle(
                uiPreset = UiPreset.MD3,
                androidNativeLiquidGlassEnabled = false
            )
        )

        assertEquals(
            SegmentedControlChromeStyle.LIQUID_PILL,
            resolveSegmentedControlChromeStyle(
                uiPreset = UiPreset.MD3,
                androidNativeLiquidGlassEnabled = true
            )
        )

        assertEquals(
            SegmentedControlChromeStyle.LIQUID_PILL,
            resolveSegmentedControlChromeStyle(
                uiPreset = UiPreset.IOS,
                androidNativeLiquidGlassEnabled = false
            )
        )
    }

    @Test
    fun `android native export layer tint keeps theme hue in dark theme`() {
        val color = resolveAndroidNativeExportTintColor(
            themeColor = Color(0xFF4F7CFF),
            darkTheme = true
        )

        assertTrue(color.alpha > 0.2f)
        assertTrue(color.blue >= color.red)
    }

    @Test
    fun `android native export layer tint preserves theme brightness in light theme`() {
        val themeColor = Color(0xFFE67A73)
        val color = resolveAndroidNativeExportTintColor(
            themeColor = themeColor,
            darkTheme = false
        )

        assertEquals(themeColor.red, color.red, 0.001f)
        assertEquals(themeColor.green, color.green, 0.001f)
        assertEquals(themeColor.blue, color.blue, 0.001f)
        assertEquals(themeColor.alpha, color.alpha, 0.001f)
    }

    @Test
    fun `android native glass export tint preserves theme hue`() {
        val themeColor = Color(0xFF4F7CFF)
        val containerColor = Color(0xFFE8ECEF)
        val color = resolveAndroidNativeExportTintColor(
            themeColor = themeColor,
            darkTheme = false,
            containerColor = containerColor,
            glassEnabled = true
        )

        assertEquals(themeColor.red, color.red, 0.001f)
        assertEquals(themeColor.green, color.green, 0.001f)
        assertEquals(themeColor.blue, color.blue, 0.001f)
        assertEquals(themeColor.alpha, color.alpha, 0.001f)
    }

    @Test
    fun `android native glass visible selected item stays themed while indicator is idle`() {
        val unselected = Color(0xFF202124)
        val selected = Color(0xFF00A1D6)
        val color = resolveBottomBarGlassVisibleContentColor(
            unselectedColor = unselected,
            selectedColor = selected,
            themeWeight = 1f,
            glassEnabled = true,
            indicatorProgress = 0f
        )

        assertEquals(selected.red, color.red, 0.001f)
        assertEquals(selected.green, color.green, 0.001f)
        assertEquals(selected.blue, color.blue, 0.001f)
    }

    @Test
    fun `android native glass visible layer turns neutral only while indicator refracts`() {
        val unselected = Color(0xFF202124)
        val selected = Color(0xFF00A1D6)
        val color = resolveBottomBarGlassVisibleContentColor(
            unselectedColor = unselected,
            selectedColor = selected,
            themeWeight = 1f,
            glassEnabled = true,
            indicatorProgress = 1f
        )

        assertEquals(unselected.red, color.red, 0.001f)
        assertEquals(unselected.green, color.green, 0.001f)
        assertEquals(unselected.blue, color.blue, 0.001f)
    }

    @Test
    fun `android native glass visible selected item stays themed when indicator backdrop is disabled`() {
        val unselected = Color(0xFF202124)
        val selected = Color(0xFF00A1D6)
        val color = resolveBottomBarGlassVisibleContentColor(
            unselectedColor = unselected,
            selectedColor = selected,
            themeWeight = 1f,
            glassEnabled = true,
            indicatorProgress = 1f,
            indicatorBackdropEnabled = false
        )

        assertEquals(selected.red, color.red, 0.001f)
        assertEquals(selected.green, color.green, 0.001f)
        assertEquals(selected.blue, color.blue, 0.001f)
    }

    @Test
    fun `android native glass export content keeps full theme hue while partially covered`() {
        val unselected = Color(0xFF202124)
        val selected = Color(0xFF00A1D6)
        val color = resolveBottomBarGlassExportContentColor(
            unselectedColor = unselected,
            selectedColor = selected,
            themeWeight = 0.42f,
            glassEnabled = true
        )

        assertEquals(selected.red, color.red, 0.001f)
        assertEquals(selected.green, color.green, 0.001f)
        assertEquals(selected.blue, color.blue, 0.001f)
    }

    @Test
    fun `android native glass export content keeps uncovered item neutral`() {
        val unselected = Color(0xFF202124)
        val selected = Color(0xFF00A1D6)
        val color = resolveBottomBarGlassExportContentColor(
            unselectedColor = unselected,
            selectedColor = selected,
            themeWeight = 0f,
            glassEnabled = true
        )

        assertEquals(unselected.red, color.red, 0.001f)
        assertEquals(unselected.green, color.green, 0.001f)
        assertEquals(unselected.blue, color.blue, 0.001f)
    }

    @Test
    fun `android native idle glass indicator keeps neutral gray capsule`() {
        val themeIndicator = resolveAndroidNativeIndicatorColor(
            themeColor = Color(0xFF00A1D6),
            darkTheme = false
        )
        val idleIndicator = resolveAndroidNativeIdleIndicatorSurfaceColor(
            darkTheme = false
        )

        assertEquals(Color.Black.red, idleIndicator.red, 0.001f)
        assertEquals(Color.Black.green, idleIndicator.green, 0.001f)
        assertEquals(Color.Black.blue, idleIndicator.blue, 0.001f)
        assertEquals(0.1f, idleIndicator.alpha, 0.003f)
        assertFalse(
            idleIndicator.red == themeIndicator.red &&
                idleIndicator.green == themeIndicator.green &&
                idleIndicator.blue == themeIndicator.blue
        )
    }

    @Test
    fun `moving floating bottom bar staggers shell and indicator refraction offsets`() {
        val profile = resolveBottomBarRefractionMotionProfile(
            position = 1.35f,
            velocity = 900f,
            isDragging = true
        )

        assertTrue(profile.progress > 0f)
        assertTrue(profile.exportPanelOffsetFraction > 0f)
        assertTrue(profile.indicatorPanelOffsetFraction > 0f)
        assertTrue(profile.visiblePanelOffsetFraction > 0f)
        assertTrue(profile.exportPanelOffsetFraction < profile.indicatorPanelOffsetFraction)
        assertTrue(profile.visiblePanelOffsetFraction < profile.indicatorPanelOffsetFraction)
        assertTrue(profile.visibleSelectionEmphasis < 1f)
        assertTrue(profile.exportSelectionEmphasis < 1f)
        assertTrue(profile.forceChromaticAberration)
        assertTrue(profile.indicatorLensAmountScale > 1f)
        assertTrue(profile.indicatorLensHeightScale > 1f)
    }

    @Test
    fun `idle floating bottom bar refraction profile stays neutral`() {
        val profile = resolveBottomBarRefractionMotionProfile(
            position = 2f,
            velocity = 0f,
            isDragging = false
        )

        assertEquals(0f, profile.progress, 0.001f)
        assertEquals(0f, profile.exportPanelOffsetFraction, 0.001f)
        assertEquals(0f, profile.indicatorPanelOffsetFraction, 0.001f)
        assertEquals(0f, profile.visiblePanelOffsetFraction, 0.001f)
        assertEquals(1f, profile.visibleSelectionEmphasis, 0.001f)
        assertEquals(1f, profile.exportSelectionEmphasis, 0.001f)
    }

    @Test
    fun `moving floating indicator uses combined backdrop when tinted layer is exported`() {
        val policy = resolveBottomBarRefractionLayerPolicy(
            isFloating = true,
            isLiquidGlassEnabled = true,
            indicatorVisualPolicy = BottomBarIndicatorVisualPolicy(
                isInMotion = true,
                shouldRefract = true,
                useNeutralTint = false
            )
        )

        assertTrue(policy.captureTintedContentLayer)
        assertTrue(policy.useCombinedBackdrop)
    }

    @Test
    fun `idle floating indicator does not export tinted layer or combined backdrop`() {
        val policy = resolveBottomBarRefractionLayerPolicy(
            isFloating = true,
            isLiquidGlassEnabled = true,
            indicatorVisualPolicy = BottomBarIndicatorVisualPolicy(
                isInMotion = false,
                shouldRefract = false,
                useNeutralTint = false
            )
        )

        assertFalse(policy.captureTintedContentLayer)
        assertFalse(policy.useCombinedBackdrop)
    }
}
