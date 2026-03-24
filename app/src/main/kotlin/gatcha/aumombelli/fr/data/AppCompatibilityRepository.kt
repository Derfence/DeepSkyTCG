package fr.aumombelli.gatcha.data

import fr.aumombelli.gatcha.model.CompatibilityStatuses
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AppCompatibilityController {
    private val _state = MutableStateFlow<AppCompatibilityState>(AppCompatibilityState.Checking)
    val state: StateFlow<AppCompatibilityState> = _state.asStateFlow()

    fun markChecking() {
        _state.value = AppCompatibilityState.Checking
    }

    fun markCompatible() {
        _state.value = AppCompatibilityState.Compatible
    }

    fun markBlocked(message: String, canRetry: Boolean = true) {
        _state.value = AppCompatibilityState.Blocked(message = message, canRetry = canRetry)
    }
}

class AppStatusRepository(
    private val apiService: AppStatusApi,
    private val compatibilityController: AppCompatibilityController,
) : AppStatusGateway {
    override val state: StateFlow<AppCompatibilityState> = compatibilityController.state

    override suspend fun verifyCompatibility() {
        compatibilityController.markChecking()
        runCatching { apiService.fetchAppStatus() }
            .onSuccess { response ->
                when (response.compatibilityStatus) {
                    CompatibilityStatuses.Compatible -> compatibilityController.markCompatible()
                    CompatibilityStatuses.ClientUpdateRequired,
                    CompatibilityStatuses.ServerUpdatePending,
                    -> compatibilityController.markBlocked(
                        message = response.message ?: "A compatible client/server pair is required.",
                    )
                    else -> compatibilityController.markBlocked(
                        message = "Catalog compatibility could not be determined.",
                    )
                }
            }
            .onFailure { exception ->
                compatibilityController.markBlocked(
                    message = exception.message ?: "Unable to reach the server. Retry once it becomes available.",
                )
            }
    }
}
