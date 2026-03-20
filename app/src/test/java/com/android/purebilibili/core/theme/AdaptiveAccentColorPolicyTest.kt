package com.android.purebilibili.core.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.graphics.Color
import kotlin.test.Test
import kotlin.test.assertEquals

class AdaptiveAccentColorPolicyTest {

    @Test
    fun `primary accent falls back to container when dark monet primary is near white`() {
        val scheme = darkColorScheme(
            primary = Color.White,
            onPrimary = Color.White,
            primaryContainer = Color(0xFF2A2A2A),
            onPrimaryContainer = Color(0xFFF2F2F2),
            surface = Color(0xFF121212)
        )

        val colors = resolveAdaptivePrimaryAccentColors(scheme)

        assertEquals(scheme.primaryContainer, colors.backgroundColor)
        assertEquals(scheme.onPrimaryContainer, colors.contentColor)
    }

    @Test
    fun `primary accent keeps md3 primary pair when contrast is healthy`() {
        val scheme = darkColorScheme(
            primary = Color(0xFF0057D8),
            onPrimary = Color.White,
            primaryContainer = Color(0xFF0F2942),
            onPrimaryContainer = Color(0xFFD6E9FF),
            surface = Color(0xFF121212)
        )

        val colors = resolveAdaptivePrimaryAccentColors(scheme)

        assertEquals(scheme.primary, colors.backgroundColor)
        assertEquals(scheme.onPrimary, colors.contentColor)
    }

    @Test
    fun `tertiary accent falls back to container when dark monet tertiary is too bright`() {
        val scheme = darkColorScheme(
            tertiary = Color(0xFFFDF7FF),
            onTertiary = Color.White,
            tertiaryContainer = Color(0xFF342B3A),
            onTertiaryContainer = Color(0xFFF4DAFF),
            surface = Color(0xFF121212)
        )

        val colors = resolveAdaptiveTertiaryAccentColors(scheme)

        assertEquals(scheme.tertiaryContainer, colors.backgroundColor)
        assertEquals(scheme.onTertiaryContainer, colors.contentColor)
    }
}
