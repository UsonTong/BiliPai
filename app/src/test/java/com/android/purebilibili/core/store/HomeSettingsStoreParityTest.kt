package com.android.purebilibili.core.store

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.mutablePreferencesOf
import com.android.purebilibili.core.store.home.HomeSettingsStore
import kotlin.test.Test
import kotlin.test.assertEquals

class HomeSettingsStoreParityTest {

    @Test
    fun `home store maps defaults the same way as settings manager policy`() {
        val prefs = mutablePreferencesOf()

        assertEquals(
            mapHomeSettingsFromPreferences(prefs),
            HomeSettingsStore.mapFromPreferences(prefs)
        )
    }

    @Test
    fun `home store maps populated preferences the same way as settings manager policy`() {
        val prefs = mutablePreferencesOf(
            intPreferencesKey("display_mode") to 1,
            booleanPreferencesKey("bottom_bar_floating") to false,
            intPreferencesKey("bottom_bar_label_mode") to 2
        )

        assertEquals(
            mapHomeSettingsFromPreferences(prefs),
            HomeSettingsStore.mapFromPreferences(prefs)
        )
    }
}
