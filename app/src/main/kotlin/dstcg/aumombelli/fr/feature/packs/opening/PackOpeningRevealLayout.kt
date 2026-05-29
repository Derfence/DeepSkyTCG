package fr.aumombelli.dstcg.feature.packs.opening

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import fr.aumombelli.dstcg.ui.component.TRADING_CARD_WIDTH_OVER_HEIGHT
import kotlin.math.max

internal data class PackOpeningRevealLayout(
    val pageHorizontalPadding: Dp,
    val pageVerticalPadding: Dp,
    val cardWidth: Dp,
    val cardHeight: Dp,
    val topOverlayReserve: Dp,
    val bottomSafeInset: Dp,
)

internal data class PackOpeningSceneLayout(
    val horizontalContentPadding: Dp,
    val verticalContentPadding: Dp,
    val sectionSpacing: Dp,
    val hintLabelTopPadding: Dp,
    val compactHeader: Boolean,
)

internal data class PackOpeningRevealSlotLayout(
    val cardWidth: Dp,
    val cardHeight: Dp,
    val cardStart: Dp,
    val cardTop: Dp,
    val cardCenterX: Dp,
    val cardCenterY: Dp,
)

internal fun calculatePackOpeningSceneLayout(
    availableWidth: Dp,
    availableHeight: Dp,
): PackOpeningSceneLayout {
    val compactness = max(
        sceneCompactHeightProgress(availableHeight),
        compactWidthProgress(availableWidth),
    )

    return PackOpeningSceneLayout(
        horizontalContentPadding = 0.dp,
        verticalContentPadding = 0.dp,
        sectionSpacing = lerp(18.dp, 4.dp, compactness),
        hintLabelTopPadding = lerp(10.dp, 4.dp, compactness),
        compactHeader = compactness >= 0.55f,
    )
}

internal fun calculatePackOpeningRevealLayout(
    availableWidth: Dp,
    availableHeight: Dp,
): PackOpeningRevealLayout {
    val compactness = max(
        revealCompactHeightProgress(availableHeight),
        compactWidthProgress(availableWidth),
    )
    val pageHorizontalPadding = 0.dp
    val pageVerticalPadding = 0.dp
    val topOverlayReserve = lerp(40.dp, 4.dp, compactness)
    val bottomSafeInset = lerp(12.dp, 2.dp, compactness)
    val horizontalChrome = lerp(48.dp, 40.dp, compactness)
    val widthBudget = (availableWidth - horizontalChrome - (pageHorizontalPadding * 2f)).coerceAtLeast(0.dp)
    val heightBudget = (
        availableHeight -
            topOverlayReserve -
            bottomSafeInset -
            (pageVerticalPadding * 2f)
        ).coerceAtLeast(0.dp)
    val cardWidth = minOf(widthBudget, heightBudget * TRADING_CARD_WIDTH_OVER_HEIGHT)
    val cardHeight = if (cardWidth > 0.dp) {
        cardWidth / TRADING_CARD_WIDTH_OVER_HEIGHT
    } else {
        0.dp
    }

    return PackOpeningRevealLayout(
        pageHorizontalPadding = pageHorizontalPadding,
        pageVerticalPadding = pageVerticalPadding,
        cardWidth = cardWidth,
        cardHeight = cardHeight,
        topOverlayReserve = topOverlayReserve,
        bottomSafeInset = bottomSafeInset,
    )
}

internal fun calculatePackOpeningRevealSlotLayout(
    availableWidth: Dp,
    availableHeight: Dp,
): PackOpeningRevealSlotLayout {
    val revealLayout = calculatePackOpeningRevealLayout(
        availableWidth = availableWidth,
        availableHeight = availableHeight,
    )
    val cardStart = ((availableWidth - revealLayout.cardWidth) / 2f).coerceAtLeast(0.dp)
    val cardTop = (
        revealLayout.topOverlayReserve +
            (
                availableHeight -
                    revealLayout.topOverlayReserve -
                    revealLayout.bottomSafeInset -
                    revealLayout.cardHeight
                ) / 2f
        ).coerceAtLeast(0.dp)

    return PackOpeningRevealSlotLayout(
        cardWidth = revealLayout.cardWidth,
        cardHeight = revealLayout.cardHeight,
        cardStart = cardStart,
        cardTop = cardTop,
        cardCenterX = cardStart + revealLayout.cardWidth / 2f,
        cardCenterY = cardTop + revealLayout.cardHeight / 2f,
    )
}

private fun sceneCompactHeightProgress(availableHeight: Dp): Float =
    ((760.dp - availableHeight).value / 180f).coerceIn(0f, 1f)

private fun revealCompactHeightProgress(availableHeight: Dp): Float =
    ((520.dp - availableHeight).value / 180f).coerceIn(0f, 1f)

private fun compactWidthProgress(availableWidth: Dp): Float =
    ((360.dp - availableWidth).value / 72f).coerceIn(0f, 1f)
