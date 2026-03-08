package com.android.purebilibili.feature.video.playback.resolver

import com.android.purebilibili.feature.video.player.ExternalPlaylistSource

internal enum class AudioNextPlaybackStrategy {
    PLAY_EXTERNAL_PLAYLIST,
    PAGE_THEN_SEASON_THEN_RELATED
}

internal enum class PlayInOrderNextSource {
    PAGE_OR_SEASON,
    PLAYLIST,
    NONE
}

internal fun resolvePlayInOrderNextSource(
    hasNextPage: Boolean,
    hasNextSeasonEpisode: Boolean,
    hasNextPlaylistItem: Boolean
): PlayInOrderNextSource {
    return when {
        hasNextPage || hasNextSeasonEpisode -> PlayInOrderNextSource.PAGE_OR_SEASON
        hasNextPlaylistItem -> PlayInOrderNextSource.PLAYLIST
        else -> PlayInOrderNextSource.NONE
    }
}

internal fun resolveAudioNextPlaybackStrategy(
    isExternalPlaylist: Boolean,
    externalPlaylistSource: ExternalPlaylistSource
): AudioNextPlaybackStrategy {
    if (!isExternalPlaylist || externalPlaylistSource == ExternalPlaylistSource.NONE) {
        return AudioNextPlaybackStrategy.PAGE_THEN_SEASON_THEN_RELATED
    }
    return AudioNextPlaybackStrategy.PLAY_EXTERNAL_PLAYLIST
}
