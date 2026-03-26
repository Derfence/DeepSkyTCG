package fr.aumombelli.gatcha.model

import kotlinx.serialization.Serializable

@Serializable
data class CatalogMetadata(
    val catalogVersion: Int,
)

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
    val skyQualityWeights: List<WeightedCode>,
    val finishWeights: List<WeightedCode>,
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
    val drawWeight: Int,
    val imageRef: String,
    val variantProfileId: String,
    val astronomy: AstronomyInfo,
)
