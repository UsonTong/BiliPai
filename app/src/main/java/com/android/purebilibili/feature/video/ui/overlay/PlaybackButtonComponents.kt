package com.android.purebilibili.feature.video.ui.overlay

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
internal fun OverlayPlaybackButton(
    isPlaying: Boolean,
    onClick: () -> Unit,
    outerSize: Dp,
    innerSize: Dp,
    glyphSize: Dp,
    modifier: Modifier = Modifier
) {
    val isDarkTheme = resolveOverlayPlaybackIsDarkTheme()
    val style = resolveCenterPlaybackButtonStyle(isDarkTheme = isDarkTheme)

    Surface(
        onClick = onClick,
        color = style.containerColor,
        shape = CircleShape,
        border = BorderStroke(1.dp, style.borderColor),
        modifier = modifier.size(outerSize)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(innerSize)
                    .background(style.innerColor, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                PlaybackGlyph(
                    isPlaying = isPlaying,
                    tint = style.iconTint,
                    modifier = Modifier.size(glyphSize)
                )
            }
        }
    }
}

@Composable
private fun resolveOverlayPlaybackIsDarkTheme(): Boolean {
    return LocalConfiguration.current.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK ==
        android.content.res.Configuration.UI_MODE_NIGHT_YES
}

@Composable
internal fun PlaybackGlyph(
    isPlaying: Boolean,
    tint: Color,
    modifier: Modifier = Modifier
) {
    val horizontalBias = resolvePlaybackGlyphHorizontalBias(isPlaying = isPlaying)
    Canvas(modifier = modifier) {
        if (isPlaying) {
            val barWidth = size.width * 0.20f
            val gap = size.width * 0.16f
            val barHeight = size.height * 0.66f
            val top = (size.height - barHeight) / 2f
            val totalWidth = barWidth * 2f + gap
            val left = (size.width - totalWidth) / 2f
            val radius = CornerRadius(barWidth * 0.18f, barWidth * 0.18f)
            drawRoundRect(
                color = tint,
                topLeft = Offset(left, top),
                size = Size(barWidth, barHeight),
                cornerRadius = radius
            )
            drawRoundRect(
                color = tint,
                topLeft = Offset(left + barWidth + gap, top),
                size = Size(barWidth, barHeight),
                cornerRadius = radius
            )
        } else {
            val triangleWidth = size.width * 0.54f
            val triangleHeight = size.height * 0.62f
            val left = (size.width - triangleWidth) / 2f + size.width * horizontalBias
            val top = (size.height - triangleHeight) / 2f
            val centerY = size.height / 2f
            val path = Path().apply {
                moveTo(left, top)
                lineTo(left, top + triangleHeight)
                lineTo(left + triangleWidth, centerY)
                close()
            }
            drawPath(path = path, color = tint)
        }
    }
}

internal fun resolvePlaybackGlyphHorizontalBias(
    isPlaying: Boolean
): Float = if (isPlaying) 0f else 0.04f
