package fr.aumombelli.dstcg

import fr.aumombelli.dstcg.feature.badges.BadgeBookViewModel
import fr.aumombelli.dstcg.model.ExtensionDefinition
import fr.aumombelli.dstcg.model.OwnedVariantCount
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
        val progressGateway = FakeProgressGateway().apply {
            progress = progress.copy(
                collection = ownedCollectionWithVariants(
                    "MON-001",
                    OwnedVariantCount("city", "standard", 1),
                ),
                openedPackCount = 1,
            )
        }

        val viewModel = BadgeBookViewModel(catalogGateway, progressGateway)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(false, state.isLoading)
        assertEquals(listOf("general", "core-alpha", "moon-dawn"), state.sections.map { it.extensionId })
        assertEquals("Général", state.sections.first().extensionName)
        assertEquals("1 / 1 pack ouvert", state.sections.first().badges.single().progress.label)
        assertEquals("Moon Dawn", state.sections[2].extensionName)
        assertEquals("1 / 1 cartes valides", state.sections[2].badges.first().progress.label)
    }

    @Test
    fun `refresh surfaces loading failure`() = runTest {
        val catalogGateway = FakeCatalogGateway().apply {
            extensionsFailure = IllegalStateException("Catalog unavailable")
        }

        val viewModel = BadgeBookViewModel(catalogGateway, FakeProgressGateway())
        advanceUntilIdle()

        assertEquals("Catalog unavailable", viewModel.uiState.value.errorMessage)
        assertEquals(false, viewModel.uiState.value.isLoading)
    }
}
