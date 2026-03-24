package fr.aumombelli.gatcha.model

import kotlinx.serialization.SerialName
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
    val extensionName: String,
    val ownedCount: Int,
    val availableVariants: List<DisplayCardVariant> = emptyList(),
)

data class LibrarySection(
    val extension: ExtensionDefinition,
    val cards: List<LibraryCardItem>,
)

data class DisplayCardVariant(
    val skyQuality: String,
    val skyQualityLabel: String,
    val finish: String,
    val finishLabel: String,
    val isHolographic: Boolean,
    val count: Int = 0,
) {
    val key: String get() = "$skyQuality::$finish"

    val selectorLabel: String
        get() = buildString {
            append(skyQualityLabel)
            if (finish != "standard") {
                append(" · ")
                append(finishLabel)
            }
            if (count > 0) {
                append(" ×")
                append(count)
            }
        }
}

data class DisplayCard(
    val definition: CardDefinition,
    val extensionName: String,
    val activeVariant: DisplayCardVariant,
    val availableVariants: List<DisplayCardVariant> = listOf(activeVariant),
)

@Serializable
data class AstronomyInfo(
    val commonName: String? = null,
    val primaryCatalogName: String,
    val catalogNumber: String,
    val objectFamily: String,
    val objectTypeLabel: String,
    val constellation: String,
    val mainSeason: String,
    val coordinates: CelestialCoordinates,
    val shortDescription: String,
    val details: AstronomyDetails,
)

@Serializable
data class CelestialCoordinates(
    val rightAscension: RightAscension,
    val declination: Declination,
    val label: String,
)

@Serializable
data class RightAscension(
    val hours: Int,
    val minutes: Int,
    val seconds: Double,
    val label: String,
)

@Serializable
data class Declination(
    val sign: String,
    val degrees: Int,
    val arcMinutes: Int,
    val arcSeconds: Int,
    val label: String,
)

@Serializable
data class LightYearMeasurement(
    val lightYears: Double,
    val label: String,
)

@Serializable
data class AngularMeasurement(
    val degrees: Int,
    val arcMinutes: Int,
    val arcSeconds: Int,
    val label: String,
)

@Serializable
data class VisualSize(
    val fullMoonWidth: Double,
    val fullMoonHeight: Double,
    val angularWidth: AngularMeasurement,
    val angularHeight: AngularMeasurement,
    val label: String,
)

@Serializable
data class AbsoluteMagnitudeMeasurement(
    val value: Double,
    val label: String,
)

@Serializable
sealed class AstronomyDetails

@Serializable
@SerialName("deep_sky")
data class DeepSkyDetails(
    val distance: LightYearMeasurement,
    val realSize: LightYearMeasurement,
    val visualSize: VisualSize,
    val absoluteMagnitude: AbsoluteMagnitudeMeasurement,
) : AstronomyDetails()

@Serializable
@SerialName("star")
data class StarDetails(
    val distance: LightYearMeasurement,
    val realSize: LightYearMeasurement? = null,
    val visualSize: VisualSize? = null,
    val absoluteMagnitude: AbsoluteMagnitudeMeasurement,
) : AstronomyDetails()

@Serializable
@SerialName("constellation")
data class ConstellationDetails(
    val visualSize: VisualSize,
) : AstronomyDetails()

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

fun VariantProfile.requireSkyQualityDefinition(code: String): SkyQualityDefinition =
    checkNotNull(skyQualities.firstOrNull { it.code == code }) {
        "Unknown sky quality '$code' for variant profile '$id'."
    }

fun VariantProfile.requireFinishDefinition(code: String): CardFinishDefinition =
    checkNotNull(finishes.firstOrNull { it.code == code }) {
        "Unknown finish '$code' for variant profile '$id'."
    }

fun VariantProfile.toDisplayVariant(
    skyQuality: String,
    finish: String,
    count: Int = 0,
): DisplayCardVariant {
    val skyQualityDefinition = requireSkyQualityDefinition(skyQuality)
    val finishDefinition = requireFinishDefinition(finish)
    return DisplayCardVariant(
        skyQuality = skyQualityDefinition.code,
        skyQualityLabel = skyQualityDefinition.label,
        finish = finishDefinition.code,
        finishLabel = finishDefinition.label,
        isHolographic = finishDefinition.isHolographic,
        count = count,
    )
}

fun CardVariant.toDisplayVariant(count: Int = 0): DisplayCardVariant =
    DisplayCardVariant(
        skyQuality = skyQuality,
        skyQualityLabel = skyQualityLabel,
        finish = finish,
        finishLabel = finishLabel,
        isHolographic = isHolographic,
        count = count,
    )

fun CardDefinition.toDisplayCard(
    extensionName: String,
    activeVariant: DisplayCardVariant,
    availableVariants: List<DisplayCardVariant> = listOf(activeVariant),
): DisplayCard =
    DisplayCard(
        definition = this,
        extensionName = extensionName,
        activeVariant = activeVariant,
        availableVariants = availableVariants,
    )

fun LibraryCardItem.toDisplayCard(selectedVariantKey: String? = null): DisplayCard? {
    val activeVariant = availableVariants.firstOrNull { it.key == selectedVariantKey }
        ?: availableVariants.firstOrNull()
        ?: return null
    return definition.toDisplayCard(
        extensionName = extensionName,
        activeVariant = activeVariant,
        availableVariants = availableVariants,
    )
}

fun DisplayCard.withSelectedVariant(selectedVariantKey: String?): DisplayCard {
    val selectedVariant = availableVariants.firstOrNull { it.key == selectedVariantKey }
        ?: availableVariants.firstOrNull()
        ?: activeVariant
    return copy(activeVariant = selectedVariant)
}

fun OwnedCardEntry.toDisplayVariants(variantProfile: VariantProfile): List<DisplayCardVariant> =
    normalized().variants
        .map { variantCount ->
            variantProfile.toDisplayVariant(
                skyQuality = variantCount.skyQuality,
                finish = variantCount.finish,
                count = variantCount.count,
            )
        }
        .sortedWith(
            compareByDescending<DisplayCardVariant> { it.isHolographic }
                .thenByDescending { skyQualityRank(it.skyQuality) }
                .thenByDescending { it.count }
                .thenBy { it.skyQualityLabel }
                .thenBy { it.finishLabel },
        )

private fun skyQualityRank(code: String): Int = when (code) {
    "mountain" -> 4
    "rural" -> 3
    "suburban" -> 2
    "city" -> 1
    else -> 0
}
