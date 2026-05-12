package com.android.purebilibili.feature.bangumi

import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

class BangumiPlayerCommentPolicyTest {

    @Test
    fun `bangumi player content exposes episode comment entry`() {
        val source = loadSource(
            "app/src/main/java/com/android/purebilibili/feature/bangumi/ui/player/BangumiPlayerContent.kt"
        )

        assertTrue(source.contains("onCommentClick: () -> Unit"))
        assertTrue(source.contains("Text(\"评论\")"))
    }

    @Test
    fun `bangumi player screen hosts video comment sheet for current episode aid`() {
        val source = loadSource(
            "app/src/main/java/com/android/purebilibili/feature/bangumi/BangumiPlayerScreen.kt"
        )

        assertTrue(source.contains("VideoCommentSheetHost("))
        assertTrue(source.contains("aid = currentAid"))
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
