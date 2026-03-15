package com.android.purebilibili.feature.home.components

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import com.android.purebilibili.core.ui.blur.BlurSurfaceType
import com.android.purebilibili.feature.home.HomeGlassResolvedColors
import com.android.purebilibili.core.ui.blur.BlurIntensity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class iOSHomeHeaderVisualPolicyTest {

    @Test
    fun `wide liquid glass chrome prefers flat treatment to avoid center seam`() {
        assertEquals(
            HomeTopChromeSurfaceTreatment.FLAT_GLASS,
            resolveHomeTopChromeSurfaceTreatment(
                renderMode = HomeTopChromeRenderMode.LIQUID_GLASS_BACKDROP,
                preferFlatGlass = true
            )
        )
        assertEquals(
            HomeTopChromeSurfaceTreatment.FLAT_GLASS,
            resolveHomeTopChromeSurfaceTreatment(
                renderMode = HomeTopChromeRenderMode.LIQUID_GLASS_HAZE,
                preferFlatGlass = true
            )
        )
    }

    @Test
    fun `compact controls keep structured glass treatment`() {
        assertEquals(
            HomeTopChromeSurfaceTreatment.STRUCTURED_GLASS,
            resolveHomeTopChromeSurfaceTreatment(
                renderMode = HomeTopChromeRenderMode.LIQUID_GLASS_BACKDROP,
                preferFlatGlass = false
            )
        )
        assertEquals(
            HomeTopChromeSurfaceTreatment.STRUCTURED_GLASS,
            resolveHomeTopChromeSurfaceTreatment(
                renderMode = HomeTopChromeRenderMode.BLUR,
                preferFlatGlass = true
            )
        )
    }

    @Test
    fun `wide blur chrome softens highlight and underlay to avoid banding`() {
        val baseHighlight = Color.White.copy(alpha = 0.10f)
        val softenedHighlight = resolveHomeTopChromeHighlightOverlayColor(
            baseColor = baseHighlight,
            renderMode = HomeTopChromeRenderMode.BLUR,
            softenWideChrome = true
        )
        val defaultUnderlay = resolveHomeTopInnerUnderlayColor(
            isLightMode = true,
            renderMode = HomeTopChromeRenderMode.BLUR
        )
        val softenedUnderlay = resolveHomeTopInnerUnderlayColor(
            isLightMode = true,
            renderMode = HomeTopChromeRenderMode.BLUR,
            softenWideChrome = true
        )

        assertTrue(softenedHighlight.alpha < baseHighlight.alpha)
        assertTrue(softenedUnderlay.alpha < defaultUnderlay.alpha)
        assertTrue(softenedUnderlay.alpha > 0f)
    }

    @Test
    fun `home header trims top chrome heights for better content density`() {
        assertEquals(48.dp, resolveHomeTopSearchBarHeight())
        assertEquals(56.dp, resolveHomeTopTabRowHeight(isTabFloating = true))
        assertEquals(46.dp, resolveHomeTopTabRowHeight(isTabFloating = false))
    }

    @Test
    fun `home header trims horizontal spacing without cramping controls`() {
        assertEquals(14.dp, resolveHomeTopSearchRowHorizontalPadding())
        assertEquals(34.dp, resolveHomeTopSearchPillHeight())
        assertEquals(14.dp, resolveHomeTopTabHorizontalPadding(isTabFloating = true))
    }

    @Test
    fun `home header uses symmetrical edge controls around search bar`() {
        assertEquals(40.dp, resolveHomeTopAvatarOuterSize())
        assertEquals(40.dp, resolveHomeTopSettingsButtonSize())
        assertEquals(30.dp, resolveHomeTopAvatarInnerSize())
        assertEquals(20.dp, resolveHomeTopSettingsIconSize())
        assertEquals(6.dp, resolveHomeTopEdgeControlGap())
    }

    @Test
    fun `top chrome uses liquid glass when liquid glass is enabled`() {
        assertEquals(
            TopTabMaterialMode.LIQUID_GLASS,
            resolveHomeTopChromeMaterialMode(
                isBottomBarBlurEnabled = true,
                isLiquidGlassEnabled = true
            )
        )
    }

    @Test
    fun `top chrome uses blur when only blur is enabled`() {
        assertEquals(
            TopTabMaterialMode.BLUR,
            resolveHomeTopChromeMaterialMode(
                isBottomBarBlurEnabled = true,
                isLiquidGlassEnabled = false
            )
        )
    }

    @Test
    fun `top chrome uses plain when blur and liquid glass are disabled`() {
        assertEquals(
            TopTabMaterialMode.PLAIN,
            resolveHomeTopChromeMaterialMode(
                isBottomBarBlurEnabled = false,
                isLiquidGlassEnabled = false
            )
        )
    }

    @Test
    fun `liquid glass top chrome prefers captured backdrop rendering`() {
        assertEquals(
            HomeTopChromeRenderMode.LIQUID_GLASS_BACKDROP,
            resolveHomeTopChromeRenderMode(
                materialMode = TopTabMaterialMode.LIQUID_GLASS,
                isGlassSupported = true,
                hasBackdrop = true,
                hasHazeState = true
            )
        )
    }

    @Test
    fun `liquid glass top chrome falls back to haze liquid glass when backdrop is unavailable`() {
        assertEquals(
            HomeTopChromeRenderMode.LIQUID_GLASS_HAZE,
            resolveHomeTopChromeRenderMode(
                materialMode = TopTabMaterialMode.LIQUID_GLASS,
                isGlassSupported = true,
                hasBackdrop = false,
                hasHazeState = true,
                allowHazeLiquidGlassFallback = true
            )
        )
    }

    @Test
    fun `android 16 liquid glass top chrome falls back to blur when backdrop is unavailable`() {
        assertEquals(
            HomeTopChromeRenderMode.BLUR,
            resolveHomeTopChromeRenderMode(
                materialMode = TopTabMaterialMode.LIQUID_GLASS,
                isGlassSupported = true,
                hasBackdrop = false,
                hasHazeState = true,
                allowHazeLiquidGlassFallback = false
            )
        )
    }

    @Test
    fun `unsupported liquid glass top chrome falls back to blur`() {
        assertEquals(
            HomeTopChromeRenderMode.BLUR,
            resolveHomeTopChromeRenderMode(
                materialMode = TopTabMaterialMode.LIQUID_GLASS,
                isGlassSupported = false,
                hasBackdrop = true,
                hasHazeState = true,
                allowHazeLiquidGlassFallback = true
            )
        )
    }

    @Test
    fun `circle top chrome shape resolves to a lens safe rounded shape`() {
        assertTrue(resolveHomeTopChromeLensShape(CircleShape) is CornerBasedShape)
    }

    @Test
    fun `rectangle top chrome shape resolves to a lens safe rounded shape`() {
        assertTrue(resolveHomeTopChromeLensShape(RectangleShape) is CornerBasedShape)
    }

    @Test
    fun `rounded top chrome shape is preserved for lens rendering`() {
        val shape = RoundedCornerShape(10)

        assertEquals(shape, resolveHomeTopChromeLensShape(shape))
    }

    @Test
    fun `liquid glass readability layer stays lighter than blur`() {
        val liquidAlpha = resolveHomeTopChromeReadabilityAlpha(HomeTopChromeRenderMode.LIQUID_GLASS_BACKDROP)
        val blurAlpha = resolveHomeTopChromeReadabilityAlpha(HomeTopChromeRenderMode.BLUR)

        assertTrue(liquidAlpha < blurAlpha)
    }

    @Test
    fun `blur readability layer stays stronger than plain`() {
        val blurAlpha = resolveHomeTopChromeReadabilityAlpha(HomeTopChromeRenderMode.BLUR)
        val plainAlpha = resolveHomeTopChromeReadabilityAlpha(HomeTopChromeRenderMode.PLAIN)

        assertTrue(blurAlpha > plainAlpha)
    }

    @Test
    fun `top search content alpha is strengthened for readability`() {
        assertTrue(
            resolveHomeTopSearchContentAlpha(HomeTopChromeRenderMode.LIQUID_GLASS_BACKDROP) > 0.72f
        )
        assertTrue(
            resolveHomeTopSearchContentAlpha(HomeTopChromeRenderMode.BLUR) >
                resolveHomeTopSearchContentAlpha(HomeTopChromeRenderMode.PLAIN)
        )
    }

    @Test
    fun `top action icon alpha is strengthened for readability`() {
        assertTrue(
            resolveHomeTopActionIconAlpha(HomeTopChromeRenderMode.LIQUID_GLASS_BACKDROP) > 0.7f
        )
    }

    @Test
    fun `top tab content underlay stays lighter than main readability layer`() {
        val underlay = resolveHomeTopTabContentUnderlayAlpha(HomeTopChromeRenderMode.LIQUID_GLASS_BACKDROP)
        val readability = resolveHomeTopChromeReadabilityAlpha(HomeTopChromeRenderMode.LIQUID_GLASS_BACKDROP)

        assertTrue(underlay < readability)
        assertTrue(underlay > 0f)
    }

    @Test
    fun `top tab unselected alpha is strengthened for readability`() {
        assertTrue(resolveTopTabUnselectedAlpha() > 0.65f)
    }

    @Test
    fun `light mode top search content uses black like bottom bar`() {
        assertEquals(
            Color.Black,
            resolveHomeTopForegroundColor(isLightMode = true)
        )
    }

    @Test
    fun `light mode top action icons use black like bottom bar`() {
        assertEquals(
            Color.Black,
            resolveHomeTopForegroundColor(isLightMode = true)
        )
    }

    @Test
    fun `light mode top tab unselected color uses black like bottom bar`() {
        assertEquals(
            Color.Black,
            resolveTopTabUnselectedColor(isLightMode = true)
        )
    }

    @Test
    fun `dark mode top foreground uses bright text`() {
        assertEquals(
            Color.White.copy(alpha = 0.92f),
            resolveHomeTopForegroundColor(isLightMode = false)
        )
    }

    @Test
    fun `dark mode top tab underlay uses dark tint instead of white`() {
        val underlay = resolveHomeTopInnerUnderlayColor(
            isLightMode = false,
            renderMode = HomeTopChromeRenderMode.LIQUID_GLASS_BACKDROP
        )

        assertNotEquals(Color.White, underlay)
        assertTrue(underlay.red < 0.2f && underlay.green < 0.2f && underlay.blue < 0.2f)
        assertTrue(underlay.alpha > 0f)
    }

    @Test
    fun `dark mode top glass accents are dimmed`() {
        val base = HomeGlassResolvedColors(
            containerColor = Color.White.copy(alpha = 0.28f),
            borderColor = Color.White.copy(alpha = 0.18f),
            highlightColor = Color.White.copy(alpha = 0.20f)
        )

        val tuned = tuneHomeTopGlassColors(
            colors = base,
            isLightMode = false,
            emphasized = true
        )

        assertTrue(tuned.borderColor.alpha < base.borderColor.alpha)
        assertTrue(tuned.highlightColor.alpha < base.highlightColor.alpha)
    }

    @Test
    fun `liquid glass header uses same base alpha as bottom bar`() {
        val alpha = resolveHomeHeaderSurfaceAlpha(
            isGlassEnabled = true,
            blurEnabled = true,
            blurIntensity = BlurIntensity.THIN
        )

        assertEquals(0.10f, alpha, 0.0001f)
    }

    @Test
    fun `blur disabled header falls back to opaque`() {
        val alpha = resolveHomeHeaderSurfaceAlpha(
            isGlassEnabled = true,
            blurEnabled = false,
            blurIntensity = BlurIntensity.THIN
        )

        assertEquals(1f, alpha, 0.0001f)
    }

    @Test
    fun `non-glass header keeps tuned blur-based alpha`() {
        val alpha = resolveHomeHeaderSurfaceAlpha(
            isGlassEnabled = false,
            blurEnabled = true,
            blurIntensity = BlurIntensity.THICK
        )
        val bottomBarAlpha = resolveBottomBarSurfaceColor(
            surfaceColor = Color.White,
            blurEnabled = true,
            blurIntensity = BlurIntensity.THICK
        ).alpha

        assertEquals(bottomBarAlpha, alpha, 0.0001f)
    }

    @Test
    fun `top blur surface type matches bottom bar budget in blur mode`() {
        assertEquals(
            BlurSurfaceType.BOTTOM_BAR,
            resolveHomeTopBlurSurfaceType(HomeTopChromeRenderMode.BLUR)
        )
    }

    @Test
    fun `blur mode uses continuous top slab instead of plain background`() {
        assertEquals(
            HomeTopChromeRenderMode.BLUR,
            resolveHomeTopContinuousSlabRenderMode(HomeTopChromeRenderMode.BLUR)
        )
        assertEquals(
            HomeTopChromeRenderMode.PLAIN,
            resolveHomeTopContinuousSlabRenderMode(HomeTopChromeRenderMode.LIQUID_GLASS_BACKDROP)
        )
    }

    @Test
    fun `blur mode keeps local search and tabs plain to avoid stacked blur lag`() {
        assertEquals(
            HomeTopChromeRenderMode.PLAIN,
            resolveHomeTopLocalChromeRenderMode(HomeTopChromeRenderMode.BLUR)
        )
        assertEquals(
            HomeTopChromeRenderMode.LIQUID_GLASS_BACKDROP,
            resolveHomeTopLocalChromeRenderMode(HomeTopChromeRenderMode.LIQUID_GLASS_BACKDROP)
        )
    }

    @Test
    fun `blur mode ignores scroll and transition budget jitter`() {
        val blurPolicy = resolveHomeTopChromeMotionPolicy(
            renderMode = HomeTopChromeRenderMode.BLUR,
            isScrolling = true,
            isTransitionRunning = true
        )
        val liquidPolicy = resolveHomeTopChromeMotionPolicy(
            renderMode = HomeTopChromeRenderMode.LIQUID_GLASS_BACKDROP,
            isScrolling = true,
            isTransitionRunning = true
        )

        assertFalse(blurPolicy.isScrolling)
        assertFalse(blurPolicy.isTransitionRunning)
        assertTrue(liquidPolicy.isScrolling)
        assertTrue(liquidPolicy.isTransitionRunning)
    }

    @Test
    fun `top blur container color reuses bottom bar surface color rule`() {
        val colors = resolveHomeTopBlurContainerColors(
            colors = HomeGlassResolvedColors(
                containerColor = Color.Transparent,
                borderColor = Color.White.copy(alpha = 0.12f),
                highlightColor = Color.White.copy(alpha = 0.1f)
            ),
            surfaceColor = Color.Black,
            blurIntensity = BlurIntensity.APPLE_DOCK
        )

        assertEquals(
            resolveBottomBarSurfaceColor(
                surfaceColor = Color.Black,
                blurEnabled = true,
                blurIntensity = BlurIntensity.APPLE_DOCK
            ),
            colors.containerColor
        )
    }

    @Test
    fun `plain dark header keeps dark surface when blur and glass are disabled`() {
        val surfaceColor = Color(0xFF121212)
        val alpha = resolveHomeHeaderSurfaceAlpha(
            isGlassEnabled = false,
            blurEnabled = false,
            blurIntensity = BlurIntensity.THIN
        )

        assertEquals(surfaceColor, surfaceColor.copy(alpha = alpha))
    }

    @Test
    fun `docked blur top tabs use same overlay alpha as blur chrome container`() {
        assertEquals(
            0.4f,
            resolveHomeTopTabOverlayAlpha(
                materialMode = TopTabMaterialMode.BLUR,
                isTabFloating = false,
                containerAlpha = 0.4f
            ),
            0.0001f
        )
    }

    @Test
    fun `top tab secondary blur enabled only in static state`() {
        assertTrue(
            shouldEnableTopTabSecondaryBlur(
                hasHeaderBlur = true,
                topTabMaterialMode = TopTabMaterialMode.BLUR,
                isScrolling = false,
                isTransitionRunning = false
            )
        )
    }

    @Test
    fun `liquid glass top tab secondary blur disabled during motion to reduce duplicate blur passes`() {
        assertFalse(
            shouldEnableTopTabSecondaryBlur(
                hasHeaderBlur = true,
                topTabMaterialMode = TopTabMaterialMode.LIQUID_GLASS,
                isScrolling = true,
                isTransitionRunning = false
            )
        )
        assertFalse(
            shouldEnableTopTabSecondaryBlur(
                hasHeaderBlur = true,
                topTabMaterialMode = TopTabMaterialMode.LIQUID_GLASS,
                isScrolling = false,
                isTransitionRunning = true
            )
        )
    }

    @Test
    fun `blur mode keeps top tab secondary blur during motion`() {
        assertTrue(
            shouldEnableTopTabSecondaryBlur(
                hasHeaderBlur = true,
                topTabMaterialMode = TopTabMaterialMode.BLUR,
                isScrolling = true,
                isTransitionRunning = false
            )
        )
        assertTrue(
            shouldEnableTopTabSecondaryBlur(
                hasHeaderBlur = true,
                topTabMaterialMode = TopTabMaterialMode.BLUR,
                isScrolling = false,
                isTransitionRunning = true
            )
        )
    }

    @Test
    fun `floating top tabs use tighter spacing beneath search`() {
        assertEquals(2f, resolveHomeTopTabVerticalPaddingDp(isTabFloating = true), 0.0001f)
        assertEquals(-4f, resolveHomeTopTabYOffsetDp(isTabFloating = true), 0.0001f)
    }

    @Test
    fun `docked top tabs keep neutral spacing beneath search`() {
        assertEquals(0f, resolveHomeTopTabVerticalPaddingDp(isTabFloating = false), 0.0001f)
        assertEquals(0f, resolveHomeTopTabYOffsetDp(isTabFloating = false), 0.0001f)
    }

    @Test
    fun `floating top tabs no longer use highlighted border`() {
        assertEquals(
            0f,
            resolveHomeHeaderTabBorderAlpha(
                isTabFloating = true,
                isTabGlassEnabled = true
            ),
            0.0001f
        )
        assertEquals(
            0f,
            resolveHomeHeaderTabBorderAlpha(
                isTabFloating = true,
                isTabGlassEnabled = false
            ),
            0.0001f
        )
    }
}
