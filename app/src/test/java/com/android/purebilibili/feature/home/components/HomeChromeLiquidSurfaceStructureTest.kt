package com.android.purebilibili.feature.home.components

import java.nio.file.Files
import java.nio.file.Paths
import kotlin.io.path.readText
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeChromeLiquidSurfaceStructureTest {

    @Test
    fun `top header uses shared renderer while bottom bar uses ksu renderer only`() {
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
        val topHeaderSource = topHeader.readText()
        val topHeaderMatchedSurfaceCalls = Regex("""\.homeTopBottomBarMatchedSurface\(""")
            .findAll(topHeaderSource)
            .count()
        val topHeaderDisabledShellLensCalls = Regex("""drawShellLens\s*=\s*false""")
            .findAll(topHeaderSource)
            .count()
        assertTrue(
            "top header should use the same matched dock surface helper as the bottom bar",
            topHeaderMatchedSurfaceCalls > 0
        )
        assertTrue(
            "all matched top header controls should disable the full-shell lens that creates a center refraction seam",
            topHeaderDisabledShellLensCalls >= topHeaderMatchedSurfaceCalls
        )
        assertTrue(
            "matched top dock helper should use the KSU floating dock renderer, not the generic chrome renderer",
            topBar.readText().contains(".kernelSuFloatingDockSurface(")
        )
        assertTrue(
            "top tab row should render its own dock surface when embedded in the unified top panel",
            topHeaderSource.contains("hasOuterChromeSurface = !useUnifiedTopPanel")
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
            "top tab dock should reuse bottom-bar tuning and container color",
            topBar.readText().contains("resolveAndroidNativeBottomBarTuning(") &&
                topBar.readText().contains("resolveAndroidNativeFloatingBottomBarContainerColor(")
        )
        assertTrue(
            "top tab dock should disable the full-shell lens that creates a center refraction seam",
            topBar.readText().contains("drawShellLens = false")
        )
        assertFalse(
            "top tab dock should not clip the enlarged indicator layer",
            topBar.readText().contains("modifier = modifier\n            .clip(shape)")
        )
        assertFalse(
            "top tab chrome should not clip enlarged child indicators to the tab shell",
            topTabChrome.readText().contains(".clip(tabShape)")
        )
        assertFalse(
            "top tab dock should not switch sampling off during feed scroll",
            topBar.readText().contains("shouldSampleTopTabDockBackdrop(")
        )
        assertTrue(
            "top tab floating indicator should keep manual row scroll out of LiquidIndicator viewport clamp",
            topBar.readText().contains("resolveTopTabIndicatorViewportClampShiftPx(")
        )
        assertTrue(
            "top tab indicator should follow pager drag offset while using a static neutral visual policy",
            topBar.readText().contains("resolveTopTabIndicatorRenderPosition(") &&
                topBar.readText().contains("pagerCurrentPageOffsetFraction = pagerState?.currentPageOffsetFraction") &&
                topBar.readText().contains("resolveTopTabStaticIndicatorVisualPolicy(") &&
                topBar.readText().contains("resolveTopTabNeutralIndicatorColor(")
        )
        assertTrue(
            "top tab indicator should inline the same backdrop indicator block as the bottom bar",
            topBar.readText().contains("resolveBottomBarBackdropPresetIndicatorLens(") &&
                topBar.readText().contains("drawBackdrop(") &&
                topBar.readText().contains("backdrop = tabsBackdrop") &&
                topBar.readText().contains("shape = { selectedIndicatorShape }") &&
                topBar.readText().contains("Highlight.Default.copy(alpha = indicatorHighlightAlpha)") &&
                topBar.readText().contains("InnerShadow(") &&
                topBar.readText().contains("resolveAndroidNativeIdleIndicatorSurfaceColor(") &&
                topBar.readText().contains("resolveBottomBarIndicatorLayerTransform(") &&
                topBar.readText().contains(".layerBackdrop(tabsBackdrop)")
        )
        assertFalse(
            "top tab indicator should not keep its old custom indicator renderer",
            topBar.readText().contains("BottomBarStyleIndicatorSurface(") ||
                topBar.readText().contains("LiquidIndicator(") ||
                topBar.readText().contains("rememberCombinedBackdrop(backdrop, tabsBackdrop)") ||
                topBar.readText().contains("rememberCombinedBackdrop(backdrop, tabContentBackdrop)")
        )
        assertTrue(
            "KSU dock surface should use backdrop vibrancy, blur, and lens like the floating bottom bar",
            bottomBar.readText().contains("internal fun Modifier.kernelSuFloatingDockSurface(") &&
                bottomBar.readText().contains("vibrancy()") &&
                bottomBar.readText().contains("drawShellLens: Boolean = true") &&
                bottomBar.readText().contains("glassEnabled && drawShellLens") &&
                bottomBar.readText().contains("refractionHeight = 24.dp.toPx()") &&
                bottomBar.readText().contains("refractionAmount = 24.dp.toPx()") &&
                bottomBar.readText().contains("depthEffect = true") &&
                bottomBar.readText().contains("chromaticAberration = true")
        )
        assertFalse(
            "bottom bar should not keep the old appChromeLiquidSurface renderer",
            bottomBar.readText().contains(".appChromeLiquidSurface(")
        )
        assertFalse(
            "bottom bar should not keep the old floating dock surface style",
            bottomBar.readText().contains("resolveFloatingDockLiquidSurfaceStyle(")
        )
        assertFalse(
            "bottom bar should not keep the old LiquidIndicator renderer",
            bottomBar.readText().contains("LiquidIndicator(")
        )
        assertFalse(
            "bottom bar should not keep the old BottomBarContent renderer",
            bottomBar.readText().contains("BottomBarContent(")
        )
    }
}
