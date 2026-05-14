package fr.aumombelli.dstcg.feature.minigames

import fr.aumombelli.dstcg.model.MiniGameDifficulty

internal data class MemoryDifficultyChoiceUi(
    val difficulty: MiniGameDifficulty,
    val title: String,
    val gridLabel: String,
    val rewardLabel: String,
    val enabled: Boolean,
    val locked: Boolean,
    val statusLabel: String,
) {
    val testTag: String = "memory-difficulty-${difficulty.name.lowercase()}"
}

internal data class MemoryCellUi(
    val index: Int,
    val face: MemoryCardFace,
    val state: MemoryCellState,
) {
    val testTag: String = "memory-cell-$index"
    val isVisible: Boolean = state != MemoryCellState.Hidden
}

internal enum class MemoryCellState {
    Hidden,
    Revealed,
    Matched,
    Mismatch,
}
