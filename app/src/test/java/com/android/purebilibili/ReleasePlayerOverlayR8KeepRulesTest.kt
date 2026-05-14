package com.android.purebilibili

import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

class ReleasePlayerOverlayR8KeepRulesTest {

    @Test
    fun releasePlayerControlUiKeepsComposeOverlayEntrypoints() {
        val rules = listOf(
            File("app/proguard-rules.pro"),
            File("proguard-rules.pro")
        ).first { it.exists() }.readText()

        assertTrue(
            rules.contains(
                "-keep class com.android.purebilibili.feature.video.ui.section.VideoPlayerSectionKt { *; }"
            )
        )
        assertTrue(
            rules.contains(
                "-keep class com.android.purebilibili.feature.video.ui.overlay.** { *; }"
            )
        )
    }
}
