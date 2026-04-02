package fr.aumombelli.dstcg.feature.badges

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fr.aumombelli.dstcg.data.CatalogGateway
import fr.aumombelli.dstcg.data.ProgressGateway
import fr.aumombelli.dstcg.data.requireUsableProgress
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class BadgeBookViewModel(
    private val catalogRepository: CatalogGateway,
    private val progressRepository: ProgressGateway,
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
                val progress = progressRepository.loadProgress().requireUsableProgress().progress
                buildBadgeBookSections(
                    extensions = extensions,
                    cards = cards,
                    variantProfiles = variantProfiles,
                    progress = progress,
                )
            }.onSuccess { sections ->
                _uiState.value = BadgeBookUiState(
                    isLoading = false,
                    sections = sections,
                )
            }.onFailure { exception ->
                _uiState.value = BadgeBookUiState(
                    isLoading = false,
                    errorMessage = exception.message ?: "Impossible de charger les badges.",
                )
            }
        }
    }
}
