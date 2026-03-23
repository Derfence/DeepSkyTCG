package gatcha.aumombelli.fr.data

import android.content.Context
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import gatcha.aumombelli.fr.model.DrawPackResponse
import gatcha.aumombelli.fr.model.SessionCredentials
import gatcha.aumombelli.fr.model.StoredSessionSnapshot
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json

private val Context.dataStore by preferencesDataStore(name = "gatcha_preferences")

class SessionRepository(
    private val context: Context,
) {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val activeSession = MutableStateFlow<SessionCredentials?>(null)

    val activeSessionFlow: Flow<SessionCredentials?> = activeSession

    fun setActiveSession(username: String, passwordHash: String) {
        activeSession.value = SessionCredentials(username = username, passwordHash = passwordHash)
    }

    fun clearActiveSession() {
        activeSession.value = null
    }

    fun requireActiveSession(): SessionCredentials =
        checkNotNull(activeSession.value) { "No active session is available." }

    suspend fun readSnapshot(): StoredSessionSnapshot = context.dataStore.data.map { preferences ->
        StoredSessionSnapshot(
            lastUsername = preferences[Keys.lastUsername],
            lastCollectionBlob = preferences[Keys.lastCollectionBlob],
            pendingCollectionBlob = preferences[Keys.pendingCollectionBlob],
            pendingPackJson = preferences[Keys.pendingPackJson],
            nextDrawAt = preferences[Keys.nextDrawAt],
            lastSavedAt = preferences[Keys.lastSavedAt],
        )
    }.first()

    suspend fun saveLoginMetadata(username: String, lastSavedAt: String?, nextDrawAt: String?) {
        context.dataStore.edit { preferences ->
            preferences[Keys.lastUsername] = username
            setOrRemove(preferences, Keys.lastSavedAt, lastSavedAt)
            setOrRemove(preferences, Keys.nextDrawAt, nextDrawAt)
        }
    }

    suspend fun commitSavedCollection(collectionBlob: String, savedAt: String?, nextDrawAt: String?) {
        context.dataStore.edit { preferences ->
            preferences[Keys.lastCollectionBlob] = collectionBlob
            setOrRemove(preferences, Keys.lastSavedAt, savedAt)
            setOrRemove(preferences, Keys.nextDrawAt, nextDrawAt)
            preferences.remove(Keys.pendingCollectionBlob)
            preferences.remove(Keys.pendingPackJson)
        }
    }

    suspend fun savePendingPack(collectionBlob: String, packResponse: DrawPackResponse) {
        context.dataStore.edit { preferences ->
            preferences[Keys.pendingCollectionBlob] = collectionBlob
            preferences[Keys.pendingPackJson] = json.encodeToString(DrawPackResponse.serializer(), packResponse)
            preferences[Keys.nextDrawAt] = packResponse.nextDrawAt
        }
    }

    suspend fun clearPendingPack() {
        context.dataStore.edit { preferences ->
            preferences.remove(Keys.pendingCollectionBlob)
            preferences.remove(Keys.pendingPackJson)
        }
    }

    suspend fun decodePendingPack(): DrawPackResponse? {
        val snapshot = readSnapshot()
        return snapshot.pendingPackJson?.let {
            json.decodeFromString(DrawPackResponse.serializer(), it)
        }
    }

    private fun setOrRemove(preferences: MutablePreferences, key: Preferences.Key<String>, value: String?) {
        if (value == null) {
            preferences.remove(key)
        } else {
            preferences[key] = value
        }
    }

    private object Keys {
        val lastUsername = stringPreferencesKey("last_username")
        val lastCollectionBlob = stringPreferencesKey("last_collection_blob")
        val pendingCollectionBlob = stringPreferencesKey("pending_collection_blob")
        val pendingPackJson = stringPreferencesKey("pending_pack_json")
        val nextDrawAt = stringPreferencesKey("next_draw_at")
        val lastSavedAt = stringPreferencesKey("last_saved_at")
    }
}
