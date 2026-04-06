package fr.aumombelli.dstcg.feature.packs.opening

import fr.aumombelli.dstcg.model.DisplayCard
import fr.aumombelli.dstcg.model.EquipmentCardDefinition

sealed interface PackRevealUiItem {
    val id: String
}

data class AstroPackRevealUiItem(
    val displayCard: DisplayCard,
) : PackRevealUiItem {
    override val id: String = displayCard.definition.id
}

data class EquipmentPackRevealUiItem(
    val definition: EquipmentCardDefinition,
) : PackRevealUiItem {
    override val id: String = definition.id
}
