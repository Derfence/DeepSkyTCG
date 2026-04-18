package fr.aumombelli.dstcg

import fr.aumombelli.dstcg.data.LocalPackEngine
import fr.aumombelli.dstcg.data.PackCooldownException
import fr.aumombelli.dstcg.data.DeterministicWeatherCalendar
import fr.aumombelli.dstcg.data.buildPackChargeUiStatus
import fr.aumombelli.dstcg.model.CardFinishDefinition
import fr.aumombelli.dstcg.model.SkyQualityDefinition
import fr.aumombelli.dstcg.model.StandaloneProgress
import fr.aumombelli.dstcg.model.VariantProfile
import java.time.Duration
import java.time.Instant
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
                testCardDefinition(
                    id = "ALP-002",
                    name = "Galaxie d'Andromede",
                    variantProfileId = "local-pack-profile",
                ),
            )
            variantProfiles = listOf(localPackProfile())
            gameBalance = localPackBalance(cardsPerDraw = 2)
        }
        val engine = LocalPackEngine(
            catalogRepository = catalogGateway,
            settings = testGameSettings(
                now = fixedNow,
                randomSeed = 0,
            ),
        )

        val response = engine.drawPack(
            extensionId = "astronomes-en-herbe",
            progress = testProgress(testRechargeState()),
            now = fixedNow,
        )
        val chargeStatus = buildPackChargeUiStatus(
            rechargeState = response.rechargeState,
            now = fixedNow,
            drawCooldown = Duration.ofHours(6),
            maxStoredDraws = 10,
            weatherPolicy = DeterministicWeatherCalendar,
        )

        assertEquals("2026-03-24T12:00:00Z", response.drawnAt)
        assertEquals(9, response.rechargeState.availableDrawCount)
        assertEquals("2026-03-24T18:00:00Z", chargeStatus.nextChargeAt)
        assertEquals(setOf("ALP-001", "ALP-002"), response.cards.map { it.cardId }.toSet())
        assertEquals(2, response.cards.map { it.cardId }.distinct().size)
        assertEquals(listOf("Holographique", "Holographique"), response.cards.map { it.variant.skyQualityLabel })
        assertEquals(listOf("Tamponnee", "Tamponnee"), response.cards.map { it.variant.finishLabel })
    }

    @Test
    fun `draw pack never duplicates the same astronomy card inside a pack`() = runTest {
        val catalogGateway = FakeCatalogGateway().apply {
            cards = listOf(
                testCardDefinition(
                    id = "ALP-001",
                    rarityLabel = "Common",
                    cardRarityMultiplier = 100.0,
                    variantProfileId = "local-pack-profile",
                ),
                testCardDefinition(
                    id = "ALP-002",
                    rarityLabel = "Common",
                    cardRarityMultiplier = 1.0,
                    variantProfileId = "local-pack-profile",
                ),
            )
            variantProfiles = listOf(localPackProfile())
            gameBalance = localPackBalance(cardsPerDraw = 2)
        }
        val engine = LocalPackEngine(
            catalogRepository = catalogGateway,
            settings = testGameSettings(
                now = fixedNow,
                randomSeed = 0,
            ),
        )

        val response = engine.drawPack(
            extensionId = "astronomes-en-herbe",
            progress = testProgress(testRechargeState()),
            now = fixedNow,
        )

        assertEquals(2, response.cards.size)
        assertEquals(2, response.cards.map { it.cardId }.distinct().size)
    }

    @Test
    fun `draw pack favors higher weighted cards across repeated draws`() = runTest {
        val catalogGateway = FakeCatalogGateway().apply {
            cards = listOf(
                testCardDefinition(id = "HEAVY", cardRarityMultiplier = 100.0, variantProfileId = "local-pack-profile"),
                testCardDefinition(id = "LIGHT", cardRarityMultiplier = 1.0, variantProfileId = "local-pack-profile"),
            )
            variantProfiles = listOf(localPackProfile())
            gameBalance = localPackBalance(cardsPerDraw = 1)
        }
        val engine = LocalPackEngine(
            catalogRepository = catalogGateway,
            settings = testGameSettings(
                now = fixedNow,
                randomSeed = 1234,
            ),
        )

        val counts = mutableMapOf("HEAVY" to 0, "LIGHT" to 0)
        repeat(200) {
            val cardId = engine.drawPack(
                extensionId = "astronomes-en-herbe",
                progress = testProgress(testRechargeState()),
                now = fixedNow,
            ).cards.single().cardId
            counts[cardId] = counts.getValue(cardId) + 1
        }

        assertTrue(counts.getValue("HEAVY") > counts.getValue("LIGHT"))
    }

    @Test
    fun `draw pack blocks when no charge is available yet`() = runTest {
        val engine = LocalPackEngine(
            catalogRepository = FakeCatalogGateway(),
            settings = testGameSettings(now = fixedNow),
        )

        val exception = try {
            engine.drawPack(
                extensionId = "astronomes-en-herbe",
                progress = testProgress(
                    testRechargeStateWithNextChargeAt(
                        availableDrawCount = 0,
                        nextChargeAt = "2026-03-24T18:00:00Z",
                    ),
                ),
                now = fixedNow,
            )
            error("Expected PackCooldownException")
        } catch (error: PackCooldownException) {
            error
        }

        assertEquals("2026-03-24T18:00:00Z", exception.retryAt)
    }

    @Test
    fun `draw pack preserves an existing recharge chain when stock is already partial`() = runTest {
        val catalogGateway = FakeCatalogGateway().apply {
            cards = listOf(
                testCardDefinition(
                    id = "ALP-001",
                    name = "Nebuleuse d'Orion",
                    variantProfileId = "local-pack-profile",
                ),
            )
            variantProfiles = listOf(localPackProfile())
            gameBalance = localPackBalance(cardsPerDraw = 1)
        }
        val engine = LocalPackEngine(
            catalogRepository = catalogGateway,
            settings = testGameSettings(
                now = fixedNow,
                randomSeed = 0,
            ),
        )

        val response = engine.drawPack(
            extensionId = "astronomes-en-herbe",
            progress = testProgress(
                testRechargeStateWithNextChargeAt(
                    availableDrawCount = 5,
                    nextChargeAt = "2026-03-24T14:00:00Z",
                ),
            ),
            now = fixedNow,
        )
        val chargeStatus = buildPackChargeUiStatus(
            rechargeState = response.rechargeState,
            now = fixedNow,
            drawCooldown = Duration.ofHours(6),
            maxStoredDraws = 10,
            weatherPolicy = DeterministicWeatherCalendar,
        )

        assertEquals(4, response.rechargeState.availableDrawCount)
        assertEquals("2026-03-24T14:00:00Z", chargeStatus.nextChargeAt)
    }

    @Test
    fun `draw pack replenishes elapsed charges before consuming one and caps at ten`() = runTest {
        val catalogGateway = FakeCatalogGateway().apply {
            cards = listOf(
                testCardDefinition(
                    id = "ALP-001",
                    name = "Nebuleuse d'Orion",
                    variantProfileId = "local-pack-profile",
                ),
            )
            variantProfiles = listOf(localPackProfile())
            gameBalance = localPackBalance(cardsPerDraw = 1)
        }
        val engine = LocalPackEngine(
            catalogRepository = catalogGateway,
            settings = testGameSettings(
                now = fixedNow,
                randomSeed = 0,
            ),
        )

        val response = engine.drawPack(
            extensionId = "astronomes-en-herbe",
            progress = testProgress(
                testRechargeStateWithNextChargeAt(
                    availableDrawCount = 7,
                    nextChargeAt = "2026-03-24T00:00:00Z",
                ),
            ),
            now = fixedNow,
        )
        val chargeStatus = buildPackChargeUiStatus(
            rechargeState = response.rechargeState,
            now = fixedNow,
            drawCooldown = Duration.ofHours(6),
            maxStoredDraws = 10,
            weatherPolicy = DeterministicWeatherCalendar,
        )

        assertEquals(9, response.rechargeState.availableDrawCount)
        assertEquals("2026-03-24T18:00:00Z", chargeStatus.nextChargeAt)
    }

    @Test
    fun `draw pack fails when no cards exist for an extension`() = runTest {
        val catalogGateway = FakeCatalogGateway().apply {
            cards = emptyList()
            variantProfiles = listOf(localPackProfile())
        }
        val engine = LocalPackEngine(
            catalogRepository = catalogGateway,
            settings = testGameSettings(now = fixedNow),
        )

        val exception = try {
            engine.drawPack(
                extensionId = "astronomes-en-herbe",
                progress = testProgress(testRechargeState()),
                now = fixedNow,
            )
            error("Expected IllegalStateException")
        } catch (error: IllegalStateException) {
            error
        }

        assertEquals("Aucune carte n'a ete trouvee pour cette extension.", exception.message)
    }
    private fun localPackProfile(): VariantProfile = VariantProfile(
        id = "local-pack-profile",
        skyQualities = listOf(
            SkyQualityDefinition("holographic", "Holographique", isHolographic = true),
        ),
        finishes = listOf(
            CardFinishDefinition("stamped", "Tamponnee", isStamped = true),
        ),
    )

    private fun localPackBalance(cardsPerDraw: Int) = testGameBalanceDefinition(
        cardsPerDraw = cardsPerDraw,
        suburbanMeanPerDay = 1.0,
        ruralMeanPerDay = 1.0,
        mountainMeanPerDay = 1.0,
    )

    private fun testProgress(rechargeState: fr.aumombelli.dstcg.model.PackRechargeState): StandaloneProgress =
        StandaloneProgress(
            collection = ownedCollectionOf(),
            rechargeState = rechargeState,
        )
}
