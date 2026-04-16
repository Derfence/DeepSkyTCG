package fr.aumombelli.dstcg

import fr.aumombelli.dstcg.ui.motion.calculateEquipmentPortalOpeningProgress
import fr.aumombelli.dstcg.ui.motion.calculateEquipmentPortalPose
import fr.aumombelli.dstcg.ui.motion.calculateEquipmentPortalTravelProgress
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EquipmentPortalOverlayTest {
    @Test
    fun `equipment portal has completed half the travel before opening starts`() {
        val pose = calculateEquipmentPortalPose(1f / 3f)

        assertEquals(0f, pose.opening, 0.001f)
        assertEquals(0.5f, calculateEquipmentPortalTravelProgress(1f / 3f), 0.001f)
        assertEquals(0f, calculateEquipmentPortalOpeningProgress(1f / 3f), 0.001f)
    }

    @Test
    fun `equipment portal reaches the equipment screen while half open`() {
        val pose = calculateEquipmentPortalPose(2f / 3f)

        assertEquals(0.5f, pose.opening, 0.001f)
        assertEquals(1f, calculateEquipmentPortalTravelProgress(2f / 3f), 0.001f)
        assertEquals(0.5f, calculateEquipmentPortalOpeningProgress(2f / 3f), 0.001f)
    }

    @Test
    fun `equipment portal finishes opening once it is immobile`() {
        val pose = calculateEquipmentPortalPose(1f)

        assertEquals(1f, pose.opening, 0.001f)
        assertEquals(1f, calculateEquipmentPortalTravelProgress(1f), 0.001f)
        assertEquals(1f, calculateEquipmentPortalOpeningProgress(1f), 0.001f)
        assertTrue(pose.glowAlpha > 0.99f)
    }
}
