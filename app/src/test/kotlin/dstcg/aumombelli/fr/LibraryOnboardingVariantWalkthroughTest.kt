package fr.aumombelli.dstcg

import androidx.compose.ui.unit.dp
import fr.aumombelli.dstcg.feature.library.LibraryOnboardingVariantWalkthroughVisual
import fr.aumombelli.dstcg.feature.library.buildLibraryOnboardingVariantWalkthroughPages
import fr.aumombelli.dstcg.feature.library.calculateLibraryOnboardingEqualCardMetrics
import fr.aumombelli.dstcg.feature.library.calculateLibraryOnboardingWeightedComparisonMetrics
import fr.aumombelli.dstcg.feature.library.randomLibraryOnboardingCardForRarity
import fr.aumombelli.dstcg.model.ExtensionDefinition
import fr.aumombelli.dstcg.ui.component.TRADING_CARD_WIDTH_OVER_HEIGHT
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

    @Test
    fun `walkthrough grid metrics fill width while height allows`() {
        val metrics = calculateLibraryOnboardingEqualCardMetrics(
            availableWidth = 360.dp,
            availableHeight = 700.dp,
            columns = 2,
            rows = 2,
            horizontalGap = 12.dp,
            verticalGap = 12.dp,
        )

        assertEquals(174f, metrics.cardWidth.value, 0.01f)
        assertEquals(360f, metrics.totalWidth.value, 0.01f)
        assertTrue(metrics.totalHeight <= 700.dp)
    }

    @Test
    fun `walkthrough grid metrics shrink width when height is limiting`() {
        val metrics = calculateLibraryOnboardingEqualCardMetrics(
            availableWidth = 360.dp,
            availableHeight = 220.dp,
            columns = 2,
            rows = 2,
            horizontalGap = 12.dp,
            verticalGap = 12.dp,
        )

        assertEquals(104f, metrics.cardHeight.value, 0.01f)
        assertEquals(104f * TRADING_CARD_WIDTH_OVER_HEIGHT, metrics.cardWidth.value, 0.01f)
        assertTrue(metrics.totalWidth < 360.dp)
        assertTrue(metrics.totalHeight <= 220.dp)
    }

    @Test
    fun `walkthrough comparison metrics reserve labels before fitting cards`() {
        val metrics = calculateLibraryOnboardingEqualCardMetrics(
            availableWidth = 300.dp,
            availableHeight = 140.dp,
            columns = 2,
            rows = 1,
            horizontalGap = 12.dp,
            verticalReservedHeight = 32.dp,
        )

        assertEquals(108f, metrics.cardHeight.value, 0.01f)
        assertTrue(metrics.totalWidth < 300.dp)
        assertTrue(metrics.totalHeight <= 140.dp)
    }

    @Test
    fun `holographic metrics keep weighted cards inside available height`() {
        val metrics = calculateLibraryOnboardingWeightedComparisonMetrics(
            availableWidth = 360.dp,
            availableHeight = 180.dp,
            leftWeight = 0.85f,
            rightWeight = 1.15f,
            horizontalGap = 12.dp,
            rightReservedHeight = 32.dp,
        )

        assertTrue(metrics.totalWidth < 360.dp)
        assertTrue(metrics.contentHeight <= 180.dp)
        assertEquals(180f, metrics.contentHeight.value, 0.01f)
    }
}
