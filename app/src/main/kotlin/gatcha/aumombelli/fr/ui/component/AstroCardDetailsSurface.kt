package fr.aumombelli.gatcha.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import fr.aumombelli.gatcha.model.DisplayCard
import fr.aumombelli.gatcha.ui.theme.skyQualityPalette

@Composable
fun AstroCardDetailsSurface(
    displayCard: DisplayCard,
    modifier: Modifier = Modifier,
    accessoryContent: (@Composable ColumnScope.() -> Unit)? = null,
) {
    val palette = skyQualityPalette(displayCard.activeVariant.skyQuality)
    val imageCredit = rememberCardArtCredit(displayCard.definition)

    Card(
        shape = androidx.compose.foundation.shape.RoundedCornerShape(34.dp),
        modifier = modifier.navigationBarsPadding(),
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
            HeroAtmosphere(palette = palette)
            if (displayCard.activeVariant.isHolographic) {
                TwinklingStarsOverlay(modifier = Modifier.fillMaxSize())
            }
            Column(
                verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(18.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(start = 22.dp, top = 22.dp, end = 22.dp, bottom = 44.dp),
            ) {
                AstroCardPreviewSurface(
                    displayCard = displayCard,
                    mode = AstroCardSurfaceMode.Preview,
                    modifier = Modifier.fillMaxWidth(),
                )
                accessoryContent?.invoke(this)
                DescriptionBlock(displayCard.definition)
                IdentitySection(displayCard)
                CoordinatesSection(displayCard)
                MeasurementsSection(displayCard)
                ImageCreditLine(
                    artistName = cardArtCreditArtistName(imageCredit?.artist),
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun ImageCreditLine(
    artistName: String,
    modifier: Modifier = Modifier,
) {
    androidx.compose.foundation.layout.Column(
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(4.dp),
        modifier = modifier,
    ) {
        Text(
            text = "Credit image",
            color = Color(0xFFBDD4EA),
            style = MaterialTheme.typography.labelMedium,
        )
        Text(
            text = artistName,
            color = Color.White,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.testTag(CardImageCreditTag),
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
