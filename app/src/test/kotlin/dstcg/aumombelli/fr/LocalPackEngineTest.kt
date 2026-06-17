package fr.aumombelli.dstcg

import fr.aumombelli.dstcg.data.ClockTrustedTimeSource
import fr.aumombelli.dstcg.data.LocalPackEngine
import fr.aumombelli.dstcg.data.PackCooldownException
import fr.aumombelli.dstcg.data.DeterministicWeatherCalendar
import fr.aumombelli.dstcg.data.EntropySource
import fr.aumombelli.dstcg.data.StandaloneGameSettings
import fr.aumombelli.dstcg.data.buildPackChargeUiStatus
import fr.aumombelli.dstcg.model.CardFinishDefinition
import fr.aumombelli.dstcg.model.NewPlayerOnboardingStep
import fr.aumombelli.dstcg.model.SkyQualityDefinition
import fr.aumombelli.dstcg.model.StandaloneProgress
import fr.aumombelli.dstcg.model.VariantProfile
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset
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
            gameBalance = localPackBalance(
                cardsPerDraw = 1,
                holographicSkyMeanPerDay = 3.9996,
                percentStampedPerDay = 99.9999,
                suburbanMeanPerDay = 0.0001,
                ruralMeanPerDay = 0.0001,
                mountainMeanPerDay = 0.0001,
            )
        }
        val engine = LocalPackEngine(
            catalogRepository = catalogGateway,
            settings = queuedGameSettings(0, 0, 4, 1),
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
        assertEquals(listOf("ALP-001"), response.cards.map { it.cardId })
        assertEquals(listOf("Holographique"), response.cards.map { it.variant.skyQualityLabel })
        assertEquals(listOf("Tamponnee"), response.cards.map { it.variant.finishLabel })
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

    @Test
    fun `draw pack uses Epic boosted rarity distribution when selected pack is boosted`() = runTest {
        val catalogGateway = FakeCatalogGateway().apply {
            cards = allRarityCards()
            variantProfiles = listOf(localPackProfile())
            gameBalance = localPackBalance(cardsPerDraw = 1)
        }
        val engine = LocalPackEngine(
            catalogRepository = catalogGateway,
            settings = queuedGameSettings(950_000),
        )

        val response = engine.drawPack(
            extensionId = "astronomes-en-herbe",
            progress = testProgress(testRechargeState()),
            now = fixedNow,
            isEpicBoosted = true,
        )

        assertEquals(true, response.isEpicBoosted)
        assertEquals(listOf("Epic"), response.cards.map { it.rarityLabel })
    }

    @Test
    fun `draw pack keeps base rarity distribution when selected pack is not boosted`() = runTest {
        val catalogGateway = FakeCatalogGateway().apply {
            cards = allRarityCards()
            variantProfiles = listOf(localPackProfile())
            gameBalance = localPackBalance(cardsPerDraw = 1)
        }
        val engine = LocalPackEngine(
            catalogRepository = catalogGateway,
            settings = queuedGameSettings(950_000),
        )

        val response = engine.drawPack(
            extensionId = "astronomes-en-herbe",
            progress = testProgress(testRechargeState()),
            now = fixedNow,
            isEpicBoosted = false,
        )

        assertEquals(false, response.isEpicBoosted)
        assertEquals(listOf("Common"), response.cards.map { it.rarityLabel })
    }

    private fun allRarityCards() = listOf(
        testCardDefinition(id = "C-1", rarityLabel = "Common", variantProfileId = "local-pack-profile"),
        testCardDefinition(id = "U-1", rarityLabel = "Uncommon", variantProfileId = "local-pack-profile"),
        testCardDefinition(id = "R-1", rarityLabel = "Rare", variantProfileId = "local-pack-profile"),
        testCardDefinition(id = "E-1", rarityLabel = "Epic", variantProfileId = "local-pack-profile"),
    )

    private fun localPackProfile(): VariantProfile = VariantProfile(
        id = "local-pack-profile",
        skyQualities = listOf(
            SkyQualityDefinition("city", "Ville"),
            SkyQualityDefinition("holographic", "Holographique", isHolographic = true),
        ),
        finishes = listOf(
            CardFinishDefinition("standard", "Standard"),
            CardFinishDefinition("stamped", "Tamponnee", isStamped = true),
        ),
    )

    private fun localPackBalance(
        cardsPerDraw: Int,
        drawCooldownHours: Double = 6.0,
        suburbanMeanPerDay: Double = 1.0,
        ruralMeanPerDay: Double = 1.0,
        mountainMeanPerDay: Double = 1.0,
        holographicSkyMeanPerDay: Double = 0.14285714285714285,
        percentStampedPerDay: Double = 10.0,
    ) = testGameBalanceDefinition(
        cardsPerDraw = cardsPerDraw,
        drawCooldownHours = drawCooldownHours,
        suburbanMeanPerDay = suburbanMeanPerDay,
        ruralMeanPerDay = ruralMeanPerDay,
        mountainMeanPerDay = mountainMeanPerDay,
        holographicSkyMeanPerDay = holographicSkyMeanPerDay,
        percentStampedPerDay = percentStampedPerDay,
    )

    private fun testProgress(
        rechargeState: fr.aumombelli.dstcg.model.PackRechargeState,
        openedPackCount: Int = 2,
        newPlayerOnboardingStep: NewPlayerOnboardingStep = NewPlayerOnboardingStep.Completed,
        newPlayerOnboardingPackCount: Int = openedPackCount.coerceAtLeast(0),
    ): StandaloneProgress =
        StandaloneProgress(
            collection = ownedCollectionOf(),
            rechargeState = rechargeState,
            openedPackCount = openedPackCount,
            newPlayerOnboardingStep = newPlayerOnboardingStep,
            newPlayerOnboardingPackCount = newPlayerOnboardingPackCount,
        )

    private fun queuedGameSettings(vararg values: Int): StandaloneGameSettings = StandaloneGameSettings(
        timeSource = ClockTrustedTimeSource(Clock.fixed(fixedNow, ZoneOffset.UTC)),
        entropySource = QueuedEntropySource(values.toList()),
    )

    private class QueuedEntropySource(
        private val queuedValues: List<Int>,
    ) : EntropySource {
        private var index = 0

        override fun nextInt(bound: Int): Int {
            val queuedValue = queuedValues.getOrNull(index) ?: 0
            index += 1
            return queuedValue.mod(bound.coerceAtLeast(1))
        }
    }
}
