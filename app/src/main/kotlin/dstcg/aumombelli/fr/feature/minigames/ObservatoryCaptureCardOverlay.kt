package fr.aumombelli.dstcg.feature.minigames

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import fr.aumombelli.dstcg.model.DisplayCard
import fr.aumombelli.dstcg.ui.component.AstroCardPreviewSurface
import fr.aumombelli.dstcg.ui.component.AstroCardSurfaceMode
import fr.aumombelli.dstcg.ui.component.TRADING_CARD_WIDTH_OVER_HEIGHT

private const val ObservatoryCaptureCardWidthRatio = 0.18f
private val ObservatoryCaptureCardMinWidth = 96.dp
private val ObservatoryCaptureCardMaxWidth = 180.dp

@Composable
internal fun ObservatoryCaptureCardOverlay(
    targetCard: DisplayCard,
    state: ObservatoryIllustrationState,
    modifier: Modifier = Modifier,
) {
    val alpha = state.captureProgress.coerceIn(0f, 1f)
    if (state.step != ObservatoryStep.Capture || alpha <= 0f) return

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val density = LocalDensity.current
        val widthPx = with(density) { maxWidth.toPx() }
        val heightPx = with(density) { maxHeight.toPx() }
        if (widthPx <= 0f || heightPx <= 0f) return@BoxWithConstraints

        val geometry = ObservatoryGeometry.from(
            size = Size(widthPx, heightPx),
            state = state,
        )
        val cardWidthPx = observatoryCaptureCardWidthPx(
            unitPx = geometry.unit,
            minWidthPx = with(density) { ObservatoryCaptureCardMinWidth.toPx() },
            maxWidthPx = with(density) { ObservatoryCaptureCardMaxWidth.toPx() },
        )
        val cardHeightPx = cardWidthPx / TRADING_CARD_WIDTH_OVER_HEIGHT
        val cardWidth = with(density) { cardWidthPx.toDp() }
        val offsetX = with(density) { (geometry.target.x - cardWidthPx / 2f).toDp() }
        val offsetY = with(density) { (geometry.target.y - cardHeightPx / 2f).toDp() }

        Box(
            modifier = Modifier
                .offset(x = offsetX, y = offsetY)
                .alpha(alpha)
                .testTag("observatory-capture-card"),
        ) {
            AstroCardPreviewSurface(
                displayCard = targetCard,
                mode = AstroCardSurfaceMode.Thumbnail,
                modifier = Modifier.width(cardWidth),
            )
        }
    }
}

internal fun observatoryCaptureCardWidthPx(
    unitPx: Float,
    minWidthPx: Float,
    maxWidthPx: Float,
): Float = (unitPx * ObservatoryCaptureCardWidthRatio).coerceIn(
    minimumValue = minWidthPx,
    maximumValue = maxWidthPx,
)
