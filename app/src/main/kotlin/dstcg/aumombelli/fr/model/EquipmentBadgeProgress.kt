package fr.aumombelli.dstcg.model

import kotlinx.serialization.Serializable

@Serializable
data class EquipmentBadgeProgress(
    val maxSimultaneouslyActiveEquipmentTypeCount: Int = 0,
    val maxSimultaneouslyActiveLevelThreeEquipmentTypeCount: Int = 0,
    val affectedPackCount: Int = 0,
)

fun EquipmentBadgeProgress.normalized(): EquipmentBadgeProgress = copy(
    maxSimultaneouslyActiveEquipmentTypeCount = maxSimultaneouslyActiveEquipmentTypeCount
        .coerceIn(0, EquipmentType.entries.size),
    maxSimultaneouslyActiveLevelThreeEquipmentTypeCount = maxSimultaneouslyActiveLevelThreeEquipmentTypeCount
        .coerceIn(0, EquipmentType.entries.size),
    affectedPackCount = affectedPackCount.coerceAtLeast(0),
)
