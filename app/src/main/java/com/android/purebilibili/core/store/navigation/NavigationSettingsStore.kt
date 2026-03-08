package com.android.purebilibili.core.store.navigation

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import com.android.purebilibili.core.store.AppNavigationSettings
import com.android.purebilibili.core.store.mapAppNavigationSettingsFromPreferences
import com.android.purebilibili.core.store.settingsDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

object NavigationSettingsStore {
    private val keyTabletUseSidebar = booleanPreferencesKey("tablet_use_sidebar")

    internal fun mapFromPreferences(preferences: Preferences): AppNavigationSettings {
        return mapAppNavigationSettingsFromPreferences(preferences)
    }

    fun observe(context: Context): Flow<AppNavigationSettings> {
        return context.settingsDataStore.data
            .map(::mapFromPreferences)
            .distinctUntilChanged()
    }

    suspend fun setTabletUseSidebar(context: Context, useSidebar: Boolean) {
        context.settingsDataStore.edit { preferences ->
            preferences[keyTabletUseSidebar] = useSidebar
        }
    }
}
