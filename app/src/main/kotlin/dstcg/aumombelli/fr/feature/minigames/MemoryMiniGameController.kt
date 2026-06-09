package fr.aumombelli.dstcg.feature.minigames

import fr.aumombelli.dstcg.data.CatalogGateway
import fr.aumombelli.dstcg.data.MiniGameAttemptConsumeResult
import fr.aumombelli.dstcg.data.MiniGameRewardGrantResult
import fr.aumombelli.dstcg.data.MiniGamesGateway
import fr.aumombelli.dstcg.data.ProgressGateway
import fr.aumombelli.dstcg.data.requireUsableProgress
import fr.aumombelli.dstcg.model.MiniGameDifficulty
import fr.aumombelli.dstcg.model.MiniGameId
import fr.aumombelli.dstcg.model.MiniGameReward
import fr.aumombelli.dstcg.model.dailyStateFor
import fr.aumombelli.dstcg.model.unlockedDifficultyFor
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

internal class MemoryMiniGameController(
    private val miniGamesRepository: MiniGamesGateway,
    private val catalogRepository: CatalogGateway,
    private val progressRepository: ProgressGateway,
    private val uiState: MutableStateFlow<MiniGamesUiState>,
    private val feedbackEmitter: MiniGameFeedbackEmitter,
    private val launch: (suspend () -> Unit) -> Unit,
) {
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
    private var feedbackEvent: MiniGameFeedbackEvent? = null

    fun open() {
        val state = uiState.value
        if (state.isLoading) return
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
        uiState.update { it.copy(screen = screen, errorMessage = null) }
    }

    fun selectDifficulty(difficulty: MiniGameDifficulty) {
        val current = uiState.value
        val choice = current.memoryDifficultyChoices.firstOrNull { it.difficulty == difficulty }
        if (current.isLoading || choice?.enabled != true) {
            return
        }

        launch {
            uiState.update { it.copy(isLoading = true, errorMessage = null) }
            runCatching {
                startMemory(difficulty)
            }.onFailure { error ->
                clear()
                uiState.update {
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

    fun selectCell(index: Int) {
        val board = activeBoard ?: return
        if (inputLocked || completionStarted || index !in board.cells.indices) return
        if (index in matchedIndexes || index in mismatchIndexes || selectedIndex == index) return

        val selectedFace = (board.cells[index] as? MemoryBoardCell.Card)?.face ?: return
        val firstIndex = selectedIndex
        if (firstIndex == null) {
            selectedIndex = index
            publishPlayingState()
            return
        }

        selectedIndex = null
        val firstFace = (board.cells[firstIndex] as? MemoryBoardCell.Card)?.face ?: return
        val isPairMatch = firstFace.role == MemoryCardRole.Pair &&
            selectedFace.role == MemoryCardRole.Pair &&
            firstFace.identity == selectedFace.identity

        if (isPairMatch) {
            recordMove(
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

    fun clear() {
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

    private suspend fun startMemory(difficulty: MiniGameDifficulty) {
        val miniGamesState = miniGamesRepository.loadMiniGamesState()
        val dailyState = miniGamesState.progress.dailyStateFor(
            miniGameId = MiniGameId.Memory,
            dateUtc = miniGamesState.todayUtc,
        )
        if (dailyState.hasPlayed || dailyState.reward != null) {
            uiState.value = miniGamesState.toUiState(
                screen = alreadyPlayedScreen(dailyState.reward),
            )
            return
        }
        val unlockedDifficulty = miniGamesState.progress.unlockedDifficultyFor(MiniGameId.Memory)
        if (difficulty.level > unlockedDifficulty.level) {
            uiState.value = miniGamesState.toUiState(
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
                uiState.value = miniGamesState.toUiState(
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
                uiState.value = consumed.miniGamesProgress.toUiState(
                    todayUtc = miniGamesState.todayUtc,
                    screen = buildPlayingState(board),
                )
            }

            is MiniGameAttemptConsumeResult.AlreadyConsumed -> {
                clear()
                uiState.value = consumed.miniGamesProgress.toUiState(
                    todayUtc = miniGamesState.todayUtc,
                    screen = alreadyPlayedScreen(consumed.dailyState.reward),
                )
            }
        }
    }

    private fun showMismatch(firstIndex: Int, secondIndex: Int) {
        recordMove(
            matched = false,
            feedbackTone = MiniGameFeedbackTone.Error,
            sourceIndexes = setOf(firstIndex, secondIndex),
        )
        inputLocked = true
        mismatchIndexes = setOf(firstIndex, secondIndex)
        publishPlayingState()
        val revision = ++mismatchRevision
        launch {
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
        if (matchedIndexes.size != board.playableCellCount || completionStarted) return
        completionStarted = true
        inputLocked = true
        feedbackEvent = feedbackEmitter.next(
            tone = MiniGameFeedbackTone.Completion,
            sourceIndexes = matchedIndexes,
        )
        publishPlayingState()
        launch {
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
                val resultFeedbackEvent = feedbackEmitter.next(
                    tone = MiniGameFeedbackTone.Completion,
                    sourceIndexes = emptySet(),
                )
                clear()
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
                uiState.value = updatedState
            }.onFailure { error ->
                clear()
                uiState.update {
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
        uiState.update {
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
            cells = board.cells.mapIndexed { index, cell ->
                when (cell) {
                    is MemoryBoardCell.Card -> MemoryCellUi(
                        index = index,
                        face = cell.face,
                        state = when {
                            index in mismatchIndexes -> MemoryCellState.Mismatch
                            index in matchedIndexes -> MemoryCellState.Matched
                            selectedIndex == index -> MemoryCellState.Revealed
                            else -> MemoryCellState.Hidden
                        },
                    )

                    is MemoryBoardCell.Hole -> MemoryCellUi(
                        index = index,
                        face = null,
                        state = MemoryCellState.Hole,
                    )
                }
            },
            matchedCount = matchedIndexes.size,
            totalCount = board.playableCellCount,
            moves = moveCount,
            currentStreak = currentStreak,
            bestStreak = bestStreak,
            feedbackEvent = feedbackEvent,
            inputLocked = inputLocked,
        )
    }

    private fun recordMove(
        matched: Boolean,
        feedbackTone: MiniGameFeedbackTone,
        sourceIndexes: Set<Int>,
    ) {
        moveCount += 1
        currentStreak = if (matched) currentStreak + 1 else 0
        bestStreak = maxOf(bestStreak, currentStreak)
        feedbackEvent = feedbackEmitter.next(
            tone = feedbackTone,
            sourceIndexes = sourceIndexes,
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

private const val MemoryMismatchDelayMillis: Long = 650L
