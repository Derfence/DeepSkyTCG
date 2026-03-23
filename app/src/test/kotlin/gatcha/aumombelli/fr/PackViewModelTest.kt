package fr.aumombelli.gatcha

import fr.aumombelli.gatcha.data.PendingSaveException
import fr.aumombelli.gatcha.model.CardDefinition
import fr.aumombelli.gatcha.model.DrawPackResponse
import fr.aumombelli.gatcha.model.ExtensionDefinition
import fr.aumombelli.gatcha.model.OwnedCollection
import fr.aumombelli.gatcha.model.PackCard
import fr.aumombelli.gatcha.model.StoredSessionSnapshot
import fr.aumombelli.gatcha.ui.viewmodel.PackEvent
import fr.aumombelli.gatcha.ui.viewmodel.PackViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PackViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `refresh loads extensions collection and next draw timestamp`() = runTest {
        val catalogGateway = FakeCatalogGateway().apply {
            extensions = listOf(ExtensionDefinition("core-alpha", "Core Alpha", "cover"))
            cards = listOf(CardDefinition("ALP-001", "core-alpha", "Spark Fox", "Common", 1, "fox"))
        }
        val collectionGateway = FakeCollectionGateway().apply {
            cachedCollection = OwnedCollection(cards = mapOf("ALP-001" to 1))
        }
        val sessionGateway = FakeSessionGateway().apply {
            snapshot = StoredSessionSnapshot(nextDrawAt = "2026-03-24T00:00:00Z")
        }

        val viewModel = PackViewModel(
            catalogRepository = catalogGateway,
            collectionRepository = collectionGateway,
            packRepository = FakePackGateway(),
            sessionRepository = sessionGateway,
        )
        advanceUntilIdle()

        assertEquals(false, viewModel.uiState.value.isLoading)
        assertEquals("2026-03-24T00:00:00Z", viewModel.uiState.value.nextDrawAt)
        assertEquals(1, viewModel.uiState.value.currentCollection.cards["ALP-001"])
    }

    @Test
    fun `open pack merges collection and emits navigation`() = runTest {
        val packGateway = FakePackGateway().apply {
            openPackResponse = DrawPackResponse(
                extensionId = "core-alpha",
                drawnAt = "2026-03-23T12:00:00Z",
                nextDrawAt = "2026-03-24T00:00:00Z",
                cards = listOf(
                    PackCard("ALP-001", "Spark Fox", "Common", "spark_fox"),
                    PackCard("ALP-002", "Steam Golem", "Common", "steam_golem"),
                ),
            )
        }
        val collectionGateway = FakeCollectionGateway().apply {
            cachedCollection = OwnedCollection(cards = mapOf("ALP-001" to 1))
        }
        val catalogGateway = FakeCatalogGateway().apply {
            extensions = listOf(ExtensionDefinition("core-alpha", "Core Alpha", "cover"))
        }

        val viewModel = PackViewModel(
            catalogRepository = catalogGateway,
            collectionRepository = collectionGateway,
            packRepository = packGateway,
            sessionRepository = FakeSessionGateway(),
        )
        advanceUntilIdle()

        val event = async { viewModel.events.first() }
        viewModel.openPack("core-alpha")
        advanceUntilIdle()

        assertEquals(PackEvent.NavigateToOpening, event.await())
        assertEquals(2, viewModel.uiState.value.currentCollection.cards["ALP-001"])
        assertEquals(1, viewModel.uiState.value.currentCollection.cards["ALP-002"])
        assertEquals("2026-03-24T00:00:00Z", viewModel.uiState.value.nextDrawAt)
    }

    @Test
    fun `open pack surfaces pending save message`() = runTest {
        val viewModel = PackViewModel(
            catalogRepository = FakeCatalogGateway().apply {
                extensions = listOf(ExtensionDefinition("core-alpha", "Core Alpha", "cover"))
            },
            collectionRepository = FakeCollectionGateway(),
            packRepository = FakePackGateway().apply {
                openPackFailure = PendingSaveException("Pack drawn but save failed.", IllegalStateException("offline"))
            },
            sessionRepository = FakeSessionGateway(),
        )
        advanceUntilIdle()

        viewModel.openPack("core-alpha")
        advanceUntilIdle()

        assertEquals("Pack drawn but save failed.", viewModel.uiState.value.errorMessage)
        assertEquals(false, viewModel.uiState.value.isLoading)
    }
}
