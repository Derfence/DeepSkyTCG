package fr.aumombelli.dstcg

import fr.aumombelli.dstcg.data.TradeGateway
import fr.aumombelli.dstcg.data.TradeSettings
import fr.aumombelli.dstcg.data.TradeSettingsGateway
import fr.aumombelli.dstcg.feature.trade.BluetoothTradeControllerEvent
import fr.aumombelli.dstcg.feature.trade.BluetoothTradePartner
import fr.aumombelli.dstcg.feature.trade.TradePhase
import fr.aumombelli.dstcg.feature.trade.TradeViewModel
import fr.aumombelli.dstcg.model.DisplayCard
import fr.aumombelli.dstcg.model.DisplayCardVariant
import fr.aumombelli.dstcg.model.TradeCardCandidate
import fr.aumombelli.dstcg.model.TradeCardRef
import fr.aumombelli.dstcg.model.TradeValidationResult
import fr.aumombelli.dstcg.model.toDisplayCard
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
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
    fun `refresh prepares Bluetooth exchange with selected candidate and local name`() = runTest {
        val selectedCandidate = testTradeCandidate()
        val viewModel = testViewModel(selectedCandidate = selectedCandidate)

        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(false, state.isLoading)
        assertEquals(selectedCandidate, state.selectedCandidate)
        assertEquals("catalog-1", state.catalogFingerprint)
        assertEquals("Observatoire test", state.localName)
        assertEquals(TradePhase.Discovering, state.phase)
    }

    @Test
    fun `partner discovery updates list and selection creates connection command`() = runTest {
        val viewModel = testViewModel()
        advanceUntilIdle()
        val partner = BluetoothTradePartner(
            id = "peer-1",
            displayName = "Observatoire ami",
            sessionId = "peer-session",
            isCompatible = true,
        )

        viewModel.onBluetoothEvent(BluetoothTradeControllerEvent.PartnerFound(partner))
        viewModel.selectPartner(partner)

        val state = viewModel.uiState.value
        assertEquals(listOf(partner), state.discoveredPartners)
        assertEquals(partner, state.selectedPartner)
        assertEquals(TradePhase.Connecting, state.phase)
        assertEquals("peer-1", state.connectionCommand?.partnerId)
    }

    @Test
    fun `partner leaving removes matching partner from discovery list`() = runTest {
        val viewModel = testViewModel()
        advanceUntilIdle()
        val leavingPartner = BluetoothTradePartner(
            id = "peer-1",
            displayName = "Observatoire ami",
            sessionId = "peer-session",
            isCompatible = true,
        )
        val remainingPartner = BluetoothTradePartner(
            id = "peer-2",
            displayName = "Observatoire voisin",
            sessionId = "other-session",
            isCompatible = true,
        )

        viewModel.onBluetoothEvent(BluetoothTradeControllerEvent.PartnerFound(leavingPartner))
        viewModel.onBluetoothEvent(BluetoothTradeControllerEvent.PartnerFound(remainingPartner))
        viewModel.onBluetoothEvent(
            BluetoothTradeControllerEvent.PartnerLeft(
                partnerId = "rotated-address",
                sessionId = "peer-session",
            ),
        )

        val state = viewModel.uiState.value
        assertEquals(listOf(remainingPartner), state.discoveredPartners)
        assertEquals("Sélectionne le partenaire d'échange.", state.message)
    }

    @Test
    fun `remote offer resolves received proposal before confirmation`() = runTest {
        val remoteRef = TradeCardRef("ALP-002", "city", "standard")
        val remoteDisplayCard = testCardDefinition("ALP-002", name = "Galaxie d'Andromède")
            .toDisplayCard(
                extensionName = "Astronomes en herbe",
                activeVariant = testTradeCandidate().variant.copy(count = 1),
                availableVariants = listOf(testTradeCandidate().variant.copy(count = 1)),
            )
        val viewModel = testViewModel(
            tradeGateway = FakeTradeGateway(
                catalogFingerprint = "catalog-1",
                displayCardsByRef = mapOf(remoteRef to remoteDisplayCard),
            ),
        )
        advanceUntilIdle()

        viewModel.onBluetoothEvent(
            BluetoothTradeControllerEvent.RemoteOffer(
                partnerName = "Observatoire ami",
                remoteCard = remoteRef,
                verificationCode = "1234",
            ),
        )
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(TradePhase.Confirming, state.phase)
        assertEquals(remoteRef, state.remoteCardRef)
        assertEquals(remoteDisplayCard, state.remoteDisplayCard)
        assertEquals("1234", state.verificationCode)
    }

    @Test
    fun `confirm exchange updates command and waits remote confirmation`() = runTest {
        val viewModel = testViewModel()
        advanceUntilIdle()

        viewModel.confirmExchange()

        val state = viewModel.uiState.value
        assertEquals(true, state.localConfirmed)
        assertEquals(1L, state.confirmationCommandId)
        assertEquals("Confirmation envoyée. En attente du partenaire.", state.message)
    }

    @Test
    fun `local name edition is constrained to Bluetooth advertisement size`() = runTest {
        val viewModel = testViewModel()
        advanceUntilIdle()

        viewModel.onLocalNameEdited("Observatoire 4821")

        assertEquals("Observatoire", viewModel.uiState.value.editableLocalName)
    }

    @Test
    fun `succeeded event resolves received card for success reveal`() = runTest {
        val selectedCandidate = testTradeCandidate()
        val receivedRef = TradeCardRef("ALP-002", "city", "standard")
        val receivedDisplayCard = testCardDefinition("ALP-002", name = "Galaxie d'Andromède")
            .toDisplayCard(
                extensionName = "Astronomes en herbe",
                activeVariant = selectedCandidate.variant.copy(count = 1),
                availableVariants = listOf(selectedCandidate.variant.copy(count = 1)),
            )
        val viewModel = testViewModel(
            selectedCandidate = selectedCandidate,
            tradeGateway = FakeTradeGateway(
                catalogFingerprint = "catalog-1",
                displayCardsByRef = mapOf(receivedRef to receivedDisplayCard),
            ),
        )
        advanceUntilIdle()

        viewModel.onBluetoothEvent(BluetoothTradeControllerEvent.Succeeded(receivedRef))
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(TradePhase.Succeeded, state.phase)
        assertEquals(receivedRef, state.receivedCardRef)
        assertEquals(receivedDisplayCard, state.receivedDisplayCard)
        assertEquals(false, state.isResolvingReceivedCard)
        assertEquals("Échange réussi !", state.message)
    }

    private fun testViewModel(
        selectedCandidate: TradeCardCandidate = testTradeCandidate(),
        tradeGateway: FakeTradeGateway = FakeTradeGateway(catalogFingerprint = "catalog-1"),
        tradeSettingsGateway: FakeTradeSettingsGateway = FakeTradeSettingsGateway("Observatoire test"),
    ): TradeViewModel =
        TradeViewModel(
            selectedCandidate = selectedCandidate,
            tradeRepository = tradeGateway,
            tradeSettingsRepository = tradeSettingsGateway,
        )

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

    private class FakeTradeSettingsGateway(
        initialName: String,
    ) : TradeSettingsGateway {
        private val mutableSettings = MutableStateFlow(TradeSettings(initialName))

        override val settings: StateFlow<TradeSettings> = mutableSettings

        override suspend fun ensureLocalName(): String = mutableSettings.value.localName

        override suspend fun setLocalName(name: String): String {
            val nextName = name.trim().ifBlank { "Observatoire test" }
            mutableSettings.value = TradeSettings(nextName)
            return nextName
        }
    }

    private class FakeTradeGateway(
        private val catalogFingerprint: String,
        private val displayCardsByRef: Map<TradeCardRef, DisplayCard> = emptyMap(),
    ) : TradeGateway {
        override suspend fun loadTradeCandidates(): List<TradeCardCandidate> = emptyList()

        override suspend fun loadTradeCard(ref: TradeCardRef): DisplayCard? = displayCardsByRef[ref]

        override suspend fun catalogFingerprint(): String = catalogFingerprint

        override suspend fun validateTrade(
            localOutgoing: TradeCardRef,
            remoteOutgoing: TradeCardRef,
        ): TradeValidationResult = TradeValidationResult.Valid

        override suspend fun prepareTrade(
            tradeId: String,
            outgoing: TradeCardRef,
            incoming: TradeCardRef,
        ): TradeValidationResult = TradeValidationResult.Valid

        override suspend fun clearPreparedTrade(tradeId: String) = Unit

        override suspend fun applyTrade(
            tradeId: String,
            outgoing: TradeCardRef,
            incoming: TradeCardRef,
        ): TradeValidationResult = TradeValidationResult.Valid
    }
}
