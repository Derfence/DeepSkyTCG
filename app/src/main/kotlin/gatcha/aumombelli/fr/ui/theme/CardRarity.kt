package fr.aumombelli.gatcha.ui.theme

import androidx.compose.ui.graphics.Color

data class RarityBadgeStyle(
    val branchCount: Int,
    val color: Color,
    val glowColor: Color,
)

data class SkyQualityPalette(
    val top: Color,
    val bottom: Color,
    val glow: Color,
    val mist: Color,
)

fun rarityBadgeStyle(rarityLabel: String): RarityBadgeStyle = when (rarityLabel) {
    "Common" -> RarityBadgeStyle(
        branchCount = 4,
        color = Color(0xFFF6FBFF),
        glowColor = Color(0x66FFFFFF),
    )
    "Uncommon" -> RarityBadgeStyle(
        branchCount = 4,
        color = Color(0xFF6CCBFF),
        glowColor = Color(0x664AA8FF),
    )
    "Rare" -> RarityBadgeStyle(
        branchCount = 4,
        color = Color(0xFFFFD76A),
        glowColor = Color(0x66FFB400),
    )
    "Epic" -> RarityBadgeStyle(
        branchCount = 6,
        color = Color(0xFFFFE8A8),
        glowColor = Color(0x88FFBB33),
    )
    else -> RarityBadgeStyle(
        branchCount = 4,
        color = Color(0xFFD4DFEB),
        glowColor = Color(0x554D6A88),
    )
}

fun skyQualityPalette(skyQuality: String): SkyQualityPalette = when (skyQuality) {
    "city" -> SkyQualityPalette(
        top = Color(0xFFD9A441),
        bottom = Color(0xFF564122),
        glow = Color(0x99FFE082),
        mist = Color(0x66FFF4C2),
    )
    "suburban" -> SkyQualityPalette(
        top = Color(0xFFB66A3D),
        bottom = Color(0xFF2B2131),
        glow = Color(0x88FFB36B),
        mist = Color(0x55FFD2A5),
    )
    "rural" -> SkyQualityPalette(
        top = Color(0xFF123660),
        bottom = Color(0xFF08182C),
        glow = Color(0x884EA1FF),
        mist = Color(0x444AA3FF),
    )
    "mountain" -> SkyQualityPalette(
        top = Color(0xFF061323),
        bottom = Color(0xFF010308),
        glow = Color(0x886CA7FF),
        mist = Color(0x338AAFFF),
    )
    else -> SkyQualityPalette(
        top = Color(0xFF24364F),
        bottom = Color(0xFF0A1018),
        glow = Color(0x664A6B8F),
        mist = Color(0x333E5E84),
    )
}

fun rarityColor(rarityLabel: String): Color = rarityBadgeStyle(rarityLabel).color
