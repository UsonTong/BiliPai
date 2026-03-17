package com.android.purebilibili.core.ui.components

import com.android.purebilibili.core.theme.UiPreset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AdaptiveListComponentPolicyTest {

    @Test
    fun `md3 preset should use taller search bar and flatter divider geometry`() {
        val spec = resolveAdaptiveListComponentVisualSpec(UiPreset.MD3)

        assertEquals(48, spec.searchBarHeightDp)
        assertEquals(28, spec.searchBarCornerRadiusDp)
        assertEquals(40, spec.iconContainerSizeDp)
        assertEquals(22, spec.iconGlyphSizeDp)
        assertEquals(0.18f, spec.iconBackgroundAlpha, 0.0001f)
        assertEquals(1f, spec.dividerThicknessDp)
        assertEquals(16, spec.dividerStartIndentDp)
    }

    @Test
    fun `ios preset should preserve compact inset list geometry`() {
        val spec = resolveAdaptiveListComponentVisualSpec(UiPreset.IOS)

        assertEquals(40, spec.searchBarHeightDp)
        assertEquals(10, spec.searchBarCornerRadiusDp)
        assertEquals(36, spec.iconContainerSizeDp)
        assertEquals(20, spec.iconGlyphSizeDp)
        assertEquals(0.12f, spec.iconBackgroundAlpha, 0.0001f)
        assertEquals(66, spec.dividerStartIndentDp)
        assertTrue(spec.groupCornerRadiusDp < resolveAdaptiveListComponentVisualSpec(UiPreset.MD3).groupCornerRadiusDp)
    }
}
