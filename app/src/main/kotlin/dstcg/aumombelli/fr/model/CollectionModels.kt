package fr.aumombelli.dstcg.model

import kotlinx.serialization.Serializable

@Serializable
data class OwnedCollection(
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
    val availableDrawCount: Int = 10,
    val nextChargeAt: String? = null,
    val openedPackCount: Int = 0,
)
