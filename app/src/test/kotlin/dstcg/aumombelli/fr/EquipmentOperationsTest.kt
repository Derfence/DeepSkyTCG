package fr.aumombelli.dstcg

import fr.aumombelli.dstcg.model.ActiveEquipmentEffect
import fr.aumombelli.dstcg.model.EquipmentType
import fr.aumombelli.dstcg.model.StandaloneProgress
import fr.aumombelli.dstcg.model.consumeEquipmentEffectsAfterPackOpen
import fr.aumombelli.dstcg.model.consumeObservatoryEffectAfterPackRecharge
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class EquipmentOperationsTest {
    @Test
    fun `pack opening consumes draw bonuses but preserves observatory validity`() = runTest {
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

        assertEquals(1, updated.activeEquipmentByType[EquipmentType.Observatory]?.packsRemaining)
        assertEquals(1, updated.activeEquipmentByType[EquipmentType.Telescope]?.packsRemaining)
    }

    @Test
    fun `pack recharge consumes observatory only when a charge is recovered`() = runTest {
        val progress = StandaloneProgress(
            collection = ownedCollectionOf(),
            rechargeState = testRechargeState(),
            activeEquipmentByType = mapOf(
                EquipmentType.Observatory to ActiveEquipmentEffect(
                    equipmentCardId = "observatory-beginner",
                    equipmentType = EquipmentType.Observatory,
                    packsRemaining = 2,
                ),
                EquipmentType.Telescope to ActiveEquipmentEffect(
                    equipmentCardId = "telescope-beginner",
                    equipmentType = EquipmentType.Telescope,
                    packsRemaining = 2,
                ),
            ),
        )

        val unchanged = progress.consumeObservatoryEffectAfterPackRecharge(rechargedPackCount = 0)
        val updated = progress.consumeObservatoryEffectAfterPackRecharge(rechargedPackCount = 1)

        assertEquals(progress.activeEquipmentByType, unchanged.activeEquipmentByType)
        assertEquals(1, updated.activeEquipmentByType[EquipmentType.Observatory]?.packsRemaining)
        assertEquals(2, updated.activeEquipmentByType[EquipmentType.Telescope]?.packsRemaining)
    }
}
