package fr.aumombelli.dstcg

import fr.aumombelli.dstcg.data.LocalPackEngine
import fr.aumombelli.dstcg.data.ClockTrustedTimeSource
import fr.aumombelli.dstcg.data.EntropySource
import fr.aumombelli.dstcg.data.StandaloneGameSettings
import fr.aumombelli.dstcg.model.ActiveEquipmentEffect
import fr.aumombelli.dstcg.model.CardFinishDefinition
import fr.aumombelli.dstcg.model.EquipmentSettingsDefinition
import fr.aumombelli.dstcg.model.EquipmentType
import fr.aumombelli.dstcg.model.NewPlayerOnboardingStep
import fr.aumombelli.dstcg.model.SkyQualityDefinition
import fr.aumombelli.dstcg.model.StandaloneProgress
import fr.aumombelli.dstcg.model.VariantProfile
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalPackEngineEquipmentTest {
    private val fixedNow = Instant.parse("2026-03-24T12:00:00Z")

    @Test
    fun `draw pack can replace a common slot with an equipment reward`() = runTest {
        val rewardCard = testEquipmentCardDefinition(
            id = "mount-beginner",
            type = EquipmentType.Mount,
            dropWeight = 1,
        )
        val catalogGateway = FakeCatalogGateway().apply {
            cards = listOf(
                testCardDefinition(
                    id = "ALP-001",
                    rarityLabel = "Common",
                    variantProfileId = "equipment-pack-profile",
                ),
            )
            variantProfiles = listOf(equipmentPackProfile())
            gameBalance = testGameBalanceDefinition(
                cardsPerDraw = 1,
                suburbanMeanPerDay = 1.0,
                ruralMeanPerDay = 1.0,
                mountainMeanPerDay = 1.0,
            )
            equipmentCards = listOf(rewardCard)
            equipmentSettings = EquipmentSettingsDefinition(
                commonReplacementChancePercent = 100.0,
            )
        }
        val engine = LocalPackEngine(
            catalogRepository = catalogGateway,
            settings = testGameSettings(now = fixedNow, randomSeed = 0),
        )

        val response = engine.drawPack(
            extensionId = "astronomes-en-herbe",
            progress = testProgress(),
            now = fixedNow,
        )

        assertTrue(response.cards.isEmpty())
        assertEquals(listOf(rewardCard.id), response.equipmentCards.map { it.id })
    }

    @Test
    fun `first onboarding draw never awards equipment even with one hundred percent chance`() = runTest {
        val rewardCard = testEquipmentCardDefinition(
            id = "mount-beginner",
            type = EquipmentType.Mount,
            dropWeight = 1,
        )
        val catalogGateway = FakeCatalogGateway().apply {
            cards = listOf(
                testCardDefinition(
                    id = "ALP-001",
                    rarityLabel = "Common",
                    variantProfileId = "equipment-pack-profile",
                ),
            )
            variantProfiles = listOf(equipmentPackProfile())
            gameBalance = testGameBalanceDefinition(
                cardsPerDraw = 1,
                suburbanMeanPerDay = 1.0,
                ruralMeanPerDay = 1.0,
                mountainMeanPerDay = 1.0,
            )
            equipmentCards = listOf(rewardCard)
            equipmentSettings = EquipmentSettingsDefinition(
                commonReplacementChancePercent = 100.0,
            )
        }
        val engine = LocalPackEngine(
            catalogRepository = catalogGateway,
            settings = testGameSettings(now = fixedNow, randomSeed = 0),
        )

        val response = engine.drawPack(
            extensionId = "astronomes-en-herbe",
            progress = testProgress(
                openedPackCount = 0,
                newPlayerOnboardingStep = NewPlayerOnboardingStep.SelectFirstBooster,
            ),
            now = fixedNow,
        )

        assertEquals(1, response.cards.size)
        assertTrue(response.equipmentCards.isEmpty())
    }

    @Test
    fun `second onboarding draw forces exactly one level one equipment`() = runTest {
        val levelOneReward = testEquipmentCardDefinition(
            id = "mount-lv1",
            type = EquipmentType.Mount,
            level = 1,
            dropWeight = 1,
        )
        val higherLevelReward = testEquipmentCardDefinition(
            id = "mount-lv2",
            type = EquipmentType.Mount,
            level = 2,
            dropWeight = 999,
        )
        val catalogGateway = FakeCatalogGateway().apply {
            cards = listOf(
                testCardDefinition(
                    id = "ALP-001",
                    rarityLabel = "Common",
                    variantProfileId = "equipment-pack-profile",
                ),
            )
            variantProfiles = listOf(equipmentPackProfile())
            gameBalance = testGameBalanceDefinition(
                cardsPerDraw = 2,
                suburbanMeanPerDay = 1.0,
                ruralMeanPerDay = 1.0,
                mountainMeanPerDay = 1.0,
            )
            equipmentCards = listOf(levelOneReward, higherLevelReward)
            equipmentSettings = EquipmentSettingsDefinition(
                commonReplacementChancePercent = 100.0,
            )
        }
        val engine = LocalPackEngine(
            catalogRepository = catalogGateway,
            settings = testGameSettings(now = fixedNow, randomSeed = 0),
        )

        val response = engine.drawPack(
            extensionId = "astronomes-en-herbe",
            progress = testProgress(
                openedPackCount = 1,
                newPlayerOnboardingStep = NewPlayerOnboardingStep.OpenSecondPackMenu,
            ),
            now = fixedNow,
        )

        assertEquals(1, response.equipmentCards.size)
        assertEquals(levelOneReward.id, response.equipmentCards.single().id)
        assertEquals(1, response.cards.size)
    }

    @Test
    fun `second onboarding draw prefers replacing a common slot`() = runTest {
        val rewardCard = testEquipmentCardDefinition(
            id = "observatory-lv1",
            type = EquipmentType.Observatory,
            level = 1,
            dropWeight = 1,
        )
        val catalogGateway = FakeCatalogGateway().apply {
            cards = listOf(
                testCardDefinition(
                    id = "ALP-COMMON",
                    rarityLabel = "Common",
                    variantProfileId = "equipment-pack-profile",
                ),
                testCardDefinition(
                    id = "ALP-UNCOMMON",
                    rarityLabel = "Uncommon",
                    variantProfileId = "equipment-pack-profile",
                ),
            )
            variantProfiles = listOf(equipmentPackProfile())
            gameBalance = testGameBalanceDefinition(
                cardsPerDraw = 2,
                percentUncommonPerDay = 49.0,
                percentRarePerDay = 1.0,
                percentEpicPerDay = 1.0,
                suburbanMeanPerDay = 1.0,
                ruralMeanPerDay = 1.0,
                mountainMeanPerDay = 1.0,
            )
            equipmentCards = listOf(rewardCard)
            equipmentSettings = EquipmentSettingsDefinition(
                commonReplacementChancePercent = 0.0,
            )
        }
        val engine = LocalPackEngine(
            catalogRepository = catalogGateway,
            settings = onboardingGameSettings(0, 1, 0, 0, 0),
        )

        val response = engine.drawPack(
            extensionId = "astronomes-en-herbe",
            progress = testProgress(
                openedPackCount = 1,
                newPlayerOnboardingStep = NewPlayerOnboardingStep.OpenSecondPackMenu,
            ),
            now = fixedNow,
        )

        assertEquals(listOf(rewardCard.id), response.equipmentCards.map { it.id })
        assertEquals(listOf("ALP-UNCOMMON"), response.cards.map { it.cardId })
    }

    @Test
    fun `second onboarding draw falls back to the lowest rarity slot when no common exists`() = runTest {
        val rewardCard = testEquipmentCardDefinition(
            id = "telescope-lv1",
            type = EquipmentType.Telescope,
            level = 1,
            dropWeight = 1,
        )
        val catalogGateway = FakeCatalogGateway().apply {
            cards = listOf(
                testCardDefinition(
                    id = "ALP-UNCOMMON",
                    rarityLabel = "Uncommon",
                    variantProfileId = "equipment-pack-profile",
                ),
                testCardDefinition(
                    id = "ALP-RARE",
                    rarityLabel = "Rare",
                    variantProfileId = "equipment-pack-profile",
                ),
            )
            variantProfiles = listOf(equipmentPackProfile())
            gameBalance = testGameBalanceDefinition(
                cardsPerDraw = 2,
                percentUncommonPerDay = 40.0,
                percentRarePerDay = 40.0,
                percentEpicPerDay = 1.0,
                suburbanMeanPerDay = 1.0,
                ruralMeanPerDay = 1.0,
                mountainMeanPerDay = 1.0,
            )
            equipmentCards = listOf(rewardCard)
            equipmentSettings = EquipmentSettingsDefinition(
                commonReplacementChancePercent = 0.0,
            )
        }
        val engine = LocalPackEngine(
            catalogRepository = catalogGateway,
            settings = onboardingGameSettings(0, 1),
        )

        val response = engine.drawPack(
            extensionId = "astronomes-en-herbe",
            progress = testProgress(
                openedPackCount = 1,
                newPlayerOnboardingStep = NewPlayerOnboardingStep.OpenSecondPackMenu,
            ),
            now = fixedNow,
        )

        assertEquals(listOf(rewardCard.id), response.equipmentCards.map { it.id })
        assertEquals(listOf("ALP-RARE"), response.cards.map { it.cardId })
    }

    @Test
    fun `standard equipment replacement resumes outside onboarding overrides`() = runTest {
        val rewardCard = testEquipmentCardDefinition(
            id = "mount-beginner",
            type = EquipmentType.Mount,
            level = 1,
            dropWeight = 1,
        )
        val catalogGateway = FakeCatalogGateway().apply {
            cards = listOf(
                testCardDefinition(
                    id = "ALP-001",
                    rarityLabel = "Common",
                    variantProfileId = "equipment-pack-profile",
                ),
            )
            variantProfiles = listOf(equipmentPackProfile())
            gameBalance = testGameBalanceDefinition(
                cardsPerDraw = 1,
                suburbanMeanPerDay = 1.0,
                ruralMeanPerDay = 1.0,
                mountainMeanPerDay = 1.0,
            )
            equipmentCards = listOf(rewardCard)
            equipmentSettings = EquipmentSettingsDefinition(
                commonReplacementChancePercent = 100.0,
            )
        }
        val engine = LocalPackEngine(
            catalogRepository = catalogGateway,
            settings = testGameSettings(now = fixedNow, randomSeed = 0),
        )

        val response = engine.drawPack(
            extensionId = "astronomes-en-herbe",
            progress = testProgress(
                openedPackCount = 2,
                newPlayerOnboardingStep = NewPlayerOnboardingStep.Completed,
            ),
            now = fixedNow,
        )

        assertTrue(response.cards.isEmpty())
        assertEquals(listOf(rewardCard.id), response.equipmentCards.map { it.id })
    }

    @Test
    fun `standard equipment replacement does not duplicate the same equipment inside a pack`() = runTest {
        val rewardCard = testEquipmentCardDefinition(
            id = "mount-beginner",
            type = EquipmentType.Mount,
            level = 1,
            dropWeight = 1,
        )
        val catalogGateway = FakeCatalogGateway().apply {
            cards = listOf(
                testCardDefinition(
                    id = "ALP-001",
                    rarityLabel = "Common",
                    variantProfileId = "equipment-pack-profile",
                ),
                testCardDefinition(
                    id = "ALP-002",
                    rarityLabel = "Common",
                    variantProfileId = "equipment-pack-profile",
                ),
            )
            variantProfiles = listOf(equipmentPackProfile())
            gameBalance = testGameBalanceDefinition(
                cardsPerDraw = 2,
                suburbanMeanPerDay = 1.0,
                ruralMeanPerDay = 1.0,
                mountainMeanPerDay = 1.0,
            )
            equipmentCards = listOf(rewardCard)
            equipmentSettings = EquipmentSettingsDefinition(
                commonReplacementChancePercent = 100.0,
            )
        }
        val engine = LocalPackEngine(
            catalogRepository = catalogGateway,
            settings = testGameSettings(now = fixedNow, randomSeed = 0),
        )

        val response = engine.drawPack(
            extensionId = "astronomes-en-herbe",
            progress = testProgress(
                openedPackCount = 2,
                newPlayerOnboardingStep = NewPlayerOnboardingStep.Completed,
            ),
            now = fixedNow,
        )

        assertEquals(listOf(rewardCard.id), response.equipmentCards.map { it.id })
        assertEquals(1, response.cards.size)
        assertEquals(1, response.cards.map { it.cardId }.distinct().size)
    }

    @Test
    fun `draw pack applies mount and telescope bonuses together`() = runTest {
        val mountCard = testEquipmentCardDefinition(
            id = "mount-master",
            type = EquipmentType.Mount,
            bonusValue = 100.0,
            packsAffected = 2,
        )
        val telescopeCard = testEquipmentCardDefinition(
            id = "telescope-master",
            type = EquipmentType.Telescope,
            bonusValue = 100.0,
            packsAffected = 2,
        )
        val catalogGateway = FakeCatalogGateway().apply {
            cards = listOf(
                testCardDefinition(
                    id = "ALP-COMMON",
                    rarityLabel = "Common",
                    variantProfileId = "equipment-pack-profile",
                ),
                testCardDefinition(
                    id = "ALP-UNCOMMON",
                    rarityLabel = "Uncommon",
                    variantProfileId = "equipment-pack-profile",
                ),
            )
            variantProfiles = listOf(equipmentPackProfile())
            gameBalance = testGameBalanceDefinition(
                cardsPerDraw = 1,
                suburbanMeanPerDay = 1.0,
                ruralMeanPerDay = 1.0,
                mountainMeanPerDay = 1.0,
            )
            equipmentCards = listOf(mountCard, telescopeCard)
            equipmentSettings = EquipmentSettingsDefinition(
                commonReplacementChancePercent = 0.0,
            )
        }
        val engine = LocalPackEngine(
            catalogRepository = catalogGateway,
            settings = testGameSettings(now = fixedNow, randomSeed = 0),
        )

        val response = engine.drawPack(
            extensionId = "astronomes-en-herbe",
            progress = testProgress(
                activeEquipmentByType = mapOf(
                    EquipmentType.Mount to ActiveEquipmentEffect(
                        equipmentCardId = mountCard.id,
                        equipmentType = EquipmentType.Mount,
                        packsRemaining = 2,
                    ),
                    EquipmentType.Telescope to ActiveEquipmentEffect(
                        equipmentCardId = telescopeCard.id,
                        equipmentType = EquipmentType.Telescope,
                        packsRemaining = 2,
                    ),
                ),
            ),
            now = fixedNow,
        )

        val card = response.cards.single()
        assertEquals("ALP-UNCOMMON", card.cardId)
        assertEquals("Uncommon", card.rarityLabel)
        assertTrue(card.variant.isHolographic)
    }

    private fun equipmentPackProfile(): VariantProfile = VariantProfile(
        id = "equipment-pack-profile",
        skyQualities = listOf(
            SkyQualityDefinition("city", "Ville"),
            SkyQualityDefinition("suburban", "Periurbain"),
            SkyQualityDefinition("rural", "Campagne"),
            SkyQualityDefinition("mountain", "Montagne"),
        ),
        finishes = listOf(
            CardFinishDefinition("standard", "Standard"),
            CardFinishDefinition("holographic", "Holographique", isHolographic = true),
        ),
    )

    private fun testProgress(
        openedPackCount: Int = 0,
        newPlayerOnboardingStep: NewPlayerOnboardingStep = NewPlayerOnboardingStep.Completed,
        activeEquipmentByType: Map<EquipmentType, ActiveEquipmentEffect> = emptyMap(),
    ): StandaloneProgress = StandaloneProgress(
        collection = ownedCollectionOf(),
        rechargeState = testRechargeState(),
        openedPackCount = openedPackCount,
        newPlayerOnboardingStep = newPlayerOnboardingStep,
        activeEquipmentByType = activeEquipmentByType,
    )

    private fun onboardingGameSettings(vararg values: Int): StandaloneGameSettings = StandaloneGameSettings(
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
