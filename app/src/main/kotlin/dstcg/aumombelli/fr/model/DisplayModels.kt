package fr.aumombelli.dstcg.model

data class LibraryCardItem(
    val definition: CardDefinition,
    val extensionName: String,
    val ownedCount: Int,
    val showNewIndicator: Boolean = false,
    val availableVariants: List<DisplayCardVariant> = emptyList(),
)

data class LibrarySection(
    val extension: ExtensionDefinition,
    val cards: List<LibraryCardItem>,
)

data class DisplayCardVariant(
    val skyQuality: String,
    val skyQualityLabel: String,
    val finish: String,
    val finishLabel: String,
    val isHolographic: Boolean,
    val count: Int = 0,
    val isStamped: Boolean = false,
) {
    val key: String get() = "$skyQuality::$finish"

    val selectorLabel: String
        get() = buildString {
            append(skyQualityLabel)
            if (finish != "standard") {
                append(" · ")
                append(finishLabel)
            }
            if (count > 0) {
                append(" ×")
                append(count)
            }
        }
}

data class DisplayCard(
    val definition: CardDefinition,
    val extensionName: String,
    val activeVariant: DisplayCardVariant,
    val availableVariants: List<DisplayCardVariant> = listOf(activeVariant),
)

fun raritySortPriority(rarityLabel: String): Int = when (rarityLabel) {
    "Common" -> 0
    "Uncommon" -> 1
    "Rare" -> 2
    "Epic" -> 3
    else -> 4
}

fun skyQualitySortPriority(code: String): Int = when (code) {
    "city" -> 1
    "suburban" -> 2
    "rural" -> 3
    "mountain" -> 4
    "holographic" -> 5
    else -> 0
}
