package fr.aumombelli.dstcg.feature.minigames

import fr.aumombelli.dstcg.model.DisplayCard
import fr.aumombelli.dstcg.model.MiniGameDifficulty

internal data class TimelineDifficultyChoiceUi(
    val difficulty: MiniGameDifficulty,
    val title: String,
    val comparisonLabel: String,
    val rewardLabel: String,
    val enabled: Boolean,
    val locked: Boolean,
    val statusLabel: String,
) {
    val testTag: String = "timeline-difficulty-${difficulty.name.lowercase()}"
}

internal data class TimelineCardUi(
    val id: String,
    val displayCard: DisplayCard,
    val valueLabel: String,
) {
    val testTag: String = "timeline-card-$id"
}

internal data class TimelineSlotUi(
    val index: Int,
    val placedCard: TimelineCardUi?,
    val emptyLabel: String? = null,
) {
    val testTag: String = "timeline-slot-$index"
}

internal data class TimelineSlotResultUi(
    val index: Int,
    val placedCard: TimelineCardUi,
    val correctCard: TimelineCardUi,
    val isCorrect: Boolean,
) {
    val testTag: String = "timeline-result-slot-$index"
}

internal fun TimelineCard.toUi(): TimelineCardUi =
    TimelineCardUi(
        id = id,
        displayCard = displayCard,
        valueLabel = valueLabel,
    )

internal data class TimelineComparisonResultUi(
    val index: Int,
    val firstSlotLabel: String,
    val lastSlotLabel: String,
    val placedCards: List<TimelineCardUi>,
    val correctCards: List<TimelineCardUi>,
    val isCorrect: Boolean,
) {
    val testTag: String = "timeline-result-comparison-$index"
}
