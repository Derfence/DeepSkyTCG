package fr.aumombelli.dstcg

import fr.aumombelli.dstcg.app.newPlayerModalCardVerticalShiftPx
import org.junit.Assert.assertEquals
import org.junit.Test

class NewPlayerBlockingModalLayoutTest {
    @Test
    fun `card shifts upward when it can avoid the decorative bottom area`() {
        assertEquals(
            -42f,
            newPlayerModalCardVerticalShiftPx(
                modalHeightPx = 640f,
                cardHeightPx = 180f,
                bottomAvoidanceHeightPx = 260f,
                gapPx = 12f,
            ),
            0.001f,
        )
    }

    @Test
    fun `card stays centered when the screen cannot fit it above the decorative area`() {
        assertEquals(
            0f,
            newPlayerModalCardVerticalShiftPx(
                modalHeightPx = 360f,
                cardHeightPx = 220f,
                bottomAvoidanceHeightPx = 180f,
                gapPx = 12f,
            ),
            0.001f,
        )
    }

    @Test
    fun `card stays centered when it already clears the decorative area`() {
        assertEquals(
            0f,
            newPlayerModalCardVerticalShiftPx(
                modalHeightPx = 640f,
                cardHeightPx = 100f,
                bottomAvoidanceHeightPx = 180f,
                gapPx = 12f,
            ),
            0.001f,
        )
    }
}
