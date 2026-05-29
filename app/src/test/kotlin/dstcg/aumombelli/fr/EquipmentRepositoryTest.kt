package fr.aumombelli.dstcg

import fr.aumombelli.dstcg.data.EquipmentRepository
import fr.aumombelli.dstcg.data.HomeMenuNoveltyEvaluator
import fr.aumombelli.dstcg.model.ActiveEquipmentEffect
import fr.aumombelli.dstcg.model.EquipmentBadgeProgress
import fr.aumombelli.dstcg.model.EquipmentType
import fr.aumombelli.dstcg.model.HomeMenuNoveltyState
import fr.aumombelli.dstcg.model.OwnedEquipmentCardEntry
import fr.aumombelli.dstcg.model.OwnedEquipmentInventory
import fr.aumombelli.dstcg.model.StandaloneProgress
import fr.aumombelli.dstcg.model.entryFor
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EquipmentRepositoryTest {
    @Test
    fun `activate equipment consumes stock increments activations and tracks last used card`() = runTest {
        val definition = testEquipmentCardDefinition(
            id = "observatory-beginner",
            type = EquipmentType.Observatory,
            packsAffected = 4,
        )
        val progressGateway = FakeProgressGateway().apply {
            progress = StandaloneProgress(
                collection = ownedCollectionOf(),
                rechargeState = testRechargeState(),
                equipmentInventory = OwnedEquipmentInventory(
                    cards = mapOf(
                        definition.id to OwnedEquipmentCardEntry(
                            countOwned = 2,
                            activationCount = 1,
                        ),
                    ),
                ),
            )
        }
        val repository = EquipmentRepository(
            progressRepository = progressGateway,
            catalogRepository = FakeCatalogGateway().apply {
                equipmentCards = listOf(definition)
            },
            homeMenuNoveltyEvaluator = HomeMenuNoveltyEvaluator(FakeCatalogGateway().apply {
                equipmentCards = listOf(definition)
            }),
        )

        val state = repository.activateEquipment(definition.id)

        assertEquals(1, state.inventory.entryFor(definition.id).countOwned)
        assertEquals(2, state.inventory.entryFor(definition.id).activationCount)
        assertEquals(definition.id, state.activeEquipmentByType[EquipmentType.Observatory]?.equipmentCardId)
        assertEquals(4, state.activeEquipmentByType[EquipmentType.Observatory]?.packsRemaining)
        assertEquals(definition.id, state.lastActivatedCardIdByType[EquipmentType.Observatory])
        assertEquals(1, progressGateway.progress.equipmentBadgeProgress.maxSimultaneouslyActiveEquipmentTypeCount)
    }

    @Test
    fun `activate equipment rejects a second active card of the same type`() = runTest {
        val activeDefinition = testEquipmentCardDefinition(
            id = "telescope-beginner",
            type = EquipmentType.Telescope,
            packsAffected = 3,
        )
        val reserveDefinition = testEquipmentCardDefinition(
            id = "telescope-advanced",
            type = EquipmentType.Telescope,
            packsAffected = 5,
            level = 2,
        )
        val progressGateway = FakeProgressGateway().apply {
            progress = StandaloneProgress(
                collection = ownedCollectionOf(),
                rechargeState = testRechargeState(),
                equipmentInventory = OwnedEquipmentInventory(
                    cards = mapOf(
                        reserveDefinition.id to OwnedEquipmentCardEntry(countOwned = 1, activationCount = 0),
                    ),
                ),
                activeEquipmentByType = mapOf(
                    EquipmentType.Telescope to ActiveEquipmentEffect(
                        equipmentCardId = activeDefinition.id,
                        equipmentType = EquipmentType.Telescope,
                        packsRemaining = 2,
                    ),
                ),
            )
        }
        val repository = EquipmentRepository(
            progressRepository = progressGateway,
            catalogRepository = FakeCatalogGateway().apply {
                equipmentCards = listOf(activeDefinition, reserveDefinition)
            },
            homeMenuNoveltyEvaluator = HomeMenuNoveltyEvaluator(FakeCatalogGateway().apply {
                equipmentCards = listOf(activeDefinition, reserveDefinition)
            }),
        )

        val error = try {
            repository.activateEquipment(reserveDefinition.id)
            error("Expected an IllegalStateException")
        } catch (error: IllegalStateException) {
            error
        }

        assertEquals("Un telescope est deja actif.", error.message)
        assertEquals(1, progressGateway.progress.equipmentInventory.entryFor(reserveDefinition.id).countOwned)
    }

    @Test
    fun `activate equipment updates simultaneous maxima and badge novelty when trio completes`() = runTest {
        val observatory = testEquipmentCardDefinition(
            id = "observatory-master",
            type = EquipmentType.Observatory,
            level = 3,
        )
        val telescope = testEquipmentCardDefinition(
            id = "telescope-master",
            type = EquipmentType.Telescope,
            level = 3,
        )
        val mount = testEquipmentCardDefinition(
            id = "mount-master",
            type = EquipmentType.Mount,
            level = 3,
        )
        val catalogGateway = FakeCatalogGateway().apply {
            equipmentCards = listOf(observatory, telescope, mount)
        }
        val progressGateway = FakeProgressGateway().apply {
            progress = StandaloneProgress(
                collection = ownedCollectionOf(),
                rechargeState = testRechargeState(),
                equipmentInventory = OwnedEquipmentInventory(
                    cards = mapOf(
                        mount.id to OwnedEquipmentCardEntry(countOwned = 1),
                    ),
                ),
                activeEquipmentByType = mapOf(
                    EquipmentType.Observatory to ActiveEquipmentEffect(
                        equipmentCardId = observatory.id,
                        equipmentType = EquipmentType.Observatory,
                        packsRemaining = 2,
                    ),
                    EquipmentType.Telescope to ActiveEquipmentEffect(
                        equipmentCardId = telescope.id,
                        equipmentType = EquipmentType.Telescope,
                        packsRemaining = 2,
                    ),
                ),
                equipmentBadgeProgress = EquipmentBadgeProgress(
                    maxSimultaneouslyActiveEquipmentTypeCount = 2,
                    maxSimultaneouslyActiveLevelThreeEquipmentTypeCount = 2,
                ),
                homeMenuNoveltyState = HomeMenuNoveltyState(),
            )
        }
        val repository = EquipmentRepository(
            progressRepository = progressGateway,
            catalogRepository = catalogGateway,
            homeMenuNoveltyEvaluator = HomeMenuNoveltyEvaluator(catalogGateway),
        )

        repository.activateEquipment(mount.id)

        assertEquals(3, progressGateway.progress.equipmentBadgeProgress.maxSimultaneouslyActiveEquipmentTypeCount)
        assertEquals(3, progressGateway.progress.equipmentBadgeProgress.maxSimultaneouslyActiveLevelThreeEquipmentTypeCount)
        assertTrue(progressGateway.progress.homeMenuNoveltyState.badgeBook)
    }
}
