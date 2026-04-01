package fr.aumombelli.gatcha.feature.packs.selection

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fr.aumombelli.gatcha.data.CatalogGateway
import fr.aumombelli.gatcha.data.DEFAULT_MAX_STORED_DRAWS
import fr.aumombelli.gatcha.data.LoadedProgress
import fr.aumombelli.gatcha.data.PackGateway
import fr.aumombelli.gatcha.data.ProgressGateway
import fr.aumombelli.gatcha.data.StandaloneGameSettings
import fr.aumombelli.gatcha.data.buildPackChargeUiStatus
import fr.aumombelli.gatcha.data.requireUsableProgress
import fr.aumombelli.gatcha.feature.badges.BadgeItem
import fr.aumombelli.gatcha.feature.badges.buildNewlyUnlockedBadges
import fr.aumombelli.gatcha.model.ExtensionDefinition
import fr.aumombelli.gatcha.model.OwnedCollection
import fr.aumombelli.gatcha.model.StandaloneProgress
import java.time.Duration
import java.time.Instant
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
    val availableDrawCount: Int = DEFAULT_MAX_STORED_DRAWS,
    val maxStoredDraws: Int = DEFAULT_MAX_STORED_DRAWS,
    val nextChargeAt: String? = null,
    val remainingDuration: Duration? = null,
    val rechargeProgress: Float = 1f,
    val isDrawLocked: Boolean = false,
    val trustedNow: Instant = Instant.EPOCH,
    val trustedElapsedRealtimeMs: Long = 0L,
    val selectedExtensionId: String? = null,
    val selectedBoosterIndex: Int? = null,
    val isAwaitingPackResult: Boolean = false,
    val errorMessage: String? = null,
)

sealed interface PackEvent {
    data class PackReadyForReveal(
        val newlyUnlockedBadges: List<BadgeItem> = emptyList(),
    ) : PackEvent
}

class PackViewModel(
    private val catalogRepository: CatalogGateway,
    private val progressRepository: ProgressGateway,
    private val packRepository: PackGateway,
    private val gameSettings: StandaloneGameSettings = StandaloneGameSettings(),
) : ViewModel() {
    private val _uiState = MutableStateFlow(PackSelectionUiState())
    val uiState: StateFlow<PackSelectionUiState> = _uiState.asStateFlow()
    private var isPackRequestInFlight: Boolean = false

    private val _events = MutableSharedFlow<PackEvent>()
    val events: SharedFlow<PackEvent> = _events.asSharedFlow()

    init {
        refresh()
    }

    fun refresh() {
        _uiState.update {
            it.copy(
                isLoading = true,
                selectedExtensionId = null,
                selectedBoosterIndex = null,
                isAwaitingPackResult = false,
                errorMessage = null,
            )
        }
        viewModelScope.launch {
            runCatching {
                val extensions = catalogRepository.loadExtensions()
                val loadedProgress = progressRepository.loadProgress().requireUsableProgress()
                extensions to loadedProgress
            }.onSuccess { (extensions, loadedProgress) ->
                _uiState.value = PackSelectionUiState(
                    isLoading = false,
                    extensions = extensions,
                    currentCollection = loadedProgress.progress.collection,
                ).withPackChargeStatus(
                    loadedProgress = loadedProgress,
                )
            }.onFailure { exception ->
                _uiState.value = PackSelectionUiState(
                    isLoading = false,
                    errorMessage = exception.message ?: "Impossible de charger les donnees des packs.",
                )
            }
        }
    }

    fun selectExtension(extensionId: String) {
        _uiState.update {
            it.copy(
                selectedExtensionId = extensionId,
                selectedBoosterIndex = null,
                errorMessage = null,
            )
        }
    }

    fun clearExtensionSelection() {
        _uiState.update {
            it.copy(
                selectedExtensionId = null,
                selectedBoosterIndex = null,
                isAwaitingPackResult = false,
                errorMessage = null,
            )
        }
    }

    fun selectBooster(index: Int) {
        _uiState.update {
            it.copy(
                selectedBoosterIndex = index,
                isAwaitingPackResult = true,
                errorMessage = null,
            )
        }
    }

    fun openPack(extensionId: String) {
        if (isPackRequestInFlight) {
            return
        }

        isPackRequestInFlight = true
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    selectedExtensionId = extensionId,
                    isAwaitingPackResult = true,
                    errorMessage = null,
                )
            }
            try {
                runCatching {
                    val beforeProgress = progressRepository.loadProgress().requireUsableProgress().progress
                    val response = packRepository.openPack(extensionId)
                    val loadedProgress = progressRepository.loadProgress().requireUsableProgress()
                    val newlyUnlockedBadges = loadNewlyUnlockedBadges(
                        beforeProgress = beforeProgress,
                        afterProgress = loadedProgress.progress,
                    )
                    Triple(response, loadedProgress, newlyUnlockedBadges)
                }.onSuccess { (_, loadedProgress, newlyUnlockedBadges) ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            currentCollection = loadedProgress.progress.collection,
                            isAwaitingPackResult = false,
                        ).withPackChargeStatus(
                            loadedProgress = loadedProgress,
                        )
                    }
                    _events.emit(PackEvent.PackReadyForReveal(newlyUnlockedBadges = newlyUnlockedBadges))
                }.onFailure { exception ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            selectedBoosterIndex = null,
                            isAwaitingPackResult = false,
                            errorMessage = exception.message ?: "Impossible d'ouvrir le pack.",
                        )
                    }
                }
            } finally {
                isPackRequestInFlight = false
            }
        }
    }

    private suspend fun loadNewlyUnlockedBadges(
        beforeProgress: StandaloneProgress,
        afterProgress: StandaloneProgress,
    ): List<BadgeItem> = runCatching {
        buildNewlyUnlockedBadges(
            extensions = catalogRepository.loadExtensions(),
            cards = catalogRepository.loadCards(),
            variantProfiles = catalogRepository.loadVariantProfiles(),
            beforeProgress = beforeProgress,
            afterProgress = afterProgress,
        )
    }.getOrElse { emptyList() }

    private fun PackSelectionUiState.withPackChargeStatus(
        loadedProgress: LoadedProgress,
    ): PackSelectionUiState {
        val referenceEvidence = gameSettings.timeSource.now()
        val chargeStatus = buildPackChargeUiStatus(
            availableDrawCount = loadedProgress.progress.availableDrawCount,
            nextChargeAt = loadedProgress.progress.nextChargeAt,
            now = loadedProgress.trustedNow,
            drawCooldown = gameSettings.drawCooldown,
            maxStoredDraws = gameSettings.maxStoredDraws,
        )
        return copy(
            availableDrawCount = chargeStatus.availableDrawCount,
            maxStoredDraws = chargeStatus.maxStoredDraws,
            nextChargeAt = chargeStatus.nextChargeAt,
            remainingDuration = chargeStatus.remainingDuration,
            rechargeProgress = chargeStatus.rechargeProgress,
            isDrawLocked = chargeStatus.isDrawLocked,
            trustedNow = loadedProgress.trustedNow,
            trustedElapsedRealtimeMs = referenceEvidence.elapsedRealtimeMs,
        )
    }
}
