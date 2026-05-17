package com.android.purebilibili.feature.home

import java.io.File
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class HomeFeedScrollStatePersistenceStructureTest {

    @Test
    fun `home category grid states are saveable per category`() {
        val source = loadSource("app/src/main/java/com/android/purebilibili/feature/home/HomeScreen.kt")

        assertTrue(source.contains("rememberSaveable("))
        assertTrue(source.contains("category.name"))
        assertTrue(source.contains("saver = LazyGridState.Saver"))
        assertTrue(source.contains("LazyGridState()"))
        assertFalse(source.contains("gridStates[category] = rememberLazyGridState()"))
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
