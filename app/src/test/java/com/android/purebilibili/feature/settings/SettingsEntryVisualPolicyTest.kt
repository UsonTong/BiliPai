package com.android.purebilibili.feature.settings

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
}
