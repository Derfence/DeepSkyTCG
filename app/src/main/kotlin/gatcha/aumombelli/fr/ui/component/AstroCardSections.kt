package fr.aumombelli.gatcha.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.platform.testTag
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import fr.aumombelli.gatcha.model.AstronomyDetails
import fr.aumombelli.gatcha.model.CardDefinition
import fr.aumombelli.gatcha.model.ConstellationDetails
import fr.aumombelli.gatcha.model.DeepSkyDetails
import fr.aumombelli.gatcha.model.DisplayCard
import fr.aumombelli.gatcha.model.DisplayCardVariant
import fr.aumombelli.gatcha.model.LibraryCardItem
import fr.aumombelli.gatcha.model.SkyEventDetails
import fr.aumombelli.gatcha.model.StarDetails
import fr.aumombelli.gatcha.model.toDisplayCard
import fr.aumombelli.gatcha.ui.motion.ExtensionAnimationStyle
import fr.aumombelli.gatcha.ui.motion.ExtensionConstellationOverlay
import fr.aumombelli.gatcha.ui.motion.LaunchLogoMark
import fr.aumombelli.gatcha.ui.motion.extensionAnimationSpec

internal const val CardBackgroundArtTag = "astro-card-background-art"
internal const val CardBackgroundFallbackAssetTag = "astro-card-background-fallback-asset"
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
        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.Bottom,
            modifier = Modifier
                .weight(1f)
                .wrapContentHeight(align = Alignment.Bottom),
        ) {
            RarityStarBadge(
                rarityLabel = displayCard.definition.rarityLabel,
                modifier = Modifier
                    .size(if (compact) 36.dp else 48.dp)
                    .testTag(rarityBadgeTag(displayCard.definition.rarityLabel)),
            )
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
private fun ExtensionLogoMark(
    extensionId: String,
    compact: Boolean,
    modifier: Modifier = Modifier,
) {
    val spec = remember(extensionId) { extensionAnimationSpec(extensionId) }
    val size = if (compact) 32.dp else 40.dp
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.size(size),
    ) {
        if (spec.style == ExtensionAnimationStyle.BigDipper) {
            ExtensionConstellationOverlay(
                spec = spec,
                lineProgress = 1f,
                isReversing = false,
                modifier = Modifier.fillMaxSize(),
                tag = null,
            )
        } else {
            LaunchLogoMark(
                showWordmark = false,
                emblemSize = size,
            )
        }
    }
}

@Composable
internal fun DescriptionBlock(definition: CardDefinition) {
    SectionCard(title = "Description") {
        Text(
            text = definition.astronomy.shortDescription,
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
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        color = Color.Black.copy(alpha = 0.18f),
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

internal fun fallbackDisplayCard(item: LibraryCardItem) =
    item.definition.toDisplayCard(
        extensionName = item.extensionName,
        activeVariant = DisplayCardVariant(
            skyQuality = "city",
            skyQualityLabel = "Ville",
            finish = "standard",
            finishLabel = "Standard",
            isHolographic = false,
        ),
    )
