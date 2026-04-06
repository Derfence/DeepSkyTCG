package fr.aumombelli.dstcg.model

import kotlinx.serialization.Serializable

@Serializable
data class ExtensionDefinition(
    val id: String,
    val name: String,
    val coverImageRef: String,
)

@Serializable
data class VariantProfile(
    val id: String,
    val skyQualities: List<SkyQualityDefinition>,
    val finishes: List<CardFinishDefinition>,
)

@Serializable
data class SkyQualityDefinition(
    val code: String,
    val label: String,
)

@Serializable
data class CardFinishDefinition(
    val code: String,
    val label: String,
    val isHolographic: Boolean = false,
)

@Serializable
data class WeightedCode(
    val code: String,
    val weight: Int,
)

@Serializable
data class CardDefinition(
    val id: String,
    val extensionId: String,
    val name: String,
    val rarityLabel: String,
    val cardRarityMultiplier: Double,
    val imageRef: String,
    val variantProfileId: String,
    val astronomy: AstronomyInfo,
)

@Serializable
data class GameBalanceDefinition(
    val cardsPerDraw: Int,
    val drawCooldownHours: Double,
    val percentUncommonPerDay: Double,
    val percentRarePerDay: Double,
    val percentEpicPerDay: Double,
    val suburbanMeanPerDay: Double,
    val ruralMeanPerDay: Double,
    val mountainMeanPerDay: Double,
    val percentHoloMeanPerDay: Double,
)
