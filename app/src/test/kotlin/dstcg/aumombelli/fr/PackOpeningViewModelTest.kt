package fr.aumombelli.dstcg

import fr.aumombelli.dstcg.model.DrawPackResponse
import fr.aumombelli.dstcg.model.ExtensionDefinition
import fr.aumombelli.dstcg.ui.viewmodel.PackOpeningViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PackOpeningViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `view model enriches pack cards with catalog and extension data`() = runTest {
        val catalogGateway = FakeCatalogGateway().apply {
            extensions = listOf(ExtensionDefinition("astronomes-en-herbe", "Astronomes en herbe", "cover"))
            cards = listOf(
                testCardDefinition(
                    id = "M42",
                    extensionId = "astronomes-en-herbe",
                    name = "Nebuleuse d'Orion",
                ),
            )
        }
        val packGateway = FakePackGateway().apply {
            openPackResponse = DrawPackResponse.fromCards(
                extensionId = "astronomes-en-herbe",
                drawnAt = "2026-03-24T12:00:00Z",
                rechargeState = testRechargeStateWithNextChargeAt(
                    availableDrawCount = 9,
                    nextChargeAt = "2026-03-24T18:00:00Z",
                ),
                cards = listOf(
                    testPackCard(
                        cardId = "M42",
                        name = "Nebuleuse d'Orion",
                        rarityLabel = "Common",
                        imageRef = "m42",
                        skyQuality = "holographic",
                        skyQualityLabel = "Holographique",
                        isHolographic = true,
                    ),
                ),
            )
        }

        val viewModel = PackOpeningViewModel(
            catalogRepository = catalogGateway,
            packRepository = packGateway,
        )

        packGateway.openPack("astronomes-en-herbe")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(1, state.displayCards.size)
        assertEquals("Astronomes en herbe", state.displayCards.first().extensionName)
        assertEquals("Holographique", state.displayCards.first().activeVariant.skyQualityLabel)
        assertEquals(true, state.displayCards.first().activeVariant.isHolographic)
    }
}
