package com.android.purebilibili.feature.home.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.android.purebilibili.core.plugin.skin.UiSkinState
import com.android.purebilibili.core.plugin.skin.UiSkinSurface
import java.io.File

data class BottomBarUiSkinDecoration(
    val skinId: String,
    val bottomTrimTint: Color,
    val bottomTrimAccent: Color,
    val bottomTrimImagePath: String? = null,
    val bottomBarIconPaths: Map<BottomNavItem, BottomBarSkinIconPaths> = emptyMap()
) {
    fun iconPathFor(item: BottomNavItem, selected: Boolean = false): String? {
        val paths = bottomBarIconPaths[item] ?: return null
        return if (selected) {
            paths.selected ?: paths.unselected
        } else {
            paths.unselected
        }
    }
}

data class BottomBarSkinIconPaths(
    val unselected: String,
    val selected: String? = null
)

data class HomeUiSkinDecoration(
    val skinId: String,
    val topAtmosphereTint: Color,
    val searchCapsuleTint: Color,
    val topAtmosphereImagePath: String? = null
)

internal fun resolveBottomBarSkinDockIconSize(): Dp = 36.dp

internal fun resolveBottomBarSkinDockHeight(): Dp = 88.dp

internal fun resolveBottomBarSkinDockContentPadding(): PaddingValues = PaddingValues(
    start = 4.dp,
    end = 4.dp,
    top = 0.dp,
    bottom = 0.dp
)

internal fun resolveBottomBarSkinIconLabelGap(): Dp = 2.dp

internal fun resolveBottomBarSkinDockIconTopPadding(): Dp = 10.dp

internal fun resolveBottomBarSkinDockLabelBottomPadding(): Dp = 18.dp

internal fun resolveBottomBarSkinDockLabelFontSize(): TextUnit = 12.sp

internal fun resolveBottomBarSkinDockLabelLineHeight(): TextUnit = 18.sp

internal fun resolveBottomBarMiuixSkinDockIconSize(): Dp = 34.dp

internal fun resolveBottomBarCompactSkinHomeIconSize(): Dp = 36.dp

internal fun resolveMiuixDockedBottomBarItemHeight(hasUiSkinDecoration: Boolean): Dp {
    return if (hasUiSkinDecoration) {
        resolveBottomBarSkinDockHeight()
    } else {
        64.dp
    }
}

@Composable
fun rememberBottomBarUiSkinDecoration(uiSkinState: UiSkinState): BottomBarUiSkinDecoration? {
    return remember(uiSkinState) {
        resolveBottomBarUiSkinDecoration(uiSkinState)
    }
}

@Composable
fun rememberHomeUiSkinDecoration(uiSkinState: UiSkinState): HomeUiSkinDecoration? {
    return remember(uiSkinState) {
        resolveHomeUiSkinDecoration(uiSkinState)
    }
}

fun resolveBottomBarUiSkinDecoration(uiSkinState: UiSkinState): BottomBarUiSkinDecoration? {
    val activeSkin = uiSkinState.activeSkin
    return if (!uiSkinState.enabled || activeSkin == null) {
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
            ),
            bottomTrimImagePath = activeSkin.assetFilePath(activeSkin.manifest.assets.bottomBarTrim),
            bottomBarIconPaths = resolveBottomBarSkinIconPaths(activeSkin)
        )
    }
}

fun resolveHomeUiSkinDecoration(uiSkinState: UiSkinState): HomeUiSkinDecoration? {
    val activeSkin = uiSkinState.activeSkin
    return if (!uiSkinState.enabled || activeSkin == null) {
        null
    } else {
        val manifest = activeSkin.manifest
        val hasTopDecoration = UiSkinSurface.HOME_TOP_CHROME in manifest.surfaces &&
            (
                manifest.assets.topAtmosphere != null ||
                    manifest.colors.topAtmosphereTint != null ||
                    manifest.colors.searchCapsuleTint != null
                )
        if (!hasTopDecoration) return null
        HomeUiSkinDecoration(
            skinId = manifest.skinId,
            topAtmosphereTint = parseUiSkinColor(
                value = manifest.colors.topAtmosphereTint,
                fallback = Color(0xFFDFF5FF)
            ),
            searchCapsuleTint = parseUiSkinColor(
                value = manifest.colors.searchCapsuleTint,
                fallback = Color.White
            ),
            topAtmosphereImagePath = activeSkin.assetFilePath(manifest.assets.topAtmosphere)
        )
    }
}

@Composable
internal fun HomeSkinAtmosphere(
    decoration: HomeUiSkinDecoration?,
    statusBarHeight: Dp = 0.dp,
    modifier: Modifier = Modifier
) {
    if (decoration == null) return
    Box(
        modifier = modifier
            .fillMaxSize()
            .clearAndSetSemantics {}
            .drawBehind {
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            decoration.topAtmosphereTint.copy(alpha = 0.30f),
                            decoration.searchCapsuleTint.copy(alpha = 0.12f),
                            Color.Transparent
                        ),
                        startY = 0f,
                        endY = size.height * 0.42f
                    )
                )
            }
    ) {
        val imagePath = decoration.topAtmosphereImagePath
        if (!imagePath.isNullOrBlank()) {
            AsyncImage(
                model = File(imagePath),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(190.dp + statusBarHeight)
                    .alpha(0.44f)
                    .clearAndSetSemantics {}
            )
        }
    }
}

@Composable
internal fun BottomBarSkinIcon(
    iconPath: String,
    contentDescription: String?,
    size: Dp = resolveBottomBarSkinDockIconSize(),
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.size(size)) {
        AsyncImage(
            model = File(iconPath),
            contentDescription = contentDescription,
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize()
        )
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
    ) {
        val imagePath = decoration.bottomTrimImagePath
        if (!imagePath.isNullOrBlank()) {
            AsyncImage(
                model = File(imagePath),
                contentDescription = null,
                contentScale = ContentScale.FillBounds,
                modifier = Modifier
                    .matchParentSize()
                    .alpha(0.82f)
                    .clearAndSetSemantics {}
            )
        }
    }
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

private fun resolveBottomBarSkinIconPaths(
    activeSkin: com.android.purebilibili.core.plugin.skin.InstalledUiSkinPackage
): Map<BottomNavItem, BottomBarSkinIconPaths> {
    val manifestIcons = activeSkin.manifest.assets.bottomBarIcons
    return buildMap {
        mapOf(
            "home" to ("home_selected" to BottomNavItem.HOME),
            "following" to ("following_selected" to BottomNavItem.DYNAMIC),
            "member" to ("member_selected" to BottomNavItem.HISTORY),
            "profile" to ("profile_selected" to BottomNavItem.PROFILE)
        ).forEach { (unselectedKey, selectedKeyAndItem) ->
            val (selectedKey, item) = selectedKeyAndItem
            activeSkin.assetFilePath(manifestIcons[unselectedKey])?.let { unselectedPath ->
                put(
                    item,
                    BottomBarSkinIconPaths(
                        unselected = unselectedPath,
                        selected = activeSkin.assetFilePath(manifestIcons[selectedKey])
                    )
                )
            }
        }
    }
}
