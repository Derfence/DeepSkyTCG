package fr.aumombelli.dstcg.feature.library

import fr.aumombelli.dstcg.model.DisplayCardVariant
import fr.aumombelli.dstcg.model.ExtensionDefinition
import fr.aumombelli.dstcg.model.LibraryCardItem
import fr.aumombelli.dstcg.model.LibrarySection
import fr.aumombelli.dstcg.testsupport.fixtures.testCardDefinition
import fr.aumombelli.dstcg.testsupport.fixtures.testVariantProfiles
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LibraryFiltersTest {
    @Test
    fun `filter options are built from catalog definitions`() {
        val options = buildLibraryFilterOptions(
            extensions = listOf(
                ExtensionDefinition("alpha", "Alpha", "cover-alpha"),
                ExtensionDefinition("beta", "Beta", "cover-beta"),
            ),
            cards = listOf(
                testCardDefinition("ALP-001", rarityLabel = "Epic"),
                testCardDefinition("ALP-002", rarityLabel = "Common"),
                testCardDefinition("ALP-003", rarityLabel = "Rare"),
                testCardDefinition("ALP-005", rarityLabel = "Uncommon"),
                testCardDefinition("ALP-004", rarityLabel = "Common"),
            ),
            variantProfiles = testVariantProfiles(),
        )

        assertEquals(listOf("alpha", "beta"), options.extensions.map { it.id })
        assertEquals(listOf("Common", "Uncommon", "Rare", "Epic"), options.rarities.map { it.id })
        assertEquals(listOf("Com.", "Unc.", "Rare", "Epic"), options.rarities.map { it.label })
        assertEquals(
            listOf("city", "suburban", "rural", "mountain", "holographic"),
            options.skyQualities.map { it.id },
        )
        assertEquals(
            listOf("Ville", "Péri.", "Camp.", "Mont.", "Holo"),
            options.skyQualities.map { it.label },
        )
    }

    @Test
    fun `filters combine extension rarity sky quality and tradeable variant`() {
        val sections = listOf(
            LibrarySection(
                extension = ExtensionDefinition("alpha", "Alpha", "cover-alpha"),
                cards = listOf(
                    libraryItem(
                        id = "ALP-001",
                        extensionId = "alpha",
                        rarityLabel = "Common",
                        variants = listOf(city(count = 2)),
                    ),
                    libraryItem(
                        id = "ALP-002",
                        extensionId = "alpha",
                        rarityLabel = "Rare",
                        variants = listOf(mountain(count = 1)),
                    ),
                    libraryItem(
                        id = "ALP-003",
                        extensionId = "alpha",
                        rarityLabel = "Rare",
                        variants = emptyList(),
                    ),
                ),
            ),
            LibrarySection(
                extension = ExtensionDefinition("beta", "Beta", "cover-beta"),
                cards = listOf(
                    libraryItem(
                        id = "BET-001",
                        extensionId = "beta",
                        rarityLabel = "Rare",
                        variants = listOf(holographic(count = 2)),
                    ),
                    libraryItem(
                        id = "BET-002",
                        extensionId = "beta",
                        rarityLabel = "Rare",
                        variants = listOf(
                            holographic(count = 1),
                            city(count = 2),
                        ),
                    ),
                ),
            ),
        )

        val filtered = filterLibrarySections(
            sections = sections,
            filters = LibraryFilters(
                extensionId = "beta",
                rarityLabel = "Rare",
                skyQuality = "holographic",
                tradeableOnly = true,
            ),
        )

        assertEquals(listOf("beta"), filtered.map { it.extension.id })
        assertEquals(listOf("BET-001"), filtered.single().cards.map { it.definition.id })
        assertEquals("holographic::standard", filtered.single().cards.single().bestVariantMatching(
            LibraryFilters(
                skyQuality = "holographic",
                tradeableOnly = true,
            ),
        )?.key)
        assertNull(
            sections.last().cards.last().bestVariantMatching(
                LibraryFilters(
                    skyQuality = "holographic",
                    tradeableOnly = true,
                ),
            ),
        )
    }

    @Test
    fun `tradeable filter selects best tradeable variant`() {
        val item = libraryItem(
            id = "ALP-001",
            extensionId = "alpha",
            rarityLabel = "Rare",
            variants = listOf(
                holographic(count = 1),
                city(count = 4),
                mountain(count = 2),
            ),
        )

        val selectedVariant = item.bestVariantMatching(LibraryFilters(tradeableOnly = true))

        assertEquals("mountain::standard", selectedVariant?.key)
    }

    private fun libraryItem(
        id: String,
        extensionId: String,
        rarityLabel: String,
        variants: List<DisplayCardVariant>,
    ): LibraryCardItem =
        LibraryCardItem(
            definition = testCardDefinition(
                id = id,
                extensionId = extensionId,
                rarityLabel = rarityLabel,
            ),
            extensionName = extensionId,
            ownedCount = variants.sumOf { it.count },
            availableVariants = variants,
        )

    private fun city(count: Int): DisplayCardVariant =
        DisplayCardVariant("city", "Ville", "standard", "Standard", false, count)

    private fun mountain(count: Int): DisplayCardVariant =
        DisplayCardVariant("mountain", "Montagne", "standard", "Standard", false, count)

    private fun holographic(count: Int): DisplayCardVariant =
        DisplayCardVariant("holographic", "Holographique", "standard", "Standard", true, count)
}
