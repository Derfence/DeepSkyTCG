package fr.aumombelli.dstcg

import fr.aumombelli.dstcg.data.MiniGameCardResolver
import fr.aumombelli.dstcg.model.MiniGameCardResolutionSource
import fr.aumombelli.dstcg.model.MiniGameGlobalCardRef
import fr.aumombelli.dstcg.model.MiniGameId
import fr.aumombelli.dstcg.model.OwnedCardEntry
import fr.aumombelli.dstcg.model.OwnedCollection
import fr.aumombelli.dstcg.model.OwnedVariantCount
import org.junit.Assert.assertEquals
import org.junit.Test

class MiniGameCardResolverTest {
    private val resolver = MiniGameCardResolver()
    private val cards = listOf(
        testCardDefinition(id = "ALP-001", extensionId = "alpha"),
        testCardDefinition(id = "ALP-002", extensionId = "alpha"),
        testCardDefinition(id = "BET-001", extensionId = "beta"),
    )

    @Test
    fun `owned global card is used first`() {
        val resolved = resolver.resolve(
            globalCard = MiniGameGlobalCardRef(cardId = "ALP-001", extensionId = "alpha"),
            cards = cards,
            collection = ownedCollection(
                "ALP-001" to listOf(OwnedVariantCount("city", "standard", 1)),
                "ALP-002" to listOf(OwnedVariantCount("city", "standard", 1)),
            ),
            miniGameId = MiniGameId.Quiz,
            dateUtc = "2026-05-10",
            slot = 0,
        )

        requireNotNull(resolved)
        assertEquals(MiniGameCardResolutionSource.GlobalCard, resolved.source)
        assertEquals("ALP-001", resolved.ownedVariant.cardId)
    }

    @Test
    fun `fallback stays in the same extension when global card is missing`() {
        val resolved = resolver.resolve(
            globalCard = MiniGameGlobalCardRef(cardId = "ALP-001", extensionId = "alpha"),
            cards = cards,
            collection = ownedCollection(
                "ALP-002" to listOf(OwnedVariantCount("city", "standard", 1)),
                "BET-001" to listOf(OwnedVariantCount("city", "standard", 1)),
            ),
            miniGameId = MiniGameId.Memory,
            dateUtc = "2026-05-10",
            slot = 0,
        )

        requireNotNull(resolved)
        assertEquals(MiniGameCardResolutionSource.SameExtensionFallback, resolved.source)
        assertEquals("alpha", resolved.ownedVariant.extensionId)
    }

    @Test
    fun `fallback uses other extensions when selected extension is absent from collection`() {
        val resolved = resolver.resolve(
            globalCard = MiniGameGlobalCardRef(cardId = "ALP-001", extensionId = "alpha"),
            cards = cards,
            collection = ownedCollection(
                "BET-001" to listOf(OwnedVariantCount("city", "standard", 1)),
            ),
            miniGameId = MiniGameId.Timeline,
            dateUtc = "2026-05-10",
            slot = 0,
        )

        requireNotNull(resolved)
        assertEquals(MiniGameCardResolutionSource.AnyExtensionFallback, resolved.source)
        assertEquals("beta", resolved.ownedVariant.extensionId)
    }

    @Test
    fun `fallback resolution is stable`() {
        val globalCard = MiniGameGlobalCardRef(cardId = "ALP-001", extensionId = "alpha")
        val collection = ownedCollection(
            "BET-001" to listOf(
                OwnedVariantCount("city", "standard", 1),
                OwnedVariantCount("rural", "standard", 1),
            ),
        )

        val first = resolver.resolve(
            globalCard = globalCard,
            cards = cards,
            collection = collection,
            miniGameId = MiniGameId.Observatory,
            dateUtc = "2026-05-10",
            slot = 2,
        )
        val second = resolver.resolve(
            globalCard = globalCard,
            cards = cards.reversed(),
            collection = collection,
            miniGameId = MiniGameId.Observatory,
            dateUtc = "2026-05-10",
            slot = 2,
        )

        assertEquals(first, second)
    }

    private fun ownedCollection(
        vararg cards: Pair<String, List<OwnedVariantCount>>,
    ): OwnedCollection = OwnedCollection(
        cards = cards.associate { (cardId, variants) ->
            cardId to OwnedCardEntry(
                totalOwned = variants.sumOf { it.count },
                variants = variants,
            )
        },
    )
}
