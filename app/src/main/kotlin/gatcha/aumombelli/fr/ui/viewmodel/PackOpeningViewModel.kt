package fr.aumombelli.gatcha.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fr.aumombelli.gatcha.data.CatalogGateway
import fr.aumombelli.gatcha.data.PackGateway
import fr.aumombelli.gatcha.model.DisplayCard
import fr.aumombelli.gatcha.model.DrawPackResponse
import fr.aumombelli.gatcha.model.toDisplayCard
import fr.aumombelli.gatcha.model.toDisplayVariant
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class PackOpeningUiState(
    val packResult: DrawPackResponse? = null,
    val displayCards: List<DisplayCard> = emptyList(),
    val errorMessage: String? = null,
)

class PackOpeningViewModel(
    private val catalogRepository: CatalogGateway,
    packRepository: PackGateway,
) : ViewModel() {
    private val _uiState = MutableStateFlow(PackOpeningUiState())
    val uiState: StateFlow<PackOpeningUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            packRepository.currentPackResult().collect { packResult ->
                if (packResult == null) {
                    _uiState.value = PackOpeningUiState()
                } else {
                    _uiState.value = runCatching { buildUiState(packResult) }
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

    private suspend fun buildUiState(packResult: DrawPackResponse): PackOpeningUiState {
        val cardsById = catalogRepository.loadCards().associateBy { it.id }
        val extensionName = catalogRepository.loadExtensions()
            .firstOrNull { it.id == packResult.extensionId }
            ?.name
            ?: packResult.extensionId

        val displayCards = packResult.cards.map { card ->
            val definition = checkNotNull(cardsById[card.cardId]) {
                "Unknown card '${card.cardId}' for the current catalog."
            }
            definition.toDisplayCard(
                extensionName = extensionName,
                activeVariant = card.variant.toDisplayVariant(),
            )
        }

        return PackOpeningUiState(
            packResult = packResult,
            displayCards = displayCards,
        )
    }
}
