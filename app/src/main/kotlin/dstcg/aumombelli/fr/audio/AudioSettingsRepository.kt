package fr.aumombelli.dstcg.audio

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStore
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

private val AudioEnabledKey = booleanPreferencesKey("audio_enabled")

val Context.audioSettingsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "dstcg_audio_settings",
)

class AudioSettingsRepository(
    private val dataStore: DataStore<Preferences>,
) {
    val settings: Flow<AudioSettings> = dataStore.data
        .catch { error ->
            if (error is IOException) {
                emit(emptyPreferences())
            } else {
                throw error
            }
        }
        .map { preferences ->
            AudioSettings(
                enabled = preferences[AudioEnabledKey] ?: true,
            )
        }

    suspend fun setEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[AudioEnabledKey] = enabled
        }
    }
}
