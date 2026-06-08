package fr.aumombelli.dstcg

import fr.aumombelli.dstcg.model.HomeMenuNoveltyState
import fr.aumombelli.dstcg.model.LibraryCardNoveltyState
import fr.aumombelli.dstcg.model.ExtensionDefinition
import fr.aumombelli.dstcg.model.OwnedVariantCount
import fr.aumombelli.dstcg.model.StandaloneProgress
import fr.aumombelli.dstcg.ui.viewmodel.LibraryViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
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
                testCardDefinition("MON-050", extensionId = "moon-dawn", name = "M57", rarityLabel = "Epic", imageRef = "ring"),
                testCardDefinition("ALP-001", extensionId = "core-alpha", name = "Orion", imageRef = "fox"),
                testCardDefinition("MON-001", extensionId = "moon-dawn", name = "M31", rarityLabel = "Rare", imageRef = "hare"),
            )
        }
        val collectionGateway = FakeCollectionGateway().apply {
            collection = ownedCollectionWithVariants(
                "MON-050",
                OwnedVariantCount("city", "standard", 1),
                OwnedVariantCount("holographic", "stamped", 1),
            )
        }

        val viewModel = LibraryViewModel(catalogGateway, collectionGateway)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(false, state.isLoading)
        assertNull(state.errorMessage)
        assertEquals(listOf("core-alpha", "moon-dawn"), state.sections.map { it.extension.id })
        assertEquals(listOf("MON-002", "MON-001", "MON-050"), state.sections[1].cards.map { it.definition.id })
        val ownedCard = state.sections[1].cards.first { it.definition.id == "MON-050" }
        assertEquals(2, ownedCard.ownedCount)
        assertEquals("Moon Dawn", ownedCard.extensionName)
        assertEquals(
            listOf("holographic::stamped", "city::standard"),
            ownedCard.availableVariants.map { it.key },
        )
        assertEquals(4, state.onboardingVariantWalkthroughPages.size)
        assertEquals(
            listOf("Rareté", "Qualité du ciel", "Tampon", "Holographique"),
            state.onboardingVariantWalkthroughPages.map { it.title },
        )
        assertEquals(
            listOf("core-alpha", "moon-dawn"),
            state.filterOptions.extensions.map { it.id },
        )
        assertEquals(
            listOf("Common", "Rare", "Epic"),
            state.filterOptions.rarities.map { it.id },
        )
        assertEquals(
            listOf("city", "suburban", "rural", "mountain", "holographic"),
            state.filterOptions.skyQualities.map { it.id },
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

    @Test
    fun `refresh marks new library cards and clears presented novelty`() = runTest {
        val catalogGateway = FakeCatalogGateway().apply {
            extensions = listOf(
                ExtensionDefinition("moon-dawn", "Moon Dawn", "cover"),
            )
            cards = listOf(
                testCardDefinition("MON-001", extensionId = "moon-dawn", name = "M31", imageRef = "hare"),
                testCardDefinition("MON-050", extensionId = "moon-dawn", name = "M57", rarityLabel = "Epic", imageRef = "ring"),
            )
        }
        val progressGateway = FakeProgressGateway().apply {
            progress = StandaloneProgress(
                collection = ownedCollectionOf(
                    "MON-001" to 1,
                    "MON-050" to 1,
                ),
                rechargeState = testRechargeState(),
                homeMenuNoveltyState = HomeMenuNoveltyState(library = true),
                libraryCardNoveltyState = LibraryCardNoveltyState(
                    newCardIds = setOf("MON-050"),
                ),
            )
        }

        val viewModel = LibraryViewModel(
            catalogRepository = catalogGateway,
            collectionRepository = FakeCollectionGateway(),
            progressRepository = progressGateway,
        )
        advanceUntilIdle()

        val state = viewModel.uiState.value
        val regularCard = state.sections.single().cards.first { it.definition.id == "MON-001" }
        val newCard = state.sections.single().cards.first { it.definition.id == "MON-050" }

        assertFalse(regularCard.showNewIndicator)
        assertTrue(newCard.showNewIndicator)
        assertFalse(progressGateway.progress.homeMenuNoveltyState.library)
        assertTrue(progressGateway.progress.libraryCardNoveltyState.newCardIds.isEmpty())
    }
}
