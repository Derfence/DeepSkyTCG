package fr.aumombelli.dstcg.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TradeCardRef(
    @SerialName("id")
    val cardId: String,
    @SerialName("sq")
    val skyQuality: String,
    @SerialName("fn")
    val finish: String,
) {
    val variantKey: String get() = "$skyQuality::$finish"
}

@Serializable
data class TradeLedgerState(
    val completedTradeIds: List<String> = emptyList(),
    val pendingTrade: PendingTradeState? = null,
    val pendingTradeId: String? = null,
) {
    fun hasCompleted(tradeId: String): Boolean = tradeId in completedTradeIds

    fun hasPending(tradeId: String): Boolean =
        pendingTrade?.tradeId == tradeId || pendingTradeId == tradeId

    fun beginPending(
        tradeId: String,
        outgoing: TradeCardRef,
        incoming: TradeCardRef,
    ): TradeLedgerState =
        if (hasCompleted(tradeId)) {
            this
        } else {
            copy(
                pendingTrade = PendingTradeState(
                    tradeId = tradeId,
                    outgoing = outgoing,
                    incoming = incoming,
                ),
                pendingTradeId = tradeId,
            )
        }

    fun clearPending(tradeId: String): TradeLedgerState =
        if (hasPending(tradeId)) {
            copy(
                pendingTrade = null,
                pendingTradeId = null,
            )
        } else {
            this
        }

    fun markCompleted(tradeId: String, maxRememberedTrades: Int = 32): TradeLedgerState =
        copy(
            completedTradeIds = (completedTradeIds + tradeId).distinct().takeLast(maxRememberedTrades),
            pendingTrade = null,
            pendingTradeId = null,
        )
}

@Serializable
data class PendingTradeState(
    val tradeId: String,
    val outgoing: TradeCardRef,
    val incoming: TradeCardRef,
)

data class TradeCardCandidate(
    val card: CardDefinition,
    val extensionName: String,
    val variant: DisplayCardVariant,
)

const val MINIMUM_TRADE_VARIANT_COUNT: Int = 2

sealed interface TradeValidationResult {
    data object Valid : TradeValidationResult

    data class Invalid(
        val message: String,
    ) : TradeValidationResult
}

fun TradeValidationResult.isValid(): Boolean = this is TradeValidationResult.Valid

fun OwnedCollection.tradeCountFor(ref: TradeCardRef): Int =
    cards[ref.cardId]
        ?.normalized()
        ?.variants
        ?.firstOrNull { it.skyQuality == ref.skyQuality && it.finish == ref.finish }
        ?.count
        ?: 0

fun OwnedCollection.canTradeAway(ref: TradeCardRef): Boolean =
    tradeCountFor(ref) >= MINIMUM_TRADE_VARIANT_COUNT

fun OwnedVariantCount.canTradeAway(): Boolean = count >= MINIMUM_TRADE_VARIANT_COUNT

fun DisplayCardVariant.canTradeAway(): Boolean = count >= MINIMUM_TRADE_VARIANT_COUNT

fun LibraryCardItem.hasTradeableVariant(): Boolean = availableVariants.any(DisplayCardVariant::canTradeAway)

fun LibraryCardItem.firstTradeableVariant(): DisplayCardVariant? =
    availableVariants.firstOrNull(DisplayCardVariant::canTradeAway)

fun OwnedCollection.applyTrade(
    outgoing: TradeCardRef,
    incoming: TradeCardRef,
): OwnedCollection =
    decrementVariant(outgoing)
        .incrementVariant(incoming)
        .normalized()

fun validateTradePair(
    localCollection: OwnedCollection,
    localOutgoing: TradeCardRef,
    remoteOutgoing: TradeCardRef,
    cardsById: Map<String, CardDefinition>,
    variantProfilesById: Map<String, VariantProfile>,
): TradeValidationResult {
    val localCard = cardsById[localOutgoing.cardId]
        ?: return TradeValidationResult.Invalid("Carte locale inconnue.")
    val remoteCard = cardsById[remoteOutgoing.cardId]
        ?: return TradeValidationResult.Invalid("Carte distante inconnue.")

    if (!localCollection.canTradeAway(localOutgoing)) {
        return TradeValidationResult.Invalid("Cette variante n'est pas disponible en doublon.")
    }
    if (!localCard.supportsTradeRef(localOutgoing, variantProfilesById)) {
        return TradeValidationResult.Invalid("La variante locale n'existe pas dans le catalogue.")
    }
    if (!remoteCard.supportsTradeRef(remoteOutgoing, variantProfilesById)) {
        return TradeValidationResult.Invalid("La variante distante n'existe pas dans le catalogue.")
    }
    if (localCard.rarityLabel != remoteCard.rarityLabel) {
        return TradeValidationResult.Invalid("Les deux cartes n'ont pas la même rareté.")
    }
    if (localOutgoing.variantKey != remoteOutgoing.variantKey) {
        return TradeValidationResult.Invalid("Les deux cartes n'ont pas la même variante.")
    }
    if (localOutgoing == remoteOutgoing) {
        return TradeValidationResult.Invalid("L'échange de deux variantes identiques est inutile.")
    }
    return TradeValidationResult.Valid
}

private fun CardDefinition.supportsTradeRef(
    ref: TradeCardRef,
    variantProfilesById: Map<String, VariantProfile>,
): Boolean {
    val profile = variantProfilesById[variantProfileId] ?: return false
    return profile.skyQualities.any { it.code == ref.skyQuality } &&
        profile.finishes.any { it.code == ref.finish }
}

private fun OwnedCollection.decrementVariant(ref: TradeCardRef): OwnedCollection {
    val updatedCards = cards.toMutableMap()
    val entry = updatedCards[ref.cardId]?.normalized() ?: return this
    val variants = entry.variants.mapNotNull { variant ->
        if (variant.skyQuality == ref.skyQuality && variant.finish == ref.finish) {
            val nextCount = variant.count - 1
            if (nextCount > 0) variant.copy(count = nextCount) else null
        } else {
            variant
        }
    }
    if (variants.isEmpty()) {
        updatedCards.remove(ref.cardId)
    } else {
        updatedCards[ref.cardId] = OwnedCardEntry(variants = variants).normalized()
    }
    return copy(cards = updatedCards.toSortedMap()).normalized()
}

private fun OwnedCollection.incrementVariant(ref: TradeCardRef): OwnedCollection {
    val updatedCards = cards.toMutableMap()
    val entry = updatedCards[ref.cardId]?.normalized() ?: OwnedCardEntry()
    val variants = entry.variants.toMutableList()
    val existingIndex = variants.indexOfFirst {
        it.skyQuality == ref.skyQuality && it.finish == ref.finish
    }
    if (existingIndex >= 0) {
        val variant = variants[existingIndex]
        variants[existingIndex] = variant.copy(count = variant.count + 1)
    } else {
        variants += OwnedVariantCount(
            skyQuality = ref.skyQuality,
            finish = ref.finish,
            count = 1,
        )
    }
    updatedCards[ref.cardId] = OwnedCardEntry(variants = variants).normalized()
    return copy(cards = updatedCards.toSortedMap()).normalized()
}
