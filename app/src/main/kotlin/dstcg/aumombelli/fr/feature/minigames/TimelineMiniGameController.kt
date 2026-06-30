package fr.aumombelli.dstcg.feature.minigames

import fr.aumombelli.dstcg.data.CatalogGateway
import fr.aumombelli.dstcg.data.MiniGameAttemptConsumeResult
import fr.aumombelli.dstcg.data.MiniGameRewardGrantResult
import fr.aumombelli.dstcg.data.MiniGamesGateway
import fr.aumombelli.dstcg.data.ProgressGateway
import fr.aumombelli.dstcg.data.requireUsableProgress
import fr.aumombelli.dstcg.model.MiniGameDifficulty
import fr.aumombelli.dstcg.model.MiniGameId
import fr.aumombelli.dstcg.model.MiniGameReward
import fr.aumombelli.dstcg.model.dailyStateFor
import fr.aumombelli.dstcg.model.normalized
import fr.aumombelli.dstcg.model.unlockedDifficultyFor
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
    private var comparisonIndex: Int = 0
    private var placements: List<String?> = emptyList()
    private var handSlots: List<String?> = emptyList()
    private var score: Int = 0
    private var corrections: List<TimelineComparisonResultUi> = emptyList()
    private var currentCorrection: TimelineComparisonResultUi? = null
    private var completionStarted: Boolean = false
    private var feedbackEvent: MiniGameFeedbackEvent? = null

    fun open() {
        launch {
            uiState.update { it.copy(isLoading = true, errorMessage = null) }
            runCatching {
                openTimelineScreen()
            }.onFailure { error ->
                clear()
                uiState.update {
                    it.copy(
                        isLoading = false,
                        screen = MiniGamesScreenUiState.TimelineUnavailable(
                            message = error.message ?: "Impossible de préparer Comparaison.",
                        ),
                    )
                }
            }
        }
    }

    fun selectDifficulty(difficulty: MiniGameDifficulty) {
        val current = uiState.value
        val choice = current.timelineDifficultyChoices.firstOrNull { it.difficulty == difficulty }
        if (current.isLoading || choice?.enabled != true) {
            return
        }

        launch {
            uiState.update { it.copy(isLoading = true, errorMessage = null) }
            runCatching {
                startTimeline(difficulty)
            }.onFailure { error ->
                clear()
                uiState.update {
                    it.copy(
                        isLoading = false,
                        screen = MiniGamesScreenUiState.TimelineUnavailable(
                            message = error.message ?: "Impossible de préparer Comparaison.",
                        ),
                    )
                }
            }
        }
    }

    fun placeCard(cardId: String, slotIndex: Int) {
        val comparison = activeComparison() ?: return
        if (completionStarted || currentCorrection != null) return
        if (slotIndex !in comparison.correctSlots.indices) return
        if (comparison.cards.none { it.id == cardId }) return

        val updatedPlacements = placements.toMutableList()
        val updatedHandSlots = normalizedHandSlots(
            comparison = comparison,
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
        val comparison = activeComparison() ?: return
        if (completionStarted || currentCorrection != null) return
        if (handSlotIndex !in comparison.cards.indices) return
        if (comparison.cards.none { it.id == cardId }) return

        val previousSlot = placements.indexOf(cardId)
        if (previousSlot < 0) return

        val updatedHandSlots = normalizedHandSlots(
            comparison = comparison,
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
        if (completionStarted || currentCorrection != null || placements.any { it == null }) return

        val evaluation = evaluateTimelineComparison(
            game = timeline,
            comparisonIndex = comparisonIndex,
            placedCardIds = placements,
        ) ?: return

        if (evaluation.isCorrect) {
            score += 1
        }
        val correction = evaluation.toUi(timeline.criterion)
        corrections += correction
        currentCorrection = correction
        feedbackEvent = feedbackEmitter.next(
            tone = if (evaluation.isCorrect) MiniGameFeedbackTone.Success else MiniGameFeedbackTone.Error,
            sourceIndexes = setOf(0, 1),
        )
        publishPlayingState()
    }

    fun continueTimeline() {
        val timeline = activeTimeline ?: return
        if (completionStarted || currentCorrection == null) return
        if (comparisonIndex < timeline.comparisons.lastIndex) {
            comparisonIndex += 1
            currentCorrection = null
            feedbackEvent = null
            resetComparisonState(timeline.comparisons[comparisonIndex])
            publishPlayingState()
            return
        }

        completeTimeline(timeline)
    }

    fun clear() {
        activeTimeline = null
        comparisonIndex = 0
        placements = emptyList()
        handSlots = emptyList()
        score = 0
        corrections = emptyList()
        currentCorrection = null
        completionStarted = false
        feedbackEvent = null
    }

    private suspend fun openTimelineScreen() {
        val miniGamesState = miniGamesRepository.loadMiniGamesState()
        val dailyState = miniGamesState.progress.dailyStateFor(
            miniGameId = MiniGameId.Timeline,
            dateUtc = miniGamesState.todayUtc,
        )
        if (dailyState.hasPlayed || dailyState.reward != null) {
            uiState.value = miniGamesState.toUiState(
                screen = alreadyPlayedTimelineScreen(dailyState.reward),
            )
            return
        }

        val criterion = selectPlayableTimelineCriterionForToday(miniGamesState.todayUtc)
        val screen = if (criterion == null) {
            MiniGamesScreenUiState.TimelineUnavailable(
                message = "Tu dois posséder au moins deux cartes comparables pour préparer Comparaison.",
            )
        } else {
            MiniGamesScreenUiState.TimelineDifficultySelection
        }
        uiState.value = miniGamesState.toUiState(screen = screen)
    }

    private suspend fun startTimeline(difficulty: MiniGameDifficulty) {
        val miniGamesState = miniGamesRepository.loadMiniGamesState()
        val dailyState = miniGamesState.progress.dailyStateFor(
            miniGameId = MiniGameId.Timeline,
            dateUtc = miniGamesState.todayUtc,
        )
        if (dailyState.hasPlayed || dailyState.reward != null) {
            uiState.value = miniGamesState.toUiState(
                screen = alreadyPlayedTimelineScreen(dailyState.reward),
            )
            return
        }
        val unlockedDifficulty = miniGamesState.progress.unlockedDifficultyFor(MiniGameId.Timeline)
        if (difficulty.level > unlockedDifficulty.level) {
            uiState.value = miniGamesState.toUiState(
                screen = MiniGamesScreenUiState.TimelineUnavailable(
                    message = "Cette difficulté n'est pas encore débloquée.",
                ),
            )
            return
        }

        val game = when (
            val timelineResult = buildTimelineForDifficulty(
                difficulty = difficulty,
                todayUtc = miniGamesState.todayUtc,
            )
        ) {
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
                comparisonIndex = 0
                score = 0
                corrections = emptyList()
                currentCorrection = null
                completionStarted = false
                feedbackEvent = null
                resetComparisonState(game.comparisons.first())
                uiState.value = consumed.miniGamesProgress.toUiState(
                    todayUtc = miniGamesState.todayUtc,
                    screen = buildPlayingState(game),
                )
            }

            is MiniGameAttemptConsumeResult.AlreadyConsumed -> {
                clear()
                uiState.value = consumed.miniGamesProgress.toUiState(
                    todayUtc = miniGamesState.todayUtc,
                    screen = alreadyPlayedTimelineScreen(consumed.dailyState.reward),
                )
            }
        }
    }

    private suspend fun buildTimelineForDifficulty(
        difficulty: MiniGameDifficulty,
        todayUtc: String,
    ): TimelineGameBuildResult {
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
            message = "Tu dois posséder au moins deux cartes comparables pour préparer Comparaison.",
        )
        val eligibleCardIds = eligibleTimelineCardIds(
            criterion = criterion,
            cards = cards,
        )
        val resolvedCards = miniGamesRepository.prepareResolvedCardsForToday(
            miniGameId = MiniGameId.Timeline,
            slotCount = timelineResolvedCardCountForDifficulty(difficulty),
            eligibleCardIds = eligibleCardIds,
            distinctOwnedCards = true,
        )
        return buildTimelineGame(
            difficulty = difficulty,
            criterion = criterion,
            dateUtc = todayUtc,
            resolvedCards = resolvedCards,
            cards = cards,
            extensions = catalogRepository.loadExtensions(),
            variantProfiles = catalogRepository.loadVariantProfiles(),
        )
    }

    private suspend fun selectPlayableTimelineCriterionForToday(todayUtc: String): TimelineCriterion? {
        val cards = catalogRepository.loadCards()
        val loadedProgress = progressRepository.loadProgress().requireUsableProgress()
        val ownedCardIds = loadedProgress.progress.collection.normalized().cards
            .filter { (_, entry) -> entry.totalOwned > 0 }
            .keys
        return selectPlayableTimelineCriterion(
            dateUtc = todayUtc,
            cards = cards,
            ownedCardIds = ownedCardIds,
        )
    }

    private fun completeTimeline(timeline: TimelineGame) {
        if (completionStarted) return
        val finalScore = score
        val finalCorrections = corrections
        completionStarted = true
        currentCorrection = null
        feedbackEvent = feedbackEmitter.next(
            tone = MiniGameFeedbackTone.Completion,
            sourceIndexes = emptySet(),
        )
        publishPlayingState()
        launch {
            runCatching {
                val reward = calculateTimelineReward(
                    difficulty = timeline.difficulty,
                    correctCount = finalScore,
                    comparisonCount = timeline.comparisons.size,
                )
                val grantResult = miniGamesRepository.grantRewardForToday(
                    miniGameId = MiniGameId.Timeline,
                    reward = reward,
                )
                val nextDifficulty = timeline.difficulty.next()
                    ?.takeIf { finalScore == timeline.comparisons.size }
                if (grantResult is MiniGameRewardGrantResult.Granted && nextDifficulty != null) {
                    miniGamesRepository.unlockDifficulty(
                        miniGameId = MiniGameId.Timeline,
                        difficulty = nextDifficulty,
                    )
                }
                val refreshed = miniGamesRepository.loadMiniGamesState()
                val resultFeedbackEvent = feedbackEmitter.next(
                    tone = MiniGameFeedbackTone.Completion,
                    sourceIndexes = emptySet(),
                )
                val resultScreen = buildResultState(
                    timeline = timeline,
                    score = finalScore,
                    reward = reward,
                    corrections = finalCorrections,
                    nextDifficultyName = nextDifficulty
                        ?.takeIf { grantResult is MiniGameRewardGrantResult.Granted }
                        ?.displayName,
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
        val comparison = timeline.comparisons[comparisonIndex]
        val handSlotIds = normalizedHandSlots(
            comparison = comparison,
            handSlots = handSlots,
            placements = placements,
        )
        val cardsById = comparison.cards.associateBy(TimelineCard::id)
        return MiniGamesScreenUiState.TimelinePlaying(
            difficultyName = timeline.difficulty.displayName,
            criterionTitle = timeline.criterion.title,
            instruction = timeline.criterion.instruction,
            rewardLabel = formatReward(timeline.difficulty.reward),
            comparisonIndex = comparisonIndex,
            comparisonCount = timeline.comparisons.size,
            score = score,
            slots = comparison.correctSlots.mapIndexed { index, _ ->
                TimelineSlotUi(
                    index = index,
                    placedCard = placements.getOrNull(index)
                        ?.let { cardId -> comparison.cards.firstOrNull { it.id == cardId } }
                        ?.toUi(),
                    emptyLabel = timeline.criterion.emptySlotLabelFor(index),
                )
            },
            handCards = handSlotIds
                .mapNotNull { cardId -> cardId?.let(cardsById::get) }
                .map(TimelineCard::toUi),
            handSlots = handSlotIds.map { cardId -> cardId?.let(cardsById::get)?.toUi() },
            canValidate = placements.size == comparison.correctSlots.size &&
                placements.all { it != null } &&
                !completionStarted &&
                currentCorrection == null,
            feedbackEvent = feedbackEvent,
            currentCorrection = currentCorrection,
        )
    }

    private fun buildResultState(
        timeline: TimelineGame,
        score: Int,
        reward: MiniGameReward,
        corrections: List<TimelineComparisonResultUi>,
        nextDifficultyName: String?,
        feedbackEvent: MiniGameFeedbackEvent?,
        alreadyPlayed: Boolean,
    ): MiniGamesScreenUiState.TimelineResult =
        MiniGamesScreenUiState.TimelineResult(
            difficultyName = if (alreadyPlayed) "Comparaison" else timeline.difficulty.displayName,
            criterionTitle = timeline.criterion.title,
            scoreLabel = if (alreadyPlayed) {
                "Déjà joué aujourd'hui"
            } else {
                "$score/${timeline.comparisons.size}"
            },
            rewardLabel = formatReward(reward),
            corrections = corrections,
            nextDifficultyName = nextDifficultyName,
            feedbackEvent = feedbackEvent,
        )

    private fun alreadyPlayedTimelineScreen(
        reward: MiniGameReward?,
    ): MiniGamesScreenUiState =
        if (reward != null) {
            MiniGamesScreenUiState.TimelineResult(
                difficultyName = "Comparaison",
                criterionTitle = "Comparaison",
                scoreLabel = "Déjà joué aujourd'hui",
                rewardLabel = formatReward(reward),
                corrections = emptyList(),
                nextDifficultyName = null,
                feedbackEvent = null,
            )
        } else {
            MiniGamesScreenUiState.TimelineUnavailable(
                message = "Ton essai Comparaison est déjà utilisé pour aujourd'hui.",
            )
        }

    private fun activeComparison(): TimelineComparison? =
        activeTimeline?.comparisons?.getOrNull(comparisonIndex)

    private fun resetComparisonState(comparison: TimelineComparison) {
        placements = List(comparison.correctSlots.size) { null }
        handSlots = comparison.cards.map(TimelineCard::id)
    }
}

private fun TimelineCriterion.emptySlotLabelFor(index: Int): String =
    when (index) {
        0 -> firstSlotLabel
        else -> lastSlotLabel
    }

private fun normalizedHandSlots(
    comparison: TimelineComparison,
    handSlots: List<String?>,
    placements: List<String?>,
): List<String?> {
    if (handSlots.size == comparison.cards.size) {
        return handSlots
    }
    val placedIds = placements.filterNotNull().toSet()
    return comparison.cards.map { card ->
        card.id.takeIf { it !in placedIds }
    }
}

private fun MutableList<String?>.removeTimelineHandCard(cardId: String) {
    val currentIndex = indexOf(cardId)
    if (currentIndex >= 0) {
        this[currentIndex] = null
    }
}

private fun TimelineComparisonEvaluation.toUi(
    criterion: TimelineCriterion,
): TimelineComparisonResultUi =
    TimelineComparisonResultUi(
        index = comparisonIndex,
        firstSlotLabel = criterion.firstSlotLabel,
        lastSlotLabel = criterion.lastSlotLabel,
        placedCards = placedCards.map(TimelineCard::toUi),
        correctCards = correctCards.map(TimelineCard::toUi),
        isCorrect = isCorrect,
    )
