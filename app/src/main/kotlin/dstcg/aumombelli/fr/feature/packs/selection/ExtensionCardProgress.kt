package fr.aumombelli.dstcg.feature.packs.selection

import fr.aumombelli.dstcg.model.CardDefinition
import fr.aumombelli.dstcg.model.ExtensionDefinition
import fr.aumombelli.dstcg.model.OwnedCollection
import fr.aumombelli.dstcg.model.ownedCountFor

data class ExtensionCardProgress(
    val obtainedCount: Int = 0,
    val totalCount: Int = 0,
) {
    val displayLabel: String
        get() = "$obtainedCount/$totalCount"
}

internal fun buildExtensionCardProgress(
    extensions: List<ExtensionDefinition>,
    cards: List<CardDefinition>,
    collection: OwnedCollection,
): Map<String, ExtensionCardProgress> {
    val cardsByExtensionId = cards.groupBy(CardDefinition::extensionId)
    return extensions.associate { extension ->
        val extensionCards = cardsByExtensionId[extension.id].orEmpty()
        val obtainedCount = extensionCards.count { card -> collection.ownedCountFor(card.id) > 0 }
        extension.id to ExtensionCardProgress(
            obtainedCount = obtainedCount,
            totalCount = extensionCards.size,
        )
    }
}
