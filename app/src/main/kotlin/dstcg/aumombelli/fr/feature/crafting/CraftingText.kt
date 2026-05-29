package fr.aumombelli.dstcg.feature.crafting

import fr.aumombelli.dstcg.model.CraftingMode
import fr.aumombelli.dstcg.model.DefaultSkyUpgradeCosts
import fr.aumombelli.dstcg.model.StampedCraftingCost

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

internal fun CraftingMode.costSummary(): String = when (this) {
    CraftingMode.DarkenSky -> "Coûts : " +
        "Ville ${DefaultSkyUpgradeCosts.getValue("city")} · " +
        "Périurbain ${DefaultSkyUpgradeCosts.getValue("suburban")} · " +
        "Campagne ${DefaultSkyUpgradeCosts.getValue("rural")} · " +
        "Montagne ${DefaultSkyUpgradeCosts.getValue("mountain")}"

    CraftingMode.SpaceAgency -> "Coût : $StampedCraftingCost standards identiques"
}
