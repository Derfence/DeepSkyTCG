package fr.aumombelli.dstcg.data

import fr.aumombelli.dstcg.model.ActiveEquipmentEffect
import fr.aumombelli.dstcg.model.EquipmentBonusUnit
import fr.aumombelli.dstcg.model.EquipmentCardDefinition
import fr.aumombelli.dstcg.model.EquipmentType
import fr.aumombelli.dstcg.model.raritySortPriority

internal data class ActiveEquipmentBonus(
    val rarityBoostPercent: Double = 0.0,
    val holographicQualityPercent: Double = 0.0,
    val rechargeMultiplier: Double = 1.0,
)

internal fun resolveActiveEquipmentBonus(
    activeEquipmentByType: Map<EquipmentType, ActiveEquipmentEffect>,
    equipmentCards: List<EquipmentCardDefinition>,
): ActiveEquipmentBonus {
    val cardsById = equipmentCards.associateBy { it.id }
    var rarityBoostPercent = 0.0
    var holographicQualityPercent = 0.0
    var rechargeMultiplier = 1.0

    activeEquipmentByType.forEach { (type, effect) ->
        val definition = cardsById[effect.equipmentCardId] ?: return@forEach
        if (definition.type != type || definition.type != effect.equipmentType) {
            return@forEach
        }
        when (definition.bonusUnit) {
            EquipmentBonusUnit.RarityBoost -> {
                if (type == EquipmentType.Mount) {
                    rarityBoostPercent += definition.bonusValue
                }
            }

            EquipmentBonusUnit.HolographicQualityPercent -> {
                if (type == EquipmentType.Telescope) {
                    holographicQualityPercent += definition.bonusValue
                }
            }

            EquipmentBonusUnit.RechargeMultiplier -> {
                if (type == EquipmentType.Observatory) {
                    rechargeMultiplier *= definition.bonusValue
                }
            }
        }
    }

    return ActiveEquipmentBonus(
        rarityBoostPercent = rarityBoostPercent.coerceAtLeast(0.0),
        holographicQualityPercent = holographicQualityPercent.coerceAtLeast(0.0),
        rechargeMultiplier = rechargeMultiplier.coerceAtLeast(1.0),
    )
}

internal fun nextHigherConfiguredRarity(
    currentRarity: String,
    availableRarities: Set<String>,
): String {
    val orderedRarities = listOf("Common", "Uncommon", "Rare", "Epic")
    val currentIndex = orderedRarities.indexOf(currentRarity)
    if (currentIndex < 0) return currentRarity
    for (index in (currentIndex + 1) until orderedRarities.size) {
        val candidate = orderedRarities[index]
        if (availableRarities.contains(candidate)) {
            return candidate
        }
    }
    return currentRarity
}

internal fun highestConfiguredRarity(availableRarities: Set<String>): String? =
    availableRarities.maxByOrNull(::raritySortPriority)
