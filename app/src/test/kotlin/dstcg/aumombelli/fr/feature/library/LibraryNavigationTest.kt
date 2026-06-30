package fr.aumombelli.dstcg.feature.library

import fr.aumombelli.dstcg.model.DisplayCardVariant
import fr.aumombelli.dstcg.model.ExtensionDefinition
import fr.aumombelli.dstcg.model.LibraryCardItem
import fr.aumombelli.dstcg.model.LibrarySection
import fr.aumombelli.dstcg.testsupport.fixtures.testCardDefinition
import org.junit.Assert.assertEquals
import org.junit.Test

class LibraryNavigationTest {
    @Test
    fun `navigable cards follow visible section and rarity order and skip unowned cards`() {
        val sections = listOf(
            LibrarySection(
                extension = ExtensionDefinition("alpha", "Alpha", "cover-alpha"),
                cards = listOf(
                    libraryItem(id = "ALP-RARE", extensionId = "alpha", rarityLabel = "Rare"),
                    libraryItem(id = "ALP-COMMON", extensionId = "alpha", rarityLabel = "Common"),
                    libraryItem(
                        id = "ALP-UNOWNED",
                        extensionId = "alpha",
                        rarityLabel = "Common",
                        variants = emptyList(),
                    ),
                    libraryItem(id = "ALP-EPIC", extensionId = "alpha", rarityLabel = "Epic"),
                ),
            ),
            LibrarySection(
                extension = ExtensionDefinition("beta", "Beta", "cover-beta"),
                cards = listOf(
                    libraryItem(id = "BET-COMMON", extensionId = "beta", rarityLabel = "Common"),
                ),
            ),
        )

        val navigableCards = buildNavigableLibraryCards(sections)

        assertEquals(
            listOf("ALP-COMMON", "ALP-RARE", "ALP-EPIC", "BET-COMMON"),
            navigableCards.map { it.definition.id },
        )
    }

    @Test
    fun `navigable cards respect active visible filters`() {
        val sections = listOf(
            LibrarySection(
                extension = ExtensionDefinition("alpha", "Alpha", "cover-alpha"),
                cards = listOf(
                    libraryItem(
                        id = "ALP-CITY",
                        extensionId = "alpha",
                        rarityLabel = "Rare",
                        variants = listOf(city(count = 2)),
                    ),
                ),
            ),
            LibrarySection(
                extension = ExtensionDefinition("beta", "Beta", "cover-beta"),
                cards = listOf(
                    libraryItem(
                        id = "BET-HOLO-TRADEABLE",
                        extensionId = "beta",
                        rarityLabel = "Rare",
                        variants = listOf(holographic(count = 2)),
                    ),
                    libraryItem(
                        id = "BET-HOLO-SINGLE",
                        extensionId = "beta",
                        rarityLabel = "Rare",
                        variants = listOf(holographic(count = 1)),
                    ),
                ),
            ),
        )

        val filteredSections = filterLibrarySections(
            sections = sections,
            filters = LibraryFilters(
                extensionId = "beta",
                rarityLabel = "Rare",
                skyQuality = "holographic",
                tradeableOnly = true,
            ),
        )

        assertEquals(
            listOf("BET-HOLO-TRADEABLE"),
            buildNavigableLibraryCards(filteredSections).map { it.definition.id },
        )
    }

    @Test
    fun `default navigation variant uses filters before first owned fallback`() {
        val item = libraryItem(
            id = "ALP-001",
            extensionId = "alpha",
            rarityLabel = "Rare",
            variants = listOf(
                city(count = 2),
                holographic(count = 1),
            ),
        )

        assertEquals("city::standard", item.defaultLibraryVariantKey(LibraryFilters()))
        assertEquals(
            "holographic::standard",
            item.defaultLibraryVariantKey(LibraryFilters(skyQuality = "holographic")),
        )
        assertEquals(
            "city::standard",
            item.defaultLibraryVariantKey(LibraryFilters(tradeableOnly = true)),
        )
    }

    private fun libraryItem(
        id: String,
        extensionId: String,
        rarityLabel: String,
        variants: List<DisplayCardVariant> = listOf(city(count = 1)),
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

    private fun holographic(count: Int): DisplayCardVariant =
        DisplayCardVariant("holographic", "Holographique", "standard", "Standard", true, count)
}
