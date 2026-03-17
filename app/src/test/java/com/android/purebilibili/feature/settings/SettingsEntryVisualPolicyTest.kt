package com.android.purebilibili.feature.settings

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Security
import androidx.compose.ui.graphics.Color
import com.android.purebilibili.core.theme.UiPreset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import io.github.alexzhirkevich.cupertino.icons.CupertinoIcons
import io.github.alexzhirkevich.cupertino.icons.outlined.ArrowClockwise
import io.github.alexzhirkevich.cupertino.icons.outlined.ArrowTriangle2Circlepath
import io.github.alexzhirkevich.cupertino.icons.outlined.BellBadge
import io.github.alexzhirkevich.cupertino.icons.outlined.Bolt
import io.github.alexzhirkevich.cupertino.icons.outlined.ChartBar
import io.github.alexzhirkevich.cupertino.icons.outlined.EyeSlash
import io.github.alexzhirkevich.cupertino.icons.outlined.ExclamationmarkTriangle
import io.github.alexzhirkevich.cupertino.icons.outlined.Gift
import io.github.alexzhirkevich.cupertino.icons.outlined.Sparkles
import io.github.alexzhirkevich.cupertino.icons.outlined.Tag
import io.github.alexzhirkevich.cupertino.icons.outlined.XmarkCircle

class SettingsEntryVisualPolicyTest {

    private val md3Palette = SettingsEntryThemePalette(
        primary = Color(0xFF112233),
        secondary = Color(0xFF223344),
        tertiary = Color(0xFF334455),
        error = Color(0xFF445566)
    )

    @Test
    fun `general section entries should use distinct icons`() {
        val visuals = listOf(
            resolveSettingsEntryVisual(SettingsSearchTarget.APPEARANCE),
            resolveSettingsEntryVisual(SettingsSearchTarget.PLAYBACK),
            resolveSettingsEntryVisual(SettingsSearchTarget.BOTTOM_BAR)
        )

        assertTrue(visuals.all { it.icon != null })
        assertEquals(3, visuals.map { it.icon }.toSet().size)
    }

    @Test
    fun `blocked list should use explicit blocked semantic icon`() {
        val visual = resolveSettingsEntryVisual(SettingsSearchTarget.BLOCKED_LIST)
        assertNotNull(visual.icon)
        assertEquals(CupertinoIcons.Default.XmarkCircle, visual.icon)
    }

    @Test
    fun `donate should use gift semantic icon`() {
        val visual = resolveSettingsEntryVisual(SettingsSearchTarget.DONATE)
        assertNotNull(visual.icon)
        assertEquals(CupertinoIcons.Default.Gift, visual.icon)
    }

    @Test
    fun `md3 preset should use material semantic icons for key settings entries`() {
        assertEquals(
            Icons.Outlined.Palette,
            resolveSettingsEntryVisual(
                SettingsSearchTarget.APPEARANCE,
                UiPreset.MD3,
                md3Palette
            ).icon
        )
        assertEquals(
            Icons.Outlined.Security,
            resolveSettingsEntryVisual(
                SettingsSearchTarget.PERMISSION,
                UiPreset.MD3,
                md3Palette
            ).icon
        )
    }

    @Test
    fun `md3 preset should derive settings entry tints from theme palette roles`() {
        assertEquals(
            md3Palette.tertiary,
            resolveSettingsEntryVisual(
                SettingsSearchTarget.APPEARANCE,
                UiPreset.MD3,
                md3Palette
            ).iconTint
        )
        assertEquals(
            md3Palette.secondary,
            resolveSettingsEntryVisual(
                SettingsSearchTarget.PERMISSION,
                UiPreset.MD3,
                md3Palette
            ).iconTint
        )
        assertEquals(
            md3Palette.primary,
            resolveSettingsEntryVisual(
                SettingsSearchTarget.TELEGRAM,
                UiPreset.MD3,
                md3Palette
            ).iconTint
        )
        assertEquals(
            md3Palette.error,
            resolveSettingsEntryVisual(
                SettingsSearchTarget.CLEAR_CACHE,
                UiPreset.MD3,
                md3Palette
            ).iconTint
        )
    }

    @Test
    fun `all settings targets should avoid duplicate icon vectors`() {
        val iconVisuals = SettingsSearchTarget.entries
            .map(::resolveSettingsEntryVisual)
            .mapNotNull { it.icon }

        assertEquals(iconVisuals.size, iconVisuals.toSet().size)
    }

    @Test
    fun `mobile settings homepage icons should all be unique in strict mode`() {
        val sectionTargetIcons = listOf(
            SettingsSearchTarget.APPEARANCE,
            SettingsSearchTarget.PLAYBACK,
            SettingsSearchTarget.BOTTOM_BAR,
            SettingsSearchTarget.PERMISSION,
            SettingsSearchTarget.BLOCKED_LIST,
            SettingsSearchTarget.WEBDAV_BACKUP,
            SettingsSearchTarget.DOWNLOAD_PATH,
            SettingsSearchTarget.CLEAR_CACHE,
            SettingsSearchTarget.PLUGINS,
            SettingsSearchTarget.EXPORT_LOGS,
            SettingsSearchTarget.DISCLAIMER,
            SettingsSearchTarget.OPEN_SOURCE_LICENSES,
            SettingsSearchTarget.OPEN_SOURCE_HOME,
            SettingsSearchTarget.CHECK_UPDATE,
            SettingsSearchTarget.VIEW_RELEASE_NOTES,
            SettingsSearchTarget.REPLAY_ONBOARDING,
            SettingsSearchTarget.TIPS,
            SettingsSearchTarget.OPEN_LINKS,
            SettingsSearchTarget.DONATE,
            SettingsSearchTarget.TWITTER
        ).map(::resolveSettingsEntryVisual).mapNotNull { it.icon }

        val homepageDirectSwitchIcons = listOf(
            CupertinoIcons.Default.EyeSlash,
            CupertinoIcons.Default.Bolt,
            CupertinoIcons.Default.ChartBar,
            CupertinoIcons.Default.ArrowClockwise,
            CupertinoIcons.Default.BellBadge,
            CupertinoIcons.Default.Tag,
            CupertinoIcons.Default.Sparkles
        )

        val allHomepageIcons = sectionTargetIcons + homepageDirectSwitchIcons
        assertEquals(allHomepageIcons.size, allHomepageIcons.toSet().size)
    }

    @Test
    fun `md3 settings homepage icons should also remain distinct`() {
        val sectionTargetIcons = listOf(
            SettingsSearchTarget.APPEARANCE,
            SettingsSearchTarget.PLAYBACK,
            SettingsSearchTarget.BOTTOM_BAR,
            SettingsSearchTarget.PERMISSION,
            SettingsSearchTarget.BLOCKED_LIST,
            SettingsSearchTarget.WEBDAV_BACKUP,
            SettingsSearchTarget.DOWNLOAD_PATH,
            SettingsSearchTarget.CLEAR_CACHE,
            SettingsSearchTarget.PLUGINS,
            SettingsSearchTarget.EXPORT_LOGS,
            SettingsSearchTarget.DISCLAIMER,
            SettingsSearchTarget.OPEN_SOURCE_LICENSES,
            SettingsSearchTarget.OPEN_SOURCE_HOME,
            SettingsSearchTarget.CHECK_UPDATE,
            SettingsSearchTarget.VIEW_RELEASE_NOTES,
            SettingsSearchTarget.REPLAY_ONBOARDING,
            SettingsSearchTarget.TIPS,
            SettingsSearchTarget.OPEN_LINKS,
            SettingsSearchTarget.DONATE,
            SettingsSearchTarget.TWITTER
        ).map {
            resolveSettingsEntryVisual(it, UiPreset.MD3)
        }.mapNotNull { it.icon }

        assertEquals(sectionTargetIcons.size, sectionTargetIcons.toSet().size)
    }
}
