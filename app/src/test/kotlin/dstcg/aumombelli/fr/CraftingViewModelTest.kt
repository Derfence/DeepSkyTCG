package fr.aumombelli.dstcg

import fr.aumombelli.dstcg.data.CraftingRepository
import fr.aumombelli.dstcg.data.CraftingGateway
import fr.aumombelli.dstcg.feature.crafting.CraftingViewModel
import fr.aumombelli.dstcg.model.CraftingApplyResult
import fr.aumombelli.dstcg.model.CraftingCardCandidate
import fr.aumombelli.dstcg.model.CraftingCardRef
import fr.aumombelli.dstcg.model.CraftingMode
import fr.aumombelli.dstcg.model.DisplayCardVariant
import fr.aumombelli.dstcg.model.ExtensionDefinition
import fr.aumombelli.dstcg.model.OwnedVariantCount
import fr.aumombelli.dstcg.model.StandaloneProgress
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CraftingViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `select mode loads filtered crafting candidates`() = runTest {
        val viewModel = CraftingViewModel(
            craftingRepository = craftingRepository(
                collection = ownedCollectionWithVariants(
                    "ALP-001",
                    OwnedVariantCount("city", "standard", 10),
                ),
            ),
        )

        viewModel.selectMode(CraftingMode.SpaceAgency)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(CraftingMode.SpaceAgency, state.selectedMode)
        assertEquals(1, state.sections.single().cards.single().candidates.size)
    }

    @Test
    fun `apply crafting refreshes candidates and exposes completion`() = runTest {
        val viewModel = CraftingViewModel(
            craftingRepository = craftingRepository(
                collection = ownedCollectionWithVariants(
                    "ALP-001",
                    OwnedVariantCount("city", "standard", 10),
                ),
            ),
        )
        viewModel.selectMode(CraftingMode.SpaceAgency)
        advanceUntilIdle()
        val candidate = viewModel.uiState.value.sections.single().cards.single().candidates.single()

        viewModel.applyCrafting(candidate)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isApplying)
        assertNotNull(state.completion)
        assertTrue(state.completion?.recipe?.target == CraftingCardRef("ALP-001", "city", "stamped"))
        assertTrue(state.successMessage?.isNotBlank() == true)
    }

    @Test
    fun `apply crafting surfaces validation errors`() = runTest {
        val candidate = testCraftingCandidate()
        val viewModel = CraftingViewModel(
            craftingRepository = InvalidCraftingGateway(candidate),
        )
        viewModel.selectMode(CraftingMode.DarkenSky)
        advanceUntilIdle()
        viewModel.applyCrafting(candidate)
        advanceUntilIdle()

        assertEquals("Validation refusee.", viewModel.uiState.value.errorMessage)
        assertFalse(viewModel.uiState.value.isApplying)
    }

    @Test
    fun `apply crafting ignores repeated requests while first request is pending`() = runTest {
        val candidate = testCraftingCandidate()
        val gateway = InvalidCraftingGateway(candidate)
        val viewModel = CraftingViewModel(
            craftingRepository = gateway,
        )
        viewModel.selectMode(CraftingMode.DarkenSky)
        advanceUntilIdle()

        viewModel.applyCrafting(candidate)
        viewModel.applyCrafting(candidate)
        advanceUntilIdle()

        assertEquals(1, gateway.applyCallCount)
    }

    private fun craftingRepository(
        collection: fr.aumombelli.dstcg.model.OwnedCollection,
    ): CraftingRepository {
        val progressGateway = FakeProgressGateway().apply {
            progress = StandaloneProgress(collection = collection)
        }
        val catalogGateway = FakeCatalogGateway().apply {
            extensions = listOf(
                ExtensionDefinition(
                    id = "astronomes-en-herbe",
                    name = "Astronomes en herbe",
                    coverImageRef = "cover",
                ),
            )
            cards = listOf(testCardDefinition("ALP-001"))
            variantProfiles = testVariantProfiles()
        }
        return CraftingRepository(
            catalogRepository = catalogGateway,
            progressRepository = progressGateway,
        )
    }

    private fun testCraftingCandidate(): CraftingCardCandidate =
        CraftingCardCandidate(
            card = testCardDefinition("ALP-001"),
            extensionName = "Astronomes en herbe",
            mode = CraftingMode.DarkenSky,
            sourceVariant = DisplayCardVariant(
                skyQuality = "city",
                skyQualityLabel = "Ville",
                finish = "standard",
                finishLabel = "Standard",
                isHolographic = false,
                count = 2,
            ),
            targetVariant = DisplayCardVariant(
                skyQuality = "suburban",
                skyQualityLabel = "Periurbain",
                finish = "standard",
                finishLabel = "Standard",
                isHolographic = false,
                count = 0,
            ),
            consumedCount = 2,
        )

    private class InvalidCraftingGateway(
        private val candidate: CraftingCardCandidate,
    ) : CraftingGateway {
        var applyCallCount = 0
            private set

        override suspend fun loadCraftingCandidates(mode: CraftingMode): List<CraftingCardCandidate> =
            listOf(candidate)

        override suspend fun hasDarkenSkyCandidates(): Boolean = true

        override suspend fun applyCrafting(
            mode: CraftingMode,
            source: CraftingCardRef,
        ): CraftingApplyResult {
            applyCallCount += 1
            return CraftingApplyResult.Invalid("Validation refusee.")
        }
    }
}
