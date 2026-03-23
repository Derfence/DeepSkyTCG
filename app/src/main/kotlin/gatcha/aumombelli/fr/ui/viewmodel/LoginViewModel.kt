package gatcha.aumombelli.fr.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import gatcha.aumombelli.fr.data.AuthGateway
import gatcha.aumombelli.fr.data.CollectionGateway
import gatcha.aumombelli.fr.data.SecurityUtils
import gatcha.aumombelli.fr.data.SessionGateway
import gatcha.aumombelli.fr.model.CreateAccountRequest
import gatcha.aumombelli.fr.model.LoginRequest
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
    val errorMessage: String? = null,
)

sealed interface LoginEvent {
    data object NavigateToMenu : LoginEvent
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
        _uiState.update { it.copy(username = value, errorMessage = null) }
    }

    fun updateEmail(value: String) {
        _uiState.update { it.copy(email = value, errorMessage = null) }
    }

    fun updatePassword(value: String) {
        _uiState.update { it.copy(password = value, errorMessage = null) }
    }

    fun toggleMode() {
        _uiState.update { it.copy(isCreateMode = !it.isCreateMode, errorMessage = null) }
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
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

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
                _events.emit(LoginEvent.NavigateToMenu)
            }.onFailure { exception ->
                _uiState.update { current ->
                    current.copy(errorMessage = exception.message ?: "Unexpected login error.")
                }
            }

            _uiState.update { it.copy(isLoading = false) }
        }
    }
}
