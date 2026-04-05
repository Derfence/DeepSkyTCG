package fr.aumombelli.dstcg

import fr.aumombelli.dstcg.model.AstronomyPackRevealSlot
import fr.aumombelli.dstcg.model.EquipmentPackRevealSlot
import fr.aumombelli.dstcg.model.EquipmentType
import fr.aumombelli.dstcg.model.sortedForPackReveal
import fr.aumombelli.dstcg.model.sortedRevealSlotsForPackReveal
import org.junit.Assert.assertEquals
import org.junit.Test

class PackModelsTest {
    @Test
    fun `pack reveal order keeps holographics last then rarity then sky quality`() {
        val sortedCards = listOf(
            testPackCard(
                cardId = "epic-holo",
                name = "Epic holographique",
                rarityLabel = "Epic",
                imageRef = "epic_holo",
                skyQuality = "mountain",
                skyQualityLabel = "Montagne",
                finish = "holographic",
                finishLabel = "Holographique",
                isHolographic = true,
            ),
            testPackCard(
                cardId = "rare-rural",
                name = "Rare campagne",
                rarityLabel = "Rare",
                imageRef = "rare_rural",
                skyQuality = "rural",
                skyQualityLabel = "Campagne",
            ),
            testPackCard(
                cardId = "common-mountain",
                name = "Common montagne",
                rarityLabel = "Common",
                imageRef = "common_mountain",
                skyQuality = "mountain",
                skyQualityLabel = "Montagne",
            ),
            testPackCard(
                cardId = "common-city",
                name = "Common ville",
                rarityLabel = "Common",
                imageRef = "common_city",
                skyQuality = "city",
                skyQualityLabel = "Ville",
            ),
            testPackCard(
                cardId = "rare-city",
                name = "Rare ville",
                rarityLabel = "Rare",
                imageRef = "rare_city",
                skyQuality = "city",
                skyQualityLabel = "Ville",
            ),
            testPackCard(
                cardId = "common-holo",
                name = "Common holographique",
                rarityLabel = "Common",
                imageRef = "common_holo",
                skyQuality = "city",
                skyQualityLabel = "Ville",
                finish = "holographic",
                finishLabel = "Holographique",
                isHolographic = true,
            ),
        ).sortedForPackReveal()

        assertEquals(
            listOf(
                "common-city",
                "common-mountain",
                "rare-city",
                "rare-rural",
                "common-holo",
                "epic-holo",
            ),
            sortedCards.map { it.cardId },
        )
    }

    @Test
    fun `pack reveal slot order keeps astronomy cards sorted and places equipment with common rewards`() {
        val sortedSlots = listOf(
            AstronomyPackRevealSlot(
                slotIndex = 0,
                card = testPackCard(
                    cardId = "epic-holo",
                    name = "Epic holographique",
                    rarityLabel = "Epic",
                    imageRef = "epic_holo",
                    skyQuality = "mountain",
                    skyQualityLabel = "Montagne",
                    finish = "holographic",
                    finishLabel = "Holographique",
                    isHolographic = true,
                ),
            ),
            EquipmentPackRevealSlot(
                slotIndex = 1,
                definition = testEquipmentCardDefinition(
                    id = "mount-beginner",
                    type = EquipmentType.Mount,
                    level = 1,
                ),
            ),
            AstronomyPackRevealSlot(
                slotIndex = 2,
                card = testPackCard(
                    cardId = "rare-rural",
                    name = "Rare campagne",
                    rarityLabel = "Rare",
                    imageRef = "rare_rural",
                    skyQuality = "rural",
                    skyQualityLabel = "Campagne",
                ),
            ),
            AstronomyPackRevealSlot(
                slotIndex = 3,
                card = testPackCard(
                    cardId = "common-city",
                    name = "Common ville",
                    rarityLabel = "Common",
                    imageRef = "common_city",
                    skyQuality = "city",
                    skyQualityLabel = "Ville",
                ),
            ),
        ).sortedRevealSlotsForPackReveal()

        assertEquals(
            listOf(
                "common-city",
                "mount-beginner",
                "rare-rural",
                "epic-holo",
            ),
            sortedSlots.map { slot ->
                when (slot) {
                    is AstronomyPackRevealSlot -> slot.card.cardId
                    is EquipmentPackRevealSlot -> slot.definition.id
                }
            },
        )
    }
}
