package fr.aumombelli.gatcha.feature.packs.selection

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fr.aumombelli.gatcha.data.CatalogGateway
import fr.aumombelli.gatcha.data.PackGateway
import fr.aumombelli.gatcha.data.ProgressGateway
import fr.aumombelli.gatcha.model.ExtensionDefinition
import fr.aumombelli.gatcha.model.OwnedCollection
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PackSelectionUiState(
    val isLoading: Boolean = true,
    val extensions: List<ExtensionDefinition> = emptyList(),
    val currentCollection: OwnedCollection = OwnedCollection(),
    val nextDrawAt: String? = null,
    val selectedExtensionId: String? = null,
    val selectedBoosterIndex: Int? = null,
    val isAwaitingPackResult: Boolean = false,
    val errorMessage: String? = null,
)

sealed interface PackEvent {
    data object PackReadyForReveal : PackEvent
}

class PackViewModel(
    private val catalogRepository: CatalogGateway,
    private val progressRepository: ProgressGateway,
    private val packRepository: PackGateway,
) : ViewModel() {
    private val _uiState = MutableStateFlow(PackSelectionUiState())
    val uiState: StateFlow<PackSelectionUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<PackEvent>()
    val events: SharedFlow<PackEvent> = _events.asSharedFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            runCatching {
                val extensions = catalogRepository.loadExtensions()
                val progress = progressRepository.loadProgress()
                Triple(extensions, progress.collection, progress.nextDrawAt)
            }.onSuccess { (extensions, collection, nextDrawAt) ->
                _uiState.value = PackSelectionUiState(
                    isLoading = false,
                    extensions = extensions,
                    currentCollection = collection,
                    nextDrawAt = nextDrawAt,
                )
            }.onFailure { exception ->
                _uiState.value = PackSelectionUiState(
                    isLoading = false,
                    errorMessage = exception.message ?: "Unable to load pack data.",
                )
            }
        }
    }

    fun selectExtension(extensionId: String) {
        _uiState.update {
            it.copy(
                selectedExtensionId = extensionId,
                selectedBoosterIndex = null,
                errorMessage = null,
            )
        }
    }

    fun clearExtensionSelection() {
        _uiState.update {
            it.copy(
                selectedExtensionId = null,
                selectedBoosterIndex = null,
                isAwaitingPackResult = false,
                errorMessage = null,
            )
        }
    }

    fun selectBooster(index: Int) {
        _uiState.update {
            it.copy(
                selectedBoosterIndex = index,
                isAwaitingPackResult = true,
                errorMessage = null,
            )
        }
    }

    fun openPack(extensionId: String) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    selectedExtensionId = extensionId,
                    isAwaitingPackResult = true,
                    errorMessage = null,
                )
            }
            runCatching {
                val response = packRepository.openPack(extensionId)
                val progress = progressRepository.loadProgress()
                response to progress
            }.onSuccess { (response, progress) ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        currentCollection = progress.collection,
                        nextDrawAt = progress.nextDrawAt,
                        isAwaitingPackResult = false,
                    )
                }
                _events.emit(PackEvent.PackReadyForReveal)
            }.onFailure { exception ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        selectedBoosterIndex = null,
                        isAwaitingPackResult = false,
                        errorMessage = exception.message ?: "Unable to open the pack.",
                    )
                }
            }
        }
    }
}
