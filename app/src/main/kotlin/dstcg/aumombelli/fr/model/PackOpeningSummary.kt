package fr.aumombelli.dstcg.model

data class PackOpeningSummary(
    val highestRarityLabel: String,
    val hasHolographicCard: Boolean,
)

fun summarizePackOpening(displayCards: List<DisplayCard>): PackOpeningSummary? {
    if (displayCards.isEmpty()) return null

    val highestRarityLabel = displayCards
        .maxBy { raritySortPriority(it.definition.rarityLabel) }
        .definition
        .rarityLabel

    return PackOpeningSummary(
        highestRarityLabel = highestRarityLabel,
        hasHolographicCard = displayCards.any { it.activeVariant.isHolographic },
    )
}
