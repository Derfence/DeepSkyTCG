package fr.aumombelli.dstcg

import fr.aumombelli.dstcg.model.ActiveEquipmentEffect
import fr.aumombelli.dstcg.model.EquipmentType
import fr.aumombelli.dstcg.model.StandaloneProgress
import fr.aumombelli.dstcg.model.consumeEquipmentEffectsAfterPackOpen
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class EquipmentOperationsTest {
    @Test
    fun `consume equipment effects decrements all active types and removes expired ones`() = runTest {
        val progress = StandaloneProgress(
            collection = ownedCollectionOf(),
            rechargeState = testRechargeState(),
            activeEquipmentByType = mapOf(
                EquipmentType.Observatory to ActiveEquipmentEffect(
                    equipmentCardId = "observatory-beginner",
                    equipmentType = EquipmentType.Observatory,
                    packsRemaining = 1,
                ),
                EquipmentType.Telescope to ActiveEquipmentEffect(
                    equipmentCardId = "telescope-beginner",
                    equipmentType = EquipmentType.Telescope,
                    packsRemaining = 2,
                ),
            ),
        )

        val updated = progress.consumeEquipmentEffectsAfterPackOpen()

        assertFalse(updated.activeEquipmentByType.containsKey(EquipmentType.Observatory))
        assertEquals(1, updated.activeEquipmentByType[EquipmentType.Telescope]?.packsRemaining)
    }
}
