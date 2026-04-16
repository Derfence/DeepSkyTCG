package fr.aumombelli.dstcg.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fr.aumombelli.dstcg.data.ProgressGateway
import fr.aumombelli.dstcg.data.ProgressLoadResult
import fr.aumombelli.dstcg.data.requireUsableProgress
import fr.aumombelli.dstcg.model.HomeMenuDestination
import fr.aumombelli.dstcg.model.hasUnlockedEquipmentMenu
import fr.aumombelli.dstcg.model.markHomeMenuSeen
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class HomeUiState(
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val isResettingProgress: Boolean = false,
    val isEquipmentMenuVisible: Boolean = false,
    val showLibraryNewIndicator: Boolean = false,
    val showEquipmentNewIndicator: Boolean = false,
    val showBadgeBookNewIndicator: Boolean = false,
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

    fun markLibrarySeen() {
        markMenuSeen(HomeMenuDestination.Library)
    }

    fun markEquipmentSeen() {
        markMenuSeen(HomeMenuDestination.Equipment)
    }

    fun markBadgeBookSeen() {
        markMenuSeen(HomeMenuDestination.BadgeBook)
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

    private fun markMenuSeen(destination: HomeMenuDestination) {
        val currentState = _uiState.value
        if (currentState.isLoading || currentState.isResettingProgress || !currentState.hasNewIndicator(destination)) {
            return
        }

        _uiState.value = currentState.consumeIndicator(destination)

        viewModelScope.launch {
            runCatching {
                progressRepository.updateProgress { progress ->
                    progress.markHomeMenuSeen(destination)
                }
            }.onFailure {
                refreshProgressState()
            }
        }
    }

    private fun refreshProgressState() {
        viewModelScope.launch {
            _uiState.value = HomeUiState(isLoading = true)
            runCatching { progressRepository.loadProgress() }
                .onSuccess { result ->
                    _uiState.value = when (result) {
                        is ProgressLoadResult.Ok -> result.toHomeUiState()

                        is ProgressLoadResult.Recovered -> result.toHomeUiState()

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

private fun ProgressLoadResult.Ok.toHomeUiState(): HomeUiState = HomeUiState(
    isLoading = false,
    isEquipmentMenuVisible = progress.hasUnlockedEquipmentMenu(),
    showLibraryNewIndicator = progress.homeMenuNoveltyState.library,
    showEquipmentNewIndicator = progress.homeMenuNoveltyState.equipment,
    showBadgeBookNewIndicator = progress.homeMenuNoveltyState.badgeBook,
)

private fun ProgressLoadResult.Recovered.toHomeUiState(): HomeUiState = HomeUiState(
    isLoading = false,
    isEquipmentMenuVisible = progress.hasUnlockedEquipmentMenu(),
    showLibraryNewIndicator = progress.homeMenuNoveltyState.library,
    showEquipmentNewIndicator = progress.homeMenuNoveltyState.equipment,
    showBadgeBookNewIndicator = progress.homeMenuNoveltyState.badgeBook,
)

private fun HomeUiState.hasNewIndicator(destination: HomeMenuDestination): Boolean = when (destination) {
    HomeMenuDestination.Library -> showLibraryNewIndicator
    HomeMenuDestination.Equipment -> showEquipmentNewIndicator
    HomeMenuDestination.BadgeBook -> showBadgeBookNewIndicator
}

private fun HomeUiState.consumeIndicator(destination: HomeMenuDestination): HomeUiState = when (destination) {
    HomeMenuDestination.Library -> copy(showLibraryNewIndicator = false)
    HomeMenuDestination.Equipment -> copy(showEquipmentNewIndicator = false)
    HomeMenuDestination.BadgeBook -> copy(showBadgeBookNewIndicator = false)
}
