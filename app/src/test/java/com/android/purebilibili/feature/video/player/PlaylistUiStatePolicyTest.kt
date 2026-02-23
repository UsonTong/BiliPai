package com.android.purebilibili.feature.video.player

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PlaylistUiStatePolicyTest {

    @Test
    fun resolvePlaylistUiState_mapsAllFields() {
        val playlist = listOf(
            PlaylistItem(
                bvid = "BV1xx411c7mD",
                title = "video",
                cover = "cover",
                owner = "owner"
            )
        )

        val result = resolvePlaylistUiState(
            playMode = PlayMode.SHUFFLE,
            playlist = playlist,
            currentIndex = 0,
            isExternalPlaylist = true,
            externalPlaylistSource = ExternalPlaylistSource.WATCH_LATER
        )

        assertEquals(PlayMode.SHUFFLE, result.playMode)
        assertEquals(playlist, result.playlist)
        assertEquals(0, result.currentIndex)
        assertTrue(result.isExternalPlaylist)
        assertEquals(ExternalPlaylistSource.WATCH_LATER, result.externalPlaylistSource)
    }

    @Test
    fun resolvePlaylistUiState_defaultsRemainStable() {
        val result = resolvePlaylistUiState(
            playMode = PlayMode.SEQUENTIAL,
            playlist = emptyList(),
            currentIndex = -1,
            isExternalPlaylist = false,
            externalPlaylistSource = ExternalPlaylistSource.NONE
        )

        assertEquals(PlayMode.SEQUENTIAL, result.playMode)
        assertEquals(emptyList(), result.playlist)
        assertEquals(-1, result.currentIndex)
        assertFalse(result.isExternalPlaylist)
        assertEquals(ExternalPlaylistSource.NONE, result.externalPlaylistSource)
    }
}
