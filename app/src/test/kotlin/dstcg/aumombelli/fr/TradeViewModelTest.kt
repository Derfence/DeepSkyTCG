package fr.aumombelli.dstcg

import fr.aumombelli.dstcg.data.TradeGateway
import fr.aumombelli.dstcg.feature.trade.NfcTradeControllerEvent
import fr.aumombelli.dstcg.feature.trade.TradePhase
import fr.aumombelli.dstcg.feature.trade.TradeViewModel
import fr.aumombelli.dstcg.model.DisplayCardVariant
import fr.aumombelli.dstcg.model.TradeCardCandidate
import fr.aumombelli.dstcg.model.TradeCardRef
import fr.aumombelli.dstcg.model.TradeValidationResult
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TradeViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `refresh prepares direct NFC exchange with selected candidate`() = runTest {
        val selectedCandidate = testTradeCandidate()
        val viewModel = TradeViewModel(
            selectedCandidate = selectedCandidate,
            tradeRepository = FakeTradeGateway(catalogFingerprint = "catalog-1"),
        )

        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(false, state.isLoading)
        assertEquals(selectedCandidate, state.selectedCandidate)
        assertEquals("catalog-1", state.catalogFingerprint)
        assertEquals(TradePhase.Ready, state.phase)
    }

    @Test
    fun `retry keeps selected candidate and restarts ready phase`() = runTest {
        val selectedCandidate = testTradeCandidate()
        val viewModel = TradeViewModel(
            selectedCandidate = selectedCandidate,
            tradeRepository = FakeTradeGateway(catalogFingerprint = "catalog-1"),
        )
        advanceUntilIdle()

        viewModel.onNfcEvent(NfcTradeControllerEvent.Failed("Tap trop court."))
        viewModel.retryExchange()

        val state = viewModel.uiState.value
        assertEquals(selectedCandidate, state.selectedCandidate)
        assertEquals(TradePhase.Ready, state.phase)
        assertEquals("Rapproche les deux telephones et garde-les immobiles.", state.message)
    }

    private fun testTradeCandidate(): TradeCardCandidate = TradeCardCandidate(
        card = testCardDefinition("ALP-001", rarityLabel = "Common"),
        extensionName = "Astronomes en herbe",
        variant = DisplayCardVariant(
            skyQuality = "city",
            skyQualityLabel = "Ville",
            finish = "standard",
            finishLabel = "Standard",
            isHolographic = false,
            count = 2,
        ),
    )

    private class FakeTradeGateway(
        private val catalogFingerprint: String,
    ) : TradeGateway {
        override suspend fun loadTradeCandidates(): List<TradeCardCandidate> = emptyList()

        override suspend fun catalogFingerprint(): String = catalogFingerprint

        override suspend fun validateTrade(
            localOutgoing: TradeCardRef,
            remoteOutgoing: TradeCardRef,
        ): TradeValidationResult = TradeValidationResult.Valid

        override suspend fun applyTrade(
            tradeId: String,
            outgoing: TradeCardRef,
            incoming: TradeCardRef,
        ): TradeValidationResult = TradeValidationResult.Valid
    }
}
