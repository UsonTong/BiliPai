package com.android.purebilibili.feature.home.components

import java.io.File
import com.android.purebilibili.core.theme.UiPreset
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BottomBarLiquidSegmentedControlStructureTest {

    @Test
    fun `android native inline segmented control avoids liquid pill when global glass is enabled`() {
        assertEquals(
            SegmentedControlChromeStyle.ANDROID_NATIVE_UNDERLINE,
            resolveSegmentedControlChromeStyle(
                uiPreset = UiPreset.MD3,
                androidNativeLiquidGlassEnabled = true,
                preferInlineContentStyle = true
            )
        )
    }

    @Test
    fun `android native chrome segmented control keeps liquid pill when global glass is enabled`() {
        assertEquals(
            SegmentedControlChromeStyle.LIQUID_PILL,
            resolveSegmentedControlChromeStyle(
                uiPreset = UiPreset.MD3,
                androidNativeLiquidGlassEnabled = true,
                preferInlineContentStyle = false
            )
        )
    }

    @Test
    fun `segmented control keeps sliding glass by default with opt out flag`() {
        val source = loadSource(
            "app/src/main/java/com/android/purebilibili/feature/home/components/BottomBarLiquidSegmentedControl.kt"
        )

        assertTrue(source.contains("BottomBarMotionProfile.ANDROID_NATIVE_FLOATING"))
        assertTrue(source.contains("resolveBottomBarRefractionMotionProfile("))
        assertTrue(source.contains(".background(containerColor, containerShape)"))
        assertTrue(source.contains("val neutralIndicatorColor = if (isDarkTheme) Color.White.copy(0.1f) else Color.Black.copy(0.1f)"))
        assertTrue(source.contains("resolveLiquidSegmentedIndicatorColor("))
        assertTrue(source.contains("liquidGlassEffectsEnabled: Boolean = true"))
        assertTrue(source.contains("dragSelectionEnabled: Boolean = true"))
        assertTrue(source.contains("background(indicatorColor, indicatorShape)"))
        assertFalse(source.contains("rememberCombinedBackdrop("))
        assertFalse(source.contains("shellBackdrop"))
        assertTrue(source.contains("val contentBackdrop = rememberLayerBackdrop()"))
        assertTrue(source.contains(".layerBackdrop(contentBackdrop)"))
        assertTrue(source.contains("val useIndicatorBackdrop = liquidGlassEnabled && motionProgress > 0f"))
        assertTrue(source.contains("drawBackdrop("))
        assertTrue(source.contains("backdrop = contentBackdrop"))
        assertTrue(source.contains("shape = { containerShape }"))
        assertTrue(source.contains("lens("))
        assertTrue(source.contains("chromaticAberration = true"))
        assertTrue(source.contains("Highlight.Default.copy(alpha = motionProgress)"))
        assertTrue(source.contains("Shadow(alpha = if (liquidGlassEnabled) motionProgress else 0f)"))
        assertTrue(source.contains("InnerShadow("))
        assertTrue(source.contains("getHomeSettings("))
        assertTrue(source.contains("resolveEffectiveLiquidGlassEnabled("))
        assertTrue(source.contains("resolveSegmentedControlChromeStyle("))
        assertTrue(source.contains("AndroidNativeUnderlinedSegmentedControl("))
        assertTrue(source.contains("SegmentedControlChromeStyle.ANDROID_NATIVE_UNDERLINE"))
        assertTrue(source.contains("onIndicatorPositionChanged?.invoke(safeSelectedIndex.toFloat())"))
        assertTrue(source.contains(".widthIn(min = 28.dp, max = 56.dp)"))
        assertTrue(source.contains("if (enabled && itemCount > 1)"))
        assertTrue(source.contains("consumePointerChanges = dragSelectionEnabled"))
        assertTrue(source.contains("notifyIndexChanged = dragSelectionEnabled"))
        assertTrue(source.contains("settleIndex = if (dragSelectionEnabled) null else safeSelectedIndex"))
        assertFalse(source.contains("indicatorEffectProgress"))
        assertFalse(source.contains("backdrop = if (shouldRefractContent)"))
        assertFalse(source.contains("backdrop = shellBackdrop"))
        assertFalse(source.contains(".clip(containerShape)"))
        assertTrue(source.contains(".offset(x = segmentWidth * dragState.value)"))
        assertTrue(source.contains("78f / 56f"))
        assertTrue(source.contains("dragState.velocity / 10f"))
        assertTrue(source.contains("resolveBottomBarItemMotionVisual("))
        val indicatorIndex = source.indexOf("drawBackdrop(")
        val visibleLabelsIndex = source.indexOf(
            "selectionEmphasis = refractionMotionProfile.visibleSelectionEmphasis",
            startIndex = indicatorIndex
        )
        assertTrue(indicatorIndex >= 0)
        assertTrue(visibleLabelsIndex > indicatorIndex)
        assertFalse(source.contains("LiquidIndicator("))
        assertFalse(source.contains("resolveBottomBarIndicatorPolicy(itemCount = itemCount)"))
        assertFalse(source.contains("indicatorWidthMultiplier = 0.92f"))
        assertFalse(source.contains("maxScale = 1.06f"))
    }

    @Test
    fun `global video dynamic and live segmented surfaces share android native fallback`() {
        val paths = listOf(
            "app/src/main/java/com/android/purebilibili/feature/video/ui/components/CommentSortFilterBar.kt",
            "app/src/main/java/com/android/purebilibili/feature/video/screen/VideoContentSection.kt",
            "app/src/main/java/com/android/purebilibili/feature/dynamic/components/DynamicTopBar.kt",
            "app/src/main/java/com/android/purebilibili/feature/live/LiveListScreen.kt",
            "app/src/main/java/com/android/purebilibili/feature/live/LiveAreaScreen.kt",
            "app/src/main/java/com/android/purebilibili/feature/live/LivePlayerScreen.kt"
        )

        paths.forEach { path ->
            assertTrue(
                loadSource(path).contains("BottomBarLiquidSegmentedControl("),
                "$path should keep using BottomBarLiquidSegmentedControl so the global Android native fallback applies"
            )
        }
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
