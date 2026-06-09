package fr.aumombelli.dstcg

import fr.aumombelli.dstcg.data.TradeRepository
import fr.aumombelli.dstcg.model.ExtensionDefinition
import fr.aumombelli.dstcg.model.OwnedVariantCount
import fr.aumombelli.dstcg.model.StandaloneProgress
import fr.aumombelli.dstcg.model.TradeCardRef
import fr.aumombelli.dstcg.model.TradeValidationResult
import fr.aumombelli.dstcg.model.tradeCountFor
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TradeRepositoryTest {
    @Test
    fun `load candidates returns only duplicated variants`() = runTest {
        val repository = testTradeRepository(
            collection = ownedCollectionWithVariants(
                "ALP-001",
                OwnedVariantCount("city", "standard", 2),
                OwnedVariantCount("rural", "standard", 1),
            ),
        ).repository

        val candidates = repository.loadTradeCandidates()

        assertEquals(1, candidates.size)
        assertEquals("ALP-001", candidates.single().card.id)
        assertEquals("city", candidates.single().variant.skyQuality)
    }

    @Test
    fun `apply trade is idempotent for completed trade id`() = runTest {
        val fixture = testTradeRepository(
            collection = ownedCollectionWithVariants(
                "ALP-001",
                OwnedVariantCount("city", "standard", 2),
            ),
        )
        val outgoing = TradeCardRef("ALP-001", "city", "standard")
        val incoming = TradeCardRef("ALP-002", "city", "standard")

        val firstResult = fixture.repository.applyTrade(
            tradeId = "trade-1",
            outgoing = outgoing,
            incoming = incoming,
        )
        val secondResult = fixture.repository.applyTrade(
            tradeId = "trade-1",
            outgoing = outgoing,
            incoming = incoming,
        )

        assertEquals(TradeValidationResult.Valid, firstResult)
        assertEquals(TradeValidationResult.Valid, secondResult)
        assertEquals(1, fixture.progressGateway.progress.collection.tradeCountFor(outgoing))
        assertEquals(1, fixture.progressGateway.progress.collection.tradeCountFor(incoming))
        assertTrue(fixture.progressGateway.progress.tradeLedgerState.hasCompleted("trade-1"))
    }

    @Test
    fun `apply trade adds incoming card to user library and resolves it for success screen`() = runTest {
        val fixture = testTradeRepository(
            collection = ownedCollectionWithVariants(
                "ALP-001",
                OwnedVariantCount("city", "standard", 2),
            ),
        )
        val outgoing = TradeCardRef("ALP-001", "city", "standard")
        val incoming = TradeCardRef("ALP-002", "city", "standard")

        val result = fixture.repository.applyTrade(
            tradeId = "trade-2",
            outgoing = outgoing,
            incoming = incoming,
        )
        val receivedCard = fixture.repository.loadTradeCard(incoming)

        assertEquals(TradeValidationResult.Valid, result)
        assertEquals(1, fixture.progressGateway.progress.collection.tradeCountFor(incoming))
        assertEquals("ALP-002", receivedCard?.definition?.id)
        assertEquals("Astronomes en herbe", receivedCard?.extensionName)
        assertEquals(1, receivedCard?.activeVariant?.count)
    }

    private fun testTradeRepository(
        collection: fr.aumombelli.dstcg.model.OwnedCollection,
    ): TradeRepositoryFixture {
        val progressGateway = FakeProgressGateway().apply {
            progress = StandaloneProgress(
                collection = collection,
            )
        }
        val catalogGateway = FakeCatalogGateway().apply {
            extensions = listOf(
                ExtensionDefinition(
                    id = "astronomes-en-herbe",
                    name = "Astronomes en herbe",
                    coverImageRef = "cover",
                ),
            )
            cards = listOf(
                testCardDefinition("ALP-001", rarityLabel = "Common"),
                testCardDefinition("ALP-002", rarityLabel = "Common"),
            )
            variantProfiles = testVariantProfiles()
        }
        return TradeRepositoryFixture(
            progressGateway = progressGateway,
            repository = TradeRepository(
                catalogRepository = catalogGateway,
                progressRepository = progressGateway,
            ),
        )
    }

    private data class TradeRepositoryFixture(
        val progressGateway: FakeProgressGateway,
        val repository: TradeRepository,
    )
}
