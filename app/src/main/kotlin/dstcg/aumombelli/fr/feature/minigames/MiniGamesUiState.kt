package fr.aumombelli.dstcg.feature.minigames

import fr.aumombelli.dstcg.model.DisplayCard

internal data class MiniGamesUiState(
    val isLoading: Boolean = true,
    val todayUtc: String = "",
    val quizStatusLabel: String = "Chargement",
    val quizPlayedToday: Boolean = false,
    val quizRewardLabel: String? = null,
    val quizDifficultyChoices: List<QuizDifficultyChoiceUi> = emptyList(),
    val memoryStatusLabel: String = "Chargement",
    val memoryPlayedToday: Boolean = false,
    val memoryRewardLabel: String? = null,
    val memoryDifficultyChoices: List<MemoryDifficultyChoiceUi> = emptyList(),
    val timelineStatusLabel: String = "Chargement",
    val timelinePlayedToday: Boolean = false,
    val timelineRewardLabel: String? = null,
    val screen: MiniGamesScreenUiState = MiniGamesScreenUiState.Menu,
    val errorMessage: String? = null,
)

internal sealed interface MiniGamesScreenUiState {
    data object Menu : MiniGamesScreenUiState

    data class QuizDifficultySelection(
        val card: DisplayCard,
    ) : MiniGamesScreenUiState

    data class QuizPlaying(
        val card: DisplayCard,
        val difficultyName: String,
        val rewardLabel: String,
        val questionIndex: Int,
        val questionCount: Int,
        val score: Int,
        val prompt: String,
        val answers: List<QuizAnswerUi>,
        val canAdvance: Boolean,
        val feedbackEvent: MiniGameFeedbackEvent?,
    ) : MiniGamesScreenUiState

    data class QuizResult(
        val card: DisplayCard?,
        val difficultyName: String,
        val scoreLabel: String,
        val rewardLabel: String,
        val corrections: List<QuizCorrectionUi> = emptyList(),
        val nextDifficultyName: String?,
        val feedbackEvent: MiniGameFeedbackEvent? = null,
    ) : MiniGamesScreenUiState

    data class QuizUnavailable(
        val message: String,
    ) : MiniGamesScreenUiState

    data object MemoryDifficultySelection : MiniGamesScreenUiState

    data class MemoryPlaying(
        val difficultyName: String,
        val gridLabel: String,
        val rewardLabel: String,
        val columns: Int,
        val cells: List<MemoryCellUi>,
        val matchedCount: Int,
        val totalCount: Int,
        val moves: Int,
        val currentStreak: Int,
        val bestStreak: Int,
        val feedbackEvent: MiniGameFeedbackEvent?,
        val inputLocked: Boolean,
    ) : MiniGamesScreenUiState

    data class MemoryResult(
        val difficultyName: String,
        val rewardLabel: String,
        val nextDifficultyName: String?,
        val feedbackEvent: MiniGameFeedbackEvent? = null,
    ) : MiniGamesScreenUiState

    data class MemoryUnavailable(
        val message: String,
    ) : MiniGamesScreenUiState

    data class TimelinePlaying(
        val criterionTitle: String,
        val instruction: String,
        val rewardLabel: String,
        val slots: List<TimelineSlotUi>,
        val handCards: List<TimelineCardUi>,
        val handSlots: List<TimelineCardUi?> = handCards,
        val canValidate: Boolean,
        val feedbackEvent: MiniGameFeedbackEvent?,
    ) : MiniGamesScreenUiState

    data class TimelineResult(
        val criterionTitle: String,
        val scoreLabel: String,
        val rewardLabel: String,
        val slotResults: List<TimelineSlotResultUi>,
        val correctOrder: List<TimelineCardUi>,
        val showCorrectOrder: Boolean,
        val feedbackEvent: MiniGameFeedbackEvent? = null,
    ) : MiniGamesScreenUiState

    data class TimelineUnavailable(
        val message: String,
    ) : MiniGamesScreenUiState
}
