package fr.aumombelli.dstcg.feature.minigames

internal class MiniGameFeedbackEmitter {
    private var sequence: Long = 0L

    fun next(
        tone: MiniGameFeedbackTone,
        sourceIndexes: Set<Int>,
    ): MiniGameFeedbackEvent {
        sequence += 1L
        return MiniGameFeedbackEvent(
            id = sequence,
            tone = tone,
            sourceIndexes = sourceIndexes,
        )
    }
}
