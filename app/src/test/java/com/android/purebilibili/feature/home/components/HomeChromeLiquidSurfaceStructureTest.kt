package com.android.purebilibili.feature.home.components

import com.android.purebilibili.core.ui.blur.BlurSurfaceType
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.io.path.readText
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeChromeLiquidSurfaceStructureTest {

    @Test
    fun `top header and bottom bar both use shared liquid surface renderer`() {
        val workspaceRoot = generateSequence(
            Paths.get(System.getProperty("user.dir")).toAbsolutePath()
        ) { current ->
            current.parent
        }.first { candidate ->
            Files.exists(
                candidate.resolve(
                    "app/src/main/java/com/android/purebilibili/feature/home/components/iOSHomeHeader.kt"
                )
            )
        }
        val componentsDir = workspaceRoot.resolve(
            "app/src/main/java/com/android/purebilibili/feature/home/components"
        )

        val sharedRenderer = componentsDir.resolve("HomeChromeLiquidSurface.kt")
        val topHeader = componentsDir.resolve("iOSHomeHeader.kt")
        val topTabChrome = componentsDir.resolve("HomeTopTabChrome.kt")
        val topBar = componentsDir.resolve("TopBar.kt")
        val bottomBar = componentsDir.resolve("BottomBar.kt")

        assertTrue(
            "shared renderer file should exist",
            Files.exists(sharedRenderer)
        )
        assertTrue(
            "top header should delegate to the shared liquid surface renderer",
            topHeader.readText().contains(".appChromeLiquidSurface(")
        )
        assertTrue(
            "top search pill should use the same matched dock surface helper as the bottom bar",
            topHeader.readText().contains(".homeTopBottomBarMatchedSurface(")
        )
        assertTrue(
            "matched top dock helper should use the KSU floating dock renderer, not the generic chrome renderer",
            topBar.readText().contains(".kernelSuFloatingDockSurface(")
        )
        assertTrue(
            "top tab row should render its own dock surface when embedded in the unified top panel",
            topHeader.readText().contains("hasOuterChromeSurface = !useUnifiedTopPanel")
        )
        assertTrue(
            "bottom bar should delegate to the shared liquid surface renderer",
            bottomBar.readText().contains(".appChromeLiquidSurface(")
        )
        assertTrue(
            "top tab dock should use the same KSU dock surface renderer as the bottom bar",
            topBar.readText().contains(".kernelSuFloatingDockSurface(")
        )
        assertTrue(
            "top tab inner dock should use the KSU dock surface renderer",
            topBar.readText().contains(".kernelSuFloatingDockSurface(")
        )
        assertTrue(
            "top tab floating indicator should feed bottom-bar style panel offset into LiquidIndicator viewport shift",
            topBar.readText().contains("viewportShiftPx = scrollOffset - indicatorPanelOffsetPx")
        )
        assertTrue(
            "top tab indicator should follow pager drag offset while using a static neutral visual policy",
            topBar.readText().contains("resolveTopTabIndicatorRenderPosition(") &&
                topBar.readText().contains("pagerCurrentPageOffsetFraction = pagerState?.currentPageOffsetFraction") &&
                topBar.readText().contains("resolveTopTabStaticIndicatorVisualPolicy(") &&
                topBar.readText().contains("resolveTopTabNeutralIndicatorColor(")
        )
        assertTrue(
            "top tab indicator should combine page backdrop and exported tab content while moving",
            topBar.readText().contains("rememberCombinedBackdrop(backdrop, tabContentBackdrop)") &&
                topBar.readText().contains("LiquidIndicator(")
        )
        assertTrue(
            "top tab indicator should keep bottom-bar style chromatic motion tuning",
            topBar.readText().contains("forceChromaticAberration = topTabRefractionProfile.forceChromaticAberration")
        )
        assertTrue(
            "bottom bar should use the shared floating dock liquid surface style",
            bottomBar.readText().contains("resolveFloatingDockLiquidSurfaceStyle(")
        )
        assertTrue(
            "KSU dock surface should use backdrop vibrancy, blur, and lens like the floating bottom bar",
            bottomBar.readText().contains("internal fun Modifier.kernelSuFloatingDockSurface(") &&
                bottomBar.readText().contains("vibrancy()") &&
                bottomBar.readText().contains("lens(24.dp.toPx(), 24.dp.toPx())")
        )
    }

    @Test
    fun `floating dock surface style matches bottom bar glass tuning`() {
        val style = resolveFloatingDockLiquidSurfaceStyle(depthEffect = true)

        assertEquals(BlurSurfaceType.BOTTOM_BAR, style.blurSurfaceType)
        assertEquals(true, style.depthEffect)
        assertEquals(0.02f, style.refractionAmountScrollMultiplier, 0.0001f)
        assertEquals(14f, style.refractionAmountScrollCap, 0.0001f)
        assertEquals(0.00015f, style.surfaceAlphaScrollMultiplier, 0.00001f)
        assertEquals(0.04f, style.surfaceAlphaScrollCap, 0.0001f)
        assertEquals(0.86f, style.darkThemeWhiteOverlayMultiplier, 0.0001f)
        assertEquals(true, style.useTuningSurfaceAlpha)
        assertEquals(0.4f, style.hazeBackgroundAlphaMultiplier, 0.0001f)
    }
}
