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
import kotlinx.coroutines.delay
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
    private var azimuth: Float = ObservatoryCenteredSetting
    private var altitude: Float = ObservatoryCenteredSetting
    private var focus: Float = 0f
    private var captureProgress: Float = 0f
    private var cloudProgress: Float = 0f
    private var cloudPausedStep: ObservatoryStep? = null
    private var cloudCycleGeneration: Long = 0L
    private var cloudCycleActive: Boolean = false
    private var captureDecayGeneration: Long = 0L
    private var captureValidationGeneration: Long = 0L
    private var captureValidationPending: Boolean = false
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
        if (
            completionStarted ||
            (step != ObservatoryStep.OpenDome && step != ObservatoryStep.CloseDome)
        ) {
            return
        }
        domeProgress = progress.coerceIn(0f, 1f)
        feedbackEvent = null
        publishPlayingState(game)
    }

    fun validateDomeProgress() {
        val game = activeGame ?: return
        if (completionStarted) return
        when (step) {
            ObservatoryStep.OpenDome -> {
                if (domeProgress >= ObservatoryDomeReadyThreshold) {
                    feedbackEvent = feedbackEmitter.next(
                        tone = MiniGameFeedbackTone.Success,
                        sourceIndexes = setOf(targetIndex),
                    )
                    step = ObservatoryStep.Align
                    startCloudCycleIfNeeded(game)
                }
                publishPlayingState(game)
            }

            ObservatoryStep.CloseDome -> {
                if (domeProgress <= ObservatoryDomeClosedThreshold) {
                    completeObservatory(game)
                    return
                }
                publishPlayingState(game)
            }

            else -> Unit
        }
    }

    fun setAzimuth(value: Float) {
        val game = activeGame ?: return
        if (completionStarted || step != ObservatoryStep.Align) return
        azimuth = value.coerceIn(0f, 1f)
        feedbackEvent = null
        publishPlayingState(game)
    }

    fun setAltitude(value: Float) {
        val game = activeGame ?: return
        if (completionStarted || step != ObservatoryStep.Align) return
        altitude = value.coerceIn(0f, 1f)
        feedbackEvent = null
        publishPlayingState(game)
    }

    fun validateAlignment() {
        val game = activeGame ?: return
        if (completionStarted || step != ObservatoryStep.Align) return
        advanceFromAlignmentIfReady(game)
        publishPlayingState(game)
    }

    fun scrubCloud(amount: Float) {
        val game = activeGame ?: return
        if (completionStarted || step != ObservatoryStep.ClearCloud) return
        cloudProgress = (cloudProgress - amount.coerceAtLeast(0f)).coerceIn(0f, 1f)
        if (cloudProgress > ObservatoryCloudClearedThreshold) {
            feedbackEvent = null
            publishPlayingState(game)
            return
        }
        resumeAfterCloud(game)
    }

    fun clearCloud() {
        scrubCloud(1f)
    }

    private fun resumeAfterCloud(game: ObservatoryGame) {
        val resumedStep = cloudPausedStep ?: ObservatoryStep.Focus
        cloudPausedStep = null
        cloudProgress = 0f
        feedbackEvent = feedbackEmitter.next(
            tone = MiniGameFeedbackTone.Special,
            sourceIndexes = setOf(targetIndex),
        )
        step = resumedStep
        if (resumedStep == ObservatoryStep.Capture && !captureValidationPending) {
            startCaptureDecay(game)
        }
        startCloudCycleIfNeeded(game)
        publishPlayingState(game)
    }

    fun setFocus(value: Float) {
        val game = activeGame ?: return
        if (completionStarted || step != ObservatoryStep.Focus) return
        focus = value.coerceIn(0f, 1f)
        feedbackEvent = null
        publishPlayingState(game)
    }

    fun validateFocus() {
        val game = activeGame ?: return
        if (completionStarted || step != ObservatoryStep.Focus) return
        val target = game.targets[targetIndex]
        if (isObservatorySettingReady(focus, target.focus, game.tolerance)) {
            feedbackEvent = feedbackEmitter.next(
                tone = MiniGameFeedbackTone.Success,
                sourceIndexes = setOf(targetIndex),
            )
            beginCaptureStep(game)
        }
        publishPlayingState(game)
    }

    fun captureTarget() {
        val game = activeGame ?: return
        if (completionStarted || step != ObservatoryStep.Capture || captureValidationPending) return
        captureProgress = (captureProgress + ObservatoryCapturePressBoost).coerceIn(0f, 1f)
        if (captureProgress < ObservatoryCaptureReadyThreshold) {
            feedbackEvent = null
            publishPlayingState(game)
            return
        }
        captureProgress = 1f
        captureValidationPending = true
        stopCaptureDecay()
        feedbackEvent = feedbackEmitter.next(
            tone = MiniGameFeedbackTone.Success,
            sourceIndexes = setOf(targetIndex),
        )
        publishPlayingState(game)
        scheduleCaptureResolution(game)
    }

    fun clear() {
        activeGame = null
        targetIndex = 0
        stopCloudCycle()
        stopCaptureDecay()
        cancelCaptureValidation()
        resetSessionInputs()
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
                resetSessionInputs()
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
        azimuth = target.azimuth
        altitude = target.altitude
        step = ObservatoryStep.Focus
        startCloudCycleIfNeeded(game)
    }

    private fun completeObservatory(game: ObservatoryGame) {
        if (completionStarted) return
        completionStarted = true
        stopCloudCycle()
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

    private fun resetSessionInputs() {
        stopCloudCycle()
        step = ObservatoryStep.OpenDome
        domeProgress = 0f
        azimuth = ObservatoryCenteredSetting
        altitude = ObservatoryCenteredSetting
        focus = 0f
        captureProgress = 0f
        cloudProgress = 0f
        cloudPausedStep = null
        captureValidationPending = false
    }

    private fun prepareNextTarget() {
        stopCaptureDecay()
        cancelCaptureValidation()
        step = ObservatoryStep.Align
        domeProgress = 1f
        focus = 0f
        captureProgress = 0f
        cloudPausedStep = null
        activeGame?.let(::startCloudCycleIfNeeded)
    }

    private fun beginCaptureStep(game: ObservatoryGame) {
        step = ObservatoryStep.Capture
        captureProgress = 0f
        captureValidationPending = false
        startCaptureDecay(game)
        startCloudCycleIfNeeded(game)
    }

    private fun scheduleCaptureResolution(game: ObservatoryGame) {
        captureValidationGeneration += 1L
        val generation = captureValidationGeneration
        launch {
            delay(ObservatoryCaptureValidationDelayMillis)
            if (
                generation != captureValidationGeneration ||
                activeGame !== game ||
                completionStarted ||
                step != ObservatoryStep.Capture ||
                !captureValidationPending
            ) {
                return@launch
            }
            finishCapturedTarget(game)
        }
    }

    private fun finishCapturedTarget(game: ObservatoryGame) {
        captureValidationPending = false
        captureProgress = 0f
        cloudPausedStep = null
        if (targetIndex < game.targets.lastIndex) {
            targetIndex += 1
            prepareNextTarget()
            feedbackEvent = null
            publishPlayingState(game)
            return
        }
        step = ObservatoryStep.CloseDome
        domeProgress = 1f
        focus = 0f
        feedbackEvent = null
        startCloudCycleIfNeeded(game)
        publishPlayingState(game)
    }

    private fun startCaptureDecay(game: ObservatoryGame) {
        captureDecayGeneration += 1L
        val generation = captureDecayGeneration
        launch {
            while (true) {
                delay(ObservatoryCaptureDecayTickMillis)
                if (
                    generation != captureDecayGeneration ||
                    activeGame !== game ||
                    completionStarted ||
                    step != ObservatoryStep.Capture
                ) {
                    return@launch
                }

                val decay = game.difficulty.observatoryCaptureDecayPerSecond() *
                    (ObservatoryCaptureDecayTickMillis / 1_000f)
                val nextProgress = (captureProgress - decay).coerceAtLeast(0f)
                if (nextProgress != captureProgress) {
                    captureProgress = nextProgress
                    feedbackEvent = null
                    publishPlayingState(game)
                }
            }
        }
    }

    private fun stopCaptureDecay() {
        captureDecayGeneration += 1L
    }

    private fun startCloudCycleIfNeeded(game: ObservatoryGame) {
        if (!shouldRunCloudCycle(game)) {
            stopCloudCycle()
            return
        }
        if (cloudCycleActive) return

        cloudCycleActive = true
        cloudCycleGeneration += 1L
        val generation = cloudCycleGeneration
        launch {
            delay(observatoryRandomCloudInterCycleWaitMillis())
            if (generation != cloudCycleGeneration) {
                return@launch
            }
            if (!shouldRunCloudCycle(game)) {
                cloudCycleActive = false
                return@launch
            }

            while (true) {
                delay(ObservatoryCloudAccumulationTickMillis)
                if (generation != cloudCycleGeneration) {
                    return@launch
                }
                if (!shouldRunCloudCycle(game)) {
                    cloudCycleActive = false
                    return@launch
                }

                val nextProgress = observatoryCloudProgressAfterTick(cloudProgress)
                if (nextProgress >= 1f) {
                    val wasAlreadyFull = cloudProgress >= 1f
                    cloudProgress = 1f
                    if (!isCloudInterruptibleStep()) {
                        if (!wasAlreadyFull) {
                            publishPlayingState(game)
                        }
                        continue
                    }
                    pauseForCloud(game)
                    cloudCycleActive = false
                    return@launch
                }
                cloudProgress = nextProgress
                feedbackEvent = null
                publishPlayingState(game)
            }
        }
    }

    private fun stopCloudCycle() {
        cloudCycleGeneration += 1L
        cloudCycleActive = false
    }

    private fun shouldRunCloudCycle(game: ObservatoryGame): Boolean {
        if (activeGame !== game || completionStarted || cloudPausedStep != null) return false
        return game.targets.getOrNull(targetIndex) != null && isCloudCycleStep()
    }

    private fun isCloudCycleStep(): Boolean =
        when (step) {
            ObservatoryStep.Align,
            ObservatoryStep.Focus,
            ObservatoryStep.Capture,
            ObservatoryStep.CloseDome -> true

            ObservatoryStep.OpenDome,
            ObservatoryStep.ClearCloud -> false
        }

    private fun isCloudInterruptibleStep(): Boolean =
        when (step) {
            ObservatoryStep.Align,
            ObservatoryStep.Focus,
            ObservatoryStep.CloseDome -> true

            ObservatoryStep.Capture -> !captureValidationPending

            ObservatoryStep.OpenDome,
            ObservatoryStep.ClearCloud -> false
        }

    private fun pauseForCloud(game: ObservatoryGame) {
        if (!isCloudInterruptibleStep()) return
        cloudPausedStep = step
        if (step == ObservatoryStep.Capture) {
            stopCaptureDecay()
        }
        step = ObservatoryStep.ClearCloud
        feedbackEvent = feedbackEmitter.next(
            tone = MiniGameFeedbackTone.Special,
            sourceIndexes = setOf(targetIndex),
        )
        publishPlayingState(game)
    }

    private fun cancelCaptureValidation() {
        captureValidationGeneration += 1L
        captureValidationPending = false
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
            captureProgress = captureProgress,
            cloudProgress = cloudProgress,
            targetAzimuth = target.azimuth,
            targetAltitude = target.altitude,
            targetFocus = target.focus,
            tolerance = game.tolerance,
            toleranceLabel = spec.precisionLabel,
            domeReady = domeProgress >= ObservatoryDomeReadyThreshold,
            domeClosed = domeProgress <= ObservatoryDomeClosedThreshold,
            alignmentReady = alignmentReady,
            focusReady = focusReady,
            canClearCloud = step == ObservatoryStep.ClearCloud,
            canCapture = step == ObservatoryStep.Capture && !completionStarted && !captureValidationPending,
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
        ObservatoryStep.CloseDome -> "Fermeture de la coupole"
    }

private val ObservatoryStep.instruction: String
    get() = when (this) {
        ObservatoryStep.OpenDome -> "Glisse jusqu'au bout pour ouvrir le panneau."
        ObservatoryStep.Align -> "Aligne le réticule mobile sur la cible lumineuse."
        ObservatoryStep.ClearCloud -> "Efface-le avec ton doigt"
        ObservatoryStep.Focus -> "Fais coïncider l'anneau du réticule avec la cible."
        ObservatoryStep.Capture -> "Appuie plusieurs fois pour stabiliser la capture."
        ObservatoryStep.CloseDome -> "Referme la coupole pour terminer l'observation."
    }

private const val ObservatoryDomeReadyThreshold: Float = 0.98f
private const val ObservatoryDomeClosedThreshold: Float = 0.02f
private const val ObservatoryCenteredSetting: Float = 0.5f
private const val ObservatoryCaptureReadyThreshold: Float = 0.999f
private const val ObservatoryCaptureValidationDelayMillis: Long = 720L
private const val ObservatoryCloudClearedThreshold: Float = 0.02f
