package fr.aumombelli.dstcg.data

import fr.aumombelli.dstcg.model.CardDefinition
import fr.aumombelli.dstcg.model.MiniGameCardResolutionSource
import fr.aumombelli.dstcg.model.MiniGameGlobalCardRef
import fr.aumombelli.dstcg.model.MiniGameId
import fr.aumombelli.dstcg.model.MiniGameOwnedVariantRef
import fr.aumombelli.dstcg.model.MiniGameResolvedCardRef
import fr.aumombelli.dstcg.model.OwnedCollection
import fr.aumombelli.dstcg.model.normalized

class MiniGameCardResolver(
    private val drawPolicy: MiniGameDeterministicDrawPolicy = MiniGameDeterministicDrawPolicy(),
) {
    fun resolve(
        globalCard: MiniGameGlobalCardRef,
        cards: List<CardDefinition>,
        collection: OwnedCollection,
        miniGameId: MiniGameId,
        dateUtc: String,
        slot: Int,
        eligibleCardIds: Set<String>? = null,
        excludedOwnedCardIds: Set<String> = emptySet(),
    ): MiniGameResolvedCardRef? {
        val ownedVariants = collection.ownedMiniGameVariants(cards)
            .filter { eligibleCardIds == null || it.cardId in eligibleCardIds }
            .filter { it.cardId !in excludedOwnedCardIds }
        val globalCardCandidates = ownedVariants.filter { it.cardId == globalCard.cardId }
        val sameExtensionCandidates = ownedVariants.filter { it.extensionId == globalCard.extensionId }
        val otherExtensionCandidates = ownedVariants.filter { it.extensionId != globalCard.extensionId }

        val sourceAndCandidate = when {
            globalCardCandidates.isNotEmpty() -> MiniGameCardResolutionSource.GlobalCard to globalCardCandidates
            sameExtensionCandidates.isNotEmpty() -> MiniGameCardResolutionSource.SameExtensionFallback to sameExtensionCandidates
            otherExtensionCandidates.isNotEmpty() -> MiniGameCardResolutionSource.AnyExtensionFallback to otherExtensionCandidates
            else -> return null
        }

        val source = sourceAndCandidate.first
        val selectedVariant = chooseOwnedVariant(
            candidates = sourceAndCandidate.second,
            globalCard = globalCard,
            miniGameId = miniGameId,
            dateUtc = dateUtc,
            slot = slot,
            source = source,
        )

        return MiniGameResolvedCardRef(
            globalCard = globalCard,
            ownedVariant = selectedVariant,
            source = source,
        )
    }

    private fun chooseOwnedVariant(
        candidates: List<MiniGameOwnedVariantRef>,
        globalCard: MiniGameGlobalCardRef,
        miniGameId: MiniGameId,
        dateUtc: String,
        slot: Int,
        source: MiniGameCardResolutionSource,
    ): MiniGameOwnedVariantRef =
        candidates.minWith(
            compareBy<MiniGameOwnedVariantRef> {
                stableMiniGameHash(
                    "owned-variant",
                    "v${drawPolicy.algorithmVersion}",
                    miniGameId.name,
                    dateUtc,
                    slot.coerceAtLeast(0).toString(),
                    source.name,
                    globalCard.extensionId,
                    globalCard.cardId,
                    it.extensionId,
                    it.cardId,
                    it.skyQuality,
                    it.finish,
                )
            }.thenBy { it.extensionId }
                .thenBy { it.cardId }
                .thenBy { it.skyQuality }
                .thenBy { it.finish },
        )
}

internal fun OwnedCollection.ownedMiniGameVariants(
    cards: List<CardDefinition>,
): List<MiniGameOwnedVariantRef> {
    val cardsById = cards.associateBy(CardDefinition::id)
    return normalized().cards.flatMap { (cardId, entry) ->
        val definition = cardsById[cardId] ?: return@flatMap emptyList()
        entry.normalized().variants
            .filter { it.count > 0 }
            .map { variant ->
                MiniGameOwnedVariantRef(
                    cardId = definition.id,
                    extensionId = definition.extensionId,
                    skyQuality = variant.skyQuality,
                    finish = variant.finish,
                )
            }
    }
}
