package com.android.purebilibili.feature.settings

import java.io.File
import kotlin.test.Test
import kotlin.test.assertFalse

class AppearanceSettingsNavigationPolicyTest {

    @Test
    fun appearanceSettings_noLongerHostsNavigationManagementShortcuts() {
        val source = loadSource("app/src/main/java/com/android/purebilibili/feature/settings/screen/AppearanceSettingsScreen.kt")

        assertFalse(source.contains("openTopTabManagement("))
        assertFalse(source.contains("title = \"顶部标签页\""))
        assertFalse(source.contains("title = \"顶部栏自动收缩\""))
        assertFalse(source.contains("title = \"侧边导航栏\""))
    }

    private fun loadSource(path: String): String {
        val normalizedPath = path.removePrefix("app/")
        val sourceFile = listOf(
            File(path),
            File(normalizedPath)
        ).firstOrNull { it.exists() }
        require(sourceFile != null) { "Cannot locate $path from ${File(".").absolutePath}" }
        return sourceFile.readText()
    }
}
