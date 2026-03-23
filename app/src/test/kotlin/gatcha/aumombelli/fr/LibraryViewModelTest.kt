package gatcha.aumombelli.fr

import gatcha.aumombelli.fr.model.CardDefinition
import gatcha.aumombelli.fr.model.ExtensionDefinition
import gatcha.aumombelli.fr.model.OwnedCollection
import gatcha.aumombelli.fr.ui.viewmodel.LibraryViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LibraryViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `refresh groups cards by extension and keeps owned counts`() = runTest {
        val catalogGateway = FakeCatalogGateway().apply {
            extensions = listOf(
                ExtensionDefinition("core-alpha", "Core Alpha", "cover"),
                ExtensionDefinition("moon-dawn", "Moon Dawn", "cover"),
            )
            cards = listOf(
                CardDefinition("MON-002", "moon-dawn", "Dawn Scribe", "Common", 1, "dawn"),
                CardDefinition("ALP-001", "core-alpha", "Spark Fox", "Common", 1, "fox"),
                CardDefinition("MON-001", "moon-dawn", "Moonlit Hare", "Rare", 1, "hare"),
            )
        }
        val collectionGateway = FakeCollectionGateway().apply {
            cachedCollection = OwnedCollection(cards = mapOf("MON-001" to 2))
        }

        val viewModel = LibraryViewModel(catalogGateway, collectionGateway)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(false, state.isLoading)
        assertNull(state.errorMessage)
        assertEquals(listOf("core-alpha", "moon-dawn"), state.sections.map { it.extension.id })
        assertEquals(listOf("MON-001", "MON-002"), state.sections[1].cards.map { it.definition.id })
        assertEquals(2, state.sections[1].cards.first().ownedCount)
    }

    @Test
    fun `refresh surfaces loading failure`() = runTest {
        val catalogGateway = FakeCatalogGateway().apply {
            extensionsFailure = IllegalStateException("Catalog unavailable")
        }

        val viewModel = LibraryViewModel(catalogGateway, FakeCollectionGateway())
        advanceUntilIdle()

        assertEquals("Catalog unavailable", viewModel.uiState.value.errorMessage)
        assertEquals(false, viewModel.uiState.value.isLoading)
    }
}
