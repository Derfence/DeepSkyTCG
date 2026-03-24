package fr.aumombelli.gatcha

import androidx.compose.ui.graphics.Color
import fr.aumombelli.gatcha.model.OwnedCardEntry
import fr.aumombelli.gatcha.model.OwnedVariantCount
import fr.aumombelli.gatcha.model.toDisplayVariants
import fr.aumombelli.gatcha.ui.theme.rarityBadgeStyle
import fr.aumombelli.gatcha.ui.theme.skyQualityPalette
import org.junit.Assert.assertEquals
import org.junit.Test

class CardDisplayModelsTest {
    @Test
    fun `owned variants are sorted by holographic then sky quality`() {
        val profile = testVariantProfiles().first()
        val entry = OwnedCardEntry(
            variants = listOf(
                OwnedVariantCount("city", "standard", 4),
                OwnedVariantCount("mountain", "standard", 1),
                OwnedVariantCount("rural", "holographic", 2),
            ),
        )

        val variants = entry.toDisplayVariants(profile)

        assertEquals(
            listOf(
                "rural::holographic",
                "mountain::standard",
                "city::standard",
            ),
            variants.map { it.key },
        )
        assertEquals("Campagne · Holographique ×2", variants.first().selectorLabel)
    }

    @Test
    fun `rarity badge style maps expected star branches`() {
        assertEquals(4, rarityBadgeStyle("Common").branchCount)
        assertEquals(4, rarityBadgeStyle("Uncommon").branchCount)
        assertEquals(4, rarityBadgeStyle("Rare").branchCount)
        assertEquals(6, rarityBadgeStyle("Epic").branchCount)
    }

    @Test
    fun `sky quality palette uses polluted and deep sky colors`() {
        assertEquals(Color(0xFF8E845F), skyQualityPalette("city").top)
        assertEquals(Color(0xFF5F4A46), skyQualityPalette("suburban").top)
        assertEquals(Color(0xFF010308), skyQualityPalette("mountain").bottom)
    }
}
