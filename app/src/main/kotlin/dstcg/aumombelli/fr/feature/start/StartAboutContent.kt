package fr.aumombelli.dstcg.feature.start

internal const val StartFooterAppVersion: String = "v0.9.0"

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
        ),
    ),
)
