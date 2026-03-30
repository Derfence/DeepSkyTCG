package fr.aumombelli.gatcha.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import fr.aumombelli.gatcha.model.DisplayCard
import fr.aumombelli.gatcha.performance.LocalAppPerformanceProfile
import fr.aumombelli.gatcha.ui.theme.skyQualityPalette

const val TRADING_CARD_WIDTH_OVER_HEIGHT = 1f / 1.754f

enum class AstroCardSurfaceMode {
    Thumbnail,
    Preview,
    PackReveal,
}

@Composable
fun AstroCardPreviewSurface(
    displayCard: DisplayCard,
    modifier: Modifier = Modifier,
    mode: AstroCardSurfaceMode = AstroCardSurfaceMode.Preview,
    onClick: (() -> Unit)? = null,
) {
    val palette = skyQualityPalette(displayCard.activeVariant.skyQuality)
    val performanceProfile = LocalAppPerformanceProfile.current
    val compact = mode == AstroCardSurfaceMode.Thumbnail
    val shape = RoundedCornerShape(if (compact) 24.dp else 30.dp)
    val artInset = cardArtInset(mode)
    val clickableModifier = if (onClick == null) {
        modifier
    } else {
        modifier.clickable(onClick = onClick)
    }
    val cardModifier = clickableModifier.aspectRatio(TRADING_CARD_WIDTH_OVER_HEIGHT)

    Card(
        shape = shape,
        modifier = cardModifier,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            palette.top,
                            palette.bottom,
                        ),
                    ),
                ),
        ) {
            CardArtBackground(
                definition = displayCard.definition,
                mode = mode,
                palette = palette,
                inset = artInset,
                artShape = cardArtShape(mode),
                modifier = Modifier.fillMaxSize(),
            )
            HeroAtmosphere(palette = palette)
            CardFaceScrim(modifier = Modifier.fillMaxSize())
            if (displayCard.activeVariant.isHolographic) {
                TwinklingStarsOverlay(
                    animated = mode != AstroCardSurfaceMode.Thumbnail ||
                        performanceProfile.enableAnimatedThumbnailTwinkles,
                    modifier = Modifier.fillMaxSize(),
                )
            }
            CardFaceContent(
                displayCard = displayCard,
                compact = compact,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(if (compact) 14.dp else 20.dp),
            )
        }
    }
}

private fun cardArtInset(mode: AstroCardSurfaceMode) = when (mode) {
    AstroCardSurfaceMode.Thumbnail -> 10.dp
    AstroCardSurfaceMode.Preview -> 12.dp
    AstroCardSurfaceMode.PackReveal -> 12.dp
}

private fun cardArtShape(mode: AstroCardSurfaceMode): Shape = when (mode) {
    AstroCardSurfaceMode.Thumbnail -> RoundedCornerShape(18.dp)
    AstroCardSurfaceMode.Preview -> RoundedCornerShape(24.dp)
    AstroCardSurfaceMode.PackReveal -> RoundedCornerShape(24.dp)
}

@Composable
private fun CardFaceScrim(
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.background(
            Brush.verticalGradient(
                colorStops = arrayOf(
                    0.0f to Color.Black.copy(alpha = 0.54f),
                    0.18f to Color.Black.copy(alpha = 0.18f),
                    0.4f to Color.Transparent,
                    0.6f to Color.Transparent,
                    0.82f to Color.Black.copy(alpha = 0.22f),
                    1.0f to Color.Black.copy(alpha = 0.56f),
                ),
            ),
        ),
    )
}
