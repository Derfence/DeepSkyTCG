package fr.aumombelli.dstcg

import fr.aumombelli.dstcg.data.LocalPackEngine
import fr.aumombelli.dstcg.model.ActiveEquipmentEffect
import fr.aumombelli.dstcg.model.CardFinishDefinition
import fr.aumombelli.dstcg.model.EquipmentSettingsDefinition
import fr.aumombelli.dstcg.model.EquipmentType
import fr.aumombelli.dstcg.model.SkyQualityDefinition
import fr.aumombelli.dstcg.model.StandaloneProgress
import fr.aumombelli.dstcg.model.VariantProfile
import java.time.Instant
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
        activeEquipmentByType: Map<EquipmentType, ActiveEquipmentEffect> = emptyMap(),
    ): StandaloneProgress = StandaloneProgress(
        collection = ownedCollectionOf(),
        rechargeState = testRechargeState(),
        activeEquipmentByType = activeEquipmentByType,
    )
}
