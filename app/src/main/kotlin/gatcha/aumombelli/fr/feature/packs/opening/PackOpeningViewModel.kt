package fr.aumombelli.gatcha.feature.packs.opening

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fr.aumombelli.gatcha.data.CatalogGateway
import fr.aumombelli.gatcha.data.PackGateway
import fr.aumombelli.gatcha.model.DisplayCard
import fr.aumombelli.gatcha.model.DrawPackResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class PackOpeningUiState(
    val packResult: DrawPackResponse? = null,
    val displayCards: List<DisplayCard> = emptyList(),
    val highestBurstRarity: String? = null,
    val hasHolographicBurst: Boolean = false,
    val errorMessage: String? = null,
)

class PackOpeningViewModel(
    private val catalogRepository: CatalogGateway,
    packRepository: PackGateway,
) : ViewModel() {
    private val _uiState = MutableStateFlow(
        PackOpeningUiState(
            packResult = packRepository.currentPackResult().value,
        ),
    )
    val uiState: StateFlow<PackOpeningUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            packRepository.currentPackResult().collect { packResult ->
                if (packResult == null) {
                    _uiState.value = PackOpeningUiState()
                } else {
                    _uiState.value = runCatching { buildPackOpeningUiState(catalogRepository, packResult) }
                        .getOrElse { exception ->
                            PackOpeningUiState(
                                packResult = packResult,
                                errorMessage = exception.message ?: "Unable to prepare the drawn cards.",
                            )
                        }
                }
            }
        }
    }
}
