package fr.aumombelli.dstcg

import fr.aumombelli.dstcg.data.CraftingRepository
import fr.aumombelli.dstcg.model.CraftingApplyResult
import fr.aumombelli.dstcg.model.CraftingCardRef
import fr.aumombelli.dstcg.model.CraftingMode
import fr.aumombelli.dstcg.model.ExtensionDefinition
import fr.aumombelli.dstcg.model.OwnedVariantCount
import fr.aumombelli.dstcg.model.StandaloneProgress
import fr.aumombelli.dstcg.model.craftingCountFor
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CraftingRepositoryTest {
    @Test
    fun `load candidates returns only variants eligible for selected mode`() = runTest {
        val fixture = testCraftingRepository(
            collection = ownedCollectionWithVariants(
                "ALP-001",
                OwnedVariantCount("city", "standard", 10),
                OwnedVariantCount("rural", "standard", 1),
            ),
        )

        val skyCandidates = fixture.repository.loadCraftingCandidates(CraftingMode.DarkenSky)
        val stampCandidates = fixture.repository.loadCraftingCandidates(CraftingMode.SpaceAgency)

        assertEquals(listOf("city::standard"), skyCandidates.map { it.sourceVariant.key })
        assertEquals(listOf("city::standard"), stampCandidates.map { it.sourceVariant.key })
    }

    @Test
    fun `apply crafting mutates progress atomically after validation`() = runTest {
        val fixture = testCraftingRepository(
            collection = ownedCollectionWithVariants(
                "ALP-001",
                OwnedVariantCount("city", "standard", 2),
            ),
        )

        val result = fixture.repository.applyCrafting(
            mode = CraftingMode.DarkenSky,
            source = CraftingCardRef("ALP-001", "city", "standard"),
        )

        assertTrue(result is CraftingApplyResult.Success)
        assertEquals(0, fixture.progressGateway.progress.collection.craftingCountFor(
            CraftingCardRef("ALP-001", "city", "standard"),
        ))
        assertEquals(1, fixture.progressGateway.progress.collection.craftingCountFor(
            CraftingCardRef("ALP-001", "suburban", "standard"),
        ))
    }

    @Test
    fun `apply crafting does not update progress when preflight validation fails`() = runTest {
        val fixture = testCraftingRepository(
            collection = ownedCollectionWithVariants(
                "ALP-001",
                OwnedVariantCount("city", "standard", 1),
            ),
        )

        val result = fixture.repository.applyCrafting(
            mode = CraftingMode.DarkenSky,
            source = CraftingCardRef("ALP-001", "city", "standard"),
        )

        assertTrue(result is CraftingApplyResult.Invalid)
        assertEquals(0, fixture.progressGateway.savedProgress.size)
        assertEquals(1, fixture.progressGateway.progress.collection.craftingCountFor(
            CraftingCardRef("ALP-001", "city", "standard"),
        ))
    }

    private fun testCraftingRepository(
        collection: fr.aumombelli.dstcg.model.OwnedCollection,
    ): CraftingRepositoryFixture {
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
        return CraftingRepositoryFixture(
            progressGateway = progressGateway,
            repository = CraftingRepository(
                catalogRepository = catalogGateway,
                progressRepository = progressGateway,
            ),
        )
    }

    private data class CraftingRepositoryFixture(
        val progressGateway: FakeProgressGateway,
        val repository: CraftingRepository,
    )
}
