package fr.aumombelli.gatcha.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
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

@Composable
internal fun CardHeader(
    displayCard: DisplayCard,
    compact: Boolean,
) {
    Text(
        text = displayCard.definition.name,
        color = Color.White,
        textAlign = TextAlign.Center,
        fontWeight = FontWeight.Bold,
        style = if (compact) MaterialTheme.typography.titleMedium else MaterialTheme.typography.headlineMedium,
        modifier = Modifier.fillMaxWidth(),
        maxLines = if (compact) 2 else Int.MAX_VALUE,
        overflow = if (compact) TextOverflow.Ellipsis else TextOverflow.Clip,
    )
    Text(
        text = displayCard.extensionName,
        color = Color(0xFFE6EFF8),
        textAlign = TextAlign.Center,
        style = if (compact) MaterialTheme.typography.labelLarge else MaterialTheme.typography.titleSmall,
        modifier = Modifier.fillMaxWidth(),
        maxLines = if (compact) 1 else Int.MAX_VALUE,
        overflow = if (compact) TextOverflow.Ellipsis else TextOverflow.Clip,
    )
}

@Composable
internal fun CardHero(
    displayCard: DisplayCard,
    mode: AstroCardSurfaceMode,
) {
    val compact = mode == AstroCardSurfaceMode.Thumbnail

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxWidth()
            .height(if (compact) 160.dp else 230.dp)
            .clip(RoundedCornerShape(if (compact) 20.dp else 28.dp))
            .background(
                Brush.radialGradient(
                    listOf(
                        Color.White.copy(alpha = 0.18f),
                        Color.Transparent,
                    ),
                ),
            ),
    ) {
        if (mode == AstroCardSurfaceMode.Thumbnail) {
            Box(
                modifier = Modifier
                    .size(88.dp)
                    .clip(androidx.compose.foundation.shape.CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.2f),
                                Color.White.copy(alpha = 0.06f),
                                Color.Transparent,
                            ),
                        ),
                    ),
            )
        } else {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.padding(12.dp),
            ) {
                Text(
                    text = displayCard.definition.astronomy.catalogNumber,
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    style = (if (compact) {
                        MaterialTheme.typography.headlineMedium
                    } else {
                        MaterialTheme.typography.displaySmall
                    }).copy(
                        shadow = Shadow(
                            color = Color.Black.copy(alpha = 0.35f),
                            offset = Offset(0f, 5f),
                            blurRadius = 18f,
                        ),
                    ),
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = displayCard.definition.astronomy.objectTypeLabel,
                    color = Color(0xFFF4F8FC),
                    textAlign = TextAlign.Center,
                    style = if (compact) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.titleMedium,
                )
                Surface(
                    color = Color.Black.copy(alpha = 0.22f),
                    shape = RoundedCornerShape(50),
                ) {
                    Text(
                        text = buildVariantLine(displayCard.activeVariant),
                        color = Color(0xFFF9F4DA),
                        textAlign = TextAlign.Center,
                        style = if (compact) MaterialTheme.typography.labelMedium else MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    )
                }
            }
        }
    }
}

@Composable
internal fun CardFooter(
    definition: CardDefinition,
    rarityLabel: String,
    compact: Boolean,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(2.dp),
            modifier = Modifier.weight(1f),
        ) {
            Text(
                text = "Constellation",
                color = Color(0xFFBDD4EA),
                style = if (compact) MaterialTheme.typography.labelSmall else MaterialTheme.typography.labelMedium,
            )
            Text(
                text = definition.astronomy.constellation,
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
                style = if (compact) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.titleSmall,
            )
        }
        RarityStarBadge(
            rarityLabel = rarityLabel,
            modifier = Modifier.size(if (compact) 36.dp else 48.dp),
        )
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
