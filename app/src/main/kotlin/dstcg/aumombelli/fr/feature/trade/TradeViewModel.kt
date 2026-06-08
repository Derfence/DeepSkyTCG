package fr.aumombelli.dstcg.feature.trade

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fr.aumombelli.dstcg.data.TradeGateway
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
    val selectedCandidate: TradeCardCandidate? = null,
    val receivedCardRef: TradeCardRef? = null,
    val receivedDisplayCard: DisplayCard? = null,
    val isResolvingReceivedCard: Boolean = false,
    val phase: TradePhase = TradePhase.Ready,
    val message: String? = null,
)

enum class TradePhase {
    Ready,
    Exchanging,
    Succeeded,
    Failed,
}

class TradeViewModel(
    selectedCandidate: TradeCardCandidate,
    private val tradeRepository: TradeGateway,
) : ViewModel() {
    private val _uiState = MutableStateFlow(
        TradeUiState(
            selectedCandidate = selectedCandidate,
            message = "Préparation de l'échange NFC...",
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
                    phase = TradePhase.Ready,
                    receivedCardRef = null,
                    receivedDisplayCard = null,
                    isResolvingReceivedCard = false,
                    message = "Préparation de l'échange NFC...",
                )
            }
            runCatching {
                tradeRepository.catalogFingerprint()
            }.onSuccess { content ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        catalogFingerprint = content,
                        phase = TradePhase.Ready,
                        receivedCardRef = null,
                        receivedDisplayCard = null,
                        isResolvingReceivedCard = false,
                        message = "Rapproche les deux téléphones et garde-les immobiles.",
                    )
                }
            }.onFailure { exception ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        phase = TradePhase.Failed,
                        receivedCardRef = null,
                        receivedDisplayCard = null,
                        isResolvingReceivedCard = false,
                        message = exception.message ?: "Impossible de préparer l'échange NFC.",
                    )
                }
            }
        }
    }

    fun retryExchange() {
        _uiState.update {
            it.copy(
                phase = TradePhase.Ready,
                receivedCardRef = null,
                receivedDisplayCard = null,
                isResolvingReceivedCard = false,
                message = "Rapproche les deux téléphones et garde-les immobiles.",
            )
        }
    }

    internal fun onNfcEvent(event: NfcTradeControllerEvent) {
        if (event is NfcTradeControllerEvent.Succeeded) {
            onExchangeSucceeded(event.receivedCard)
            return
        }
        _uiState.update { state ->
            when (event) {
                NfcTradeControllerEvent.Waiting -> state.copy(
                    phase = TradePhase.Ready,
                    receivedCardRef = null,
                    receivedDisplayCard = null,
                    isResolvingReceivedCard = false,
                    message = "Rapproche les deux téléphones et garde-les immobiles.",
                )

                NfcTradeControllerEvent.Exchanging -> state.copy(
                    phase = TradePhase.Exchanging,
                    receivedCardRef = null,
                    receivedDisplayCard = null,
                    isResolvingReceivedCard = false,
                    message = "Échange en cours...",
                )

                is NfcTradeControllerEvent.Failed -> state.copy(
                    phase = TradePhase.Failed,
                    receivedCardRef = null,
                    receivedDisplayCard = null,
                    isResolvingReceivedCard = false,
                    message = event.message,
                )

                is NfcTradeControllerEvent.Succeeded -> state
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
