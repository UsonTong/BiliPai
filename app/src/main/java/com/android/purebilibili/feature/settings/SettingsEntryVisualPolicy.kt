package com.android.purebilibili.feature.settings

import androidx.annotation.DrawableRes
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.android.purebilibili.R
import com.android.purebilibili.core.theme.iOSBlue
import com.android.purebilibili.core.theme.iOSGreen
import com.android.purebilibili.core.theme.iOSOrange
import com.android.purebilibili.core.theme.iOSPink
import com.android.purebilibili.core.theme.iOSPurple
import com.android.purebilibili.core.theme.iOSRed
import com.android.purebilibili.core.theme.iOSTeal
import com.android.purebilibili.core.ui.AppIcons
import io.github.alexzhirkevich.cupertino.icons.CupertinoIcons
import io.github.alexzhirkevich.cupertino.icons.outlined.ArrowCounterclockwise
import io.github.alexzhirkevich.cupertino.icons.outlined.ArrowTriangle2Circlepath
import io.github.alexzhirkevich.cupertino.icons.outlined.DocOnDoc
import io.github.alexzhirkevich.cupertino.icons.outlined.DocText
import io.github.alexzhirkevich.cupertino.icons.outlined.ExclamationmarkTriangle
import io.github.alexzhirkevich.cupertino.icons.outlined.Folder
import io.github.alexzhirkevich.cupertino.icons.outlined.Gift
import io.github.alexzhirkevich.cupertino.icons.outlined.Lightbulb
import io.github.alexzhirkevich.cupertino.icons.outlined.Link
import io.github.alexzhirkevich.cupertino.icons.outlined.ListBullet
import io.github.alexzhirkevich.cupertino.icons.outlined.Lock
import io.github.alexzhirkevich.cupertino.icons.outlined.Newspaper
import io.github.alexzhirkevich.cupertino.icons.outlined.PaintbrushPointed
import io.github.alexzhirkevich.cupertino.icons.outlined.Person
import io.github.alexzhirkevich.cupertino.icons.outlined.PlayCircle
import io.github.alexzhirkevich.cupertino.icons.outlined.PuzzlepieceExtension
import io.github.alexzhirkevich.cupertino.icons.outlined.SquareAndArrowUp
import io.github.alexzhirkevich.cupertino.icons.outlined.SquareStack3dUp
import io.github.alexzhirkevich.cupertino.icons.outlined.Terminal
import io.github.alexzhirkevich.cupertino.icons.outlined.Trash
import io.github.alexzhirkevich.cupertino.icons.outlined.XmarkCircle

internal data class SettingsEntryVisual(
    val icon: ImageVector? = null,
    @DrawableRes val iconResId: Int? = null,
    val iconTint: Color
)

internal fun resolveSettingsEntryVisual(target: SettingsSearchTarget): SettingsEntryVisual {
    return when (target) {
        SettingsSearchTarget.APPEARANCE -> SettingsEntryVisual(
            icon = CupertinoIcons.Default.PaintbrushPointed,
            iconTint = iOSPink
        )
        SettingsSearchTarget.PLAYBACK -> SettingsEntryVisual(
            icon = CupertinoIcons.Default.PlayCircle,
            iconTint = iOSGreen
        )
        SettingsSearchTarget.BOTTOM_BAR -> SettingsEntryVisual(
            icon = CupertinoIcons.Default.SquareStack3dUp,
            iconTint = iOSBlue
        )
        SettingsSearchTarget.PERMISSION -> SettingsEntryVisual(
            icon = CupertinoIcons.Default.Lock,
            iconTint = iOSTeal
        )
        SettingsSearchTarget.BLOCKED_LIST -> SettingsEntryVisual(
            icon = CupertinoIcons.Default.XmarkCircle,
            iconTint = iOSRed
        )
        SettingsSearchTarget.WEBDAV_BACKUP -> SettingsEntryVisual(
            icon = CupertinoIcons.Default.DocOnDoc,
            iconTint = iOSBlue
        )
        SettingsSearchTarget.DOWNLOAD_PATH -> SettingsEntryVisual(
            icon = CupertinoIcons.Default.Folder,
            iconTint = iOSBlue
        )
        SettingsSearchTarget.CLEAR_CACHE -> SettingsEntryVisual(
            icon = CupertinoIcons.Default.Trash,
            iconTint = iOSPink
        )
        SettingsSearchTarget.PLUGINS -> SettingsEntryVisual(
            icon = CupertinoIcons.Default.PuzzlepieceExtension,
            iconTint = iOSPurple
        )
        SettingsSearchTarget.EXPORT_LOGS -> SettingsEntryVisual(
            icon = CupertinoIcons.Default.Terminal,
            iconTint = iOSTeal
        )
        SettingsSearchTarget.OPEN_SOURCE_LICENSES -> SettingsEntryVisual(
            icon = CupertinoIcons.Default.DocText,
            iconTint = iOSOrange
        )
        SettingsSearchTarget.OPEN_SOURCE_HOME -> SettingsEntryVisual(
            icon = CupertinoIcons.Default.SquareAndArrowUp,
            iconTint = iOSPurple
        )
        SettingsSearchTarget.CHECK_UPDATE -> SettingsEntryVisual(
            icon = CupertinoIcons.Default.ArrowTriangle2Circlepath,
            iconTint = iOSBlue
        )
        SettingsSearchTarget.VIEW_RELEASE_NOTES -> SettingsEntryVisual(
            icon = CupertinoIcons.Default.Newspaper,
            iconTint = iOSTeal
        )
        SettingsSearchTarget.REPLAY_ONBOARDING -> SettingsEntryVisual(
            icon = CupertinoIcons.Default.ArrowCounterclockwise,
            iconTint = iOSPink
        )
        SettingsSearchTarget.TIPS -> SettingsEntryVisual(
            icon = CupertinoIcons.Default.Lightbulb,
            iconTint = iOSOrange
        )
        SettingsSearchTarget.OPEN_LINKS -> SettingsEntryVisual(
            icon = CupertinoIcons.Default.Link,
            iconTint = iOSTeal
        )
        SettingsSearchTarget.DONATE -> SettingsEntryVisual(
            icon = CupertinoIcons.Default.Gift,
            iconTint = Color(0xFFFF3B30)
        )
        SettingsSearchTarget.TELEGRAM -> SettingsEntryVisual(
            iconResId = R.drawable.ic_telegram_mono,
            iconTint = Color(0xFF0088CC)
        )
        SettingsSearchTarget.TWITTER -> SettingsEntryVisual(
            icon = AppIcons.Twitter,
            iconTint = Color(0xFF1DA1F2)
        )
        SettingsSearchTarget.DISCLAIMER -> SettingsEntryVisual(
            icon = CupertinoIcons.Default.ExclamationmarkTriangle,
            iconTint = iOSBlue
        )
    }
}
