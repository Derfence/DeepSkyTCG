package fr.aumombelli.dstcg

import fr.aumombelli.dstcg.data.MiniGameDeterministicDrawPolicy
import fr.aumombelli.dstcg.model.MiniGameId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MiniGameDeterministicDrawPolicyTest {
    private val policy = MiniGameDeterministicDrawPolicy()
    private val cards = listOf(
        testCardDefinition(id = "ALP-001", extensionId = "alpha"),
        testCardDefinition(id = "ALP-002", extensionId = "alpha"),
        testCardDefinition(id = "BET-001", extensionId = "beta"),
        testCardDefinition(id = "BET-002", extensionId = "beta"),
    )

    @Test
    fun `same game date and slot draws the same global card`() {
        val first = policy.drawGlobalCard(
            cards = cards,
            miniGameId = MiniGameId.Quiz,
            dateUtc = "2026-05-10",
            slot = 0,
        )
        val second = policy.drawGlobalCard(
            cards = cards.reversed(),
            miniGameId = MiniGameId.Quiz,
            dateUtc = "2026-05-10",
            slot = 0,
        )

        assertEquals(first, second)
    }

    @Test
    fun `draw does not depend on player collection`() {
        val globalCard = policy.drawGlobalCard(
            cards = cards,
            miniGameId = MiniGameId.Memory,
            dateUtc = "2026-05-10",
            slot = 1,
        )

        val sameGlobalCardForAnotherPlayer = policy.drawGlobalCard(
            cards = cards,
            miniGameId = MiniGameId.Memory,
            dateUtc = "2026-05-10",
            slot = 1,
        )

        assertEquals(globalCard, sameGlobalCardForAnotherPlayer)
    }

    @Test
    fun `multiple slots avoid duplicates when enough candidates exist`() {
        val drawnCards = policy.drawGlobalCards(
            cards = cards,
            miniGameId = MiniGameId.Timeline,
            dateUtc = "2026-05-10",
            slotCount = 3,
        )

        assertEquals(3, drawnCards.size)
        assertEquals(3, drawnCards.map { it.cardId }.toSet().size)
    }

    @Test
    fun `extension filter limits global draw candidates`() {
        val drawnCards = policy.drawGlobalCards(
            cards = cards,
            miniGameId = MiniGameId.Observatory,
            dateUtc = "2026-05-10",
            slotCount = 2,
            extensionId = "beta",
        )

        assertEquals(setOf("beta"), drawnCards.map { it.extensionId }.toSet())
        assertTrue(drawnCards.isNotEmpty())
    }
}
