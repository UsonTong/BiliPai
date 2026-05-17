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
    fun `manifest opts into system predictive back so enabled setting can show the effect`() {
        val manifest = listOf(
            File("app/src/main/AndroidManifest.xml"),
            File("src/main/AndroidManifest.xml")
        ).firstOrNull { it.exists() } ?: error("Cannot locate AndroidManifest.xml")

        val source = manifest.readText()

        assertTrue(
            source.contains("""android:enableOnBackInvokedCallback="true""""),
            "Predictive back requires manifest opt-in; setting false disables the platform animation globally."
        )
        assertFalse(
            source.contains("""android:enableOnBackInvokedCallback="false""""),
            "Do not opt out globally because the in-app enabled state would never receive predictive back progress."
        )
    }
}
