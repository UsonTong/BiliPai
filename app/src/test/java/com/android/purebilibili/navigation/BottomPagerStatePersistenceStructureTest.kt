package com.android.purebilibili.navigation

import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

class BottomPagerStatePersistenceStructureTest {

    @Test
    fun `bottom pager keeps saveable state by bottom tab identity`() {
        val source = loadSource("app/src/main/java/com/android/purebilibili/navigation/AppNavigation.kt")

        assertTrue(source.contains("rememberSaveableStateHolder()"))
        assertTrue(source.contains("bottomPagerSaveableStateHolder.SaveableStateProvider("))
        assertTrue(source.contains("key = resolveBottomPagerSaveableStateKey(pageItem)"))
        assertTrue(source.contains("key = { page ->"))
        assertTrue(source.contains("resolveBottomPagerItemForPage(page, visibleBottomBarItems)"))
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
