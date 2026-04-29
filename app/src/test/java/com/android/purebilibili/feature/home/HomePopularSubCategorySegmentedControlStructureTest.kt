package com.android.purebilibili.feature.home

import java.io.File
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class HomePopularSubCategorySegmentedControlStructureTest {

    @Test
    fun `popular subcategory row delegates to bottom bar liquid segmented control`() {
        val source = loadSource(
            "app/src/main/java/com/android/purebilibili/feature/home/HomeCategoryPage.kt"
        )

        assertTrue(source.contains("BottomBarLiquidSegmentedControl("))
        assertTrue(source.contains("PopularSubCategorySegmentedControl("))
        assertTrue(source.contains("dragSelectionEnabled = true"))
        assertTrue(source.contains("liquidGlassEffectsEnabled = true"))
        assertFalse(source.contains("PopularSubCategory.entries.forEach { subCategory ->\n                            FilterChip("))
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
