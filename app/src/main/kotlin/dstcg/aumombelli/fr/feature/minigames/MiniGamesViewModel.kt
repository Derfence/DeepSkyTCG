package fr.aumombelli.dstcg.feature.minigames

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fr.aumombelli.dstcg.data.CatalogGateway
import fr.aumombelli.dstcg.data.MiniGamesGateway
import fr.aumombelli.dstcg.data.ProgressGateway
import fr.aumombelli.dstcg.model.MiniGameDifficulty
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal class MiniGamesViewModel(
    private val miniGamesRepository: MiniGamesGateway,
    catalogRepository: CatalogGateway,
    private val progressRepository: ProgressGateway,
) : ViewModel() {
    private val _uiState = MutableStateFlow(MiniGamesUiState())
    val uiState: StateFlow<MiniGamesUiState> = _uiState.asStateFlow()

    private val feedbackEmitter = MiniGameFeedbackEmitter()
    private val quizController = QuizMiniGameController(
        miniGamesRepository = miniGamesRepository,
        catalogRepository = catalogRepository,
        uiState = _uiState,
        feedbackEmitter = feedbackEmitter,
        launch = ::launchControllerWork,
    )
    private val memoryController = MemoryMiniGameController(
        miniGamesRepository = miniGamesRepository,
        catalogRepository = catalogRepository,
        progressRepository = progressRepository,
        uiState = _uiState,
        feedbackEmitter = feedbackEmitter,
        launch = ::launchControllerWork,
    )
    private val timelineController = TimelineMiniGameController(
        miniGamesRepository = miniGamesRepository,
        catalogRepository = catalogRepository,
        progressRepository = progressRepository,
        uiState = _uiState,
        feedbackEmitter = feedbackEmitter,
        launch = ::launchControllerWork,
    )
    private val observatoryController = ObservatoryMiniGameController(
        miniGamesRepository = miniGamesRepository,
        catalogRepository = catalogRepository,
        uiState = _uiState,
        feedbackEmitter = feedbackEmitter,
        launch = ::launchControllerWork,
    )

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
        if (_uiState.value.isLoading) return
        memoryController.clear()
        timelineController.clear()
        observatoryController.clear()
        quizController.open()
    }

    fun openMemory() {
        if (_uiState.value.isLoading) return
        quizController.clear()
        timelineController.clear()
        observatoryController.clear()
        memoryController.open()
    }

    fun openTimeline() {
        if (_uiState.value.isLoading) return
        memoryController.clear()
        quizController.clear()
        observatoryController.clear()
        timelineController.open()
    }

    fun openObservatory() {
        if (_uiState.value.isLoading) return
        memoryController.clear()
        quizController.clear()
        timelineController.clear()
        observatoryController.open()
    }

    fun backToMenu() {
        memoryController.clear()
        quizController.clear()
        timelineController.clear()
        observatoryController.clear()
        refresh()
    }

    fun resetDailyAttemptsForDebug() {
        if (_uiState.value.isLoading) return
        memoryController.clear()
        quizController.clear()
        timelineController.clear()
        observatoryController.clear()
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            runCatching {
                progressRepository.updateProgress { progress ->
                    progress.copy(
                        miniGamesProgress = progress.miniGamesProgress.copy(
                            dailyStates = progress.miniGamesProgress.dailyStates.mapValues { (_, dailyState) ->
                                dailyState.copy(
                                    hasPlayed = false,
                                    reward = null,
                                )
                            },
                        ),
                    )
                }
                miniGamesRepository.loadMiniGamesState()
            }.onSuccess { state ->
                _uiState.value = state.toUiState(screen = MiniGamesScreenUiState.Menu)
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "Impossible de réinitialiser les essais.",
                    )
                }
            }
        }
    }

    fun selectQuizDifficulty(difficulty: MiniGameDifficulty) {
        quizController.selectDifficulty(difficulty)
    }

    fun selectQuizAnswer(index: Int) {
        quizController.selectAnswer(index)
    }

    fun continueQuiz() {
        quizController.continueQuiz()
    }

    fun selectMemoryDifficulty(difficulty: MiniGameDifficulty) {
        memoryController.selectDifficulty(difficulty)
    }

    fun selectMemoryCell(index: Int) {
        memoryController.selectCell(index)
    }

    fun placeTimelineCard(cardId: String, slotIndex: Int) {
        timelineController.placeCard(cardId, slotIndex)
    }

    fun returnTimelineCardToHand(cardId: String, handSlotIndex: Int) {
        timelineController.returnCardToHand(cardId, handSlotIndex)
    }

    fun selectTimelineDifficulty(difficulty: MiniGameDifficulty) {
        timelineController.selectDifficulty(difficulty)
    }

    fun validateTimeline() {
        timelineController.validate()
    }

    fun selectObservatoryDifficulty(difficulty: MiniGameDifficulty) {
        observatoryController.selectDifficulty(difficulty)
    }

    fun setObservatoryDomeProgress(progress: Float) {
        observatoryController.setDomeProgress(progress)
    }

    fun validateObservatoryDomeProgress() {
        observatoryController.validateDomeProgress()
    }

    fun setObservatoryAzimuth(value: Float) {
        observatoryController.setAzimuth(value)
    }

    fun setObservatoryAltitude(value: Float) {
        observatoryController.setAltitude(value)
    }

    fun validateObservatoryAlignment() {
        observatoryController.validateAlignment()
    }

    fun setObservatoryFocus(value: Float) {
        observatoryController.setFocus(value)
    }

    fun validateObservatoryFocus() {
        observatoryController.validateFocus()
    }

    fun scrubObservatoryCloud(amount: Float) {
        observatoryController.scrubCloud(amount)
    }

    fun clearObservatoryCloud() {
        observatoryController.clearCloud()
    }

    fun captureObservatoryTarget() {
        observatoryController.captureTarget()
    }

    private fun launchControllerWork(block: suspend () -> Unit) {
        viewModelScope.launch {
            block()
        }
    }
}
