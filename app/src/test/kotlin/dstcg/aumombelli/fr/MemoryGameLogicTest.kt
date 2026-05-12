package fr.aumombelli.dstcg

import fr.aumombelli.dstcg.feature.minigames.MemoryBoardBuildResult
import fr.aumombelli.dstcg.feature.minigames.MemoryCardRole
import fr.aumombelli.dstcg.feature.minigames.buildMemoryBoard
import fr.aumombelli.dstcg.model.CardDefinition
import fr.aumombelli.dstcg.model.ExtensionDefinition
import fr.aumombelli.dstcg.model.MiniGameDifficulty
import fr.aumombelli.dstcg.model.MiniGameGlobalCardRef
import fr.aumombelli.dstcg.model.MiniGameOwnedVariantRef
import fr.aumombelli.dstcg.model.MiniGameResolvedCardRef
import fr.aumombelli.dstcg.model.OwnedCardEntry
import fr.aumombelli.dstcg.model.OwnedCollection
import fr.aumombelli.dstcg.model.OwnedVariantCount
import fr.aumombelli.dstcg.model.VariantProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MemoryGameLogicTest {
    private val extension = ExtensionDefinition(
        id = "alpha",
        name = "Alpha",
        coverImageRef = "cover",
    )
    private val variantProfiles = testVariantProfiles()

    @Test
    fun `apprentice board duplicates each selected variant exactly twice`() {
        val cards = testCards(2)
        val result = buildReadyBoard(
            difficulty = MiniGameDifficulty.Apprentice,
            cards = cards,
            collection = ownedCollection(
                "ALP-001" to listOf(OwnedVariantCount("city", "standard", 1)),
                "ALP-002" to listOf(OwnedVariantCount("suburban", "standard", 1)),
            ),
            resolvedPairCards = listOf(
                resolved("ALP-001", "city"),
                resolved("ALP-002", "suburban"),
            ),
        )

        assertEquals(4, result.cards.size)
        assertEquals(2, result.columns)
        assertEquals(2, result.rows)
        assertEquals(
            listOf(2, 2),
            result.cards
                .groupingBy { it.identity.key }
                .eachCount()
                .values
                .sorted(),
        )
        assertTrue(result.cards.all { it.role == MemoryCardRole.Pair })
    }

    @Test
    fun `odd board prefers an owned holographic singleton`() {
        val cards = testCards(5)
        val result = buildReadyBoard(
            difficulty = MiniGameDifficulty.Observer,
            cards = cards,
            collection = ownedCollection(
                "ALP-001" to listOf(OwnedVariantCount("city", "standard", 1)),
                "ALP-002" to listOf(OwnedVariantCount("city", "standard", 1)),
                "ALP-003" to listOf(OwnedVariantCount("city", "standard", 1)),
                "ALP-004" to listOf(OwnedVariantCount("city", "standard", 1)),
                "ALP-005" to listOf(OwnedVariantCount("holographic", "standard", 1)),
            ),
            resolvedPairCards = listOf(
                resolved("ALP-001"),
                resolved("ALP-002"),
                resolved("ALP-003"),
                resolved("ALP-004"),
            ),
        )

        val singleton = result.cards.single { it.role == MemoryCardRole.HolographicSingleton }
        assertEquals("ALP-005", singleton.identity.cardId)
        assertTrue(singleton.displayCard.activeVariant.isHolographic)
        assertFalse(singleton.isVisualHolographicFallback)
    }

    @Test
    fun `odd board uses visual holographic fallback when no owned holo exists`() {
        val cards = testCards(5)
        val result = buildReadyBoard(
            difficulty = MiniGameDifficulty.Observer,
            cards = cards,
            collection = ownedCollection(
                "ALP-001" to listOf(OwnedVariantCount("city", "standard", 1)),
                "ALP-002" to listOf(OwnedVariantCount("city", "standard", 1)),
                "ALP-003" to listOf(OwnedVariantCount("city", "standard", 1)),
                "ALP-004" to listOf(OwnedVariantCount("city", "standard", 1)),
                "ALP-005" to listOf(OwnedVariantCount("suburban", "standard", 1)),
            ),
            resolvedPairCards = listOf(
                resolved("ALP-001"),
                resolved("ALP-002"),
                resolved("ALP-003"),
                resolved("ALP-004"),
            ),
        )

        val singleton = result.cards.single { it.role == MemoryCardRole.HolographicSingleton }
        assertTrue(singleton.displayCard.activeVariant.isHolographic)
        assertTrue(singleton.isVisualHolographicFallback)
    }

    @Test
    fun `board is unavailable when distinct pair cards are missing`() {
        val cards = testCards(1)
        val result = buildMemoryBoard(
            difficulty = MiniGameDifficulty.Apprentice,
            dateUtc = "2026-05-10",
            resolvedPairCards = listOf(
                resolved("ALP-001"),
                resolved("ALP-001"),
            ),
            cards = cards,
            extensions = listOf(extension),
            variantProfiles = variantProfiles,
            collection = ownedCollection(
                "ALP-001" to listOf(OwnedVariantCount("city", "standard", 1)),
            ),
        )

        assertTrue(result is MemoryBoardBuildResult.Unavailable)
    }

    @Test
    fun `board fills duplicate resolved pairs from owned collection`() {
        val cards = testCards(2)
        val result = buildReadyBoard(
            difficulty = MiniGameDifficulty.Apprentice,
            cards = cards,
            collection = ownedCollection(
                "ALP-001" to listOf(OwnedVariantCount("city", "standard", 1)),
                "ALP-002" to listOf(OwnedVariantCount("city", "standard", 1)),
            ),
            resolvedPairCards = listOf(
                resolved("ALP-001"),
                resolved("ALP-001"),
            ),
        )

        assertEquals(2, result.cards.map { it.identity.key }.toSet().size)
    }

    @Test
    fun `board forbids selecting the same card through two variants`() {
        val cards = testCards(2)
        val result = buildReadyBoard(
            difficulty = MiniGameDifficulty.Apprentice,
            cards = cards,
            collection = ownedCollection(
                "ALP-001" to listOf(
                    OwnedVariantCount("city", "standard", 1),
                    OwnedVariantCount("suburban", "standard", 1),
                ),
                "ALP-002" to listOf(OwnedVariantCount("city", "standard", 1)),
            ),
            resolvedPairCards = listOf(
                resolved("ALP-001", "city"),
                resolved("ALP-001", "suburban"),
            ),
        )

        assertEquals(2, result.cards.map { it.identity.cardKey }.toSet().size)
        assertEquals(2, result.cards.count { it.identity.cardId == "ALP-001" })
        assertEquals(2, result.cards.count { it.identity.cardId == "ALP-002" })
    }

    @Test
    fun `holographic singleton avoids cards already selected as pairs`() {
        val cards = testCards(5)
        val result = buildReadyBoard(
            difficulty = MiniGameDifficulty.Observer,
            cards = cards,
            collection = ownedCollection(
                "ALP-001" to listOf(
                    OwnedVariantCount("city", "standard", 1),
                    OwnedVariantCount("holographic", "standard", 1),
                ),
                "ALP-002" to listOf(OwnedVariantCount("city", "standard", 1)),
                "ALP-003" to listOf(OwnedVariantCount("city", "standard", 1)),
                "ALP-004" to listOf(OwnedVariantCount("city", "standard", 1)),
                "ALP-005" to listOf(OwnedVariantCount("holographic", "standard", 1)),
            ),
            resolvedPairCards = listOf(
                resolved("ALP-001", "city"),
                resolved("ALP-002"),
                resolved("ALP-003"),
                resolved("ALP-004"),
            ),
        )

        val singleton = result.cards.single { it.role == MemoryCardRole.HolographicSingleton }
        assertEquals("ALP-005", singleton.identity.cardId)
    }

    private fun buildReadyBoard(
        difficulty: MiniGameDifficulty,
        cards: List<CardDefinition>,
        collection: OwnedCollection,
        resolvedPairCards: List<MiniGameResolvedCardRef>,
    ) = when (
        val result = buildMemoryBoard(
            difficulty = difficulty,
            dateUtc = "2026-05-10",
            resolvedPairCards = resolvedPairCards,
            cards = cards,
            extensions = listOf(extension),
            variantProfiles = variantProfiles,
            collection = collection,
        )
    ) {
        is MemoryBoardBuildResult.Ready -> result.board
        is MemoryBoardBuildResult.Unavailable -> error(result.message)
    }

    private fun testCards(count: Int): List<CardDefinition> =
        (1..count).map { index ->
            testCardDefinition(
                id = "ALP-${index.toString().padStart(3, '0')}",
                extensionId = extension.id,
                name = "Carte $index",
            )
        }

    private fun resolved(
        cardId: String,
        skyQuality: String = "city",
        finish: String = "standard",
    ): MiniGameResolvedCardRef = MiniGameResolvedCardRef(
        globalCard = MiniGameGlobalCardRef(
            cardId = cardId,
            extensionId = extension.id,
        ),
        ownedVariant = MiniGameOwnedVariantRef(
            cardId = cardId,
            extensionId = extension.id,
            skyQuality = skyQuality,
            finish = finish,
        ),
        source = fr.aumombelli.dstcg.model.MiniGameCardResolutionSource.GlobalCard,
    )

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
