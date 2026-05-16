package com.android.purebilibili.feature.settings

import kotlin.test.Test
import kotlin.test.assertEquals

class SettingsRootCategoryPolicyTest {

    @Test
    fun `mobile and tablet settings share scene based root category order`() {
        val expected = listOf(
            SettingsRootCategory.INTERFACE_THEME,
            SettingsRootCategory.HOME_FEED,
            SettingsRootCategory.NAVIGATION_LABELS,
            SettingsRootCategory.PLAYBACK_QUALITY,
            SettingsRootCategory.FULLSCREEN_GESTURE,
            SettingsRootCategory.INTERACTION_COMMENT,
            SettingsRootCategory.DATA_BACKUP,
            SettingsRootCategory.PRIVACY_PERMISSION,
            SettingsRootCategory.DIAGNOSTICS_DEVELOPER,
            SettingsRootCategory.ABOUT_SUPPORT
        )

        assertEquals(expected, resolveSettingsRootCategoryOrder())
        assertEquals(resolveSettingsRootCategoryOrder(), resolveTabletSettingsRootCategoryOrder())
    }

    @Test
    fun `scene based root categories expose user facing titles`() {
        assertEquals(
            listOf(
                "界面与主题",
                "首页与推荐",
                "导航与标签",
                "播放与画质",
                "全屏与手势",
                "互动与评论",
                "数据与备份",
                "隐私与权限",
                "诊断与开发",
                "关于与支持"
            ),
            resolveSettingsRootCategoryOrder().map { it.title }
        )
    }

    @Test
    fun `scene search targets map back to root categories`() {
        assertEquals(
            SettingsRootCategory.HOME_FEED,
            resolveSettingsRootCategoryForSearchTarget(SettingsSearchTarget.HOME_FEED)
        )
        assertEquals(
            SettingsRootCategory.FULLSCREEN_GESTURE,
            resolveSettingsRootCategoryForSearchTarget(SettingsSearchTarget.FULLSCREEN_GESTURE)
        )
        assertEquals(
            SettingsRootCategory.DIAGNOSTICS_DEVELOPER,
            resolveSettingsRootCategoryForSearchTarget(SettingsSearchTarget.DIAGNOSTICS)
        )
    }
}
