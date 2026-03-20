package com.android.purebilibili.feature.video.ui.overlay

import androidx.compose.ui.graphics.Color

data class PlaybackDebugInfo(
    val resolution: String = "",
    val videoBitrate: String = "",
    val audioBitrate: String = "",
    val videoCodec: String = "",
    val audioCodec: String = "",
    val frameRate: String = "",
    val videoDecoder: String = "",
    val audioDecoder: String = ""
)

data class DebugStatRow(
    val label: String,
    val value: String
)

data class CenterPlaybackButtonStyle(
    val containerColor: Color,
    val innerColor: Color,
    val borderColor: Color,
    val iconTint: Color
)

internal fun resolvePlaybackDebugRows(
    info: PlaybackDebugInfo
): List<DebugStatRow> {
    val candidates = listOf(
        DebugStatRow("Resolution", info.resolution),
        DebugStatRow("Video bitrate", info.videoBitrate),
        DebugStatRow("Audio bitrate", info.audioBitrate),
        DebugStatRow("Video codec", info.videoCodec),
        DebugStatRow("Audio codec", info.audioCodec),
        DebugStatRow("Frame rate", info.frameRate),
        DebugStatRow("Video decoder", info.videoDecoder),
        DebugStatRow("Audio decoder", info.audioDecoder)
    )
    return candidates.filter { it.value.isNotBlank() }
}

internal fun resolveCenterPlaybackButtonStyle(
    isDarkTheme: Boolean
): CenterPlaybackButtonStyle {
    return if (isDarkTheme) {
        CenterPlaybackButtonStyle(
            containerColor = Color.Black.copy(alpha = 0.44f),
            innerColor = Color.White.copy(alpha = 0.16f),
            borderColor = Color.White.copy(alpha = 0.22f),
            iconTint = Color.White
        )
    } else {
        CenterPlaybackButtonStyle(
            containerColor = Color.Black.copy(alpha = 0.28f),
            innerColor = Color.Black.copy(alpha = 0.18f),
            borderColor = Color.Black.copy(alpha = 0.16f),
            iconTint = Color.White
        )
    }
}
