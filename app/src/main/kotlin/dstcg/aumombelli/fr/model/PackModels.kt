package fr.aumombelli.dstcg.model

import kotlinx.serialization.Serializable

@Serializable
data class DrawPackResponse(
    val extensionId: String,
    val drawnAt: String,
    val rechargeState: PackRechargeState,
    val cards: List<PackCard>,
)

@Serializable
data class PackCard(
    val cardId: String,
    val name: String,
    val rarityLabel: String,
    val imageRef: String,
    val variant: CardVariant,
)

@Serializable
data class CardVariant(
    val skyQuality: String,
    val skyQualityLabel: String,
    val finish: String,
    val finishLabel: String,
    val isHolographic: Boolean,
)
