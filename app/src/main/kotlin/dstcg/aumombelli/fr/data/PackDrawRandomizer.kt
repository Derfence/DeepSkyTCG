package fr.aumombelli.dstcg.data

import fr.aumombelli.dstcg.model.AstronomyPackRevealSlot
import fr.aumombelli.dstcg.model.CardVariant
import fr.aumombelli.dstcg.model.EquipmentCardDefinition
import fr.aumombelli.dstcg.model.PackCard
import fr.aumombelli.dstcg.model.WeightedCode
import fr.aumombelli.dstcg.model.raritySortPriority
import fr.aumombelli.dstcg.model.requireFinishDefinition
import fr.aumombelli.dstcg.model.requireSkyQualityDefinition
import kotlin.math.abs

internal class PackDrawRandomizer(
    private val entropySource: EntropySource,
) {
    fun drawAstronomyRevealSlot(
        plannedSlot: PlannedRevealSlot,
        extensionPlan: ExtensionDrawPlan,
        variantProfilesById: Map<String, fr.aumombelli.dstcg.model.VariantProfile>,
        runtimeCatalog: RuntimeCatalogBalance,
        activeBonus: ActiveEquipmentBonus,
        excludedCardIds: MutableSet<String>,
        maxAllowedRarityLabel: String?,
        variantDrawPolicy: AstronomyVariantDrawPolicy,
        holographicCardsAlreadyDrawn: Int,
    ): AstronomyPackRevealSlot {
        val definition = pickUniqueAstronomyCardDefinition(
            plannedSlot = plannedSlot,
            extensionPlan = extensionPlan,
            excludedCardIds = excludedCardIds,
            maxAllowedRarityLabel = maxAllowedRarityLabel,
        )
        excludedCardIds += definition.id
        val variantProfile = checkNotNull(variantProfilesById[definition.variantProfileId]) {
            "Profil de variante inconnu '${definition.variantProfileId}' pour la carte '${definition.id}'."
        }
        val runtimeVariantWeights = checkNotNull(runtimeCatalog.variantWeightsByProfileId[definition.variantProfileId]) {
            "Poids de variante introuvables pour '${definition.variantProfileId}'."
        }
        val skyQuality = pickSkyQualityWithEquipmentBonus(
            variantProfile = variantProfile,
            runtimeVariantWeights = runtimeVariantWeights,
            holographicQualityPercent = activeBonus.holographicQualityPercent,
            allowHolographic =
                variantDrawPolicy.allowHolographic &&
                    holographicCardsAlreadyDrawn < variantDrawPolicy.maxHolographicCardsPerPack,
        )
        val finish = pickFinishWithPolicy(
            variantProfile = variantProfile,
            runtimeVariantWeights = runtimeVariantWeights,
            allowStamped = variantDrawPolicy.allowStamped,
        )
        return AstronomyPackRevealSlot(
            slotIndex = plannedSlot.slotIndex,
            card = PackCard(
                cardId = definition.id,
                name = definition.name,
                rarityLabel = definition.rarityLabel,
                imageRef = definition.imageRef,
                variant = CardVariant(
                    skyQuality = skyQuality.code,
                    skyQualityLabel = skyQuality.label,
                    finish = finish.code,
                    finishLabel = finish.label,
                    isHolographic = skyQuality.isHolographic,
                    isStamped = finish.isStamped,
                ),
            ),
        )
    }

    fun <T> pickWeighted(entries: List<T>, weightOf: (T) -> Int): T {
        val totalWeight = entries.sumOf(weightOf)
        require(totalWeight > 0) { "Les poids doivent etre strictement positifs." }
        var cursor = entropySource.nextInt(totalWeight)
        for (entry in entries) {
            cursor -= weightOf(entry)
            if (cursor < 0) {
                return entry
            }
        }
        return entries.last()
    }

    fun maybePromoteRarity(
        baseRarity: String,
        availableRarities: Set<String>,
        rarityBoostPercent: Double,
    ): String {
        if (rarityBoostPercent <= 0.0 || baseRarity == "Epic") {
            return baseRarity
        }
        if (!rollPercent(rarityBoostPercent)) {
            return baseRarity
        }
        return nextHigherConfiguredRarity(
            currentRarity = baseRarity,
            availableRarities = availableRarities,
        )
    }

    fun clampAstronomyRarity(
        rarityLabel: String,
        availableRarities: Set<String>,
        maxAllowedRarityLabel: String?,
    ): String {
        if (maxAllowedRarityLabel == null) {
            return rarityLabel
        }
        if (raritySortPriority(rarityLabel) <= raritySortPriority(maxAllowedRarityLabel)) {
            return rarityLabel
        }
        return availableRarities
            .filter { rarity -> raritySortPriority(rarity) <= raritySortPriority(maxAllowedRarityLabel) }
            .maxByOrNull(::raritySortPriority)
            ?: availableRarities.minByOrNull(::raritySortPriority)
            ?: rarityLabel
    }

    fun maybeDrawEquipmentReward(
        rarityLabel: String,
        equipmentCards: List<EquipmentCardDefinition>,
        replacementChancePercent: Double,
        excludedEquipmentDefinitionIds: Set<String>,
    ): EquipmentCardDefinition? {
        if (rarityLabel != "Common") {
            return null
        }
        val weightedEquipmentCards = equipmentCards.filter { card ->
            card.dropWeight > 0 && card.id !in excludedEquipmentDefinitionIds
        }
        if (weightedEquipmentCards.isEmpty()) {
            return null
        }
        if (!rollPercent(replacementChancePercent)) {
            return null
        }
        return pickWeighted(weightedEquipmentCards) { it.dropWeight }
    }

    private fun pickUniqueAstronomyCardDefinition(
        plannedSlot: PlannedRevealSlot,
        extensionPlan: ExtensionDrawPlan,
        excludedCardIds: Set<String>,
        maxAllowedRarityLabel: String?,
    ) = pickWeighted(
        findUniqueAstronomyCandidatePool(
            plannedSlot = plannedSlot,
            extensionPlan = extensionPlan,
            excludedCardIds = excludedCardIds,
            maxAllowedRarityLabel = maxAllowedRarityLabel,
        ),
    ) { it.weight }.card

    private fun findUniqueAstronomyCandidatePool(
        plannedSlot: PlannedRevealSlot,
        extensionPlan: ExtensionDrawPlan,
        excludedCardIds: Set<String>,
        maxAllowedRarityLabel: String?,
    ): List<WeightedCardDefinition> {
        val targetRarityPool = checkNotNull(extensionPlan.cardsByRarity[plannedSlot.rarityLabel]) {
            "Aucune carte n'a ete configuree pour la rarete '${plannedSlot.rarityLabel}'."
        }.filterNot { candidate -> candidate.card.id in excludedCardIds }
        if (targetRarityPool.isNotEmpty()) {
            return targetRarityPool
        }

        val targetRarityPriority = raritySortPriority(plannedSlot.rarityLabel)
        val fallbackPool = extensionPlan.cardsByRarity.keys
            .asSequence()
            .filterNot { rarity -> rarity == plannedSlot.rarityLabel }
            .filter { rarity ->
                maxAllowedRarityLabel == null ||
                    raritySortPriority(rarity) <= raritySortPriority(maxAllowedRarityLabel)
            }
            .sortedWith(
                compareBy<String>(
                    { rarity -> abs(raritySortPriority(rarity) - targetRarityPriority) },
                    { rarity -> -extensionPlan.rarityProbabilities.getValue(rarity) },
                    ::raritySortPriority,
                ),
            )
            .mapNotNull { rarity ->
                extensionPlan.cardsByRarity
                    .getValue(rarity)
                    .filterNot { candidate -> candidate.card.id in excludedCardIds }
                    .takeIf { candidates -> candidates.isNotEmpty() }
            }
            .firstOrNull()

        return requireNotNull(fallbackPool) {
            "Le tirage requiert plus de cartes uniques que l'extension n'en propose."
        }
    }

    private fun pickWeightedCode(options: List<WeightedCode>): String =
        pickWeighted(options) { it.weight }.code

    private fun pickSkyQualityWithEquipmentBonus(
        variantProfile: fr.aumombelli.dstcg.model.VariantProfile,
        runtimeVariantWeights: RuntimeVariantWeights,
        holographicQualityPercent: Double,
        allowHolographic: Boolean,
    ): fr.aumombelli.dstcg.model.SkyQualityDefinition {
        val skyWeightsByCode = runtimeVariantWeights.skyQualityWeights.associateBy { it.code }
        val holographicEntries = variantProfile.skyQualities
            .filter { it.isHolographic }
            .mapNotNull { skyQuality -> skyWeightsByCode[skyQuality.code] }
        val nonHolographicEntries = variantProfile.skyQualities
            .filterNot { it.isHolographic }
            .mapNotNull { skyQuality -> skyWeightsByCode[skyQuality.code] }

        if (!allowHolographic) {
            val selectedPool = nonHolographicEntries.ifEmpty { runtimeVariantWeights.skyQualityWeights }
            return variantProfile.requireSkyQualityDefinition(
                pickWeighted(selectedPool) { it.weight }.code,
            )
        }

        if (holographicQualityPercent <= 0.0) {
            return variantProfile.requireSkyQualityDefinition(
                pickWeightedCode(runtimeVariantWeights.skyQualityWeights),
            )
        }

        if (holographicEntries.isEmpty() || nonHolographicEntries.isEmpty()) {
            return variantProfile.requireSkyQualityDefinition(
                pickWeightedCode(runtimeVariantWeights.skyQualityWeights),
            )
        }

        val totalWeight = runtimeVariantWeights.skyQualityWeights.sumOf { it.weight }.coerceAtLeast(1)
        val baseHolographicProbability = holographicEntries.sumOf { it.weight }.toDouble() / totalWeight.toDouble()
        val finalHolographicProbability = (baseHolographicProbability + holographicQualityPercent / 100.0)
            .coerceIn(0.0, 1.0)
        val selectedPool = if (rollProbability(finalHolographicProbability)) {
            holographicEntries
        } else {
            nonHolographicEntries
        }
        return variantProfile.requireSkyQualityDefinition(
            pickWeighted(selectedPool) { it.weight }.code,
        )
    }

    private fun pickFinishWithPolicy(
        variantProfile: fr.aumombelli.dstcg.model.VariantProfile,
        runtimeVariantWeights: RuntimeVariantWeights,
        allowStamped: Boolean,
    ): fr.aumombelli.dstcg.model.CardFinishDefinition {
        if (allowStamped) {
            return variantProfile.requireFinishDefinition(
                pickWeightedCode(runtimeVariantWeights.finishWeights),
            )
        }
        val finishWeightsByCode = runtimeVariantWeights.finishWeights.associateBy { it.code }
        val nonStampedEntries = variantProfile.finishes
            .filterNot { it.isStamped }
            .mapNotNull { finish -> finishWeightsByCode[finish.code] }
            .ifEmpty { runtimeVariantWeights.finishWeights }
        return variantProfile.requireFinishDefinition(
            pickWeighted(nonStampedEntries) { it.weight }.code,
        )
    }

    private fun rollPercent(percent: Double): Boolean = rollProbability(
        probability = (percent / 100.0).coerceIn(0.0, 1.0),
    )

    private fun rollProbability(probability: Double): Boolean {
        if (probability <= 0.0) return false
        if (probability >= 1.0) return true
        val scale = 1_000_000
        val threshold = (probability * scale).toInt().coerceIn(1, scale)
        return entropySource.nextInt(scale) < threshold
    }
}
