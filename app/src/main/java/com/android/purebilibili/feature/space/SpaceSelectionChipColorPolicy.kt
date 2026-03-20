package com.android.purebilibili.feature.space

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import com.android.purebilibili.core.theme.resolveAdaptivePrimaryAccentColors

internal data class SpaceSelectionChipColors(
    val backgroundColor: Color,
    val textColor: Color
)

internal fun resolveSpaceSelectionChipColors(
    isSelected: Boolean,
    colorScheme: ColorScheme,
    unselectedAlpha: Float = 0.5f
): SpaceSelectionChipColors {
    if (!isSelected) {
        return SpaceSelectionChipColors(
            backgroundColor = colorScheme.surfaceVariant.copy(alpha = unselectedAlpha),
            textColor = colorScheme.onSurfaceVariant
        )
    }

    val selectedColors = resolveAdaptivePrimaryAccentColors(colorScheme)

    return SpaceSelectionChipColors(
        backgroundColor = selectedColors.backgroundColor,
        textColor = selectedColors.contentColor
    )
}
