package com.example.pokeplanner.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

private val Context.settingsStore by preferencesDataStore("settings")

data class Settings(val dark: Boolean = false, val onlyPlanned: Boolean = false, val refreshHours: Int = 12)

interface SettingsSource {
    val flow: Flow<Settings>
    suspend fun dark(value: Boolean)
    suspend fun planned(value: Boolean)
    suspend fun hours(value: Int)
}

@Singleton
class SettingsRepository @Inject constructor(@ApplicationContext private val context: Context) : SettingsSource {
    private val darkKey = booleanPreferencesKey("dark")
    private val plannedKey = booleanPreferencesKey("planned")
    private val hoursKey = intPreferencesKey("refresh_hours")

    override val flow = context.settingsStore.data.catch { error ->
        if (error is IOException) emit(emptyPreferences()) else throw error
    }.map { preferences ->
        Settings(
            dark = preferences[darkKey] ?: false,
            onlyPlanned = preferences[plannedKey] ?: false,
            refreshHours = preferences[hoursKey]?.takeIf { it in CachePolicy.allowedHours } ?: 12,
        )
    }

    override suspend fun dark(value: Boolean) {
        context.settingsStore.edit { it[darkKey] = value }
    }

    override suspend fun planned(value: Boolean) {
        context.settingsStore.edit { it[plannedKey] = value }
    }

    override suspend fun hours(value: Int) {
        CachePolicy.validateHours(value)
        context.settingsStore.edit { it[hoursKey] = value }
    }
}
