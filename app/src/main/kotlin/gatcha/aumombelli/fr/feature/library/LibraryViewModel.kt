package fr.aumombelli.gatcha.feature.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fr.aumombelli.gatcha.data.CatalogGateway
import fr.aumombelli.gatcha.data.CollectionGateway
import fr.aumombelli.gatcha.model.LibrarySection
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LibraryUiState(
    val isLoading: Boolean = true,
    val sections: List<LibrarySection> = emptyList(),
    val errorMessage: String? = null,
)

class LibraryViewModel(
    private val catalogRepository: CatalogGateway,
    private val collectionRepository: CollectionGateway,
) : ViewModel() {
    private val _uiState = MutableStateFlow(LibraryUiState())
    val uiState: StateFlow<LibraryUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            runCatching {
                val extensions = catalogRepository.loadExtensions()
                val cards = catalogRepository.loadCards()
                val variantProfiles = catalogRepository.loadVariantProfiles()
                val collection = collectionRepository.loadCollection()
                buildLibrarySections(
                    extensions = extensions,
                    cards = cards,
                    variantProfiles = variantProfiles,
                    collection = collection,
                )
            }.onSuccess { sections ->
                _uiState.value = LibraryUiState(
                    isLoading = false,
                    sections = sections,
                )
            }.onFailure { exception ->
                _uiState.value = LibraryUiState(
                    isLoading = false,
                    errorMessage = exception.message ?: "Impossible de charger la bibliothèque.",
                )
            }
        }
    }
}
