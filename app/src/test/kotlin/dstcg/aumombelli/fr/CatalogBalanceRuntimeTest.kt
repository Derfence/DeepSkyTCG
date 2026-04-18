package fr.aumombelli.dstcg

import fr.aumombelli.dstcg.data.CatalogBalanceRuntimeCalculator
import fr.aumombelli.dstcg.data.cardsPerDay
import fr.aumombelli.dstcg.data.cityProbability
import fr.aumombelli.dstcg.data.commonRarityProbability
import fr.aumombelli.dstcg.data.drawCooldownDuration
import fr.aumombelli.dstcg.data.finishProbabilities
import fr.aumombelli.dstcg.data.rarityProbabilities
import fr.aumombelli.dstcg.data.skyQualityProbabilities
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CatalogBalanceRuntimeTest {
    private val calculator = CatalogBalanceRuntimeCalculator()

    @Test
    fun `game balance exposes expected derived probabilities`() {
        val balance = testGameBalanceDefinition()

        assertEquals(20.0, balance.cardsPerDay(), 0.000001)
        assertEquals(0.5, balance.commonRarityProbability(), 0.000001)
        assertEquals(0.4928571428571429, balance.cityProbability(), 0.000001)
        assertEquals(0.9, balance.finishProbabilities().getValue("standard"), 0.000001)
        assertEquals(0.1, balance.finishProbabilities().getValue("stamped"), 0.000001)
        assertEquals(0.3, balance.rarityProbabilities().getValue("Uncommon"), 0.000001)
        assertEquals(0.15, balance.skyQualityProbabilities().getValue("rural"), 0.000001)
        assertEquals(6L, balance.drawCooldownDuration().toHours())
    }

    @Test
    fun `extension rarity probabilities are renormalized over present rarities`() {
        val runtime = calculator.resolve(
            cards = listOf(
                testCardDefinition(id = "R-1", extensionId = "rare-only", rarityLabel = "Rare"),
                testCardDefinition(id = "E-1", extensionId = "rare-only", rarityLabel = "Epic"),
            ),
            variantProfiles = testVariantProfiles(),
            gameBalance = testGameBalanceDefinition(),
        )

        val plan = checkNotNull(runtime.extensionPlansById["rare-only"])

        assertEquals(0.75, plan.rarityProbabilities.getValue("Rare"), 0.000001)
        assertEquals(0.25, plan.rarityProbabilities.getValue("Epic"), 0.000001)
    }

    @Test
    fun `card multipliers drive the second draw phase inside a rarity`() {
        val runtime = calculator.resolve(
            cards = listOf(
                testCardDefinition(id = "C-4", extensionId = "commons", rarityLabel = "Common", cardRarityMultiplier = 4.0),
                testCardDefinition(id = "C-1", extensionId = "commons", rarityLabel = "Common", cardRarityMultiplier = 1.0),
            ),
            variantProfiles = testVariantProfiles(),
            gameBalance = testGameBalanceDefinition(),
        )

        val plan = checkNotNull(runtime.extensionPlansById["commons"])

        assertEquals(1.0, plan.rarityProbabilities.getValue("Common"), 0.000001)
        assertEquals(0.8, plan.cardProbabilities.getValue("C-4"), 0.000001)
        assertEquals(0.2, plan.cardProbabilities.getValue("C-1"), 0.000001)
        assertTrue(plan.cardsByRarity.getValue("Common").first { it.card.id == "C-4" }.weight >
            plan.cardsByRarity.getValue("Common").first { it.card.id == "C-1" }.weight)
    }

    @Test
    fun `runtime draw config comes from the catalog balance`() {
        val runtime = calculator.resolve(
            cards = listOf(
                testCardDefinition(id = "A-1"),
            ),
            variantProfiles = testVariantProfiles(),
            gameBalance = testGameBalanceDefinition(
                cardsPerDraw = 3,
                drawCooldownHours = 12.0,
                suburbanMeanPerDay = 1.0,
                ruralMeanPerDay = 0.5,
                mountainMeanPerDay = 0.5,
            ),
        )

        assertEquals(3, runtime.drawConfig.cardsPerDraw)
        assertEquals(12L, runtime.drawConfig.drawCooldown.toHours())
    }
}
