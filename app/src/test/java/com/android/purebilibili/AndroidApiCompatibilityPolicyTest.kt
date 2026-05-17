package com.android.purebilibili

import java.io.File
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AndroidApiCompatibilityPolicyTest {

    @Test
    fun `production sources avoid API 35 list removeFirst calls`() {
        val sourceRoot = listOf(
            File("app/src/main/java"),
            File("src/main/java")
        ).firstOrNull { it.exists() } ?: error("Cannot locate production source root")

        val offendingLines = sourceRoot
            .walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .flatMap { file ->
                file.readLines().mapIndexedNotNull { index, line ->
                    if (line.contains(".removeFirst(")) {
                        "${file.relativeTo(sourceRoot)}:${index + 1}: ${line.trim()}"
                    } else {
                        null
                    }
                }
            }
            .toList()

        assertTrue(
            offendingLines.isEmpty(),
            "Use removeAt(0) or an Android-compatible queue API instead of List.removeFirst():\n" +
                offendingLines.joinToString(separator = "\n")
        )
    }

    @Test
    fun `manifest opts out of system predictive back so app switch can truly disable the effect`() {
        val manifest = listOf(
            File("app/src/main/AndroidManifest.xml"),
            File("src/main/AndroidManifest.xml")
        ).firstOrNull { it.exists() } ?: error("Cannot locate AndroidManifest.xml")

        val source = manifest.readText()

        assertFalse(
            source.contains("""android:enableOnBackInvokedCallback="true""""),
            "Do not opt in globally: the platform predictive back animation cannot be reliably disabled by the in-app switch at runtime."
        )
        assertTrue(
            source.contains("""android:enableOnBackInvokedCallback="false""""),
            "The app must stay system-predictive-back opt-out so closing the setting has a real effect."
        )
    }
}
