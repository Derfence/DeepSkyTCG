package fr.aumombelli.dstcg.feature.minigames

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Test

class ObservatoryAltitudeSideControlTest {
    @Test
    fun `altitude slider keeps slight inset from control height`() {
        assertEquals(
            184f,
            observatoryAltitudeSliderLength(containerHeight = 200.dp).value,
            0.001f,
        )
    }
}
