package fr.aumombelli.gatcha

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import fr.aumombelli.gatcha.data.CollectionMigrationService
import fr.aumombelli.gatcha.data.ProgressRepository
import fr.aumombelli.gatcha.model.OwnedCollection
import fr.aumombelli.gatcha.model.StandaloneProgress
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

@OptIn(ExperimentalCoroutinesApi::class)
class ProgressRepositoryTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    @Test
    fun `first load creates an empty collection at the current catalog version`() = runTest {
        val dataStore = newDataStore(backgroundScope, "fresh.preferences_pb")
        val repository = newRepository(dataStore)

        val progress = repository.loadProgress()

        assertEquals(5, progress.collection.version)
        assertEquals(emptyMap<String, Int>(), progress.collection.cards.mapValues { it.value.totalOwned })
        assertEquals(
            5,
            decodeStoredCollection(dataStore).version,
        )
    }

    @Test
    fun `save progress persists collection and next draw timestamp`() = runTest {
        val dataStore = newDataStore(backgroundScope, "saved.preferences_pb")
        val repository = newRepository(dataStore)
        val progress = StandaloneProgress(
            collection = ownedCollectionOf("ALP-001" to 2).copy(version = 5),
            nextDrawAt = "2026-03-25T00:00:00Z",
        )

        repository.saveProgress(progress)
        val reloaded = repository.loadProgress()

        assertEquals(progress, reloaded)
        assertEquals("2026-03-25T00:00:00Z", dataStore.data.first()[NextDrawAtKey])
    }

    @Test
    fun `load progress migrates legacy collection and rewrites stored json`() = runTest {
        val dataStore = newDataStore(backgroundScope, "migrated.preferences_pb")
        dataStore.edit { preferences ->
            preferences[CollectionJsonKey] = json.encodeToString(
                OwnedCollection.serializer(),
                ownedCollectionOf("ALP-001" to 1).copy(version = 1),
            )
            preferences[NextDrawAtKey] = "2026-03-25T00:00:00Z"
        }
        val repository = newRepository(dataStore)

        val progress = repository.loadProgress()

        assertEquals(5, progress.collection.version)
        assertEquals("2026-03-25T00:00:00Z", progress.nextDrawAt)
        assertEquals(5, decodeStoredCollection(dataStore).version)
    }

    private fun newRepository(
        dataStore: androidx.datastore.core.DataStore<androidx.datastore.preferences.core.Preferences>,
    ): ProgressRepository = ProgressRepository(
        dataStore = dataStore,
        collectionMigrationService = CollectionMigrationService(FakeCatalogGateway()),
    )

    private fun newDataStore(
        scope: CoroutineScope,
        name: String,
    ) = PreferenceDataStoreFactory.create(
        scope = scope,
        produceFile = { persistentFile(name) },
    )

    private fun persistentFile(name: String): File = temporaryFolder.newFile(name)

    private suspend fun decodeStoredCollection(
        dataStore: androidx.datastore.core.DataStore<androidx.datastore.preferences.core.Preferences>,
    ): OwnedCollection = json.decodeFromString(
        OwnedCollection.serializer(),
        checkNotNull(dataStore.data.first()[CollectionJsonKey]),
    )

    private companion object {
        val CollectionJsonKey = stringPreferencesKey("collection_json")
        val NextDrawAtKey = stringPreferencesKey("next_draw_at")
    }
}
