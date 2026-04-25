package fr.aumombelli.dstcg.feature.packs.opening

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import fr.aumombelli.dstcg.ui.motion.AnimatedExtensionPackCard
import fr.aumombelli.dstcg.ui.component.TRADING_CARD_WIDTH_OVER_HEIGHT
import fr.aumombelli.dstcg.ui.motion.PackRevealBounds

@Composable
internal fun BoosterCover(
    extensionId: String,
    scale: Float,
    exitProgress: Float,
    decorSeed: Any? = Unit,
    onBoundsChanged: (PackRevealBounds?) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val easedExitProgress = easeInDownwardMotion(exitProgress)

    BoxWithConstraints(modifier = modifier) {
        val exitTranslationY = with(density) { maxHeight.toPx() } * 1.08f * easedExitProgress

        PackOpeningRevealCardFrame(
            modifier = Modifier.fillMaxSize(),
            onCardBoundsChanged = onBoundsChanged,
        ) {
            AnimatedExtensionPackCard(
                extensionId = extensionId,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(TRADING_CARD_WIDTH_OVER_HEIGHT)
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        translationY = exitTranslationY
                    }
                    .testTag("pack-opening-booster"),
                revealProgressOverride = 1f,
                decorSeed = decorSeed,
                showContainerChrome = false,
            )
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
