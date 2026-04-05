package fr.aumombelli.dstcg.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class EquipmentType {
    @SerialName("observatory")
    Observatory,

    @SerialName("telescope")
    Telescope,

    @SerialName("mount")
    Mount,
    ;

    val code: String
        get() = when (this) {
            Observatory -> "observatory"
            Telescope -> "telescope"
            Mount -> "mount"
        }

    val displayName: String
        get() = when (this) {
            Observatory -> "Observatoire"
            Telescope -> "Telescope"
            Mount -> "Monture"
        }

    companion object {
        fun fromCode(code: String): EquipmentType? = entries.firstOrNull { it.code == code }
    }
}

@Serializable
enum class EquipmentBonusUnit {
    @SerialName("rarityBoost")
    RarityBoost,

    @SerialName("holographicPercent")
    HolographicPercent,

    @SerialName("rechargeMultiplier")
    RechargeMultiplier,
    ;

    val code: String
        get() = when (this) {
            RarityBoost -> "rarityBoost"
            HolographicPercent -> "holographicPercent"
            RechargeMultiplier -> "rechargeMultiplier"
        }

    companion object {
        fun fromCode(code: String): EquipmentBonusUnit? = entries.firstOrNull { it.code == code }
    }
}

@Serializable
data class EquipmentCardDefinition(
    val id: String,
    val type: EquipmentType,
    val displayName: String,
    val level: Int,
    val imageRef: String,
    val packsAffected: Int,
    val bonusValue: Double,
    val bonusUnit: EquipmentBonusUnit,
    val dropWeight: Int,
    val description: String,
)

@Serializable
data class EquipmentSettingsDefinition(
    val commonReplacementChancePercent: Double,
)

@Serializable
data class OwnedEquipmentCardEntry(
    val countOwned: Int = 0,
    val activationCount: Int = 0,
)

@Serializable
data class OwnedEquipmentInventory(
    val cards: Map<String, OwnedEquipmentCardEntry> = emptyMap(),
)

@Serializable
data class ActiveEquipmentEffect(
    val equipmentCardId: String,
    val equipmentType: EquipmentType,
    val packsRemaining: Int,
)
