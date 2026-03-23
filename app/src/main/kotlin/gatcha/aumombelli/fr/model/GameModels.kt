package fr.aumombelli.gatcha.model

import kotlinx.serialization.Serializable

@Serializable
data class ExtensionDefinition(
    val id: String,
    val name: String,
    val coverImageRef: String,
)

@Serializable
data class CardDefinition(
    val id: String,
    val extensionId: String,
    val name: String,
    val rarityLabel: String,
    val drawWeight: Int,
    val imageRef: String,
)

@Serializable
data class OwnedCollection(
    val version: Int = 1,
    val cards: Map<String, Int> = emptyMap(),
)

@Serializable
data class PackCard(
    val cardId: String,
    val name: String,
    val rarityLabel: String,
    val imageRef: String,
)

data class SessionCredentials(
    val username: String,
    val passwordHash: String,
)

data class StoredSessionSnapshot(
    val lastUsername: String? = null,
    val lastCollectionBlob: String? = null,
    val pendingCollectionBlob: String? = null,
    val pendingPackJson: String? = null,
    val nextDrawAt: String? = null,
    val lastSavedAt: String? = null,
)

data class LibraryCardItem(
    val definition: CardDefinition,
    val ownedCount: Int,
)

data class LibrarySection(
    val extension: ExtensionDefinition,
    val cards: List<LibraryCardItem>,
)

fun OwnedCollection.mergePackCards(cards: List<PackCard>): OwnedCollection {
    val merged = this.cards.toMutableMap()
    cards.forEach { card ->
        merged[card.cardId] = (merged[card.cardId] ?: 0) + 1
    }
    return copy(cards = merged.toSortedMap())
}
