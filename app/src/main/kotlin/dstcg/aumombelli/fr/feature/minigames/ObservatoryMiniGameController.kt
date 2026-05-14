package fr.aumombelli.dstcg.feature.minigames

import fr.aumombelli.dstcg.data.CatalogGateway
import fr.aumombelli.dstcg.data.MiniGameAttemptConsumeResult
import fr.aumombelli.dstcg.data.MiniGameRewardGrantResult
import fr.aumombelli.dstcg.data.MiniGamesGateway
import fr.aumombelli.dstcg.model.MiniGameDifficulty
import fr.aumombelli.dstcg.model.MiniGameId
import fr.aumombelli.dstcg.model.MiniGameReward
import fr.aumombelli.dstcg.model.dailyStateFor
import fr.aumombelli.dstcg.model.unlockedDifficultyFor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

internal class ObservatoryMiniGameController(
    private val miniGamesRepository: MiniGamesGateway,
    private val catalogRepository: CatalogGateway,
    private val uiState: MutableStateFlow<MiniGamesUiState>,
    private val feedbackEmitter: MiniGameFeedbackEmitter,
    private val launch: (suspend () -> Unit) -> Unit,
) {
    private var activeGame: ObservatoryGame? = null
    private var targetIndex: Int = 0
    private var step: ObservatoryStep = ObservatoryStep.OpenDome
    private var domeProgress: Float = 0f
    private var azimuth: Float = 0f
    private var altitude: Float = 0f
    private var focus: Float = 0f
    private var completionStarted: Boolean = false
    private var feedbackEvent: MiniGameFeedbackEvent? = null

    fun open() {
        val state = uiState.value
        if (state.isLoading) return
        val rewardLabel = state.observatoryRewardLabel
        val screen = when {
            rewardLabel != null -> MiniGamesScreenUiState.ObservatoryResult(
                difficultyName = observatoryDifficultyNameForReward(rewardLabel),
                rewardLabel = rewardLabel,
                targetCount = 0,
                nextDifficultyName = null,
                feedbackEvent = null,
            )

            state.observatoryPlayedToday -> MiniGamesScreenUiState.ObservatoryUnavailable(
                message = "Ton essai Observatoire est déjà utilisé pour aujourd'hui.",
            )

            else -> MiniGamesScreenUiState.ObservatoryDifficultySelection
        }
        uiState.update { it.copy(screen = screen, errorMessage = null) }
    }

    fun selectDifficulty(difficulty: MiniGameDifficulty) {
        val current = uiState.value
        val choice = current.observatoryDifficultyChoices.firstOrNull { it.difficulty == difficulty }
        if (current.isLoading || choice?.enabled != true) {
            return
        }

        launch {
            uiState.update { it.copy(isLoading = true, errorMessage = null) }
            runCatching {
                startObservatory(difficulty)
            }.onFailure { error ->
                clear()
                uiState.update {
                    it.copy(
                        isLoading = false,
                        screen = MiniGamesScreenUiState.ObservatoryUnavailable(
                            message = error.message ?: "Impossible de préparer l'Observatoire.",
                        ),
                    )
                }
            }
        }
    }

    fun setDomeProgress(progress: Float) {
        val game = activeGame ?: return
        if (completionStarted || step != ObservatoryStep.OpenDome) return
        domeProgress = progress.coerceIn(0f, 1f)
        if (domeProgress >= ObservatoryDomeReadyThreshold) {
            feedbackEvent = feedbackEmitter.next(
                tone = MiniGameFeedbackTone.Success,
                sourceIndexes = setOf(targetIndex),
            )
            step = ObservatoryStep.Align
        } else {
            feedbackEvent = null
        }
        publishPlayingState(game)
    }

    fun setAzimuth(value: Float) {
        val game = activeGame ?: return
        if (completionStarted || step != ObservatoryStep.Align) return
        azimuth = value.coerceIn(0f, 1f)
        advanceFromAlignmentIfReady(game)
        publishPlayingState(game)
    }

    fun setAltitude(value: Float) {
        val game = activeGame ?: return
        if (completionStarted || step != ObservatoryStep.Align) return
        altitude = value.coerceIn(0f, 1f)
        advanceFromAlignmentIfReady(game)
        publishPlayingState(game)
    }

    fun clearCloud() {
        val game = activeGame ?: return
        if (completionStarted || step != ObservatoryStep.ClearCloud) return
        feedbackEvent = feedbackEmitter.next(
            tone = MiniGameFeedbackTone.Special,
            sourceIndexes = setOf(targetIndex),
        )
        step = ObservatoryStep.Focus
        publishPlayingState(game)
    }

    fun setFocus(value: Float) {
        val game = activeGame ?: return
        if (completionStarted || step != ObservatoryStep.Focus) return
        focus = value.coerceIn(0f, 1f)
        val target = game.targets[targetIndex]
        if (isObservatorySettingReady(focus, target.focus, game.tolerance)) {
            feedbackEvent = feedbackEmitter.next(
                tone = MiniGameFeedbackTone.Success,
                sourceIndexes = setOf(targetIndex),
            )
            step = ObservatoryStep.Capture
        } else {
            feedbackEvent = null
        }
        publishPlayingState(game)
    }

    fun captureTarget() {
        val game = activeGame ?: return
        if (completionStarted || step != ObservatoryStep.Capture) return
        if (targetIndex < game.targets.lastIndex) {
            feedbackEvent = feedbackEmitter.next(
                tone = MiniGameFeedbackTone.Success,
                sourceIndexes = setOf(targetIndex),
            )
            targetIndex += 1
            resetTargetInputs()
            publishPlayingState(game)
            return
        }
        completeObservatory(game)
    }

    fun clear() {
        activeGame = null
        targetIndex = 0
        resetTargetInputs()
        completionStarted = false
        feedbackEvent = null
    }

    private suspend fun startObservatory(difficulty: MiniGameDifficulty) {
        val miniGamesState = miniGamesRepository.loadMiniGamesState()
        val dailyState = miniGamesState.progress.dailyStateFor(
            miniGameId = MiniGameId.Observatory,
            dateUtc = miniGamesState.todayUtc,
        )
        if (dailyState.hasPlayed || dailyState.reward != null) {
            uiState.value = miniGamesState.toUiState(
                screen = alreadyPlayedScreen(dailyState.reward),
            )
            return
        }
        val unlockedDifficulty = miniGamesState.progress.unlockedDifficultyFor(MiniGameId.Observatory)
        if (difficulty.level > unlockedDifficulty.level) {
            uiState.value = miniGamesState.toUiState(
                screen = MiniGamesScreenUiState.ObservatoryUnavailable(
                    message = "Cette difficulté n'est pas encore débloquée.",
                ),
            )
            return
        }

        val spec = ObservatoryDifficultySpec.forDifficulty(difficulty)
        val resolvedCards = miniGamesRepository.prepareResolvedCardsForToday(
            miniGameId = MiniGameId.Observatory,
            slotCount = spec.targetCount,
            distinctOwnedCards = true,
        )
        val game = when (
            val buildResult = buildObservatoryGame(
                difficulty = difficulty,
                dateUtc = miniGamesState.todayUtc,
                resolvedCards = resolvedCards,
                cards = catalogRepository.loadCards(),
                extensions = catalogRepository.loadExtensions(),
                variantProfiles = catalogRepository.loadVariantProfiles(),
            )
        ) {
            is ObservatoryGameBuildResult.Ready -> buildResult.game
            is ObservatoryGameBuildResult.Unavailable -> {
                uiState.value = miniGamesState.toUiState(
                    screen = MiniGamesScreenUiState.ObservatoryUnavailable(buildResult.message),
                )
                return
            }
        }

        when (val consumed = miniGamesRepository.consumeAttemptForToday(MiniGameId.Observatory)) {
            is MiniGameAttemptConsumeResult.Consumed -> {
                activeGame = game
                targetIndex = 0
                resetTargetInputs()
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
                    screen = alreadyPlayedScreen(consumed.dailyState.reward),
                )
            }
        }
    }

    private fun advanceFromAlignmentIfReady(game: ObservatoryGame) {
        val target = game.targets[targetIndex]
        val ready = isObservatorySettingReady(azimuth, target.azimuth, game.tolerance) &&
            isObservatorySettingReady(altitude, target.altitude, game.tolerance)
        if (!ready) {
            feedbackEvent = null
            return
        }
        feedbackEvent = feedbackEmitter.next(
            tone = MiniGameFeedbackTone.Success,
            sourceIndexes = setOf(targetIndex),
        )
        step = if (target.hasCloudEvent) {
            ObservatoryStep.ClearCloud
        } else {
            ObservatoryStep.Focus
        }
    }

    private fun completeObservatory(game: ObservatoryGame) {
        if (completionStarted) return
        completionStarted = true
        feedbackEvent = feedbackEmitter.next(
            tone = MiniGameFeedbackTone.Completion,
            sourceIndexes = emptySet(),
        )
        publishPlayingState(game)

        launch {
            runCatching {
                val grantResult = miniGamesRepository.grantRewardForToday(
                    miniGameId = MiniGameId.Observatory,
                    reward = game.difficulty.reward,
                )
                val nextDifficulty = game.difficulty.next()
                if (grantResult is MiniGameRewardGrantResult.Granted && nextDifficulty != null) {
                    miniGamesRepository.unlockDifficulty(
                        miniGameId = MiniGameId.Observatory,
                        difficulty = nextDifficulty,
                    )
                }
                val refreshed = miniGamesRepository.loadMiniGamesState()
                val resultFeedbackEvent = feedbackEmitter.next(
                    tone = MiniGameFeedbackTone.Completion,
                    sourceIndexes = emptySet(),
                )
                clear()
                refreshed.toUiState(
                    screen = MiniGamesScreenUiState.ObservatoryResult(
                        difficultyName = game.difficulty.displayName,
                        rewardLabel = formatReward(game.difficulty.reward),
                        targetCount = game.targetCount,
                        nextDifficultyName = nextDifficulty
                            ?.takeIf { grantResult is MiniGameRewardGrantResult.Granted }
                            ?.displayName,
                        feedbackEvent = resultFeedbackEvent,
                    ),
                )
            }.onSuccess { updatedState ->
                uiState.value = updatedState
            }.onFailure { error ->
                clear()
                uiState.update {
                    it.copy(
                        isLoading = false,
                        screen = MiniGamesScreenUiState.ObservatoryUnavailable(
                            message = error.message ?: "Impossible d'attribuer la récompense.",
                        ),
                    )
                }
            }
        }
    }

    private fun resetTargetInputs() {
        step = ObservatoryStep.OpenDome
        domeProgress = 0f
        azimuth = 0f
        altitude = 0f
        focus = 0f
    }

    private fun publishPlayingState(game: ObservatoryGame) {
        uiState.update {
            it.copy(
                isLoading = false,
                screen = buildPlayingState(game),
            )
        }
    }

    private fun buildPlayingState(game: ObservatoryGame): MiniGamesScreenUiState.ObservatoryPlaying {
        val target = game.targets[targetIndex]
        val spec = ObservatoryDifficultySpec.forDifficulty(game.difficulty)
        val alignmentReady = isObservatorySettingReady(azimuth, target.azimuth, game.tolerance) &&
            isObservatorySettingReady(altitude, target.altitude, game.tolerance)
        val focusReady = isObservatorySettingReady(focus, target.focus, game.tolerance)
        return MiniGamesScreenUiState.ObservatoryPlaying(
            difficultyName = game.difficulty.displayName,
            rewardLabel = formatReward(game.difficulty.reward),
            targetIndex = targetIndex,
            targetCount = game.targetCount,
            targetCard = target.displayCard,
            step = step,
            stepTitle = step.title,
            stepInstruction = step.instruction,
            domeProgress = domeProgress,
            azimuth = azimuth,
            altitude = altitude,
            focus = focus,
            targetAzimuthLabel = target.azimuthLabel,
            targetAltitudeLabel = target.altitudeLabel,
            targetFocusLabel = target.focusLabel,
            toleranceLabel = spec.precisionLabel,
            domeReady = domeProgress >= ObservatoryDomeReadyThreshold,
            alignmentReady = alignmentReady,
            focusReady = focusReady,
            canClearCloud = step == ObservatoryStep.ClearCloud,
            canCapture = step == ObservatoryStep.Capture && !completionStarted,
            feedbackEvent = feedbackEvent,
        )
    }

    private fun alreadyPlayedScreen(reward: MiniGameReward?): MiniGamesScreenUiState =
        if (reward != null) {
            MiniGamesScreenUiState.ObservatoryResult(
                difficultyName = observatoryDifficultyNameForReward(formatReward(reward)),
                rewardLabel = formatReward(reward),
                targetCount = 0,
                nextDifficultyName = null,
                feedbackEvent = null,
            )
        } else {
            MiniGamesScreenUiState.ObservatoryUnavailable(
                message = "Ton essai Observatoire est déjà utilisé pour aujourd'hui.",
            )
        }
}

private val ObservatoryStep.title: String
    get() = when (this) {
        ObservatoryStep.OpenDome -> "Ouverture de la coupole"
        ObservatoryStep.Align -> "Alignement du télescope"
        ObservatoryStep.ClearCloud -> "Nuage de passage"
        ObservatoryStep.Focus -> "Mise au point"
        ObservatoryStep.Capture -> "Capture"
    }

private val ObservatoryStep.instruction: String
    get() = when (this) {
        ObservatoryStep.OpenDome -> "Glisse jusqu'au bout pour ouvrir le panneau."
        ObservatoryStep.Align -> "Rapproche l'azimut et l'altitude de leurs repères."
        ObservatoryStep.ClearCloud -> "Patiente un instant et dégage la fenêtre d'observation."
        ObservatoryStep.Focus -> "Ajuste la netteté avant la capture."
        ObservatoryStep.Capture -> "La cible est prête."
    }

private const val ObservatoryDomeReadyThreshold: Float = 0.98f
