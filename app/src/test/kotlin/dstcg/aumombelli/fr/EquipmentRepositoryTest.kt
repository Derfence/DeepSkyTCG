package fr.aumombelli.dstcg

import fr.aumombelli.dstcg.data.EquipmentRepository
import fr.aumombelli.dstcg.model.ActiveEquipmentEffect
import fr.aumombelli.dstcg.model.EquipmentType
import fr.aumombelli.dstcg.model.OwnedEquipmentCardEntry
import fr.aumombelli.dstcg.model.OwnedEquipmentInventory
import fr.aumombelli.dstcg.model.StandaloneProgress
import fr.aumombelli.dstcg.model.entryFor
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
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
        )

        val state = repository.activateEquipment(definition.id)

        assertEquals(1, state.inventory.entryFor(definition.id).countOwned)
        assertEquals(2, state.inventory.entryFor(definition.id).activationCount)
        assertEquals(definition.id, state.activeEquipmentByType[EquipmentType.Observatory]?.equipmentCardId)
        assertEquals(4, state.activeEquipmentByType[EquipmentType.Observatory]?.packsRemaining)
        assertEquals(definition.id, state.lastActivatedCardIdByType[EquipmentType.Observatory])
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
}
