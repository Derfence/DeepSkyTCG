package fr.aumombelli.gatcha.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fr.aumombelli.gatcha.data.CatalogGateway
import fr.aumombelli.gatcha.data.CollectionGateway
import fr.aumombelli.gatcha.data.PackGateway
import fr.aumombelli.gatcha.data.PendingSaveException
import fr.aumombelli.gatcha.data.SessionGateway
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
    val errorMessage: String? = null,
)

sealed interface PackEvent {
    data object NavigateToOpening : PackEvent
}

class PackViewModel(
    private val catalogRepository: CatalogGateway,
    private val collectionRepository: CollectionGateway,
    private val packRepository: PackGateway,
    private val sessionRepository: SessionGateway,
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
                val collection = collectionRepository.getCachedCollectionOrEmpty()
                val snapshot = sessionRepository.readSnapshot()
                Triple(extensions, collection, snapshot.nextDrawAt)
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

    fun openPack(extensionId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            runCatching {
                val response = packRepository.openPack(extensionId, _uiState.value.currentCollection)
                val merged = collectionRepository.mergeCards(_uiState.value.currentCollection, response.cards)
                response to merged
            }.onSuccess { (response, merged) ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        currentCollection = merged,
                        nextDrawAt = response.nextDrawAt,
                    )
                }
                _events.emit(PackEvent.NavigateToOpening)
            }.onFailure { exception ->
                val message = when (exception) {
                    is PendingSaveException -> exception.message
                    else -> exception.message ?: "Unable to open the pack."
                }
                _uiState.update { it.copy(isLoading = false, errorMessage = message) }
            }
        }
    }
}
