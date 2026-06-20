package fr.aumombelli.dstcg

import fr.aumombelli.dstcg.data.CraftingRepository
import fr.aumombelli.dstcg.data.CraftingGateway
import fr.aumombelli.dstcg.feature.crafting.CraftingViewModel
import fr.aumombelli.dstcg.model.CraftingApplyResult
import fr.aumombelli.dstcg.model.CraftingCardCandidate
import fr.aumombelli.dstcg.model.CraftingCardRef
import fr.aumombelli.dstcg.model.CraftingMode
import fr.aumombelli.dstcg.model.CraftingRecipe
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
    fun `apply all visible darken sky candidates applies one candidate per visible card`() = runTest {
        val firstCandidate = testCraftingCandidate(cardId = "ALP-001")
        val secondCandidate = testCraftingCandidate(cardId = "ALP-002")
        val gateway = RecordingCraftingGateway(
            initialCandidates = listOf(firstCandidate, secondCandidate),
            refreshedCandidates = emptyList(),
        )
        val viewModel = CraftingViewModel(
            craftingRepository = gateway,
        )
        viewModel.selectMode(CraftingMode.DarkenSky)
        advanceUntilIdle()

        viewModel.applyAllVisibleDarkenSkyCandidates()
        advanceUntilIdle()

        assertEquals(
            listOf(
                CraftingMode.DarkenSky to firstCandidate.sourceRef,
                CraftingMode.DarkenSky to secondCandidate.sourceRef,
            ),
            gateway.applied,
        )
        val state = viewModel.uiState.value
        assertFalse(state.isApplying)
        assertTrue(state.sections.isEmpty())
        assertEquals("2 cartes ont été assombries.", state.successMessage)
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

    private fun testCraftingCandidate(cardId: String = "ALP-001"): CraftingCardCandidate =
        CraftingCardCandidate(
            card = testCardDefinition(cardId),
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

    private class RecordingCraftingGateway(
        private val initialCandidates: List<CraftingCardCandidate>,
        private val refreshedCandidates: List<CraftingCardCandidate>,
    ) : CraftingGateway {
        private val candidatesByRef = (initialCandidates + refreshedCandidates).associateBy { it.sourceRef }
        private var loadCount = 0
        val applied = mutableListOf<Pair<CraftingMode, CraftingCardRef>>()
        var applyCallCount = 0
            private set

        override suspend fun loadCraftingCandidates(mode: CraftingMode): List<CraftingCardCandidate> =
            if (loadCount++ == 0) initialCandidates else refreshedCandidates

        override suspend fun hasDarkenSkyCandidates(): Boolean = initialCandidates.isNotEmpty()

        override suspend fun applyCrafting(
            mode: CraftingMode,
            source: CraftingCardRef,
        ): CraftingApplyResult {
            applyCallCount += 1
            applied += mode to source
            val candidate = candidatesByRef.getValue(source)
            return CraftingApplyResult.Success(
                CraftingRecipe(
                    mode = mode,
                    source = source,
                    target = candidate.targetRef,
                    consumedCount = candidate.consumedCount,
                ),
            )
        }
    }

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
