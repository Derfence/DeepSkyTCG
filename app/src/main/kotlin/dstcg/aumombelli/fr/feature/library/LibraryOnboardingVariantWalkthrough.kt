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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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

@Composable
internal fun LibraryOnboardingVariantWalkthroughVisual(
    page: LibraryOnboardingVariantWalkthroughPage,
    modifier: Modifier = Modifier,
) {
    when (val visual = page.visual) {
        is LibraryOnboardingVariantWalkthroughVisual.RarityGrid ->
            TwoByTwoCardGrid(
                cards = visual.cards,
                modifier = modifier,
                tagPrefix = "library-onboarding-rarity",
            )

        is LibraryOnboardingVariantWalkthroughVisual.SkyQualityGrid ->
            TwoByTwoCardGrid(
                cards = visual.cards,
                modifier = modifier,
                tagPrefix = "library-onboarding-sky-quality",
            )

        is LibraryOnboardingVariantWalkthroughVisual.StampComparison ->
            CardComparisonRow(
                modifier = modifier,
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
                standardCard = visual.standardCard,
                holographicCard = visual.holographicCard,
            )

        LibraryOnboardingVariantWalkthroughVisual.Placeholder ->
            PlaceholderVisual(modifier = modifier)
    }
}

@Composable
private fun TwoByTwoCardGrid(
    cards: List<DisplayCard>,
    tagPrefix: String,
    modifier: Modifier = Modifier,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        cards.chunked(2).forEachIndexed { rowIndex, rowCards ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                rowCards.forEachIndexed { columnIndex, card ->
                    AstroCardPreviewSurface(
                        displayCard = card,
                        mode = AstroCardSurfaceMode.Thumbnail,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("$tagPrefix-${rowIndex * 2 + columnIndex}"),
                    )
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
    modifier: Modifier = Modifier,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            CardWithLabel(
                label = leftLabel,
                displayCard = leftCard,
                testTag = leftTag,
                modifier = Modifier.weight(1f),
            )
            CardWithLabel(
                label = rightLabel,
                displayCard = rightCard,
                testTag = rightTag,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun CardWithLabel(
    label: String,
    displayCard: DisplayCard,
    testTag: String,
    modifier: Modifier = Modifier,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = Color(0xFFF5D58F),
            textAlign = TextAlign.Center,
        )
        AstroCardPreviewSurface(
            displayCard = displayCard,
            mode = AstroCardSurfaceMode.Preview,
            modifier = Modifier
                .fillMaxWidth()
                .testTag(testTag),
        )
    }
}

@Composable
private fun HolographicShowcase(
    standardCard: DisplayCard,
    holographicCard: DisplayCard,
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

    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.fillMaxWidth(),
    ) {
        AstroCardPreviewSurface(
            displayCard = standardCard,
            mode = AstroCardSurfaceMode.Thumbnail,
            modifier = Modifier
                .weight(0.85f)
                .testTag("library-onboarding-holographic-standard-card"),
        )
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.weight(1.15f),
        ) {
            Text(
                text = holographicCard.activeVariant.skyQualityLabel,
                style = MaterialTheme.typography.labelLarge,
                color = Color(0xFFF5D58F),
                textAlign = TextAlign.Center,
            )
            AstroCardPreviewSurface(
                displayCard = holographicCard,
                mode = AstroCardSurfaceMode.Preview,
                holographicMotion = HolographicCardMotion(
                    sweepFraction = holoSweep,
                    highlightAlpha = 0.24f,
                    edgeGlowAlpha = 0.36f,
                    sparkleBoost = sparkleBoost,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("library-onboarding-holographic-card"),
            )
        }
    }
}

@Composable
private fun PlaceholderVisual(
    modifier: Modifier = Modifier,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .fillMaxWidth()
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
