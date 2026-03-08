package com.android.purebilibili.core.store

import com.android.purebilibili.core.store.player.PlayerSettingsStore
import kotlin.test.Test
import kotlin.test.assertEquals

class PlayerSettingsStoreParityTest {

    @Test
    fun `player store preferred speed resolution stays aligned`() {
        assertEquals(
            resolvePreferredPlaybackSpeed(
                defaultSpeed = 1.3f,
                rememberLastSpeed = true,
                lastSpeed = 1.8f
            ),
            PlayerSettingsStore.resolvePreferredPlaybackSpeed(
                defaultSpeed = 1.3f,
                rememberLastSpeed = true,
                lastSpeed = 1.8f
            )
        )
    }

    @Test
    fun `player store playback speed normalization stays aligned`() {
        assertEquals(
            normalizePlaybackSpeed(0.0f),
            PlayerSettingsStore.normalizePlaybackSpeed(0.0f)
        )
        assertEquals(
            normalizePlaybackSpeed(9.5f),
            PlayerSettingsStore.normalizePlaybackSpeed(9.5f)
        )
    }
}
