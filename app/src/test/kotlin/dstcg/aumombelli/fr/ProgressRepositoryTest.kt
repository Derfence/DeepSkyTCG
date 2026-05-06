package fr.aumombelli.dstcg

import androidx.datastore.core.DataStore
import fr.aumombelli.dstcg.data.DeterministicWeatherCalendar
import fr.aumombelli.dstcg.data.EncryptedProgressEnvelope
import fr.aumombelli.dstcg.data.EncryptedProgressEnvelopeSerializer
import fr.aumombelli.dstcg.data.ProgressLoadResult
import fr.aumombelli.dstcg.data.ProgressRepository
import fr.aumombelli.dstcg.data.ProgressSnapshot
import fr.aumombelli.dstcg.data.StandaloneGameSettings
import fr.aumombelli.dstcg.data.buildPackChargeUiStatus
import fr.aumombelli.dstcg.data.drawCooldownDuration
import fr.aumombelli.dstcg.data.requireUsableProgress
import fr.aumombelli.dstcg.model.HomeMenuNoveltyState
import fr.aumombelli.dstcg.model.LibraryCardNoveltyState
import fr.aumombelli.dstcg.model.NewPlayerOnboardingStep
import fr.aumombelli.dstcg.model.OwnedCardEntry
import fr.aumombelli.dstcg.model.OwnedCollection
import fr.aumombelli.dstcg.model.OwnedVariantCount
import fr.aumombelli.dstcg.model.StandaloneProgress
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
    fun `first load creates an empty collection`() = runTest {
        val fixture = newFixture()

        val result = fixture.repository.loadProgress()

        val loaded = result.requireUsableProgress()
        assertEquals(OwnedCollection(), loaded.progress.collection)
        assertEquals(emptyMap<String, Int>(), loaded.progress.collection.cards.mapValues { it.value.totalOwned })
        assertEquals(10, loaded.progress.rechargeState.availableDrawCount)
        assertEquals(0, loaded.progress.openedPackCount)
        assertEquals(false, loaded.progress.hasOpenedEpicBoostedPack)
        assertEquals(NewPlayerOnboardingStep.ShowWelcomeIntro, loaded.progress.newPlayerOnboardingStep)
        assertFalse(fixture.secureDataStore.data.first().isEmpty())
    }

    @Test
    fun `save progress persists encrypted snapshot`() = runTest {
        val fixture = newFixture()
        val progress = StandaloneProgress(
            collection = ownedCollectionOf("ALP-001" to 2),
            rechargeState = testRechargeStateWithNextChargeAt(
                availableDrawCount = 4,
                nextChargeAt = "2026-03-25T00:00:00Z",
            ),
            openedPackCount = 3,
            hasOpenedEpicBoostedPack = true,
            newPlayerOnboardingStep = NewPlayerOnboardingStep.LearnLibraryVariants,
            homeMenuNoveltyState = HomeMenuNoveltyState(
                library = true,
                badgeBook = true,
            ),
            libraryCardNoveltyState = LibraryCardNoveltyState(
                newCardIds = setOf("ALP-001"),
            ),
        )

        fixture.repository.saveProgress(progress)
        val reloaded = fixture.repository.loadProgress().requireUsableProgress()

        assertEquals(progress, reloaded.progress)
        assertEquals(NewPlayerOnboardingStep.LearnLibraryVariants, reloaded.progress.newPlayerOnboardingStep)
        val storedEnvelope = fixture.secureDataStore.data.first()
        assertFalse(storedEnvelope.isEmpty())
        assertFalse(storedEnvelope.ciphertextBase64.contains("ALP-001"))
    }

    @Test
    fun `reset progress restores onboarding to first step`() = runTest {
        val fixture = newFixture()
        fixture.repository.saveProgress(
            StandaloneProgress(
                collection = ownedCollectionOf("ALP-001" to 1),
                rechargeState = testRechargeStateWithNextChargeAt(
                    availableDrawCount = 5,
                    nextChargeAt = "2026-03-25T00:00:00Z",
                ),
                openedPackCount = 1,
                hasOpenedEpicBoostedPack = true,
                newPlayerOnboardingStep = NewPlayerOnboardingStep.Completed,
            ),
        )

        fixture.repository.resetProgress()

        val reloaded = fixture.repository.loadProgress().requireUsableProgress().progress
        assertEquals(NewPlayerOnboardingStep.ShowWelcomeIntro, reloaded.newPlayerOnboardingStep)
        assertEquals(0, reloaded.openedPackCount)
        assertEquals(false, reloaded.hasOpenedEpicBoostedPack)
        assertEquals(OwnedCollection(), reloaded.collection)
        assertEquals(10, reloaded.rechargeState.availableDrawCount)
        assertEquals(HomeMenuNoveltyState(), reloaded.homeMenuNoveltyState)
        assertEquals(LibraryCardNoveltyState(), reloaded.libraryCardNoveltyState)
    }

    @Test
    fun `save progress persists crafting tools onboarding step`() = runTest {
        val fixture = newFixture()
        val progress = StandaloneProgress(
            collection = ownedCollectionOf("ALP-001" to 2),
            rechargeState = testRechargeStateWithNextChargeAt(
                availableDrawCount = 4,
                nextChargeAt = "2026-03-25T00:00:00Z",
            ),
            openedPackCount = 3,
            newPlayerOnboardingStep = NewPlayerOnboardingStep.LearnCraftingTools,
        )

        fixture.repository.saveProgress(progress)

        val reloaded = fixture.repository.loadProgress().requireUsableProgress().progress
        assertEquals(NewPlayerOnboardingStep.LearnCraftingTools, reloaded.newPlayerOnboardingStep)
    }

    @Test
    fun `load progress normalizes invalid cards and variants from secure storage`() = runTest {
        val fixture = newFixture()
        writeSecureSnapshot(
            fixture = fixture,
            snapshot = ProgressSnapshot(
                installId = "install-1",
                collection = OwnedCollection(
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
                rechargeState = testRechargeState(),
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
                collection = ownedCollectionOf("ALP-001" to 1),
                rechargeState = testRechargeState(),
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
                collection = ownedCollectionOf("ALP-001" to 1),
                rechargeState = testRechargeStateWithNextChargeAt(
                    availableDrawCount = 4,
                    nextChargeAt = "2026-03-24T18:00:00Z",
                    now = fixedNow,
                ),
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
        val loaded = result.requireUsableProgress()
        val chargeStatus = buildPackChargeUiStatus(
            rechargeState = loaded.progress.rechargeState,
            now = loaded.trustedNow,
            drawCooldown = fixture.gameBalance.drawCooldownDuration(),
            maxStoredDraws = fixture.settings.maxStoredDraws,
            weatherPolicy = DeterministicWeatherCalendar,
        )
        assertEquals(4, loaded.progress.rechargeState.availableDrawCount)
        assertEquals("2026-03-24T18:00:00Z", chargeStatus.nextChargeAt)
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
                collection = ownedCollectionOf("ALP-001" to 1),
                rechargeState = testRechargeStateWithNextChargeAt(
                    availableDrawCount = 0,
                    nextChargeAt = "2026-03-24T18:00:00Z",
                    now = fixedNow,
                ),
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
        val chargeStatus = buildPackChargeUiStatus(
            rechargeState = loaded.progress.rechargeState,
            now = loaded.trustedNow,
            drawCooldown = fixture.gameBalance.drawCooldownDuration(),
            maxStoredDraws = fixture.settings.maxStoredDraws,
            weatherPolicy = DeterministicWeatherCalendar,
        )
        assertEquals(fixedNow, loaded.trustedNow)
        assertEquals(0, loaded.progress.rechargeState.availableDrawCount)
        assertEquals("2026-03-24T18:00:00Z", chargeStatus.nextChargeAt)
    }

    @Test
    fun `load progress migrates schema version 4 to version 6 without losing collection or recharge`() = runTest {
        val fixture = newFixture()
        val legacyRechargeState = testRechargeStateWithNextChargeAt(
            availableDrawCount = 4,
            nextChargeAt = "2026-03-24T18:00:00Z",
            now = fixedNow,
        )
        val legacySnapshotJson = """
            {
              "installId":"install-1",
              "schemaVersion":4,
              "collection":{
                "cards":{
                  "ALP-001":{
                    "totalOwned":2,
                    "variants":[
                      {"skyQuality":"city","finish":"standard","count":2}
                    ]
                  }
                }
              },
              "rechargeState":{
                "availableDrawCount":${legacyRechargeState.availableDrawCount},
                "accumulatedChargeUnits":${legacyRechargeState.accumulatedChargeUnits},
                "lastChargeEvaluationAt":"${legacyRechargeState.lastChargeEvaluationAt}"
              },
              "openedPackCount":2,
              "newPlayerOnboardingStep":"Completed",
              "equipmentInventory":{"cards":{}},
              "activeEquipmentByType":{},
              "lastActivatedCardIdByType":{},
              "lastTrustedWallClockUtc":"${fixedNow}",
              "lastTrustedElapsedRealtimeMs":1000,
              "lastObservedBootMarker":"test-boot",
              "tamperFlag":false
            }
        """.trimIndent()
        val payload = fixture.cipher.encrypt(legacySnapshotJson.encodeToByteArray())
        fixture.secureDataStore.updateData {
            EncryptedProgressEnvelope.fromPayload(payload)
        }

        val result = fixture.repository.loadProgress()

        assertTrue(result is ProgressLoadResult.Recovered)
        val loaded = result.requireUsableProgress().progress
        assertEquals(2, loaded.collection.cards.getValue("ALP-001").totalOwned)
        assertEquals(4, loaded.rechargeState.availableDrawCount)
        assertEquals(2, loaded.openedPackCount)
        assertEquals(false, loaded.hasOpenedEpicBoostedPack)
        assertTrue(loaded.equipmentInventory.cards.isEmpty())
        assertTrue(loaded.activeEquipmentByType.isEmpty())
        assertTrue(loaded.lastActivatedCardIdByType.isEmpty())
        assertEquals(HomeMenuNoveltyState(), loaded.homeMenuNoveltyState)
        assertEquals(LibraryCardNoveltyState(), loaded.libraryCardNoveltyState)
    }

    private fun newFixture(
        timeSource: MutableTrustedTimeSource = MutableTrustedTimeSource(
            wallClockUtc = fixedNow,
            elapsedRealtimeMs = 1_000L,
            bootSessionId = "test-boot",
        ),
    ): RepositoryFixture {
        val secureDataStore = inMemoryDataStore(EncryptedProgressEnvelopeSerializer.defaultValue)
        val cipher = newTestProgressCipher()
        val settings = StandaloneGameSettings(
            timeSource = timeSource,
        )
        val catalogGateway = FakeCatalogGateway().apply {
            cards = listOf(testCardDefinition("ALP-001", variantProfileId = "observation-default"))
        }
        return RepositoryFixture(
            secureDataStore = secureDataStore,
            cipher = cipher,
            settings = settings,
            repository = ProgressRepository(
                secureDataStore = secureDataStore,
                catalogRepository = catalogGateway,
                settings = settings,
                progressCipher = cipher,
                installIdFactory = { "install-under-test" },
            ),
            gameBalance = catalogGateway.gameBalance,
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
        val cipher: fr.aumombelli.dstcg.data.ProgressCipher,
        val settings: StandaloneGameSettings,
        val repository: ProgressRepository,
        val gameBalance: fr.aumombelli.dstcg.model.GameBalanceDefinition,
    )
}
