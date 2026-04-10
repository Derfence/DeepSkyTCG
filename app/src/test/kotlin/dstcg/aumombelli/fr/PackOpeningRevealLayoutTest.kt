package fr.aumombelli.dstcg

import androidx.compose.ui.unit.dp
import fr.aumombelli.dstcg.feature.packs.opening.calculatePackOpeningSceneLayout
import fr.aumombelli.dstcg.feature.packs.opening.calculatePackOpeningRevealLayout
import fr.aumombelli.dstcg.ui.component.TRADING_CARD_WIDTH_OVER_HEIGHT
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PackOpeningRevealLayoutTest {
    @Test
    fun compact_height_reduces_pack_reveal_card_size() {
        val compactLayout = calculatePackOpeningRevealLayout(
            availableWidth = 320.dp,
            availableHeight = 220.dp,
        )
        val roomyLayout = calculatePackOpeningRevealLayout(
            availableWidth = 320.dp,
            availableHeight = 420.dp,
        )

        assertTrue(compactLayout.cardWidth < roomyLayout.cardWidth)
        assertTrue(compactLayout.cardHeight < roomyLayout.cardHeight)
    }

    @Test
    fun reveal_layout_keeps_card_ratio_and_reserves_top_overlay_space() {
        val layout = calculatePackOpeningRevealLayout(
            availableWidth = 360.dp,
            availableHeight = 540.dp,
        )

        assertEquals(
            TRADING_CARD_WIDTH_OVER_HEIGHT,
            layout.cardWidth.value / layout.cardHeight.value,
            0.001f,
        )
        assertTrue(layout.topOverlayReserve > 0.dp)
        assertTrue(layout.bottomSafeInset > 0.dp)
    }

    @Test
    fun layout_keeps_zero_outer_padding_while_compacting_other_chrome() {
        val compactSceneLayout = calculatePackOpeningSceneLayout(
            availableWidth = 320.dp,
            availableHeight = 520.dp,
        )
        val roomySceneLayout = calculatePackOpeningSceneLayout(
            availableWidth = 411.dp,
            availableHeight = 760.dp,
        )
        val compactRevealLayout = calculatePackOpeningRevealLayout(
            availableWidth = 280.dp,
            availableHeight = 320.dp,
        )
        val roomyRevealLayout = calculatePackOpeningRevealLayout(
            availableWidth = 360.dp,
            availableHeight = 520.dp,
        )

        assertEquals(0.dp, compactSceneLayout.horizontalContentPadding)
        assertEquals(0.dp, roomySceneLayout.horizontalContentPadding)
        assertEquals(0.dp, compactSceneLayout.verticalContentPadding)
        assertEquals(0.dp, roomySceneLayout.verticalContentPadding)
        assertEquals(0.dp, compactRevealLayout.pageHorizontalPadding)
        assertEquals(0.dp, roomyRevealLayout.pageHorizontalPadding)
        assertEquals(0.dp, compactRevealLayout.pageVerticalPadding)
        assertEquals(0.dp, roomyRevealLayout.pageVerticalPadding)
        assertTrue(compactSceneLayout.sectionSpacing < roomySceneLayout.sectionSpacing)
        assertTrue(compactRevealLayout.topOverlayReserve < roomyRevealLayout.topOverlayReserve)
    }
}
