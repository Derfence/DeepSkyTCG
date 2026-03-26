package fr.aumombelli.gatcha

import fr.aumombelli.gatcha.data.PendingSaveException
import fr.aumombelli.gatcha.feature.packs.selection.PackEvent
import fr.aumombelli.gatcha.model.CardDefinition
import fr.aumombelli.gatcha.model.DrawPackResponse
import fr.aumombelli.gatcha.model.ExtensionDefinition
import fr.aumombelli.gatcha.model.OwnedCollection
import fr.aumombelli.gatcha.model.PackCard
import fr.aumombelli.gatcha.model.StoredSessionSnapshot
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
    fun `select extension and clear selection reset transient booster state`() = runTest {
        val viewModel = PackViewModel(
            catalogRepository = FakeCatalogGateway(),
            collectionRepository = FakeCollectionGateway(),
            packRepository = FakePackGateway(),
            sessionRepository = FakeSessionGateway(),
        )
        advanceUntilIdle()

        viewModel.selectExtension("core-alpha")
        viewModel.selectBooster(3)
        viewModel.clearExtensionSelection()

        assertEquals(null, viewModel.uiState.value.selectedExtensionId)
        assertEquals(null, viewModel.uiState.value.selectedBoosterIndex)
        assertEquals(false, viewModel.uiState.value.isAwaitingPackResult)
        assertEquals(null, viewModel.uiState.value.errorMessage)
    }

    @Test
    fun `refresh loads extensions collection and next draw timestamp`() = runTest {
        val catalogGateway = FakeCatalogGateway().apply {
            extensions = listOf(ExtensionDefinition("core-alpha", "Core Alpha", "cover"))
            cards = listOf(testCardDefinition("ALP-001", extensionId = "core-alpha", name = "Nebuleuse d'Orion", imageRef = "fox"))
        }
        val collectionGateway = FakeCollectionGateway().apply {
            cachedCollection = ownedCollectionOf("ALP-001" to 1)
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
        assertEquals(1, viewModel.uiState.value.currentCollection.cards["ALP-001"]?.totalOwned)
    }

    @Test
    fun `open pack merges collection and emits navigation`() = runTest {
        val packGateway = FakePackGateway().apply {
            openPackResponse = DrawPackResponse(
                extensionId = "core-alpha",
                drawnAt = "2026-03-23T12:00:00Z",
                nextDrawAt = "2026-03-24T00:00:00Z",
                cards = listOf(
                    testPackCard("ALP-001", "Nebuleuse d'Orion", "Common", "spark_fox"),
                    testPackCard("ALP-002", "Galaxie d'Andromede", "Common", "steam_golem", skyQuality = "rural", skyQualityLabel = "Campagne"),
                ),
            )
        }
        val collectionGateway = FakeCollectionGateway().apply {
            cachedCollection = ownedCollectionOf("ALP-001" to 1)
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
        viewModel.selectExtension("core-alpha")
        viewModel.selectBooster(2)
        viewModel.openPack("core-alpha")
        advanceUntilIdle()

        assertEquals(PackEvent.PackReadyForReveal, event.await())
        assertEquals(2, viewModel.uiState.value.currentCollection.cards["ALP-001"]?.totalOwned)
        assertEquals(1, viewModel.uiState.value.currentCollection.cards["ALP-002"]?.totalOwned)
        assertEquals("2026-03-24T00:00:00Z", viewModel.uiState.value.nextDrawAt)
        assertEquals("core-alpha", viewModel.uiState.value.selectedExtensionId)
        assertEquals(2, viewModel.uiState.value.selectedBoosterIndex)
        assertEquals(false, viewModel.uiState.value.isAwaitingPackResult)
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

    @Test
    fun `open pack generic failure resets booster selection and uses default message`() = runTest {
        val viewModel = PackViewModel(
            catalogRepository = FakeCatalogGateway().apply {
                extensions = listOf(ExtensionDefinition("core-alpha", "Core Alpha", "cover"))
            },
            collectionRepository = FakeCollectionGateway(),
            packRepository = FakePackGateway().apply {
                openPackFailure = IllegalStateException()
            },
            sessionRepository = FakeSessionGateway(),
        )
        advanceUntilIdle()

        viewModel.selectExtension("core-alpha")
        viewModel.selectBooster(1)
        viewModel.openPack("core-alpha")
        advanceUntilIdle()

        assertEquals("core-alpha", viewModel.uiState.value.selectedExtensionId)
        assertEquals(null, viewModel.uiState.value.selectedBoosterIndex)
        assertEquals(false, viewModel.uiState.value.isAwaitingPackResult)
        assertEquals("Unable to open the pack.", viewModel.uiState.value.errorMessage)
    }
}
