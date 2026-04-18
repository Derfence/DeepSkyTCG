package fr.aumombelli.dstcg.model

fun EquipmentCardDefinition.bonusLabel(): String = when (bonusUnit) {
    EquipmentBonusUnit.RarityBoost -> "+${formatEquipmentNumber(bonusValue)}% de promotion de rarete"
    EquipmentBonusUnit.HolographicQualityPercent -> "+${formatEquipmentNumber(bonusValue)}% de qualite holo"
    EquipmentBonusUnit.RechargeMultiplier -> "Recharge x${formatEquipmentNumber(bonusValue)}"
}

fun formatEquipmentNumber(value: Double): String =
    if (value == value.toInt().toDouble()) {
        value.toInt().toString()
    } else {
        value.toString()
    }
