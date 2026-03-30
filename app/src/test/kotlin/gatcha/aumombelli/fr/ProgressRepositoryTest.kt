package fr.aumombelli.gatcha

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import fr.aumombelli.gatcha.data.CollectionMigrationService
import fr.aumombelli.gatcha.data.EncryptedProgressEnvelope
import fr.aumombelli.gatcha.data.EncryptedProgressEnvelopeSerializer
import fr.aumombelli.gatcha.data.ProgressLoadResult
import fr.aumombelli.gatcha.data.ProgressRepository
import fr.aumombelli.gatcha.data.ProgressSnapshot
import fr.aumombelli.gatcha.data.StandaloneGameSettings
import fr.aumombelli.gatcha.data.requireUsableProgress
import fr.aumombelli.gatcha.model.OwnedCardEntry
import fr.aumombelli.gatcha.model.OwnedCollection
import fr.aumombelli.gatcha.model.OwnedVariantCount
import fr.aumombelli.gatcha.model.StandaloneProgress
import java.time.Duration
import java.time.Instant
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ProgressRepositoryTest {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val fixedNow = Instant.parse("2026-03-24T12:00:00Z")

    @Test
    fun `first load creates an empty collection at the current catalog version`() = runTest {
        val fixture = newFixture()

        val result = fixture.repository.loadProgress()

        val loaded = result.requireUsableProgress()
        assertEquals(5, loaded.progress.collection.version)
        assertEquals(emptyMap<String, Int>(), loaded.progress.collection.cards.mapValues { it.value.totalOwned })
        assertEquals(10, loaded.progress.availableDrawCount)
        assertEquals(0, loaded.progress.openedPackCount)
        assertFalse(fixture.secureDataStore.data.first().isEmpty())
    }

    @Test
    fun `save progress persists encrypted snapshot`() = runTest {
        val fixture = newFixture()
        val progress = StandaloneProgress(
            collection = ownedCollectionOf("ALP-001" to 2).copy(version = 5),
            availableDrawCount = 4,
            nextChargeAt = "2026-03-25T00:00:00Z",
            openedPackCount = 3,
        )

        fixture.repository.saveProgress(progress)
        val reloaded = fixture.repository.loadProgress().requireUsableProgress()

        assertEquals(progress, reloaded.progress)
        val storedEnvelope = fixture.secureDataStore.data.first()
        assertFalse(storedEnvelope.isEmpty())
        assertFalse(storedEnvelope.ciphertextBase64.contains("ALP-001"))
    }

    @Test
    fun `load progress migrates legacy collection and rewrites secure storage`() = runTest {
        val fixture = newFixture()
        fixture.legacyDataStore.edit { preferences ->
            preferences[CollectionJsonKey] = json.encodeToString(
                OwnedCollection.serializer(),
                ownedCollectionOf("ALP-001" to 1).copy(version = 1),
            )
            preferences[AvailableDrawCountKey] = 4
            preferences[NextChargeAtKey] = "2026-03-25T00:00:00Z"
        }

        val result = fixture.repository.loadProgress()

        assertTrue(result is ProgressLoadResult.Recovered)
        val loaded = result.requireUsableProgress()
        assertEquals(5, loaded.progress.collection.version)
        assertEquals(4, loaded.progress.availableDrawCount)
        assertEquals("2026-03-25T00:00:00Z", loaded.progress.nextChargeAt)
        assertEquals(null, fixture.legacyDataStore.data.first()[CollectionJsonKey])
        assertFalse(fixture.secureDataStore.data.first().isEmpty())
    }

    @Test
    fun `load progress normalizes invalid cards and variants from secure storage`() = runTest {
        val fixture = newFixture()
        writeSecureSnapshot(
            fixture = fixture,
            snapshot = ProgressSnapshot(
                installId = "install-1",
                collection = OwnedCollection(
                    version = 5,
                    cards = mapOf(
                        "ALP-001" to OwnedCardEntry(
                            totalOwned = 99,
                            variants = listOf(
                                OwnedVariantCount("city", "standard", 2),
                                OwnedVariantCount("unknown-sky", "standard", 1),
                                OwnedVariantCount("city", "unknown-finish", 1),
                                OwnedVariantCount("city", "standard", -1),
                            ),
                        ),
                        "UNKNOWN" to OwnedCardEntry(
                            totalOwned = 1,
                            variants = listOf(OwnedVariantCount("city", "standard", 1)),
                        ),
                    ),
                ),
                availableDrawCount = 10,
                nextChargeAt = null,
                openedPackCount = 1,
                lastTrustedWallClockUtc = fixedNow.toString(),
                lastTrustedElapsedRealtimeMs = 1_000L,
                lastObservedBootMarker = "test-boot",
                tamperFlag = false,
            ),
        )

        val result = fixture.repository.loadProgress()

        assertTrue(result is ProgressLoadResult.Recovered)
        val loaded = result.requireUsableProgress().progress
        assertEquals(setOf("ALP-001"), loaded.collection.cards.keys)
        assertEquals(2, loaded.collection.cards.getValue("ALP-001").totalOwned)
        assertEquals(
            listOf(OwnedVariantCount("city", "standard", 2)),
            loaded.collection.cards.getValue("ALP-001").variants,
        )
    }

    @Test
    fun `load progress returns compromised when encrypted blob is altered`() = runTest {
        val fixture = newFixture()
        fixture.repository.saveProgress(
            StandaloneProgress(
                collection = ownedCollectionOf("ALP-001" to 1).copy(version = 5),
                availableDrawCount = 10,
                nextChargeAt = null,
            ),
        )
        fixture.secureDataStore.updateData { envelope ->
            envelope.copy(ciphertextBase64 = envelope.ciphertextBase64.dropLast(4) + "ABCD")
        }

        val result = fixture.repository.loadProgress()

        assertTrue(result is ProgressLoadResult.Compromised)
    }

    @Test
    fun `load progress resists same boot wall clock fast forward`() = runTest {
        val timeSource = MutableTrustedTimeSource(
            wallClockUtc = fixedNow,
            elapsedRealtimeMs = 1_000L,
            bootSessionId = "same-boot",
        )
        val fixture = newFixture(timeSource = timeSource)
        writeSecureSnapshot(
            fixture = fixture,
            snapshot = ProgressSnapshot(
                installId = "install-1",
                collection = ownedCollectionOf("ALP-001" to 1).copy(version = 5),
                availableDrawCount = 4,
                nextChargeAt = "2026-03-24T18:00:00Z",
                openedPackCount = 0,
                lastTrustedWallClockUtc = fixedNow.toString(),
                lastTrustedElapsedRealtimeMs = 1_000L,
                lastObservedBootMarker = "same-boot",
                tamperFlag = false,
            ),
        )
        timeSource.currentEvidence = timeSource.currentEvidence.copy(
            wallClockUtc = fixedNow.plus(Duration.ofDays(1)),
            elapsedRealtimeMs = 2_000L,
        )

        val result = fixture.repository.loadProgress()

        assertTrue(result is ProgressLoadResult.Recovered)
        val loaded = result.requireUsableProgress().progress
        assertEquals(4, loaded.availableDrawCount)
        assertEquals("2026-03-24T18:00:00Z", loaded.nextChargeAt)
    }

    @Test
    fun `load progress freezes recharge after reboot when wall clock moves backwards`() = runTest {
        val timeSource = MutableTrustedTimeSource(
            wallClockUtc = fixedNow,
            elapsedRealtimeMs = 1_000L,
            bootSessionId = "boot-a",
        )
        val fixture = newFixture(timeSource = timeSource)
        writeSecureSnapshot(
            fixture = fixture,
            snapshot = ProgressSnapshot(
                installId = "install-1",
                collection = ownedCollectionOf("ALP-001" to 1).copy(version = 5),
                availableDrawCount = 0,
                nextChargeAt = "2026-03-24T18:00:00Z",
                openedPackCount = 0,
                lastTrustedWallClockUtc = fixedNow.toString(),
                lastTrustedElapsedRealtimeMs = 1_000L,
                lastObservedBootMarker = "boot-a",
                tamperFlag = false,
            ),
        )
        timeSource.reboot(
            wallClockUtc = fixedNow.minus(Duration.ofHours(4)),
            elapsedRealtimeMs = 500L,
            bootSessionId = "boot-b",
        )

        val result = fixture.repository.loadProgress()

        assertTrue(result is ProgressLoadResult.Recovered)
        val loaded = result.requireUsableProgress()
        assertEquals(fixedNow, loaded.trustedNow)
        assertEquals(0, loaded.progress.availableDrawCount)
    }

    private fun newFixture(
        timeSource: MutableTrustedTimeSource = MutableTrustedTimeSource(
            wallClockUtc = fixedNow,
            elapsedRealtimeMs = 1_000L,
            bootSessionId = "test-boot",
        ),
    ): RepositoryFixture {
        val secureDataStore = inMemoryDataStore(EncryptedProgressEnvelopeSerializer.defaultValue)
        val legacyDataStore = inMemoryPreferencesDataStore()
        val cipher = newTestProgressCipher()
        return RepositoryFixture(
            secureDataStore = secureDataStore,
            legacyDataStore = legacyDataStore,
            cipher = cipher,
            repository = ProgressRepository(
                secureDataStore = secureDataStore,
                legacyDataStore = legacyDataStore,
                collectionMigrationService = CollectionMigrationService(FakeCatalogGateway()),
                catalogRepository = FakeCatalogGateway().apply {
                    cards = listOf(testCardDefinition("ALP-001", variantProfileId = "observation-default"))
                },
                settings = StandaloneGameSettings(
                    timeSource = timeSource,
                ),
                progressCipher = cipher,
                installIdFactory = { "install-under-test" },
            ),
        )
    }

    private suspend fun writeSecureSnapshot(
        fixture: RepositoryFixture,
        snapshot: ProgressSnapshot,
    ) {
        val payload = fixture.cipher.encrypt(
            json.encodeToString(ProgressSnapshot.serializer(), snapshot).encodeToByteArray(),
        )
        fixture.secureDataStore.updateData {
            EncryptedProgressEnvelope.fromPayload(payload)
        }
    }

    private data class RepositoryFixture(
        val secureDataStore: DataStore<EncryptedProgressEnvelope>,
        val legacyDataStore: DataStore<Preferences>,
        val cipher: fr.aumombelli.gatcha.data.ProgressCipher,
        val repository: ProgressRepository,
    )

    private companion object {
        val CollectionJsonKey = stringPreferencesKey("collection_json")
        val AvailableDrawCountKey = intPreferencesKey("available_draw_count")
        val NextChargeAtKey = stringPreferencesKey("next_charge_at")
    }
}
