package fr.aumombelli.dstcg

import fr.aumombelli.dstcg.ui.motion.calculateBadgeChestOpeningProgress
import fr.aumombelli.dstcg.ui.motion.calculateBadgeChestPose
import fr.aumombelli.dstcg.ui.motion.calculateBadgeChestTravelProgress
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ChestPortalOverlayTest {
    @Test
    fun `badge chest has completed half the travel before opening starts`() {
        val pose = calculateBadgeChestPose(1f / 3f)

        assertEquals(0f, pose.openAngle, 0.001f)
        assertEquals(0.5f, calculateBadgeChestTravelProgress(1f / 3f), 0.001f)
        assertEquals(0f, calculateBadgeChestOpeningProgress(1f / 3f), 0.001f)
    }

    @Test
    fun `badge chest reaches the badge book while half open`() {
        val pose = calculateBadgeChestPose(2f / 3f)

        assertEquals(142f * 0.5f, pose.openAngle, 0.001f)
        assertEquals(1f, calculateBadgeChestTravelProgress(2f / 3f), 0.001f)
        assertEquals(0.5f, calculateBadgeChestOpeningProgress(2f / 3f), 0.001f)
    }

    @Test
    fun `badge chest finishes opening once it is immobile`() {
        val pose = calculateBadgeChestPose(1f)

        assertEquals(142f, pose.openAngle, 0.001f)
        assertEquals(1f, calculateBadgeChestTravelProgress(1f), 0.001f)
        assertEquals(1f, calculateBadgeChestOpeningProgress(1f), 0.001f)
        assertTrue(pose.shadowAlpha > 0.3f)
    }
}
