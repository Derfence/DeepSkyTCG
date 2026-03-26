package fr.aumombelli.gatcha.feature.start

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
        viewModelScope.launch {
            runCatching { progressRepository.loadProgress() }
                .onSuccess {
                    _uiState.value = StartUiState(isLoading = false)
                }
                .onFailure { exception ->
                    _uiState.value = StartUiState(
                        isLoading = false,
                        errorMessage = exception.message ?: "Unable to load local progress.",
                    )
                }
        }
    }

    fun begin() {
        val state = _uiState.value
        if (state.isLoading || state.errorMessage != null || state.isTransitioningToMenu) {
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isTransitioningToMenu = true) }
            _events.emit(StartEvent.ReadyToEnterMenu)
        }
    }
}
