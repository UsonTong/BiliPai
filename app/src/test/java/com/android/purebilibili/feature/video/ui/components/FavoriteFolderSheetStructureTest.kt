package com.android.purebilibili.feature.video.ui.components

import java.nio.file.Files
import java.nio.file.Paths
import kotlin.io.path.readText
import org.junit.Assert.assertTrue
import org.junit.Test

class FavoriteFolderSheetStructureTest {

    @Test
    fun `favorite folder sheet constrains sheet height and keeps footer outside the list`() {
        val workspaceRoot = generateSequence(
            Paths.get(System.getProperty("user.dir")).toAbsolutePath()
        ) { current ->
            current.parent
        }.first { candidate ->
            Files.exists(
                candidate.resolve(
                    "app/src/main/java/com/android/purebilibili/feature/video/ui/components/FavoriteFolderSheet.kt"
                )
            )
        }
        val file = workspaceRoot.resolve(
            "app/src/main/java/com/android/purebilibili/feature/video/ui/components/FavoriteFolderSheet.kt"
        )
        val source = file.readText()

        assertTrue(
            "sheet content should cap its height so the footer stays visible",
            source.contains(".heightIn(max = maxSheetHeight)")
        )
        assertTrue(
            "folder list should occupy the remaining height and scroll internally",
            source.contains("modifier = Modifier.weight(1f)")
        )
        assertTrue(
            "footer should reserve navigation bar space instead of being pushed out of view",
            source.contains(".navigationBarsPadding()")
        )
    }
}
