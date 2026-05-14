package fr.aumombelli.dstcg.feature.minigames

import fr.aumombelli.dstcg.data.CatalogGateway
import fr.aumombelli.dstcg.data.MiniGameAttemptConsumeResult
import fr.aumombelli.dstcg.data.MiniGamesGateway
import fr.aumombelli.dstcg.data.ProgressGateway
import fr.aumombelli.dstcg.data.requireUsableProgress
import fr.aumombelli.dstcg.model.MiniGameId
import fr.aumombelli.dstcg.model.MiniGameReward
import fr.aumombelli.dstcg.model.dailyStateFor
import fr.aumombelli.dstcg.model.normalized
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

internal class TimelineMiniGameController(
    private val miniGamesRepository: MiniGamesGateway,
    private val catalogRepository: CatalogGateway,
    private val progressRepository: ProgressGateway,
    private val uiState: MutableStateFlow<MiniGamesUiState>,
    private val feedbackEmitter: MiniGameFeedbackEmitter,
    private val launch: (suspend () -> Unit) -> Unit,
) {
    private var activeTimeline: TimelineGame? = null
    private var placements: List<String?> = emptyList()
    private var handSlots: List<String?> = emptyList()
    private var completionStarted: Boolean = false
    private var feedbackEvent: MiniGameFeedbackEvent? = null

    fun open() {
        launch {
            uiState.update { it.copy(isLoading = true, errorMessage = null) }
            runCatching {
                startTimeline()
            }.onFailure { error ->
                clear()
                uiState.update {
                    it.copy(
                        isLoading = false,
                        screen = MiniGamesScreenUiState.TimelineUnavailable(
                            message = error.message ?: "Impossible de préparer la Timeline.",
                        ),
                    )
                }
            }
        }
    }

    fun placeCard(cardId: String, slotIndex: Int) {
        val timeline = activeTimeline ?: return
        if (completionStarted) return
        if (slotIndex !in timeline.correctOrder.indices) return
        if (timeline.cards.none { it.id == cardId }) return

        val updatedPlacements = placements.toMutableList()
        val updatedHandSlots = normalizedHandSlots(
            timeline = timeline,
            handSlots = handSlots,
            placements = placements,
        ).toMutableList()
        val sourceSlotIndex = updatedPlacements.indexOf(cardId)
        if (sourceSlotIndex == slotIndex) return
        val sourceHandSlotIndex = updatedHandSlots.indexOf(cardId)
        if (sourceSlotIndex < 0 && sourceHandSlotIndex < 0) return

        val displacedCardId = updatedPlacements[slotIndex]
        if (sourceSlotIndex >= 0) {
            updatedPlacements[sourceSlotIndex] = displacedCardId
        } else if (sourceHandSlotIndex >= 0) {
            updatedHandSlots[sourceHandSlotIndex] = displacedCardId
        }
        updatedHandSlots.removeTimelineHandCard(cardId)
        updatedPlacements[slotIndex] = cardId
        placements = updatedPlacements
        handSlots = updatedHandSlots
        feedbackEvent = null
        publishPlayingState()
    }

    fun returnCardToHand(cardId: String, handSlotIndex: Int) {
        val timeline = activeTimeline ?: return
        if (completionStarted) return
        if (handSlotIndex !in timeline.cards.indices) return
        if (timeline.cards.none { it.id == cardId }) return

        val previousSlot = placements.indexOf(cardId)
        if (previousSlot < 0) return

        val updatedHandSlots = normalizedHandSlots(
            timeline = timeline,
            handSlots = handSlots,
            placements = placements,
        ).toMutableList()
        if (updatedHandSlots[handSlotIndex] != null) return

        val updatedPlacements = placements.toMutableList()
        updatedPlacements[previousSlot] = null
        updatedHandSlots.removeTimelineHandCard(cardId)
        updatedHandSlots[handSlotIndex] = cardId

        placements = updatedPlacements
        handSlots = updatedHandSlots
        feedbackEvent = null
        publishPlayingState()
    }

    fun validate() {
        val timeline = activeTimeline ?: return
        if (completionStarted || placements.any { it == null }) return
        completionStarted = true
        feedbackEvent = feedbackEmitter.next(
            tone = MiniGameFeedbackTone.Completion,
            sourceIndexes = emptySet(),
        )
        publishPlayingState()

        val completedPlacements = placements
        launch {
            runCatching {
                val evaluation = evaluateTimelinePlacement(
                    game = timeline,
                    placedCardIds = completedPlacements,
                )
                val reward = calculateTimelineReward(
                    correctCount = evaluation.correctCount,
                    totalCount = evaluation.totalCount,
                )
                miniGamesRepository.grantRewardForToday(
                    miniGameId = MiniGameId.Timeline,
                    reward = reward,
                )
                val refreshed = miniGamesRepository.loadMiniGamesState()
                val resultFeedbackEvent = feedbackEmitter.next(
                    tone = MiniGameFeedbackTone.Completion,
                    sourceIndexes = emptySet(),
                )
                val resultScreen = buildResultState(
                    timeline = timeline,
                    evaluation = evaluation,
                    reward = reward,
                    feedbackEvent = resultFeedbackEvent,
                    alreadyPlayed = false,
                )
                clear()
                refreshed.toUiState(screen = resultScreen)
            }.onSuccess { updatedState ->
                uiState.value = updatedState
            }.onFailure { error ->
                clear()
                uiState.update {
                    it.copy(
                        isLoading = false,
                        screen = MiniGamesScreenUiState.TimelineUnavailable(
                            message = error.message ?: "Impossible d'attribuer la récompense.",
                        ),
                    )
                }
            }
        }
    }

    fun clear() {
        activeTimeline = null
        placements = emptyList()
        handSlots = emptyList()
        completionStarted = false
        feedbackEvent = null
    }

    private suspend fun startTimeline() {
        val miniGamesState = miniGamesRepository.loadMiniGamesState()
        val dailyState = miniGamesState.progress.dailyStateFor(
            miniGameId = MiniGameId.Timeline,
            dateUtc = miniGamesState.todayUtc,
        )
        if (dailyState.hasPlayed) {
            if (dailyState.reward != null) {
                val timeline = (buildTimelineForToday(miniGamesState.todayUtc) as? TimelineGameBuildResult.Ready)
                    ?.game
                uiState.value = miniGamesState.toUiState(
                    screen = alreadyPlayedTimelineScreen(
                        reward = dailyState.reward,
                        timeline = timeline,
                    ),
                )
                return
            }
            uiState.value = miniGamesState.toUiState(
                screen = MiniGamesScreenUiState.TimelineUnavailable(
                    message = "Ton essai Timeline est déjà utilisé pour aujourd'hui.",
                ),
            )
            return
        }

        val timelineResult = buildTimelineForToday(miniGamesState.todayUtc)
        val game = when (timelineResult) {
            is TimelineGameBuildResult.Ready -> timelineResult.game
            is TimelineGameBuildResult.Unavailable -> {
                uiState.value = miniGamesState.toUiState(
                    screen = MiniGamesScreenUiState.TimelineUnavailable(timelineResult.message),
                )
                return
            }
        }

        when (val consumed = miniGamesRepository.consumeAttemptForToday(MiniGameId.Timeline)) {
            is MiniGameAttemptConsumeResult.Consumed -> {
                activeTimeline = game
                placements = List(game.cards.size) { null }
                handSlots = game.cards.map(TimelineCard::id)
                completionStarted = false
                feedbackEvent = null
                uiState.value = consumed.miniGamesProgress.toUiState(
                    todayUtc = miniGamesState.todayUtc,
                    screen = buildPlayingState(game),
                )
            }

            is MiniGameAttemptConsumeResult.AlreadyConsumed -> {
                clear()
                uiState.value = consumed.miniGamesProgress.toUiState(
                    todayUtc = miniGamesState.todayUtc,
                    screen = alreadyPlayedTimelineScreen(
                        reward = consumed.dailyState.reward,
                        timeline = game,
                    ),
                )
            }
        }
    }

    private suspend fun buildTimelineForToday(todayUtc: String): TimelineGameBuildResult {
        val cards = catalogRepository.loadCards()
        val loadedProgress = progressRepository.loadProgress().requireUsableProgress()
        val ownedCardIds = loadedProgress.progress.collection.normalized().cards
            .filter { (_, entry) -> entry.totalOwned > 0 }
            .keys
        val criterion = selectPlayableTimelineCriterion(
            dateUtc = todayUtc,
            cards = cards,
            ownedCardIds = ownedCardIds,
        ) ?: return TimelineGameBuildResult.Unavailable(
            message = "Tu dois posséder au moins deux cartes pour préparer la Timeline.",
        )
        val eligibleCardIds = eligibleTimelineCardIds(
            criterion = criterion,
            cards = cards,
        )
        val resolvedCards = miniGamesRepository.prepareResolvedCardsForToday(
            miniGameId = MiniGameId.Timeline,
            slotCount = TimelinePreferredCardCount,
            eligibleCardIds = eligibleCardIds,
            distinctOwnedCards = true,
        )
        return buildTimelineGame(
            criterion = criterion,
            dateUtc = todayUtc,
            resolvedCards = resolvedCards,
            cards = cards,
            extensions = catalogRepository.loadExtensions(),
            variantProfiles = catalogRepository.loadVariantProfiles(),
        )
    }

    private fun publishPlayingState() {
        val timeline = activeTimeline ?: return
        uiState.update {
            it.copy(
                isLoading = false,
                screen = buildPlayingState(timeline),
            )
        }
    }

    private fun buildPlayingState(timeline: TimelineGame): MiniGamesScreenUiState.TimelinePlaying {
        val handSlotIds = normalizedHandSlots(
            timeline = timeline,
            handSlots = handSlots,
            placements = placements,
        )
        val cardsById = timeline.cards.associateBy(TimelineCard::id)
        return MiniGamesScreenUiState.TimelinePlaying(
            criterionTitle = timeline.criterion.title,
            instruction = timeline.criterion.instruction,
            rewardLabel = formatReward(MiniGameReward.fromMinutes(60L)),
            slots = timeline.correctOrder.mapIndexed { index, _ ->
                TimelineSlotUi(
                    index = index,
                    placedCard = placements.getOrNull(index)
                        ?.let { cardId -> timeline.cards.firstOrNull { it.id == cardId } }
                        ?.toUi(),
                    emptyLabel = timeline.criterion.emptySlotLabelFor(
                        index = index,
                        slotCount = timeline.correctOrder.size,
                    ),
                )
            },
            handCards = handSlotIds
                .mapNotNull { cardId -> cardId?.let(cardsById::get) }
                .map(TimelineCard::toUi),
            handSlots = handSlotIds.map { cardId -> cardId?.let(cardsById::get)?.toUi() },
            canValidate = placements.size == timeline.cards.size &&
                placements.all { it != null } &&
                !completionStarted,
            feedbackEvent = feedbackEvent,
        )
    }

    private fun buildResultState(
        timeline: TimelineGame,
        evaluation: TimelineEvaluation,
        reward: MiniGameReward,
        feedbackEvent: MiniGameFeedbackEvent?,
        alreadyPlayed: Boolean,
    ): MiniGamesScreenUiState.TimelineResult =
        MiniGamesScreenUiState.TimelineResult(
            criterionTitle = timeline.criterion.title,
            scoreLabel = if (alreadyPlayed) {
                "Déjà joué aujourd'hui"
            } else {
                "${evaluation.correctCount}/${evaluation.totalCount}"
            },
            rewardLabel = formatReward(reward),
            slotResults = evaluation.slotResults.map { result ->
                TimelineSlotResultUi(
                    index = result.slotIndex,
                    placedCard = result.placedCard.toUi(),
                    correctCard = result.correctCard.toUi(),
                    isCorrect = result.isCorrect,
                )
            },
            correctOrder = timeline.correctOrder.map(TimelineCard::toUi),
            showCorrectOrder = !alreadyPlayed && !evaluation.isPerfect,
            feedbackEvent = feedbackEvent,
        )

    private fun alreadyPlayedTimelineScreen(
        reward: MiniGameReward?,
        timeline: TimelineGame?,
    ): MiniGamesScreenUiState =
        when {
            reward != null && timeline != null -> {
                val evaluation = TimelineEvaluation(
                    correctCount = timeline.correctOrder.size,
                    totalCount = timeline.correctOrder.size,
                    slotResults = timeline.correctOrder.mapIndexed { index, card ->
                        TimelineSlotResult(
                            slotIndex = index,
                            placedCard = card,
                            correctCard = card,
                            isCorrect = true,
                        )
                    },
                )
                buildResultState(
                    timeline = timeline,
                    evaluation = evaluation,
                    reward = reward,
                    feedbackEvent = null,
                    alreadyPlayed = true,
                )
            }

            reward != null -> MiniGamesScreenUiState.TimelineResult(
                criterionTitle = "Timeline",
                scoreLabel = "Déjà joué aujourd'hui",
                rewardLabel = formatReward(reward),
                slotResults = emptyList(),
                correctOrder = emptyList(),
                showCorrectOrder = false,
                feedbackEvent = null,
            )

            else -> MiniGamesScreenUiState.TimelineUnavailable(
                message = "Ton essai Timeline est déjà utilisé pour aujourd'hui.",
            )
        }
}

private fun TimelineCriterion.emptySlotLabelFor(
    index: Int,
    slotCount: Int,
): String? = when (index) {
    0 -> firstSlotLabel
    slotCount - 1 -> lastSlotLabel
    else -> null
}

private fun normalizedHandSlots(
    timeline: TimelineGame,
    handSlots: List<String?>,
    placements: List<String?>,
): List<String?> {
    if (handSlots.size == timeline.cards.size) {
        return handSlots
    }
    val placedIds = placements.filterNotNull().toSet()
    return timeline.cards.map { card ->
        card.id.takeIf { it !in placedIds }
    }
}

private fun MutableList<String?>.removeTimelineHandCard(cardId: String) {
    val currentIndex = indexOf(cardId)
    if (currentIndex >= 0) {
        this[currentIndex] = null
    }
}
