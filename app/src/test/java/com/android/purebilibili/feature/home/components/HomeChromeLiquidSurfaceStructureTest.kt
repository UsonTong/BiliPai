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
            "top tab dock should use the same floating dock liquid surface style as the bottom bar",
            topTabChrome.readText().contains("resolveFloatingDockLiquidSurfaceStyle(")
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
            "top floating dock should render category text through the bottom-style fixed slot row",
            topBar.readText().contains("private fun TopDockBottomStyleRow(") &&
                topBar.readText().contains("TopDockBottomStyleItem(")
        )
        assertTrue(
            "top floating dock items should match bottom bar slot shape and text metrics",
            topBar.readText().contains(".defaultMinSize(minWidth = 76.dp)") &&
                topBar.readText().contains(".clip(resolveSharedBottomBarCapsuleShape())") &&
                topBar.readText().contains("fontSize = 11.sp") &&
                topBar.readText().contains("lineHeight = 14.sp") &&
                topBar.readText().contains("fontWeight = FontWeight.Medium")
        )
        assertTrue(
            "top tab exported refraction layer should apply the bottom bar capture width scale",
            topBar.readText().contains("scaleX = topTabRefractionProfile.exportCaptureWidthScale")
        )
        assertTrue(
            "top tab indicator should force chromatic aberration only for combined backdrop refraction",
            topBar.readText().contains(
                "forceChromaticAberration = refractionLayerPolicy.useCombinedBackdrop &&"
            )
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
