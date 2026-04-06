package fr.aumombelli.dstcg.feature.packs.selection

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fr.aumombelli.dstcg.data.CatalogGateway
import fr.aumombelli.dstcg.data.DEFAULT_MAX_STORED_DRAWS
import fr.aumombelli.dstcg.data.DEFAULT_DRAW_COOLDOWN
import fr.aumombelli.dstcg.data.DeterministicWeatherCalendar
import fr.aumombelli.dstcg.data.LoadedProgress
import fr.aumombelli.dstcg.data.PackGateway
import fr.aumombelli.dstcg.data.ProgressGateway
import fr.aumombelli.dstcg.data.resolveActiveEquipmentBonus
import fr.aumombelli.dstcg.data.StandaloneGameSettings
import fr.aumombelli.dstcg.data.WeatherPolicy
import fr.aumombelli.dstcg.data.WeatherState
import fr.aumombelli.dstcg.data.buildPackChargeUiStatus
import fr.aumombelli.dstcg.data.drawCooldownDuration
import fr.aumombelli.dstcg.data.requireUsableProgress
import fr.aumombelli.dstcg.data.validated
import fr.aumombelli.dstcg.feature.badges.BadgeItem
import fr.aumombelli.dstcg.feature.badges.buildNewlyUnlockedBadges
import fr.aumombelli.dstcg.model.ExtensionDefinition
import fr.aumombelli.dstcg.model.GameBalanceDefinition
import fr.aumombelli.dstcg.model.OwnedCollection
import fr.aumombelli.dstcg.model.PackRechargeState
import fr.aumombelli.dstcg.model.StandaloneProgress
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
    val rechargeState: PackRechargeState = PackRechargeState(),
    val availableDrawCount: Int = DEFAULT_MAX_STORED_DRAWS,
    val maxStoredDraws: Int = DEFAULT_MAX_STORED_DRAWS,
    val drawCooldown: Duration = DEFAULT_DRAW_COOLDOWN,
    val weatherPolicy: WeatherPolicy = DeterministicWeatherCalendar,
    val currentWeather: WeatherState = WeatherState.Clear,
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
                val gameBalance = catalogRepository.loadGameBalance().validated()
                val equipmentCards = catalogRepository.loadEquipmentCards()
                val loadedProgress = progressRepository.loadProgress().requireUsableProgress()
                Quadruple(extensions, loadedProgress, gameBalance, equipmentCards)
            }.onSuccess { (extensions, loadedProgress, gameBalance, equipmentCards) ->
                _uiState.value = PackSelectionUiState(
                    isLoading = false,
                    extensions = extensions,
                    currentCollection = loadedProgress.progress.collection,
                ).withPackChargeStatus(
                    loadedProgress = loadedProgress,
                    gameBalance = gameBalance,
                    equipmentCards = equipmentCards,
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
                    packRepository.openPack(extensionId)
                    val gameBalance = catalogRepository.loadGameBalance().validated()
                    val equipmentCards = catalogRepository.loadEquipmentCards()
                    val loadedProgress = progressRepository.loadProgress().requireUsableProgress()
                    val newlyUnlockedBadges = loadNewlyUnlockedBadges(
                        beforeProgress = beforeProgress,
                        afterProgress = loadedProgress.progress,
                    )
                    Quadruple(loadedProgress, newlyUnlockedBadges, gameBalance, equipmentCards)
                }.onSuccess { (loadedProgress, newlyUnlockedBadges, gameBalance, equipmentCards) ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            currentCollection = loadedProgress.progress.collection,
                            isAwaitingPackResult = false,
                        ).withPackChargeStatus(
                            loadedProgress = loadedProgress,
                            gameBalance = gameBalance,
                            equipmentCards = equipmentCards,
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
        gameBalance: GameBalanceDefinition,
        equipmentCards: List<fr.aumombelli.dstcg.model.EquipmentCardDefinition>,
    ): PackSelectionUiState {
        val referenceEvidence = gameSettings.timeSource.now()
        val drawCooldown = gameBalance.drawCooldownDuration()
        val rechargeMultiplier = resolveActiveEquipmentBonus(
            activeEquipmentByType = loadedProgress.progress.activeEquipmentByType,
            equipmentCards = equipmentCards,
        ).rechargeMultiplier
        val chargeStatus = buildPackChargeUiStatus(
            rechargeState = loadedProgress.progress.rechargeState,
            now = loadedProgress.trustedNow,
            drawCooldown = drawCooldown,
            maxStoredDraws = gameSettings.maxStoredDraws,
            weatherPolicy = gameSettings.weatherPolicy,
            rechargeMultiplier = rechargeMultiplier,
        )
        return copy(
            rechargeState = chargeStatus.rechargeState,
            availableDrawCount = chargeStatus.availableDrawCount,
            maxStoredDraws = chargeStatus.maxStoredDraws,
            drawCooldown = drawCooldown,
            weatherPolicy = gameSettings.weatherPolicy,
            currentWeather = chargeStatus.currentWeather,
            nextChargeAt = chargeStatus.nextChargeAt,
            remainingDuration = chargeStatus.remainingDuration,
            rechargeProgress = chargeStatus.rechargeProgress,
            isDrawLocked = chargeStatus.isDrawLocked,
            trustedNow = loadedProgress.trustedNow,
            trustedElapsedRealtimeMs = referenceEvidence.elapsedRealtimeMs,
        )
    }
}

private data class Quadruple<A, B, C, D>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D,
)
