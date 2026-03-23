package fr.aumombelli.gatcha.ui.theme

import androidx.compose.ui.graphics.Color

fun rarityColor(rarityLabel: String): Color = when (rarityLabel) {
    "Epic" -> Color(0xFF8D5CFF)
    "Rare" -> Color(0xFF2EC4B6)
    else -> Color(0xFF587291)
}
