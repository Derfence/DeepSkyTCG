package fr.aumombelli.gatcha.feature.packs.opening

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.IntOffset
import fr.aumombelli.gatcha.ui.component.TRADING_CARD_WIDTH_OVER_HEIGHT
import fr.aumombelli.gatcha.ui.motion.AnimatedExtensionPackCard
import fr.aumombelli.gatcha.ui.motion.PACK_REVEAL_WIDTH_FRACTION
import fr.aumombelli.gatcha.ui.motion.PackRevealBounds
import kotlin.math.roundToInt

@Composable
internal fun BoosterCover(
    extensionId: String,
    scale: Float,
    exitProgress: Float,
    initialBoosterBounds: PackRevealBounds?,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val easedExitProgress = easeInDownwardMotion(exitProgress)

    BoxWithConstraints(modifier = modifier) {
        val viewportHeightPx = with(density) { maxHeight.toPx() }
        if (initialBoosterBounds != null) {
            val width = with(density) { initialBoosterBounds.widthPx.toDp() }
            val height = with(density) { initialBoosterBounds.heightPx.toDp() }
            val exitTranslationY = (
                (viewportHeightPx - initialBoosterBounds.topPx) +
                    initialBoosterBounds.heightPx * 1.18f
                ) * easedExitProgress

            AnimatedExtensionPackCard(
                extensionId = extensionId,
                modifier = Modifier
                    .offset {
                        IntOffset(
                            x = initialBoosterBounds.leftPx.roundToInt(),
                            y = initialBoosterBounds.topPx.roundToInt(),
                        )
                    }
                    .size(width = width, height = height)
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        translationY = exitTranslationY
                    }
                    .testTag("pack-opening-booster"),
                revealProgressOverride = 1f,
            )
        } else {
            val exitTranslationY = viewportHeightPx * 1.04f * easedExitProgress
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxSize(),
            ) {
                AnimatedExtensionPackCard(
                    extensionId = extensionId,
                    modifier = Modifier
                        .fillMaxWidth(PACK_REVEAL_WIDTH_FRACTION)
                        .aspectRatio(TRADING_CARD_WIDTH_OVER_HEIGHT)
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                            translationY = exitTranslationY
                        }
                        .testTag("pack-opening-booster"),
                    revealProgressOverride = 1f,
                )
            }
        }
    }
}

internal fun normalizedPhase(
    progress: Float,
    start: Float,
    end: Float,
): Float = ((progress - start) / (end - start).coerceAtLeast(0.0001f)).coerceIn(0f, 1f)

private fun easeInDownwardMotion(progress: Float): Float {
    val clamped = progress.coerceIn(0f, 1f)
    return clamped * clamped
}
