package fr.aumombelli.dstcg.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fr.aumombelli.dstcg.data.ProgressGateway
import fr.aumombelli.dstcg.data.ProgressLoadResult
import fr.aumombelli.dstcg.model.hasUnlockedEquipmentMenu
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class HomeUiState(
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val isResettingProgress: Boolean = false,
    val isEquipmentMenuVisible: Boolean = false,
)

class HomeViewModel(
    private val progressRepository: ProgressGateway,
) : ViewModel() {
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        refreshProgressState()
    }

    fun refresh() {
        refreshProgressState()
    }

    fun resetProgress() {
        val state = _uiState.value
        if (state.isLoading || state.isResettingProgress) {
            return
        }

        viewModelScope.launch {
            _uiState.value = state.copy(isResettingProgress = true)
            runCatching { progressRepository.resetProgress() }
                .onSuccess {
                    refreshProgressState()
                }
                .onFailure { exception ->
                    _uiState.value = HomeUiState(
                        isLoading = false,
                        errorMessage = exception.message ?: "Impossible de réinitialiser la progression locale.",
                    )
                }
        }
    }

    private fun refreshProgressState() {
        viewModelScope.launch {
            _uiState.value = HomeUiState(isLoading = true)
            runCatching { progressRepository.loadProgress() }
                .onSuccess { result ->
                    _uiState.value = when (result) {
                        is ProgressLoadResult.Ok -> HomeUiState(
                            isLoading = false,
                            isEquipmentMenuVisible = result.progress.hasUnlockedEquipmentMenu(),
                        )

                        is ProgressLoadResult.Recovered -> HomeUiState(
                            isLoading = false,
                            isEquipmentMenuVisible = result.progress.hasUnlockedEquipmentMenu(),
                        )

                        is ProgressLoadResult.Compromised -> HomeUiState(
                            isLoading = false,
                            errorMessage = result.message,
                        )
                    }
                }
                .onFailure { exception ->
                    _uiState.value = HomeUiState(
                        isLoading = false,
                        errorMessage = exception.message ?: "Impossible de charger la progression locale.",
                    )
                }
        }
    }
}
