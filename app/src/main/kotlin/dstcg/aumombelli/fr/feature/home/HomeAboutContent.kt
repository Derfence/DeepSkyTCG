package fr.aumombelli.dstcg.feature.home

internal const val HomeAboutAppVersion: String = "v1.3.0"

internal data class HomeAboutSection(
    val title: String,
    val lines: List<String>,
)

internal val HomeAboutSections: List<HomeAboutSection> = listOf(
    HomeAboutSection(
        title = "Crédits",
        lines = listOf(
            "Idée originale, conception, architecture, direction artistique : Aurélien Mombelli",
            "Production technique : Codex et GPT-5.4",
            "Crédits détaillés des illustrations : visibles sur chaque carte.",
            "",
            "Cette application n'est pas, et ne sera jamais, payante ni financée par de la publicité.",
            "C'est une application que j'ai réalisée pour m'amuser et que je partage avec plaisir.",
            "Licence : CC BY-SA-NC-ND",
        ),
    ),
)
