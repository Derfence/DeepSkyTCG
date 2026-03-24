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
        color = Color(0xFFC69BFF),
        glowColor = Color(0x887A3DFF),
    )
    else -> RarityBadgeStyle(
        branchCount = 4,
        color = Color(0xFFD4DFEB),
        glowColor = Color(0x554D6A88),
    )
}

fun skyQualityPalette(skyQuality: String): SkyQualityPalette = when (skyQuality) {
    "city" -> SkyQualityPalette(
        top = Color(0xFF8E845F),
        bottom = Color(0xFF3E382C),
        glow = Color(0x668B7B4A),
        mist = Color(0x334E4A3D),
    )
    "suburban" -> SkyQualityPalette(
        top = Color(0xFF5F4A46),
        bottom = Color(0xFF211C24),
        glow = Color(0x665C4A54),
        mist = Color(0x33453B48),
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
