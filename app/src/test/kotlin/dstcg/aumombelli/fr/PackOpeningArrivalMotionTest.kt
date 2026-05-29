package fr.aumombelli.dstcg

import fr.aumombelli.dstcg.feature.packs.opening.holographicArrivalLiftPx
import fr.aumombelli.dstcg.feature.packs.opening.holographicArrivalScale
import fr.aumombelli.dstcg.feature.packs.opening.shouldTriggerPackOpeningHolographicCue
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PackOpeningArrivalMotionTest {
    @Test
    fun `holographic arrival motion starts and ends at rest`() {
        assertEquals(1f, holographicArrivalScale(0f), 0.001f)
        assertEquals(1f, holographicArrivalScale(1f), 0.001f)
        assertEquals(0f, holographicArrivalLiftPx(0f), 0.001f)
        assertEquals(0f, holographicArrivalLiftPx(1f), 0.001f)
    }

    @Test
    fun `holographic arrival motion still produces a visible punch mid animation`() {
        val midScale = holographicArrivalScale(0.18f)
        val midLift = holographicArrivalLiftPx(0.18f)

        assertTrue(midScale > 1.08f)
        assertTrue(midLift < -20f)
    }

    @Test
    fun `holographic cue only triggers once per card after it has been played`() {
        assertTrue(
            shouldTriggerPackOpeningHolographicCue(
                cardsVisible = true,
                currentPage = 1,
                lastObservedPage = 0,
                isHolographic = true,
                alreadyPlayed = false,
            ),
        )
        assertTrue(
            !shouldTriggerPackOpeningHolographicCue(
                cardsVisible = true,
                currentPage = 1,
                lastObservedPage = 0,
                isHolographic = true,
                alreadyPlayed = true,
            ),
        )
    }
}
