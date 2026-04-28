package fr.aumombelli.dstcg.ui.component

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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import fr.aumombelli.dstcg.model.DisplayCard
import fr.aumombelli.dstcg.performance.LocalAppPerformanceProfile
import fr.aumombelli.dstcg.ui.motion.HolographicCardMotion
import fr.aumombelli.dstcg.ui.theme.SkyQualityPalette
import fr.aumombelli.dstcg.ui.theme.skyQualityPalette

const val TRADING_CARD_WIDTH_OVER_HEIGHT = 1f / 1.754f

enum class AstroCardSurfaceMode {
    Thumbnail,
    Preview,
    PackReveal,
}

internal enum class CardArtVisibility {
    Visible,
    Hidden,
}

@Composable
internal fun AstroCardPreviewSurface(
    displayCard: DisplayCard,
    modifier: Modifier = Modifier,
    mode: AstroCardSurfaceMode = AstroCardSurfaceMode.Preview,
    artVisibility: CardArtVisibility = CardArtVisibility.Visible,
    holographicMotion: HolographicCardMotion? = null,
    paletteOverride: SkyQualityPalette? = null,
    onClick: (() -> Unit)? = null,
) {
    val palette = paletteOverride ?: skyQualityPalette(displayCard.activeVariant.skyQuality)
    val performanceProfile = LocalAppPerformanceProfile.current
    val compact = mode == AstroCardSurfaceMode.Thumbnail
    val shape = RoundedCornerShape(if (compact) 24.dp else 30.dp)
    val artInset = cardArtInset(mode)
    val interactiveHoloMotion = holographicMotion
        ?.takeIf { displayCard.activeVariant.isHolographic }
    val renderedHoloMotion = if (displayCard.activeVariant.isHolographic) {
        interactiveHoloMotion ?: HolographicCardMotion()
    } else {
        null
    }
    val clickableModifier = if (onClick == null) {
        modifier
    } else {
        modifier.clickable(onClick = onClick)
    }
    val cardModifier = clickableModifier
        .aspectRatio(TRADING_CARD_WIDTH_OVER_HEIGHT)
        .graphicsLayer {
            rotationY = interactiveHoloMotion?.rotationYDeg ?: 0f
            cameraDistance = 16f * density * 72f
        }

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
                artVisibility = artVisibility,
                modifier = Modifier.fillMaxSize(),
            )
            CardFaceScrim(modifier = Modifier.fillMaxSize())
            if (renderedHoloMotion != null) {
                HolographicFoilOverlay(
                    motion = renderedHoloMotion,
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("astro-card-holo-foil"),
                )
                HolographicGlareOverlay(
                    motion = renderedHoloMotion,
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("astro-card-holo-glare"),
                )
                HolographicRimLightOverlay(
                    motion = renderedHoloMotion,
                    modifier = Modifier.fillMaxSize(),
                )
                TwinklingStarsOverlay(
                    animated = mode != AstroCardSurfaceMode.Thumbnail ||
                        performanceProfile.enableAnimatedThumbnailTwinkles,
                    sparkleBoost = if (performanceProfile.enableInteractiveHolographicEffects) {
                        interactiveHoloMotion?.sparkleBoost ?: 0f
                    } else {
                        0f
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .then(
                            if (
                                interactiveHoloMotion != null &&
                                performanceProfile.enableInteractiveHolographicEffects
                            ) {
                                Modifier.testTag("astro-card-holo-sparkles")
                            } else {
                                Modifier
                            },
                        ),
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
