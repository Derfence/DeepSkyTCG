package fr.aumombelli.gatcha.ui.component

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
import fr.aumombelli.gatcha.ui.theme.rarityBadgeStyle
import fr.aumombelli.gatcha.ui.theme.skyQualityPalette
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

const val TRADING_CARD_WIDTH_OVER_HEIGHT = 1f / 1.754f

enum class AstroCardSurfaceMode {
    Thumbnail,
    Preview,
    PackReveal,
}

@Composable
fun AstroCardThumbnail(
    item: LibraryCardItem,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val owned = item.ownedCount > 0
    val displayCard = remember(item) { item.toDisplayCard() ?: fallbackDisplayCard(item) }

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier.testTag("library-card-${item.definition.id}"),
    ) {
        Box(
            modifier = Modifier.alpha(if (owned) 1f else 0.42f),
        ) {
            AstroCardPreviewSurface(
                displayCard = displayCard,
                mode = AstroCardSurfaceMode.Thumbnail,
                modifier = Modifier.fillMaxWidth(),
                onClick = if (owned) onClick else null,
            )
            QuantityPill(
                text = if (owned) "×${item.ownedCount}" else "0",
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(10.dp),
            )
        }
        Text(
            text = if (owned) "Owned: ${item.ownedCount}" else "Not owned yet",
            color = Color(0xFFD3E3F4),
            modifier = Modifier.testTag("library-owned-${item.definition.id}"),
        )
    }
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
    val shape = RoundedCornerShape(if (compact) 24.dp else 30.dp)
    val clickableModifier = if (onClick == null) {
        modifier
    } else {
        modifier.clickable(onClick = onClick)
    }
    val cardModifier = if (mode == AstroCardSurfaceMode.Thumbnail) {
        clickableModifier
    } else {
        clickableModifier.aspectRatio(TRADING_CARD_WIDTH_OVER_HEIGHT)
    }

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

@Composable
fun AstroCardDetailsSurface(
    displayCard: DisplayCard,
    modifier: Modifier = Modifier,
    accessoryContent: (@Composable ColumnScope.() -> Unit)? = null,
) {
    val palette = skyQualityPalette(displayCard.activeVariant.skyQuality)

    Card(
        shape = RoundedCornerShape(34.dp),
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
                verticalArrangement = Arrangement.spacedBy(18.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(start = 22.dp, top = 22.dp, end = 22.dp, bottom = 44.dp),
            ) {
                CardHeader(
                    displayCard = displayCard,
                    compact = false,
                )
                accessoryContent?.invoke(this)
                CardHero(
                    displayCard = displayCard,
                    mode = AstroCardSurfaceMode.Preview,
                )
                DescriptionBlock(displayCard.definition)
                IdentitySection(displayCard)
                CoordinatesSection(displayCard)
                MeasurementsSection(displayCard)
                CardFooter(
                    definition = displayCard.definition,
                    rarityLabel = displayCard.definition.rarityLabel,
                    compact = false,
                )
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
fun DisplayCardVariantSelector(
    variants: List<DisplayCardVariant>,
    selectedVariantKey: String,
    onVariantSelected: (DisplayCardVariant) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (variants.size <= 1) return

    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier.horizontalScroll(rememberScrollState()),
    ) {
        variants.forEach { variant ->
            FilterChip(
                selected = variant.key == selectedVariantKey,
                onClick = { onVariantSelected(variant) },
                label = { Text(variant.selectorLabel) },
                modifier = Modifier.testTag("astro-card-variant-${variant.skyQuality}-${variant.finish}"),
            )
        }
    }
}

@Composable
private fun CardHeader(
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
    )
    Text(
        text = displayCard.extensionName,
        color = Color(0xFFE6EFF8),
        textAlign = TextAlign.Center,
        style = if (compact) MaterialTheme.typography.labelLarge else MaterialTheme.typography.titleSmall,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun CardHero(
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
            )
    ) {
        if (mode == AstroCardSurfaceMode.Thumbnail) {
            Box(
                modifier = Modifier
                    .size(88.dp)
                    .clip(CircleShape)
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
private fun CardFooter(
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
private fun DescriptionBlock(definition: CardDefinition) {
    SectionCard(title = "Description") {
        Text(
            text = definition.astronomy.shortDescription,
            color = Color(0xFFF3F7FB),
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}

@Composable
private fun IdentitySection(displayCard: DisplayCard) {
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
private fun CoordinatesSection(displayCard: DisplayCard) {
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
private fun MeasurementsSection(displayCard: DisplayCard) {
    val items = measurementItems(displayCard.definition.astronomy.details)
    if (items.isEmpty()) return

    SectionCard(title = "Mesures") {
        KeyValueList(items)
    }
}

@Composable
private fun SectionCard(
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
private fun KeyValueList(items: List<Pair<String, String>>) {
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

@Composable
private fun QuantityPill(
    text: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        color = Color.Black.copy(alpha = 0.35f),
        shape = CircleShape,
        modifier = modifier,
    ) {
        Text(
            text = text,
            color = Color.White,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
        )
    }
}

@Composable
private fun HeroAtmosphere(
    palette: fr.aumombelli.gatcha.ui.theme.SkyQualityPalette,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        palette.glow,
                        Color.Transparent,
                    ),
                    center = Offset(280f, 120f),
                    radius = 520f,
                ),
            ),
    )
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        palette.mist,
                        Color.Transparent,
                    ),
                    center = Offset(120f, 340f),
                    radius = 420f,
                ),
            ),
    )
}

@Composable
private fun RarityStarBadge(
    rarityLabel: String,
    modifier: Modifier = Modifier,
) {
    val style = rarityBadgeStyle(rarityLabel)

    Canvas(modifier = modifier) {
        val center = Offset(size.width / 2f, size.height / 2f)
        drawCircle(
            color = style.glowColor,
            radius = size.minDimension * 0.44f,
            center = center,
        )
        drawPath(
            path = starPath(
                center = center,
                points = style.branchCount,
                outerRadius = size.minDimension * 0.34f,
                innerRadius = size.minDimension * 0.16f,
            ),
            color = style.color,
            style = Fill,
        )
        drawPath(
            path = starPath(
                center = center,
                points = style.branchCount,
                outerRadius = size.minDimension * 0.34f,
                innerRadius = size.minDimension * 0.16f,
            ),
            color = Color.White.copy(alpha = 0.75f),
            style = Stroke(width = size.minDimension * 0.03f),
        )
    }
}

@Composable
private fun TwinklingStarsOverlay(
    modifier: Modifier = Modifier,
) {
    val transition = rememberInfiniteTransition(label = "twinkle")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "twinkle-progress",
    )

    Canvas(modifier = modifier) {
        TwinkleStars.forEach { star ->
            val alpha = 0.18f + 0.82f * ((sin((progress + star.phase) * PI * 2).toFloat() + 1f) / 2f)
            val center = Offset(size.width * star.x, size.height * star.y)
            drawPath(
                path = starPath(
                    center = center,
                    points = 4,
                    outerRadius = size.minDimension * star.radius,
                    innerRadius = size.minDimension * star.radius * 0.42f,
                ),
                color = Color.White.copy(alpha = alpha),
            )
            drawCircle(
                color = Color(0xFFFFF8D6).copy(alpha = alpha * 0.7f),
                radius = size.minDimension * star.radius * 0.12f,
                center = center,
            )
        }
    }
}

private fun measurementItems(details: AstronomyDetails): List<Pair<String, String>> = when (details) {
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

private fun buildVariantLine(variant: DisplayCardVariant): String = buildString {
    append(variant.skyQualityLabel)
    append(" · ")
    append(variant.finishLabel)
}

private fun fallbackDisplayCard(item: LibraryCardItem): DisplayCard =
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

private fun starPath(
    center: Offset,
    points: Int,
    outerRadius: Float,
    innerRadius: Float,
): Path {
    val path = Path()
    val angleStep = PI / points

    repeat(points * 2) { index ->
        val radius = if (index % 2 == 0) outerRadius else innerRadius
        val angle = -PI / 2 + angleStep * index
        val x = center.x + radius * cos(angle).toFloat()
        val y = center.y + radius * sin(angle).toFloat()
        if (index == 0) {
            path.moveTo(x, y)
        } else {
            path.lineTo(x, y)
        }
    }
    path.close()
    return path
}

private data class TwinkleStar(
    val x: Float,
    val y: Float,
    val radius: Float,
    val phase: Float,
)

private val TwinkleStars = listOf(
    TwinkleStar(0.16f, 0.14f, 0.024f, 0.0f),
    TwinkleStar(0.78f, 0.18f, 0.019f, 0.17f),
    TwinkleStar(0.64f, 0.34f, 0.016f, 0.39f),
    TwinkleStar(0.24f, 0.44f, 0.021f, 0.56f),
    TwinkleStar(0.83f, 0.58f, 0.018f, 0.73f),
    TwinkleStar(0.14f, 0.72f, 0.02f, 0.88f),
    TwinkleStar(0.52f, 0.78f, 0.014f, 0.28f),
    TwinkleStar(0.72f, 0.86f, 0.022f, 0.49f),
)
