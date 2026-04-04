package fr.aumombelli.dstcg

import fr.aumombelli.dstcg.data.CollectionRepository
import fr.aumombelli.dstcg.data.LocalPackEngine
import fr.aumombelli.dstcg.data.PackRepository
import fr.aumombelli.dstcg.model.CardFinishDefinition
import fr.aumombelli.dstcg.model.SkyQualityDefinition
import fr.aumombelli.dstcg.model.StandaloneProgress
import fr.aumombelli.dstcg.model.VariantProfile
import fr.aumombelli.dstcg.model.WeightedCode
import java.time.Duration
import java.time.Instant
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class PackRepositoryTest {
    @Test
    fun `open pack persists merged collection pack count next draw timestamp and current pack result`() = runTest {
        val fixedNow = Instant.parse("2026-03-24T12:00:00Z")
        val progressGateway = FakeProgressGateway().apply {
            progress = StandaloneProgress(
                collection = ownedCollectionOf("ALP-001" to 1),
                availableDrawCount = 10,
                nextChargeAt = null,
            )
        }
        val catalogGateway = FakeCatalogGateway().apply {
            cards = listOf(
                testCardDefinition("ALP-001", name = "Nebuleuse d'Orion", variantProfileId = "local-pack-profile"),
                testCardDefinition("ALP-002", name = "Galaxie d'Andromede", variantProfileId = "local-pack-profile"),
            )
            variantProfiles = listOf(localPackProfile())
        }
        val repository = PackRepository(
            progressRepository = progressGateway,
            collectionRepository = CollectionRepository(progressGateway),
            localPackEngine = LocalPackEngine(
                catalogRepository = catalogGateway,
                settings = testGameSettings(
                    cardsPerPack = 2,
                    now = fixedNow,
                    drawCooldown = Duration.ofHours(6),
                    maxStoredDraws = 10,
                    randomSeed = 4,
                ),
            ),
        )

        val response = repository.openPack("astronomes-en-herbe")

        assertEquals(response, repository.currentPackResult().value)
        assertEquals(response.availableDrawCount, progressGateway.progress.availableDrawCount)
        assertEquals(response.nextChargeAt, progressGateway.progress.nextChargeAt)
        assertEquals(1, progressGateway.progress.openedPackCount)
        assertEquals(3, progressGateway.progress.collection.cards.values.sumOf { it.totalOwned })
    }

    private fun localPackProfile(): VariantProfile = VariantProfile(
        id = "local-pack-profile",
        skyQualities = listOf(
            SkyQualityDefinition("city", "Ville"),
            SkyQualityDefinition("mountain", "Montagne"),
        ),
        finishes = listOf(
            CardFinishDefinition("standard", "Standard"),
            CardFinishDefinition("holographic", "Holographique", isHolographic = true),
        ),
        skyQualityWeights = listOf(
            WeightedCode("mountain", 100),
            WeightedCode("city", 1),
        ),
        finishWeights = listOf(
            WeightedCode("holographic", 100),
            WeightedCode("standard", 1),
        ),
    )
}
