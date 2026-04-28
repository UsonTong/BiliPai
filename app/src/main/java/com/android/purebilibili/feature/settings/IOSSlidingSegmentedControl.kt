package com.android.purebilibili.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.purebilibili.core.theme.LocalUiPreset
import com.android.purebilibili.core.theme.LocalAndroidNativeVariant
import com.android.purebilibili.core.theme.AndroidNativeVariant
import com.android.purebilibili.core.theme.UiPreset
import com.android.purebilibili.feature.home.components.BottomBarLiquidSegmentedControl
import top.yukonga.miuix.kmp.theme.MiuixTheme

internal fun resolveMd3SegmentedLabelFontSizeSp(
    optionCount: Int,
    longestLabelLength: Int
): Float {
    return when {
        optionCount >= 5 -> 12f
        optionCount >= 4 && longestLabelLength >= 6 -> 13f
        optionCount >= 4 -> 14f
        longestLabelLength >= 8 -> 13f
        else -> 15f
    }
}

internal data class Md3SegmentedControlColorTokens(
    val outerContainerColor: Color,
    val activeContainerColor: Color,
    val activeContentColor: Color,
    val inactiveContentColor: Color
)

internal fun resolveMd3SegmentedControlColorTokens(
    androidNativeVariant: AndroidNativeVariant,
    materialPrimaryContainer: Color,
    materialOnPrimaryContainer: Color,
    materialSurfaceContainerHigh: Color,
    materialOnSurfaceVariant: Color,
    miuixSecondaryContainer: Color,
    miuixOnSecondaryContainer: Color,
    miuixSurfaceContainerHigh: Color,
    miuixOnSurfaceVariantSummary: Color
): Md3SegmentedControlColorTokens {
    return if (androidNativeVariant == AndroidNativeVariant.MATERIAL3) {
        Md3SegmentedControlColorTokens(
            outerContainerColor = materialSurfaceContainerHigh,
            activeContainerColor = materialPrimaryContainer,
            activeContentColor = materialOnPrimaryContainer,
            inactiveContentColor = materialOnSurfaceVariant
        )
    } else {
        Md3SegmentedControlColorTokens(
            outerContainerColor = miuixSurfaceContainerHigh,
            activeContainerColor = miuixSecondaryContainer,
            activeContentColor = miuixOnSecondaryContainer,
            inactiveContentColor = miuixOnSurfaceVariantSummary
        )
    }
}

@Composable
internal fun <T> IOSSlidingSegmentedSetting(
    title: String,
    options: List<PlaybackSegmentOption<T>>,
    selectedValue: T,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    enabled: Boolean = true,
    onSelectionChange: (T) -> Unit
) {
    val uiPreset = LocalUiPreset.current
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = title,
            style = if (uiPreset == UiPreset.MD3) {
                MaterialTheme.typography.titleMedium
            } else {
                MaterialTheme.typography.bodyLarge
            },
            color = if (uiPreset == UiPreset.MD3) {
                MiuixTheme.colorScheme.onSurface
            } else {
                MaterialTheme.colorScheme.onSurface
            }
        )
        if (!subtitle.isNullOrBlank()) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = if (uiPreset == UiPreset.MD3) {
                    MiuixTheme.colorScheme.onSurfaceVariantSummary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        }
        IOSSlidingSegmentedControl(
            options = options,
            selectedValue = selectedValue,
            enabled = enabled,
            onSelectionChange = onSelectionChange
        )
    }
}

@Composable
internal fun <T> IOSSlidingSegmentedControl(
    options: List<PlaybackSegmentOption<T>>,
    selectedValue: T,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onSelectionChange: (T) -> Unit
) {
    if (options.isEmpty()) return
    val uiPreset = LocalUiPreset.current
    if (uiPreset == UiPreset.MD3) {
        Md3SegmentedControl(
            options = options,
            selectedValue = selectedValue,
            modifier = modifier,
            enabled = enabled,
            onSelectionChange = onSelectionChange
        )
        return
    }
    IOSSlidingSegmentedControlImpl(
        options = options,
        selectedValue = selectedValue,
        modifier = modifier,
        enabled = enabled,
        onSelectionChange = onSelectionChange
    )
}

@Composable
private fun <T> Md3SegmentedControl(
    options: List<PlaybackSegmentOption<T>>,
    selectedValue: T,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onSelectionChange: (T) -> Unit
) {
    val longestLabelLength = remember(options) {
        options.maxOfOrNull { it.label.length } ?: 0
    }
    val androidNativeVariant = LocalAndroidNativeVariant.current
    val materialColorScheme = MaterialTheme.colorScheme
    val miuixColorScheme = MiuixTheme.colorScheme
    val colorTokens = resolveMd3SegmentedControlColorTokens(
        androidNativeVariant = androidNativeVariant,
        materialPrimaryContainer = materialColorScheme.primaryContainer,
        materialOnPrimaryContainer = materialColorScheme.onPrimaryContainer,
        materialSurfaceContainerHigh = materialColorScheme.surfaceContainerHigh,
        materialOnSurfaceVariant = materialColorScheme.onSurfaceVariant,
        miuixSecondaryContainer = miuixColorScheme.secondaryContainer,
        miuixOnSecondaryContainer = miuixColorScheme.onSecondaryContainer,
        miuixSurfaceContainerHigh = miuixColorScheme.surfaceContainerHigh,
        miuixOnSurfaceVariantSummary = miuixColorScheme.onSurfaceVariantSummary
    )
    val labelFontSize = remember(options.size, longestLabelLength) {
        resolveMd3SegmentedLabelFontSizeSp(
            optionCount = options.size,
            longestLabelLength = longestLabelLength
        ).sp
    }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(colorTokens.outerContainerColor)
            .padding(4.dp)
    ) {
        SingleChoiceSegmentedButtonRow(
            modifier = Modifier.fillMaxWidth()
        ) {
            options.forEachIndexed { index, option ->
                SegmentedButton(
                    selected = option.value == selectedValue,
                    onClick = { onSelectionChange(option.value) },
                    enabled = enabled,
                    shape = SegmentedButtonDefaults.itemShape(
                        index = index,
                        count = options.size
                    ),
                    colors = SegmentedButtonDefaults.colors(
                        activeContainerColor = colorTokens.activeContainerColor,
                        activeContentColor = colorTokens.activeContentColor,
                        inactiveContainerColor = Color.Transparent,
                        inactiveContentColor = colorTokens.inactiveContentColor,
                        disabledActiveContainerColor = colorTokens.activeContainerColor.copy(alpha = 0.35f),
                        disabledActiveContentColor = colorTokens.activeContentColor.copy(alpha = 0.55f),
                        disabledInactiveContainerColor = Color.Transparent,
                        disabledInactiveContentColor = colorTokens.inactiveContentColor.copy(alpha = 0.45f)
                    ),
                    modifier = Modifier.weight(1f),
                    icon = {}
                ) {
                    Text(
                        text = option.label,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.labelLarge.copy(fontSize = labelFontSize),
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun <T> IOSSlidingSegmentedControlImpl(
    options: List<PlaybackSegmentOption<T>>,
    selectedValue: T,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onSelectionChange: (T) -> Unit
) {
    val selectedIndex = resolveSelectionIndex(options = options, selectedValue = selectedValue)
    BottomBarLiquidSegmentedControl(
        items = options.map { it.label },
        selectedIndex = selectedIndex,
        onSelected = { index ->
            options.getOrNull(index)?.let { option ->
                onSelectionChange(option.value)
            }
        },
        modifier = modifier.fillMaxWidth(),
        enabled = enabled,
        height = 46.dp,
        indicatorHeight = 40.dp,
        labelFontSize = 12.sp
    )
}
