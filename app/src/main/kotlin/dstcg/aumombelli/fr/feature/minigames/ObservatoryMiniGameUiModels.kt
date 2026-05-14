package fr.aumombelli.dstcg.feature.minigames

import fr.aumombelli.dstcg.model.MiniGameDifficulty

internal data class ObservatoryDifficultyChoiceUi(
    val difficulty: MiniGameDifficulty,
    val title: String,
    val targetLabel: String,
    val precisionLabel: String,
    val rewardLabel: String,
    val enabled: Boolean,
    val locked: Boolean,
    val statusLabel: String,
) {
    val testTag: String = "observatory-difficulty-${difficulty.name.lowercase()}"
}

internal enum class ObservatoryStep {
    OpenDome,
    Align,
    ClearCloud,
    Focus,
    Capture,
}
