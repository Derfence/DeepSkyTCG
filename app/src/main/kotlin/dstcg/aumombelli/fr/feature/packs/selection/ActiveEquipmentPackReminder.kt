package fr.aumombelli.dstcg.feature.packs.selection

import fr.aumombelli.dstcg.model.EquipmentCardDefinition
import fr.aumombelli.dstcg.model.EquipmentType
import fr.aumombelli.dstcg.model.StandaloneProgress

data class ActiveEquipmentPackReminderUi(
    val type: EquipmentType,
    val level: Int,
    val packsRemaining: Int,
)

internal fun buildActiveEquipmentPackReminders(
    progress: StandaloneProgress,
    equipmentCards: List<EquipmentCardDefinition>,
): List<ActiveEquipmentPackReminderUi> {
    val equipmentCardsById = equipmentCards.associateBy(EquipmentCardDefinition::id)
    return EquipmentType.entries.mapNotNull { type ->
        val effect = progress.activeEquipmentByType[type] ?: return@mapNotNull null
        val definition = equipmentCardsById[effect.equipmentCardId] ?: return@mapNotNull null
        val packsRemaining = effect.packsRemaining.coerceAtLeast(0)
        if (packsRemaining == 0) return@mapNotNull null

        ActiveEquipmentPackReminderUi(
            type = type,
            level = definition.level.coerceAtLeast(1),
            packsRemaining = packsRemaining,
        )
    }
}
