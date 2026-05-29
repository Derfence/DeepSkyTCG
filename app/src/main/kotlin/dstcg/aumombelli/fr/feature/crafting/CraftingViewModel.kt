package fr.aumombelli.dstcg.feature.crafting

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fr.aumombelli.dstcg.data.CraftingGateway
import fr.aumombelli.dstcg.model.CraftingApplyResult
import fr.aumombelli.dstcg.model.CraftingCardCandidate
import fr.aumombelli.dstcg.model.CraftingMode
import fr.aumombelli.dstcg.model.CraftingRecipe
import fr.aumombelli.dstcg.model.DisplayCardVariant
import fr.aumombelli.dstcg.model.raritySortPriority
import fr.aumombelli.dstcg.model.skyQualitySortPriority
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CraftingUiState(
    val isLoading: Boolean = false,
    val isApplying: Boolean = false,
    val selectedMode: CraftingMode? = null,
    val sections: List<CraftingSection> = emptyList(),
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val completion: CraftingCompletion? = null,
)

data class CraftingSection(
    val extensionId: String,
    val extensionName: String,
    val cards: List<CraftingCardGroup>,
)

data class CraftingCardGroup(
    val cardId: String,
    val cardName: String,
    val candidates: List<CraftingCardCandidate>,
) {
    val firstCandidate: CraftingCardCandidate get() = candidates.first()
    val availableVariants: List<DisplayCardVariant> get() = candidates.map { it.sourceVariant }
}

data class CraftingCompletion(
    val id: Int,
    val mode: CraftingMode,
    val recipe: CraftingRecipe,
)

sealed interface CraftingEvent {
    data class Applied(
        val mode: CraftingMode,
    ) : CraftingEvent
}

class CraftingViewModel(
    private val craftingRepository: CraftingGateway,
) : ViewModel() {
    private val _uiState = MutableStateFlow(CraftingUiState())
    val uiState: StateFlow<CraftingUiState> = _uiState.asStateFlow()
    private val _events = MutableSharedFlow<CraftingEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<CraftingEvent> = _events.asSharedFlow()
    private var completionCounter = 0

    fun selectMode(mode: CraftingMode) {
        _uiState.value = CraftingUiState(
            isLoading = true,
            selectedMode = mode,
        )
        refreshSelectedMode()
    }

    fun backToModeSelection() {
        _uiState.value = CraftingUiState()
    }

    fun refresh() {
        refreshSelectedMode()
    }

    fun applyCrafting(candidate: CraftingCardCandidate) {
        val mode = _uiState.value.selectedMode ?: return
        if (_uiState.value.isApplying) return
        _uiState.update {
            it.copy(
                isApplying = true,
                errorMessage = null,
                successMessage = null,
                completion = null,
            )
        }
        viewModelScope.launch {
            when (val result = craftingRepository.applyCrafting(mode, candidate.sourceRef)) {
                is CraftingApplyResult.Invalid -> {
                    _uiState.update {
                        it.copy(
                            isApplying = false,
                            errorMessage = result.message,
                        )
                    }
                }

                is CraftingApplyResult.Success -> {
                    val refreshedSections = runCatching {
                        craftingRepository.loadCraftingCandidates(mode).toCraftingSections()
                    }.getOrDefault(emptyList())
                    completionCounter += 1
                    _uiState.update {
                        it.copy(
                            isApplying = false,
                            sections = refreshedSections,
                            successMessage = successMessageFor(mode),
                            completion = CraftingCompletion(
                                id = completionCounter,
                                mode = mode,
                                recipe = result.recipe,
                            ),
                        )
                    }
                    _events.tryEmit(CraftingEvent.Applied(mode))
                }
            }
        }
    }

    private fun refreshSelectedMode() {
        val mode = _uiState.value.selectedMode ?: return
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    errorMessage = null,
                    successMessage = null,
                )
            }
            runCatching {
                craftingRepository.loadCraftingCandidates(mode).toCraftingSections()
            }.onSuccess { sections ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        sections = sections,
                    )
                }
            }.onFailure { exception ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        sections = emptyList(),
                        errorMessage = exception.message ?: "Impossible de charger l'artisanat.",
                    )
                }
            }
        }
    }
}

private fun List<CraftingCardCandidate>.toCraftingSections(): List<CraftingSection> =
    groupBy { it.card.extensionId to it.extensionName }
        .map { (extensionKey, extensionCandidates) ->
            CraftingSection(
                extensionId = extensionKey.first,
                extensionName = extensionKey.second,
                cards = extensionCandidates
                    .groupBy { it.card.id }
                    .map { (cardId, candidates) ->
                        CraftingCardGroup(
                            cardId = cardId,
                            cardName = candidates.first().card.name,
                            candidates = candidates.sortedWith(
                                compareByDescending<CraftingCardCandidate> {
                                    skyQualitySortPriority(it.sourceVariant.skyQuality)
                                }.thenBy { it.sourceVariant.finishLabel },
                            ),
                        )
                    }
                    .sortedWith(
                        compareBy<CraftingCardGroup> { group ->
                            raritySortPriority(group.firstCandidate.card.rarityLabel)
                        }.thenBy { it.cardName },
                    ),
            )
        }
        .sortedBy { it.extensionId }

private fun successMessageFor(mode: CraftingMode): String = when (mode) {
    CraftingMode.DarkenSky -> "Le ciel de la carte a été assombri."
    CraftingMode.SpaceAgency -> "La carte a été tamponnée."
}
