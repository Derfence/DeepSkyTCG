package fr.aumombelli.dstcg.feature.library

import fr.aumombelli.dstcg.model.LibraryCardItem
import fr.aumombelli.dstcg.model.LibrarySection
import fr.aumombelli.dstcg.model.raritySortPriority

internal enum class LibraryCardDialogMode {
    Preview,
    Fullscreen,
}

internal data class LibraryCardNavigationState(
    val cardId: String,
    val mode: LibraryCardDialogMode,
)

internal fun buildNavigableLibraryCards(sections: List<LibrarySection>): List<LibraryCardItem> =
    sections.flatMap { section ->
        section.cards
            .groupedByLibraryRarity()
            .flatMap { (_, cards) -> cards }
    }.filter { card ->
        card.ownedCount > 0 && card.availableVariants.isNotEmpty()
    }

internal fun List<LibraryCardItem>.groupedByLibraryRarity(): List<Pair<String, List<LibraryCardItem>>> =
    groupBy { card -> card.definition.rarityLabel }
        .toList()
        .sortedWith(
            compareBy(
                { (rarityLabel, _) -> raritySortPriority(rarityLabel) },
                { (rarityLabel, _) -> rarityLabel },
            ),
        )

internal fun LibraryCardItem.defaultLibraryVariantKey(filters: LibraryFilters): String? =
    if (filters.affectsVariantChoice()) {
        bestVariantMatching(filters)?.key
    } else {
        null
    } ?: availableVariants.firstOrNull()?.key
