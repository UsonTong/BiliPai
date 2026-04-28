package com.android.purebilibili.feature.home.components

import java.io.File
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BottomBarMiuixStructureTest {

    @Test
    fun `android native floating branch renders through kernelsu aligned renderer`() {
        val source = loadSource("app/src/main/java/com/android/purebilibili/feature/home/components/BottomBar.kt")
        val kernelSuRendererSource = source
            .substringAfter("private fun KernelSuAlignedBottomBar(")
            .substringBefore("@Composable\nprivate fun AndroidNativeBottomBarItem(")

        assertTrue(source.contains("KernelSuAlignedBottomBar("))
        assertTrue(kernelSuRendererSource.contains("AndroidNativeBottomBarTuning"))
        assertTrue(kernelSuRendererSource.contains("resolveSharedBottomBarCapsuleShape("))
        assertTrue(kernelSuRendererSource.contains("drawBackdrop("))
        assertTrue(kernelSuRendererSource.contains("vibrancy()"))
        assertTrue(kernelSuRendererSource.contains("lens("))
        assertTrue(kernelSuRendererSource.contains("rememberCombinedBackdrop(backdrop, tabsBackdrop)"))
        assertTrue(kernelSuRendererSource.contains("val tabsBackdrop = rememberLayerBackdrop()"))
        assertTrue(kernelSuRendererSource.contains("val progress = dampedDragState.pressProgress"))
        assertTrue(kernelSuRendererSource.contains("layerBlock = {"))
        assertTrue(kernelSuRendererSource.contains("val indicatorScale = lerp(1f, 78f / 56f, motionProgress)"))
        assertTrue(kernelSuRendererSource.contains("val velocity = dampedDragState.velocity / 10f"))
        assertTrue(kernelSuRendererSource.contains("scaleX = indicatorScale /"))
        assertTrue(kernelSuRendererSource.contains("scaleY = indicatorScale *"))
        assertTrue(kernelSuRendererSource.contains("chromaticAberration = true"))
        assertTrue(kernelSuRendererSource.contains("selected = visual.useSelectedIcon,"))
        assertTrue(kernelSuRendererSource.contains("contentColorOverride = contentColor,"))
        assertTrue(kernelSuRendererSource.contains("selectionEmphasis = refractionMotionProfile.exportSelectionEmphasis"))
        assertTrue(kernelSuRendererSource.contains("Highlight.Default.copy(alpha = motionProgress)"))
        assertFalse(kernelSuRendererSource.contains("item = currentItem,"))
        assertFalse(kernelSuRendererSource.contains("val tintedContentBackdrop = rememberLayerBackdrop()"))
        assertFalse(kernelSuRendererSource.contains("val refractionMotionProfile by remember"))
    }

    @Test
    fun `sukisu renderer draws visible content below indicator with transparent input overlay`() {
        val source = loadSource("app/src/main/java/com/android/purebilibili/feature/home/components/BottomBar.kt")
        val kernelSuRendererSource = source
            .substringAfter("private fun KernelSuAlignedBottomBar(")
            .substringBefore("@Composable\nprivate fun AndroidNativeBottomBarItem(")

        val visibleContentIndex = kernelSuRendererSource.indexOf(
            "selectionEmphasis = refractionMotionProfile.visibleSelectionEmphasis"
        )
        val tintCaptureIndex = kernelSuRendererSource.indexOf(".layerBackdrop(tabsBackdrop)")
        val indicatorIndex = kernelSuRendererSource.indexOf("backdrop = contentBackdrop")
        val hitOverlayIndex = kernelSuRendererSource.indexOf(
            ".alpha(0f)\n                        .graphicsLayer { translationX = panelOffsetPx }\n                        .horizontalDragGesture",
            startIndex = indicatorIndex
        )

        assertTrue(visibleContentIndex >= 0)
        assertTrue(tintCaptureIndex > visibleContentIndex)
        assertTrue(indicatorIndex > tintCaptureIndex)
        assertTrue(hitOverlayIndex > indicatorIndex)
    }

    @Test
    fun `android native input overlay forwards press state to indicator animation`() {
        val source = loadSource("app/src/main/java/com/android/purebilibili/feature/home/components/BottomBar.kt")
        val kernelSuRendererSource = source
            .substringAfter("private fun KernelSuAlignedBottomBar(")
            .substringBefore("@Composable\nprivate fun AndroidNativeBottomBarItem(")
        val androidItemSource = source.substringAfter("@Composable\nprivate fun AndroidNativeBottomBarItem(")

        assertTrue(kernelSuRendererSource.contains("onPressChanged = dampedDragState::setPressed"))
        assertTrue(androidItemSource.contains("collectIsPressedAsState()"))
        assertTrue(androidItemSource.contains("LaunchedEffect(isPressed, interactive)"))
    }

    @Test
    fun `ios floating bottom bar also routes to sukisu renderer`() {
        val source = loadSource("app/src/main/java/com/android/purebilibili/feature/home/components/BottomBar.kt")
        val iosRendererSource = source
            .substringAfter("if (LocalUiPreset.current == UiPreset.MD3)")
            .substringBefore("// 🔒 [防抖]")

        assertTrue(iosRendererSource.contains("KernelSuAlignedBottomBar("))
        assertTrue(iosRendererSource.contains("iconStyle = SharedFloatingBottomBarIconStyle.CUPERTINO"))
    }

    @Test
    fun `android native miuix variant routes to dedicated miuix bottom bar renderer`() {
        val source = loadSource("app/src/main/java/com/android/purebilibili/feature/home/components/BottomBar.kt")

        assertTrue(source.contains("val androidNativeVariant = LocalAndroidNativeVariant.current"))
        assertTrue(source.contains("androidNativeVariant == AndroidNativeVariant.MIUIX"))
        assertTrue(source.contains("MiuixBottomBar("))
        assertTrue(source.contains("if (isFloating) {"))
        assertTrue(source.contains("KernelSuAlignedBottomBar("))
        assertTrue(source.contains("iconStyle = SharedFloatingBottomBarIconStyle.CUPERTINO"))
        assertTrue(source.contains("private enum class SharedFloatingBottomBarIconStyle"))
        assertTrue(source.contains("MiuixNavigationBar("))
        assertTrue(source.contains("MiuixDockedBottomBarItem("))
    }

    @Test
    fun `docked miuix bottom bar avoids floating navigation insets`() {
        val source = loadSource("app/src/main/java/com/android/purebilibili/feature/home/components/BottomBar.kt")
        val miuixRendererSource = source
            .substringAfter("private fun MiuixBottomBar(")
            .substringBefore("@Composable\nprivate fun MiuixFloatingCapsuleBottomBar(")

        assertTrue(miuixRendererSource.contains("MiuixNavigationBar("))
        assertTrue(miuixRendererSource.contains("MiuixDockedBottomBarItem("))
        assertFalse(miuixRendererSource.contains("MiuixNavigationBarItem("))
        assertFalse(miuixRendererSource.contains("MiuixFloatingNavigationBar("))
        assertFalse(miuixRendererSource.contains("MiuixFloatingNavigationBarItem("))
    }

    private fun loadSource(path: String): String {
        val normalizedPath = path.removePrefix("app/")
        val sourceFile = listOf(
            File(path),
            File(normalizedPath)
        ).firstOrNull { it.exists() }
        require(sourceFile != null) { "Cannot locate $path from ${File(".").absolutePath}" }
        return sourceFile.readText()
    }
}
