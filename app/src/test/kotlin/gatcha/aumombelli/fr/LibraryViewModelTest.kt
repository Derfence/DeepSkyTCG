package fr.aumombelli.gatcha

import fr.aumombelli.gatcha.model.CardDefinition
import fr.aumombelli.gatcha.model.ExtensionDefinition
import fr.aumombelli.gatcha.model.OwnedVariantCount
import fr.aumombelli.gatcha.ui.viewmodel.LibraryViewModel
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
                testCardDefinition("MON-002", extensionId = "moon-dawn", name = "M42", imageRef = "dawn"),
                testCardDefinition("ALP-001", extensionId = "core-alpha", name = "Orion", imageRef = "fox"),
                testCardDefinition("MON-001", extensionId = "moon-dawn", name = "M31", rarityLabel = "Rare", imageRef = "hare"),
            )
        }
        val collectionGateway = FakeCollectionGateway().apply {
            cachedCollection = ownedCollectionWithVariants(
                "MON-001",
                OwnedVariantCount("city", "standard", 1),
                OwnedVariantCount("mountain", "holographic", 1),
            )
        }

        val viewModel = LibraryViewModel(catalogGateway, collectionGateway)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(false, state.isLoading)
        assertNull(state.errorMessage)
        assertEquals(listOf("core-alpha", "moon-dawn"), state.sections.map { it.extension.id })
        assertEquals(listOf("MON-001", "MON-002"), state.sections[1].cards.map { it.definition.id })
        assertEquals(2, state.sections[1].cards.first().ownedCount)
        assertEquals("Moon Dawn", state.sections[1].cards.first().extensionName)
        assertEquals(
            listOf("mountain::holographic", "city::standard"),
            state.sections[1].cards.first().availableVariants.map { it.key },
        )
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
