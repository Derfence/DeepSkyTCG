package fr.aumombelli.gatcha.feature.bootstrap

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fr.aumombelli.gatcha.data.AppCompatibilityState
import fr.aumombelli.gatcha.data.AppStatusGateway
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AppBootstrapUiState(
    val isLoading: Boolean = true,
    val isCompatible: Boolean = false,
    val message: String? = null,
    val canRetry: Boolean = false,
)

class AppBootstrapViewModel(
    private val appStatusRepository: AppStatusGateway,
) : ViewModel() {
    private val _uiState = MutableStateFlow(AppBootstrapUiState())
    val uiState: StateFlow<AppBootstrapUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            appStatusRepository.state.collect { state ->
                _uiState.value = when (state) {
                    AppCompatibilityState.Checking -> AppBootstrapUiState(isLoading = true)
                    AppCompatibilityState.Compatible -> AppBootstrapUiState(
                        isLoading = false,
                        isCompatible = true,
                    )
                    is AppCompatibilityState.Blocked -> AppBootstrapUiState(
                        isLoading = false,
                        message = state.message,
                        canRetry = state.canRetry,
                    )
                }
            }
        }
        retry()
    }

    fun retry() {
        viewModelScope.launch {
            appStatusRepository.verifyCompatibility()
        }
    }
}
