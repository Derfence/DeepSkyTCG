package fr.aumombelli.dstcg

import fr.aumombelli.dstcg.data.CollectionRepository
import fr.aumombelli.dstcg.model.StandaloneProgress
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class CollectionRepositoryTest {
    @Test
    fun `load collection returns persisted local collection`() = runTest {
        val progressGateway = FakeProgressGateway().apply {
            progress = StandaloneProgress(
                collection = ownedCollectionOf("ALP-001" to 1),
                availableDrawCount = 4,
                nextChargeAt = "2026-03-25T00:00:00Z",
            )
        }
        val repository = CollectionRepository(progressGateway)

        val collection = repository.loadCollection()

        assertEquals(1, collection.cards["ALP-001"]?.totalOwned)
    }

    @Test
    fun `save collection preserves stored draw charges inside progress`() = runTest {
        val progressGateway = FakeProgressGateway().apply {
            progress = StandaloneProgress(
                collection = ownedCollectionOf("ALP-001" to 1),
                availableDrawCount = 4,
                nextChargeAt = "2026-03-25T00:00:00Z",
            )
        }
        val repository = CollectionRepository(progressGateway)
        val newCollection = ownedCollectionOf("ALP-002" to 3)

        repository.saveCollection(newCollection)

        assertEquals(newCollection, progressGateway.progress.collection)
        assertEquals(4, progressGateway.progress.availableDrawCount)
        assertEquals("2026-03-25T00:00:00Z", progressGateway.progress.nextChargeAt)
    }

    @Test
    fun `merge cards increments owned counts by variant`() {
        val repository = CollectionRepository(FakeProgressGateway())

        val merged = repository.mergeCards(
            collection = ownedCollectionOf("ALP-001" to 1),
            cards = listOf(
                testPackCard(
                    cardId = "ALP-001",
                    name = "Nebuleuse d'Orion",
                    rarityLabel = "Common",
                    imageRef = "m42",
                    skyQuality = "rural",
                    skyQualityLabel = "Campagne",
                ),
                testPackCard(
                    cardId = "MON-006",
                    name = "Sirius",
                    rarityLabel = "Epic",
                    imageRef = "sirius",
                    finish = "holographic",
                    finishLabel = "Holographique",
                    isHolographic = true,
                ),
            ),
        )

        assertEquals(2, merged.cards["ALP-001"]?.totalOwned)
        assertEquals(1, merged.cards["MON-006"]?.totalOwned)
        assertEquals(2, merged.cards["ALP-001"]?.variants?.size)
        assertEquals("holographic", merged.cards["MON-006"]?.variants?.single()?.finish)
    }
}
