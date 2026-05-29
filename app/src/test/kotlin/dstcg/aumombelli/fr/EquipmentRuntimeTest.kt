package fr.aumombelli.dstcg

import fr.aumombelli.dstcg.data.DeterministicWeatherCalendar
import fr.aumombelli.dstcg.data.buildPackChargeUiStatus
import fr.aumombelli.dstcg.data.resolveActiveEquipmentBonus
import fr.aumombelli.dstcg.model.ActiveEquipmentEffect
import fr.aumombelli.dstcg.model.EquipmentType
import java.time.Duration
import java.time.Instant
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class EquipmentRuntimeTest {
    private val fixedNow = Instant.parse("2026-03-24T12:00:00Z")

    @Test
    fun `resolve active equipment bonuses combines all types and accelerates recharge`() = runTest {
        val observatory = testEquipmentCardDefinition(
            id = "observatory-master",
            type = EquipmentType.Observatory,
            bonusValue = 2.0,
        )
        val telescope = testEquipmentCardDefinition(
            id = "telescope-master",
            type = EquipmentType.Telescope,
            bonusValue = 100.0,
        )
        val mount = testEquipmentCardDefinition(
            id = "mount-master",
            type = EquipmentType.Mount,
            bonusValue = 100.0,
        )

        val bonus = resolveActiveEquipmentBonus(
            activeEquipmentByType = mapOf(
                EquipmentType.Observatory to ActiveEquipmentEffect(observatory.id, EquipmentType.Observatory, 3),
                EquipmentType.Telescope to ActiveEquipmentEffect(telescope.id, EquipmentType.Telescope, 3),
                EquipmentType.Mount to ActiveEquipmentEffect(mount.id, EquipmentType.Mount, 3),
            ),
            equipmentCards = listOf(observatory, telescope, mount),
        )

        assertEquals(100.0, bonus.rarityBoostPercent, 0.0001)
        assertEquals(100.0, bonus.holographicQualityPercent, 0.0001)
        assertEquals(2.0, bonus.rechargeMultiplier, 0.0001)

        val chargeStatus = buildPackChargeUiStatus(
            rechargeState = testRechargeStateWithNextChargeAt(
                availableDrawCount = 0,
                nextChargeAt = "2026-03-24T18:00:00Z",
                now = fixedNow,
            ),
            now = fixedNow,
            drawCooldown = Duration.ofHours(6),
            maxStoredDraws = 10,
            weatherPolicy = DeterministicWeatherCalendar,
            rechargeMultiplier = bonus.rechargeMultiplier,
        )

        assertEquals("2026-03-24T15:00:00Z", chargeStatus.nextChargeAt)
    }
}
