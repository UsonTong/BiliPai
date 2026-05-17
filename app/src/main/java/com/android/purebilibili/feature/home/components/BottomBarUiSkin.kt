package com.android.purebilibili.feature.home.components

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.clearAndSetSemantics
import com.android.purebilibili.core.plugin.skin.UiSkinState

data class BottomBarUiSkinDecoration(
    val skinId: String,
    val bottomTrimTint: Color,
    val bottomTrimAccent: Color
)

@Composable
fun rememberBottomBarUiSkinDecoration(uiSkinState: UiSkinState): BottomBarUiSkinDecoration? {
    val activeSkin = uiSkinState.activeSkin
    return remember(uiSkinState.enabled, activeSkin?.manifest) {
        if (!uiSkinState.enabled || activeSkin == null) {
            null
        } else {
            BottomBarUiSkinDecoration(
                skinId = activeSkin.manifest.skinId,
                bottomTrimTint = parseUiSkinColor(
                    value = activeSkin.manifest.colors.bottomBarTrimTint,
                    fallback = Color(0xFFEAF8FF)
                ),
                bottomTrimAccent = parseUiSkinColor(
                    value = activeSkin.manifest.colors.topAtmosphereTint,
                    fallback = Color(0xFFDFF5FF)
                )
            )
        }
    }
}

@Composable
internal fun BottomBarSkinDecorativeTrim(
    decoration: BottomBarUiSkinDecoration?,
    modifier: Modifier = Modifier
) {
    if (decoration == null) return
    Box(
        modifier = modifier
            .clearAndSetSemantics {}
            .drawBehind {
                val trimHeight = size.height * 0.36f
                val top = size.height - trimHeight
                drawRoundRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            decoration.bottomTrimTint.copy(alpha = 0.08f),
                            decoration.bottomTrimTint.copy(alpha = 0.28f)
                        ),
                        startY = top,
                        endY = size.height
                    ),
                    topLeft = Offset(0f, top),
                    size = Size(size.width, trimHeight),
                    cornerRadius = CornerRadius(trimHeight, trimHeight)
                )

                val cloudRadius = trimHeight * 0.38f
                val centers = listOf(0.12f, 0.26f, 0.44f, 0.62f, 0.78f, 0.91f)
                centers.forEachIndexed { index, fraction ->
                    val y = top + trimHeight * if (index % 2 == 0) 0.46f else 0.58f
                    drawCircle(
                        color = decoration.bottomTrimAccent.copy(alpha = 0.18f),
                        radius = cloudRadius * if (index % 2 == 0) 1.0f else 0.78f,
                        center = Offset(size.width * fraction, y)
                    )
                }
            }
    )
}

private fun parseUiSkinColor(
    value: String?,
    fallback: Color
): Color {
    val normalized = value
        ?.trim()
        ?.removePrefix("#")
        ?.takeIf { it.length == 6 || it.length == 8 }
        ?: return fallback
    val argb = if (normalized.length == 6) "FF$normalized" else normalized
    return runCatching { Color(argb.toLong(16)) }.getOrDefault(fallback)
}
