package com.android.purebilibili.core.ui.components

import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

class IOSGroupSurfaceShapeStructureTest {

    @Test
    fun `ios group passes rounded shape into surface for md3 and miuix borders`() {
        val source = loadSource("app/src/main/java/com/android/purebilibili/core/ui/components/iOSListComponents.kt")
        val iosGroupSource = source
            .substringAfter("fun IOSGroup(")
            .substringBefore("@Composable\nfun IOSSwitchItem")

        assertTrue(iosGroupSource.contains(".clip(appliedShape)"))
        assertTrue(iosGroupSource.contains("Surface("))
        assertTrue(iosGroupSource.contains("shape = appliedShape,"))
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
