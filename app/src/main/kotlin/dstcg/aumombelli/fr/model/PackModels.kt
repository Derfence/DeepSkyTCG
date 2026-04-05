package fr.aumombelli.dstcg.model

data class DrawPackResponse(
    val extensionId: String,
    val drawnAt: String,
    val rechargeState: PackRechargeState,
    val revealSlots: List<PackRevealSlot>,
) {
    companion object {
        fun fromCards(
            extensionId: String,
            drawnAt: String,
            rechargeState: PackRechargeState,
            cards: List<PackCard>,
        ): DrawPackResponse = DrawPackResponse(
            extensionId = extensionId,
            drawnAt = drawnAt,
            rechargeState = rechargeState,
            revealSlots = cards.mapIndexed { index, card ->
                AstronomyPackRevealSlot(
                    slotIndex = index,
                    card = card,
                )
            },
        )
    }

    val cards: List<PackCard>
        get() = revealSlots
            .filterIsInstance<AstronomyPackRevealSlot>()
            .map { it.card }

    val equipmentCards: List<EquipmentCardDefinition>
        get() = revealSlots
            .filterIsInstance<EquipmentPackRevealSlot>()
            .map { it.definition }
}

sealed interface PackRevealSlot {
    val slotIndex: Int
}

data class AstronomyPackRevealSlot(
    override val slotIndex: Int,
    val card: PackCard,
) : PackRevealSlot

data class EquipmentPackRevealSlot(
    override val slotIndex: Int,
    val definition: EquipmentCardDefinition,
) : PackRevealSlot

data class PackCard(
    val cardId: String,
    val name: String,
    val rarityLabel: String,
    val imageRef: String,
    val variant: CardVariant,
)

data class CardVariant(
    val skyQuality: String,
    val skyQualityLabel: String,
    val finish: String,
    val finishLabel: String,
    val isHolographic: Boolean,
)

fun List<PackCard>.sortedForPackReveal(): List<PackCard> =
    withIndex()
        .sortedWith(
            compareBy<IndexedValue<PackCard>> { it.value.variant.isHolographic }
                .thenBy { raritySortPriority(it.value.rarityLabel) }
                .thenBy { skyQualitySortPriority(it.value.variant.skyQuality) }
                .thenBy { it.index },
        )
        .map { it.value }
