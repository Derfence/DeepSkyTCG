package fr.aumombelli.dstcg.feature.packs.selection

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import fr.aumombelli.dstcg.ui.component.TRADING_CARD_WIDTH_OVER_HEIGHT
import kotlin.math.max

internal data class PackSelectionBoosterStageChrome(
    val titleTopPadding: Dp,
    val fieldStartPadding: Dp,
    val fieldTopPadding: Dp,
    val fieldEndPadding: Dp,
    val fieldBottomPadding: Dp,
)

internal data class PackSelectionBoosterGridMetrics(
    val horizontalGap: Dp,
    val verticalGap: Dp,
    val gridPackWidth: Dp,
    val gridPackHeight: Dp,
    val gridStartX: Dp,
    val gridStartY: Dp,
)

internal fun calculatePackSelectionBoosterStageChrome(
    screenWidth: Dp,
    screenHeight: Dp,
    safeStartInset: Dp,
    safeTopInset: Dp,
    safeEndInset: Dp,
    safeBottomInset: Dp,
    stageStartInset: Dp,
    stageTopInset: Dp,
    stageEndInset: Dp,
    stageBottomInset: Dp,
    heroProgress: Float,
): PackSelectionBoosterStageChrome {
    val compactness = max(
        packSelectionCompactHeightProgress(screenHeight),
        packSelectionCompactWidthProgress(screenWidth),
    )
    val expandedTitleTopPadding = safeTopInset + lerp(14.dp, 10.dp, compactness)
    val expandedFieldSidePadding = lerp(6.dp, 2.dp, compactness)
    val expandedFieldDownshift = 2.dp
    val expandedFieldTopPadding = expandedTitleTopPadding + lerp(28.dp, 24.dp, compactness)
    val expandedFieldBottomPadding =
        (safeBottomInset + lerp(4.dp, 2.dp, compactness) - expandedFieldDownshift).coerceAtLeast(0.dp)

    return PackSelectionBoosterStageChrome(
        titleTopPadding = lerp(18.dp, expandedTitleTopPadding, heroProgress),
        fieldStartPadding = lerp(stageStartInset, safeStartInset + expandedFieldSidePadding, heroProgress),
        fieldTopPadding = lerp(
            88.dp + stageTopInset,
            expandedFieldTopPadding + expandedFieldDownshift,
            heroProgress,
        ),
        fieldEndPadding = lerp(stageEndInset, safeEndInset + expandedFieldSidePadding, heroProgress),
        fieldBottomPadding = lerp(18.dp + stageBottomInset, expandedFieldBottomPadding, heroProgress),
    )
}

internal fun calculatePackSelectionBoosterGridMetrics(
    availableWidth: Dp,
    availableHeight: Dp,
): PackSelectionBoosterGridMetrics {
    val compactness = max(
        packSelectionCompactHeightProgress(availableHeight),
        packSelectionCompactWidthProgress(availableWidth),
    )
    val horizontalGap = lerp(10.dp, 6.dp, compactness)
    val verticalGap = lerp(10.dp, 6.dp, compactness)
    val horizontalGuard = lerp(4.dp, 2.dp, compactness)
    val verticalGuard = lerp(4.dp, 2.dp, compactness)
    val usableWidth = (availableWidth - horizontalGuard * 2f - horizontalGap).coerceAtLeast(0.dp)
    val usableHeight = (availableHeight - verticalGuard * 2f - verticalGap).coerceAtLeast(0.dp)
    val gridPackWidth = minOf(
        usableWidth / 2f,
        (usableHeight / 2f) * TRADING_CARD_WIDTH_OVER_HEIGHT,
    )
    val gridPackHeight = if (gridPackWidth > 0.dp) {
        gridPackWidth / TRADING_CARD_WIDTH_OVER_HEIGHT
    } else {
        0.dp
    }

    return PackSelectionBoosterGridMetrics(
        horizontalGap = horizontalGap,
        verticalGap = verticalGap,
        gridPackWidth = gridPackWidth,
        gridPackHeight = gridPackHeight,
        gridStartX = (availableWidth - (gridPackWidth * 2f + horizontalGap)) / 2f,
        gridStartY = (availableHeight - (gridPackHeight * 2f + verticalGap)) / 2f,
    )
}

private fun packSelectionCompactHeightProgress(availableHeight: Dp): Float =
    ((520.dp - availableHeight).value / 200f).coerceIn(0f, 1f)

private fun packSelectionCompactWidthProgress(availableWidth: Dp): Float =
    ((360.dp - availableWidth).value / 72f).coerceIn(0f, 1f)
