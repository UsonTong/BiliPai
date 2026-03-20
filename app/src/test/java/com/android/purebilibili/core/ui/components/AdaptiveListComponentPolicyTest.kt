package com.android.purebilibili.core.ui.components

import androidx.compose.material3.darkColorScheme
import com.android.purebilibili.core.theme.iOSBlue
import com.android.purebilibili.core.theme.iOSGreen
import com.android.purebilibili.core.theme.iOSPurple
import com.android.purebilibili.core.theme.iOSRed
import com.android.purebilibili.core.theme.iOSSystemGray
import com.android.purebilibili.core.theme.UiPreset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AdaptiveListComponentPolicyTest {

    @Test
    fun `md3 preset should use taller search bar and hide adaptive list dividers`() {
        val spec = resolveAdaptiveListComponentVisualSpec(UiPreset.MD3)

        assertEquals(48, spec.searchBarHeightDp)
        assertEquals(28, spec.searchBarCornerRadiusDp)
        assertEquals(40, spec.iconContainerSizeDp)
        assertEquals(22, spec.iconGlyphSizeDp)
        assertEquals(0.18f, spec.iconBackgroundAlpha, 0.0001f)
        assertEquals(0f, spec.dividerThicknessDp, 0.0001f)
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
        assertEquals(0.5f, spec.dividerThicknessDp, 0.0001f)
        assertEquals(66, spec.dividerStartIndentDp)
        assertTrue(spec.groupCornerRadiusDp < resolveAdaptiveListComponentVisualSpec(UiPreset.MD3).groupCornerRadiusDp)
    }

    @Test
    fun `md3 preset should map legacy ios accent tints to semantic colors`() {
        val colorScheme = darkColorScheme()

        assertEquals(
            colorScheme.secondary,
            resolveAdaptiveSemanticIconTint(iOSBlue, UiPreset.MD3, colorScheme)
        )
        assertEquals(
            colorScheme.primary,
            resolveAdaptiveSemanticIconTint(iOSGreen, UiPreset.MD3, colorScheme)
        )
        assertEquals(
            colorScheme.tertiary,
            resolveAdaptiveSemanticIconTint(iOSPurple, UiPreset.MD3, colorScheme)
        )
        assertEquals(
            colorScheme.error,
            resolveAdaptiveSemanticIconTint(iOSRed, UiPreset.MD3, colorScheme)
        )
        assertEquals(
            colorScheme.onSurfaceVariant,
            resolveAdaptiveSemanticIconTint(iOSSystemGray, UiPreset.MD3, colorScheme)
        )
    }

    @Test
    fun `md3 preset without dynamic color should collapse legacy accent tints to primary`() {
        val colorScheme = darkColorScheme()

        assertEquals(
            colorScheme.primary,
            resolveAdaptiveSemanticIconTint(iOSBlue, UiPreset.MD3, colorScheme, useSemanticAccentRoles = false)
        )
        assertEquals(
            colorScheme.primary,
            resolveAdaptiveSemanticIconTint(iOSPurple, UiPreset.MD3, colorScheme, useSemanticAccentRoles = false)
        )
        assertEquals(
            colorScheme.primary,
            resolveAdaptiveSemanticIconTint(iOSGreen, UiPreset.MD3, colorScheme, useSemanticAccentRoles = false)
        )
        assertEquals(
            colorScheme.error,
            resolveAdaptiveSemanticIconTint(iOSRed, UiPreset.MD3, colorScheme, useSemanticAccentRoles = false)
        )
        assertEquals(
            colorScheme.onSurfaceVariant,
            resolveAdaptiveSemanticIconTint(iOSSystemGray, UiPreset.MD3, colorScheme, useSemanticAccentRoles = false)
        )
    }

    @Test
    fun `ios preset should preserve legacy ios accent tints`() {
        val colorScheme = darkColorScheme()

        assertEquals(
            iOSBlue,
            resolveAdaptiveSemanticIconTint(iOSBlue, UiPreset.IOS, colorScheme)
        )
    }

    @Test
    fun `md3 preset should defer switch colors to material defaults`() {
        val colorScheme = darkColorScheme()

        val spec = resolveAdaptiveSwitchVisualSpec(
            uiPreset = UiPreset.MD3,
            colorScheme = colorScheme
        )

        assertTrue(spec.usePlatformDefaults)
    }
}
