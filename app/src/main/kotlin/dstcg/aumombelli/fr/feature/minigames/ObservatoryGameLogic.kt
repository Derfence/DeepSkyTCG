package fr.aumombelli.dstcg.feature.minigames

import fr.aumombelli.dstcg.data.MiniGameDeterministicDrawPolicy
import fr.aumombelli.dstcg.data.stableMiniGameHash
import fr.aumombelli.dstcg.model.CardDefinition
import fr.aumombelli.dstcg.model.DisplayCard
import fr.aumombelli.dstcg.model.ExtensionDefinition
import fr.aumombelli.dstcg.model.MiniGameDifficulty
import fr.aumombelli.dstcg.model.MiniGameOwnedVariantRef
import fr.aumombelli.dstcg.model.MiniGameResolvedCardRef
import fr.aumombelli.dstcg.model.VariantProfile
import fr.aumombelli.dstcg.model.toDisplayCard
import fr.aumombelli.dstcg.model.toDisplayVariant
import kotlin.math.abs

internal data class ObservatoryDifficultySpec(
    val difficulty: MiniGameDifficulty,
    val targetCount: Int,
    val tolerance: Float,
) {
    val targetLabel: String = if (targetCount == 1) {
        "1 cible"
    } else {
        "$targetCount cibles"
    }
    val precisionLabel: String = "±${(tolerance * 100f).toInt()}%"

    companion object {
        fun forDifficulty(difficulty: MiniGameDifficulty): ObservatoryDifficultySpec = when (difficulty) {
            MiniGameDifficulty.Apprentice -> ObservatoryDifficultySpec(difficulty, targetCount = 1, tolerance = 0.12f)
            MiniGameDifficulty.Observer -> ObservatoryDifficultySpec(difficulty, targetCount = 2, tolerance = 0.09f)
            MiniGameDifficulty.Scientist -> ObservatoryDifficultySpec(difficulty, targetCount = 3, tolerance = 0.06f)
            MiniGameDifficulty.Explorer -> ObservatoryDifficultySpec(difficulty, targetCount = 4, tolerance = 0.04f)
        }
    }
}

internal data class ObservatoryTarget(
    val id: String,
    val displayCard: DisplayCard,
    val azimuth: Float,
    val altitude: Float,
    val focus: Float,
    val hasCloudEvent: Boolean,
) {
    val azimuthLabel: String = azimuth.toPercentLabel()
    val altitudeLabel: String = altitude.toPercentLabel()
    val focusLabel: String = focus.toPercentLabel()
}

internal data class ObservatoryGame(
    val difficulty: MiniGameDifficulty,
    val dateUtc: String,
    val targets: List<ObservatoryTarget>,
    val tolerance: Float,
) {
    val targetCount: Int = targets.size
}

internal sealed interface ObservatoryGameBuildResult {
    data class Ready(
        val game: ObservatoryGame,
    ) : ObservatoryGameBuildResult

    data class Unavailable(
        val message: String,
    ) : ObservatoryGameBuildResult
}

internal fun buildObservatoryGame(
    difficulty: MiniGameDifficulty,
    dateUtc: String,
    resolvedCards: List<MiniGameResolvedCardRef>,
    cards: List<CardDefinition>,
    extensions: List<ExtensionDefinition>,
    variantProfiles: List<VariantProfile>,
): ObservatoryGameBuildResult {
    val spec = ObservatoryDifficultySpec.forDifficulty(difficulty)
    val catalog = ObservatoryCatalogLookup(
        cards = cards,
        extensions = extensions,
        variantProfiles = variantProfiles,
    )
    val entries = resolvedCards
        .mapNotNull { catalog.entryFor(it.ownedVariant) }
        .distinctBy { it.definition.id }

    if (entries.size < spec.targetCount) {
        return ObservatoryGameBuildResult.Unavailable(
            message = "Pas assez de cartes distinctes dans ta bibliothèque pour préparer l'Observatoire.",
        )
    }

    val selectedEntries = entries.take(spec.targetCount)
    val cloudTargetIndex = deterministicCloudTargetIndex(
        dateUtc = dateUtc,
        difficulty = difficulty,
        targetCount = spec.targetCount,
    )
    val targets = selectedEntries.mapIndexed { index, entry ->
        ObservatoryTarget(
            id = entry.definition.id,
            displayCard = entry.displayCard,
            azimuth = deterministicSetting(
                dateUtc = dateUtc,
                difficulty = difficulty,
                cardId = entry.definition.id,
                targetIndex = index,
                settingId = "azimuth",
            ),
            altitude = deterministicSetting(
                dateUtc = dateUtc,
                difficulty = difficulty,
                cardId = entry.definition.id,
                targetIndex = index,
                settingId = "altitude",
            ),
            focus = deterministicSetting(
                dateUtc = dateUtc,
                difficulty = difficulty,
                cardId = entry.definition.id,
                targetIndex = index,
                settingId = "focus",
            ),
            hasCloudEvent = index == cloudTargetIndex,
        )
    }

    return ObservatoryGameBuildResult.Ready(
        game = ObservatoryGame(
            difficulty = difficulty,
            dateUtc = dateUtc,
            targets = targets,
            tolerance = spec.tolerance,
        ),
    )
}

internal fun isObservatorySettingReady(
    value: Float,
    target: Float,
    tolerance: Float,
): Boolean = abs(value.coerceIn(0f, 1f) - target) <= tolerance

private fun deterministicCloudTargetIndex(
    dateUtc: String,
    difficulty: MiniGameDifficulty,
    targetCount: Int,
): Int {
    if (targetCount <= 1) return 0
    return (stableMiniGameHash(
        "observatory-cloud-target",
        "v${MiniGameDeterministicDrawPolicy.CurrentAlgorithmVersion}",
        dateUtc,
        difficulty.name,
        targetCount.toString(),
    ).take(8).toLong(radix = 16) % targetCount).toInt()
}

private fun deterministicSetting(
    dateUtc: String,
    difficulty: MiniGameDifficulty,
    cardId: String,
    targetIndex: Int,
    settingId: String,
): Float {
    val unit = stableUnitFloat(
        "observatory-setting",
        "v${MiniGameDeterministicDrawPolicy.CurrentAlgorithmVersion}",
        dateUtc,
        difficulty.name,
        targetIndex.toString(),
        cardId,
        settingId,
    )
    return 0.22f + unit * 0.56f
}

private fun stableUnitFloat(vararg parts: String): Float {
    val sample = stableMiniGameHash(*parts).take(8).toLong(radix = 16)
    return (sample.toDouble() / 0xFFFF_FFFFL.toDouble()).toFloat().coerceIn(0f, 1f)
}

private data class ObservatoryCatalogLookup(
    val cards: List<CardDefinition>,
    val extensions: List<ExtensionDefinition>,
    val variantProfiles: List<VariantProfile>,
) {
    private val cardsById = cards.associateBy(CardDefinition::id)
    private val extensionNamesById = extensions.associate { it.id to it.name }
    private val variantProfilesById = variantProfiles.associateBy(VariantProfile::id)

    fun entryFor(variant: MiniGameOwnedVariantRef): ObservatoryCardEntry? {
        val definition = cardsById[variant.cardId] ?: return null
        val profile = variantProfilesById[definition.variantProfileId] ?: return null
        val extensionName = extensionNamesById[definition.extensionId] ?: definition.extensionId
        return ObservatoryCardEntry(
            definition = definition,
            displayCard = definition.toDisplayCard(
                extensionName = extensionName,
                activeVariant = profile.toDisplayVariant(
                    skyQuality = variant.skyQuality,
                    finish = variant.finish,
                ),
            ),
        )
    }
}

private data class ObservatoryCardEntry(
    val definition: CardDefinition,
    val displayCard: DisplayCard,
)

private fun Float.toPercentLabel(): String =
    "${(this.coerceIn(0f, 1f) * 100f).toInt()}%"
