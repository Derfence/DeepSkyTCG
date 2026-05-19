package fr.aumombelli.dstcg.feature.minigames

import org.junit.Assert.assertEquals
import org.junit.Test

class ObservatoryFocusWheelsControlTest {
    @Test
    fun `fine wheel uses one fifth of coarse adjustment`() {
        val coarse = observatoryFocusWheelValue(
            value = 0.5f,
            deltaDegrees = 90f,
            reductionRatio = 1f,
        )
        val fine = observatoryFocusWheelValue(
            value = 0.5f,
            deltaDegrees = 90f,
            reductionRatio = ObservatoryFocusFineReductionRatio,
        )

        assertEquals(0.75f, coarse, 0.001f)
        assertEquals(0.55f, fine, 0.001f)
    }

    @Test
    fun `focus wheel delta crosses zero angle smoothly`() {
        assertEquals(
            20f,
            observatoryFocusWheelDeltaDegrees(previousAngle = 350f, currentAngle = 10f),
            0.001f,
        )
        assertEquals(
            -20f,
            observatoryFocusWheelDeltaDegrees(previousAngle = 10f, currentAngle = 350f),
            0.001f,
        )
    }
}
