package fr.aumombelli.dstcg

import androidx.compose.ui.unit.dp
import fr.aumombelli.dstcg.ui.component.TRADING_CARD_WIDTH_OVER_HEIGHT
import fr.aumombelli.dstcg.ui.component.calculateTradingCardFitWidth
import org.junit.Assert.assertEquals
import org.junit.Test

class AstroCardResponsiveLayoutTest {
    @Test
    fun `card fills available width while height allows`() {
        val width = calculateTradingCardFitWidth(
            maxWidth = 320.dp,
            maxHeight = 700.dp,
        )

        assertEquals(320f, width.value, 0.01f)
    }

    @Test
    fun `card shrinks width when height is limiting`() {
        val width = calculateTradingCardFitWidth(
            maxWidth = 320.dp,
            maxHeight = 240.dp,
        )

        assertEquals(240f * TRADING_CARD_WIDTH_OVER_HEIGHT, width.value, 0.01f)
    }
}
