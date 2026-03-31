package fr.aumombelli.gatcha.feature.start

internal const val StartFooterAppVersion: String = "v0.9.0"

internal data class StartAboutSection(
    val title: String,
    val lines: List<String>,
)

internal val StartAboutSections: List<StartAboutSection> = listOf(
    StartAboutSection(
        title = "Application",
        lines = listOf(
            "Gatcha",
            "Client Android standalone hors ligne.",
        ),
    ),
    StartAboutSection(
        title = "Expérience",
        lines = listOf(
            "Collection locale, ouverture de packs et consultation de la bibliothèque.",
            "Animations d'introduction et navigation embarquées dans l'application.",
        ),
    ),
    StartAboutSection(
        title = "Crédits",
        lines = listOf(
            "Projet et interface : équipe Gatcha.",
            "Crédits détaillés des illustrations : visibles sur chaque carte.",
        ),
    ),
)
