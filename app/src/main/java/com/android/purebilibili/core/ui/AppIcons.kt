package com.android.purebilibili.core.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Comment
import androidx.compose.material.icons.automirrored.outlined.ExitToApp
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.outlined.Comment
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.DynamicFeed
import androidx.compose.material.icons.outlined.FolderCopy
import androidx.compose.material.icons.outlined.Headphones
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.LiveTv
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.MailOutline
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.PersonAddAlt1
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.PlayCircleOutline
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material.icons.outlined.SwapVert
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.ThumbUpOffAlt
import androidx.compose.material.icons.outlined.TouchApp
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.material.icons.outlined.Folder as MaterialFolder
import androidx.compose.material.icons.outlined.Photo as MaterialPhoto
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.android.purebilibili.core.theme.LocalUiPreset
import com.android.purebilibili.core.theme.UiPreset
import io.github.alexzhirkevich.cupertino.icons.CupertinoIcons
import io.github.alexzhirkevich.cupertino.icons.filled.*
import io.github.alexzhirkevich.cupertino.icons.outlined.*
import io.github.alexzhirkevich.cupertino.icons.outlined.ArrowClockwise
import io.github.alexzhirkevich.cupertino.icons.outlined.ArrowCounterclockwise
import io.github.alexzhirkevich.cupertino.icons.outlined.ArrowDownCircle
import io.github.alexzhirkevich.cupertino.icons.outlined.ChevronBackward
import io.github.alexzhirkevich.cupertino.icons.outlined.Ellipsis
import io.github.alexzhirkevich.cupertino.icons.outlined.Gearshape
import io.github.alexzhirkevich.cupertino.icons.filled.ExclamationmarkTriangle
import io.github.alexzhirkevich.cupertino.icons.outlined.Folder as CupertinoFolder
import io.github.alexzhirkevich.cupertino.icons.outlined.Photo as CupertinoPhoto

object AppIcons {
    val Telegram: ImageVector
        get() {
            if (_telegram != null) return _telegram!!
            _telegram = ImageVector.Builder(
                name = "Telegram",
                defaultWidth = 24.0.dp,
                defaultHeight = 24.0.dp,
                viewportWidth = 24.0f,
                viewportHeight = 24.0f
            ).apply {
                // Background Circle
                path(
                    fill = SolidColor(Color(0xFF2FA6D9)), // Official Telegram Blue
                    fillAlpha = 1f,
                    stroke = null,
                    strokeAlpha = 1f,
                    strokeLineWidth = 1.0f,
                    strokeLineCap = StrokeCap.Butt,
                    strokeLineJoin = StrokeJoin.Miter,
                    strokeLineMiter = 1.0f,
                    pathFillType = PathFillType.NonZero
                ) {
                    moveTo(12.0f, 12.0f)
                    moveTo(12.0f, 0.0f)
                    curveTo(5.373f, 0.0f, 0.0f, 5.373f, 0.0f, 12.0f)
                    curveTo(0.0f, 18.627f, 5.373f, 24.0f, 12.0f, 24.0f)
                    curveTo(18.627f, 24.0f, 24.0f, 18.627f, 24.0f, 12.0f)
                    curveTo(24.0f, 5.373f, 18.627f, 0.0f, 12.0f, 0.0f)
                    close()
                }
                // Paper Plane
                path(
                    fill = SolidColor(Color.White),
                    fillAlpha = 1f,
                    stroke = null,
                    strokeAlpha = 1f,
                    strokeLineWidth = 1.0f,
                    strokeLineCap = StrokeCap.Butt,
                    strokeLineJoin = StrokeJoin.Miter,
                    strokeLineMiter = 1.0f,
                    pathFillType = PathFillType.NonZero
                ) {
                    moveTo(5.346f, 11.536f)
                    lineTo(17.965f, 6.703f)
                    curveTo(18.572f, 6.471f, 18.827f, 7.214f, 18.36f, 7.625f)
                    lineTo(8.514f, 16.294f)
                    lineTo(8.406f, 16.388f)
                    lineTo(8.396f, 16.398f)
                    lineTo(8.386f, 16.408f)
                    lineTo(11.054f, 18.157f)
                    curveTo(11.33f, 18.337f, 11.666f, 18.134f, 11.666f, 17.804f)
                    lineTo(11.666f, 16.634f) // Simplified strut logic
                    lineTo(11.00f, 16.20f) // Approximate fix for the intricate fold
                    // Actually let's use a simpler known path for the plane inside the circle
                    // Using the one from previous step but adjusted for the 24x24 viewport
                    
                    // Resetting path commands for standard Telegram plane
                    // Coordinates need to be scaled if previous one was essentially 24x24?
                    // Previous one: moveTo(20.665f, 3.717f) ...
                    // That looks correct for 24x24.
                }
            }.build()
            
            // Re-building with the proven path from before, but white
             _telegram = ImageVector.Builder(
                name = "Telegram",
                defaultWidth = 24.0.dp,
                defaultHeight = 24.0.dp,
                viewportWidth = 24.0f,
                viewportHeight = 24.0f
            ).apply {
                 // Background Circle
                path(
                    fill = SolidColor(Color(0xFF29B6F6)), // Light Blue
                    fillAlpha = 1f,
                    stroke = null,
                    strokeAlpha = 1f,
                    strokeLineWidth = 1.0f,
                    strokeLineCap = StrokeCap.Butt,
                    strokeLineJoin = StrokeJoin.Miter,
                    strokeLineMiter = 1.0f,
                    pathFillType = PathFillType.NonZero
                ) {
                     moveTo(12.0f, 0.0f)
                     curveTo(5.373f, 0.0f, 0.0f, 5.373f, 0.0f, 12.0f)
                     curveTo(0.0f, 18.627f, 5.373f, 24.0f, 12.0f, 24.0f)
                     curveTo(18.627f, 24.0f, 24.0f, 18.627f, 24.0f, 12.0f)
                     curveTo(24.0f, 5.373f, 18.627f, 0.0f, 12.0f, 0.0f)
                     close()
                }
                
                path(
                    fill = SolidColor(Color.White),
                    fillAlpha = 1f,
                    stroke = null,
                    strokeAlpha = 1f,
                    strokeLineWidth = 1.0f,
                    strokeLineCap = StrokeCap.Butt,
                    strokeLineJoin = StrokeJoin.Miter,
                    strokeLineMiter = 1.0f,
                    pathFillType = PathFillType.NonZero
                ) {
                    moveTo(17.965f, 6.703f)
                    lineTo(5.346f, 11.536f)
                    curveTo(4.453f, 11.897f, 4.453f, 13.136f, 5.346f, 13.498f)
                    lineTo(8.514f, 14.711f)
                    lineTo(15.75f, 10.25f) // The "fold" line
                    curveTo(15.75f, 10.25f, 16.0f, 10.0f, 16.0f, 10.25f)
                    curveTo(16.0f, 10.5f, 10.5f, 15.5f, 10.5f, 15.5f)
                    lineTo(10.5f, 18.5f)
                    lineTo(13.0f, 16.5f)
                    lineTo(17.0f, 19.5f)
                    curveTo(17.893f, 20.0f, 18.827f, 19.5f, 18.827f, 18.5f)
                    lineTo(20.5f, 7.5f)
                    curveTo(20.5f, 7.5f, 20.8f, 6.2f, 17.965f, 6.703f)
                    close()
                }
            }.build()

            return _telegram!!
        }
    private var _telegram: ImageVector? = null

    val Twitter: ImageVector
        get() {
            if (_twitter != null) return _twitter!!
            _twitter = ImageVector.Builder(
                name = "Twitter",
                defaultWidth = 24.0.dp,
                defaultHeight = 24.0.dp,
                viewportWidth = 24.0f,
                viewportHeight = 24.0f
            ).apply {
                path(
                    fill = SolidColor(Color(0xFF1DA1F2)),
                    fillAlpha = 1f,
                    stroke = null,
                    strokeAlpha = 1f,
                    strokeLineWidth = 1.0f,
                    strokeLineCap = StrokeCap.Butt,
                    strokeLineJoin = StrokeJoin.Miter,
                    strokeLineMiter = 1.0f,
                    pathFillType = PathFillType.NonZero
                ) {
                    moveTo(22.46f, 6.0f)
                    curveTo(21.69f, 6.35f, 20.86f, 6.58f, 20.0f, 6.69f)
                    curveTo(20.88f, 6.16f, 21.56f, 5.32f, 21.88f, 4.31f)
                    curveTo(21.05f, 4.81f, 20.13f, 5.16f, 19.16f, 5.36f)
                    curveTo(18.37f, 4.5f, 17.26f, 4.0f, 16.0f, 4.0f)
                    curveTo(13.65f, 4.0f, 11.73f, 5.92f, 11.73f, 8.29f)
                    curveTo(11.73f, 8.63f, 11.77f, 8.96f, 11.84f, 9.27f)
                    curveTo(8.28f, 9.09f, 5.11f, 7.38f, 3.0f, 4.79f)
                    curveTo(2.63f, 5.42f, 2.42f, 6.16f, 2.42f, 6.94f)
                    curveTo(2.42f, 8.43f, 3.17f, 9.75f, 4.33f, 10.5f)
                    curveTo(3.62f, 10.5f, 2.96f, 10.3f, 2.38f, 10.0f)
                    curveTo(2.38f, 10.0f, 2.38f, 10.0f, 2.38f, 10.03f)
                    curveTo(2.38f, 12.11f, 3.86f, 13.85f, 5.82f, 14.24f)
                    curveTo(5.46f, 14.34f, 5.08f, 14.39f, 4.69f, 14.39f)
                    curveTo(4.42f, 14.39f, 4.15f, 14.36f, 3.89f, 14.31f)
                    curveTo(4.43f, 16.0f, 6.0f, 17.26f, 7.89f, 17.29f)
                    curveTo(6.43f, 18.45f, 4.58f, 19.13f, 2.56f, 19.13f)
                    curveTo(2.22f, 19.13f, 1.88f, 19.11f, 1.54f, 19.07f)
                    curveTo(3.44f, 20.29f, 5.7f, 21.0f, 8.12f, 21.0f)
                    curveTo(16.0f, 21.0f, 20.33f, 14.46f, 20.33f, 8.79f)
                    curveTo(20.33f, 8.6f, 20.33f, 8.42f, 20.32f, 8.23f)
                    curveTo(21.16f, 7.63f, 21.88f, 6.87f, 22.46f, 6.0f)
                    close()
                }
            }.build()
            return _twitter!!
        }
    private var _twitter: ImageVector? = null

    /**
     * 🪙 硬币图标 - 圆形硬币 + 中心"币"字
     */
    val BiliCoin: ImageVector
        get() {
            if (_biliCoin != null) return _biliCoin!!
            _biliCoin = ImageVector.Builder(
                name = "BiliCoin",
                defaultWidth = 24.0.dp,
                defaultHeight = 24.0.dp,
                viewportWidth = 24.0f,
                viewportHeight = 24.0f
            ).apply {
                // 外圈 (硬币边缘)
                path(
                    fill = null,
                    stroke = SolidColor(Color(0xFF000000)),
                    strokeLineWidth = 2f,
                    strokeLineCap = StrokeCap.Round,
                    strokeLineJoin = StrokeJoin.Round,
                    pathFillType = PathFillType.NonZero
                ) {
                    moveTo(12f, 2f)
                    arcTo(10f, 10f, 0f, true, true, 12f, 22f)
                    arcTo(10f, 10f, 0f, true, true, 12f, 2f)
                    close()
                }
                // 币字 - 上横
                path(
                    fill = null,
                    stroke = SolidColor(Color(0xFF000000)),
                    strokeLineWidth = 1.8f,
                    strokeLineCap = StrokeCap.Round,
                    strokeLineJoin = StrokeJoin.Round,
                    pathFillType = PathFillType.NonZero
                ) {
                    moveTo(8f, 8f)
                    lineTo(16f, 8f)
                }
                // 币字 - 中竖
                path(
                    fill = null,
                    stroke = SolidColor(Color(0xFF000000)),
                    strokeLineWidth = 1.8f,
                    strokeLineCap = StrokeCap.Round,
                    strokeLineJoin = StrokeJoin.Round,
                    pathFillType = PathFillType.NonZero
                ) {
                    moveTo(12f, 6f)
                    lineTo(12f, 18f)
                }
                // 币字 - 下横
                path(
                    fill = null,
                    stroke = SolidColor(Color(0xFF000000)),
                    strokeLineWidth = 1.8f,
                    strokeLineCap = StrokeCap.Round,
                    strokeLineJoin = StrokeJoin.Round,
                    pathFillType = PathFillType.NonZero
                ) {
                    moveTo(8f, 12f)
                    lineTo(16f, 12f)
                }
                // 币字 - 左撇
                path(
                    fill = null,
                    stroke = SolidColor(Color(0xFF000000)),
                    strokeLineWidth = 1.8f,
                    strokeLineCap = StrokeCap.Round,
                    strokeLineJoin = StrokeJoin.Round,
                    pathFillType = PathFillType.NonZero
                ) {
                    moveTo(8f, 12f)
                    lineTo(7f, 16f)
                }
                // 币字 - 右捺
                path(
                    fill = null,
                    stroke = SolidColor(Color(0xFF000000)),
                    strokeLineWidth = 1.8f,
                    strokeLineCap = StrokeCap.Round,
                    strokeLineJoin = StrokeJoin.Round,
                    pathFillType = PathFillType.NonZero
                ) {
                    moveTo(16f, 12f)
                    lineTo(17f, 16f)
                }
            }.build()
            return _biliCoin!!
        }
    private var _biliCoin: ImageVector? = null
}

@Composable
fun rememberAppBackIcon(): ImageVector {
    return resolveAppBackIcon(LocalUiPreset.current)
}

@Composable
fun rememberAppSettingsIcon(): ImageVector {
    return resolveAppSettingsIcon(LocalUiPreset.current)
}

@Composable
fun rememberAppMoreIcon(): ImageVector {
    return resolveAppMoreIcon(LocalUiPreset.current)
}

@Composable
fun rememberAppPhotoIcon(): ImageVector {
    return resolveAppPhotoIcon(LocalUiPreset.current)
}

@Composable
fun rememberAppFolderIcon(): ImageVector {
    return resolveAppFolderIcon(LocalUiPreset.current)
}

@Composable
fun rememberAppRestoreIcon(): ImageVector {
    return resolveAppRestoreIcon(LocalUiPreset.current)
}

@Composable
fun rememberAppWarningIcon(): ImageVector {
    return resolveAppWarningIcon(LocalUiPreset.current)
}

@Composable
fun rememberAppRefreshIcon(): ImageVector {
    return resolveAppRefreshIcon(LocalUiPreset.current)
}

@Composable
fun rememberAppDownloadIcon(): ImageVector {
    return resolveAppDownloadIcon(LocalUiPreset.current)
}

@Composable
fun rememberAppSearchIcon(): ImageVector = resolveAppSearchIcon(LocalUiPreset.current)

@Composable
fun rememberAppClearIcon(): ImageVector = resolveAppClearIcon(LocalUiPreset.current)

@Composable
fun rememberAppHistoryIcon(): ImageVector = resolveAppHistoryIcon(LocalUiPreset.current)

@Composable
fun rememberAppBookmarkIcon(): ImageVector = resolveAppBookmarkIcon(LocalUiPreset.current)

@Composable
fun rememberAppInboxIcon(): ImageVector = resolveAppInboxIcon(LocalUiPreset.current)

@Composable
fun rememberAppTvIcon(): ImageVector = resolveAppTvIcon(LocalUiPreset.current)

@Composable
fun rememberAppLogoutIcon(): ImageVector = resolveAppLogoutIcon(LocalUiPreset.current)

@Composable
fun rememberAppTimerIcon(): ImageVector = resolveAppTimerIcon(LocalUiPreset.current)

@Composable
fun rememberAppMusicIcon(): ImageVector = resolveAppMusicIcon(LocalUiPreset.current)

@Composable
fun rememberAppFlipHorizontalIcon(): ImageVector = resolveAppFlipHorizontalIcon(LocalUiPreset.current)

@Composable
fun rememberAppFlipVerticalIcon(): ImageVector = resolveAppFlipVerticalIcon(LocalUiPreset.current)

@Composable
fun rememberAppHeadphonesIcon(): ImageVector = resolveAppHeadphonesIcon(LocalUiPreset.current)

@Composable
fun rememberAppQualityIcon(): ImageVector = resolveAppQualityIcon(LocalUiPreset.current)

@Composable
fun rememberAppCodecIcon(): ImageVector = resolveAppCodecIcon(LocalUiPreset.current)

@Composable
fun rememberAppSpeedIcon(): ImageVector = resolveAppSpeedIcon(LocalUiPreset.current)

@Composable
fun rememberAppGestureTapIcon(): ImageVector = resolveAppGestureTapIcon(LocalUiPreset.current)

@Composable
fun rememberAppWifiIcon(): ImageVector = resolveAppWifiIcon(LocalUiPreset.current)

@Composable
fun rememberAppChevronForwardIcon(): ImageVector = resolveAppChevronForwardIcon(LocalUiPreset.current)

@Composable
fun rememberAppChevronDownIcon(): ImageVector = resolveAppChevronDownIcon(LocalUiPreset.current)

@Composable
fun rememberAppProfileAddIcon(): ImageVector = resolveAppProfileAddIcon(LocalUiPreset.current)

@Composable
fun rememberAppLockIcon(): ImageVector = resolveAppLockIcon(LocalUiPreset.current)

@Composable
fun rememberAppHomeIcon(): ImageVector = resolveAppHomeIcon(LocalUiPreset.current)

@Composable
fun rememberAppDynamicIcon(): ImageVector = resolveAppDynamicIcon(LocalUiPreset.current)

@Composable
fun rememberAppPlayIcon(): ImageVector = resolveAppPlayIcon(LocalUiPreset.current)

@Composable
fun rememberAppCollectionIcon(): ImageVector = resolveAppCollectionIcon(LocalUiPreset.current)

@Composable
fun rememberAppCommentIcon(): ImageVector = resolveAppCommentIcon(LocalUiPreset.current)

@Composable
fun rememberAppLikeIcon(): ImageVector = resolveAppLikeIcon(LocalUiPreset.current)

@Composable
fun rememberAppLikeFilledIcon(): ImageVector = resolveAppLikeFilledIcon(LocalUiPreset.current)

@Composable
fun rememberAppShareIcon(): ImageVector = resolveAppShareIcon(LocalUiPreset.current)

@Composable
fun rememberAppVisibilityOnIcon(): ImageVector = resolveAppVisibilityOnIcon(LocalUiPreset.current)

@Composable
fun rememberAppVisibilityOffIcon(): ImageVector = resolveAppVisibilityOffIcon(LocalUiPreset.current)

fun resolveAppBackIcon(uiPreset: UiPreset): ImageVector =
    if (uiPreset == UiPreset.MD3) Icons.AutoMirrored.Filled.ArrowBack else CupertinoIcons.Outlined.ChevronBackward

fun resolveAppSettingsIcon(uiPreset: UiPreset): ImageVector =
    if (uiPreset == UiPreset.MD3) Icons.Outlined.Settings else CupertinoIcons.Outlined.Gearshape

fun resolveAppMoreIcon(uiPreset: UiPreset): ImageVector =
    if (uiPreset == UiPreset.MD3) Icons.Filled.MoreVert else CupertinoIcons.Outlined.Ellipsis

fun resolveAppPhotoIcon(uiPreset: UiPreset): ImageVector =
    if (uiPreset == UiPreset.MD3) Icons.Outlined.MaterialPhoto else CupertinoIcons.Outlined.CupertinoPhoto

fun resolveAppFolderIcon(uiPreset: UiPreset): ImageVector =
    if (uiPreset == UiPreset.MD3) Icons.Outlined.MaterialFolder else CupertinoIcons.Outlined.CupertinoFolder

fun resolveAppRestoreIcon(uiPreset: UiPreset): ImageVector =
    if (uiPreset == UiPreset.MD3) Icons.Outlined.Restore else CupertinoIcons.Outlined.ArrowCounterclockwise

fun resolveAppWarningIcon(uiPreset: UiPreset): ImageVector =
    if (uiPreset == UiPreset.MD3) Icons.Outlined.WarningAmber else CupertinoIcons.Filled.ExclamationmarkTriangle

fun resolveAppRefreshIcon(uiPreset: UiPreset): ImageVector =
    if (uiPreset == UiPreset.MD3) Icons.Outlined.Refresh else CupertinoIcons.Outlined.ArrowClockwise

fun resolveAppDownloadIcon(uiPreset: UiPreset): ImageVector =
    if (uiPreset == UiPreset.MD3) Icons.Outlined.Download else CupertinoIcons.Outlined.ArrowDownCircle

fun resolveAppSearchIcon(uiPreset: UiPreset): ImageVector =
    if (uiPreset == UiPreset.MD3) Icons.Filled.Search else CupertinoIcons.Outlined.MagnifyingGlass

fun resolveAppClearIcon(uiPreset: UiPreset): ImageVector =
    if (uiPreset == UiPreset.MD3) Icons.Filled.Clear else CupertinoIcons.Outlined.XmarkCircle

fun resolveAppHistoryIcon(uiPreset: UiPreset): ImageVector =
    if (uiPreset == UiPreset.MD3) Icons.Outlined.History else CupertinoIcons.Outlined.Clock

fun resolveAppBookmarkIcon(uiPreset: UiPreset): ImageVector =
    if (uiPreset == UiPreset.MD3) Icons.Outlined.BookmarkBorder else CupertinoIcons.Outlined.Bookmark

fun resolveAppInboxIcon(uiPreset: UiPreset): ImageVector =
    if (uiPreset == UiPreset.MD3) Icons.Outlined.MailOutline else CupertinoIcons.Outlined.Envelope

fun resolveAppTvIcon(uiPreset: UiPreset): ImageVector =
    if (uiPreset == UiPreset.MD3) Icons.Outlined.LiveTv else CupertinoIcons.Filled.Tv

fun resolveAppLogoutIcon(uiPreset: UiPreset): ImageVector =
    if (uiPreset == UiPreset.MD3) Icons.AutoMirrored.Outlined.ExitToApp else CupertinoIcons.Outlined.RectanglePortraitAndArrowForward

fun resolveAppTimerIcon(uiPreset: UiPreset): ImageVector =
    if (uiPreset == UiPreset.MD3) Icons.Outlined.Timer else CupertinoIcons.Outlined.Timer

fun resolveAppMusicIcon(uiPreset: UiPreset): ImageVector =
    if (uiPreset == UiPreset.MD3) Icons.Outlined.MusicNote else CupertinoIcons.Outlined.MusicNote

fun resolveAppFlipHorizontalIcon(uiPreset: UiPreset): ImageVector =
    if (uiPreset == UiPreset.MD3) Icons.Outlined.SwapHoriz else CupertinoIcons.Outlined.ArrowLeftArrowRight

fun resolveAppFlipVerticalIcon(uiPreset: UiPreset): ImageVector =
    if (uiPreset == UiPreset.MD3) Icons.Outlined.SwapVert else CupertinoIcons.Outlined.ArrowUpArrowDown

fun resolveAppHeadphonesIcon(uiPreset: UiPreset): ImageVector =
    if (uiPreset == UiPreset.MD3) Icons.Outlined.Headphones else CupertinoIcons.Outlined.Headphones

fun resolveAppQualityIcon(uiPreset: UiPreset): ImageVector =
    if (uiPreset == UiPreset.MD3) Icons.Outlined.PlayCircleOutline else CupertinoIcons.Outlined.PlayCircle

fun resolveAppCodecIcon(uiPreset: UiPreset): ImageVector =
    if (uiPreset == UiPreset.MD3) Icons.Outlined.Memory else CupertinoIcons.Outlined.Cpu

fun resolveAppSpeedIcon(uiPreset: UiPreset): ImageVector =
    if (uiPreset == UiPreset.MD3) Icons.Outlined.Speed else CupertinoIcons.Outlined.Speedometer

fun resolveAppGestureTapIcon(uiPreset: UiPreset): ImageVector =
    if (uiPreset == UiPreset.MD3) Icons.Outlined.TouchApp else CupertinoIcons.Outlined.HandTap

fun resolveAppWifiIcon(uiPreset: UiPreset): ImageVector =
    if (uiPreset == UiPreset.MD3) Icons.Outlined.Wifi else CupertinoIcons.Outlined.Wifi

fun resolveAppChevronForwardIcon(uiPreset: UiPreset): ImageVector =
    if (uiPreset == UiPreset.MD3) Icons.AutoMirrored.Outlined.KeyboardArrowRight else CupertinoIcons.Outlined.ChevronForward

fun resolveAppChevronDownIcon(uiPreset: UiPreset): ImageVector =
    if (uiPreset == UiPreset.MD3) Icons.Outlined.KeyboardArrowDown else CupertinoIcons.Outlined.ChevronDown

fun resolveAppProfileAddIcon(uiPreset: UiPreset): ImageVector =
    if (uiPreset == UiPreset.MD3) Icons.Outlined.PersonAddAlt1 else CupertinoIcons.Outlined.PersonCropCircleBadgePlus

fun resolveAppLockIcon(uiPreset: UiPreset): ImageVector =
    if (uiPreset == UiPreset.MD3) Icons.Outlined.Lock else CupertinoIcons.Outlined.Lock

fun resolveAppHomeIcon(uiPreset: UiPreset): ImageVector =
    if (uiPreset == UiPreset.MD3) Icons.Outlined.Home else CupertinoIcons.Outlined.House

fun resolveAppDynamicIcon(uiPreset: UiPreset): ImageVector =
    if (uiPreset == UiPreset.MD3) Icons.Outlined.DynamicFeed else CupertinoIcons.Outlined.RectangleStack

fun resolveAppPlayIcon(uiPreset: UiPreset): ImageVector =
    if (uiPreset == UiPreset.MD3) Icons.Outlined.PlayArrow else CupertinoIcons.Outlined.Play

fun resolveAppCollectionIcon(uiPreset: UiPreset): ImageVector =
    if (uiPreset == UiPreset.MD3) Icons.Outlined.FolderCopy else CupertinoIcons.Outlined.CupertinoFolder

fun resolveAppCommentIcon(uiPreset: UiPreset): ImageVector =
    if (uiPreset == UiPreset.MD3) Icons.AutoMirrored.Outlined.Comment else CupertinoIcons.Outlined.Message

fun resolveAppLikeIcon(uiPreset: UiPreset): ImageVector =
    if (uiPreset == UiPreset.MD3) Icons.Outlined.ThumbUpOffAlt else CupertinoIcons.Outlined.HandThumbsup

fun resolveAppLikeFilledIcon(uiPreset: UiPreset): ImageVector =
    if (uiPreset == UiPreset.MD3) Icons.Filled.ThumbUp else CupertinoIcons.Filled.HandThumbsup

fun resolveAppShareIcon(uiPreset: UiPreset): ImageVector =
    if (uiPreset == UiPreset.MD3) Icons.Outlined.Share else CupertinoIcons.Outlined.ArrowTurnUpRight

fun resolveAppVisibilityOnIcon(uiPreset: UiPreset): ImageVector =
    if (uiPreset == UiPreset.MD3) Icons.Outlined.Visibility else CupertinoIcons.Outlined.Eye

fun resolveAppVisibilityOffIcon(uiPreset: UiPreset): ImageVector =
    if (uiPreset == UiPreset.MD3) Icons.Outlined.VisibilityOff else CupertinoIcons.Outlined.EyeSlash
