package fr.aumombelli.gatcha.model

import kotlinx.serialization.Serializable

@Serializable
data class OwnedCollection(
    val version: Int = 1,
    val cards: Map<String, OwnedCardEntry> = emptyMap(),
)

@Serializable
data class OwnedCardEntry(
    val totalOwned: Int = 0,
    val variants: List<OwnedVariantCount> = emptyList(),
)

@Serializable
data class OwnedVariantCount(
    val skyQuality: String,
    val finish: String,
    val count: Int,
)

data class StandaloneProgress(
    val collection: OwnedCollection,
    val nextDrawAt: String? = null,
)
