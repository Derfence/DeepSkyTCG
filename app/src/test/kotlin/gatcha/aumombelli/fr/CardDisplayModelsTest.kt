package fr.aumombelli.gatcha

import androidx.compose.ui.graphics.Color
import fr.aumombelli.gatcha.model.OwnedCardEntry
import fr.aumombelli.gatcha.model.OwnedVariantCount
import fr.aumombelli.gatcha.model.raritySortPriority
import fr.aumombelli.gatcha.model.toDisplayVariants
import fr.aumombelli.gatcha.ui.component.cardArtCreditArtistName
import fr.aumombelli.gatcha.ui.component.cardHeadlineContent
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
        assertEquals(Color(0xFFC69BFF), rarityBadgeStyle("Epic").color)
    }

    @Test
    fun `rarity sort priority orders cards from common to epic`() {
        assertEquals(0, raritySortPriority("Common"))
        assertEquals(1, raritySortPriority("Uncommon"))
        assertEquals(2, raritySortPriority("Rare"))
        assertEquals(3, raritySortPriority("Epic"))
    }

    @Test
    fun `sky quality palette uses polluted and deep sky colors`() {
        assertEquals(Color(0xFF8E845F), skyQualityPalette("city").top)
        assertEquals(Color(0xFF5F4A46), skyQualityPalette("suburban").top)
        assertEquals(Color(0xFF010308), skyQualityPalette("mountain").bottom)
    }

    @Test
    fun `card headline prefers common name and keeps catalog line`() {
        val definition = testCardDefinition(
            id = "M42",
            name = "Nebuleuse d'Orion",
            commonName = "Nebuleuse d'Orion",
            catalogNumber = "M42",
        )

        val headline = cardHeadlineContent(definition)

        assertEquals("Nebuleuse d'Orion", headline.title)
        assertEquals("M42", headline.catalogLine)
    }

    @Test
    fun `card headline falls back to catalog number when common name is missing`() {
        val definition = testCardDefinition(
            id = "NGC7000",
            name = "Nebuleuse catalogue",
            commonName = null,
            catalogNumber = "NGC 7000",
        )

        val headline = cardHeadlineContent(definition)

        assertEquals("NGC 7000", headline.title)
        assertEquals(null, headline.catalogLine)
    }

    @Test
    fun `card headline avoids duplicating catalog line when common name matches catalog number`() {
        val definition = testCardDefinition(
            id = "M42",
            name = "M42",
            commonName = "M42",
            catalogNumber = "M42",
        )

        val headline = cardHeadlineContent(definition)

        assertEquals("M42", headline.title)
        assertEquals(null, headline.catalogLine)
    }

    @Test
    fun `card art credit artist name falls back to inconnu when missing`() {
        assertEquals("Inconnu", cardArtCreditArtistName(null))
        assertEquals("Inconnu", cardArtCreditArtistName("   "))
    }

    @Test
    fun `card art credit artist name keeps the provided artist`() {
        assertEquals("Dylan O'Donnell", cardArtCreditArtistName("Dylan O'Donnell"))
    }
}
