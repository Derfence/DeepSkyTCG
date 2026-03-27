package fr.aumombelli.gatcha.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
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
    private val settings: StandaloneGameSettings = StandaloneGameSettings(),
) : ProgressGateway {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    override suspend fun loadProgress(): StandaloneProgress {
        val preferences = dataStore.data.first()
        val storedCollectionJson = preferences[Keys.collectionJson]
        val availableDrawCount = preferences[Keys.availableDrawCount] ?: settings.maxStoredDraws
        val nextChargeAt = preferences[Keys.nextChargeAt]

        val originalCollection = storedCollectionJson?.let(::decodeCollection)
            ?: collectionMigrationService.emptyCollection()
        val migratedCollection = collectionMigrationService.migrateToCurrentVersion(originalCollection)
        val progress = StandaloneProgress(
            collection = migratedCollection,
            availableDrawCount = availableDrawCount,
            nextChargeAt = nextChargeAt,
        ).withNormalizedPackCharge(
            now = settings.clock.instant(),
            drawCooldown = settings.drawCooldown,
            maxStoredDraws = settings.maxStoredDraws,
        )

        if (
            storedCollectionJson == null ||
            migratedCollection != originalCollection ||
            availableDrawCount != progress.availableDrawCount ||
            nextChargeAt != progress.nextChargeAt ||
            preferences[Keys.legacyNextDrawAt] != null
        ) {
            saveProgress(progress)
        }

        return progress
    }

    override suspend fun saveProgress(progress: StandaloneProgress) {
        val normalizedProgress = progress.withNormalizedPackCharge(
            now = settings.clock.instant(),
            drawCooldown = settings.drawCooldown,
            maxStoredDraws = settings.maxStoredDraws,
        )
        dataStore.edit { preferences ->
            preferences[Keys.collectionJson] = json.encodeToString(OwnedCollection.serializer(), normalizedProgress.collection)
            preferences[Keys.availableDrawCount] = normalizedProgress.availableDrawCount
            setOrRemove(preferences, Keys.nextChargeAt, normalizedProgress.nextChargeAt)
            preferences.remove(Keys.legacyNextDrawAt)
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
        val availableDrawCount = intPreferencesKey("available_draw_count")
        val nextChargeAt = stringPreferencesKey("next_charge_at")
        val legacyNextDrawAt = stringPreferencesKey("next_draw_at")
    }

    companion object {
        fun fromContext(
            context: Context,
            collectionMigrationService: CollectionMigrationService,
            settings: StandaloneGameSettings = StandaloneGameSettings(),
        ): ProgressRepository = ProgressRepository(
            dataStore = context.standaloneProgressDataStore,
            collectionMigrationService = collectionMigrationService,
            settings = settings,
        )
    }
}
