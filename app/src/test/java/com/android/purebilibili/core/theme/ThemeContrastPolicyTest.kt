package com.android.purebilibili.core.theme

import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ThemeContrastPolicyTest {

    @Test
    fun `dynamic light scheme keeps readable text colors unchanged`() {
        val scheme = lightColorScheme(
            background = Color(0xFFF6F7FB),
            onBackground = Color(0xFF1B1C1F),
            surface = Color(0xFFFFFFFF),
            onSurface = Color(0xFF202124),
            surfaceVariant = Color(0xFFECEEF4),
            onSurfaceVariant = Color(0xFF5F6368)
        )

        val result = enforceDynamicLightTextContrast(scheme)

        assertEquals(scheme.onBackground, result.onBackground)
        assertEquals(scheme.onSurface, result.onSurface)
        assertEquals(scheme.onSurfaceVariant, result.onSurfaceVariant)
    }

    @Test
    fun `dynamic light scheme falls back when primary text contrast is too low`() {
        val scheme = lightColorScheme(
            background = Color(0xFFF7F7F7),
            onBackground = Color(0xFFF1F1F1),
            surface = Color(0xFFFFFFFF),
            onSurface = Color(0xFFF4F4F4),
            surfaceVariant = Color(0xFFF2F2F2),
            onSurfaceVariant = Color(0xFFECECEC),
            primary = Color(0xFFF5F3F7),
            onPrimary = Color(0xFFFFFFFF),
            primaryContainer = Color(0xFFF7F5F9),
            onPrimaryContainer = Color(0xFFFFFFFF)
        )

        val result = enforceDynamicLightTextContrast(scheme)

        assertTrue(calculateContrastRatio(result.onBackground, result.background) >= 4.5f)
        assertTrue(calculateContrastRatio(result.onSurface, result.surface) >= 4.5f)
        assertTrue(calculateContrastRatio(result.onSurfaceVariant, result.surfaceVariant) >= 3.0f)
        assertTrue(calculateContrastRatio(result.onPrimary, result.primary) >= 4.5f)
        assertTrue(calculateContrastRatio(result.onPrimaryContainer, result.primaryContainer) >= 4.5f)
    }

    @Test
    fun `contrast helper only falls back below threshold`() {
        val preserved = resolveReadableTextColor(
            candidate = Color(0xFF2E3135),
            background = Color.White,
            fallback = TextPrimary,
            minimumContrast = 4.5f
        )
        val replaced = resolveReadableTextColor(
            candidate = Color(0xFFF1F1F1),
            background = Color.White,
            fallback = TextPrimary,
            minimumContrast = 4.5f
        )

        assertEquals(Color(0xFF2E3135), preserved)
        assertEquals(TextPrimary, replaced)
        assertTrue(calculateContrastRatio(TextPrimary, Color.White) >= 4.5f)
    }
}
