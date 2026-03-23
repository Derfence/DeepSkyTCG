package gatcha.aumombelli.fr

import gatcha.aumombelli.fr.model.OwnedCollection
import gatcha.aumombelli.fr.model.PackCard
import gatcha.aumombelli.fr.model.mergePackCards
import org.junit.Assert.assertEquals
import org.junit.Test

class CollectionRepositoryTest {
    @Test
    fun `merge cards increments owned counts`() {
        val merged = OwnedCollection(cards = mapOf("ALP-001" to 1)).mergePackCards(
            cards = listOf(
                PackCard("ALP-001", "Spark Fox", "Common", "spark_fox"),
                PackCard("MON-006", "Eclipse Regent", "Epic", "eclipse_regent"),
            ),
        )

        assertEquals(2, merged.cards["ALP-001"])
        assertEquals(1, merged.cards["MON-006"])
    }
}
