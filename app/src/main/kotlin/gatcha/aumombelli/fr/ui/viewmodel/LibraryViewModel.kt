package fr.aumombelli.gatcha.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fr.aumombelli.gatcha.data.CatalogGateway
import fr.aumombelli.gatcha.data.CollectionGateway
import fr.aumombelli.gatcha.model.LibraryCardItem
import fr.aumombelli.gatcha.model.LibrarySection
import fr.aumombelli.gatcha.model.ownedCountFor
import fr.aumombelli.gatcha.model.raritySortPriority
import fr.aumombelli.gatcha.model.toDisplayVariants
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
                val collection = collectionRepository.getCachedCollectionOrEmpty()
                val variantProfilesById = variantProfiles.associateBy { it.id }

                extensions.map { extension ->
                    LibrarySection(
                        extension = extension,
                        cards = cards
                            .filter { it.extensionId == extension.id }
                            .sortedWith(
                                compareBy(
                                    { raritySortPriority(it.rarityLabel) },
                                    { it.id },
                                ),
                            )
                            .map { card ->
                                val availableVariants = collection.cards[card.id]
                                    ?.toDisplayVariants(
                                        checkNotNull(variantProfilesById[card.variantProfileId]) {
                                            "Unknown variant profile '${card.variantProfileId}' for '${card.id}'."
                                        },
                                    )
                                    .orEmpty()
                                LibraryCardItem(
                                    definition = card,
                                    extensionName = extension.name,
                                    ownedCount = collection.ownedCountFor(card.id),
                                    availableVariants = availableVariants,
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
