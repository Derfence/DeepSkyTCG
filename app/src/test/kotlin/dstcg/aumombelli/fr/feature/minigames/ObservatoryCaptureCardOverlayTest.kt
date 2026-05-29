package fr.aumombelli.dstcg.feature.minigames

import org.junit.Assert.assertEquals
import org.junit.Test

class ObservatoryCaptureCardOverlayTest {
    @Test
    fun `capture card uses readable width bounds`() {
        assertEquals(
            96f,
            observatoryCaptureCardWidthPx(unitPx = 300f, minWidthPx = 96f, maxWidthPx = 180f),
            0.001f,
        )
        assertEquals(
            144f,
            observatoryCaptureCardWidthPx(unitPx = 800f, minWidthPx = 96f, maxWidthPx = 180f),
            0.001f,
        )
        assertEquals(
            180f,
            observatoryCaptureCardWidthPx(unitPx = 1200f, minWidthPx = 96f, maxWidthPx = 180f),
            0.001f,
        )
    }
}
