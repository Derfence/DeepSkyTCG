package fr.aumombelli.dstcg

import fr.aumombelli.dstcg.feature.library.LibraryOnboardingVariantWalkthroughVisual
import fr.aumombelli.dstcg.feature.library.buildLibraryOnboardingVariantWalkthroughPages
import fr.aumombelli.dstcg.feature.library.randomLibraryOnboardingCardForRarity
import fr.aumombelli.dstcg.model.ExtensionDefinition
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LibraryOnboardingVariantWalkthroughTest {
    @Test
    fun `random rarity helper only returns cards of requested rarity`() {
        val extensions = listOf(
            ExtensionDefinition("alpha", "Alpha", "cover"),
            ExtensionDefinition("beta", "Beta", "cover"),
        )
        val cards = listOf(
            testCardDefinition("ALP-001", extensionId = "alpha", rarityLabel = "Common", imageRef = "common_1"),
            testCardDefinition("BET-001", extensionId = "beta", rarityLabel = "Common", imageRef = "common_2"),
            testCardDefinition("ALP-010", extensionId = "alpha", rarityLabel = "Rare", imageRef = "rare_1"),
        )

        val picked = randomLibraryOnboardingCardForRarity(
            rarityLabel = "Common",
            extensions = extensions,
            cards = cards,
            variantProfiles = testVariantProfiles(),
            random = Random(7),
            requiredSkyQualities = setOf("city"),
            requiredFinishes = setOf("standard"),
        )

        assertNotNull(picked)
        assertEquals("Common", picked?.definition?.rarityLabel)
        assertTrue(picked?.definition?.id in setOf("ALP-001", "BET-001"))
    }

    @Test
    fun `rarity walkthrough page uses real catalog cards instead of cloning one card across rarities`() {
        val extensions = listOf(
            ExtensionDefinition("alpha", "Alpha", "cover"),
            ExtensionDefinition("beta", "Beta", "cover"),
        )
        val pages = buildLibraryOnboardingVariantWalkthroughPages(
            extensions = extensions,
            cards = listOf(
                testCardDefinition("ALP-001", extensionId = "alpha", rarityLabel = "Common", imageRef = "common_1"),
                testCardDefinition("BET-014", extensionId = "beta", rarityLabel = "Uncommon", imageRef = "uncommon_1"),
                testCardDefinition("ALP-120", extensionId = "alpha", rarityLabel = "Rare", imageRef = "rare_1"),
                testCardDefinition("BET-222", extensionId = "beta", rarityLabel = "Epic", imageRef = "epic_1"),
            ),
            variantProfiles = testVariantProfiles(),
            random = Random(3),
        )

        val rarityVisual = pages.first().visual as LibraryOnboardingVariantWalkthroughVisual.RarityGrid

        assertEquals(
            listOf("Common", "Uncommon", "Rare", "Epic"),
            rarityVisual.cards.map { it.definition.rarityLabel },
        )
        assertEquals(4, rarityVisual.cards.map { it.definition.id }.distinct().size)
        assertEquals(
            setOf("ALP-001", "BET-014", "ALP-120", "BET-222"),
            rarityVisual.cards.map { it.definition.id }.toSet(),
        )
    }
}
