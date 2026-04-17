package fr.aumombelli.dstcg

import fr.aumombelli.dstcg.ui.motion.calculateEquipmentPortalOpeningProgress
import fr.aumombelli.dstcg.ui.motion.calculateEquipmentPortalInstrumentRevealProgress
import fr.aumombelli.dstcg.ui.motion.calculateEquipmentPortalPose
import fr.aumombelli.dstcg.ui.motion.calculateEquipmentPortalTravelProgress
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EquipmentPortalOverlayTest {
    @Test
    fun `equipment portal keeps the house closed during the first beat`() {
        val pose = calculateEquipmentPortalPose(1f / 3f)

        assertEquals(0f, pose.roofOpening, 0.001f)
        assertEquals(0f, pose.instrumentReveal, 0.001f)
        assertEquals(0.5f, calculateEquipmentPortalTravelProgress(1f / 3f), 0.001f)
        assertEquals(0f, calculateEquipmentPortalOpeningProgress(1f / 3f), 0.001f)
        assertEquals(0f, calculateEquipmentPortalInstrumentRevealProgress(1f / 3f), 0.001f)
    }

    @Test
    fun `equipment portal finishes opening the roof by the second beat`() {
        val pose = calculateEquipmentPortalPose(2f / 3f)

        assertEquals(1f, pose.roofOpening, 0.001f)
        assertEquals(0f, pose.instrumentReveal, 0.001f)
        assertEquals(1f, calculateEquipmentPortalTravelProgress(2f / 3f), 0.001f)
        assertEquals(1f, calculateEquipmentPortalOpeningProgress(2f / 3f), 0.001f)
        assertEquals(0f, calculateEquipmentPortalInstrumentRevealProgress(2f / 3f), 0.001f)
    }

    @Test
    fun `equipment portal reveals the instrument only during the final beat`() {
        val pose = calculateEquipmentPortalPose(5f / 6f)

        assertEquals(1f, pose.roofOpening, 0.001f)
        assertEquals(0.5f, pose.instrumentReveal, 0.001f)
        assertEquals(1f, calculateEquipmentPortalTravelProgress(5f / 6f), 0.001f)
        assertEquals(1f, calculateEquipmentPortalOpeningProgress(5f / 6f), 0.001f)
        assertEquals(0.5f, calculateEquipmentPortalInstrumentRevealProgress(5f / 6f), 0.001f)
    }

    @Test
    fun `equipment portal ends with an open roof and visible instrument`() {
        val pose = calculateEquipmentPortalPose(1f)

        assertEquals(1f, pose.roofOpening, 0.001f)
        assertEquals(1f, pose.instrumentReveal, 0.001f)
        assertEquals(1f, calculateEquipmentPortalTravelProgress(1f), 0.001f)
        assertEquals(1f, calculateEquipmentPortalOpeningProgress(1f), 0.001f)
        assertEquals(1f, calculateEquipmentPortalInstrumentRevealProgress(1f), 0.001f)
        assertTrue(pose.glowAlpha > 0.99f)
    }
}
