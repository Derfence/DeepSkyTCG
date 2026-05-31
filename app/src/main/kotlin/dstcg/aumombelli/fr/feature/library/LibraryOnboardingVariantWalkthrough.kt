package fr.aumombelli.dstcg.feature.library

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import fr.aumombelli.dstcg.model.CardDefinition
import fr.aumombelli.dstcg.model.DisplayCard
import fr.aumombelli.dstcg.model.ExtensionDefinition
import fr.aumombelli.dstcg.model.NewPlayerOnboardingContent
import fr.aumombelli.dstcg.model.LibraryVariantWalkthroughPageContent
import fr.aumombelli.dstcg.model.LibraryVariantWalkthroughVisualKind
import fr.aumombelli.dstcg.model.VariantProfile
import fr.aumombelli.dstcg.model.toDisplayCard
import fr.aumombelli.dstcg.model.toDisplayVariant
import fr.aumombelli.dstcg.ui.component.AstroCardPreviewSurface
import fr.aumombelli.dstcg.ui.component.AstroCardSurfaceMode
import fr.aumombelli.dstcg.ui.component.TRADING_CARD_WIDTH_OVER_HEIGHT
import fr.aumombelli.dstcg.ui.motion.HolographicCardMotion
import kotlin.random.Random

data class LibraryOnboardingVariantWalkthroughPage(
    val title: String,
    val message: String,
    val visual: LibraryOnboardingVariantWalkthroughVisual,
)

sealed interface LibraryOnboardingVariantWalkthroughVisual {
    data class RarityGrid(
        val cards: List<DisplayCard>,
    ) : LibraryOnboardingVariantWalkthroughVisual

    data class SkyQualityGrid(
        val cards: List<DisplayCard>,
    ) : LibraryOnboardingVariantWalkthroughVisual

    data class StampComparison(
        val standardCard: DisplayCard,
        val stampedCard: DisplayCard,
    ) : LibraryOnboardingVariantWalkthroughVisual

    data class HolographicComparison(
        val standardCard: DisplayCard,
        val holographicCard: DisplayCard,
    ) : LibraryOnboardingVariantWalkthroughVisual

    data object Placeholder : LibraryOnboardingVariantWalkthroughVisual
}

internal data class LibraryOnboardingWalkthroughCardExample(
    val definition: CardDefinition,
    val extensionName: String,
    val variantProfile: VariantProfile,
)

internal fun buildLibraryOnboardingVariantWalkthroughPages(
    extensions: List<ExtensionDefinition>,
    cards: List<CardDefinition>,
    variantProfiles: List<VariantProfile>,
    random: Random = Random.Default,
): List<LibraryOnboardingVariantWalkthroughPage> {
    return NewPlayerOnboardingContent.libraryVariantWalkthroughPages.map { page ->
        LibraryOnboardingVariantWalkthroughPage(
            title = page.title,
            message = page.message,
            visual = buildLibraryOnboardingVariantVisual(
                page = page,
                extensions = extensions,
                cards = cards,
                variantProfiles = variantProfiles,
                random = random,
            ),
        )
    }
}

internal fun randomLibraryOnboardingCardForRarity(
    rarityLabel: String,
    extensions: List<ExtensionDefinition>,
    cards: List<CardDefinition>,
    variantProfiles: List<VariantProfile>,
    random: Random = Random.Default,
    requiredSkyQualities: Set<String> = emptySet(),
    requiredFinishes: Set<String> = emptySet(),
): LibraryOnboardingWalkthroughCardExample? {
    // The walkthrough must showcase real catalog cards, so selection happens inside the requested
    // rarity bucket instead of synthesizing impossible rarity variants from a single definition.
    val extensionNamesById = extensions.associate { it.id to it.name }
    val variantProfilesById = variantProfiles.associateBy(VariantProfile::id)
    val candidates = cards.mapNotNull { definition ->
        if (definition.rarityLabel != rarityLabel) return@mapNotNull null

        val extensionName = extensionNamesById[definition.extensionId] ?: return@mapNotNull null
        val variantProfile = variantProfilesById[definition.variantProfileId] ?: return@mapNotNull null
        if (!requiredSkyQualities.all { code -> variantProfile.skyQualities.any { it.code == code } }) {
            return@mapNotNull null
        }
        if (!requiredFinishes.all { code -> variantProfile.finishes.any { it.code == code } }) {
            return@mapNotNull null
        }

        LibraryOnboardingWalkthroughCardExample(
            definition = definition,
            extensionName = extensionName,
            variantProfile = variantProfile,
        )
    }

    if (candidates.isEmpty()) return null
    return candidates.random(random)
}

private fun buildLibraryOnboardingVariantVisual(
    page: LibraryVariantWalkthroughPageContent,
    extensions: List<ExtensionDefinition>,
    cards: List<CardDefinition>,
    variantProfiles: List<VariantProfile>,
    random: Random,
): LibraryOnboardingVariantWalkthroughVisual {
    fun buildCard(
        example: LibraryOnboardingWalkthroughCardExample,
        skyQuality: String,
        finish: String,
    ): DisplayCard? {
        val skyQualityDefinition = example.variantProfile.skyQualities.firstOrNull { it.code == skyQuality } ?: return null
        val finishDefinition = example.variantProfile.finishes.firstOrNull { it.code == finish } ?: return null
        val activeVariant = example.variantProfile.toDisplayVariant(
            skyQuality = skyQualityDefinition.code,
            finish = finishDefinition.code,
        )
        return example.definition.toDisplayCard(
            extensionName = example.extensionName,
            activeVariant = activeVariant,
        )
    }

    fun pickRandomExample(
        vararg preferredRarities: String,
        requiredSkyQualities: Set<String> = emptySet(),
        requiredFinishes: Set<String> = emptySet(),
    ): LibraryOnboardingWalkthroughCardExample? = preferredRarities.firstNotNullOfOrNull { rarityLabel ->
        randomLibraryOnboardingCardForRarity(
            rarityLabel = rarityLabel,
            extensions = extensions,
            cards = cards,
            variantProfiles = variantProfiles,
            random = random,
            requiredSkyQualities = requiredSkyQualities,
            requiredFinishes = requiredFinishes,
        )
    }

    return when (page.visualKind) {
        LibraryVariantWalkthroughVisualKind.Rarity -> {
            val rarityCards = listOf("Common", "Uncommon", "Rare", "Epic")
                .mapNotNull { rarityLabel ->
                    randomLibraryOnboardingCardForRarity(
                        rarityLabel = rarityLabel,
                        extensions = extensions,
                        cards = cards,
                        variantProfiles = variantProfiles,
                        random = random,
                        requiredSkyQualities = setOf("city"),
                        requiredFinishes = setOf("standard"),
                    )?.let { example ->
                        buildCard(
                            example = example,
                            skyQuality = "city",
                            finish = "standard",
                        )
                    }
                }
            if (rarityCards.size == 4) {
                LibraryOnboardingVariantWalkthroughVisual.RarityGrid(rarityCards)
            } else {
                LibraryOnboardingVariantWalkthroughVisual.Placeholder
            }
        }

        LibraryVariantWalkthroughVisualKind.SkyQuality -> {
            val example = pickRandomExample(
                "Common",
                "Uncommon",
                "Rare",
                "Epic",
                requiredSkyQualities = setOf("city", "suburban", "rural", "mountain"),
                requiredFinishes = setOf("standard"),
            )
            val skyQualityCards = example?.let { selectedExample ->
                listOf("city", "suburban", "rural", "mountain")
                    .mapNotNull { skyQuality ->
                        buildCard(
                            example = selectedExample,
                            skyQuality = skyQuality,
                            finish = "standard",
                        )
                    }
            }.orEmpty()
            if (skyQualityCards.size == 4) {
                LibraryOnboardingVariantWalkthroughVisual.SkyQualityGrid(skyQualityCards)
            } else {
                LibraryOnboardingVariantWalkthroughVisual.Placeholder
            }
        }

        LibraryVariantWalkthroughVisualKind.Stamp -> {
            val example = pickRandomExample(
                "Uncommon",
                "Rare",
                "Common",
                "Epic",
                requiredSkyQualities = setOf("city"),
                requiredFinishes = setOf("standard", "stamped"),
            )
            val standardCard = example?.let { selectedExample ->
                buildCard(
                    example = selectedExample,
                    skyQuality = "city",
                    finish = "standard",
                )
            }
            val stampedCard = example?.let { selectedExample ->
                buildCard(
                    example = selectedExample,
                    skyQuality = "city",
                    finish = "stamped",
                )
            }
            if (standardCard != null && stampedCard != null) {
                LibraryOnboardingVariantWalkthroughVisual.StampComparison(
                    standardCard = standardCard,
                    stampedCard = stampedCard,
                )
            } else {
                LibraryOnboardingVariantWalkthroughVisual.Placeholder
            }
        }

        LibraryVariantWalkthroughVisualKind.Holographic -> {
            val example = pickRandomExample(
                "Rare",
                "Epic",
                "Uncommon",
                "Common",
                requiredSkyQualities = setOf("city", "holographic"),
                requiredFinishes = setOf("standard"),
            )
            val standardCard = example?.let { selectedExample ->
                buildCard(
                    example = selectedExample,
                    skyQuality = "city",
                    finish = "standard",
                )
            }
            val holographicCard = example?.let { selectedExample ->
                buildCard(
                    example = selectedExample,
                    skyQuality = "holographic",
                    finish = "standard",
                )
            }
            if (standardCard != null && holographicCard != null) {
                LibraryOnboardingVariantWalkthroughVisual.HolographicComparison(
                    standardCard = standardCard,
                    holographicCard = holographicCard,
                )
            } else {
                LibraryOnboardingVariantWalkthroughVisual.Placeholder
            }
        }
    }
}

internal data class LibraryOnboardingEqualCardMetrics(
    val horizontalGap: Dp,
    val verticalGap: Dp,
    val cardWidth: Dp,
    val cardHeight: Dp,
    val totalWidth: Dp,
    val totalHeight: Dp,
)

internal data class LibraryOnboardingWeightedComparisonMetrics(
    val horizontalGap: Dp,
    val leftCardWidth: Dp,
    val leftCardHeight: Dp,
    val rightCardWidth: Dp,
    val rightCardHeight: Dp,
    val totalWidth: Dp,
    val contentHeight: Dp,
)

internal fun calculateLibraryOnboardingEqualCardMetrics(
    availableWidth: Dp,
    availableHeight: Dp,
    columns: Int,
    rows: Int,
    horizontalGap: Dp = LibraryOnboardingCardGap,
    verticalGap: Dp = LibraryOnboardingCardGap,
    verticalReservedHeight: Dp = 0.dp,
): LibraryOnboardingEqualCardMetrics {
    val safeColumns = columns.coerceAtLeast(1)
    val safeRows = rows.coerceAtLeast(1)
    val totalHorizontalGap = horizontalGap * (safeColumns - 1).toFloat()
    val totalVerticalGap = verticalGap * (safeRows - 1).toFloat()
    val usableWidth = (availableWidth - totalHorizontalGap).coerceAtLeast(0.dp)
    val usableHeight = (availableHeight - verticalReservedHeight - totalVerticalGap).coerceAtLeast(0.dp)
    val widthLimitedCardWidth = usableWidth / safeColumns.toFloat()
    val heightLimitedCardWidth = (usableHeight / safeRows.toFloat()) * TRADING_CARD_WIDTH_OVER_HEIGHT
    val cardWidth = minOf(widthLimitedCardWidth, heightLimitedCardWidth).coerceAtLeast(0.dp)
    val cardHeight = if (cardWidth > 0.dp) {
        cardWidth / TRADING_CARD_WIDTH_OVER_HEIGHT
    } else {
        0.dp
    }

    return LibraryOnboardingEqualCardMetrics(
        horizontalGap = horizontalGap,
        verticalGap = verticalGap,
        cardWidth = cardWidth,
        cardHeight = cardHeight,
        totalWidth = cardWidth * safeColumns.toFloat() + totalHorizontalGap,
        totalHeight = cardHeight * safeRows.toFloat() + totalVerticalGap + verticalReservedHeight,
    )
}

internal fun calculateLibraryOnboardingWeightedComparisonMetrics(
    availableWidth: Dp,
    availableHeight: Dp,
    leftWeight: Float,
    rightWeight: Float,
    horizontalGap: Dp = LibraryOnboardingCardGap,
    rightReservedHeight: Dp = LibraryOnboardingLabelHeight + LibraryOnboardingLabelGap,
): LibraryOnboardingWeightedComparisonMetrics {
    val safeLeftWeight = leftWeight.coerceAtLeast(0f)
    val safeRightWeight = rightWeight.coerceAtLeast(0f)
    val totalWeight = (safeLeftWeight + safeRightWeight).takeIf { it > 0f } ?: 1f
    val usableWidth = (availableWidth - horizontalGap).coerceAtLeast(0.dp)
    val leftWidthByWidth = usableWidth * (safeLeftWeight / totalWeight)
    val rightWidthByWidth = usableWidth * (safeRightWeight / totalWeight)
    val heightLimitedRightWidth =
        (availableHeight - rightReservedHeight).coerceAtLeast(0.dp) * TRADING_CARD_WIDTH_OVER_HEIGHT
    val heightScale = if (rightWidthByWidth.value > 0f) {
        minOf(1f, heightLimitedRightWidth.value / rightWidthByWidth.value)
    } else {
        0f
    }
    val leftCardWidth = leftWidthByWidth * heightScale
    val rightCardWidth = rightWidthByWidth * heightScale
    val leftCardHeight = if (leftCardWidth > 0.dp) {
        leftCardWidth / TRADING_CARD_WIDTH_OVER_HEIGHT
    } else {
        0.dp
    }
    val rightCardHeight = if (rightCardWidth > 0.dp) {
        rightCardWidth / TRADING_CARD_WIDTH_OVER_HEIGHT
    } else {
        0.dp
    }
    val effectiveGap = if (leftCardWidth > 0.dp && rightCardWidth > 0.dp) {
        horizontalGap
    } else {
        0.dp
    }

    return LibraryOnboardingWeightedComparisonMetrics(
        horizontalGap = effectiveGap,
        leftCardWidth = leftCardWidth,
        leftCardHeight = leftCardHeight,
        rightCardWidth = rightCardWidth,
        rightCardHeight = rightCardHeight,
        totalWidth = leftCardWidth + effectiveGap + rightCardWidth,
        contentHeight = maxOf(leftCardHeight, rightReservedHeight + rightCardHeight),
    )
}

private val LibraryOnboardingCardGap = 12.dp
private val LibraryOnboardingLabelGap = 8.dp
private val LibraryOnboardingLabelHeight = 24.dp
private const val HolographicStandardCardWeight = 0.85f
private const val HolographicFeaturedCardWeight = 1.15f

@Composable
internal fun LibraryOnboardingVariantWalkthroughVisual(
    page: LibraryOnboardingVariantWalkthroughPage,
    availableHeight: Dp,
    modifier: Modifier = Modifier,
) {
    when (val visual = page.visual) {
        is LibraryOnboardingVariantWalkthroughVisual.RarityGrid ->
            TwoByTwoCardGrid(
                cards = visual.cards,
                availableHeight = availableHeight,
                modifier = modifier,
                tagPrefix = "library-onboarding-rarity",
            )

        is LibraryOnboardingVariantWalkthroughVisual.SkyQualityGrid ->
            TwoByTwoCardGrid(
                cards = visual.cards,
                availableHeight = availableHeight,
                modifier = modifier,
                tagPrefix = "library-onboarding-sky-quality",
            )

        is LibraryOnboardingVariantWalkthroughVisual.StampComparison ->
            CardComparisonRow(
                modifier = modifier,
                availableHeight = availableHeight,
                leftLabel = visual.standardCard.activeVariant.finishLabel,
                rightLabel = visual.stampedCard.activeVariant.finishLabel,
                leftCard = visual.standardCard,
                rightCard = visual.stampedCard,
                rightTag = "library-onboarding-stamp-card",
                leftTag = "library-onboarding-standard-card",
            )

        is LibraryOnboardingVariantWalkthroughVisual.HolographicComparison ->
            HolographicShowcase(
                modifier = modifier,
                availableHeight = availableHeight,
                standardCard = visual.standardCard,
                holographicCard = visual.holographicCard,
            )

        LibraryOnboardingVariantWalkthroughVisual.Placeholder ->
            PlaceholderVisual(
                availableHeight = availableHeight,
                modifier = modifier,
            )
    }
}

@Composable
private fun TwoByTwoCardGrid(
    cards: List<DisplayCard>,
    tagPrefix: String,
    availableHeight: Dp,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(
        modifier = modifier.fillMaxWidth(),
    ) {
        val metrics = calculateLibraryOnboardingEqualCardMetrics(
            availableWidth = maxWidth,
            availableHeight = availableHeight,
            columns = 2,
            rows = 2,
        )
        Column(
            verticalArrangement = Arrangement.spacedBy(metrics.verticalGap),
            modifier = Modifier
                .width(metrics.totalWidth)
                .align(Alignment.Center),
        ) {
            cards.chunked(2).forEachIndexed { rowIndex, rowCards ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(metrics.horizontalGap),
                    modifier = Modifier.width(metrics.totalWidth),
                ) {
                    rowCards.forEachIndexed { columnIndex, card ->
                        AstroCardPreviewSurface(
                            displayCard = card,
                            mode = AstroCardSurfaceMode.Thumbnail,
                            modifier = Modifier
                                .width(metrics.cardWidth)
                                .testTag("$tagPrefix-${rowIndex * 2 + columnIndex}"),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CardComparisonRow(
    leftLabel: String,
    rightLabel: String,
    leftCard: DisplayCard,
    rightCard: DisplayCard,
    leftTag: String,
    rightTag: String,
    availableHeight: Dp,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(
        modifier = modifier.fillMaxWidth(),
    ) {
        val metrics = calculateLibraryOnboardingEqualCardMetrics(
            availableWidth = maxWidth,
            availableHeight = availableHeight,
            columns = 2,
            rows = 1,
            verticalReservedHeight = LibraryOnboardingLabelHeight + LibraryOnboardingLabelGap,
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(metrics.horizontalGap),
            modifier = Modifier
                .width(metrics.totalWidth)
                .align(Alignment.Center),
        ) {
            CardWithLabel(
                label = leftLabel,
                displayCard = leftCard,
                testTag = leftTag,
                cardWidth = metrics.cardWidth,
            )
            CardWithLabel(
                label = rightLabel,
                displayCard = rightCard,
                testTag = rightTag,
                cardWidth = metrics.cardWidth,
            )
        }
    }
}

@Composable
private fun CardWithLabel(
    label: String,
    displayCard: DisplayCard,
    testTag: String,
    cardWidth: Dp,
    mode: AstroCardSurfaceMode = AstroCardSurfaceMode.Preview,
    holographicMotion: HolographicCardMotion? = null,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(LibraryOnboardingLabelGap),
        modifier = Modifier.width(cardWidth),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = Color(0xFFF5D58F),
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .width(cardWidth)
                .height(LibraryOnboardingLabelHeight),
        )
        AstroCardPreviewSurface(
            displayCard = displayCard,
            mode = mode,
            holographicMotion = holographicMotion,
            modifier = Modifier
                .width(cardWidth)
                .testTag(testTag),
        )
    }
}

@Composable
private fun HolographicShowcase(
    standardCard: DisplayCard,
    holographicCard: DisplayCard,
    availableHeight: Dp,
    modifier: Modifier = Modifier,
) {
    val holoTransition = rememberInfiniteTransition(label = "library-onboarding-holographic")
    val holoSweep by holoTransition.animateFloat(
        initialValue = 0.18f,
        targetValue = 0.82f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "library-onboarding-holographic-sweep",
    )
    val sparkleBoost by holoTransition.animateFloat(
        initialValue = 0.08f,
        targetValue = 0.42f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "library-onboarding-holographic-sparkle",
    )

    BoxWithConstraints(
        modifier = modifier.fillMaxWidth(),
    ) {
        val metrics = calculateLibraryOnboardingWeightedComparisonMetrics(
            availableWidth = maxWidth,
            availableHeight = availableHeight,
            leftWeight = HolographicStandardCardWeight,
            rightWeight = HolographicFeaturedCardWeight,
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(metrics.horizontalGap),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .width(metrics.totalWidth)
                .align(Alignment.Center),
        ) {
            AstroCardPreviewSurface(
                displayCard = standardCard,
                mode = AstroCardSurfaceMode.Thumbnail,
                modifier = Modifier
                    .width(metrics.leftCardWidth)
                    .testTag("library-onboarding-holographic-standard-card"),
            )
            CardWithLabel(
                label = holographicCard.activeVariant.skyQualityLabel,
                displayCard = holographicCard,
                testTag = "library-onboarding-holographic-card",
                cardWidth = metrics.rightCardWidth,
                mode = AstroCardSurfaceMode.Preview,
                holographicMotion = HolographicCardMotion(
                    sweepFraction = holoSweep,
                    highlightAlpha = 0.24f,
                    edgeGlowAlpha = 0.36f,
                    sparkleBoost = sparkleBoost,
                ),
            )
        }
    }
}

@Composable
private fun PlaceholderVisual(
    availableHeight: Dp,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(
        modifier = modifier.fillMaxWidth(),
    ) {
        val placeholderWidth = minOf(maxWidth, availableHeight * 1.55f)
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .width(placeholderWidth)
                .align(Alignment.Center)
                .aspectRatio(1.55f)
                .clip(RoundedCornerShape(26.dp))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color(0xFF142945), Color(0xFF0A172A)),
                    ),
                )
                .padding(20.dp)
                .testTag("library-onboarding-placeholder"),
        ) {
            Text(
                text = "Les exemples de variantes apparaîtront ici.",
                style = MaterialTheme.typography.bodyLarge,
                color = Color(0xFFD5E4F7),
                textAlign = TextAlign.Center,
            )
        }
    }
}
