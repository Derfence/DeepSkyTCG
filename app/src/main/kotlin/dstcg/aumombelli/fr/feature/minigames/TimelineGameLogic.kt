package fr.aumombelli.dstcg.feature.minigames

import fr.aumombelli.dstcg.data.MiniGameDeterministicDrawPolicy
import fr.aumombelli.dstcg.data.stableMiniGameHash
import fr.aumombelli.dstcg.model.CardDefinition
import fr.aumombelli.dstcg.model.DeepSkyDetails
import fr.aumombelli.dstcg.model.DisplayCard
import fr.aumombelli.dstcg.model.ExtensionDefinition
import fr.aumombelli.dstcg.model.MiniGameResolvedCardRef
import fr.aumombelli.dstcg.model.MiniGameReward
import fr.aumombelli.dstcg.model.MiniGameOwnedVariantRef
import fr.aumombelli.dstcg.model.SolarSystemDetails
import fr.aumombelli.dstcg.model.StarDetails
import fr.aumombelli.dstcg.model.VariantProfile
import fr.aumombelli.dstcg.model.toDisplayCard
import fr.aumombelli.dstcg.model.toDisplayVariant

internal enum class TimelineCriterion(
    val id: String,
    val title: String,
    val instruction: String,
) {
    StellarDistance(
        id = "stellar-distance",
        title = "Distance",
        instruction = "Classe les cartes de la plus proche à la plus lointaine.",
    ),
    DeepSkyRealSize(
        id = "deep-sky-real-size",
        title = "Taille réelle",
        instruction = "Classe les objets du ciel profond du plus petit au plus grand.",
    ),
    SolarSystemDiameter(
        id = "solar-system-diameter",
        title = "Diamètre",
        instruction = "Classe les objets du Système solaire du plus petit au plus grand.",
    ),
    VisualSize(
        id = "visual-size",
        title = "Taille apparente",
        instruction = "Classe les cartes de la plus petite à la plus grande dans le ciel.",
    ),
    Luminosity(
        id = "luminosity",
        title = "Luminosité",
        instruction = "Classe les cartes de la plus lumineuse à la moins lumineuse.",
    ),
}

internal data class TimelineCriterionValue(
    val sortValue: Double,
    val label: String,
)

internal data class TimelineCard(
    val id: String,
    val displayCard: DisplayCard,
    val valueLabel: String,
)

internal data class TimelineGame(
    val criterion: TimelineCriterion,
    val cards: List<TimelineCard>,
    val correctOrder: List<TimelineCard>,
)

internal sealed interface TimelineGameBuildResult {
    data class Ready(
        val game: TimelineGame,
    ) : TimelineGameBuildResult

    data class Unavailable(
        val message: String,
    ) : TimelineGameBuildResult
}

internal data class TimelineEvaluation(
    val correctCount: Int,
    val totalCount: Int,
    val slotResults: List<TimelineSlotResult>,
) {
    val isPerfect: Boolean = correctCount == totalCount
}

internal data class TimelineSlotResult(
    val slotIndex: Int,
    val placedCard: TimelineCard,
    val correctCard: TimelineCard,
    val isCorrect: Boolean,
)

internal fun selectTimelineCriterion(dateUtc: String): TimelineCriterion =
    TimelineCriterion.entries.minWith(
        compareBy<TimelineCriterion> {
            stableMiniGameHash(
                "timeline-criterion",
                "v${MiniGameDeterministicDrawPolicy.CurrentAlgorithmVersion}",
                dateUtc,
                it.id,
            )
        }.thenBy { it.id },
    )

internal fun eligibleTimelineCardIds(
    criterion: TimelineCriterion,
    cards: List<CardDefinition>,
): Set<String> = cards
    .filter { criterion.valueFor(it) != null }
    .mapTo(mutableSetOf(), CardDefinition::id)

internal fun buildTimelineGame(
    criterion: TimelineCriterion,
    dateUtc: String,
    resolvedCards: List<MiniGameResolvedCardRef>,
    cards: List<CardDefinition>,
    extensions: List<ExtensionDefinition>,
    variantProfiles: List<VariantProfile>,
): TimelineGameBuildResult {
    val catalog = TimelineCatalogLookup(
        cards = cards,
        extensions = extensions,
        variantProfiles = variantProfiles,
    )
    val entries = resolvedCards
        .mapNotNull { resolvedCard ->
            val entry = catalog.entryFor(resolvedCard.ownedVariant) ?: return@mapNotNull null
            val value = criterion.valueFor(entry.definition) ?: return@mapNotNull null
            TimelineCardEntry(
                card = TimelineCard(
                    id = entry.definition.id,
                    displayCard = entry.displayCard,
                    valueLabel = value.label,
                ),
                value = value,
            )
        }
        .distinctBy { it.card.id }

    if (entries.size < TimelineMinimumCardCount) {
        return TimelineGameBuildResult.Unavailable(
            message = "Pas assez de cartes compatibles dans ta bibliothèque pour préparer la Timeline.",
        )
    }

    val selectedEntries = entries.take(TimelinePreferredCardCount)
    val correctOrderEntries = selectedEntries.sortedWith(
        compareBy<TimelineCardEntry> { it.value.sortValue }
            .thenBy {
                stableMiniGameHash(
                    "timeline-order-tie",
                    "v${MiniGameDeterministicDrawPolicy.CurrentAlgorithmVersion}",
                    dateUtc,
                    criterion.id,
                    it.card.id,
                )
            }
            .thenBy { it.card.id },
    )
    val handCards = selectedEntries
        .map(TimelineCardEntry::card)
        .sortedWith(
            compareBy<TimelineCard> {
                stableMiniGameHash(
                    "timeline-hand",
                    "v${MiniGameDeterministicDrawPolicy.CurrentAlgorithmVersion}",
                    dateUtc,
                    criterion.id,
                    it.id,
                )
            }.thenBy { it.id },
        )

    return TimelineGameBuildResult.Ready(
        game = TimelineGame(
            criterion = criterion,
            cards = handCards,
            correctOrder = correctOrderEntries.map(TimelineCardEntry::card),
        ),
    )
}

internal fun evaluateTimelinePlacement(
    game: TimelineGame,
    placedCardIds: List<String?>,
): TimelineEvaluation {
    val cardsById = game.cards.associateBy(TimelineCard::id)
    val slotResults = game.correctOrder.mapIndexedNotNull { slotIndex, correctCard ->
        val placedCardId = placedCardIds.getOrNull(slotIndex) ?: return@mapIndexedNotNull null
        val placedCard = cardsById[placedCardId] ?: return@mapIndexedNotNull null
        TimelineSlotResult(
            slotIndex = slotIndex,
            placedCard = placedCard,
            correctCard = correctCard,
            isCorrect = placedCard.id == correctCard.id,
        )
    }
    return TimelineEvaluation(
        correctCount = slotResults.count(TimelineSlotResult::isCorrect),
        totalCount = game.correctOrder.size,
        slotResults = slotResults,
    )
}

internal fun calculateTimelineReward(
    correctCount: Int,
    totalCount: Int,
): MiniGameReward {
    val safeTotalCount = totalCount.coerceAtLeast(1)
    val clampedCorrectCount = correctCount.coerceIn(0, safeTotalCount)
    val baseRewardSeconds = TimelineMaxRewardSeconds / 2L
    val scoreBonusSeconds = (baseRewardSeconds * clampedCorrectCount) / safeTotalCount
    return MiniGameReward.fromSeconds(baseRewardSeconds + scoreBonusSeconds)
}

private fun TimelineCriterion.valueFor(card: CardDefinition): TimelineCriterionValue? = when (this) {
    TimelineCriterion.StellarDistance -> when (val details = card.astronomy.details) {
        is StarDetails -> details.distance.toTimelineValue()
        is DeepSkyDetails -> details.distance.toTimelineValue()
        else -> null
    }

    TimelineCriterion.DeepSkyRealSize -> (card.astronomy.details as? DeepSkyDetails)
        ?.realSize
        ?.toTimelineValue()

    TimelineCriterion.SolarSystemDiameter -> (card.astronomy.details as? SolarSystemDetails)
        ?.realSize
        ?.toTimelineValue()

    TimelineCriterion.VisualSize -> card.astronomy.details.timelineVisualSize()
        ?.let { visualSize ->
            TimelineCriterionValue(
                sortValue = visualSize.fullMoonWidth * visualSize.fullMoonHeight,
                label = visualSize.label,
            )
        }

    TimelineCriterion.Luminosity -> when (val details = card.astronomy.details) {
        is StarDetails -> details.absoluteMagnitude.toTimelineValue()
        is DeepSkyDetails -> details.absoluteMagnitude?.toTimelineValue()
        else -> null
    }
}

private fun fr.aumombelli.dstcg.model.AstronomyDetails.timelineVisualSize():
    fr.aumombelli.dstcg.model.VisualSize? = when (this) {
    is DeepSkyDetails -> visualSize
    is fr.aumombelli.dstcg.model.ConstellationDetails -> visualSize
    is fr.aumombelli.dstcg.model.SkyEventDetails -> visualSize
    is StarDetails -> visualSize
    else -> null
}

private fun fr.aumombelli.dstcg.model.LightYearMeasurement.toTimelineValue(): TimelineCriterionValue =
    TimelineCriterionValue(
        sortValue = lightYears,
        label = label,
    )

private fun fr.aumombelli.dstcg.model.AbsoluteMagnitudeMeasurement.toTimelineValue(): TimelineCriterionValue =
    TimelineCriterionValue(
        sortValue = value,
        label = label,
    )

private data class TimelineCardEntry(
    val card: TimelineCard,
    val value: TimelineCriterionValue,
)

private data class TimelineCatalogLookup(
    val cards: List<CardDefinition>,
    val extensions: List<ExtensionDefinition>,
    val variantProfiles: List<VariantProfile>,
) {
    private val cardsById = cards.associateBy(CardDefinition::id)
    private val extensionNamesById = extensions.associate { it.id to it.name }
    private val variantProfilesById = variantProfiles.associateBy(VariantProfile::id)

    fun entryFor(variant: MiniGameOwnedVariantRef): TimelineDisplayCardEntry? {
        val definition = cardsById[variant.cardId] ?: return null
        val profile = variantProfilesById[definition.variantProfileId] ?: return null
        val activeVariant = runCatching {
            profile.toDisplayVariant(
                skyQuality = variant.skyQuality,
                finish = variant.finish,
            )
        }.getOrNull() ?: return null
        return TimelineDisplayCardEntry(
            definition = definition,
            displayCard = definition.toDisplayCard(
                extensionName = extensionNamesById[definition.extensionId] ?: definition.extensionId,
                activeVariant = activeVariant,
            ),
        )
    }
}

private data class TimelineDisplayCardEntry(
    val definition: CardDefinition,
    val displayCard: DisplayCard,
)

internal const val TimelinePreferredCardCount: Int = 5
internal const val TimelineMinimumCardCount: Int = 2
private const val TimelineMaxRewardSeconds: Long = 60L * 60L
