package com.android.purebilibili.feature.space

import java.io.File
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SpaceScreenStructureTest {

    @Test
    fun `contribution videos render as grid cards instead of full width rows`() {
        val source = loadSource("app/src/main/java/com/android/purebilibili/feature/space/SpaceScreen.kt")

        assertTrue(source.contains("columns = GridCells.Fixed("))
        assertTrue(source.contains("resolveSpaceContentGridColumnCount("))
        assertTrue(source.contains("SpaceContributionVideoLayoutMode.GRID"))
        assertTrue(source.contains("SpaceHomeVideoCard("))
        assertTrue(source.contains("key = { \"space_video_${'$'}{it.bvid}_${'$'}{it.aid}\" }"))
        assertFalse(source.contains("SpaceVideoListItemRow("))
    }

    @Test
    fun `contribution videos expose single column toggle with animated card transition`() {
        val source = loadSource("app/src/main/java/com/android/purebilibili/feature/space/SpaceScreen.kt")

        assertTrue(source.contains("onLayoutModeClick"))
        assertTrue(source.contains("toggleSpaceContributionVideoLayoutMode"))
        assertTrue(source.contains("AnimatedContent("))
        assertTrue(source.contains("SizeTransform(clip = false)"))
        assertTrue(source.contains("resolveSpaceContributionVideoGridSpan("))
        assertTrue(source.contains("SpaceContributionVideoLayoutMode.SINGLE_COLUMN"))
        assertTrue(source.contains("SpaceArchiveListItemRow("))
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
