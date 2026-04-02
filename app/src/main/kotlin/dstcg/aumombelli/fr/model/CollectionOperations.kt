package fr.aumombelli.dstcg.model

fun OwnedCollection.mergePackCards(cards: List<PackCard>): OwnedCollection {
    val merged = this.cards.mapValues { (_, entry) -> entry.normalized() }.toMutableMap()
    cards.forEach { card ->
        val current = merged[card.cardId]?.normalized() ?: OwnedCardEntry()
        val updatedVariants = current.variants.toMutableList()
        val variantIndex = updatedVariants.indexOfFirst {
            it.skyQuality == card.variant.skyQuality && it.finish == card.variant.finish
        }
        if (variantIndex >= 0) {
            val variant = updatedVariants[variantIndex]
            updatedVariants[variantIndex] = variant.copy(count = variant.count + 1)
        } else {
            updatedVariants += OwnedVariantCount(
                skyQuality = card.variant.skyQuality,
                finish = card.variant.finish,
                count = 1,
            )
        }
        merged[card.cardId] = OwnedCardEntry(
            totalOwned = current.totalOwned + 1,
            variants = updatedVariants,
        ).normalized()
    }
    return copy(cards = merged.toSortedMap()).normalized()
}

fun OwnedCollection.ownedCountFor(cardId: String): Int = cards[cardId]?.normalized()?.totalOwned ?: 0

fun OwnedCollection.normalized(): OwnedCollection =
    copy(cards = cards.mapValues { (_, entry) -> entry.normalized() }.toSortedMap())

fun OwnedCardEntry.normalized(): OwnedCardEntry {
    val mergedVariants = variants
        .groupBy { it.skyQuality to it.finish }
        .map { (key, counts) ->
            OwnedVariantCount(
                skyQuality = key.first,
                finish = key.second,
                count = counts.sumOf { it.count },
            )
        }
        .sortedWith(compareBy<OwnedVariantCount> { it.skyQuality }.thenBy { it.finish })

    return copy(
        totalOwned = mergedVariants.sumOf { it.count },
        variants = mergedVariants,
    )
}
