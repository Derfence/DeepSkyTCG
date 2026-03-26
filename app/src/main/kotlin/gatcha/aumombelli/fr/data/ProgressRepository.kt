package fr.aumombelli.gatcha.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import fr.aumombelli.gatcha.model.OwnedCollection
import fr.aumombelli.gatcha.model.StandaloneProgress
import kotlinx.coroutines.flow.first
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

internal val Context.standaloneProgressDataStore by preferencesDataStore(name = "gatcha_standalone_progress")

class ProgressRepository(
    private val dataStore: DataStore<Preferences>,
    private val collectionMigrationService: CollectionMigrationService,
) : ProgressGateway {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    override suspend fun loadProgress(): StandaloneProgress {
        val preferences = dataStore.data.first()
        val storedCollectionJson = preferences[Keys.collectionJson]
        val nextDrawAt = preferences[Keys.nextDrawAt]

        val originalCollection = storedCollectionJson?.let(::decodeCollection)
            ?: collectionMigrationService.emptyCollection()
        val migratedCollection = collectionMigrationService.migrateToCurrentVersion(originalCollection)
        val progress = StandaloneProgress(
            collection = migratedCollection,
            nextDrawAt = nextDrawAt,
        )

        if (storedCollectionJson == null || migratedCollection != originalCollection) {
            saveProgress(progress)
        }

        return progress
    }

    override suspend fun saveProgress(progress: StandaloneProgress) {
        dataStore.edit { preferences ->
            preferences[Keys.collectionJson] = json.encodeToString(OwnedCollection.serializer(), progress.collection)
            setOrRemove(preferences, Keys.nextDrawAt, progress.nextDrawAt)
        }
    }

    private fun decodeCollection(collectionJson: String): OwnedCollection =
        try {
            json.decodeFromString(OwnedCollection.serializer(), collectionJson)
        } catch (exception: SerializationException) {
            throw IllegalStateException("Saved progression could not be read.", exception)
        }

    private fun setOrRemove(preferences: androidx.datastore.preferences.core.MutablePreferences, key: Preferences.Key<String>, value: String?) {
        if (value == null) {
            preferences.remove(key)
        } else {
            preferences[key] = value
        }
    }

    private object Keys {
        val collectionJson = stringPreferencesKey("collection_json")
        val nextDrawAt = stringPreferencesKey("next_draw_at")
    }

    companion object {
        fun fromContext(
            context: Context,
            collectionMigrationService: CollectionMigrationService,
        ): ProgressRepository = ProgressRepository(
            dataStore = context.standaloneProgressDataStore,
            collectionMigrationService = collectionMigrationService,
        )
    }
}
