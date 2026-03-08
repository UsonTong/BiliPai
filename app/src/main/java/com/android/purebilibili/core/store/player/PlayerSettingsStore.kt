package com.android.purebilibili.core.store.player

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import com.android.purebilibili.core.store.resolvePreferredPlaybackSpeed as resolvePreferredPlaybackSpeedPolicy
import com.android.purebilibili.core.store.normalizePlaybackSpeed as normalizePlaybackSpeedPolicy
import com.android.purebilibili.core.store.settingsDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

object PlayerSettingsStore {
    private val keyDefaultPlaybackSpeed = floatPreferencesKey("default_playback_speed")
    private val keyRememberLastPlaybackSpeed = booleanPreferencesKey("remember_last_playback_speed")
    private val keyLastPlaybackSpeed = floatPreferencesKey("last_playback_speed")
    private const val playbackSpeedCachePrefs = "playback_speed_cache"
    private const val cacheKeyDefaultPlaybackSpeed = "default_speed"
    private const val cacheKeyRememberLastSpeed = "remember_last_speed"
    private const val cacheKeyLastPlaybackSpeed = "last_speed"

    fun normalizePlaybackSpeed(speed: Float): Float {
        return normalizePlaybackSpeedPolicy(speed)
    }

    fun resolvePreferredPlaybackSpeed(
        defaultSpeed: Float,
        rememberLastSpeed: Boolean,
        lastSpeed: Float
    ): Float {
        return resolvePreferredPlaybackSpeedPolicy(
            defaultSpeed = defaultSpeed,
            rememberLastSpeed = rememberLastSpeed,
            lastSpeed = lastSpeed
        )
    }

    fun getDefaultPlaybackSpeed(context: Context): Flow<Float> = context.settingsDataStore.data
        .map { preferences -> normalizePlaybackSpeed(preferences[keyDefaultPlaybackSpeed] ?: 1.0f) }

    suspend fun setDefaultPlaybackSpeed(context: Context, speed: Float) {
        val normalized = normalizePlaybackSpeed(speed)
        context.settingsDataStore.edit { preferences ->
            preferences[keyDefaultPlaybackSpeed] = normalized
        }
        context.getSharedPreferences(playbackSpeedCachePrefs, Context.MODE_PRIVATE)
            .edit()
            .putFloat(cacheKeyDefaultPlaybackSpeed, normalized)
            .apply()
    }

    fun getRememberLastPlaybackSpeed(context: Context): Flow<Boolean> = context.settingsDataStore.data
        .map { preferences -> preferences[keyRememberLastPlaybackSpeed] ?: false }

    suspend fun setRememberLastPlaybackSpeed(context: Context, enabled: Boolean) {
        context.settingsDataStore.edit { preferences ->
            preferences[keyRememberLastPlaybackSpeed] = enabled
        }
        context.getSharedPreferences(playbackSpeedCachePrefs, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(cacheKeyRememberLastSpeed, enabled)
            .apply()
    }

    fun getLastPlaybackSpeed(context: Context): Flow<Float> = context.settingsDataStore.data
        .map { preferences -> normalizePlaybackSpeed(preferences[keyLastPlaybackSpeed] ?: 1.0f) }

    suspend fun setLastPlaybackSpeed(context: Context, speed: Float) {
        val normalized = normalizePlaybackSpeed(speed)
        context.settingsDataStore.edit { preferences ->
            preferences[keyLastPlaybackSpeed] = normalized
        }
        context.getSharedPreferences(playbackSpeedCachePrefs, Context.MODE_PRIVATE)
            .edit()
            .putFloat(cacheKeyLastPlaybackSpeed, normalized)
            .apply()
    }

    fun getPreferredPlaybackSpeed(context: Context): Flow<Float> = combine(
        getDefaultPlaybackSpeed(context),
        getRememberLastPlaybackSpeed(context),
        getLastPlaybackSpeed(context)
    ) { defaultSpeed, rememberLast, lastSpeed ->
        resolvePreferredPlaybackSpeed(
            defaultSpeed = defaultSpeed,
            rememberLastSpeed = rememberLast,
            lastSpeed = lastSpeed
        )
    }

    fun getPreferredPlaybackSpeedSync(context: Context): Float {
        val prefs = context.getSharedPreferences(playbackSpeedCachePrefs, Context.MODE_PRIVATE)
        val defaultSpeed = normalizePlaybackSpeed(prefs.getFloat(cacheKeyDefaultPlaybackSpeed, 1.0f))
        val rememberLast = prefs.getBoolean(cacheKeyRememberLastSpeed, false)
        val lastSpeed = normalizePlaybackSpeed(prefs.getFloat(cacheKeyLastPlaybackSpeed, 1.0f))
        return resolvePreferredPlaybackSpeed(
            defaultSpeed = defaultSpeed,
            rememberLastSpeed = rememberLast,
            lastSpeed = lastSpeed
        )
    }
}
