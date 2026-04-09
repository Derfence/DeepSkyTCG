package fr.aumombelli.dstcg.feature.badges

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.IntSize
import kotlin.math.PI
import kotlin.math.sin

internal fun badgeCelebrationStartCenter(rootSize: IntSize): Offset = Offset(
    x = rootSize.width / 2f,
    y = rootSize.height / 2f,
)

internal fun badgeCelebrationEndCenter(
    targetBounds: Rect,
    displayedCoinSizePx: Float,
    gapPx: Float,
): Offset = Offset(
    x = targetBounds.left + ((targetBounds.right - targetBounds.left) / 2f),
    y = targetBounds.top + gapPx + (displayedCoinSizePx / 2f),
)

internal fun badgeCelebrationInitialCoinCenters(
    badgeCount: Int,
    startCenter: Offset,
    radius: Float,
): List<Offset> = List(badgeCount) { index ->
    val startOffset = badgeCelebrationFanOffset(index = index, radius = radius)
    Offset(
        x = startCenter.x + startOffset.x,
        y = startCenter.y + startOffset.y,
    )
}

internal fun badgeCelebrationTitleTopLeft(
    titleSize: IntSize,
    coinCenters: List<Offset>,
    groupCenterX: Float,
    displayedCoinSizePx: Float,
    gapPx: Float,
): Offset {
    val topMostCoinTop = (coinCenters.minOfOrNull { it.y } ?: 0f) - (displayedCoinSizePx / 2f)
    return Offset(
        x = groupCenterX - (titleSize.width / 2f),
        y = topMostCoinTop - titleSize.height - gapPx,
    )
}

internal fun badgeCelebrationStaticTitleTopLeft(
    titleSize: IntSize,
    startCenter: Offset,
    badgeCount: Int,
    radius: Float,
    displayedCoinSizePx: Float,
    gapPx: Float,
): Offset = badgeCelebrationTitleTopLeft(
    titleSize = titleSize,
    coinCenters = badgeCelebrationInitialCoinCenters(
        badgeCount = badgeCount,
        startCenter = startCenter,
        radius = radius,
    ),
    groupCenterX = startCenter.x,
    displayedCoinSizePx = displayedCoinSizePx,
    gapPx = gapPx,
)

internal fun badgeCelebrationEndScale(badgeCount: Int): Float = when {
    badgeCount >= 6 -> 0.38f
    badgeCount >= 4 -> 0.44f
    else -> 0.50f
}

internal fun badgeCelebrationFanOffset(
    index: Int,
    radius: Float,
): Offset {
    if (index == 0) return Offset.Zero
    val step = (index + 1) / 2
    val side = if (index % 2 == 1) -1f else 1f
    val angleDegrees = 12f * step * side
    val angleRadians = angleDegrees * PI.toFloat() / 180f
    return Offset(
        x = sin(angleRadians) * radius,
        y = -step * radius * 0.16f,
    )
}
