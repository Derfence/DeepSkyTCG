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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
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
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}
