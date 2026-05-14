package fr.aumombelli.dstcg.feature.minigames

import fr.aumombelli.dstcg.model.DisplayCard

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
