package fr.aumombelli.gatcha.feature.start

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fr.aumombelli.gatcha.data.ProgressLoadResult
import fr.aumombelli.gatcha.data.ProgressGateway
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class StartUiState(
    val isLoading: Boolean = true,
    val isTransitioningToMenu: Boolean = false,
    val errorMessage: String? = null,
    val isResettingProgress: Boolean = false,
)

sealed interface StartEvent {
    data object ReadyToEnterMenu : StartEvent
}

class StartViewModel(
    private val progressRepository: ProgressGateway,
) : ViewModel() {
    private val _uiState = MutableStateFlow(StartUiState())
    val uiState: StateFlow<StartUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<StartEvent>()
    val events: SharedFlow<StartEvent> = _events.asSharedFlow()

    init {
        refreshProgressState()
    }

    fun begin() {
        val state = _uiState.value
        if (
            state.isLoading ||
            state.errorMessage != null ||
            state.isTransitioningToMenu ||
            state.isResettingProgress
        ) {
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isTransitioningToMenu = true) }
            _events.emit(StartEvent.ReadyToEnterMenu)
        }
    }

    fun resetProgress() {
        val state = _uiState.value
        if (state.isLoading || state.isResettingProgress) {
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isResettingProgress = true) }
            runCatching { progressRepository.resetProgress() }
                .onSuccess {
                    refreshProgressState()
                }
                .onFailure { exception ->
                    _uiState.value = StartUiState(
                        isLoading = false,
                        errorMessage = exception.message ?: "Impossible de reinitialiser la progression locale.",
                    )
                }
        }
    }

    private fun refreshProgressState() {
        viewModelScope.launch {
            _uiState.value = StartUiState(isLoading = true)
            runCatching { progressRepository.loadProgress() }
                .onSuccess { result ->
                    _uiState.value = when (result) {
                        is ProgressLoadResult.Ok -> StartUiState(isLoading = false)
                        is ProgressLoadResult.Recovered -> StartUiState(isLoading = false)

                        is ProgressLoadResult.Compromised -> StartUiState(
                            isLoading = false,
                            errorMessage = result.message,
                        )
                    }
                }
                .onFailure { exception ->
                    _uiState.value = StartUiState(
                        isLoading = false,
                        errorMessage = exception.message ?: "Impossible de charger la progression locale.",
                    )
                }
        }
    }
}
