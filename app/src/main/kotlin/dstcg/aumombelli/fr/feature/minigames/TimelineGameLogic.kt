package fr.aumombelli.dstcg.feature.minigames

import fr.aumombelli.dstcg.data.MiniGameDeterministicDrawPolicy
import fr.aumombelli.dstcg.data.stableMiniGameHash
import fr.aumombelli.dstcg.model.CardDefinition
import fr.aumombelli.dstcg.model.DeepSkyDetails
import fr.aumombelli.dstcg.model.DisplayCard
import fr.aumombelli.dstcg.model.ExtensionDefinition
import fr.aumombelli.dstcg.model.MiniGameDifficulty
import fr.aumombelli.dstcg.model.MiniGameOwnedVariantRef
import fr.aumombelli.dstcg.model.MiniGameResolvedCardRef
import fr.aumombelli.dstcg.model.MiniGameReward
import fr.aumombelli.dstcg.model.SolarSystemDetails
import fr.aumombelli.dstcg.model.StarDetails
import fr.aumombelli.dstcg.model.VariantProfile
import fr.aumombelli.dstcg.model.toDisplayCard
import fr.aumombelli.dstcg.model.toDisplayVariant

internal enum class TimelineCriterion(
    val id: String,
    val title: String,
    val instruction: String,
    val firstSlotLabel: String,
    val lastSlotLabel: String,
    val lowerValueFirst: Boolean = true,
) {
    StellarDistance(
        id = "stellar-distance",
        title = "Distance",
        instruction = "Compare les deux cartes selon leur distance.",
        firstSlotLabel = "La plus proche",
        lastSlotLabel = "La plus lointaine",
    ),
    DeepSkyRealSize(
        id = "deep-sky-real-size",
        title = "Taille réelle",
        instruction = "Compare les deux objets du ciel profond selon leur taille réelle.",
        firstSlotLabel = "Le plus petit",
        lastSlotLabel = "Le plus grand",
    ),
    SolarSystemDiameter(
        id = "solar-system-diameter",
        title = "Diamètre",
        instruction = "Compare les deux objets du Système solaire selon leur diamètre.",
        firstSlotLabel = "Le plus petit",
        lastSlotLabel = "Le plus grand",
    ),
    VisualSize(
        id = "visual-size",
        title = "Taille apparente",
        instruction = "Compare les deux cartes selon leur taille apparente dans le ciel.",
        firstSlotLabel = "La plus petite",
        lastSlotLabel = "La plus grande",
    ),
    Luminosity(
        id = "luminosity",
        title = "Luminosité",
        instruction = "Compare les deux cartes selon leur luminosité.",
        firstSlotLabel = "La moins lumineuse",
        lastSlotLabel = "La plus lumineuse",
        lowerValueFirst = false,
    ),
    SkyPosition(
        id = "sky-position",
        title = "Position dans le ciel",
        instruction = "Compare les deux cartes selon leur position du sud vers le nord.",
        firstSlotLabel = "Le plus au sud",
        lastSlotLabel = "Le plus au nord",
    ),
}

internal data class TimelineDifficultySpec(
    val difficulty: MiniGameDifficulty,
    val comparisonCount: Int,
) {
    val comparisonLabel: String = if (comparisonCount == 1) {
        "1 comparaison"
    } else {
        "$comparisonCount comparaisons"
    }

    companion object {
        fun forDifficulty(difficulty: MiniGameDifficulty): TimelineDifficultySpec =
            TimelineDifficultySpec(
                difficulty = difficulty,
                comparisonCount = difficulty.level,
            )
    }
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

internal data class TimelineComparison(
    val index: Int,
    val cards: List<TimelineCard>,
    val correctSlots: List<TimelineCard>,
)

internal data class TimelineGame(
    val difficulty: MiniGameDifficulty,
    val criterion: TimelineCriterion,
    val comparisons: List<TimelineComparison>,
)

internal sealed interface TimelineGameBuildResult {
    data class Ready(
        val game: TimelineGame,
    ) : TimelineGameBuildResult

    data class Unavailable(
        val message: String,
    ) : TimelineGameBuildResult
}

internal data class TimelineComparisonEvaluation(
    val comparisonIndex: Int,
    val placedCards: List<TimelineCard>,
    val correctCards: List<TimelineCard>,
    val isCorrect: Boolean,
)

internal fun selectTimelineCriterion(dateUtc: String): TimelineCriterion =
    sortedTimelineCriteria(dateUtc).first()

internal fun selectPlayableTimelineCriterion(
    dateUtc: String,
    cards: List<CardDefinition>,
    ownedCardIds: Set<String>,
): TimelineCriterion? =
    sortedTimelineCriteria(dateUtc).firstOrNull { criterion ->
        cards.asSequence()
            .filter { it.id in ownedCardIds }
            .mapNotNull { criterion.valueFor(it)?.sortValue }
            .distinct()
            .take(TimelineMinimumCardCount)
            .count() >= TimelineMinimumCardCount
    }

private fun sortedTimelineCriteria(dateUtc: String): List<TimelineCriterion> {
    val primaryCriteria = TimelineCriterion.entries.filterNot { it == TimelineCriterion.SkyPosition }
    return primaryCriteria.sortedWith(
        compareBy<TimelineCriterion> {
            stableMiniGameHash(
                "timeline-criterion",
                "v${MiniGameDeterministicDrawPolicy.CurrentAlgorithmVersion}",
                dateUtc,
                it.id,
            )
        }.thenBy { it.id },
    ) + TimelineCriterion.SkyPosition
}

internal fun eligibleTimelineCardIds(
    criterion: TimelineCriterion,
    cards: List<CardDefinition>,
): Set<String> = cards
    .filter { criterion.valueFor(it) != null }
    .mapTo(mutableSetOf(), CardDefinition::id)

internal fun timelineResolvedCardCountForDifficulty(difficulty: MiniGameDifficulty): Int =
    (difficulty.level + 1)
        .coerceAtLeast(TimelineMinimumCardCount)
        .coerceAtMost(TimelinePreferredCardCount)

internal fun buildTimelineGame(
    difficulty: MiniGameDifficulty,
    criterion: TimelineCriterion,
    dateUtc: String,
    resolvedCards: List<MiniGameResolvedCardRef>,
    cards: List<CardDefinition>,
    extensions: List<ExtensionDefinition>,
    variantProfiles: List<VariantProfile>,
): TimelineGameBuildResult {
    val spec = TimelineDifficultySpec.forDifficulty(difficulty)
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
        .sortedWith(
            compareBy<TimelineCardEntry> {
                stableMiniGameHash(
                    "timeline-pool",
                    "v${MiniGameDeterministicDrawPolicy.CurrentAlgorithmVersion}",
                    dateUtc,
                    criterion.id,
                    difficulty.name,
                    it.card.id,
                )
            }.thenBy { it.card.id },
        )

    if (entries.size < TimelineMinimumCardCount) {
        return TimelineGameBuildResult.Unavailable(
            message = "Pas assez de cartes compatibles dans ta bibliothèque pour préparer Comparaison.",
        )
    }

    val comparisons = buildTimelineComparisons(
        difficulty = difficulty,
        criterion = criterion,
        dateUtc = dateUtc,
        entries = entries,
    )

    if (comparisons.size < spec.comparisonCount) {
        return TimelineGameBuildResult.Unavailable(
            message = "Pas assez de comparaisons possibles dans ta bibliothèque pour cette difficulté.",
        )
    }

    return TimelineGameBuildResult.Ready(
        game = TimelineGame(
            difficulty = difficulty,
            criterion = criterion,
            comparisons = comparisons.take(spec.comparisonCount).mapIndexed { index, comparison ->
                comparison.copy(index = index)
            },
        ),
    )
}

private fun buildTimelineComparisons(
    difficulty: MiniGameDifficulty,
    criterion: TimelineCriterion,
    dateUtc: String,
    entries: List<TimelineCardEntry>,
): List<TimelineComparison> =
    entries
        .flatMapIndexed { leftIndex, left ->
            entries.drop(leftIndex + 1).mapNotNull { right ->
                if (left.value.sortValue == right.value.sortValue) {
                    null
                } else {
                    val ordered = criterion.orderComparisonEntries(listOf(left, right))
                    val displayCards = ordered
                        .map(TimelineCardEntry::card)
                        .sortedWith(
                            compareBy<TimelineCard> {
                                stableMiniGameHash(
                                    "timeline-comparison-hand",
                                    "v${MiniGameDeterministicDrawPolicy.CurrentAlgorithmVersion}",
                                    dateUtc,
                                    criterion.id,
                                    difficulty.name,
                                    left.card.id,
                                    right.card.id,
                                    it.id,
                                )
                            }.thenBy { it.id },
                        )
                    TimelineComparisonCandidate(
                        cardIds = listOf(left.card.id, right.card.id).sorted(),
                        comparison = TimelineComparison(
                            index = 0,
                            cards = displayCards,
                            correctSlots = ordered.map(TimelineCardEntry::card),
                        ),
                    )
                }
            }
        }
        .sortedWith(
            compareBy<TimelineComparisonCandidate> {
                stableMiniGameHash(
                    "timeline-comparison",
                    "v${MiniGameDeterministicDrawPolicy.CurrentAlgorithmVersion}",
                    dateUtc,
                    criterion.id,
                    difficulty.name,
                    it.cardIds.joinToString(":"),
                )
            }.thenBy { it.cardIds.joinToString(":") },
        )
        .map(TimelineComparisonCandidate::comparison)

private fun TimelineCriterion.orderComparisonEntries(
    entries: List<TimelineCardEntry>,
): List<TimelineCardEntry> = if (lowerValueFirst) {
    entries.sortedBy { it.value.sortValue }
} else {
    entries.sortedByDescending { it.value.sortValue }
}

internal fun evaluateTimelineComparison(
    game: TimelineGame,
    comparisonIndex: Int,
    placedCardIds: List<String?>,
): TimelineComparisonEvaluation? {
    val comparison = game.comparisons.getOrNull(comparisonIndex) ?: return null
    val cardsById = comparison.cards.associateBy(TimelineCard::id)
    val placedCards = comparison.correctSlots.indices.map { slotIndex ->
        val placedCardId = placedCardIds.getOrNull(slotIndex) ?: return null
        cardsById[placedCardId] ?: return null
    }
    return TimelineComparisonEvaluation(
        comparisonIndex = comparison.index,
        placedCards = placedCards,
        correctCards = comparison.correctSlots,
        isCorrect = placedCards.map(TimelineCard::id) == comparison.correctSlots.map(TimelineCard::id),
    )
}

internal fun calculateTimelineReward(
    difficulty: MiniGameDifficulty,
    correctCount: Int,
    comparisonCount: Int,
): MiniGameReward {
    val safeComparisonCount = comparisonCount.coerceAtLeast(1)
    val clampedCorrectCount = correctCount.coerceIn(0, safeComparisonCount)
    val maxRewardSeconds = difficulty.reward.reductionSeconds.coerceAtLeast(0L)
    val baseRewardSeconds = maxRewardSeconds / 2L
    val scoreBonusSeconds = (baseRewardSeconds * clampedCorrectCount) / safeComparisonCount
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

    TimelineCriterion.SkyPosition -> TimelineCriterionValue(
        sortValue = card.astronomy.coordinates.declination.toSignedDegrees(),
        label = card.astronomy.coordinates.declination.label,
    )
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

private fun fr.aumombelli.dstcg.model.Declination.toSignedDegrees(): Double {
    val absoluteDegrees = degrees + (arcMinutes / 60.0) + (arcSeconds / 3600.0)
    return if (sign == "-") -absoluteDegrees else absoluteDegrees
}

private data class TimelineCardEntry(
    val card: TimelineCard,
    val value: TimelineCriterionValue,
)

private data class TimelineComparisonCandidate(
    val cardIds: List<String>,
    val comparison: TimelineComparison,
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
