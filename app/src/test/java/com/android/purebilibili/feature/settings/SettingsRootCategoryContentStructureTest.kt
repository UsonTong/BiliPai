package com.android.purebilibili.feature.settings

import java.io.File
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SettingsRootCategoryContentStructureTest {

    @Test
    fun rootCategoryContent_usesStateAndActionHolders() {
        val source = listOf(
            File("app/src/main/java/com/android/purebilibili/feature/settings/ui/SettingsSections.kt"),
            File("src/main/java/com/android/purebilibili/feature/settings/ui/SettingsSections.kt")
        ).first { it.exists() }.readText()

        assertTrue(source.contains("internal data class SettingsRootCategoryActions("))
        assertTrue(source.contains("internal data class SettingsRootCategoryState("))
        assertTrue(
            source.contains(
                """
                internal fun SettingsRootCategoryContent(
                    category: SettingsRootCategory,
                    actions: SettingsRootCategoryActions,
                    state: SettingsRootCategoryState
                )
                """.trimIndent()
            )
        )
    }

    @Test
    fun sceneShortcutSection_submitsDetailFocusBeforeOpeningShortcut() {
        val source = listOf(
            File("app/src/main/java/com/android/purebilibili/feature/settings/ui/SettingsSections.kt"),
            File("src/main/java/com/android/purebilibili/feature/settings/ui/SettingsSections.kt")
        ).first { it.exists() }.readText()

        val sectionBlock = source
            .substringAfter("internal fun SettingsSceneShortcutSection(")
            .substringBefore("internal fun SettingsRootCategoryContent(")

        assertTrue(sectionBlock.contains("resolveSettingsSceneDetailFocus(shortcut.target)?.let"))
        assertTrue(sectionBlock.contains("SettingsSearchFocusController.submit(detailFocus.target, detailFocus.focusId)"))
        assertTrue(sectionBlock.contains("shortcut.onClick()"))
    }

    @Test
    fun sceneShortcutSection_rendersLongDescriptionAsSubtitle() {
        val source = listOf(
            File("app/src/main/java/com/android/purebilibili/feature/settings/ui/SettingsSections.kt"),
            File("src/main/java/com/android/purebilibili/feature/settings/ui/SettingsSections.kt")
        ).first { it.exists() }.readText()

        val sectionBlock = source
            .substringAfter("internal fun SettingsSceneShortcutSection(")
            .substringBefore("internal fun SettingsRootCategoryContent(")

        assertTrue(sectionBlock.contains("subtitle = shortcut.value"))
    }

    @Test
    fun feedSwitchDescription_allowsWrappingInIosSettings() {
        val source = listOf(
            File("app/src/main/java/com/android/purebilibili/feature/settings/ui/SettingsSections.kt"),
            File("src/main/java/com/android/purebilibili/feature/settings/ui/SettingsSections.kt")
        ).first { it.exists() }.readText()

        val feedSwitchBlock = source
            .substringAfter("private fun FeedSwitchItem(")
            .substringBefore("@Composable\nprivate fun FeedRefreshCountItem(")

        assertTrue(feedSwitchBlock.contains("text = subtitle"))
        assertTrue(!feedSwitchBlock.contains("maxLines = 1"))
    }

    @Test
    fun mobileSettingsRoot_doesNotRenderDuplicateCategoryHeaders() {
        val source = listOf(
            File("app/src/main/java/com/android/purebilibili/feature/settings/screen/SettingsScreen.kt"),
            File("src/main/java/com/android/purebilibili/feature/settings/screen/SettingsScreen.kt")
        ).first { it.exists() }.readText()

        val rootLoopBlock = source
            .substringAfter("sectionOrder.forEachIndexed { index, section ->")
            .substringBefore("item { Spacer(modifier = Modifier.height(16.dp)) }")

        assertFalse(rootLoopBlock.contains("SettingsCategoryHeader("))
        assertTrue(rootLoopBlock.contains("SettingsRootCategoryContent("))
    }
}
