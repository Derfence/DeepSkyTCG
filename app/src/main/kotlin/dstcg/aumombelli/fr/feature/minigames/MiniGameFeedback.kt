package fr.aumombelli.dstcg.feature.minigames

internal data class MiniGameFeedbackEvent(
    val id: Long,
    val tone: MiniGameFeedbackTone,
    val sourceIndexes: Set<Int> = emptySet(),
)

internal enum class MiniGameFeedbackTone {
    Success,
    Error,
    Special,
    Completion,
}
