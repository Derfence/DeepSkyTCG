package fr.aumombelli.gatcha

import fr.aumombelli.gatcha.model.DrawPackResponse
import fr.aumombelli.gatcha.model.ExtensionDefinition
import fr.aumombelli.gatcha.ui.viewmodel.PackOpeningViewModel
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
            openPackResponse = DrawPackResponse(
                extensionId = "astronomes-en-herbe",
                drawnAt = "2026-03-24T12:00:00Z",
                nextDrawAt = "2026-03-25T12:00:00Z",
                cards = listOf(
                    testPackCard(
                        cardId = "M42",
                        name = "Nebuleuse d'Orion",
                        rarityLabel = "Common",
                        imageRef = "m42",
                        skyQuality = "rural",
                        skyQualityLabel = "Campagne",
                        finish = "holographic",
                        finishLabel = "Holographique",
                        isHolographic = true,
                    ),
                ),
            )
        }

        val viewModel = PackOpeningViewModel(
            catalogRepository = catalogGateway,
            packRepository = packGateway,
        )

        packGateway.openPack("astronomes-en-herbe", ownedCollectionOf())
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(1, state.displayCards.size)
        assertEquals("Astronomes en herbe", state.displayCards.first().extensionName)
        assertEquals("Campagne", state.displayCards.first().activeVariant.skyQualityLabel)
        assertEquals(true, state.displayCards.first().activeVariant.isHolographic)
    }
}
