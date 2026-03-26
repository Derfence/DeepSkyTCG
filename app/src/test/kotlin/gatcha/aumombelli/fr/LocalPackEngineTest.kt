package fr.aumombelli.gatcha

import fr.aumombelli.gatcha.data.LocalPackEngine
import fr.aumombelli.gatcha.data.PackCooldownException
import fr.aumombelli.gatcha.data.StandaloneGameSettings
import fr.aumombelli.gatcha.model.CardFinishDefinition
import fr.aumombelli.gatcha.model.SkyQualityDefinition
import fr.aumombelli.gatcha.model.VariantProfile
import fr.aumombelli.gatcha.model.WeightedCode
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset
import kotlin.random.Random
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalPackEngineTest {
    private val fixedNow = Instant.parse("2026-03-24T12:00:00Z")

    @Test
    fun `draw pack resolves local card variants from the catalog`() = runTest {
        val catalogGateway = FakeCatalogGateway().apply {
            cards = listOf(
                testCardDefinition(
                    id = "ALP-001",
                    name = "Nebuleuse d'Orion",
                    variantProfileId = "local-pack-profile",
                ),
            )
            variantProfiles = listOf(localPackProfile())
        }
        val engine = LocalPackEngine(
            catalogRepository = catalogGateway,
            settings = StandaloneGameSettings(
                cardsPerPack = 2,
                clock = fixedClock(),
                random = Random(0),
            ),
        )

        val response = engine.drawPack("astronomes-en-herbe", nextDrawAt = null)

        assertEquals("2026-03-24T12:00:00Z", response.drawnAt)
        assertEquals("2026-03-25T00:00:00Z", response.nextDrawAt)
        assertEquals(listOf("ALP-001", "ALP-001"), response.cards.map { it.cardId })
        assertEquals(listOf("Montagne", "Montagne"), response.cards.map { it.variant.skyQualityLabel })
        assertEquals(listOf("Holographique", "Holographique"), response.cards.map { it.variant.finishLabel })
    }

    @Test
    fun `draw pack favors higher weighted cards across repeated draws`() = runTest {
        val catalogGateway = FakeCatalogGateway().apply {
            cards = listOf(
                testCardDefinition(id = "HEAVY", drawWeight = 100, variantProfileId = "local-pack-profile"),
                testCardDefinition(id = "LIGHT", drawWeight = 1, variantProfileId = "local-pack-profile"),
            )
            variantProfiles = listOf(localPackProfile())
        }
        val engine = LocalPackEngine(
            catalogRepository = catalogGateway,
            settings = StandaloneGameSettings(
                cardsPerPack = 1,
                drawCooldown = Duration.ZERO,
                clock = fixedClock(),
                random = Random(1234),
            ),
        )

        val counts = mutableMapOf("HEAVY" to 0, "LIGHT" to 0)
        repeat(200) {
            val cardId = engine.drawPack("astronomes-en-herbe", nextDrawAt = null).cards.single().cardId
            counts[cardId] = counts.getValue(cardId) + 1
        }

        assertTrue(counts.getValue("HEAVY") > counts.getValue("LIGHT"))
    }

    @Test
    fun `draw pack blocks when cooldown is still active`() = runTest {
        val engine = LocalPackEngine(
            catalogRepository = FakeCatalogGateway(),
            settings = StandaloneGameSettings(clock = fixedClock()),
        )

        val exception = try {
            engine.drawPack("astronomes-en-herbe", nextDrawAt = "2026-03-24T18:00:00Z")
            error("Expected PackCooldownException")
        } catch (error: PackCooldownException) {
            error
        }

        assertEquals("2026-03-24T18:00:00Z", exception.retryAt)
    }

    @Test
    fun `draw pack fails when no cards exist for an extension`() = runTest {
        val catalogGateway = FakeCatalogGateway().apply {
            cards = emptyList()
            variantProfiles = listOf(localPackProfile())
        }
        val engine = LocalPackEngine(
            catalogRepository = catalogGateway,
            settings = StandaloneGameSettings(clock = fixedClock()),
        )

        val exception = try {
            engine.drawPack("astronomes-en-herbe", nextDrawAt = null)
            error("Expected IllegalStateException")
        } catch (error: IllegalStateException) {
            error
        }

        assertEquals("No cards were found for this extension.", exception.message)
    }

    private fun fixedClock(): Clock = Clock.fixed(fixedNow, ZoneOffset.UTC)

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
