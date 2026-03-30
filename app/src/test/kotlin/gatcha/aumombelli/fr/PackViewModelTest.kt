package fr.aumombelli.gatcha

import fr.aumombelli.gatcha.feature.packs.selection.PackEvent
import fr.aumombelli.gatcha.model.DrawPackResponse
import fr.aumombelli.gatcha.model.ExtensionDefinition
import fr.aumombelli.gatcha.model.StandaloneProgress
import fr.aumombelli.gatcha.ui.viewmodel.PackViewModel
import java.time.Duration
import java.time.Instant
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
    private val fixedNow = Instant.parse("2026-03-24T12:00:00Z")
    private val gameSettings = testGameSettings(now = fixedNow)

    @Test
    fun `select extension and clear selection reset transient booster state`() = runTest {
        val viewModel = PackViewModel(
            catalogRepository = FakeCatalogGateway(),
            progressRepository = FakeProgressGateway(),
            packRepository = FakePackGateway(),
            gameSettings = gameSettings,
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
    fun `refresh loads extensions collection and charge status from local progress`() = runTest {
        val catalogGateway = FakeCatalogGateway().apply {
            extensions = listOf(ExtensionDefinition("core-alpha", "Core Alpha", "cover"))
        }
        val progressGateway = FakeProgressGateway().apply {
            progress = StandaloneProgress(
                collection = ownedCollectionOf("ALP-001" to 1),
                availableDrawCount = 4,
                nextChargeAt = "2026-03-24T18:00:00Z",
            )
        }

        val viewModel = PackViewModel(
            catalogRepository = catalogGateway,
            progressRepository = progressGateway,
            packRepository = FakePackGateway(),
            gameSettings = gameSettings,
        )
        advanceUntilIdle()

        assertEquals(false, viewModel.uiState.value.isLoading)
        assertEquals(4, viewModel.uiState.value.availableDrawCount)
        assertEquals("2026-03-24T18:00:00Z", viewModel.uiState.value.nextChargeAt)
        assertEquals(Duration.ofHours(6), viewModel.uiState.value.remainingDuration)
        assertEquals(false, viewModel.uiState.value.isDrawLocked)
        assertEquals(1, viewModel.uiState.value.currentCollection.cards["ALP-001"]?.totalOwned)
    }

    @Test
    fun `refresh immediately clears transient pack selection state`() = runTest {
        val viewModel = PackViewModel(
            catalogRepository = FakeCatalogGateway().apply {
                extensions = listOf(ExtensionDefinition("core-alpha", "Core Alpha", "cover"))
            },
            progressRepository = FakeProgressGateway(),
            packRepository = FakePackGateway(),
            gameSettings = gameSettings,
        )
        advanceUntilIdle()

        viewModel.selectExtension("core-alpha")
        viewModel.selectBooster(1)

        viewModel.refresh()

        assertEquals(true, viewModel.uiState.value.isLoading)
        assertEquals(null, viewModel.uiState.value.selectedExtensionId)
        assertEquals(null, viewModel.uiState.value.selectedBoosterIndex)
        assertEquals(false, viewModel.uiState.value.isAwaitingPackResult)
        assertEquals(null, viewModel.uiState.value.errorMessage)
    }

    @Test
    fun `open pack reloads saved progress and emits navigation`() = runTest {
        val response = DrawPackResponse(
            extensionId = "core-alpha",
            drawnAt = "2026-03-23T12:00:00Z",
            availableDrawCount = 9,
            nextChargeAt = "2026-03-24T18:00:00Z",
            cards = listOf(
                testPackCard("ALP-001", "Nebuleuse d'Orion", "Common", "spark_fox"),
                testPackCard(
                    "ALP-002",
                    "Galaxie d'Andromede",
                    "Common",
                    "steam_golem",
                    skyQuality = "rural",
                    skyQualityLabel = "Campagne",
                ),
            ),
        )
        val progressGateway = FakeProgressGateway().apply {
            progress = StandaloneProgress(
                collection = ownedCollectionOf("ALP-001" to 1),
                availableDrawCount = 10,
                nextChargeAt = null,
            )
        }
        val packGateway = FakePackGateway().apply {
            openPackResponse = response
            onOpenPack = {
                progressGateway.progress = StandaloneProgress(
                    collection = ownedCollectionOf("ALP-001" to 2, "ALP-002" to 1),
                    availableDrawCount = response.availableDrawCount,
                    nextChargeAt = response.nextChargeAt,
                )
            }
        }
        val catalogGateway = FakeCatalogGateway().apply {
            extensions = listOf(ExtensionDefinition("core-alpha", "Core Alpha", "cover"))
        }

        val viewModel = PackViewModel(
            catalogRepository = catalogGateway,
            progressRepository = progressGateway,
            packRepository = packGateway,
            gameSettings = gameSettings,
        )
        advanceUntilIdle()

        val event = async { viewModel.events.first() }
        viewModel.selectExtension("core-alpha")
        viewModel.selectBooster(2)
        viewModel.openPack("core-alpha")
        advanceUntilIdle()

        assertEquals(PackEvent.PackReadyForReveal(), event.await())
        assertEquals(listOf("core-alpha"), packGateway.openPackCalls)
        assertEquals(2, viewModel.uiState.value.currentCollection.cards["ALP-001"]?.totalOwned)
        assertEquals(1, viewModel.uiState.value.currentCollection.cards["ALP-002"]?.totalOwned)
        assertEquals(9, viewModel.uiState.value.availableDrawCount)
        assertEquals("2026-03-24T18:00:00Z", viewModel.uiState.value.nextChargeAt)
        assertEquals(Duration.ofHours(6), viewModel.uiState.value.remainingDuration)
        assertEquals("core-alpha", viewModel.uiState.value.selectedExtensionId)
        assertEquals(2, viewModel.uiState.value.selectedBoosterIndex)
        assertEquals(false, viewModel.uiState.value.isAwaitingPackResult)
    }

    @Test
    fun `open pack emits newly unlocked badges when collection crosses a badge threshold`() = runTest {
        val response = DrawPackResponse(
            extensionId = "core-alpha",
            drawnAt = "2026-03-23T12:00:00Z",
            availableDrawCount = 9,
            nextChargeAt = null,
            cards = listOf(
                testPackCard("ALP-001", "Nebuleuse d'Orion", "Common", "spark_fox"),
            ),
        )
        val progressGateway = FakeProgressGateway().apply {
            progress = StandaloneProgress(
                collection = ownedCollectionOf(),
                availableDrawCount = 10,
                nextChargeAt = null,
            )
        }
        val packGateway = FakePackGateway().apply {
            openPackResponse = response
            onOpenPack = {
                progressGateway.progress = StandaloneProgress(
                    collection = ownedCollectionOf("ALP-001" to 1),
                    availableDrawCount = response.availableDrawCount,
                    nextChargeAt = response.nextChargeAt,
                )
            }
        }
        val catalogGateway = FakeCatalogGateway().apply {
            extensions = listOf(ExtensionDefinition("core-alpha", "Core Alpha", "cover"))
            cards = listOf(testCardDefinition("ALP-001", extensionId = "core-alpha"))
        }

        val viewModel = PackViewModel(
            catalogRepository = catalogGateway,
            progressRepository = progressGateway,
            packRepository = packGateway,
            gameSettings = gameSettings,
        )
        advanceUntilIdle()

        val event = async { viewModel.events.first() }
        viewModel.openPack("core-alpha")
        advanceUntilIdle()

        assertEquals(
            listOf("core-alpha::sky::city"),
            (event.await() as PackEvent.PackReadyForReveal).newlyUnlockedBadges.map { it.id },
        )
    }

    @Test
    fun `open pack emits first pack opened badge when progress count increases to one`() = runTest {
        val response = DrawPackResponse(
            extensionId = "core-alpha",
            drawnAt = "2026-03-23T12:00:00Z",
            availableDrawCount = 9,
            nextChargeAt = null,
            cards = emptyList(),
        )
        val progressGateway = FakeProgressGateway().apply {
            progress = StandaloneProgress(
                collection = ownedCollectionOf(),
                availableDrawCount = 10,
                nextChargeAt = null,
                openedPackCount = 0,
            )
        }
        val packGateway = FakePackGateway().apply {
            openPackResponse = response
            onOpenPack = {
                progressGateway.progress = StandaloneProgress(
                    collection = ownedCollectionOf(),
                    availableDrawCount = response.availableDrawCount,
                    nextChargeAt = response.nextChargeAt,
                    openedPackCount = 1,
                )
            }
        }
        val catalogGateway = FakeCatalogGateway().apply {
            extensions = listOf(ExtensionDefinition("core-alpha", "Core Alpha", "cover"))
        }

        val viewModel = PackViewModel(
            catalogRepository = catalogGateway,
            progressRepository = progressGateway,
            packRepository = packGateway,
            gameSettings = gameSettings,
        )
        advanceUntilIdle()

        val event = async { viewModel.events.first() }
        viewModel.openPack("core-alpha")
        advanceUntilIdle()

        assertEquals(
            listOf("general::pack::first-opened"),
            (event.await() as PackEvent.PackReadyForReveal).newlyUnlockedBadges.map { it.id },
        )
    }

    @Test
    fun `open pack generic failure resets booster selection and uses default message`() = runTest {
        val viewModel = PackViewModel(
            catalogRepository = FakeCatalogGateway().apply {
                extensions = listOf(ExtensionDefinition("core-alpha", "Core Alpha", "cover"))
            },
            progressRepository = FakeProgressGateway(),
            packRepository = FakePackGateway().apply {
                openPackFailure = IllegalStateException()
            },
            gameSettings = gameSettings,
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
