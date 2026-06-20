package fr.aumombelli.dstcg.feature.trade

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fr.aumombelli.dstcg.data.TradeGateway
import fr.aumombelli.dstcg.data.TradeSettingsGateway
import fr.aumombelli.dstcg.data.normalizeTradeLocalName
import fr.aumombelli.dstcg.model.DisplayCard
import fr.aumombelli.dstcg.model.TradeCardCandidate
import fr.aumombelli.dstcg.model.TradeCardRef
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TradeUiState(
    val isLoading: Boolean = true,
    val catalogFingerprint: String? = null,
    val localName: String = "",
    val editableLocalName: String = "",
    val selectedCandidate: TradeCardCandidate? = null,
    val discoveredPartners: List<BluetoothTradePartner> = emptyList(),
    val selectedPartner: BluetoothTradePartner? = null,
    val remotePartnerName: String? = null,
    val remoteCardRef: TradeCardRef? = null,
    val remoteDisplayCard: DisplayCard? = null,
    val receivedCardRef: TradeCardRef? = null,
    val receivedDisplayCard: DisplayCard? = null,
    val isResolvingRemoteCard: Boolean = false,
    val isResolvingReceivedCard: Boolean = false,
    val verificationCode: String? = null,
    val localConfirmed: Boolean = false,
    val remoteConfirmed: Boolean = false,
    val phase: TradePhase = TradePhase.Preparing,
    val message: String? = null,
    val connectionCommand: TradeConnectionCommand? = null,
    val confirmationCommandId: Long = 0L,
)

data class TradeConnectionCommand(
    val id: Long,
    val partnerId: String,
)

enum class TradePhase {
    Preparing,
    Discovering,
    Connecting,
    Confirming,
    Exchanging,
    Succeeded,
    Failed,
}

class TradeViewModel(
    selectedCandidate: TradeCardCandidate,
    private val tradeRepository: TradeGateway,
    private val tradeSettingsRepository: TradeSettingsGateway,
) : ViewModel() {
    private val _uiState = MutableStateFlow(
        TradeUiState(
            selectedCandidate = selectedCandidate,
            message = "Préparation de l'échange Bluetooth...",
        ),
    )
    val uiState: StateFlow<TradeUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    phase = TradePhase.Preparing,
                    discoveredPartners = emptyList(),
                    selectedPartner = null,
                    remotePartnerName = null,
                    remoteCardRef = null,
                    remoteDisplayCard = null,
                    receivedCardRef = null,
                    receivedDisplayCard = null,
                    isResolvingRemoteCard = false,
                    isResolvingReceivedCard = false,
                    verificationCode = null,
                    localConfirmed = false,
                    remoteConfirmed = false,
                    connectionCommand = null,
                    message = "Préparation de l'échange Bluetooth...",
                )
            }
            runCatching {
                tradeRepository.catalogFingerprint() to tradeSettingsRepository.ensureLocalName()
            }.onSuccess { (catalogFingerprint, localName) ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        catalogFingerprint = catalogFingerprint,
                        localName = localName,
                        editableLocalName = localName,
                        phase = TradePhase.Discovering,
                        message = "Recherche de partenaires Bluetooth proches.",
                    )
                }
            }.onFailure { exception ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        phase = TradePhase.Failed,
                        message = exception.message ?: "Impossible de préparer l'échange Bluetooth.",
                    )
                }
            }
        }
    }

    fun retryExchange() {
        _uiState.update {
            it.copy(
                phase = TradePhase.Discovering,
                discoveredPartners = emptyList(),
                selectedPartner = null,
                remotePartnerName = null,
                remoteCardRef = null,
                remoteDisplayCard = null,
                receivedCardRef = null,
                receivedDisplayCard = null,
                isResolvingRemoteCard = false,
                isResolvingReceivedCard = false,
                verificationCode = null,
                localConfirmed = false,
                remoteConfirmed = false,
                connectionCommand = null,
                message = "Recherche de partenaires Bluetooth proches.",
            )
        }
    }

    fun onLocalNameEdited(name: String) {
        _uiState.update { it.copy(editableLocalName = name.normalizeTradeLocalName()) }
    }

    fun saveLocalName() {
        val name = _uiState.value.editableLocalName
        viewModelScope.launch {
            val savedName = tradeSettingsRepository.setLocalName(name)
            _uiState.update {
                it.copy(
                    localName = savedName,
                    editableLocalName = savedName,
                    phase = TradePhase.Discovering,
                    discoveredPartners = emptyList(),
                    message = "Nom visible mis à jour.",
                )
            }
        }
    }

    fun selectPartner(partner: BluetoothTradePartner) {
        _uiState.update { state ->
            val commandId = (state.connectionCommand?.id ?: 0L) + 1L
            state.copy(
                selectedPartner = partner,
                phase = TradePhase.Connecting,
                connectionCommand = TradeConnectionCommand(
                    id = commandId,
                    partnerId = partner.id,
                ),
                message = "Connexion à ${partner.displayName}...",
            )
        }
    }

    fun confirmExchange() {
        _uiState.update { state ->
            state.copy(
                localConfirmed = true,
                confirmationCommandId = state.confirmationCommandId + 1L,
                message = if (state.remoteConfirmed) {
                    "Confirmation reçue, échange en cours..."
                } else {
                    "Confirmation envoyée. En attente du partenaire."
                },
            )
        }
    }

    internal fun onBluetoothEvent(event: BluetoothTradeControllerEvent) {
        if (event is BluetoothTradeControllerEvent.Succeeded) {
            onExchangeSucceeded(event.receivedCard)
            return
        }
        when (event) {
            BluetoothTradeControllerEvent.Discovering -> _uiState.update {
                it.copy(
                    phase = TradePhase.Discovering,
                    message = "Recherche de partenaires Bluetooth proches.",
                )
            }

            is BluetoothTradeControllerEvent.PartnerFound -> _uiState.update { state ->
                val partners = (state.discoveredPartners.filterNot { it.id == event.partner.id } + event.partner)
                    .sortedBy { it.displayName.lowercase() }
                state.copy(
                    discoveredPartners = partners,
                    message = if (partners.isEmpty()) {
                        "Recherche de partenaires Bluetooth proches."
                    } else {
                        "Sélectionne le partenaire d'échange."
                    },
                )
            }

            is BluetoothTradeControllerEvent.PartnerLeft -> _uiState.update { state ->
                val partners = state.discoveredPartners.filterNot { partner ->
                    partner.id == event.partnerId || partner.sessionId == event.sessionId
                }
                state.copy(
                    discoveredPartners = partners,
                    selectedPartner = state.selectedPartner?.takeUnless { partner ->
                        partner.id == event.partnerId || partner.sessionId == event.sessionId
                    },
                    message = if (partners.isEmpty() && state.phase == TradePhase.Discovering) {
                        "Recherche de partenaires Bluetooth proches."
                    } else {
                        state.message
                    },
                )
            }

            is BluetoothTradeControllerEvent.Connecting -> _uiState.update {
                it.copy(
                    selectedPartner = event.partner,
                    phase = TradePhase.Connecting,
                    message = "Connexion à ${event.partner.displayName}...",
                )
            }

            is BluetoothTradeControllerEvent.RemoteOffer -> onRemoteOffer(event)

            BluetoothTradeControllerEvent.RemoteConfirmed -> _uiState.update {
                it.copy(
                    remoteConfirmed = true,
                    message = if (it.localConfirmed) {
                        "Confirmations reçues, échange en cours..."
                    } else {
                        "Le partenaire a confirmé. Vérifie la carte puis confirme."
                    },
                )
            }

            BluetoothTradeControllerEvent.Exchanging -> _uiState.update {
                it.copy(
                    phase = TradePhase.Exchanging,
                    message = "Échange Bluetooth en cours...",
                )
            }

            is BluetoothTradeControllerEvent.Failed -> _uiState.update {
                it.copy(
                    phase = TradePhase.Failed,
                    isResolvingRemoteCard = false,
                    isResolvingReceivedCard = false,
                    message = event.message,
                )
            }

            is BluetoothTradeControllerEvent.Succeeded -> Unit
        }
    }

    private fun onRemoteOffer(event: BluetoothTradeControllerEvent.RemoteOffer) {
        _uiState.update {
            it.copy(
                phase = TradePhase.Confirming,
                remotePartnerName = event.partnerName,
                remoteCardRef = event.remoteCard,
                remoteDisplayCard = null,
                isResolvingRemoteCard = true,
                verificationCode = event.verificationCode,
                message = "Vérifie la carte proposée par ${event.partnerName}.",
            )
        }
        viewModelScope.launch {
            val displayCard = runCatching {
                tradeRepository.loadTradeCard(event.remoteCard)
            }.getOrNull()
            _uiState.update { state ->
                if (state.remoteCardRef == event.remoteCard && state.phase == TradePhase.Confirming) {
                    state.copy(
                        remoteDisplayCard = displayCard,
                        isResolvingRemoteCard = false,
                    )
                } else {
                    state
                }
            }
        }
    }

    private fun onExchangeSucceeded(receivedCard: TradeCardRef) {
        _uiState.update {
            it.copy(
                phase = TradePhase.Succeeded,
                receivedCardRef = receivedCard,
                receivedDisplayCard = null,
                isResolvingReceivedCard = true,
                message = "Échange réussi !",
            )
        }
        viewModelScope.launch {
            val displayCard = runCatching {
                tradeRepository.loadTradeCard(receivedCard)
            }.getOrNull()
            _uiState.update { state ->
                if (state.phase == TradePhase.Succeeded && state.receivedCardRef == receivedCard) {
                    state.copy(
                        receivedDisplayCard = displayCard,
                        isResolvingReceivedCard = false,
                        message = if (displayCard == null) {
                            "Échange réussi. La carte reçue est dans ta bibliothèque."
                        } else {
                            "Échange réussi !"
                        },
                    )
                } else {
                    state
                }
            }
        }
    }
}

fun TradeCardCandidate.toTradeCardRef(): TradeCardRef = TradeCardRef(
    cardId = card.id,
    skyQuality = variant.skyQuality,
    finish = variant.finish,
)
