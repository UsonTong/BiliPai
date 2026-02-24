package com.android.purebilibili.data.model.response

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class VideoResponseCodecSelectionTest {

    @Test
    fun `getBestVideo falls back to second preferred codec when primary unavailable`() {
        val dash = Dash(
            video = listOf(
                DashVideo(id = 80, baseUrl = "https://example.com/avc.m4s", codecs = "avc1"),
                DashVideo(id = 80, baseUrl = "https://example.com/av1.m4s", codecs = "av01")
            )
        )

        val selected = dash.getBestVideo(
            targetQn = 80,
            preferCodec = "hev1",
            secondPreferCodec = "av01",
            isHevcSupported = true,
            isAv1Supported = true
        )

        assertNotNull(selected)
        assertEquals("av01", selected.codecs)
    }
}
