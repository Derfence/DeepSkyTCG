package fr.aumombelli.dstcg.feature.minigames

import fr.aumombelli.dstcg.data.CatalogGateway
import fr.aumombelli.dstcg.data.MiniGameAttemptConsumeResult
import fr.aumombelli.dstcg.data.MiniGameRewardGrantResult
import fr.aumombelli.dstcg.data.MiniGamesGateway
import fr.aumombelli.dstcg.model.DisplayCard
import fr.aumombelli.dstcg.model.MiniGameDifficulty
import fr.aumombelli.dstcg.model.MiniGameId
import fr.aumombelli.dstcg.model.MiniGameReward
import fr.aumombelli.dstcg.model.dailyStateFor
import fr.aumombelli.dstcg.model.unlockedDifficultyFor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

internal class QuizMiniGameController(
    private val miniGamesRepository: MiniGamesGateway,
    private val catalogRepository: CatalogGateway,
    private val uiState: MutableStateFlow<MiniGamesUiState>,
    private val feedbackEmitter: MiniGameFeedbackEmitter,
    private val launch: (suspend () -> Unit) -> Unit,
) {
    private var activeQuiz: QuizGame? = null
    private var questionIndex: Int = 0
    private var selectedAnswerIndex: Int? = null
    private var score: Int = 0
    private var corrections: List<QuizCorrectionUi> = emptyList()
    private var completionStarted: Boolean = false
    private var feedbackEvent: MiniGameFeedbackEvent? = null

    fun open() {
        launch {
            uiState.update { it.copy(isLoading = true, errorMessage = null) }
            runCatching {
                openQuizScreen()
            }.onFailure { error ->
                clear()
                uiState.update {
                    it.copy(
                        isLoading = false,
                        screen = MiniGamesScreenUiState.QuizUnavailable(
                            message = error.message ?: "Impossible de préparer le Quiz.",
                        ),
                    )
                }
            }
        }
    }

    fun selectDifficulty(difficulty: MiniGameDifficulty) {
        val current = uiState.value
        val choice = current.quizDifficultyChoices.firstOrNull { it.difficulty == difficulty }
        if (current.isLoading || choice?.enabled != true) {
            return
        }

        launch {
            uiState.update { it.copy(isLoading = true, errorMessage = null) }
            runCatching {
                startQuiz(difficulty)
            }.onFailure { error ->
                clear()
                uiState.update {
                    it.copy(
                        isLoading = false,
                        screen = MiniGamesScreenUiState.QuizUnavailable(
                            message = error.message ?: "Impossible de préparer le Quiz.",
                        ),
                    )
                }
            }
        }
    }

    fun selectAnswer(index: Int) {
        val quiz = activeQuiz ?: return
        if (completionStarted || selectedAnswerIndex != null) return
        val question = quiz.questions.getOrNull(questionIndex) ?: return
        if (index !in question.answers.indices) return

        val selectedAnswer = question.answers[index]
        val isCorrect = selectedAnswer == question.correctAnswer
        selectedAnswerIndex = index
        if (isCorrect) {
            score += 1
        }
        corrections += QuizCorrectionUi(
            prompt = question.prompt,
            selectedAnswer = selectedAnswer,
            correctAnswer = question.correctAnswer,
            explanation = question.explanation,
            isCorrect = isCorrect,
        )
        feedbackEvent = feedbackEmitter.next(
            tone = if (isCorrect) MiniGameFeedbackTone.Success else MiniGameFeedbackTone.Error,
            sourceIndexes = setOf(index),
        )
        publishPlayingState()
    }

    fun continueQuiz() {
        val quiz = activeQuiz ?: return
        if (completionStarted || selectedAnswerIndex == null) return
        if (questionIndex < quiz.questions.lastIndex) {
            questionIndex += 1
            selectedAnswerIndex = null
            feedbackEvent = null
            publishPlayingState()
        } else {
            completeQuiz()
        }
    }

    fun clear() {
        activeQuiz = null
        questionIndex = 0
        selectedAnswerIndex = null
        score = 0
        corrections = emptyList()
        completionStarted = false
        feedbackEvent = null
    }

    private suspend fun openQuizScreen() {
        val miniGamesState = miniGamesRepository.loadMiniGamesState()
        val dailyState = miniGamesState.progress.dailyStateFor(
            miniGameId = MiniGameId.Quiz,
            dateUtc = miniGamesState.todayUtc,
        )
        val rewardLabel = dailyState.reward?.let(::formatReward)
        if (dailyState.hasPlayed && rewardLabel == null) {
            uiState.value = miniGamesState.toUiState(
                screen = MiniGamesScreenUiState.QuizUnavailable(
                    message = "Ton essai Quiz est déjà utilisé pour aujourd'hui.",
                ),
            )
            return
        }

        val previewResult = buildQuizForDifficulty(
            difficulty = MiniGameDifficulty.Apprentice,
            todayUtc = miniGamesState.todayUtc,
        )
        val previewCard = (previewResult as? QuizGameBuildResult.Ready)?.game?.targetCard

        val screen = when {
            rewardLabel != null -> MiniGamesScreenUiState.QuizResult(
                card = previewCard,
                difficultyName = "Quiz",
                scoreLabel = "Déjà joué aujourd'hui",
                rewardLabel = rewardLabel,
                nextDifficultyName = null,
                feedbackEvent = null,
            )

            previewResult is QuizGameBuildResult.Unavailable -> MiniGamesScreenUiState.QuizUnavailable(
                message = previewResult.message,
            )

            previewCard != null -> MiniGamesScreenUiState.QuizDifficultySelection(previewCard)

            else -> MiniGamesScreenUiState.QuizUnavailable(
                message = "Impossible de préparer la carte du jour.",
            )
        }

        uiState.value = miniGamesState.toUiState(screen = screen)
    }

    private suspend fun startQuiz(difficulty: MiniGameDifficulty) {
        val miniGamesState = miniGamesRepository.loadMiniGamesState()
        val dailyState = miniGamesState.progress.dailyStateFor(
            miniGameId = MiniGameId.Quiz,
            dateUtc = miniGamesState.todayUtc,
        )
        if (dailyState.hasPlayed || dailyState.reward != null) {
            uiState.value = miniGamesState.toUiState(
                screen = alreadyPlayedQuizScreen(dailyState.reward, card = null),
            )
            return
        }
        val unlockedDifficulty = miniGamesState.progress.unlockedDifficultyFor(MiniGameId.Quiz)
        if (difficulty.level > unlockedDifficulty.level) {
            uiState.value = miniGamesState.toUiState(
                screen = MiniGamesScreenUiState.QuizUnavailable(
                    message = "Cette difficulté n'est pas encore débloquée.",
                ),
            )
            return
        }

        val game = when (
            val quizResult = buildQuizForDifficulty(
                difficulty = difficulty,
                todayUtc = miniGamesState.todayUtc,
            )
        ) {
            is QuizGameBuildResult.Ready -> quizResult.game
            is QuizGameBuildResult.Unavailable -> {
                uiState.value = miniGamesState.toUiState(
                    screen = MiniGamesScreenUiState.QuizUnavailable(quizResult.message),
                )
                return
            }
        }

        when (val consumed = miniGamesRepository.consumeAttemptForToday(MiniGameId.Quiz)) {
            is MiniGameAttemptConsumeResult.Consumed -> {
                activeQuiz = game
                questionIndex = 0
                selectedAnswerIndex = null
                score = 0
                corrections = emptyList()
                completionStarted = false
                feedbackEvent = null
                uiState.value = consumed.miniGamesProgress.toUiState(
                    todayUtc = miniGamesState.todayUtc,
                    screen = buildPlayingState(game),
                )
            }

            is MiniGameAttemptConsumeResult.AlreadyConsumed -> {
                clear()
                uiState.value = consumed.miniGamesProgress.toUiState(
                    todayUtc = miniGamesState.todayUtc,
                    screen = alreadyPlayedQuizScreen(consumed.dailyState.reward, card = game.targetCard),
                )
            }
        }
    }

    private suspend fun buildQuizForDifficulty(
        difficulty: MiniGameDifficulty,
        todayUtc: String,
    ): QuizGameBuildResult {
        val resolvedCards = miniGamesRepository.prepareResolvedCardsForToday(
            miniGameId = MiniGameId.Quiz,
            slotCount = 1,
        )
        return buildQuizGame(
            difficulty = difficulty,
            dateUtc = todayUtc,
            resolvedCards = resolvedCards,
            cards = catalogRepository.loadCards(),
            extensions = catalogRepository.loadExtensions(),
            variantProfiles = catalogRepository.loadVariantProfiles(),
        )
    }

    private fun completeQuiz() {
        val quiz = activeQuiz ?: return
        if (completionStarted) return
        completionStarted = true
        feedbackEvent = feedbackEmitter.next(
            tone = MiniGameFeedbackTone.Completion,
            sourceIndexes = emptySet(),
        )
        publishPlayingState()
        launch {
            runCatching {
                val reward = calculateQuizReward(
                    difficulty = quiz.difficulty,
                    correctCount = score,
                    questionCount = quiz.questions.size,
                )
                val grantResult = miniGamesRepository.grantRewardForToday(
                    miniGameId = MiniGameId.Quiz,
                    reward = reward,
                )
                val nextDifficulty = quiz.difficulty.next()
                    ?.takeIf { score == quiz.questions.size }
                if (grantResult is MiniGameRewardGrantResult.Granted && nextDifficulty != null) {
                    miniGamesRepository.unlockDifficulty(
                        miniGameId = MiniGameId.Quiz,
                        difficulty = nextDifficulty,
                    )
                }
                val refreshed = miniGamesRepository.loadMiniGamesState()
                val resultFeedbackEvent = feedbackEmitter.next(
                    tone = MiniGameFeedbackTone.Completion,
                    sourceIndexes = emptySet(),
                )
                val resultScreen = MiniGamesScreenUiState.QuizResult(
                    card = quiz.targetCard,
                    difficultyName = quiz.difficulty.displayName,
                    scoreLabel = "$score/${quiz.questions.size}",
                    rewardLabel = formatReward(reward),
                    corrections = corrections,
                    nextDifficultyName = nextDifficulty
                        ?.takeIf { grantResult is MiniGameRewardGrantResult.Granted }
                        ?.displayName,
                    feedbackEvent = resultFeedbackEvent,
                )
                clear()
                refreshed.toUiState(screen = resultScreen)
            }.onSuccess { updatedState ->
                uiState.value = updatedState
            }.onFailure { error ->
                clear()
                uiState.update {
                    it.copy(
                        isLoading = false,
                        screen = MiniGamesScreenUiState.QuizUnavailable(
                            message = error.message ?: "Impossible d'attribuer la récompense.",
                        ),
                    )
                }
            }
        }
    }

    private fun publishPlayingState() {
        val quiz = activeQuiz ?: return
        uiState.update {
            it.copy(
                isLoading = false,
                screen = buildPlayingState(quiz),
            )
        }
    }

    private fun buildPlayingState(quiz: QuizGame): MiniGamesScreenUiState.QuizPlaying {
        val question = quiz.questions[questionIndex]
        val selectedIndex = selectedAnswerIndex
        return MiniGamesScreenUiState.QuizPlaying(
            card = quiz.targetCard,
            difficultyName = quiz.difficulty.displayName,
            rewardLabel = formatReward(quiz.difficulty.reward),
            questionIndex = questionIndex,
            questionCount = quiz.questions.size,
            score = score,
            prompt = question.prompt,
            answers = question.answers.mapIndexed { index, answer ->
                QuizAnswerUi(
                    index = index,
                    text = answer,
                    state = when {
                        selectedIndex == null -> QuizAnswerState.Idle
                        answer == question.correctAnswer && selectedIndex == index -> QuizAnswerState.SelectedCorrect
                        answer == question.correctAnswer -> QuizAnswerState.Correct
                        selectedIndex == index -> QuizAnswerState.SelectedWrong
                        else -> QuizAnswerState.Idle
                    },
                )
            },
            canAdvance = selectedIndex != null,
            feedbackEvent = feedbackEvent,
        )
    }

    private fun alreadyPlayedQuizScreen(
        reward: MiniGameReward?,
        card: DisplayCard?,
    ): MiniGamesScreenUiState =
        if (reward != null) {
            MiniGamesScreenUiState.QuizResult(
                card = card,
                difficultyName = "Quiz",
                scoreLabel = "Déjà joué aujourd'hui",
                rewardLabel = formatReward(reward),
                nextDifficultyName = null,
                feedbackEvent = null,
            )
        } else {
            MiniGamesScreenUiState.QuizUnavailable(
                message = "Ton essai Quiz est déjà utilisé pour aujourd'hui.",
            )
        }
}
