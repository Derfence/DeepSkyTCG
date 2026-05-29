package fr.aumombelli.dstcg.feature.minigames

import fr.aumombelli.dstcg.model.CardDefinition
import fr.aumombelli.dstcg.model.DisplayCard
import fr.aumombelli.dstcg.model.ExtensionDefinition
import fr.aumombelli.dstcg.model.MiniGameDifficulty
import fr.aumombelli.dstcg.model.MiniGameResolvedCardRef
import fr.aumombelli.dstcg.model.MiniGameReward
import fr.aumombelli.dstcg.model.VariantProfile
import fr.aumombelli.dstcg.model.toDisplayCard
import fr.aumombelli.dstcg.model.toDisplayVariant

internal data class QuizDifficultySpec(
    val difficulty: MiniGameDifficulty,
    val questionCount: Int,
) {
    val questionLabel: String = if (questionCount == 1) {
        "1 question"
    } else {
        "$questionCount questions"
    }

    companion object {
        fun forDifficulty(difficulty: MiniGameDifficulty): QuizDifficultySpec =
            QuizDifficultySpec(
                difficulty = difficulty,
                questionCount = difficulty.level,
            )
    }
}

internal data class QuizGame(
    val difficulty: MiniGameDifficulty,
    val targetCard: DisplayCard,
    val questions: List<QuizQuestion>,
)

internal data class QuizQuestion(
    val id: String,
    val kind: QuizQuestionKind,
    val prompt: String,
    val correctAnswer: String,
    val answers: List<String>,
    val explanation: String,
)

internal enum class QuizQuestionKind {
    ObjectType,
    ObjectFamily,
    Constellation,
    MainSeason,
    PrimaryCatalog,
    CatalogNumber,
    CelestialHemisphere,
    DistanceScale,
    VisualMoonScale,
    RealSizeScale,
    AbsoluteMagnitudeClass,
    ProfileCategory,
    SolarSystemDistanceContext,
}

internal sealed interface QuizGameBuildResult {
    data class Ready(
        val game: QuizGame,
    ) : QuizGameBuildResult

    data class Unavailable(
        val message: String,
    ) : QuizGameBuildResult
}

internal fun buildQuizGame(
    difficulty: MiniGameDifficulty,
    dateUtc: String,
    resolvedCards: List<MiniGameResolvedCardRef>,
    cards: List<CardDefinition>,
    extensions: List<ExtensionDefinition>,
    variantProfiles: List<VariantProfile>,
): QuizGameBuildResult {
    val resolvedCard = resolvedCards.firstOrNull()
        ?: return QuizGameBuildResult.Unavailable("Aucune carte du jour disponible pour le Quiz.")
    val cardsById = cards.associateBy(CardDefinition::id)
    val targetDefinition = cardsById[resolvedCard.ownedVariant.cardId]
        ?: return QuizGameBuildResult.Unavailable("La carte du jour n'est plus disponible dans le catalogue.")
    val targetCard = targetDefinition.toQuizDisplayCard(
        resolvedCard = resolvedCard,
        extensions = extensions,
        variantProfiles = variantProfiles,
    ) ?: return QuizGameBuildResult.Unavailable("La variante de la carte du jour est indisponible.")

    val spec = QuizDifficultySpec.forDifficulty(difficulty)
    val questions = buildQuizQuestions(
        difficulty = difficulty,
        dateUtc = dateUtc,
        target = targetDefinition,
        cards = cards,
        questionCount = spec.questionCount,
    )

    return QuizGameBuildResult.Ready(
        game = QuizGame(
            difficulty = difficulty,
            targetCard = targetCard,
            questions = questions,
        ),
    )
}

internal fun calculateQuizReward(
    difficulty: MiniGameDifficulty,
    correctCount: Int,
    questionCount: Int,
): MiniGameReward {
    val safeQuestionCount = questionCount.coerceAtLeast(1)
    val clampedCorrectCount = correctCount.coerceIn(0, safeQuestionCount)
    val maxRewardSeconds = difficulty.reward.reductionSeconds.coerceAtLeast(0L)
    val baseRewardSeconds = maxRewardSeconds / 2L
    val scoreBonusSeconds = (baseRewardSeconds * clampedCorrectCount) / safeQuestionCount
    return MiniGameReward.fromSeconds(baseRewardSeconds + scoreBonusSeconds)
}

private fun CardDefinition.toQuizDisplayCard(
    resolvedCard: MiniGameResolvedCardRef,
    extensions: List<ExtensionDefinition>,
    variantProfiles: List<VariantProfile>,
): DisplayCard? {
    val extensionName = extensions.firstOrNull { it.id == extensionId }?.name ?: extensionId
    val variantProfile = variantProfiles.firstOrNull { it.id == variantProfileId } ?: return null
    val activeVariant = runCatching {
        variantProfile.toDisplayVariant(
            skyQuality = resolvedCard.ownedVariant.skyQuality,
            finish = resolvedCard.ownedVariant.finish,
        )
    }.getOrNull() ?: return null
    return toDisplayCard(
        extensionName = extensionName,
        activeVariant = activeVariant,
    )
}
