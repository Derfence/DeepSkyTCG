package fr.aumombelli.dstcg.model

internal data class NewPlayerOnboardingMessageContent(
    val title: String,
    val message: String,
)

internal enum class LibraryVariantWalkthroughVisualKind {
    Rarity,
    SkyQuality,
    Stamp,
    Holographic,
}

internal data class LibraryVariantWalkthroughPageContent(
    val title: String,
    val message: String,
    val visualKind: LibraryVariantWalkthroughVisualKind,
)

internal data class CraftingToolCostLineContent(
    val label: String,
    val cost: String,
)

internal data class CraftingToolWalkthroughPageContent(
    val title: String,
    val message: String,
    val costs: List<CraftingToolCostLineContent>,
)

internal object NewPlayerOnboardingContent {
    val welcomeIntro = NewPlayerOnboardingMessageContent(
        title = "Bienvenue dans Deep Sky TCG",
        message = "Dans un \"Trading Card Game\" ou \"Jeu de Cartes à Echanger\", tu ouvres des paquets de cartes aléatoires, découvre différentes variantes rares et échanges tes doublons pour compléter ta collection.\nMon nom est Aster, et je te guiderai pour terminer ta collection.",
    )

    val conclusion = NewPlayerOnboardingMessageContent(
        title = "Tu es prêt",
        message = "Tu sais maintenant ouvrir des packs, retrouver tes cartes, activer un équipement et améliorer une carte dans l'atelier.\nJe te laisse explorer la suite à ton rythme. Bonne collection !",
    )

    val libraryVariantWalkthroughPages = listOf(
        LibraryVariantWalkthroughPageContent(
            title = "Rareté",
            message = "Chaque carte possède un indicateur de rareté : Commune, Peu commune, Rare ou Epique. Plus la rareté monte, plus la carte est difficile à obtenir.",
            visualKind = LibraryVariantWalkthroughVisualKind.Rarity,
        ),
        LibraryVariantWalkthroughPageContent(
            title = "Qualité du ciel",
            message = "Une même carte peut être observée dans plusieurs qualités de ciel : Ville, Périurbain, Campagne ou Montagne.",
            visualKind = LibraryVariantWalkthroughVisualKind.SkyQuality,
        ),
        LibraryVariantWalkthroughPageContent(
            title = "Tampon",
            message = "Chaque cartes existent en version Tamponnée. C'est une finition spéciale qui s'ajoute à la variante de base.",
            visualKind = LibraryVariantWalkthroughVisualKind.Stamp,
        ),
        LibraryVariantWalkthroughPageContent(
            title = "Holographique",
            message = "Les variantes Holographiques sont les plus spectaculaires : elles brillent davantage et comptent parmi les plus belles pièces d'une collection mais sont également beaucoup plus rare !",
            visualKind = LibraryVariantWalkthroughVisualKind.Holographic,
        ),
    )

    val craftingToolWalkthroughPages = listOf(
        CraftingToolWalkthroughPageContent(
            title = "Assombrir le ciel",
            message = "Cet outil transforme une carte vers la qualité de ciel suivante. Il consomme des exemplaires identiques de la variante source, puis crée une carte dans la qualité suivante. Une carte tamponnée peut être utilisée mais le tampon est perdu.",
            costs = listOf(
                CraftingToolCostLineContent(
                    label = "Ville -> Périurbain",
                    cost = "${DefaultSkyUpgradeCosts.getValue("city")} exemplaires Ville",
                ),
                CraftingToolCostLineContent(
                    label = "Périurbain -> Campagne",
                    cost = "${DefaultSkyUpgradeCosts.getValue("suburban")} exemplaires Périurbain",
                ),
                CraftingToolCostLineContent(
                    label = "Campagne -> Montagne",
                    cost = "${DefaultSkyUpgradeCosts.getValue("rural")} exemplaires Campagne",
                ),
                CraftingToolCostLineContent(
                    label = "Montagne -> Holographique",
                    cost = "${DefaultSkyUpgradeCosts.getValue("mountain")} exemplaires Montagne",
                ),
            ),
        ),
        CraftingToolWalkthroughPageContent(
            title = "Agence spatiale",
            message = "Cet outil ajoute un tampon officiel à une carte standard en consommant 10 exemplaires de la même qualité de ciel. La fabrication crée une carte Tamponnée de la qualité de ciel utilisée.",
            costs = listOf(
                CraftingToolCostLineContent(
                    label = "Standard -> Tamponnée",
                    cost = "$StampedCraftingCost exemplaires standard",
                ),
            ),
        ),
    )
}
