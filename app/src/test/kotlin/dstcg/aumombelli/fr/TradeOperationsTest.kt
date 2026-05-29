package fr.aumombelli.dstcg

import fr.aumombelli.dstcg.model.OwnedVariantCount
import fr.aumombelli.dstcg.model.TradeCardRef
import fr.aumombelli.dstcg.model.TradeValidationResult
import fr.aumombelli.dstcg.model.applyTrade
import fr.aumombelli.dstcg.model.tradeCountFor
import fr.aumombelli.dstcg.model.validateTradePair
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TradeOperationsTest {
    @Test
    fun `validate trade accepts same rarity and same variant with local duplicate`() {
        val result = validateTradePair(
            localCollection = ownedCollectionWithVariants(
                "ALP-001",
                OwnedVariantCount("city", "standard", 2),
            ),
            localOutgoing = TradeCardRef("ALP-001", "city", "standard"),
            remoteOutgoing = TradeCardRef("ALP-002", "city", "standard"),
            cardsById = mapOf(
                "ALP-001" to testCardDefinition("ALP-001", rarityLabel = "Common"),
                "ALP-002" to testCardDefinition("ALP-002", rarityLabel = "Common"),
            ),
            variantProfilesById = testVariantProfiles().associateBy { it.id },
        )

        assertEquals(TradeValidationResult.Valid, result)
    }

    @Test
    fun `validate trade rejects rarity mismatch`() {
        val result = validateTradePair(
            localCollection = ownedCollectionWithVariants(
                "ALP-001",
                OwnedVariantCount("city", "standard", 2),
            ),
            localOutgoing = TradeCardRef("ALP-001", "city", "standard"),
            remoteOutgoing = TradeCardRef("ALP-002", "city", "standard"),
            cardsById = mapOf(
                "ALP-001" to testCardDefinition("ALP-001", rarityLabel = "Common"),
                "ALP-002" to testCardDefinition("ALP-002", rarityLabel = "Rare"),
            ),
            variantProfilesById = testVariantProfiles().associateBy { it.id },
        )

        assertTrue(result is TradeValidationResult.Invalid)
    }

    @Test
    fun `validate trade rejects variant mismatch`() {
        val result = validateTradePair(
            localCollection = ownedCollectionWithVariants(
                "ALP-001",
                OwnedVariantCount("city", "standard", 2),
            ),
            localOutgoing = TradeCardRef("ALP-001", "city", "standard"),
            remoteOutgoing = TradeCardRef("ALP-002", "rural", "standard"),
            cardsById = mapOf(
                "ALP-001" to testCardDefinition("ALP-001", rarityLabel = "Common"),
                "ALP-002" to testCardDefinition("ALP-002", rarityLabel = "Common"),
            ),
            variantProfilesById = testVariantProfiles().associateBy { it.id },
        )

        assertTrue(result is TradeValidationResult.Invalid)
    }

    @Test
    fun `apply trade decrements outgoing variant and increments incoming variant`() {
        val collection = ownedCollectionWithVariants(
            "ALP-001",
            OwnedVariantCount("city", "standard", 2),
            OwnedVariantCount("rural", "standard", 1),
        )

        val updated = collection.applyTrade(
            outgoing = TradeCardRef("ALP-001", "city", "standard"),
            incoming = TradeCardRef("ALP-002", "city", "standard"),
        )

        assertEquals(1, updated.tradeCountFor(TradeCardRef("ALP-001", "city", "standard")))
        assertEquals(1, updated.tradeCountFor(TradeCardRef("ALP-001", "rural", "standard")))
        assertEquals(1, updated.tradeCountFor(TradeCardRef("ALP-002", "city", "standard")))
    }
}
