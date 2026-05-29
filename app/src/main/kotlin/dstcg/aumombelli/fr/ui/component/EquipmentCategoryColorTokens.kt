package fr.aumombelli.dstcg.ui.component

import androidx.compose.ui.graphics.Color
import fr.aumombelli.dstcg.model.EquipmentType

internal data class EquipmentCategoryColorTokens(
    val panelStart: Color,
    val panelEnd: Color,
    val cardStart: Color,
    val cardEnd: Color,
    val iconStart: Color,
    val iconEnd: Color,
    val accent: Color,
    val accentText: Color,
    val iconStroke: Color,
    val chipColor: Color,
)

internal fun equipmentCategoryColorTokens(type: EquipmentType): EquipmentCategoryColorTokens = when (type) {
    EquipmentType.Observatory -> EquipmentCategoryColorTokens(
        panelStart = Color(0x66204154),
        panelEnd = Color(0x44253E62),
        cardStart = Color(0xAA173043),
        cardEnd = Color(0x88111F32),
        iconStart = Color(0xAA5BE7E0),
        iconEnd = Color(0x4444AFC6),
        accent = Color(0xFF63E0D7),
        accentText = Color(0xFFABF7F0),
        iconStroke = Color(0xFFE6FFFE),
        chipColor = Color(0x263AD0C6),
    )

    EquipmentType.Telescope -> EquipmentCategoryColorTokens(
        panelStart = Color(0x66533E22),
        panelEnd = Color(0x44483826),
        cardStart = Color(0xAA3A2B16),
        cardEnd = Color(0x88261E12),
        iconStart = Color(0xAAF4D277),
        iconEnd = Color(0x44D29F42),
        accent = Color(0xFFF0CC6A),
        accentText = Color(0xFFFFE7A6),
        iconStroke = Color(0xFFFFF7E0),
        chipColor = Color(0x26F0CC6A),
    )

    EquipmentType.Mount -> EquipmentCategoryColorTokens(
        panelStart = Color(0x66553A34),
        panelEnd = Color(0x44473439),
        cardStart = Color(0xAA38211F),
        cardEnd = Color(0x8825151A),
        iconStart = Color(0xAAFF9478),
        iconEnd = Color(0x44D86474),
        accent = Color(0xFFFF9B7A),
        accentText = Color(0xFFFFD2C2),
        iconStroke = Color(0xFFFFF1EB),
        chipColor = Color(0x26FF9B7A),
    )
}
