package fr.aumombelli.dstcg.ui.component

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import fr.aumombelli.dstcg.model.DisplayCard
import fr.aumombelli.dstcg.performance.LocalAppPerformanceProfile
import fr.aumombelli.dstcg.ui.motion.autoplayHolographicMotion
import fr.aumombelli.dstcg.ui.theme.SkyQualityPalette
import fr.aumombelli.dstcg.ui.theme.skyQualityPalette

@Composable
fun AstroCardDetailsSurface(
    displayCard: DisplayCard,
    modifier: Modifier = Modifier,
    paletteOverride: SkyQualityPalette? = null,
    accessoryContent: (@Composable ColumnScope.() -> Unit)? = null,
) {
    val palette = paletteOverride ?: skyQualityPalette(displayCard.activeVariant.skyQuality)
    val imageCredit = rememberCardArtCredit(displayCard.definition)
    val performanceProfile = LocalAppPerformanceProfile.current
    val holoLoop = if (displayCard.activeVariant.isHolographic) {
        val transition = rememberInfiniteTransition(label = "fullscreen-holo")
        val animatedProgress by transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 4_600, easing = LinearEasing),
                repeatMode = RepeatMode.Restart,
            ),
            label = "fullscreen-holo-progress",
        )
        animatedProgress
    } else {
        0f
    }
    val holographicMotion = if (displayCard.activeVariant.isHolographic) {
        autoplayHolographicMotion(
            loopProgress = holoLoop,
            interactiveEffectsEnabled = performanceProfile.enableInteractiveHolographicEffects,
        )
    } else {
        null
    }

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
                    holographicMotion = holographicMotion,
                    paletteOverride = palette,
                    modifier = Modifier.fillMaxWidth(),
                )
                accessoryContent?.invoke(this)
                DescriptionBlock(displayCard)
                IdentitySection(displayCard)
                CoordinatesSection(displayCard)
                MeasurementsSection(displayCard)
                ImageCreditLine(
                    artistName = cardArtCreditArtistName(imageCredit?.artist),
                    licenseName = cardArtCreditLicenseName(imageCredit?.license),
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
    licenseName: String,
    modifier: Modifier = Modifier,
) {
    androidx.compose.foundation.layout.Column(
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(4.dp),
        modifier = modifier,
    ) {
        Text(
            text = "Crédit image",
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
        Text(
            text = "Licence : $licenseName",
            color = Color(0xFFE9F2FC),
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.testTag(CardImageLicenseTag),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
