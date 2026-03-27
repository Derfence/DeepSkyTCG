package fr.aumombelli.gatcha.feature.badges

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fr.aumombelli.gatcha.data.CatalogGateway
import fr.aumombelli.gatcha.data.CollectionGateway
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class BadgeBookViewModel(
    private val catalogRepository: CatalogGateway,
    private val collectionRepository: CollectionGateway,
) : ViewModel() {
    private val _uiState = MutableStateFlow(BadgeBookUiState())
    val uiState: StateFlow<BadgeBookUiState> = _uiState.asStateFlow()

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
                buildBadgeBookSections(
                    extensions = extensions,
                    cards = cards,
                    variantProfiles = variantProfiles,
                    collection = collection,
                )
            }.onSuccess { sections ->
                _uiState.value = BadgeBookUiState(
                    isLoading = false,
                    sections = sections,
                )
            }.onFailure { exception ->
                _uiState.value = BadgeBookUiState(
                    isLoading = false,
                    errorMessage = exception.message ?: "Unable to load badges.",
                )
            }
        }
    }
}
