package fr.aumombelli.gatcha.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import fr.aumombelli.gatcha.model.DisplayCard
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
    accessoryContent: (@Composable ColumnScope.() -> Unit)? = null,
) {
    val palette = skyQualityPalette(displayCard.activeVariant.skyQuality)
    val compact = mode == AstroCardSurfaceMode.Thumbnail
    val shape = androidx.compose.foundation.shape.RoundedCornerShape(if (compact) 24.dp else 30.dp)
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
                .then(
                    if (mode == AstroCardSurfaceMode.Thumbnail) {
                        Modifier.fillMaxWidth()
                    } else {
                        Modifier.fillMaxSize()
                    },
                )
                .background(
                    Brush.verticalGradient(
                        listOf(
                            palette.top,
                            palette.bottom,
                        ),
                    ),
                )
                .padding(if (compact) 14.dp else 20.dp),
        ) {
            HeroAtmosphere(palette = palette)
            if (displayCard.activeVariant.isHolographic) {
                TwinklingStarsOverlay(modifier = Modifier.fillMaxSize())
            }
            Column(
                verticalArrangement = Arrangement.spacedBy(if (compact) 10.dp else 14.dp),
                modifier = if (compact) {
                    Modifier.fillMaxWidth()
                } else {
                    Modifier.fillMaxSize()
                },
            ) {
                CardHeader(
                    displayCard = displayCard,
                    compact = compact,
                )
                accessoryContent?.invoke(this)
                CardHero(
                    displayCard = displayCard,
                    mode = mode,
                )
                if (!compact) {
                    Spacer(modifier = Modifier.weight(1f))
                }
                CardFooter(
                    definition = displayCard.definition,
                    rarityLabel = displayCard.definition.rarityLabel,
                    compact = compact,
                )
            }
        }
    }
}
