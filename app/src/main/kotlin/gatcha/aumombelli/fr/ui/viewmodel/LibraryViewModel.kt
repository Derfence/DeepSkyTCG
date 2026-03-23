package gatcha.aumombelli.fr.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import gatcha.aumombelli.fr.data.CollectionRepository
import gatcha.aumombelli.fr.data.GameCatalogRepository
import gatcha.aumombelli.fr.model.LibraryCardItem
import gatcha.aumombelli.fr.model.LibrarySection
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
    private val catalogRepository: GameCatalogRepository,
    private val collectionRepository: CollectionRepository,
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
                val collection = collectionRepository.getCachedCollectionOrEmpty()

                extensions.map { extension ->
                    LibrarySection(
                        extension = extension,
                        cards = cards
                            .filter { it.extensionId == extension.id }
                            .sortedBy { it.id }
                            .map { card ->
                                LibraryCardItem(
                                    definition = card,
                                    ownedCount = collection.cards[card.id] ?: 0,
                                )
                            },
                    )
                }
            }.onSuccess { sections ->
                _uiState.value = LibraryUiState(
                    isLoading = false,
                    sections = sections,
                )
            }.onFailure { exception ->
                _uiState.value = LibraryUiState(
                    isLoading = false,
                    errorMessage = exception.message ?: "Unable to load the library.",
                )
            }
        }
    }
}
