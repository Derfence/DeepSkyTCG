package fr.aumombelli.dstcg.feature.library

import fr.aumombelli.dstcg.model.CardDefinition
import fr.aumombelli.dstcg.model.DisplayCardVariant
import fr.aumombelli.dstcg.model.ExtensionDefinition
import fr.aumombelli.dstcg.model.LibraryCardItem
import fr.aumombelli.dstcg.model.LibrarySection
import fr.aumombelli.dstcg.model.VariantProfile
import fr.aumombelli.dstcg.model.canTradeAway
import fr.aumombelli.dstcg.model.hasTradeableVariant
import fr.aumombelli.dstcg.model.raritySortPriority
import fr.aumombelli.dstcg.model.skyQualitySortPriority

data class LibraryFilterOption(
    val id: String,
    val label: String,
)

data class LibraryFilterOptions(
    val extensions: List<LibraryFilterOption> = emptyList(),
    val rarities: List<LibraryFilterOption> = emptyList(),
    val skyQualities: List<LibraryFilterOption> = emptyList(),
)

data class LibraryFilters(
    val extensionId: String? = null,
    val rarityLabel: String? = null,
    val skyQuality: String? = null,
    val tradeableOnly: Boolean = false,
)

internal fun buildLibraryFilterOptions(
    extensions: List<ExtensionDefinition>,
    cards: List<CardDefinition>,
    variantProfiles: List<VariantProfile>,
): LibraryFilterOptions =
    LibraryFilterOptions(
        extensions = extensions.map { extension ->
            LibraryFilterOption(
                id = extension.id,
                label = extension.name,
            )
        },
        rarities = cards
            .map { it.rarityLabel }
            .distinct()
            .sortedWith(compareBy({ raritySortPriority(it) }, { it }))
            .map { rarity ->
                LibraryFilterOption(
                    id = rarity,
                    label = rarityFilterLabel(rarity),
                )
            },
        skyQualities = variantProfiles
            .flatMap { it.skyQualities }
            .distinctBy { it.code }
            .sortedWith(compareBy({ skyQualitySortPriority(it.code) }, { it.label }))
            .map { skyQuality ->
                LibraryFilterOption(
                    id = skyQuality.code,
                    label = skyQualityFilterLabel(skyQuality.code, skyQuality.label),
                )
            },
    )

private fun rarityFilterLabel(rarity: String): String = when (rarity) {
    "Common" -> "Com."
    "Uncommon" -> "Unc."
    "Rare" -> "Rare"
    "Epic" -> "Epic"
    else -> rarity
}

private fun skyQualityFilterLabel(code: String, fallback: String): String = when (code) {
    "city" -> "Ville"
    "suburban" -> "Péri."
    "rural" -> "Camp."
    "mountain" -> "Mont."
    "holographic" -> "Holo"
    else -> fallback
}

internal fun filterLibrarySections(
    sections: List<LibrarySection>,
    filters: LibraryFilters,
): List<LibrarySection> =
    sections.mapNotNull { section ->
        if (filters.extensionId != null && section.extension.id != filters.extensionId) {
            return@mapNotNull null
        }

        val filteredCards = section.cards.filter { card ->
            card.matchesLibraryFilters(filters)
        }
        section.copy(cards = filteredCards).takeIf { filteredCards.isNotEmpty() }
    }

internal fun LibraryCardItem.firstVariantMatching(filters: LibraryFilters): DisplayCardVariant? =
    availableVariants.firstOrNull { variant ->
        variant.matchesVariantFilters(filters)
    }

private fun LibraryCardItem.matchesLibraryFilters(filters: LibraryFilters): Boolean {
    if (filters.rarityLabel != null && definition.rarityLabel != filters.rarityLabel) {
        return false
    }
    if (filters.skyQuality == null && !filters.tradeableOnly) {
        return true
    }
    if (filters.skyQuality == null) {
        return hasTradeableVariant()
    }
    return availableVariants.any { variant ->
        variant.matchesVariantFilters(filters)
    }
}

private fun DisplayCardVariant.matchesVariantFilters(filters: LibraryFilters): Boolean {
    if (filters.skyQuality != null && skyQuality != filters.skyQuality) {
        return false
    }
    return !filters.tradeableOnly || canTradeAway()
}
