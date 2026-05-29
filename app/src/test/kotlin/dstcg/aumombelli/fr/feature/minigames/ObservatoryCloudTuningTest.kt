package fr.aumombelli.dstcg.feature.minigames

import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ObservatoryCloudTuningTest {
    @Test
    fun `cloud inter-cycle wait stays inside tuning bounds`() {
        val random = Random(seed = 42)

        repeat(20) {
            val waitMillis = observatoryRandomCloudInterCycleWaitMillis(random)

            assertTrue(waitMillis >= ObservatoryCloudInterCycleWaitMinMillis)
            assertTrue(waitMillis <= ObservatoryCloudInterCycleWaitMaxMillis)
        }
    }

    @Test
    fun `cloud progress increases from elapsed time and clamps at full opacity`() {
        assertEquals(
            0.25f,
            observatoryCloudProgressAfterTick(
                progress = 0f,
                tickMillis = 250L,
                durationMillis = 1_000L,
            ),
            0.001f,
        )
        assertEquals(
            1f,
            observatoryCloudProgressAfterTick(
                progress = 0.90f,
                tickMillis = 250L,
                durationMillis = 1_000L,
            ),
            0.001f,
        )
    }

    @Test
    fun `cloud scrub amount follows drag distance and clamps`() {
        assertEquals(
            0.5f,
            observatoryCloudScrubAmountForDrag(ObservatoryCloudDragPixelsForFullScrub / 2f),
            0.001f,
        )
        assertEquals(
            1f,
            observatoryCloudScrubAmountForDrag(ObservatoryCloudDragPixelsForFullScrub * 2f),
            0.001f,
        )
    }
}
