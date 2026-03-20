package com.android.purebilibili.feature.home.components

import java.nio.file.Files
import java.nio.file.Paths
import kotlin.io.path.readText
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeChromeLiquidSurfaceStructureTest {

    @Test
    fun `top header and bottom bar both use shared liquid surface renderer`() {
        val workspaceRoot = generateSequence(
            Paths.get(System.getProperty("user.dir")).toAbsolutePath()
        ) { current ->
            current.parent
        }.first { candidate ->
            Files.exists(
                candidate.resolve(
                    "app/src/main/java/com/android/purebilibili/feature/home/components/iOSHomeHeader.kt"
                )
            )
        }
        val componentsDir = workspaceRoot.resolve(
            "app/src/main/java/com/android/purebilibili/feature/home/components"
        )

        val sharedRenderer = componentsDir.resolve("HomeChromeLiquidSurface.kt")
        val topHeader = componentsDir.resolve("iOSHomeHeader.kt")
        val bottomBar = componentsDir.resolve("BottomBar.kt")

        assertTrue(
            "shared renderer file should exist",
            Files.exists(sharedRenderer)
        )
        assertTrue(
            "top header should delegate to the shared liquid surface renderer",
            topHeader.readText().contains(".appChromeLiquidSurface(")
        )
        assertTrue(
            "bottom bar should delegate to the shared liquid surface renderer",
            bottomBar.readText().contains(".appChromeLiquidSurface(")
        )
    }
}
