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

internal object NewPlayerOnboardingContent {
    val welcomeIntro = NewPlayerOnboardingMessageContent(
        title = "Bienvenue dans Deep Sky TCG",
        message = "Dans un \"Trading Card Game\" ou \"Jeu de Cartes à Echanger\", tu ouvres des paquets de cartes aléatoires, découvre différentes variantes rares et échanges tes doublons pour compléter ta collection.\nBonne chance pour réunir les plus beaux objets célestes !",
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
}
