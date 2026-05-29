package fr.aumombelli.dstcg.feature.trade

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fr.aumombelli.dstcg.data.TradeGateway
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
            message = "Preparation de l'echange NFC...",
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
                    message = "Preparation de l'echange NFC...",
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
                        message = "Rapproche les deux telephones et garde-les immobiles.",
                    )
                }
            }.onFailure { exception ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        phase = TradePhase.Failed,
                        message = exception.message ?: "Impossible de preparer l'echange NFC.",
                    )
                }
            }
        }
    }

    fun retryExchange() {
        _uiState.update {
            it.copy(
                phase = TradePhase.Ready,
                message = "Rapproche les deux telephones et garde-les immobiles.",
            )
        }
    }

    internal fun onNfcEvent(event: NfcTradeControllerEvent) {
        _uiState.update { state ->
            when (event) {
                NfcTradeControllerEvent.Waiting -> state.copy(
                    phase = TradePhase.Ready,
                    message = "Rapproche les deux telephones et garde-les immobiles.",
                )

                NfcTradeControllerEvent.Exchanging -> state.copy(
                    phase = TradePhase.Exchanging,
                    message = "Echange en cours...",
                )

                NfcTradeControllerEvent.Succeeded -> state.copy(
                    phase = TradePhase.Succeeded,
                    message = "Echange reussi.",
                )

                is NfcTradeControllerEvent.Failed -> state.copy(
                    phase = TradePhase.Failed,
                    message = event.message,
                )
            }
        }
    }
}

fun TradeCardCandidate.toTradeCardRef(): TradeCardRef = TradeCardRef(
    cardId = card.id,
    skyQuality = variant.skyQuality,
    finish = variant.finish,
)
