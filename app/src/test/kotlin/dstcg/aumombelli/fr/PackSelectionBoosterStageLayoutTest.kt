package fr.aumombelli.dstcg

import androidx.compose.ui.unit.dp
import fr.aumombelli.dstcg.feature.packs.selection.calculatePackSelectionBoosterGridMetrics
import fr.aumombelli.dstcg.feature.packs.selection.calculatePackSelectionBoosterStageChrome
import fr.aumombelli.dstcg.ui.component.TRADING_CARD_WIDTH_OVER_HEIGHT
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PackSelectionBoosterStageLayoutTest {
    @Test
    fun expanded_stage_chrome_pushes_title_below_safe_top_and_reclaims_header_space() {
        val chrome = calculatePackSelectionBoosterStageChrome(
            screenWidth = 360.dp,
            screenHeight = 760.dp,
            safeStartInset = 0.dp,
            safeTopInset = 24.dp,
            safeEndInset = 0.dp,
            safeBottomInset = 16.dp,
            stageStartInset = 16.dp,
            stageTopInset = 196.dp,
            stageEndInset = 16.dp,
            stageBottomInset = 16.dp,
            heroProgress = 1f,
        )

        assertTrue(chrome.titleTopPadding > 24.dp)
        assertTrue(chrome.fieldTopPadding > chrome.titleTopPadding)
        assertTrue(chrome.fieldTopPadding < 196.dp + 88.dp)
        assertTrue(chrome.fieldStartPadding < 16.dp)
        assertTrue(chrome.fieldBottomPadding >= 16.dp)
    }

    @Test
    fun phone_sized_stage_keeps_boosters_large_after_title_reflow() {
        val chrome = calculatePackSelectionBoosterStageChrome(
            screenWidth = 411.dp,
            screenHeight = 731.dp,
            safeStartInset = 0.dp,
            safeTopInset = 24.dp,
            safeEndInset = 0.dp,
            safeBottomInset = 16.dp,
            stageStartInset = 16.dp,
            stageTopInset = 196.dp,
            stageEndInset = 16.dp,
            stageBottomInset = 16.dp,
            heroProgress = 1f,
        )
        val compact = calculatePackSelectionBoosterGridMetrics(
            availableWidth = 411.dp - chrome.fieldStartPadding - chrome.fieldEndPadding,
            availableHeight = 731.dp - chrome.fieldTopPadding - chrome.fieldBottomPadding,
        )
        val roomy = calculatePackSelectionBoosterGridMetrics(
            availableWidth = 460.dp,
            availableHeight = 700.dp,
        )

        assertEquals(
            TRADING_CARD_WIDTH_OVER_HEIGHT,
            compact.gridPackWidth.value / compact.gridPackHeight.value,
            0.001f,
        )
        assertTrue(compact.gridPackWidth / 411.dp >= 0.40f)
        assertTrue(roomy.gridPackWidth > compact.gridPackWidth)
    }

    @Test
    fun very_compact_grid_tightens_gaps_before_shrinking_cards_too_far() {
        val compact = calculatePackSelectionBoosterGridMetrics(
            availableWidth = 320.dp,
            availableHeight = 440.dp,
        )
        val roomy = calculatePackSelectionBoosterGridMetrics(
            availableWidth = 460.dp,
            availableHeight = 700.dp,
        )

        assertTrue(compact.horizontalGap < roomy.horizontalGap)
        assertTrue(compact.verticalGap < roomy.verticalGap)
        assertEquals(
            TRADING_CARD_WIDTH_OVER_HEIGHT,
            compact.gridPackWidth.value / compact.gridPackHeight.value,
            0.001f,
        )
        assertTrue(compact.gridPackWidth / 320.dp >= 0.36f)
    }
}
