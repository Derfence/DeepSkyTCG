package fr.aumombelli.dstcg.feature.start

internal const val StartFooterAppVersion: String = "v1.0"

internal data class StartAboutSection(
    val title: String,
    val lines: List<String>,
)

internal val StartAboutSections: List<StartAboutSection> = listOf(
    StartAboutSection(
        title = "Crédits",
        lines = listOf(
            "Idée originale, conception, architecture, direction artistique : Aurélien Mombelli",
            "Production technique : Codex et GPT-5.4",
            "Crédits détaillés des illustrations : visibles sur chaque carte.",
            "",
            "Cette application n'est pas, et ne sera jamais, payante ou contenant des publicité.",
            "C'est une application que j'ai réalisé pour m'amuser et que je partage avec plaisir.",
            "Licence : CC BY-SA-NC-ND",
        ),
    ),
)
