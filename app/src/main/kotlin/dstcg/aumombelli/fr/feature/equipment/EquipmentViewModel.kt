package fr.aumombelli.dstcg.feature.equipment

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fr.aumombelli.dstcg.data.CatalogGateway
import fr.aumombelli.dstcg.data.EquipmentGateway
import fr.aumombelli.dstcg.model.EquipmentCardDefinition
import fr.aumombelli.dstcg.model.EquipmentState
import fr.aumombelli.dstcg.model.EquipmentType
import fr.aumombelli.dstcg.model.bonusLabel
import fr.aumombelli.dstcg.model.entryFor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class EquipmentActiveSummaryItemUi(
    val type: EquipmentType,
    val displayName: String,
    val bonusLabel: String,
    val packsRemaining: Int,
)

data class EquipmentInventoryCardUi(
    val definition: EquipmentCardDefinition,
    val stockCount: Int,
    val activationCount: Int,
    val isActive: Boolean,
    val packsRemaining: Int? = null,
    val activationEnabled: Boolean = false,
)

data class EquipmentSectionUi(
    val type: EquipmentType,
    val title: String,
    val lastActivatedLabel: String? = null,
    val cards: List<EquipmentInventoryCardUi> = emptyList(),
)

data class EquipmentUiState(
    val isLoading: Boolean = true,
    val activeEffects: List<EquipmentActiveSummaryItemUi> = emptyList(),
    val sections: List<EquipmentSectionUi> = emptyList(),
    val activatingCardId: String? = null,
    val errorMessage: String? = null,
)

class EquipmentViewModel(
    private val catalogRepository: CatalogGateway,
    private val equipmentRepository: EquipmentGateway,
) : ViewModel() {
    private val _uiState = MutableStateFlow(EquipmentUiState())
    val uiState: StateFlow<EquipmentUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        _uiState.update {
            it.copy(
                isLoading = true,
                activatingCardId = null,
                errorMessage = null,
            )
        }
        viewModelScope.launch {
            runCatching {
                val equipmentCards = catalogRepository.loadEquipmentCards()
                val state = equipmentRepository.loadEquipmentState()
                buildEquipmentUiState(
                    equipmentCards = equipmentCards,
                    equipmentState = state,
                )
            }.onSuccess { state ->
                _uiState.value = state
            }.onFailure { error ->
                _uiState.value = EquipmentUiState(
                    isLoading = false,
                    errorMessage = error.message ?: "Impossible de charger les equipements.",
                )
            }
        }
    }

    fun activateEquipment(equipmentCardId: String) {
        val currentState = _uiState.value
        if (currentState.isLoading || currentState.activatingCardId != null) {
            return
        }
        _uiState.update {
            it.copy(
                activatingCardId = equipmentCardId,
                errorMessage = null,
            )
        }
        viewModelScope.launch {
            runCatching {
                val equipmentCards = catalogRepository.loadEquipmentCards()
                val state = equipmentRepository.activateEquipment(equipmentCardId)
                buildEquipmentUiState(
                    equipmentCards = equipmentCards,
                    equipmentState = state,
                )
            }.onSuccess { updatedState ->
                _uiState.value = updatedState
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        activatingCardId = null,
                        errorMessage = error.message ?: "Impossible d'activer cet equipement.",
                    )
                }
            }
        }
    }
}

private fun buildEquipmentUiState(
    equipmentCards: List<EquipmentCardDefinition>,
    equipmentState: EquipmentState,
): EquipmentUiState {
    val cardsById = equipmentCards.associateBy { it.id }
    val activeEffects = EquipmentType.entries.mapNotNull { type ->
        val effect = equipmentState.activeEquipmentByType[type] ?: return@mapNotNull null
        val definition = cardsById[effect.equipmentCardId] ?: return@mapNotNull null
        EquipmentActiveSummaryItemUi(
            type = type,
            displayName = definition.displayName,
            bonusLabel = definition.bonusLabel(),
            packsRemaining = effect.packsRemaining,
        )
    }
    val sections = EquipmentType.entries.map { type ->
        buildEquipmentSection(
            type = type,
            equipmentCards = equipmentCards.filter { it.type == type }.sortedBy { it.level },
            equipmentState = equipmentState,
        )
    }
    return EquipmentUiState(
        isLoading = false,
        activeEffects = activeEffects,
        sections = sections,
    )
}

private fun buildEquipmentSection(
    type: EquipmentType,
    equipmentCards: List<EquipmentCardDefinition>,
    equipmentState: EquipmentState,
): EquipmentSectionUi {
    val activeEffect = equipmentState.activeEquipmentByType[type]
    val lastActivatedLabel = equipmentState.lastActivatedCardIdByType[type]
        ?.let { cardId -> equipmentCards.firstOrNull { it.id == cardId }?.displayName }
    return EquipmentSectionUi(
        type = type,
        title = type.displayName,
        lastActivatedLabel = lastActivatedLabel,
        cards = equipmentCards.map { definition ->
            val entry = equipmentState.inventory.entryFor(definition.id)
            EquipmentInventoryCardUi(
                definition = definition,
                stockCount = entry.countOwned,
                activationCount = entry.activationCount,
                isActive = activeEffect?.equipmentCardId == definition.id,
                packsRemaining = activeEffect?.takeIf { it.equipmentCardId == definition.id }?.packsRemaining,
                activationEnabled = entry.countOwned > 0 && activeEffect == null,
            )
        },
    )
}
