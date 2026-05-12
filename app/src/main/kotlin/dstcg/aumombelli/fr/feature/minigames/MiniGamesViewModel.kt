package fr.aumombelli.dstcg.feature.minigames

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fr.aumombelli.dstcg.data.CatalogGateway
import fr.aumombelli.dstcg.data.MiniGameAttemptConsumeResult
import fr.aumombelli.dstcg.data.MiniGameRewardGrantResult
import fr.aumombelli.dstcg.data.MiniGamesGateway
import fr.aumombelli.dstcg.data.ProgressGateway
import fr.aumombelli.dstcg.data.requireUsableProgress
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
    val memoryStatusLabel: String = "Chargement",
    val memoryPlayedToday: Boolean = false,
    val memoryRewardLabel: String? = null,
    val memoryDifficultyChoices: List<MemoryDifficultyChoiceUi> = emptyList(),
    val screen: MiniGamesScreenUiState = MiniGamesScreenUiState.Menu,
    val errorMessage: String? = null,
)

internal sealed interface MiniGamesScreenUiState {
    data object Menu : MiniGamesScreenUiState

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

    fun openMemory() {
        val state = _uiState.value
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
        _uiState.update { it.copy(screen = screen, errorMessage = null) }
    }

    fun backToMenu() {
        clearActiveBoard()
        refresh()
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
    val dailyState = dailyStateFor(MiniGameId.Memory, todayUtc)
    val unlockedDifficulty = unlockedDifficultyFor(MiniGameId.Memory)
    val rewardLabel = dailyState.reward?.let(::formatReward)
    val playedToday = dailyState.hasPlayed || dailyState.reward != null
    return MiniGamesUiState(
        isLoading = false,
        todayUtc = todayUtc,
        memoryStatusLabel = when {
            rewardLabel != null -> "Joué aujourd'hui - $rewardLabel gagnées"
            playedToday -> "Essai utilisé aujourd'hui"
            else -> "Disponible - ${unlockedDifficulty.displayName}"
        },
        memoryPlayedToday = playedToday,
        memoryRewardLabel = rewardLabel,
        memoryDifficultyChoices = MiniGameDifficulty.entries.map { difficulty ->
            val unlocked = difficulty.level <= unlockedDifficulty.level
            val spec = MemoryDifficultySpec.forDifficulty(difficulty)
            MemoryDifficultyChoiceUi(
                difficulty = difficulty,
                title = difficulty.displayName,
                gridLabel = spec.gridLabel,
                rewardLabel = formatReward(difficulty.reward),
                enabled = unlocked && !playedToday,
                locked = !unlocked,
                statusLabel = when {
                    playedToday -> "Déjà joué"
                    unlocked -> "Disponible"
                    else -> "À débloquer"
                },
            )
        },
        screen = screen,
    )
}

internal fun formatReward(reward: MiniGameReward): String =
    formatRewardDuration(Duration.ofMinutes(reward.reductionMinutes.coerceAtLeast(0L)))

private fun formatRewardDuration(duration: Duration): String {
    val totalMinutes = duration.toMinutes().coerceAtLeast(0L)
    val hours = totalMinutes / 60L
    val minutes = totalMinutes % 60L
    return when {
        hours > 0L && minutes > 0L -> "${hours}h ${minutes}min"
        hours > 0L -> "${hours}h"
        else -> "${minutes}min"
    }
}

private fun memoryDifficultyNameForReward(rewardLabel: String): String =
    MiniGameDifficulty.entries.firstOrNull { formatReward(it.reward) == rewardLabel }?.displayName
        ?: "Memory"

private const val MemoryMismatchDelayMillis: Long = 650L
