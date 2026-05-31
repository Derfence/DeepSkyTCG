package fr.aumombelli.dstcg

import fr.aumombelli.dstcg.app.newPlayerModalDecorativeLayoutPx
import org.junit.Assert.assertEquals
import org.junit.Test

class NewPlayerBlockingModalLayoutTest {
    @Test
    fun `card and decoration are distributed vertically when they fit`() {
        val layout = newPlayerModalDecorativeLayoutPx(
            modalHeightPx = 640f,
            cardHeightPx = 180f,
            decorativeHeightPx = 260f,
        )

        assertEquals(
            -163.333f,
            layout.cardTranslationYPx,
            0.001f,
        )
        assertEquals(313.333f, layout.decorativeTopPx, 0.001f)
    }

    @Test
    fun `decoration stays inside the modal when available height is constrained`() {
        val layout = newPlayerModalDecorativeLayoutPx(
            modalHeightPx = 360f,
            cardHeightPx = 220f,
            decorativeHeightPx = 180f,
        )

        assertEquals(-70f, layout.cardTranslationYPx, 0.001f)
        assertEquals(180f, layout.decorativeTopPx, 0.001f)
    }

    @Test
    fun `decoration stays at the bottom until the card is measured`() {
        val layout = newPlayerModalDecorativeLayoutPx(
            modalHeightPx = 640f,
            cardHeightPx = 0f,
            decorativeHeightPx = 260f,
        )

        assertEquals(0f, layout.cardTranslationYPx, 0.001f)
        assertEquals(380f, layout.decorativeTopPx, 0.001f)
    }
}
