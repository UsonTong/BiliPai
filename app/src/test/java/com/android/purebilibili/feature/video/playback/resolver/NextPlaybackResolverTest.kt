package com.android.purebilibili.feature.video.playback.resolver

import com.android.purebilibili.feature.video.player.ExternalPlaylistSource
import kotlin.test.Test
import kotlin.test.assertEquals

class NextPlaybackResolverTest {

    @Test
    fun `external playlist should switch to playlist-first audio continuation`() {
        assertEquals(
            AudioNextPlaybackStrategy.PLAY_EXTERNAL_PLAYLIST,
            resolveAudioNextPlaybackStrategy(
                isExternalPlaylist = true,
                externalPlaylistSource = ExternalPlaylistSource.FAVORITE
            )
        )
    }

    @Test
    fun `non external session should keep page season related continuation`() {
        assertEquals(
            AudioNextPlaybackStrategy.PAGE_THEN_SEASON_THEN_RELATED,
            resolveAudioNextPlaybackStrategy(
                isExternalPlaylist = false,
                externalPlaylistSource = ExternalPlaylistSource.NONE
            )
        )
    }

    @Test
    fun `play in order should prefer page or season before playlist`() {
        assertEquals(
            PlayInOrderNextSource.PAGE_OR_SEASON,
            resolvePlayInOrderNextSource(
                hasNextPage = false,
                hasNextSeasonEpisode = true,
                hasNextPlaylistItem = true
            )
        )
    }

    @Test
    fun `play in order should fallback to playlist when collection has no next`() {
        assertEquals(
            PlayInOrderNextSource.PLAYLIST,
            resolvePlayInOrderNextSource(
                hasNextPage = false,
                hasNextSeasonEpisode = false,
                hasNextPlaylistItem = true
            )
        )
    }

    @Test
    fun `play in order should stop when no source has next`() {
        assertEquals(
            PlayInOrderNextSource.NONE,
            resolvePlayInOrderNextSource(
                hasNextPage = false,
                hasNextSeasonEpisode = false,
                hasNextPlaylistItem = false
            )
        )
    }
}
