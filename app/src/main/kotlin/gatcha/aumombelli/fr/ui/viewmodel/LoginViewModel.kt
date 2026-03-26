package fr.aumombelli.gatcha.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fr.aumombelli.gatcha.data.AuthGateway
import fr.aumombelli.gatcha.data.CollectionGateway
import fr.aumombelli.gatcha.data.SecurityUtils
import fr.aumombelli.gatcha.data.SessionGateway
import fr.aumombelli.gatcha.model.CreateAccountRequest
import fr.aumombelli.gatcha.model.LoginRequest
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LoginUiState(
    val username: String = "",
    val email: String = "",
    val password: String = "",
    val isCreateMode: Boolean = false,
    val isLoading: Boolean = false,
    val isTransitioningToMenu: Boolean = false,
    val errorMessage: String? = null,
)

sealed interface LoginEvent {
    data object AuthenticationSucceeded : LoginEvent
}

class LoginViewModel(
    private val apiService: AuthGateway,
    private val sessionRepository: SessionGateway,
    private val collectionRepository: CollectionGateway,
) : ViewModel() {
    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<LoginEvent>()
    val events: SharedFlow<LoginEvent> = _events.asSharedFlow()

    init {
        viewModelScope.launch {
            val snapshot = sessionRepository.readSnapshot()
            _uiState.update { state ->
                state.copy(username = snapshot.lastUsername.orEmpty())
            }
        }
    }

    fun updateUsername(value: String) {
        _uiState.update { it.copy(username = value, errorMessage = null, isTransitioningToMenu = false) }
    }

    fun updateEmail(value: String) {
        _uiState.update { it.copy(email = value, errorMessage = null, isTransitioningToMenu = false) }
    }

    fun updatePassword(value: String) {
        _uiState.update { it.copy(password = value, errorMessage = null, isTransitioningToMenu = false) }
    }

    fun toggleMode() {
        _uiState.update {
            it.copy(
                isCreateMode = !it.isCreateMode,
                errorMessage = null,
                isTransitioningToMenu = false,
            )
        }
    }

    fun submit() {
        val state = _uiState.value
        if (state.username.isBlank() || state.password.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Username and password are required.") }
            return
        }
        if (state.isCreateMode && state.email.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Email is required to create an account.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null, isTransitioningToMenu = false) }

            val normalizedUsername = SecurityUtils.normalizeUsername(state.username)
            val passwordHash = SecurityUtils.computeClientPasswordHash(normalizedUsername, state.password)

            runCatching {
                if (state.isCreateMode) {
                    apiService.createAccount(
                        CreateAccountRequest(
                            username = normalizedUsername,
                            email = state.email.trim(),
                            passwordHash = passwordHash,
                        ),
                    )
                }

                val loginResponse = apiService.login(
                    LoginRequest(
                        username = normalizedUsername,
                        passwordHash = passwordHash,
                    ),
                )

                sessionRepository.setActiveSession(loginResponse.username, passwordHash)
                sessionRepository.saveLoginMetadata(
                    username = loginResponse.username,
                    lastSavedAt = loginResponse.lastSavedAt,
                    nextDrawAt = loginResponse.nextDrawAt,
                )
                collectionRepository.replayPendingSaveIfNeeded()
                collectionRepository.loadCollectionFromServer()
            }.onSuccess {
                _uiState.update { it.copy(isLoading = false, isTransitioningToMenu = true) }
                _events.emit(LoginEvent.AuthenticationSucceeded)
            }.onFailure { exception ->
                _uiState.update { current ->
                    current.copy(
                        isLoading = false,
                        isTransitioningToMenu = false,
                        errorMessage = exception.message ?: "Unexpected login error.",
                    )
                }
            }
        }
    }
}
