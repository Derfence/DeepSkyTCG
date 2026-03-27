package fr.aumombelli.gatcha

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import fr.aumombelli.gatcha.data.CollectionMigrationService
import fr.aumombelli.gatcha.data.ProgressRepository
import fr.aumombelli.gatcha.data.StandaloneGameSettings
import fr.aumombelli.gatcha.model.OwnedCollection
import fr.aumombelli.gatcha.model.StandaloneProgress
import java.io.File
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
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
    private val fixedNow = Instant.parse("2026-03-24T12:00:00Z")

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
        assertEquals(10, dataStore.data.first()[AvailableDrawCountKey])
    }

    @Test
    fun `save progress persists collection and charge state`() = runTest {
        val dataStore = newDataStore(backgroundScope, "saved.preferences_pb")
        val repository = newRepository(dataStore)
        val progress = StandaloneProgress(
            collection = ownedCollectionOf("ALP-001" to 2).copy(version = 5),
            availableDrawCount = 4,
            nextChargeAt = "2026-03-25T00:00:00Z",
        )

        repository.saveProgress(progress)
        val reloaded = repository.loadProgress()

        assertEquals(progress, reloaded)
        assertEquals(4, dataStore.data.first()[AvailableDrawCountKey])
        assertEquals("2026-03-25T00:00:00Z", dataStore.data.first()[NextChargeAtKey])
    }

    @Test
    fun `load progress migrates legacy collection and rewrites stored json`() = runTest {
        val dataStore = newDataStore(backgroundScope, "migrated.preferences_pb")
        dataStore.edit { preferences ->
            preferences[CollectionJsonKey] = json.encodeToString(
                OwnedCollection.serializer(),
                ownedCollectionOf("ALP-001" to 1).copy(version = 1),
            )
            preferences[AvailableDrawCountKey] = 4
            preferences[NextChargeAtKey] = "2026-03-25T00:00:00Z"
        }
        val repository = newRepository(dataStore)

        val progress = repository.loadProgress()

        assertEquals(5, progress.collection.version)
        assertEquals(4, progress.availableDrawCount)
        assertEquals("2026-03-25T00:00:00Z", progress.nextChargeAt)
        assertEquals(5, decodeStoredCollection(dataStore).version)
    }

    @Test
    fun `load progress replenishes charges from elapsed cooldowns and caps at ten`() = runTest {
        val dataStore = newDataStore(backgroundScope, "recharged.preferences_pb")
        dataStore.edit { preferences ->
            preferences[CollectionJsonKey] = json.encodeToString(
                OwnedCollection.serializer(),
                ownedCollectionOf("ALP-001" to 1).copy(version = 5),
            )
            preferences[AvailableDrawCountKey] = 7
            preferences[NextChargeAtKey] = "2026-03-24T00:00:00Z"
        }
        val repository = newRepository(dataStore)

        val progress = repository.loadProgress()

        assertEquals(10, progress.availableDrawCount)
        assertEquals(null, progress.nextChargeAt)
        assertEquals(10, dataStore.data.first()[AvailableDrawCountKey])
        assertEquals(null, dataStore.data.first()[NextChargeAtKey])
    }

    private fun newRepository(
        dataStore: androidx.datastore.core.DataStore<androidx.datastore.preferences.core.Preferences>,
    ): ProgressRepository = ProgressRepository(
        dataStore = dataStore,
        collectionMigrationService = CollectionMigrationService(FakeCatalogGateway()),
        settings = StandaloneGameSettings(
            clock = Clock.fixed(fixedNow, ZoneOffset.UTC),
        ),
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
        val AvailableDrawCountKey = intPreferencesKey("available_draw_count")
        val NextChargeAtKey = stringPreferencesKey("next_charge_at")
    }
}
