package fr.aumombelli.gatcha

import fr.aumombelli.gatcha.feature.badges.BadgeBookViewModel
import fr.aumombelli.gatcha.model.ExtensionDefinition
import fr.aumombelli.gatcha.model.OwnedVariantCount
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class BadgeBookViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `refresh builds badge sections grouped by extension`() = runTest {
        val catalogGateway = FakeCatalogGateway().apply {
            extensions = listOf(
                ExtensionDefinition("core-alpha", "Core Alpha", "cover"),
                ExtensionDefinition("moon-dawn", "Moon Dawn", "cover"),
            )
            cards = listOf(
                testCardDefinition("ALP-001", extensionId = "core-alpha"),
                testCardDefinition("MON-001", extensionId = "moon-dawn"),
            )
        }
        val collectionGateway = FakeCollectionGateway().apply {
            collection = ownedCollectionWithVariants(
                "MON-001",
                OwnedVariantCount("city", "standard", 1),
            )
        }

        val viewModel = BadgeBookViewModel(catalogGateway, collectionGateway)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(false, state.isLoading)
        assertEquals(listOf("core-alpha", "moon-dawn"), state.sections.map { it.extensionId })
        assertEquals("Moon Dawn", state.sections[1].extensionName)
        assertEquals("1 / 1 cartes valides", state.sections[1].badges.first().progress.label)
    }

    @Test
    fun `refresh surfaces loading failure`() = runTest {
        val catalogGateway = FakeCatalogGateway().apply {
            extensionsFailure = IllegalStateException("Catalog unavailable")
        }

        val viewModel = BadgeBookViewModel(catalogGateway, FakeCollectionGateway())
        advanceUntilIdle()

        assertEquals("Catalog unavailable", viewModel.uiState.value.errorMessage)
        assertEquals(false, viewModel.uiState.value.isLoading)
    }
}
