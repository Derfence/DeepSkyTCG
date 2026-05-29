package fr.aumombelli.dstcg.feature.minigames

import fr.aumombelli.dstcg.model.MiniGameDifficulty

internal data class QuizDifficultyChoiceUi(
    val difficulty: MiniGameDifficulty,
    val title: String,
    val questionLabel: String,
    val rewardLabel: String,
    val enabled: Boolean,
    val locked: Boolean,
    val statusLabel: String,
) {
    val testTag: String = "quiz-difficulty-${difficulty.name.lowercase()}"
}

internal data class QuizAnswerUi(
    val index: Int,
    val text: String,
    val state: QuizAnswerState,
) {
    val testTag: String = "quiz-answer-$index"
}

internal enum class QuizAnswerState {
    Idle,
    SelectedCorrect,
    SelectedWrong,
    Correct,
}

internal data class QuizCorrectionUi(
    val prompt: String,
    val selectedAnswer: String,
    val correctAnswer: String,
    val explanation: String,
    val isCorrect: Boolean,
)
