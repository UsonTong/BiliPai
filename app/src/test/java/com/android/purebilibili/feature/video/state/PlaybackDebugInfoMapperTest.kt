package com.android.purebilibili.feature.video.state

import androidx.media3.common.Format
import com.android.purebilibili.feature.video.ui.overlay.PlaybackDebugInfo
import kotlin.test.Test
import kotlin.test.assertEquals

class PlaybackDebugInfoMapperTest {

    @Test
    fun applyVideoFormatDebugInfo_formatsResolutionCodecBitrateAndFrameRate() {
        val result = applyVideoFormatDebugInfo(
            current = PlaybackDebugInfo(),
            format = Format.Builder()
                .setWidth(1920)
                .setHeight(1080)
                .setSampleMimeType("video/hevc")
                .setPeakBitrate(8_400_000)
                .setFrameRate(59.94f)
                .build(),
            decoderName = "c2.qti.hevc.decoder"
        )

        assertEquals("1920 x 1080", result.resolution)
        assertEquals("8.4 Mbps", result.videoBitrate)
        assertEquals("HEVC", result.videoCodec)
        assertEquals("59.94 fps", result.frameRate)
        assertEquals("c2.qti.hevc.decoder", result.videoDecoder)
    }

    @Test
    fun applyAudioFormatDebugInfo_formatsCodecAndBitrate() {
        val result = applyAudioFormatDebugInfo(
            current = PlaybackDebugInfo(),
            format = Format.Builder()
                .setSampleMimeType("audio/mp4a-latm")
                .setPeakBitrate(192_000)
                .build(),
            decoderName = "c2.android.aac.decoder"
        )

        assertEquals("192 kbps", result.audioBitrate)
        assertEquals("AAC", result.audioCodec)
        assertEquals("c2.android.aac.decoder", result.audioDecoder)
    }
}
