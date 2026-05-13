package fr.aumombelli.dstcg.feature.minigames

import fr.aumombelli.dstcg.data.MiniGameDeterministicDrawPolicy
import fr.aumombelli.dstcg.data.stableMiniGameHash
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
)

internal enum class QuizQuestionKind(
    val prompt: String,
) {
    ObjectType(prompt = "Quel est le type d'objet indiqué ?"),
    Constellation(prompt = "Dans quelle constellation se trouve cette carte ?"),
    MainSeason(prompt = "Quelle est sa saison principale d'observation ?"),
    PrimaryCatalog(prompt = "Quel est son catalogue principal ?"),
    CatalogNumber(prompt = "Quelle est sa désignation dans ce catalogue ?"),
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
    val questionCandidates = QuizQuestionKind.entries
        .mapNotNull { kind ->
            buildQuizQuestion(
                kind = kind,
                dateUtc = dateUtc,
                difficulty = difficulty,
                target = targetDefinition,
                cards = cards,
            )
        }
        .sortedWith(
            compareBy<QuizQuestion> {
                stableMiniGameHash(
                    "quiz-question",
                    "v${MiniGameDeterministicDrawPolicy.CurrentAlgorithmVersion}",
                    dateUtc,
                    difficulty.name,
                    targetDefinition.id,
                    it.kind.name,
                )
            }.thenBy { it.kind.name },
        )

    if (questionCandidates.size < spec.questionCount) {
        return QuizGameBuildResult.Unavailable(
            message = "Pas assez de réponses distinctes dans le catalogue pour préparer ce Quiz.",
        )
    }

    return QuizGameBuildResult.Ready(
        game = QuizGame(
            difficulty = difficulty,
            targetCard = targetCard,
            questions = questionCandidates.take(spec.questionCount),
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

private fun buildQuizQuestion(
    kind: QuizQuestionKind,
    dateUtc: String,
    difficulty: MiniGameDifficulty,
    target: CardDefinition,
    cards: List<CardDefinition>,
): QuizQuestion? {
    val correctAnswer = target.quizValue(kind) ?: return null
    val distractors = cards
        .asSequence()
        .filter { it.id != target.id }
        .mapNotNull { it.quizValue(kind) }
        .filter { it != correctAnswer }
        .distinct()
        .sortedWith(
            compareBy<String> {
                stableMiniGameHash(
                    "quiz-distractor",
                    "v${MiniGameDeterministicDrawPolicy.CurrentAlgorithmVersion}",
                    dateUtc,
                    difficulty.name,
                    target.id,
                    kind.name,
                    correctAnswer,
                    it,
                )
            }.thenBy { it },
        )
        .take(QuizDistractorCount)
        .toList()
    if (distractors.size < QuizDistractorCount) {
        return null
    }

    val answers = (distractors + correctAnswer)
        .sortedWith(
            compareBy<String> {
                stableMiniGameHash(
                    "quiz-answer",
                    "v${MiniGameDeterministicDrawPolicy.CurrentAlgorithmVersion}",
                    dateUtc,
                    difficulty.name,
                    target.id,
                    kind.name,
                    it,
                )
            }.thenBy { it },
        )

    return QuizQuestion(
        id = "${target.id}-${kind.name}",
        kind = kind,
        prompt = kind.prompt,
        correctAnswer = correctAnswer,
        answers = answers,
    )
}

private fun CardDefinition.quizValue(kind: QuizQuestionKind): String? {
    val value = when (kind) {
        QuizQuestionKind.ObjectType -> astronomy.objectTypeLabel
        QuizQuestionKind.Constellation -> astronomy.constellation
        QuizQuestionKind.MainSeason -> astronomy.mainSeason
        QuizQuestionKind.PrimaryCatalog -> astronomy.primaryCatalogName
        QuizQuestionKind.CatalogNumber -> astronomy.catalogNumber
    }.trim()
    return value.takeIf { it.isNotEmpty() }
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

private const val QuizDistractorCount = 3
