package fr.aumombelli.dstcg.feature.minigames

import fr.aumombelli.dstcg.data.MiniGameDeterministicDrawPolicy
import fr.aumombelli.dstcg.data.stableMiniGameHash
import fr.aumombelli.dstcg.model.CardDefinition
import fr.aumombelli.dstcg.model.MiniGameDifficulty

internal fun buildQuizQuestions(
    difficulty: MiniGameDifficulty,
    dateUtc: String,
    target: CardDefinition,
    cards: List<CardDefinition>,
    questionCount: Int,
): List<QuizQuestion> {
    val facts = QuizFactSet.from(target)
    val catalogFacts = cards
        .asSequence()
        .filter { it.id != target.id }
        .sortedBy { it.id }
        .map(QuizFactSet::from)
        .toList()
    val context = QuizQuestionContext(
        difficulty = difficulty,
        dateUtc = dateUtc,
        target = target,
        facts = facts,
        catalogFacts = catalogFacts,
    )
    val candidates = QuizQuestionTemplates
        .filter { it.depth in difficulty.questionDepths() }
        .mapNotNull { template -> template.buildCandidate(context) }

    return selectQuestions(
        candidates = candidates,
        difficulty = difficulty,
        dateUtc = dateUtc,
        targetId = target.id,
        questionCount = questionCount,
    ).map { it.question }
}

internal data class QuizQuestionContext(
    val difficulty: MiniGameDifficulty,
    val dateUtc: String,
    val target: CardDefinition,
    val facts: QuizFactSet,
    val catalogFacts: List<QuizFactSet>,
)

internal data class QuizQuestionCandidate(
    val depth: QuizQuestionDepth,
    val question: QuizQuestion,
)

internal data class QuizQuestionTemplate(
    val id: String,
    val kind: QuizQuestionKind,
    val depth: QuizQuestionDepth,
    val prompt: (QuizFactSet) -> String,
    val answer: (QuizFactSet) -> String?,
    val explanation: (QuizFactSet, String) -> String,
    val controlledAnswers: (QuizFactSet) -> List<String>,
    val isEligible: (QuizFactSet) -> Boolean = { true },
    val catalogFilter: (QuizFactSet, QuizFactSet) -> Boolean = { _, _ -> true },
) {
    fun buildCandidate(context: QuizQuestionContext): QuizQuestionCandidate? {
        val facts = context.facts
        if (!isEligible(facts)) return null
        val correctAnswer = answer(facts).cleanAnswer() ?: return null
        val catalogAnswers = context.catalogFacts
            .asSequence()
            .filter { catalogFilter(facts, it) }
            .mapNotNull { answer(it).cleanAnswer() }
            .toList()
        val distractors = selectDistractors(
            context = context,
            templateId = id,
            correctAnswer = correctAnswer,
            answers = catalogAnswers + controlledAnswers(facts),
        )
        if (distractors.size < QuizDistractorCount) return null
        val answers = (distractors + correctAnswer)
            .distinctQuizAnswers()
            .sortedWith(context.answerComparator(templateId = id))
        if (answers.size != QuizAnswerCount) return null
        return QuizQuestionCandidate(
            depth = depth,
            question = QuizQuestion(
                id = "${context.target.id}-$id",
                kind = kind,
                prompt = prompt(facts),
                correctAnswer = correctAnswer,
                answers = answers,
                explanation = explanation(facts, correctAnswer),
            ),
        )
    }
}

internal enum class QuizQuestionDepth {
    Direct,
    SimpleDerived,
    Measurement,
    ProfileSpecific,
}

private fun selectQuestions(
    candidates: List<QuizQuestionCandidate>,
    difficulty: MiniGameDifficulty,
    dateUtc: String,
    targetId: String,
    questionCount: Int,
): List<QuizQuestionCandidate> {
    val selected = mutableListOf<QuizQuestionCandidate>()
    difficulty.depthPlan().forEach { depth ->
        candidates
            .filter { candidate ->
                candidate.depth == depth && selected.none { it.question.id == candidate.question.id }
            }
            .sortedWith(questionComparator(dateUtc, difficulty, targetId))
            .firstOrNull()
            ?.let(selected::add)
    }

    if (selected.size < questionCount) {
        candidates
            .filter { candidate -> selected.none { it.question.id == candidate.question.id } }
            .sortedWith(questionComparator(dateUtc, difficulty, targetId))
            .take(questionCount - selected.size)
            .forEach(selected::add)
    }
    return selected.take(questionCount)
}

private fun questionComparator(
    dateUtc: String,
    difficulty: MiniGameDifficulty,
    targetId: String,
): Comparator<QuizQuestionCandidate> = compareBy<QuizQuestionCandidate> {
    stableMiniGameHash(
        "quiz-question",
        "v${MiniGameDeterministicDrawPolicy.CurrentAlgorithmVersion}",
        dateUtc,
        difficulty.name,
        targetId,
        it.question.id,
    )
}.thenBy { it.question.id }

private fun selectDistractors(
    context: QuizQuestionContext,
    templateId: String,
    correctAnswer: String,
    answers: List<String>,
): List<String> = answers
    .asSequence()
    .mapNotNull(String::cleanAnswer)
    .filter { it.normalizedQuizAnswer() != correctAnswer.normalizedQuizAnswer() }
    .toList()
    .distinctQuizAnswers()
    .sortedWith(context.answerComparator(templateId = "$templateId-distractor"))
    .take(QuizDistractorCount)
    .toList()

private fun QuizQuestionContext.answerComparator(templateId: String): Comparator<String> = compareBy<String> {
    stableMiniGameHash(
        "quiz-answer",
        "v${MiniGameDeterministicDrawPolicy.CurrentAlgorithmVersion}",
        dateUtc,
        difficulty.name,
        target.id,
        templateId,
        it.normalizedQuizAnswer(),
    )
}.thenBy { it.normalizedQuizAnswer() }.thenBy { it }

private fun MiniGameDifficulty.questionDepths(): Set<QuizQuestionDepth> = when (this) {
    MiniGameDifficulty.Apprentice -> setOf(QuizQuestionDepth.Direct)
    MiniGameDifficulty.Observer -> setOf(QuizQuestionDepth.Direct, QuizQuestionDepth.SimpleDerived)
    MiniGameDifficulty.Scientist -> setOf(
        QuizQuestionDepth.Direct,
        QuizQuestionDepth.SimpleDerived,
        QuizQuestionDepth.Measurement,
    )
    MiniGameDifficulty.Explorer -> QuizQuestionDepth.entries.toSet()
}

private fun MiniGameDifficulty.depthPlan(): List<QuizQuestionDepth> = when (this) {
    MiniGameDifficulty.Apprentice -> listOf(QuizQuestionDepth.Direct)
    MiniGameDifficulty.Observer -> listOf(QuizQuestionDepth.Direct, QuizQuestionDepth.SimpleDerived)
    MiniGameDifficulty.Scientist -> listOf(
        QuizQuestionDepth.Direct,
        QuizQuestionDepth.SimpleDerived,
        QuizQuestionDepth.Measurement,
    )
    MiniGameDifficulty.Explorer -> listOf(
        QuizQuestionDepth.Direct,
        QuizQuestionDepth.SimpleDerived,
        QuizQuestionDepth.Measurement,
        QuizQuestionDepth.ProfileSpecific,
    )
}

private const val QuizDistractorCount = 3
private const val QuizAnswerCount = 4
