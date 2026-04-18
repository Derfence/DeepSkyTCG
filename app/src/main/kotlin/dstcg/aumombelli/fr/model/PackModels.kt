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
    val isStamped: Boolean,
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

fun List<PackRevealSlot>.sortedRevealSlotsForPackReveal(): List<PackRevealSlot> =
    withIndex()
        .sortedWith(
            compareBy<IndexedValue<PackRevealSlot>> { indexedValue ->
                when (val slot = indexedValue.value) {
                    is AstronomyPackRevealSlot -> slot.card.variant.isHolographic
                    is EquipmentPackRevealSlot -> false
                }
            }.thenBy { indexedValue ->
                when (val slot = indexedValue.value) {
                    is AstronomyPackRevealSlot -> raritySortPriority(slot.card.rarityLabel)
                    is EquipmentPackRevealSlot -> raritySortPriority("Common")
                }
            }.thenBy { indexedValue ->
                when (val slot = indexedValue.value) {
                    is AstronomyPackRevealSlot -> skyQualitySortPriority(slot.card.variant.skyQuality)
                    is EquipmentPackRevealSlot -> Int.MAX_VALUE
                }
            }.thenBy { indexedValue ->
                when (val slot = indexedValue.value) {
                    is AstronomyPackRevealSlot -> ""
                    is EquipmentPackRevealSlot -> slot.definition.type.code
                }
            }.thenBy { indexedValue ->
                when (val slot = indexedValue.value) {
                    is AstronomyPackRevealSlot -> 0
                    is EquipmentPackRevealSlot -> slot.definition.level
                }
            }.thenBy { it.index },
        )
        .map { it.value }
