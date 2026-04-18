package fr.aumombelli.dstcg.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import fr.aumombelli.dstcg.model.AstronomyDetails
import fr.aumombelli.dstcg.model.CardDefinition
import fr.aumombelli.dstcg.model.ConstellationDetails
import fr.aumombelli.dstcg.model.DeepSkyDetails
import fr.aumombelli.dstcg.model.DisplayCard
import fr.aumombelli.dstcg.model.DisplayCardVariant
import fr.aumombelli.dstcg.model.LibraryCardItem
import fr.aumombelli.dstcg.model.SkyEventDetails
import fr.aumombelli.dstcg.model.StarDetails
import fr.aumombelli.dstcg.model.toDisplayCard

internal const val CardBackgroundArtTag = "astro-card-background-art"
internal const val CardBackgroundFallbackAssetTag = "astro-card-background-fallback-asset"
internal const val CardBackgroundHiddenPlaceholderTag = "astro-card-background-hidden-placeholder"
internal const val CardBackgroundPlaceholderTag = "astro-card-background-placeholder"
internal const val CardFooterTag = "astro-card-footer"
internal const val CardCatalogNumberTag = "astro-card-catalog-number"
internal const val CardVariationTag = "astro-card-variation"
internal const val CardExtensionLogoTag = "astro-card-extension-logo"

internal const val CardArtFallbackAssetPath = "card_art/_fallbacks/missing.webp"

internal data class CardHeadlineContent(
    val title: String,
    val catalogLine: String?,
)

internal fun cardArtAssetPath(definition: CardDefinition): String =
    "card_art/${definition.extensionId}/${definition.imageRef}.webp"

internal fun cardHeadlineContent(definition: CardDefinition): CardHeadlineContent {
    val catalogNumber = definition.astronomy.catalogNumber.trim()
    val commonName = definition.astronomy.commonName
        ?.trim()
        ?.takeIf { it.isNotEmpty() && !it.equals(catalogNumber, ignoreCase = true) }
    return if (commonName != null) {
        CardHeadlineContent(
            title = commonName,
            catalogLine = catalogNumber,
        )
    } else {
        CardHeadlineContent(
            title = catalogNumber,
            catalogLine = null,
        )
    }
}

@Composable
internal fun CardFaceContent(
    displayCard: DisplayCard,
    compact: Boolean,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier,
    ) {
        CardHeader(
            displayCard = displayCard,
            compact = compact,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth(),
        )
        CardFooter(
            displayCard = displayCard,
            compact = compact,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth(),
        )
    }
}

@Composable
internal fun CardHeader(
    displayCard: DisplayCard,
    compact: Boolean,
    modifier: Modifier = Modifier,
) {
    val headline = cardHeadlineContent(displayCard.definition)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(if (compact) 4.dp else 6.dp),
        modifier = modifier
            .heightIn(min = if (compact) 86.dp else 122.dp)
            .padding(horizontal = if (compact) 8.dp else 14.dp),
    ) {
        Text(
            text = headline.title,
            color = Color.White,
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Bold,
            style = if (compact) MaterialTheme.typography.titleMedium else MaterialTheme.typography.headlineMedium,
            modifier = Modifier.fillMaxWidth(),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        headline.catalogLine?.let { catalogLine ->
            Text(
                text = catalogLine,
                color = Color(0xFFE9F2FC),
                textAlign = TextAlign.Center,
                style = if (compact) MaterialTheme.typography.labelMedium else MaterialTheme.typography.labelLarge,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(CardCatalogNumberTag),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Text(
            text = buildVariantLine(displayCard.activeVariant),
            color = Color(0xFFF7EEC7),
            textAlign = TextAlign.Center,
            style = if (compact) MaterialTheme.typography.labelMedium else MaterialTheme.typography.titleSmall,
            modifier = Modifier
                .fillMaxWidth()
                .testTag(CardVariationTag),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
internal fun CardFooter(
    displayCard: DisplayCard,
    compact: Boolean,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.Bottom,
        modifier = modifier
            .height(if (compact) 62.dp else 74.dp)
            .testTag(CardFooterTag),
    ) {
        FooterTextBlock(
            label = "Constellation",
            value = displayCard.definition.astronomy.constellation,
            compact = compact,
            horizontalAlignment = Alignment.Start,
            modifier = Modifier.weight(1f),
        )
        Box(
            contentAlignment = Alignment.BottomCenter,
            modifier = Modifier
                .weight(1f)
                .wrapContentHeight(align = Alignment.Bottom),
        ) {
            ExtensionLogoMark(
                extensionId = displayCard.definition.extensionId,
                compact = compact,
                modifier = Modifier.testTag(CardExtensionLogoTag),
            )
        }
        Box(
            contentAlignment = Alignment.BottomEnd,
            modifier = Modifier
                .weight(1f)
                .wrapContentHeight(align = Alignment.Bottom),
        ) {
            val rarityBadgeSize = if (compact) 36.dp else 48.dp
            val stampedSealWidth = if (compact) 60.dp else 84.dp
            val stampedSealHeight = if (compact) 10.dp else 14.dp
            val stampedSealVerticalGap = if (compact) 2.dp else 4.dp
            val stampedSealHorizontalOverlap = if (compact) 4.dp else 6.dp
            val stampedSealLeftShift =
                (stampedSealWidth + stampedSealHeight) * RotatedStampHalfHorizontalFootprintFactor
            val clusterHeight = if (displayCard.activeVariant.isStamped) {
                rarityBadgeSize + stampedSealVerticalGap + stampedSealHeight
            } else {
                rarityBadgeSize
            }

            Box(
                modifier = Modifier
                    .requiredSize(
                        width = rarityBadgeSize,
                        height = clusterHeight,
                    ),
            ) {
                if (displayCard.activeVariant.isStamped) {
                    StampedSealOverlay(
                        compact = compact,
                        modifier = Modifier
                            .requiredSize(
                                width = stampedSealWidth,
                                height = stampedSealHeight,
                            )
                            .align(Alignment.TopEnd)
                            .offset(x = stampedSealHorizontalOverlap - stampedSealLeftShift),
                    )
                }
                RarityStarBadge(
                    rarityLabel = displayCard.definition.rarityLabel,
                    modifier = Modifier
                        .size(rarityBadgeSize)
                        .align(Alignment.BottomEnd)
                        .testTag(rarityBadgeTag(displayCard.definition.rarityLabel)),
                )
            }
        }
    }
}

@Composable
private fun FooterTextBlock(
    label: String,
    value: String,
    compact: Boolean,
    horizontalAlignment: Alignment.Horizontal,
    modifier: Modifier = Modifier,
) {
    Column(
        horizontalAlignment = horizontalAlignment,
        verticalArrangement = Arrangement.Bottom,
        modifier = modifier.wrapContentHeight(align = Alignment.Bottom),
    ) {
        Text(
            text = label,
            color = Color(0xFFBDD4EA),
            style = if (compact) MaterialTheme.typography.labelSmall else MaterialTheme.typography.labelMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = value,
            color = Color.White,
            fontWeight = FontWeight.SemiBold,
            textAlign = when (horizontalAlignment) {
                Alignment.CenterHorizontally -> TextAlign.Center
                Alignment.End -> TextAlign.End
                else -> TextAlign.Start
            },
            style = if (compact) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.titleSmall,
            modifier = Modifier.fillMaxWidth(),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
internal fun DescriptionBlock(displayCard: DisplayCard) {
    SectionCard(
        title = "Description",
        containerColor = if (displayCard.activeVariant.isHolographic) {
            Color.Black.copy(alpha = 0.72f)
        } else {
            Color.Black.copy(alpha = 0.18f)
        },
    ) {
        Text(
            text = displayCard.definition.astronomy.shortDescription,
            color = Color(0xFFF3F7FB),
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}

@Composable
internal fun IdentitySection(displayCard: DisplayCard) {
    val astronomy = displayCard.definition.astronomy
    val items = buildList {
        astronomy.commonName?.let { add("Nom commun" to it) }
        add("Catalogue" to astronomy.primaryCatalogName)
        add("Numero" to astronomy.catalogNumber)
        add("Type d'objet" to astronomy.objectTypeLabel)
        add("Constellation" to astronomy.constellation)
        add("Saison" to astronomy.mainSeason)
        add("Qualite du ciel" to displayCard.activeVariant.skyQualityLabel)
        add("Finition" to displayCard.activeVariant.finishLabel)
    }

    SectionCard(title = "Identite") {
        KeyValueList(items)
    }
}

@Composable
internal fun CoordinatesSection(displayCard: DisplayCard) {
    val coordinates = displayCard.definition.astronomy.coordinates
    val items = listOf(
        "Coordonnees" to coordinates.label,
        "Ascension droite" to coordinates.rightAscension.label,
        "Declinaison" to coordinates.declination.label,
    )

    SectionCard(title = "Coordonnees Célestes") {
        KeyValueList(items)
    }
}

@Composable
internal fun MeasurementsSection(displayCard: DisplayCard) {
    val items = measurementItems(displayCard.definition.astronomy.details)
    if (items.isEmpty()) return

    SectionCard(title = "Mesures") {
        KeyValueList(items)
    }
}

@Composable
internal fun SectionCard(
    title: String,
    containerColor: Color = Color.Black.copy(alpha = 0.18f),
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        color = containerColor,
        shape = RoundedCornerShape(22.dp),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            Text(
                text = title,
                color = Color(0xFFFFF0BA),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            content()
        }
    }
}

@Composable
internal fun KeyValueList(items: List<Pair<String, String>>) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        items.forEach { (label, value) ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = label,
                    color = Color(0xFFC6D9EC),
                    fontWeight = FontWeight.Medium,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.width(136.dp),
                )
                Text(
                    text = value,
                    color = Color.White,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

internal fun measurementItems(details: AstronomyDetails): List<Pair<String, String>> = when (details) {
    is DeepSkyDetails -> buildList {
        add("Distance a la Terre" to details.distance.label)
        add("Taille reelle" to details.realSize.label)
        add("Taille visuelle" to details.visualSize.label)
        details.absoluteMagnitude?.let { add("Magnitude absolue" to it.label) }
    }
    is StarDetails -> buildList {
        add("Distance a la Terre" to details.distance.label)
        details.realSize?.let { add("Taille reelle" to it.label) }
        details.visualSize?.let { add("Taille visuelle" to it.label) }
        add("Magnitude absolue" to details.absoluteMagnitude.label)
    }
    is ConstellationDetails -> listOf(
        "Taille visuelle" to details.visualSize.label,
    )
    is SkyEventDetails -> details.visualSize?.let {
        listOf("Taille visuelle" to it.label)
    } ?: emptyList()
}

internal fun buildVariantLine(variant: DisplayCardVariant): String = buildString {
    append(variant.skyQualityLabel)
    append(" · ")
    append(variant.finishLabel)
}

internal fun rarityBadgeTag(rarityLabel: String): String =
    "astro-card-rarity-${rarityLabel.lowercase()}"

private const val RotatedStampHalfHorizontalFootprintFactor = 0.35355338f

internal fun fallbackDisplayCard(item: LibraryCardItem) =
    item.definition.toDisplayCard(
        extensionName = item.extensionName,
        activeVariant = DisplayCardVariant(
            skyQuality = "city",
            skyQualityLabel = "Ville",
            finish = "standard",
            finishLabel = "Standard",
            isHolographic = false,
            isStamped = false,
        ),
    )
