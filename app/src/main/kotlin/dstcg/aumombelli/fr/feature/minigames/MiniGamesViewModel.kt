package fr.aumombelli.dstcg.feature.minigames

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fr.aumombelli.dstcg.data.CatalogGateway
import fr.aumombelli.dstcg.data.MiniGameAttemptConsumeResult
import fr.aumombelli.dstcg.data.MiniGameRewardGrantResult
import fr.aumombelli.dstcg.data.MiniGamesGateway
import fr.aumombelli.dstcg.data.ProgressGateway
import fr.aumombelli.dstcg.data.requireUsableProgress
import fr.aumombelli.dstcg.model.DisplayCard
import fr.aumombelli.dstcg.model.MiniGameDifficulty
import fr.aumombelli.dstcg.model.MiniGameId
import fr.aumombelli.dstcg.model.MiniGameReward
import fr.aumombelli.dstcg.model.MiniGamesProgress
import fr.aumombelli.dstcg.model.dailyStateFor
import fr.aumombelli.dstcg.model.unlockedDifficultyFor
import java.time.Duration
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

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
}

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

internal data class MemoryDifficultyChoiceUi(
    val difficulty: MiniGameDifficulty,
    val title: String,
    val gridLabel: String,
    val rewardLabel: String,
    val enabled: Boolean,
    val locked: Boolean,
    val statusLabel: String,
) {
    val testTag: String = "memory-difficulty-${difficulty.name.lowercase()}"
}

internal data class MemoryCellUi(
    val index: Int,
    val face: MemoryCardFace,
    val state: MemoryCellState,
) {
    val testTag: String = "memory-cell-$index"
    val isVisible: Boolean = state != MemoryCellState.Hidden
}

internal enum class MemoryCellState {
    Hidden,
    Revealed,
    Matched,
    Mismatch,
}

internal class MiniGamesViewModel(
    private val miniGamesRepository: MiniGamesGateway,
    private val catalogRepository: CatalogGateway,
    private val progressRepository: ProgressGateway,
) : ViewModel() {
    private val _uiState = MutableStateFlow(MiniGamesUiState())
    val uiState: StateFlow<MiniGamesUiState> = _uiState.asStateFlow()

    private var activeBoard: MemoryBoard? = null
    private var selectedIndex: Int? = null
    private var matchedIndexes: Set<Int> = emptySet()
    private var mismatchIndexes: Set<Int> = emptySet()
    private var inputLocked: Boolean = false
    private var completionStarted: Boolean = false
    private var mismatchRevision: Int = 0
    private var moveCount: Int = 0
    private var currentStreak: Int = 0
    private var bestStreak: Int = 0
    private var feedbackSequence: Long = 0L
    private var feedbackEvent: MiniGameFeedbackEvent? = null
    private var activeQuiz: QuizGame? = null
    private var quizQuestionIndex: Int = 0
    private var selectedQuizAnswerIndex: Int? = null
    private var quizScore: Int = 0
    private var quizCorrections: List<QuizCorrectionUi> = emptyList()
    private var quizCompletionStarted: Boolean = false

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            runCatching {
                miniGamesRepository.loadMiniGamesState()
            }.onSuccess { state ->
                _uiState.value = state.toUiState(screen = MiniGamesScreenUiState.Menu)
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "Impossible de charger les mini-jeux.",
                    )
                }
            }
        }
    }

    fun openQuiz() {
        val state = _uiState.value
        if (state.isLoading) return
        clearActiveBoard()

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            runCatching {
                openQuizScreen()
            }.onFailure { error ->
                clearActiveQuiz()
                _uiState.update {
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

    fun openMemory() {
        val state = _uiState.value
        if (state.isLoading) return
        clearActiveQuiz()
        val rewardLabel = state.memoryRewardLabel
        val screen = when {
            rewardLabel != null -> MiniGamesScreenUiState.MemoryResult(
                difficultyName = memoryDifficultyNameForReward(rewardLabel),
                rewardLabel = rewardLabel,
                nextDifficultyName = null,
                feedbackEvent = null,
            )

            state.memoryPlayedToday -> MiniGamesScreenUiState.MemoryUnavailable(
                message = "Ton essai Memory est déjà utilisé pour aujourd'hui.",
            )

            else -> MiniGamesScreenUiState.MemoryDifficultySelection
        }
        _uiState.update { it.copy(screen = screen, errorMessage = null) }
    }

    fun backToMenu() {
        clearActiveBoard()
        clearActiveQuiz()
        refresh()
    }

    fun selectQuizDifficulty(difficulty: MiniGameDifficulty) {
        val current = _uiState.value
        val choice = current.quizDifficultyChoices.firstOrNull { it.difficulty == difficulty }
        if (current.isLoading || choice?.enabled != true) {
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            runCatching {
                startQuiz(difficulty)
            }.onFailure { error ->
                clearActiveQuiz()
                _uiState.update {
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

    fun selectQuizAnswer(index: Int) {
        val quiz = activeQuiz ?: return
        if (quizCompletionStarted || selectedQuizAnswerIndex != null) return
        val question = quiz.questions.getOrNull(quizQuestionIndex) ?: return
        if (index !in question.answers.indices) return

        val selectedAnswer = question.answers[index]
        val isCorrect = selectedAnswer == question.correctAnswer
        selectedQuizAnswerIndex = index
        if (isCorrect) {
            quizScore += 1
        }
        quizCorrections += QuizCorrectionUi(
            prompt = question.prompt,
            selectedAnswer = selectedAnswer,
            correctAnswer = question.correctAnswer,
            explanation = question.explanation,
            isCorrect = isCorrect,
        )
        feedbackEvent = nextFeedbackEvent(
            tone = if (isCorrect) MiniGameFeedbackTone.Success else MiniGameFeedbackTone.Error,
            sourceIndexes = setOf(index),
        )
        publishQuizPlayingState()
    }

    fun continueQuiz() {
        val quiz = activeQuiz ?: return
        if (quizCompletionStarted || selectedQuizAnswerIndex == null) return
        if (quizQuestionIndex < quiz.questions.lastIndex) {
            quizQuestionIndex += 1
            selectedQuizAnswerIndex = null
            feedbackEvent = null
            publishQuizPlayingState()
        } else {
            completeQuiz()
        }
    }

    fun selectMemoryDifficulty(difficulty: MiniGameDifficulty) {
        val current = _uiState.value
        val choice = current.memoryDifficultyChoices.firstOrNull { it.difficulty == difficulty }
        if (current.isLoading || choice?.enabled != true) {
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            runCatching {
                startMemory(difficulty)
            }.onFailure { error ->
                clearActiveBoard()
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        screen = MiniGamesScreenUiState.MemoryUnavailable(
                            message = error.message ?: "Impossible de préparer le Memory.",
                        ),
                    )
                }
            }
        }
    }

    fun selectMemoryCell(index: Int) {
        val board = activeBoard ?: return
        if (inputLocked || completionStarted || index !in board.cards.indices) return
        if (index in matchedIndexes || index in mismatchIndexes || selectedIndex == index) return

        val selectedFace = board.cards[index]
        val firstIndex = selectedIndex
        if (firstIndex == null) {
            if (selectedFace.role == MemoryCardRole.HolographicSingleton) {
                recordMemoryMove(
                    matched = true,
                    feedbackTone = MiniGameFeedbackTone.Special,
                    sourceIndexes = setOf(index),
                )
                matchedIndexes += index
                publishPlayingState()
                completeIfNeeded()
            } else {
                selectedIndex = index
                publishPlayingState()
            }
            return
        }

        selectedIndex = null
        val firstFace = board.cards[firstIndex]
        val isPairMatch = firstFace.role == MemoryCardRole.Pair &&
            selectedFace.role == MemoryCardRole.Pair &&
            firstFace.identity == selectedFace.identity

        if (isPairMatch) {
            recordMemoryMove(
                matched = true,
                feedbackTone = MiniGameFeedbackTone.Success,
                sourceIndexes = setOf(firstIndex, index),
            )
            matchedIndexes += setOf(firstIndex, index)
            publishPlayingState()
            completeIfNeeded()
        } else {
            showMismatch(firstIndex, index)
        }
    }

    private suspend fun openQuizScreen() {
        val miniGamesState = miniGamesRepository.loadMiniGamesState()
        val dailyState = miniGamesState.progress.dailyStateFor(
            miniGameId = MiniGameId.Quiz,
            dateUtc = miniGamesState.todayUtc,
        )
        val rewardLabel = dailyState.reward?.let(::formatReward)
        if (dailyState.hasPlayed && rewardLabel == null) {
            _uiState.value = miniGamesState.toUiState(
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

        _uiState.value = miniGamesState.toUiState(screen = screen)
    }

    private suspend fun startQuiz(difficulty: MiniGameDifficulty) {
        val miniGamesState = miniGamesRepository.loadMiniGamesState()
        val dailyState = miniGamesState.progress.dailyStateFor(
            miniGameId = MiniGameId.Quiz,
            dateUtc = miniGamesState.todayUtc,
        )
        if (dailyState.hasPlayed || dailyState.reward != null) {
            _uiState.value = miniGamesState.toUiState(
                screen = alreadyPlayedQuizScreen(dailyState.reward, card = null),
            )
            return
        }
        val unlockedDifficulty = miniGamesState.progress.unlockedDifficultyFor(MiniGameId.Quiz)
        if (difficulty.level > unlockedDifficulty.level) {
            _uiState.value = miniGamesState.toUiState(
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
                _uiState.value = miniGamesState.toUiState(
                    screen = MiniGamesScreenUiState.QuizUnavailable(quizResult.message),
                )
                return
            }
        }

        when (val consumed = miniGamesRepository.consumeAttemptForToday(MiniGameId.Quiz)) {
            is MiniGameAttemptConsumeResult.Consumed -> {
                activeQuiz = game
                quizQuestionIndex = 0
                selectedQuizAnswerIndex = null
                quizScore = 0
                quizCorrections = emptyList()
                quizCompletionStarted = false
                feedbackEvent = null
                _uiState.value = consumed.miniGamesProgress.toUiState(
                    todayUtc = miniGamesState.todayUtc,
                    screen = buildQuizPlayingState(game),
                )
            }

            is MiniGameAttemptConsumeResult.AlreadyConsumed -> {
                clearActiveQuiz()
                _uiState.value = consumed.miniGamesProgress.toUiState(
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

    private suspend fun startMemory(difficulty: MiniGameDifficulty) {
        val miniGamesState = miniGamesRepository.loadMiniGamesState()
        val dailyState = miniGamesState.progress.dailyStateFor(
            miniGameId = MiniGameId.Memory,
            dateUtc = miniGamesState.todayUtc,
        )
        if (dailyState.hasPlayed || dailyState.reward != null) {
            _uiState.value = miniGamesState.toUiState(
                screen = alreadyPlayedScreen(dailyState.reward),
            )
            return
        }
        val unlockedDifficulty = miniGamesState.progress.unlockedDifficultyFor(MiniGameId.Memory)
        if (difficulty.level > unlockedDifficulty.level) {
            _uiState.value = miniGamesState.toUiState(
                screen = MiniGamesScreenUiState.MemoryUnavailable(
                    message = "Cette difficulté n'est pas encore débloquée.",
                ),
            )
            return
        }

        val spec = MemoryDifficultySpec.forDifficulty(difficulty)
        val resolvedPairs = miniGamesRepository.prepareResolvedCardsForToday(
            miniGameId = MiniGameId.Memory,
            slotCount = spec.pairCount,
        )
        val loadedProgress = progressRepository.loadProgress().requireUsableProgress()
        val boardResult = buildMemoryBoard(
            difficulty = difficulty,
            dateUtc = miniGamesState.todayUtc,
            resolvedPairCards = resolvedPairs,
            cards = catalogRepository.loadCards(),
            extensions = catalogRepository.loadExtensions(),
            variantProfiles = catalogRepository.loadVariantProfiles(),
            collection = loadedProgress.progress.collection,
        )
        val board = when (boardResult) {
            is MemoryBoardBuildResult.Ready -> boardResult.board
            is MemoryBoardBuildResult.Unavailable -> {
                _uiState.value = miniGamesState.toUiState(
                    screen = MiniGamesScreenUiState.MemoryUnavailable(boardResult.message),
                )
                return
            }
        }

        when (val consumed = miniGamesRepository.consumeAttemptForToday(MiniGameId.Memory)) {
            is MiniGameAttemptConsumeResult.Consumed -> {
                activeBoard = board
                selectedIndex = null
                matchedIndexes = emptySet()
                mismatchIndexes = emptySet()
                inputLocked = false
                completionStarted = false
                moveCount = 0
                currentStreak = 0
                bestStreak = 0
                feedbackEvent = null
                _uiState.value = consumed.miniGamesProgress.toUiState(
                    todayUtc = miniGamesState.todayUtc,
                    screen = buildPlayingState(board),
                )
            }

            is MiniGameAttemptConsumeResult.AlreadyConsumed -> {
                clearActiveBoard()
                _uiState.value = consumed.miniGamesProgress.toUiState(
                    todayUtc = miniGamesState.todayUtc,
                    screen = alreadyPlayedScreen(consumed.dailyState.reward),
                )
            }
        }
    }

    private fun completeQuiz() {
        val quiz = activeQuiz ?: return
        if (quizCompletionStarted) return
        quizCompletionStarted = true
        feedbackEvent = nextFeedbackEvent(
            tone = MiniGameFeedbackTone.Completion,
            sourceIndexes = emptySet(),
        )
        publishQuizPlayingState()
        viewModelScope.launch {
            runCatching {
                val reward = calculateQuizReward(
                    difficulty = quiz.difficulty,
                    correctCount = quizScore,
                    questionCount = quiz.questions.size,
                )
                val grantResult = miniGamesRepository.grantRewardForToday(
                    miniGameId = MiniGameId.Quiz,
                    reward = reward,
                )
                val nextDifficulty = quiz.difficulty.next()
                    ?.takeIf { quizScore == quiz.questions.size }
                if (grantResult is MiniGameRewardGrantResult.Granted && nextDifficulty != null) {
                    miniGamesRepository.unlockDifficulty(
                        miniGameId = MiniGameId.Quiz,
                        difficulty = nextDifficulty,
                    )
                }
                val refreshed = miniGamesRepository.loadMiniGamesState()
                val resultFeedbackEvent = nextFeedbackEvent(
                    tone = MiniGameFeedbackTone.Completion,
                    sourceIndexes = emptySet(),
                )
                val resultScreen = MiniGamesScreenUiState.QuizResult(
                    card = quiz.targetCard,
                    difficultyName = quiz.difficulty.displayName,
                    scoreLabel = "$quizScore/${quiz.questions.size}",
                    rewardLabel = formatReward(reward),
                    corrections = quizCorrections,
                    nextDifficultyName = nextDifficulty
                        ?.takeIf { grantResult is MiniGameRewardGrantResult.Granted }
                        ?.displayName,
                    feedbackEvent = resultFeedbackEvent,
                )
                clearActiveQuiz()
                refreshed.toUiState(screen = resultScreen)
            }.onSuccess { updatedState ->
                _uiState.value = updatedState
            }.onFailure { error ->
                clearActiveQuiz()
                _uiState.update {
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

    private fun publishQuizPlayingState() {
        val quiz = activeQuiz ?: return
        _uiState.update {
            it.copy(
                isLoading = false,
                screen = buildQuizPlayingState(quiz),
            )
        }
    }

    private fun buildQuizPlayingState(quiz: QuizGame): MiniGamesScreenUiState.QuizPlaying {
        val question = quiz.questions[quizQuestionIndex]
        val selectedIndex = selectedQuizAnswerIndex
        return MiniGamesScreenUiState.QuizPlaying(
            card = quiz.targetCard,
            difficultyName = quiz.difficulty.displayName,
            rewardLabel = formatReward(quiz.difficulty.reward),
            questionIndex = quizQuestionIndex,
            questionCount = quiz.questions.size,
            score = quizScore,
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

    private fun showMismatch(firstIndex: Int, secondIndex: Int) {
        recordMemoryMove(
            matched = false,
            feedbackTone = MiniGameFeedbackTone.Error,
            sourceIndexes = setOf(firstIndex, secondIndex),
        )
        inputLocked = true
        mismatchIndexes = setOf(firstIndex, secondIndex)
        publishPlayingState()
        val revision = ++mismatchRevision
        viewModelScope.launch {
            delay(MemoryMismatchDelayMillis)
            if (revision == mismatchRevision) {
                mismatchIndexes = emptySet()
                inputLocked = false
                publishPlayingState()
            }
        }
    }

    private fun completeIfNeeded() {
        val board = activeBoard ?: return
        if (matchedIndexes.size != board.cards.size || completionStarted) return
        completionStarted = true
        inputLocked = true
        feedbackEvent = nextFeedbackEvent(
            tone = MiniGameFeedbackTone.Completion,
            sourceIndexes = matchedIndexes,
        )
        publishPlayingState()
        viewModelScope.launch {
            runCatching {
                val grantResult = miniGamesRepository.grantRewardForToday(
                    miniGameId = MiniGameId.Memory,
                    reward = board.difficulty.reward,
                )
                val nextDifficulty = board.difficulty.next()
                if (grantResult is MiniGameRewardGrantResult.Granted && nextDifficulty != null) {
                    miniGamesRepository.unlockDifficulty(
                        miniGameId = MiniGameId.Memory,
                        difficulty = nextDifficulty,
                    )
                }
                val refreshed = miniGamesRepository.loadMiniGamesState()
                val resultFeedbackEvent = nextFeedbackEvent(
                    tone = MiniGameFeedbackTone.Completion,
                    sourceIndexes = emptySet(),
                )
                clearActiveBoard()
                refreshed.toUiState(
                    screen = MiniGamesScreenUiState.MemoryResult(
                        difficultyName = board.difficulty.displayName,
                        rewardLabel = formatReward(board.difficulty.reward),
                        nextDifficultyName = nextDifficulty
                            ?.takeIf { grantResult is MiniGameRewardGrantResult.Granted }
                            ?.displayName,
                        feedbackEvent = resultFeedbackEvent,
                    ),
                )
            }.onSuccess { updatedState ->
                _uiState.value = updatedState
            }.onFailure { error ->
                clearActiveBoard()
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        screen = MiniGamesScreenUiState.MemoryUnavailable(
                            message = error.message ?: "Impossible d'attribuer la récompense.",
                        ),
                    )
                }
            }
        }
    }

    private fun publishPlayingState() {
        val board = activeBoard ?: return
        _uiState.update {
            it.copy(
                isLoading = false,
                screen = buildPlayingState(board),
            )
        }
    }

    private fun buildPlayingState(board: MemoryBoard): MiniGamesScreenUiState.MemoryPlaying {
        val spec = MemoryDifficultySpec.forDifficulty(board.difficulty)
        return MiniGamesScreenUiState.MemoryPlaying(
            difficultyName = board.difficulty.displayName,
            gridLabel = spec.gridLabel,
            rewardLabel = formatReward(board.difficulty.reward),
            columns = board.columns,
            cells = board.cards.mapIndexed { index, face ->
                MemoryCellUi(
                    index = index,
                    face = face,
                    state = when {
                        index in mismatchIndexes -> MemoryCellState.Mismatch
                        index in matchedIndexes -> MemoryCellState.Matched
                        selectedIndex == index -> MemoryCellState.Revealed
                        else -> MemoryCellState.Hidden
                    },
                )
            },
            matchedCount = matchedIndexes.size,
            totalCount = board.cellCount,
            moves = moveCount,
            currentStreak = currentStreak,
            bestStreak = bestStreak,
            feedbackEvent = feedbackEvent,
            inputLocked = inputLocked,
        )
    }

    private fun recordMemoryMove(
        matched: Boolean,
        feedbackTone: MiniGameFeedbackTone,
        sourceIndexes: Set<Int>,
    ) {
        moveCount += 1
        currentStreak = if (matched) currentStreak + 1 else 0
        bestStreak = maxOf(bestStreak, currentStreak)
        feedbackEvent = nextFeedbackEvent(
            tone = feedbackTone,
            sourceIndexes = sourceIndexes,
        )
    }

    private fun nextFeedbackEvent(
        tone: MiniGameFeedbackTone,
        sourceIndexes: Set<Int>,
    ): MiniGameFeedbackEvent {
        feedbackSequence += 1L
        return MiniGameFeedbackEvent(
            id = feedbackSequence,
            tone = tone,
            sourceIndexes = sourceIndexes,
        )
    }

    private fun clearActiveBoard() {
        mismatchRevision++
        activeBoard = null
        selectedIndex = null
        matchedIndexes = emptySet()
        mismatchIndexes = emptySet()
        inputLocked = false
        completionStarted = false
        moveCount = 0
        currentStreak = 0
        bestStreak = 0
        feedbackEvent = null
    }

    private fun clearActiveQuiz() {
        activeQuiz = null
        quizQuestionIndex = 0
        selectedQuizAnswerIndex = null
        quizScore = 0
        quizCorrections = emptyList()
        quizCompletionStarted = false
        feedbackEvent = null
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

    private fun alreadyPlayedScreen(reward: MiniGameReward?): MiniGamesScreenUiState =
        if (reward != null) {
            MiniGamesScreenUiState.MemoryResult(
                difficultyName = memoryDifficultyNameForReward(formatReward(reward)),
                rewardLabel = formatReward(reward),
                nextDifficultyName = null,
                feedbackEvent = null,
            )
        } else {
            MiniGamesScreenUiState.MemoryUnavailable(
                message = "Ton essai Memory est déjà utilisé pour aujourd'hui.",
            )
        }
}

private fun fr.aumombelli.dstcg.data.MiniGamesState.toUiState(
    screen: MiniGamesScreenUiState,
): MiniGamesUiState = progress.toUiState(
    todayUtc = todayUtc,
    screen = screen,
)

private fun MiniGamesProgress.toUiState(
    todayUtc: String,
    screen: MiniGamesScreenUiState,
): MiniGamesUiState {
    val quizDailyState = dailyStateFor(MiniGameId.Quiz, todayUtc)
    val quizUnlockedDifficulty = unlockedDifficultyFor(MiniGameId.Quiz)
    val quizRewardLabel = quizDailyState.reward?.let(::formatReward)
    val quizPlayedToday = quizDailyState.hasPlayed || quizDailyState.reward != null
    val memoryDailyState = dailyStateFor(MiniGameId.Memory, todayUtc)
    val memoryUnlockedDifficulty = unlockedDifficultyFor(MiniGameId.Memory)
    val memoryRewardLabel = memoryDailyState.reward?.let(::formatReward)
    val memoryPlayedToday = memoryDailyState.hasPlayed || memoryDailyState.reward != null
    return MiniGamesUiState(
        isLoading = false,
        todayUtc = todayUtc,
        quizStatusLabel = when {
            quizRewardLabel != null -> "Joué aujourd'hui - $quizRewardLabel gagnées"
            quizPlayedToday -> "Essai utilisé aujourd'hui"
            else -> "Disponible - ${quizUnlockedDifficulty.displayName}"
        },
        quizPlayedToday = quizPlayedToday,
        quizRewardLabel = quizRewardLabel,
        quizDifficultyChoices = MiniGameDifficulty.entries.map { difficulty ->
            val unlocked = difficulty.level <= quizUnlockedDifficulty.level
            val spec = QuizDifficultySpec.forDifficulty(difficulty)
            QuizDifficultyChoiceUi(
                difficulty = difficulty,
                title = difficulty.displayName,
                questionLabel = spec.questionLabel,
                rewardLabel = formatReward(difficulty.reward),
                enabled = unlocked && !quizPlayedToday,
                locked = !unlocked,
                statusLabel = when {
                    quizPlayedToday -> "Déjà joué"
                    unlocked -> "Disponible"
                    else -> "À débloquer"
                },
            )
        },
        memoryStatusLabel = when {
            memoryRewardLabel != null -> "Joué aujourd'hui - $memoryRewardLabel gagnées"
            memoryPlayedToday -> "Essai utilisé aujourd'hui"
            else -> "Disponible - ${memoryUnlockedDifficulty.displayName}"
        },
        memoryPlayedToday = memoryPlayedToday,
        memoryRewardLabel = memoryRewardLabel,
        memoryDifficultyChoices = MiniGameDifficulty.entries.map { difficulty ->
            val unlocked = difficulty.level <= memoryUnlockedDifficulty.level
            val spec = MemoryDifficultySpec.forDifficulty(difficulty)
            MemoryDifficultyChoiceUi(
                difficulty = difficulty,
                title = difficulty.displayName,
                gridLabel = spec.gridLabel,
                rewardLabel = formatReward(difficulty.reward),
                enabled = unlocked && !memoryPlayedToday,
                locked = !unlocked,
                statusLabel = when {
                    memoryPlayedToday -> "Déjà joué"
                    unlocked -> "Disponible"
                    else -> "À débloquer"
                },
            )
        },
        screen = screen,
    )
}

internal fun formatReward(reward: MiniGameReward): String =
    formatRewardDuration(Duration.ofSeconds(reward.reductionSeconds.coerceAtLeast(0L)))

private fun formatRewardDuration(duration: Duration): String {
    val totalSeconds = duration.seconds.coerceAtLeast(0L)
    val hours = totalSeconds / 3600L
    val minutes = (totalSeconds % 3600L) / 60L
    val seconds = totalSeconds % 60L
    return when {
        hours > 0L && minutes > 0L && seconds > 0L -> "${hours}h ${minutes}min ${seconds}s"
        hours > 0L && minutes > 0L -> "${hours}h ${minutes}min"
        hours > 0L && seconds > 0L -> "${hours}h ${seconds}s"
        hours > 0L -> "${hours}h"
        minutes > 0L && seconds > 0L -> "${minutes}min ${seconds}s"
        minutes > 0L -> "${minutes}min"
        seconds > 0L -> "${seconds}s"
        else -> "0min"
    }
}

private fun memoryDifficultyNameForReward(rewardLabel: String): String =
    MiniGameDifficulty.entries.firstOrNull { formatReward(it.reward) == rewardLabel }?.displayName
        ?: "Memory"

private const val MemoryMismatchDelayMillis: Long = 650L
