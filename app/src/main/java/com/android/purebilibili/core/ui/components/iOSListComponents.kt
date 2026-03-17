package com.android.purebilibili.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.graphics.SolidColor
import com.android.purebilibili.core.theme.LocalCornerRadiusScale
import com.android.purebilibili.core.theme.LocalUiPreset
import com.android.purebilibili.core.theme.UiPreset
import com.android.purebilibili.core.theme.iOSCornerRadius
import com.android.purebilibili.core.ui.common.copyOnLongPress
import io.github.alexzhirkevich.cupertino.CupertinoSwitch
import io.github.alexzhirkevich.cupertino.CupertinoSwitchDefaults
import io.github.alexzhirkevich.cupertino.icons.CupertinoIcons
import io.github.alexzhirkevich.cupertino.icons.filled.*
import io.github.alexzhirkevich.cupertino.icons.outlined.*

// ═══════════════════════════════════════════════════
//  Common iOS List Components (Reused across Settings, Profile, etc.)
// ═══════════════════════════════════════════════════

internal data class AdaptiveListComponentVisualSpec(
    val sectionStartPaddingDp: Int,
    val groupCornerRadiusDp: Int,
    val groupTonalElevationDp: Int,
    val iconCornerRadiusDp: Int,
    val iconContainerSizeDp: Int,
    val iconGlyphSizeDp: Int,
    val iconBackgroundAlpha: Float,
    val gridCornerRadiusDp: Int,
    val searchBarCornerRadiusDp: Int,
    val searchBarHeightDp: Int,
    val dividerThicknessDp: Float,
    val dividerStartIndentDp: Int
)

internal fun resolveAdaptiveListComponentVisualSpec(
    uiPreset: UiPreset
): AdaptiveListComponentVisualSpec {
    return if (uiPreset == UiPreset.MD3) {
        AdaptiveListComponentVisualSpec(
            sectionStartPaddingDp = 20,
            groupCornerRadiusDp = 28,
            groupTonalElevationDp = 3,
            iconCornerRadiusDp = 12,
            iconContainerSizeDp = 40,
            iconGlyphSizeDp = 22,
            iconBackgroundAlpha = 0.18f,
            gridCornerRadiusDp = 24,
            searchBarCornerRadiusDp = 28,
            searchBarHeightDp = 48,
            dividerThicknessDp = 1f,
            dividerStartIndentDp = 16
        )
    } else {
        AdaptiveListComponentVisualSpec(
            sectionStartPaddingDp = 32,
            groupCornerRadiusDp = 20,
            groupTonalElevationDp = 1,
            iconCornerRadiusDp = 10,
            iconContainerSizeDp = 36,
            iconGlyphSizeDp = 20,
            iconBackgroundAlpha = 0.12f,
            gridCornerRadiusDp = 20,
            searchBarCornerRadiusDp = 10,
            searchBarHeightDp = 40,
            dividerThicknessDp = 0.5f,
            dividerStartIndentDp = 66
        )
    }
}

@Composable
fun AppAdaptiveSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val uiPreset = LocalUiPreset.current
    if (uiPreset == UiPreset.MD3) {
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled,
            modifier = modifier
        )
    } else {
        val primaryColor = MaterialTheme.colorScheme.primary
        CupertinoSwitch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled,
            modifier = modifier,
            colors = CupertinoSwitchDefaults.colors(
                thumbColor = Color.White,
                checkedTrackColor = primaryColor,
                uncheckedTrackColor = Color(0xFFE9E9EA)
            )
        )
    }
}

@Composable
fun IOSSectionTitle(title: String) {
    val uiPreset = LocalUiPreset.current
    val visualSpec = remember(uiPreset) { resolveAdaptiveListComponentVisualSpec(uiPreset) }
    Text(
        text = if (uiPreset == UiPreset.MD3) title else title.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        letterSpacing = if (uiPreset == UiPreset.MD3) 0.sp else 0.5.sp,
        modifier = Modifier.padding(
            start = visualSpec.sectionStartPaddingDp.dp,
            top = 24.dp,
            bottom = 8.dp
        )
    )
}

@Composable
fun IOSGroup(
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surface,
    shape: androidx.compose.ui.graphics.Shape? = null,
    border: androidx.compose.foundation.BorderStroke? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val uiPreset = LocalUiPreset.current
    val cornerRadiusScale = LocalCornerRadiusScale.current
    val visualSpec = remember(uiPreset) { resolveAdaptiveListComponentVisualSpec(uiPreset) }
    val groupCornerRadius = iOSCornerRadius.Medium * cornerRadiusScale
    val appliedShape = shape ?: RoundedCornerShape(
        if (uiPreset == UiPreset.MD3) visualSpec.groupCornerRadiusDp.dp else groupCornerRadius
    )
    
    Surface(
        modifier = modifier
            .padding(horizontal = 16.dp)
            .clip(appliedShape),
        color = containerColor,
        shadowElevation = if (uiPreset == UiPreset.MD3) 0.dp else 0.dp,
        tonalElevation = visualSpec.groupTonalElevationDp.dp,
        border = border
    ) {
        Column(content = content)
    }
}

@Composable
fun IOSSwitchItem(
    icon: ImageVector? = null,
    title: String,
    subtitle: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true,
    iconTint: Color = MaterialTheme.colorScheme.primary,
    textColor: Color = MaterialTheme.colorScheme.onSurface,
    subtitleColor: Color = MaterialTheme.colorScheme.onSurfaceVariant
) {
    val uiPreset = LocalUiPreset.current
    val visualSpec = remember(uiPreset) { resolveAdaptiveListComponentVisualSpec(uiPreset) }
    val cornerRadiusScale = LocalCornerRadiusScale.current
    val iconCornerRadius = if (uiPreset == UiPreset.MD3) visualSpec.iconCornerRadiusDp.dp else iOSCornerRadius.Small * cornerRadiusScale
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (enabled) 1f else 0.6f)
            .clickable(enabled = enabled) { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Box(
                modifier = Modifier
                    .size(visualSpec.iconContainerSizeDp.dp)
                    .clip(RoundedCornerShape(iconCornerRadius))
                    .background(iconTint.copy(alpha = visualSpec.iconBackgroundAlpha)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(visualSpec.iconGlyphSizeDp.dp)
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge, color = textColor)
            if (subtitle != null) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = subtitleColor)
            }
        }
        AppAdaptiveSwitch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled
        )
    }
}

@Composable
fun IOSClickableItem(
    icon: ImageVector? = null,
    iconPainter: androidx.compose.ui.graphics.painter.Painter? = null,
    title: String,
    subtitle: String? = null,
    value: String? = null,
    onClick: (() -> Unit)? = null,
    iconTint: Color = MaterialTheme.colorScheme.primary,
    textColor: Color = MaterialTheme.colorScheme.onSurface,
    subtitleColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    valueColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    chevronTint: Color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f),
    centered: Boolean = false,
    enableCopy: Boolean = false,
    showChevron: Boolean = true
) {
    val uiPreset = LocalUiPreset.current
    val visualSpec = remember(uiPreset) { resolveAdaptiveListComponentVisualSpec(uiPreset) }
    val cornerRadiusScale = LocalCornerRadiusScale.current
    val iconCornerRadius = if (uiPreset == UiPreset.MD3) visualSpec.iconCornerRadiusDp.dp else iOSCornerRadius.Small * cornerRadiusScale
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = onClick != null) { onClick?.invoke() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = if (centered) Arrangement.Center else Arrangement.Start
    ) {
        if (!centered) {
            if (icon != null || iconPainter != null) {
                if (iconTint != Color.Unspecified) {
                    Box(
                        modifier = Modifier
                            .size(visualSpec.iconContainerSizeDp.dp)
                            .clip(RoundedCornerShape(iconCornerRadius))
                            .background(iconTint.copy(alpha = visualSpec.iconBackgroundAlpha)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (icon != null) {
                            Icon(
                                icon,
                                contentDescription = null,
                                tint = iconTint,
                                modifier = Modifier.size(visualSpec.iconGlyphSizeDp.dp)
                            )
                        } else if (iconPainter != null) {
                            Icon(
                                painter = iconPainter,
                                contentDescription = null,
                                tint = iconTint,
                                modifier = Modifier.size(visualSpec.iconGlyphSizeDp.dp)
                            )
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier.size(visualSpec.iconContainerSizeDp.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (icon != null) {
                            Icon(
                                icon,
                                contentDescription = null,
                                tint = Color.Unspecified,
                                modifier = Modifier.size(visualSpec.iconContainerSizeDp.dp)
                            )
                        } else if (iconPainter != null) {
                            Icon(
                                painter = iconPainter,
                                contentDescription = null,
                                tint = Color.Unspecified,
                                modifier = Modifier.size(visualSpec.iconContainerSizeDp.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.width(14.dp))
            }
        }
        
        if (centered) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = textColor,
                modifier = Modifier,
                maxLines = 1,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        } else {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = textColor,
                    maxLines = if (subtitle != null) 2 else 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Start
                )
                if (subtitle != null) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = subtitleColor,
                        maxLines = 2,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                }
            }
        }
        
        if (!centered) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (value != null) {
                    Text(
                        text = value, 
                        style = MaterialTheme.typography.bodyMedium, 
                        color = valueColor,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                        modifier = if (enableCopy) Modifier.copyOnLongPress(value, title) else Modifier
                    )
                }
                if (onClick != null && showChevron) {
                    Spacer(modifier = Modifier.width(6.dp))
                    if (uiPreset == UiPreset.MD3) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                            contentDescription = null,
                            tint = chevronTint,
                            modifier = Modifier.size(16.dp)
                        )
                    } else {
                        Icon(CupertinoIcons.Default.ChevronForward, null, tint = chevronTint, modifier = Modifier.size(20.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun IOSDivider(startIndent: androidx.compose.ui.unit.Dp = 66.dp) {
    val uiPreset = LocalUiPreset.current
    val visualSpec = remember(uiPreset) { resolveAdaptiveListComponentVisualSpec(uiPreset) }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = if (uiPreset == UiPreset.MD3) visualSpec.dividerStartIndentDp.dp else startIndent)
            .height(visualSpec.dividerThicknessDp.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)) // Subtle separator
    )
}


@Composable
fun IOSGridItem(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit,
    iconTint: Color = MaterialTheme.colorScheme.primary,
    containerColor: Color = MaterialTheme.colorScheme.surface,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    modifier: Modifier = Modifier
) {
    val uiPreset = LocalUiPreset.current
    val visualSpec = remember(uiPreset) { resolveAdaptiveListComponentVisualSpec(uiPreset) }
    val cornerRadiusScale = LocalCornerRadiusScale.current
    val itemCornerRadius = if (uiPreset == UiPreset.MD3) visualSpec.gridCornerRadiusDp.dp else iOSCornerRadius.Medium * cornerRadiusScale

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(itemCornerRadius))
            .background(containerColor)
            .clickable(onClick = onClick)
            .padding(vertical = 24.dp, horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(iOSCornerRadius.Small * cornerRadiusScale))
                .background(iconTint.copy(alpha = visualSpec.iconBackgroundAlpha)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(26.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = contentColor,
            maxLines = 1,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
        )
    }
}

@Composable
fun IOSSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "搜索",
    containerColor: Color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
) {
    val uiPreset = LocalUiPreset.current
    val visualSpec = remember(uiPreset) { resolveAdaptiveListComponentVisualSpec(uiPreset) }
    val cornerRadiusScale = LocalCornerRadiusScale.current
    val searchBarCornerRadius = if (uiPreset == UiPreset.MD3) visualSpec.searchBarCornerRadiusDp.dp else iOSCornerRadius.Small * cornerRadiusScale

    BasicTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier
            .fillMaxWidth()
            .height(visualSpec.searchBarHeightDp.dp)
            .clip(RoundedCornerShape(searchBarCornerRadius))
            .background(containerColor),
        textStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
        singleLine = true,
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        decorationBox = { innerTextField ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 12.dp)
            ) {
                if (uiPreset == UiPreset.MD3) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                } else {
                    Icon(
                        imageVector = CupertinoIcons.Default.MagnifyingGlass,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Box(contentAlignment = Alignment.CenterStart, modifier = Modifier.weight(1f)) {
                    if (query.isEmpty()) {
                        Text(
                            text = placeholder,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    innerTextField()
                }
                if (query.isNotEmpty()) {
                    IconButton(
                        onClick = { onQueryChange("") },
                        modifier = Modifier.size(20.dp)
                    ) {
                        val clearIcon = if (uiPreset == UiPreset.MD3) {
                            Icons.Default.Clear
                        } else {
                            CupertinoIcons.Default.XmarkCircle
                        }
                        Icon(
                            imageVector = clearIcon,
                            contentDescription = "Clear",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    )
}
