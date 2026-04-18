package fr.aumombelli.dstcg

import fr.aumombelli.dstcg.data.CollectionRepository
import fr.aumombelli.dstcg.data.HomeMenuNoveltyEvaluator
import fr.aumombelli.dstcg.data.LocalPackEngine
import fr.aumombelli.dstcg.data.PackRepository
import fr.aumombelli.dstcg.model.ActiveEquipmentEffect
import fr.aumombelli.dstcg.model.AstronomyPackRevealSlot
import fr.aumombelli.dstcg.model.CardFinishDefinition
import fr.aumombelli.dstcg.model.EquipmentSettingsDefinition
import fr.aumombelli.dstcg.model.EquipmentType
import fr.aumombelli.dstcg.model.HomeMenuNoveltyState
import fr.aumombelli.dstcg.model.LibraryCardNoveltyState
import fr.aumombelli.dstcg.model.NewPlayerOnboardingStep
import fr.aumombelli.dstcg.model.SkyQualityDefinition
import fr.aumombelli.dstcg.model.StandaloneProgress
import fr.aumombelli.dstcg.model.VariantProfile
import fr.aumombelli.dstcg.model.entryFor
import java.time.Instant
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class PackRepositoryTest {
    @Test
    fun `open pack persists merged collection pack count next draw timestamp and current pack result`() = runTest {
        val fixedNow = Instant.parse("2026-03-24T12:00:00Z")
        val progressGateway = FakeProgressGateway().apply {
            progress = StandaloneProgress(
                collection = ownedCollectionOf("ALP-001" to 1),
                rechargeState = testRechargeState(),
            )
        }
        val catalogGateway = FakeCatalogGateway().apply {
            cards = listOf(
                testCardDefinition("ALP-001", name = "Nebuleuse d'Orion", variantProfileId = "local-pack-profile"),
                testCardDefinition("ALP-002", name = "Galaxie d'Andromede", variantProfileId = "local-pack-profile"),
            )
            variantProfiles = listOf(localPackProfile())
            gameBalance = testGameBalanceDefinition(
                cardsPerDraw = 2,
                suburbanMeanPerDay = 1.0,
                ruralMeanPerDay = 1.0,
                mountainMeanPerDay = 1.0,
            )
        }
        val repository = PackRepository(
            progressRepository = progressGateway,
            collectionRepository = CollectionRepository(progressGateway),
            localPackEngine = LocalPackEngine(
                catalogRepository = catalogGateway,
                settings = testGameSettings(
                    now = fixedNow,
                    maxStoredDraws = 10,
                    randomSeed = 4,
                ),
            ),
            homeMenuNoveltyEvaluator = HomeMenuNoveltyEvaluator(catalogGateway),
        )

        val response = repository.openPack("astronomes-en-herbe")
        val astronomySlotsByCardId = response.revealSlots
            .filterIsInstance<AstronomyPackRevealSlot>()
            .associate { it.card.cardId to it.isFirstEncounter }

        assertEquals(response, repository.currentPackResult().value)
        assertEquals(response.rechargeState, progressGateway.progress.rechargeState)
        assertEquals(1, progressGateway.progress.openedPackCount)
        assertEquals(3, progressGateway.progress.collection.cards.values.sumOf { it.totalOwned })
        assertEquals(
            mapOf(
                "ALP-001" to false,
                "ALP-002" to true,
            ),
            astronomySlotsByCardId,
        )
        assertEquals(true, progressGateway.progress.homeMenuNoveltyState.library)
        assertEquals(false, progressGateway.progress.homeMenuNoveltyState.equipment)
        assertEquals(true, progressGateway.progress.homeMenuNoveltyState.badgeBook)
        assertEquals(
            LibraryCardNoveltyState(
                newCardIds = setOf("ALP-002"),
            ),
            progressGateway.progress.libraryCardNoveltyState,
        )
    }

    @Test
    fun `open pack stores equipment rewards and expires active effects after use`() = runTest {
        val fixedNow = Instant.parse("2026-03-24T12:00:00Z")
        val rewardCard = testEquipmentCardDefinition(
            id = "mount-beginner",
            type = EquipmentType.Mount,
            packsAffected = 1,
            dropWeight = 1,
        )
        val progressGateway = FakeProgressGateway().apply {
            progress = StandaloneProgress(
                collection = ownedCollectionOf("ALP-001" to 1),
                rechargeState = testRechargeState(),
                newPlayerOnboardingStep = NewPlayerOnboardingStep.Completed,
                activeEquipmentByType = mapOf(
                    EquipmentType.Mount to ActiveEquipmentEffect(
                        equipmentCardId = rewardCard.id,
                        equipmentType = EquipmentType.Mount,
                        packsRemaining = 1,
                    ),
                ),
            )
        }
        val catalogGateway = FakeCatalogGateway().apply {
            cards = listOf(
                testCardDefinition("ALP-001", name = "Nebuleuse d'Orion", variantProfileId = "local-pack-profile"),
            )
            variantProfiles = listOf(localPackProfile())
            gameBalance = testGameBalanceDefinition(
                cardsPerDraw = 1,
                suburbanMeanPerDay = 1.0,
                ruralMeanPerDay = 1.0,
                mountainMeanPerDay = 1.0,
            )
            equipmentCards = listOf(rewardCard)
            equipmentSettings = EquipmentSettingsDefinition(
                commonReplacementChancePercent = 100.0,
            )
        }
        val repository = PackRepository(
            progressRepository = progressGateway,
            collectionRepository = CollectionRepository(progressGateway),
            localPackEngine = LocalPackEngine(
                catalogRepository = catalogGateway,
                settings = testGameSettings(
                    now = fixedNow,
                    maxStoredDraws = 10,
                    randomSeed = 0,
                ),
            ),
            homeMenuNoveltyEvaluator = HomeMenuNoveltyEvaluator(catalogGateway),
        )

        val response = repository.openPack("astronomes-en-herbe")

        assertEquals(listOf(rewardCard.id), response.equipmentCards.map { it.id })
        assertEquals(1, progressGateway.progress.equipmentInventory.entryFor(rewardCard.id).countOwned)
        assertFalse(progressGateway.progress.activeEquipmentByType.containsKey(EquipmentType.Mount))
        assertEquals(1, progressGateway.progress.collection.cards.values.sumOf { it.totalOwned })
        assertEquals(true, progressGateway.progress.homeMenuNoveltyState.equipment)
    }

    @Test
    fun `open pack keeps pack size stable and stores forced onboarding equipment on second pack`() = runTest {
        val fixedNow = Instant.parse("2026-03-24T12:00:00Z")
        val rewardCard = testEquipmentCardDefinition(
            id = "observatory-lv1",
            type = EquipmentType.Observatory,
            level = 1,
            dropWeight = 1,
        )
        val progressGateway = FakeProgressGateway().apply {
            progress = StandaloneProgress(
                collection = ownedCollectionOf(),
                rechargeState = testRechargeState(),
                openedPackCount = 1,
                newPlayerOnboardingStep = NewPlayerOnboardingStep.OpenSecondPackMenu,
            )
        }
        val catalogGateway = FakeCatalogGateway().apply {
            cards = listOf(
                testCardDefinition("ALP-001", name = "Nebuleuse d'Orion", variantProfileId = "local-pack-profile"),
            )
            variantProfiles = listOf(localPackProfile())
            gameBalance = testGameBalanceDefinition(
                cardsPerDraw = 2,
                suburbanMeanPerDay = 1.0,
                ruralMeanPerDay = 1.0,
                mountainMeanPerDay = 1.0,
            )
            equipmentCards = listOf(rewardCard)
            equipmentSettings = EquipmentSettingsDefinition(
                commonReplacementChancePercent = 100.0,
            )
        }
        val repository = PackRepository(
            progressRepository = progressGateway,
            collectionRepository = CollectionRepository(progressGateway),
            localPackEngine = LocalPackEngine(
                catalogRepository = catalogGateway,
                settings = testGameSettings(
                    now = fixedNow,
                    maxStoredDraws = 10,
                    randomSeed = 0,
                ),
            ),
            homeMenuNoveltyEvaluator = HomeMenuNoveltyEvaluator(catalogGateway),
        )

        val response = repository.openPack("astronomes-en-herbe")

        assertEquals(2, response.revealSlots.size)
        assertEquals(listOf(rewardCard.id), response.equipmentCards.map { it.id })
        assertEquals(1, progressGateway.progress.equipmentInventory.entryFor(rewardCard.id).countOwned)
        assertEquals(2, progressGateway.progress.openedPackCount)
        assertEquals(1, progressGateway.progress.collection.cards.values.sumOf { it.totalOwned })
        assertEquals(true, progressGateway.progress.homeMenuNoveltyState.equipment)
    }

    @Test
    fun `open pack keeps sticky novelty flags and does not mark equipment when stock grows from one to two`() = runTest {
        val fixedNow = Instant.parse("2026-03-24T12:00:00Z")
        val rewardCard = testEquipmentCardDefinition(
            id = "observatory-lv1",
            type = EquipmentType.Observatory,
            level = 1,
            dropWeight = 1,
        )
        val progressGateway = FakeProgressGateway().apply {
            progress = StandaloneProgress(
                collection = ownedCollectionOf("ALP-001" to 1),
                rechargeState = testRechargeState(),
                newPlayerOnboardingStep = NewPlayerOnboardingStep.Completed,
                equipmentInventory = fr.aumombelli.dstcg.model.OwnedEquipmentInventory(
                    cards = mapOf(
                        rewardCard.id to fr.aumombelli.dstcg.model.OwnedEquipmentCardEntry(countOwned = 1),
                    ),
                ),
                homeMenuNoveltyState = HomeMenuNoveltyState(
                    library = true,
                    badgeBook = true,
                ),
                libraryCardNoveltyState = LibraryCardNoveltyState(
                    newCardIds = setOf("ALP-001"),
                ),
            )
        }
        val catalogGateway = FakeCatalogGateway().apply {
            cards = listOf(
                testCardDefinition("ALP-001", name = "Nebuleuse d'Orion", variantProfileId = "local-pack-profile"),
            )
            variantProfiles = listOf(localPackProfile())
            gameBalance = testGameBalanceDefinition(
                cardsPerDraw = 1,
                suburbanMeanPerDay = 1.0,
                ruralMeanPerDay = 1.0,
                mountainMeanPerDay = 1.0,
            )
            equipmentCards = listOf(rewardCard)
            equipmentSettings = EquipmentSettingsDefinition(
                commonReplacementChancePercent = 100.0,
            )
        }
        val repository = PackRepository(
            progressRepository = progressGateway,
            collectionRepository = CollectionRepository(progressGateway),
            localPackEngine = LocalPackEngine(
                catalogRepository = catalogGateway,
                settings = testGameSettings(
                    now = fixedNow,
                    maxStoredDraws = 10,
                    randomSeed = 0,
                ),
            ),
            homeMenuNoveltyEvaluator = HomeMenuNoveltyEvaluator(catalogGateway),
        )

        repository.openPack("astronomes-en-herbe")

        assertEquals(2, progressGateway.progress.equipmentInventory.entryFor(rewardCard.id).countOwned)
        assertEquals(
            HomeMenuNoveltyState(
                library = true,
                equipment = false,
                badgeBook = true,
            ),
            progressGateway.progress.homeMenuNoveltyState,
        )
        assertEquals(
            LibraryCardNoveltyState(
                newCardIds = setOf("ALP-001"),
            ),
            progressGateway.progress.libraryCardNoveltyState,
        )
    }

    private fun localPackProfile(): VariantProfile = VariantProfile(
        id = "local-pack-profile",
        skyQualities = listOf(
            SkyQualityDefinition("city", "Ville"),
            SkyQualityDefinition("holographic", "Holographique", isHolographic = true),
        ),
        finishes = listOf(
            CardFinishDefinition("standard", "Standard"),
            CardFinishDefinition("stamped", "Tamponnee", isStamped = true),
        ),
    )
}
