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
        val topBarSource = topBar.readText()
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
            "top tab row should only treat chrome as external when the outer surface is actually drawn",
            topHeaderSource.contains("hasOuterChromeSurface = !useUnifiedTopPanel && drawTopTabOuterChromeSurface")
        )
        assertTrue(
            "home header should suppress the nested top tab chrome when the unified top panel already draws it",
            topHeaderSource.contains("drawChromeSurface = !useUnifiedTopPanel && drawTopTabOuterChromeSurface") &&
                topTabChrome.readText().contains("drawChromeSurface: Boolean = true")
        )
        assertTrue(
            "top tabs should render after the search layer so expanded state matches the reference screenshot",
            topHeaderSource.indexOf(".height(currentSearchHeight)") in 0 until topHeaderSource.indexOf("topTabsContent()")
        )
        assertFalse(
            "top tabs should be called after search height instead of swapping search below tabs",
            topHeaderSource.substringBefore(".height(currentSearchHeight)").contains("topTabsContent()")
        )
        assertTrue(
            "top tab row should use the lightweight native tab implementation",
            topBarSource.contains("LightweightHomeTopTabs(") &&
                topBarSource.contains("resolveTopTabClickAction(index, selectedIndex)")
        )
        assertFalse(
            "top tab row should not keep the old liquid dock renderer",
            topBarSource.contains("private fun TopTabDockSurface(") ||
                topBarSource.contains("private fun Md3CategoryTabRow(")
        )
        assertFalse(
            "top tab chrome should not clip enlarged child indicators to the tab shell",
            topTabChrome.readText().contains(".clip(tabShape)")
        )
        assertFalse(
            "top tab dock should not switch sampling off during feed scroll",
            topBarSource.contains("shouldSampleTopTabDockBackdrop(")
        )
        assertTrue(
            "top tab row should continue to follow pager drag offset",
            topBarSource.contains("resolveTopTabIndicatorRenderPosition(") &&
                topBarSource.contains("pagerCurrentPageOffsetFraction = pagerState?.currentPageOffsetFraction")
        )
        assertTrue(
            "MD3 top tab indicator should be a single moving layer tied to pager offset",
            topBarSource.contains("resolveMd3TopTabIndicatorTranslationPx(") &&
                topBarSource.contains("translationX = md3IndicatorTranslationXPx")
        )
        val lightweightTopTabItemSource = topBarSource
            .substringAfter("private fun LightweightTopTabItem(")
            .substringBefore("@OptIn(ExperimentalMaterial3Api::class)")
        assertFalse(
            "MD3 top tab item should not draw a second per-item underline",
            lightweightTopTabItemSource.contains(".align(Alignment.BottomCenter)")
        )
        assertTrue(
            "matched top dock helper should still use the KSU floating dock renderer for header controls",
            topBarSource.contains(".kernelSuFloatingDockSurface(")
        )
        assertFalse(
            "top tab row should not keep backdrop capture, refraction, or liquid indicator layers",
            topBarSource.contains("backdrop = tabsBackdrop") ||
                topBarSource.contains(".layerBackdrop(tabsBackdrop)") ||
                topBarSource.contains("rememberCombinedBackdrop(backdrop, tabsBackdrop)") ||
                topBarSource.contains("rememberCombinedBackdrop(backdrop, tabContentBackdrop)")
        )
        assertFalse(
            "top tab indicator should not keep its old custom indicator renderer",
            topBarSource.contains("BottomBarStyleIndicatorSurface(") ||
                topBarSource.contains("LiquidIndicator(") ||
                topBarSource.contains("rememberCombinedBackdrop(backdrop, tabsBackdrop)") ||
                topBarSource.contains("rememberCombinedBackdrop(backdrop, tabContentBackdrop)")
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
