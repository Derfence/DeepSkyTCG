package fr.aumombelli.dstcg.feature.crafting

import fr.aumombelli.dstcg.model.CraftingMode

internal fun CraftingMode?.title(): String = when (this) {
    CraftingMode.DarkenSky -> "Assombrir le ciel"
    CraftingMode.SpaceAgency -> "Agence spatiale"
    null -> "Artisanat"
}

internal fun CraftingMode?.subtitle(): String = when (this) {
    CraftingMode.DarkenSky -> "Assombrir le ciel d'une carte grâce à ses doublons."
    CraftingMode.SpaceAgency -> "Faîtes tamponner une carte une fois que vous l'aurez suffisamment observée."
    null -> ""
}

internal fun CraftingMode.confirmLabel(): String = when (this) {
    CraftingMode.DarkenSky -> "Assombrir"
    CraftingMode.SpaceAgency -> "Tamponner"
}
