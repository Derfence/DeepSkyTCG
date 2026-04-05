package fr.aumombelli.dstcg

import fr.aumombelli.dstcg.model.sortedForPackReveal
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
}
