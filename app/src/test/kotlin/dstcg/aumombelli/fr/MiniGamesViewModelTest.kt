package fr.aumombelli.dstcg

import fr.aumombelli.dstcg.data.MiniGamesRepository
import fr.aumombelli.dstcg.feature.minigames.MemoryCardRole
import fr.aumombelli.dstcg.feature.minigames.MemoryCellState
import fr.aumombelli.dstcg.feature.minigames.MiniGameFeedbackTone
import fr.aumombelli.dstcg.feature.minigames.MiniGamesScreenUiState
import fr.aumombelli.dstcg.feature.minigames.MiniGamesViewModel
import fr.aumombelli.dstcg.feature.minigames.ObservatoryCloudAccumulationDurationMillis
import fr.aumombelli.dstcg.feature.minigames.ObservatoryCloudAccumulationTickMillis
import fr.aumombelli.dstcg.feature.minigames.ObservatoryCloudInterCycleWaitMaxMillis
import fr.aumombelli.dstcg.feature.minigames.ObservatoryGame
import fr.aumombelli.dstcg.feature.minigames.ObservatoryGameBuildResult
import fr.aumombelli.dstcg.feature.minigames.ObservatoryStep
import fr.aumombelli.dstcg.feature.minigames.ObservatoryTarget
import fr.aumombelli.dstcg.feature.minigames.buildObservatoryGame
import fr.aumombelli.dstcg.feature.minigames.QuizAnswerState
import fr.aumombelli.dstcg.feature.minigames.QuizGame
import fr.aumombelli.dstcg.feature.minigames.QuizGameBuildResult
import fr.aumombelli.dstcg.feature.minigames.TimelineGame
import fr.aumombelli.dstcg.feature.minigames.TimelineGameBuildResult
import fr.aumombelli.dstcg.feature.minigames.buildQuizGame
import fr.aumombelli.dstcg.feature.minigames.buildTimelineGame
import fr.aumombelli.dstcg.feature.minigames.eligibleTimelineCardIds
import fr.aumombelli.dstcg.feature.minigames.selectPlayableTimelineCriterion
import fr.aumombelli.dstcg.feature.minigames.timelineResolvedCardCountForDifficulty
import fr.aumombelli.dstcg.model.AbsoluteMagnitudeMeasurement
import fr.aumombelli.dstcg.model.AngularMeasurement
import fr.aumombelli.dstcg.model.CardDefinition
import fr.aumombelli.dstcg.model.DeepSkyDetails
import fr.aumombelli.dstcg.model.ExtensionDefinition
import fr.aumombelli.dstcg.model.LightYearMeasurement
import fr.aumombelli.dstcg.model.MiniGameDifficulty
import fr.aumombelli.dstcg.model.MiniGameId
import fr.aumombelli.dstcg.model.MiniGameReward
import fr.aumombelli.dstcg.model.MiniGamesProgress
import fr.aumombelli.dstcg.model.OwnedCardEntry
import fr.aumombelli.dstcg.model.OwnedCollection
import fr.aumombelli.dstcg.model.OwnedVariantCount
import fr.aumombelli.dstcg.model.PackRechargeState
import fr.aumombelli.dstcg.model.SolarSystemDetails
import fr.aumombelli.dstcg.model.StandaloneProgress
import fr.aumombelli.dstcg.model.VisualSize
import fr.aumombelli.dstcg.model.dailyStateFor
import fr.aumombelli.dstcg.model.unlockedDifficultyFor
import java.time.Instant
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MiniGamesViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val now = Instant.parse("2026-05-10T12:00:00Z")

    @Test
    fun `opening memory then returning before difficulty does not consume attempt`() = runTest {
        val fixture = newFixture(cardCount = 2)
        val viewModel = fixture.newViewModel()
        advanceUntilIdle()

        viewModel.openMemory()
        viewModel.backToMenu()
        advanceUntilIdle()

        val dailyState = fixture.progressGateway.progress.miniGamesProgress
            .dailyStateFor(MiniGameId.Memory, "2026-05-10")
        assertTrue(viewModel.uiState.value.screen is MiniGamesScreenUiState.Menu)
        assertTrue(!dailyState.hasPlayed)
        assertNull(dailyState.reward)
    }

    @Test
    fun `opening quiz then returning before difficulty does not consume attempt`() = runTest {
        val fixture = newQuizFixture()
        val viewModel = fixture.newViewModel()
        advanceUntilIdle()

        viewModel.openQuiz()
        advanceUntilIdle()
        viewModel.backToMenu()
        advanceUntilIdle()

        val dailyState = fixture.progressGateway.progress.miniGamesProgress
            .dailyStateFor(MiniGameId.Quiz, "2026-05-10")
        assertTrue(viewModel.uiState.value.screen is MiniGamesScreenUiState.Menu)
        assertTrue(!dailyState.hasPlayed)
        assertNull(dailyState.reward)
    }

    @Test
    fun `opening observatory then returning before difficulty does not consume attempt`() = runTest {
        val fixture = newFixture(cardCount = 2)
        val viewModel = fixture.newViewModel()
        advanceUntilIdle()

        viewModel.openObservatory()
        viewModel.backToMenu()
        advanceUntilIdle()

        val dailyState = fixture.progressGateway.progress.miniGamesProgress
            .dailyStateFor(MiniGameId.Observatory, "2026-05-10")
        assertTrue(viewModel.uiState.value.screen is MiniGamesScreenUiState.Menu)
        assertTrue(!dailyState.hasPlayed)
        assertNull(dailyState.reward)
    }

    @Test
    fun `opening timeline then returning before difficulty does not consume attempt`() = runTest {
        val fixture = newTimelineFixture()
        val viewModel = fixture.newViewModel()
        advanceUntilIdle()

        viewModel.openTimeline()
        advanceUntilIdle()
        viewModel.backToMenu()
        advanceUntilIdle()

        val dailyState = fixture.progressGateway.progress.miniGamesProgress
            .dailyStateFor(MiniGameId.Timeline, "2026-05-10")
        assertTrue(viewModel.uiState.value.screen is MiniGamesScreenUiState.Menu)
        assertTrue(!dailyState.hasPlayed)
        assertNull(dailyState.reward)
    }

    @Test
    fun `locked timeline difficulty is ignored`() = runTest {
        val fixture = newTimelineFixture()
        val viewModel = fixture.newViewModel()
        advanceUntilIdle()

        viewModel.openTimeline()
        advanceUntilIdle()
        viewModel.selectTimelineDifficulty(MiniGameDifficulty.Observer)
        advanceUntilIdle()

        val dailyState = fixture.progressGateway.progress.miniGamesProgress
            .dailyStateFor(MiniGameId.Timeline, "2026-05-10")
        assertTrue(viewModel.uiState.value.screen is MiniGamesScreenUiState.TimelineDifficultySelection)
        assertTrue(!dailyState.hasPlayed)
        assertNull(dailyState.reward)
    }

    @Test
    fun `selecting timeline difficulty consumes attempt without reward`() = runTest {
        val fixture = newTimelineFixture()
        val expectedTimeline = fixture.buildTimeline(MiniGameDifficulty.Apprentice)
        val viewModel = fixture.newViewModel()
        advanceUntilIdle()

        viewModel.openTimeline()
        advanceUntilIdle()
        viewModel.selectTimelineDifficulty(MiniGameDifficulty.Apprentice)
        advanceUntilIdle()

        val dailyState = fixture.progressGateway.progress.miniGamesProgress
            .dailyStateFor(MiniGameId.Timeline, "2026-05-10")
        val playing = viewModel.uiState.value.screen as MiniGamesScreenUiState.TimelinePlaying
        assertTrue(dailyState.hasPlayed)
        assertNull(dailyState.reward)
        assertEquals(1, playing.comparisonCount)
        assertEquals(0, playing.score)
        assertEquals(2, playing.slots.size)
        assertEquals(2, playing.handSlots.size)
        assertEquals(expectedTimeline.criterion.firstSlotLabel, playing.slots.first().emptyLabel)
        assertEquals(expectedTimeline.criterion.lastSlotLabel, playing.slots.last().emptyLabel)
        assertTrue(!playing.canValidate)
    }

    @Test
    fun `placing timeline cards can swap occupied comparison slots`() = runTest {
        val fixture = newTimelineFixture()
        val viewModel = fixture.newViewModel()
        advanceUntilIdle()

        viewModel.openTimeline()
        advanceUntilIdle()
        viewModel.selectTimelineDifficulty(MiniGameDifficulty.Apprentice)
        advanceUntilIdle()
        val initial = viewModel.uiState.value.screen as MiniGamesScreenUiState.TimelinePlaying
        val firstHandCard = initial.handSlots.filterNotNull().first()
        val secondHandCard = initial.handSlots.filterNotNull()[1]

        viewModel.placeTimelineCard(firstHandCard.id, slotIndex = 0)
        viewModel.placeTimelineCard(secondHandCard.id, slotIndex = 1)
        viewModel.placeTimelineCard(firstHandCard.id, slotIndex = 1)
        val updated = viewModel.uiState.value.screen as MiniGamesScreenUiState.TimelinePlaying

        assertEquals(secondHandCard.id, updated.slots[0].placedCard?.id)
        assertEquals(firstHandCard.id, updated.slots[1].placedCard?.id)
    }

    @Test
    fun `leaving timeline after difficulty consumes attempt without reward`() = runTest {
        val fixture = newTimelineFixture()
        val viewModel = fixture.newViewModel()
        advanceUntilIdle()

        viewModel.openTimeline()
        advanceUntilIdle()
        viewModel.selectTimelineDifficulty(MiniGameDifficulty.Apprentice)
        advanceUntilIdle()
        viewModel.backToMenu()
        advanceUntilIdle()

        val dailyState = fixture.progressGateway.progress.miniGamesProgress
            .dailyStateFor(MiniGameId.Timeline, "2026-05-10")
        assertTrue(dailyState.hasPlayed)
        assertNull(dailyState.reward)
        assertTrue(viewModel.uiState.value.screen is MiniGamesScreenUiState.Menu)
    }

    @Test
    fun `perfect apprentice timeline grants reward and unlocks observer`() = runTest {
        val fixture = newTimelineFixture()
        val timeline = fixture.buildTimeline(MiniGameDifficulty.Apprentice)
        val viewModel = fixture.newViewModel()
        advanceUntilIdle()

        viewModel.openTimeline()
        advanceUntilIdle()
        viewModel.selectTimelineDifficulty(MiniGameDifficulty.Apprentice)
        advanceUntilIdle()
        timeline.comparisons.single().correctSlots.forEachIndexed { index, card ->
            viewModel.placeTimelineCard(card.id, index)
        }
        val ready = viewModel.uiState.value.screen as MiniGamesScreenUiState.TimelinePlaying
        assertTrue(ready.canValidate)

        viewModel.validateTimeline()
        advanceUntilIdle()

        val feedback = viewModel.uiState.value.screen as MiniGamesScreenUiState.TimelinePlaying
        assertTrue(!feedback.canValidate)
        assertEquals(true, feedback.currentCorrection?.isCorrect)
        assertNull(fixture.progressGateway.progress.miniGamesProgress.dailyStateFor(MiniGameId.Timeline, "2026-05-10").reward)

        viewModel.continueTimeline()
        advanceUntilIdle()

        val progress = fixture.progressGateway.progress.miniGamesProgress
        val dailyState = progress.dailyStateFor(MiniGameId.Timeline, "2026-05-10")
        val result = viewModel.uiState.value.screen as MiniGamesScreenUiState.TimelineResult
        assertEquals(MiniGameReward.fromMinutes(15L), dailyState.reward)
        assertEquals("1/1", result.scoreLabel)
        assertEquals("15min", result.rewardLabel)
        assertEquals(1, result.corrections.size)
        assertEquals(MiniGameDifficulty.Observer, progress.unlockedDifficultyFor(MiniGameId.Timeline))
    }

    @Test
    fun `timeline unavailable does not consume attempt`() = runTest {
        val card = timelineDeepSkyCard("ALP-001", distance = 100.0)
        val fixture = newFixture(
            cardCount = 1,
            cardDefinitions = listOf(card),
            variants = mapOf(card.id to listOf(OwnedVariantCount("city", "standard", 1))),
        )
        val viewModel = fixture.newViewModel()
        advanceUntilIdle()

        viewModel.openTimeline()
        advanceUntilIdle()

        val dailyState = fixture.progressGateway.progress.miniGamesProgress
            .dailyStateFor(MiniGameId.Timeline, "2026-05-10")
        assertTrue(viewModel.uiState.value.screen is MiniGamesScreenUiState.TimelineUnavailable)
        assertTrue(!dailyState.hasPlayed)
        assertNull(dailyState.reward)
    }

    @Test
    fun `locked observatory difficulty is ignored`() = runTest {
        val fixture = newFixture(cardCount = 4)
        val viewModel = fixture.newViewModel()
        advanceUntilIdle()

        viewModel.openObservatory()
        viewModel.selectObservatoryDifficulty(MiniGameDifficulty.Observer)
        advanceUntilIdle()

        val dailyState = fixture.progressGateway.progress.miniGamesProgress
            .dailyStateFor(MiniGameId.Observatory, "2026-05-10")
        assertTrue(viewModel.uiState.value.screen is MiniGamesScreenUiState.ObservatoryDifficultySelection)
        assertTrue(!dailyState.hasPlayed)
        assertNull(dailyState.reward)
    }

    @Test
    fun `selecting observatory difficulty consumes attempt without reward`() = runTest {
        val fixture = newFixture(cardCount = 2)
        val viewModel = fixture.newViewModel()
        advanceUntilIdle()

        viewModel.openObservatory()
        viewModel.selectObservatoryDifficulty(MiniGameDifficulty.Apprentice)
        advanceUntilIdle()

        val dailyState = fixture.progressGateway.progress.miniGamesProgress
            .dailyStateFor(MiniGameId.Observatory, "2026-05-10")
        val playing = viewModel.uiState.value.screen as MiniGamesScreenUiState.ObservatoryPlaying
        assertTrue(dailyState.hasPlayed)
        assertNull(dailyState.reward)
        assertEquals(0, playing.targetIndex)
        assertEquals(1, playing.targetCount)
        assertEquals(ObservatoryStep.OpenDome, playing.step)
        assertEquals(0.5f, playing.azimuth, 0.001f)
        assertEquals(0.5f, playing.altitude, 0.001f)
    }

    @Test
    fun `observatory alignment snaps only after slider release`() = runTest {
        val fixture = newFixture(cardCount = 2)
        val expectedGame = fixture.buildObservatory(MiniGameDifficulty.Apprentice)
        val target = expectedGame.targets.first()
        val azimuthInTolerance = target.azimuth + valueOffsetInsideTolerance(
            targetValue = target.azimuth,
            tolerance = expectedGame.tolerance,
        )
        val altitudeInTolerance = target.altitude + valueOffsetInsideTolerance(
            targetValue = target.altitude,
            tolerance = expectedGame.tolerance,
        )
        val viewModel = fixture.newViewModel()
        advanceUntilIdle()

        viewModel.openObservatory()
        viewModel.selectObservatoryDifficulty(MiniGameDifficulty.Apprentice)
        advanceUntilIdle()
        viewModel.setObservatoryDomeProgress(1f)
        val afterDomeDrag = viewModel.uiState.value.screen as MiniGamesScreenUiState.ObservatoryPlaying
        assertEquals(ObservatoryStep.OpenDome, afterDomeDrag.step)
        assertTrue(afterDomeDrag.domeReady)

        viewModel.validateObservatoryDomeProgress()
        viewModel.setObservatoryAzimuth(azimuthInTolerance)
        viewModel.setObservatoryAltitude(altitudeInTolerance)

        val afterAlignmentDrag = viewModel.uiState.value.screen as MiniGamesScreenUiState.ObservatoryPlaying
        assertEquals(ObservatoryStep.Align, afterAlignmentDrag.step)
        assertTrue(afterAlignmentDrag.alignmentReady)
        assertEquals(azimuthInTolerance, afterAlignmentDrag.azimuth, 0.001f)
        assertEquals(altitudeInTolerance, afterAlignmentDrag.altitude, 0.001f)

        viewModel.validateObservatoryAlignment()

        val afterAlignmentRelease = viewModel.uiState.value.screen as MiniGamesScreenUiState.ObservatoryPlaying
        assertEquals(ObservatoryStep.Focus, afterAlignmentRelease.step)
        assertEquals(target.azimuth, afterAlignmentRelease.azimuth, 0.001f)
        assertEquals(target.altitude, afterAlignmentRelease.altitude, 0.001f)
    }

    @Test
    fun `observatory cloud accumulates over time and pauses alignment`() = runTest {
        val fixture = newFixture(cardCount = 2)
        val expectedGame = fixture.buildObservatory(MiniGameDifficulty.Apprentice)
        val target = expectedGame.targets.first()
        val viewModel = fixture.newViewModel()
        advanceUntilIdle()

        viewModel.openObservatory()
        viewModel.selectObservatoryDifficulty(MiniGameDifficulty.Apprentice)
        advanceUntilIdle()
        openObservatoryDome(viewModel)

        val afterOpening = viewModel.uiState.value.screen as MiniGamesScreenUiState.ObservatoryPlaying
        assertEquals(ObservatoryStep.Align, afterOpening.step)
        assertEquals(0f, afterOpening.cloudProgress, 0.001f)

        advanceObservatoryCloudCycle()

        val cloudPause = viewModel.uiState.value.screen as MiniGamesScreenUiState.ObservatoryPlaying
        assertEquals(ObservatoryStep.ClearCloud, cloudPause.step)
        assertEquals(1f, cloudPause.cloudProgress, 0.001f)
        assertTrue(cloudPause.canClearCloud)

        viewModel.setObservatoryAzimuth(target.azimuth)
        val stillPaused = viewModel.uiState.value.screen as MiniGamesScreenUiState.ObservatoryPlaying
        assertEquals(ObservatoryStep.ClearCloud, stillPaused.step)

        viewModel.scrubObservatoryCloud(0.40f)

        val partiallyCleared = viewModel.uiState.value.screen as MiniGamesScreenUiState.ObservatoryPlaying
        assertEquals(ObservatoryStep.ClearCloud, partiallyCleared.step)
        assertEquals(0.60f, partiallyCleared.cloudProgress, 0.001f)

        viewModel.scrubObservatoryCloud(1f)

        val resumed = viewModel.uiState.value.screen as MiniGamesScreenUiState.ObservatoryPlaying
        assertEquals(ObservatoryStep.Align, resumed.step)
        assertEquals(0f, resumed.cloudProgress, 0.001f)

        advanceObservatoryCloudCycle()

        val secondCloudPause = viewModel.uiState.value.screen as MiniGamesScreenUiState.ObservatoryPlaying
        assertEquals(ObservatoryStep.ClearCloud, secondCloudPause.step)
        assertEquals(1f, secondCloudPause.cloudProgress, 0.001f)
    }

    @Test
    fun `observatory cloud resumes interrupted focus step after clearing`() = runTest {
        val fixture = newFixture(cardCount = 2)
        val expectedGame = fixture.buildObservatory(MiniGameDifficulty.Apprentice)
        val target = expectedGame.targets.first()
        val viewModel = fixture.newViewModel()
        advanceUntilIdle()

        viewModel.openObservatory()
        viewModel.selectObservatoryDifficulty(MiniGameDifficulty.Apprentice)
        advanceUntilIdle()
        openObservatoryDome(viewModel)
        alignObservatory(viewModel, target)

        val afterAlignment = viewModel.uiState.value.screen as MiniGamesScreenUiState.ObservatoryPlaying
        assertEquals(ObservatoryStep.Focus, afterAlignment.step)

        advanceObservatoryCloudCycle()

        val cloudPause = viewModel.uiState.value.screen as MiniGamesScreenUiState.ObservatoryPlaying
        assertEquals(ObservatoryStep.ClearCloud, cloudPause.step)

        viewModel.scrubObservatoryCloud(1f)

        val resumed = viewModel.uiState.value.screen as MiniGamesScreenUiState.ObservatoryPlaying
        assertEquals(ObservatoryStep.Focus, resumed.step)
        assertEquals(0f, resumed.cloudProgress, 0.001f)
    }

    @Test
    fun `observatory cloud keeps cycling after target change`() = runTest {
        val fixture = newFixture(
            cardCount = 3,
            observatoryUnlockedDifficulty = MiniGameDifficulty.Observer,
        )
        val expectedGame = fixture.buildObservatory(MiniGameDifficulty.Observer)
        val firstTarget = expectedGame.targets.first()
        val viewModel = fixture.newViewModel()
        advanceUntilIdle()

        viewModel.openObservatory()
        viewModel.selectObservatoryDifficulty(MiniGameDifficulty.Observer)
        advanceUntilIdle()
        openObservatoryDome(viewModel)
        alignObservatory(viewModel, firstTarget)
        focusObservatory(viewModel, firstTarget)
        mashObservatoryCapture(viewModel)
        advanceObservatoryCaptureValidation()

        val nextTarget = viewModel.uiState.value.screen as MiniGamesScreenUiState.ObservatoryPlaying
        assertEquals(1, nextTarget.targetIndex)
        assertEquals(ObservatoryStep.Align, nextTarget.step)

        advanceObservatoryCloudCycle()

        val cloudPause = viewModel.uiState.value.screen as MiniGamesScreenUiState.ObservatoryPlaying
        assertEquals(1, cloudPause.targetIndex)
        assertEquals(ObservatoryStep.ClearCloud, cloudPause.step)
        assertEquals(1f, cloudPause.cloudProgress, 0.001f)
    }

    @Test
    fun `observatory focus waits for slider release before capture`() = runTest {
        val fixture = newFixture(cardCount = 2)
        val expectedGame = fixture.buildObservatory(MiniGameDifficulty.Apprentice)
        val target = expectedGame.targets.first()
        val focusInTolerance = target.focus + valueOffsetInsideTolerance(
            targetValue = target.focus,
            tolerance = expectedGame.tolerance,
        )
        val viewModel = fixture.newViewModel()
        advanceUntilIdle()

        viewModel.openObservatory()
        viewModel.selectObservatoryDifficulty(MiniGameDifficulty.Apprentice)
        advanceUntilIdle()
        openObservatoryDome(viewModel)
        alignObservatory(viewModel, target)
        val afterAlignment = viewModel.uiState.value.screen as MiniGamesScreenUiState.ObservatoryPlaying
        if (afterAlignment.step == ObservatoryStep.ClearCloud) {
            viewModel.clearObservatoryCloud()
        }

        viewModel.setObservatoryFocus(focusInTolerance)

        val afterFocusDrag = viewModel.uiState.value.screen as MiniGamesScreenUiState.ObservatoryPlaying
        assertEquals(ObservatoryStep.Focus, afterFocusDrag.step)
        assertTrue(afterFocusDrag.focusReady)
        assertEquals(focusInTolerance, afterFocusDrag.focus, 0.001f)

        viewModel.validateObservatoryFocus()

        val afterFocusRelease = viewModel.uiState.value.screen as MiniGamesScreenUiState.ObservatoryPlaying
        assertEquals(ObservatoryStep.Capture, afterFocusRelease.step)
        assertEquals(focusInTolerance, afterFocusRelease.focus, 0.001f)
    }

    @Test
    fun `observatory keeps dome open and reticle position for next target`() = runTest {
        val fixture = newFixture(
            cardCount = 3,
            observatoryUnlockedDifficulty = MiniGameDifficulty.Observer,
        )
        val expectedGame = fixture.buildObservatory(MiniGameDifficulty.Observer)
        val firstTarget = expectedGame.targets[0]
        val secondTarget = expectedGame.targets[1]
        val viewModel = fixture.newViewModel()
        advanceUntilIdle()

        viewModel.openObservatory()
        viewModel.selectObservatoryDifficulty(MiniGameDifficulty.Observer)
        advanceUntilIdle()
        openObservatoryDome(viewModel)
        alignObservatory(viewModel, firstTarget)
        val afterAlignment = viewModel.uiState.value.screen as MiniGamesScreenUiState.ObservatoryPlaying
        if (afterAlignment.step == ObservatoryStep.ClearCloud) {
            viewModel.clearObservatoryCloud()
        }
        focusObservatory(viewModel, firstTarget)
        mashObservatoryCapture(viewModel)
        advanceObservatoryCaptureValidation()

        val nextTarget = viewModel.uiState.value.screen as MiniGamesScreenUiState.ObservatoryPlaying
        assertEquals(1, nextTarget.targetIndex)
        assertEquals(ObservatoryStep.Align, nextTarget.step)
        assertEquals(1f, nextTarget.domeProgress, 0.001f)
        assertEquals(firstTarget.azimuth, nextTarget.azimuth, 0.001f)
        assertEquals(firstTarget.altitude, nextTarget.altitude, 0.001f)
        assertEquals(0f, nextTarget.focus, 0.001f)
        assertEquals(secondTarget.azimuth, nextTarget.targetAzimuth, 0.001f)
        assertEquals(secondTarget.altitude, nextTarget.targetAltitude, 0.001f)
    }

    @Test
    fun `observatory capture button increases capture progress by twenty percent`() = runTest {
        val fixture = newFixture(cardCount = 2)
        val expectedGame = fixture.buildObservatory(MiniGameDifficulty.Apprentice)
        val target = expectedGame.targets.first()
        val viewModel = fixture.newViewModel()
        advanceUntilIdle()

        viewModel.openObservatory()
        viewModel.selectObservatoryDifficulty(MiniGameDifficulty.Apprentice)
        advanceUntilIdle()
        advanceObservatoryToCapture(viewModel, target)

        viewModel.captureObservatoryTarget()

        val afterPress = viewModel.uiState.value.screen as MiniGamesScreenUiState.ObservatoryPlaying
        assertEquals(ObservatoryStep.Capture, afterPress.step)
        assertEquals(0.20f, afterPress.captureProgress, 0.001f)

        viewModel.backToMenu()
        advanceUntilIdle()
    }

    @Test
    fun `observatory capture progress decays before one tenth of a second`() = runTest {
        val fixture = newFixture(cardCount = 2)
        val expectedGame = fixture.buildObservatory(MiniGameDifficulty.Apprentice)
        val target = expectedGame.targets.first()
        val viewModel = fixture.newViewModel()
        advanceUntilIdle()

        viewModel.openObservatory()
        viewModel.selectObservatoryDifficulty(MiniGameDifficulty.Apprentice)
        advanceUntilIdle()
        advanceObservatoryToCapture(viewModel, target)
        viewModel.captureObservatoryTarget()

        advanceTimeBy(50L)

        val afterShortDelay = viewModel.uiState.value.screen as MiniGamesScreenUiState.ObservatoryPlaying
        assertTrue(afterShortDelay.captureProgress < 0.20f)
        assertTrue(afterShortDelay.captureProgress > 0f)

        viewModel.backToMenu()
        advanceUntilIdle()
    }

    @Test
    fun `observatory capture progress decays faster on harder difficulty`() = runTest {
        val apprenticeFixture = newFixture(cardCount = 2)
        val explorerFixture = newFixture(
            cardCount = 4,
            observatoryUnlockedDifficulty = MiniGameDifficulty.Explorer,
        )
        val apprenticeGame = apprenticeFixture.buildObservatory(MiniGameDifficulty.Apprentice)
        val explorerGame = explorerFixture.buildObservatory(MiniGameDifficulty.Explorer)
        val apprenticeViewModel = apprenticeFixture.newViewModel()
        val explorerViewModel = explorerFixture.newViewModel()
        advanceUntilIdle()

        apprenticeViewModel.openObservatory()
        apprenticeViewModel.selectObservatoryDifficulty(MiniGameDifficulty.Apprentice)
        explorerViewModel.openObservatory()
        explorerViewModel.selectObservatoryDifficulty(MiniGameDifficulty.Explorer)
        advanceUntilIdle()
        advanceObservatoryToCapture(apprenticeViewModel, apprenticeGame.targets.first())
        advanceObservatoryToCapture(explorerViewModel, explorerGame.targets.first())
        apprenticeViewModel.captureObservatoryTarget()
        explorerViewModel.captureObservatoryTarget()

        advanceTimeBy(300L)

        val apprenticeCapture = apprenticeViewModel.uiState.value.screen as MiniGamesScreenUiState.ObservatoryPlaying
        val explorerCapture = explorerViewModel.uiState.value.screen as MiniGamesScreenUiState.ObservatoryPlaying
        assertTrue(explorerCapture.captureProgress < apprenticeCapture.captureProgress)

        apprenticeViewModel.backToMenu()
        explorerViewModel.backToMenu()
        advanceUntilIdle()
    }

    @Test
    fun `observatory closes dome after final target before granting reward`() = runTest {
        val fixture = newFixture(cardCount = 2)
        val expectedGame = fixture.buildObservatory(MiniGameDifficulty.Apprentice)
        val target = expectedGame.targets.first()
        val viewModel = fixture.newViewModel()
        advanceUntilIdle()

        viewModel.openObservatory()
        viewModel.selectObservatoryDifficulty(MiniGameDifficulty.Apprentice)
        advanceUntilIdle()
        openObservatoryDome(viewModel)
        alignObservatory(viewModel, target)
        val afterAlignment = viewModel.uiState.value.screen as MiniGamesScreenUiState.ObservatoryPlaying
        if (afterAlignment.step == ObservatoryStep.ClearCloud) {
            viewModel.clearObservatoryCloud()
        }
        focusObservatory(viewModel, target)
        mashObservatoryCapture(viewModel)

        val validating = viewModel.uiState.value.screen as MiniGamesScreenUiState.ObservatoryPlaying
        val dailyStateBeforeClose = fixture.progressGateway.progress.miniGamesProgress
            .dailyStateFor(MiniGameId.Observatory, "2026-05-10")
        assertEquals(ObservatoryStep.Capture, validating.step)
        assertEquals(1f, validating.captureProgress, 0.001f)
        assertTrue(!validating.canCapture)
        assertNull(dailyStateBeforeClose.reward)

        advanceObservatoryCaptureValidation()

        val closing = viewModel.uiState.value.screen as MiniGamesScreenUiState.ObservatoryPlaying
        assertEquals(ObservatoryStep.CloseDome, closing.step)
        assertEquals(1f, closing.domeProgress, 0.001f)
        assertTrue(!closing.domeClosed)

        closeObservatoryDome(viewModel)
        advanceUntilIdle()

        val dailyStateAfterClose = fixture.progressGateway.progress.miniGamesProgress
            .dailyStateFor(MiniGameId.Observatory, "2026-05-10")
        assertTrue(viewModel.uiState.value.screen is MiniGamesScreenUiState.ObservatoryResult)
        assertEquals(MiniGameReward.fromMinutes(15L), dailyStateAfterClose.reward)
    }

    @Test
    fun `leaving observatory after difficulty consumes attempt without reward`() = runTest {
        val fixture = newFixture(cardCount = 2)
        val viewModel = fixture.newViewModel()
        advanceUntilIdle()

        viewModel.openObservatory()
        viewModel.selectObservatoryDifficulty(MiniGameDifficulty.Apprentice)
        advanceUntilIdle()
        viewModel.backToMenu()
        advanceUntilIdle()

        val dailyState = fixture.progressGateway.progress.miniGamesProgress
            .dailyStateFor(MiniGameId.Observatory, "2026-05-10")
        assertTrue(dailyState.hasPlayed)
        assertNull(dailyState.reward)
    }

    @Test
    fun `completing apprentice observatory grants reward and unlocks observer`() = runTest {
        val fixture = newFixture(cardCount = 2)
        val expectedGame = fixture.buildObservatory(MiniGameDifficulty.Apprentice)
        val viewModel = fixture.newViewModel()
        advanceUntilIdle()

        viewModel.openObservatory()
        viewModel.selectObservatoryDifficulty(MiniGameDifficulty.Apprentice)
        advanceUntilIdle()
        completeObservatory(viewModel, expectedGame)
        advanceUntilIdle()

        val progress = fixture.progressGateway.progress.miniGamesProgress
        val dailyState = progress.dailyStateFor(MiniGameId.Observatory, "2026-05-10")
        val result = viewModel.uiState.value.screen as MiniGamesScreenUiState.ObservatoryResult
        assertEquals(MiniGameReward.fromMinutes(15L), dailyState.reward)
        assertEquals("15min", result.rewardLabel)
        assertEquals(MiniGameDifficulty.Observer, progress.unlockedDifficultyFor(MiniGameId.Observatory))
    }

    @Test
    fun `selecting memory difficulty consumes attempt without reward`() = runTest {
        val fixture = newFixture(cardCount = 2)
        val viewModel = fixture.newViewModel()
        advanceUntilIdle()

        viewModel.openMemory()
        viewModel.selectMemoryDifficulty(MiniGameDifficulty.Apprentice)
        advanceUntilIdle()

        val dailyState = fixture.progressGateway.progress.miniGamesProgress
            .dailyStateFor(MiniGameId.Memory, "2026-05-10")
        val playing = viewModel.uiState.value.screen as MiniGamesScreenUiState.MemoryPlaying
        assertTrue(dailyState.hasPlayed)
        assertNull(dailyState.reward)
        assertEquals(0, playing.moves)
        assertEquals(0, playing.currentStreak)
        assertEquals(0, playing.bestStreak)
        assertNull(playing.feedbackEvent)
    }

    @Test
    fun `selecting quiz difficulty consumes attempt without reward`() = runTest {
        val fixture = newQuizFixture()
        val viewModel = fixture.newViewModel()
        advanceUntilIdle()

        viewModel.openQuiz()
        advanceUntilIdle()
        viewModel.selectQuizDifficulty(MiniGameDifficulty.Apprentice)
        advanceUntilIdle()

        val dailyState = fixture.progressGateway.progress.miniGamesProgress
            .dailyStateFor(MiniGameId.Quiz, "2026-05-10")
        val playing = viewModel.uiState.value.screen as MiniGamesScreenUiState.QuizPlaying
        assertTrue(dailyState.hasPlayed)
        assertNull(dailyState.reward)
        assertEquals(0, playing.score)
        assertEquals(1, playing.questionCount)
    }

    @Test
    fun `quiz remains playable when catalog cannot provide distractors`() = runTest {
        val card = testCardDefinition(
            id = "ALP-001",
            extensionId = "alpha",
            name = "Carte unique",
        )
        val fixture = newFixture(
            cardCount = 1,
            quizUnlockedDifficulty = MiniGameDifficulty.Explorer,
            cardDefinitions = listOf(card),
            variants = mapOf(card.id to listOf(OwnedVariantCount("city", "standard", 1))),
        )
        val viewModel = fixture.newViewModel()
        advanceUntilIdle()

        viewModel.openQuiz()
        advanceUntilIdle()
        viewModel.selectQuizDifficulty(MiniGameDifficulty.Explorer)
        advanceUntilIdle()

        val playing = viewModel.uiState.value.screen as MiniGamesScreenUiState.QuizPlaying
        assertEquals(4, playing.questionCount)
        assertEquals(4, playing.answers.size)
        assertTrue(playing.answers.map { it.text }.toSet().size == 4)
    }

    @Test
    fun `completing apprentice memory grants reward and unlocks observer`() = runTest {
        val fixture = newFixture(cardCount = 2)
        val viewModel = fixture.newViewModel()
        advanceUntilIdle()

        viewModel.openMemory()
        viewModel.selectMemoryDifficulty(MiniGameDifficulty.Apprentice)
        advanceUntilIdle()
        completeVisiblePairs(viewModel)
        advanceUntilIdle()

        val progress = fixture.progressGateway.progress.miniGamesProgress
        val dailyState = progress.dailyStateFor(MiniGameId.Memory, "2026-05-10")
        assertTrue(viewModel.uiState.value.screen is MiniGamesScreenUiState.MemoryResult)
        assertEquals(MiniGameReward.fromMinutes(15L), dailyState.reward)
        assertEquals(MiniGameDifficulty.Observer, progress.unlockedDifficultyFor(MiniGameId.Memory))
    }

    @Test
    fun `wrong quiz answer is corrected immediately`() = runTest {
        val fixture = newQuizFixture()
        val expectedQuiz = fixture.buildQuiz(MiniGameDifficulty.Apprentice)
        val viewModel = fixture.newViewModel()
        advanceUntilIdle()

        viewModel.openQuiz()
        advanceUntilIdle()
        viewModel.selectQuizDifficulty(MiniGameDifficulty.Apprentice)
        advanceUntilIdle()
        val correctAnswer = expectedQuiz.questions.first().correctAnswer
        val wrongAnswer = expectedQuiz.questions.first().answers.first { it != correctAnswer }

        selectQuizAnswer(viewModel, wrongAnswer)

        val updated = viewModel.uiState.value.screen as MiniGamesScreenUiState.QuizPlaying
        assertEquals(0, updated.score)
        assertTrue(updated.canAdvance)
        assertEquals(MiniGameFeedbackTone.Error, updated.feedbackEvent?.tone)
        assertEquals(QuizAnswerState.SelectedWrong, updated.answers.single { it.text == wrongAnswer }.state)
        assertEquals(QuizAnswerState.Correct, updated.answers.single { it.text == correctAnswer }.state)
    }

    @Test
    fun `partial quiz score grants proportional reward in seconds`() = runTest {
        val fixture = newQuizFixture(quizUnlockedDifficulty = MiniGameDifficulty.Observer)
        val expectedQuiz = fixture.buildQuiz(MiniGameDifficulty.Observer)
        val viewModel = fixture.newViewModel()
        advanceUntilIdle()

        viewModel.openQuiz()
        advanceUntilIdle()
        viewModel.selectQuizDifficulty(MiniGameDifficulty.Observer)
        advanceUntilIdle()
        selectQuizAnswer(viewModel, expectedQuiz.questions[0].correctAnswer)
        viewModel.continueQuiz()
        val wrongAnswer = expectedQuiz.questions[1].answers.first { it != expectedQuiz.questions[1].correctAnswer }
        selectQuizAnswer(viewModel, wrongAnswer)
        viewModel.continueQuiz()
        advanceUntilIdle()

        val progress = fixture.progressGateway.progress.miniGamesProgress
        val dailyState = progress.dailyStateFor(MiniGameId.Quiz, "2026-05-10")
        val result = viewModel.uiState.value.screen as MiniGamesScreenUiState.QuizResult
        assertEquals(MiniGameReward.fromSeconds(1_350L), dailyState.reward)
        assertEquals("1/2", result.scoreLabel)
        assertEquals(MiniGameDifficulty.Observer, progress.unlockedDifficultyFor(MiniGameId.Quiz))
    }

    @Test
    fun `perfect apprentice quiz grants reward and unlocks observer`() = runTest {
        val fixture = newQuizFixture()
        val expectedQuiz = fixture.buildQuiz(MiniGameDifficulty.Apprentice)
        val viewModel = fixture.newViewModel()
        advanceUntilIdle()

        viewModel.openQuiz()
        advanceUntilIdle()
        viewModel.selectQuizDifficulty(MiniGameDifficulty.Apprentice)
        advanceUntilIdle()
        selectQuizAnswer(viewModel, expectedQuiz.questions[0].correctAnswer)
        viewModel.continueQuiz()
        advanceUntilIdle()

        val progress = fixture.progressGateway.progress.miniGamesProgress
        val dailyState = progress.dailyStateFor(MiniGameId.Quiz, "2026-05-10")
        assertTrue(viewModel.uiState.value.screen is MiniGamesScreenUiState.QuizResult)
        assertEquals(MiniGameReward.fromMinutes(15L), dailyState.reward)
        assertEquals(MiniGameDifficulty.Observer, progress.unlockedDifficultyFor(MiniGameId.Quiz))
        val result = viewModel.uiState.value.screen as MiniGamesScreenUiState.QuizResult
        assertEquals(expectedQuiz.questions[0].explanation, result.corrections.single().explanation)
    }

    @Test
    fun `leaving quiz after difficulty consumes attempt without reward`() = runTest {
        val fixture = newQuizFixture()
        val viewModel = fixture.newViewModel()
        advanceUntilIdle()

        viewModel.openQuiz()
        advanceUntilIdle()
        viewModel.selectQuizDifficulty(MiniGameDifficulty.Apprentice)
        advanceUntilIdle()
        viewModel.backToMenu()
        advanceUntilIdle()

        val dailyState = fixture.progressGateway.progress.miniGamesProgress
            .dailyStateFor(MiniGameId.Quiz, "2026-05-10")
        assertTrue(dailyState.hasPlayed)
        assertNull(dailyState.reward)
    }

    @Test
    fun `hole cell is ignored when selected first`() = runTest {
        val fixture = newFixture(
            cardCount = 4,
            unlockedDifficulty = MiniGameDifficulty.Observer,
            variants = mapOf(
                "ALP-001" to listOf(OwnedVariantCount("city", "standard", 1)),
                "ALP-002" to listOf(OwnedVariantCount("city", "standard", 1)),
                "ALP-003" to listOf(OwnedVariantCount("city", "standard", 1)),
                "ALP-004" to listOf(OwnedVariantCount("city", "standard", 1)),
            ),
        )
        val viewModel = fixture.newViewModel()
        advanceUntilIdle()

        viewModel.openMemory()
        viewModel.selectMemoryDifficulty(MiniGameDifficulty.Observer)
        advanceUntilIdle()
        val playing = viewModel.uiState.value.screen as MiniGamesScreenUiState.MemoryPlaying
        val holeIndex = playing.cells.single { it.isHole }.index

        viewModel.selectMemoryCell(holeIndex)
        advanceUntilIdle()

        val updated = viewModel.uiState.value.screen as MiniGamesScreenUiState.MemoryPlaying
        assertEquals(MemoryCellState.Hole, updated.cells.single { it.index == holeIndex }.state)
        assertEquals(0, updated.moves)
        assertEquals(0, updated.currentStreak)
        assertEquals(0, updated.bestStreak)
        assertNull(updated.feedbackEvent)
    }

    @Test
    fun `hole cell is ignored after a revealed card`() = runTest {
        val fixture = newFixture(
            cardCount = 4,
            unlockedDifficulty = MiniGameDifficulty.Observer,
            variants = mapOf(
                "ALP-001" to listOf(OwnedVariantCount("city", "standard", 1)),
                "ALP-002" to listOf(OwnedVariantCount("city", "standard", 1)),
                "ALP-003" to listOf(OwnedVariantCount("city", "standard", 1)),
                "ALP-004" to listOf(OwnedVariantCount("city", "standard", 1)),
            ),
        )
        val viewModel = fixture.newViewModel()
        advanceUntilIdle()

        viewModel.openMemory()
        viewModel.selectMemoryDifficulty(MiniGameDifficulty.Observer)
        advanceUntilIdle()
        val playing = viewModel.uiState.value.screen as MiniGamesScreenUiState.MemoryPlaying
        val holeIndex = playing.cells.single { it.isHole }.index
        val firstPairCell = playing.cells.first { it.face?.role == MemoryCardRole.Pair }

        viewModel.selectMemoryCell(firstPairCell.index)
        viewModel.selectMemoryCell(holeIndex)
        advanceUntilIdle()

        val updated = viewModel.uiState.value.screen as MiniGamesScreenUiState.MemoryPlaying
        assertEquals(MemoryCellState.Revealed, updated.cells.single { it.index == firstPairCell.index }.state)
        assertEquals(MemoryCellState.Hole, updated.cells.single { it.index == holeIndex }.state)
        assertEquals(0, updated.moves)
        assertEquals(0, updated.currentStreak)
        assertEquals(0, updated.bestStreak)
        assertNull(updated.feedbackEvent)
    }

    @Test
    fun `matching a pair updates streak and emits success feedback`() = runTest {
        val fixture = newFixture(
            cardCount = 5,
            unlockedDifficulty = MiniGameDifficulty.Observer,
        )
        val viewModel = fixture.newViewModel()
        advanceUntilIdle()

        viewModel.openMemory()
        viewModel.selectMemoryDifficulty(MiniGameDifficulty.Observer)
        advanceUntilIdle()
        val playing = viewModel.uiState.value.screen as MiniGamesScreenUiState.MemoryPlaying
        val pair = playing.cells
            .filter { it.face?.role == MemoryCardRole.Pair }
            .groupBy { requireNotNull(it.face).identity.key }
            .values
            .first { it.size == 2 }

        viewModel.selectMemoryCell(pair[0].index)
        viewModel.selectMemoryCell(pair[1].index)

        val updated = viewModel.uiState.value.screen as MiniGamesScreenUiState.MemoryPlaying
        assertEquals(1, updated.moves)
        assertEquals(1, updated.currentStreak)
        assertEquals(1, updated.bestStreak)
        assertEquals(MiniGameFeedbackTone.Success, updated.feedbackEvent?.tone)
        assertEquals(setOf(pair[0].index, pair[1].index), updated.feedbackEvent?.sourceIndexes)
    }

    @Test
    fun `last match emits completion feedback before result screen`() = runTest {
        val fixture = newFixture(cardCount = 2)
        val viewModel = fixture.newViewModel()
        advanceUntilIdle()

        viewModel.openMemory()
        viewModel.selectMemoryDifficulty(MiniGameDifficulty.Apprentice)
        advanceUntilIdle()
        val playing = viewModel.uiState.value.screen as MiniGamesScreenUiState.MemoryPlaying
        val pairGroups = playing.cells
            .filter { it.face?.role == MemoryCardRole.Pair }
            .groupBy { requireNotNull(it.face).identity.key }
            .values
            .toList()
        pairGroups.dropLast(1).forEach { pair ->
            viewModel.selectMemoryCell(pair[0].index)
            viewModel.selectMemoryCell(pair[1].index)
        }
        val lastPair = pairGroups.last()

        viewModel.selectMemoryCell(lastPair[0].index)
        viewModel.selectMemoryCell(lastPair[1].index)

        val completing = viewModel.uiState.value.screen as MiniGamesScreenUiState.MemoryPlaying
        assertEquals(pairGroups.size, completing.moves)
        assertEquals(MiniGameFeedbackTone.Completion, completing.feedbackEvent?.tone)
        assertTrue(completing.inputLocked)

        advanceUntilIdle()
        val result = viewModel.uiState.value.screen as MiniGamesScreenUiState.MemoryResult
        assertEquals(MiniGameFeedbackTone.Completion, result.feedbackEvent?.tone)
    }

    private fun completeVisiblePairs(viewModel: MiniGamesViewModel) {
        val playing = viewModel.uiState.value.screen as MiniGamesScreenUiState.MemoryPlaying
        val pairGroups = playing.cells
            .filter { it.face?.role == MemoryCardRole.Pair }
            .groupBy { requireNotNull(it.face).identity.key }
            .values
        pairGroups.forEach { pair ->
            require(pair.size == 2)
            viewModel.selectMemoryCell(pair[0].index)
            viewModel.selectMemoryCell(pair[1].index)
        }
    }

    private fun TestScope.completeObservatory(
        viewModel: MiniGamesViewModel,
        game: ObservatoryGame,
    ) {
        game.targets.forEach { target ->
            val beforeTarget = viewModel.uiState.value.screen as MiniGamesScreenUiState.ObservatoryPlaying
            if (beforeTarget.step == ObservatoryStep.OpenDome) {
                openObservatoryDome(viewModel)
            }
            alignObservatory(viewModel, target)
            val afterAlignment = viewModel.uiState.value.screen as MiniGamesScreenUiState.ObservatoryPlaying
            if (afterAlignment.step == ObservatoryStep.ClearCloud) {
                viewModel.clearObservatoryCloud()
            }
            focusObservatory(viewModel, target)
            mashObservatoryCapture(viewModel)
            advanceObservatoryCaptureValidation()
        }
        val afterTargets = viewModel.uiState.value.screen as MiniGamesScreenUiState.ObservatoryPlaying
        if (afterTargets.step == ObservatoryStep.CloseDome) {
            closeObservatoryDome(viewModel)
            advanceUntilIdle()
        }
    }

    private fun advanceObservatoryToCapture(
        viewModel: MiniGamesViewModel,
        target: ObservatoryTarget,
    ) {
        val beforeTarget = viewModel.uiState.value.screen as MiniGamesScreenUiState.ObservatoryPlaying
        if (beforeTarget.step == ObservatoryStep.OpenDome) {
            openObservatoryDome(viewModel)
        }
        alignObservatory(viewModel, target)
        val afterAlignment = viewModel.uiState.value.screen as MiniGamesScreenUiState.ObservatoryPlaying
        if (afterAlignment.step == ObservatoryStep.ClearCloud) {
            viewModel.clearObservatoryCloud()
        }
        focusObservatory(viewModel, target)
    }

    private fun openObservatoryDome(viewModel: MiniGamesViewModel) {
        viewModel.setObservatoryDomeProgress(1f)
        viewModel.validateObservatoryDomeProgress()
    }

    private fun closeObservatoryDome(viewModel: MiniGamesViewModel) {
        viewModel.setObservatoryDomeProgress(0f)
        viewModel.validateObservatoryDomeProgress()
    }

    private fun alignObservatory(
        viewModel: MiniGamesViewModel,
        target: ObservatoryTarget,
    ) {
        viewModel.setObservatoryAzimuth(target.azimuth)
        viewModel.setObservatoryAltitude(target.altitude)
        viewModel.validateObservatoryAlignment()
    }

    private fun focusObservatory(
        viewModel: MiniGamesViewModel,
        target: ObservatoryTarget,
    ) {
        viewModel.setObservatoryFocus(target.focus)
        viewModel.validateObservatoryFocus()
    }

    private fun valueOffsetInsideTolerance(
        targetValue: Float,
        tolerance: Float,
    ): Float {
        val offset = tolerance / 2f
        return if (targetValue <= 0.5f) offset else -offset
    }

    private fun mashObservatoryCapture(viewModel: MiniGamesViewModel) {
        repeat(5) {
            viewModel.captureObservatoryTarget()
        }
    }

    private fun TestScope.advanceObservatoryCaptureValidation() {
        advanceTimeBy(720L)
        runCurrent()
    }

    private fun TestScope.advanceObservatoryCloudCycle() {
        advanceTimeBy(
            ObservatoryCloudInterCycleWaitMaxMillis +
                ObservatoryCloudAccumulationDurationMillis +
                ObservatoryCloudAccumulationTickMillis,
        )
        runCurrent()
    }

    private fun selectQuizAnswer(
        viewModel: MiniGamesViewModel,
        answer: String,
    ) {
        val playing = viewModel.uiState.value.screen as MiniGamesScreenUiState.QuizPlaying
        val index = playing.answers.single { it.text == answer }.index
        viewModel.selectQuizAnswer(index)
    }

    private fun newQuizFixture(
        quizUnlockedDifficulty: MiniGameDifficulty = MiniGameDifficulty.Apprentice,
    ): Fixture {
        val cards = quizCards()
        return newFixture(
            cardCount = cards.size,
            quizUnlockedDifficulty = quizUnlockedDifficulty,
            cardDefinitions = cards,
            variants = cards.associate { card ->
                card.id to listOf(OwnedVariantCount("city", "standard", 1))
            },
        )
    }

    private fun newTimelineFixture(): Fixture {
        val cards = timelineCards()
        return newFixture(
            cardCount = cards.size,
            cardDefinitions = cards,
            variants = cards.associate { card ->
                card.id to listOf(OwnedVariantCount("city", "standard", 1))
            },
        )
    }

    private fun newFixture(
        cardCount: Int,
        unlockedDifficulty: MiniGameDifficulty = MiniGameDifficulty.Apprentice,
        quizUnlockedDifficulty: MiniGameDifficulty = MiniGameDifficulty.Apprentice,
        timelineUnlockedDifficulty: MiniGameDifficulty = MiniGameDifficulty.Apprentice,
        observatoryUnlockedDifficulty: MiniGameDifficulty = MiniGameDifficulty.Apprentice,
        cardDefinitions: List<CardDefinition> = (1..cardCount).map { index ->
            testCardDefinition(
                id = "ALP-${index.toString().padStart(3, '0')}",
                extensionId = "alpha",
                name = "Carte $index",
            )
        },
        variants: Map<String, List<OwnedVariantCount>> = (1..cardCount).associate { index ->
            "ALP-${index.toString().padStart(3, '0')}" to listOf(
                OwnedVariantCount("city", "standard", 1),
            )
        },
    ): Fixture {
        val progressGateway = FakeProgressGateway().apply {
            trustedNow = now
            progress = StandaloneProgress(
                collection = OwnedCollection(
                    cards = variants.mapValues { (_, ownedVariants) ->
                        OwnedCardEntry(
                            totalOwned = ownedVariants.sumOf { it.count },
                            variants = ownedVariants,
                        )
                    },
                ),
                rechargeState = PackRechargeState(
                    availableDrawCount = 0,
                    accumulatedChargeUnits = 0L,
                    lastChargeEvaluationAt = now.toString(),
                ),
                miniGamesProgress = MiniGamesProgress(
                    unlockedDifficulties = mapOf(
                        MiniGameId.Memory to unlockedDifficulty,
                        MiniGameId.Quiz to quizUnlockedDifficulty,
                        MiniGameId.Timeline to timelineUnlockedDifficulty,
                        MiniGameId.Observatory to observatoryUnlockedDifficulty,
                    ),
                ),
            )
        }
        val catalogGateway = FakeCatalogGateway().apply {
            extensions = listOf(
                ExtensionDefinition(
                    id = "alpha",
                    name = "Alpha",
                    coverImageRef = "cover",
                ),
            )
            cards = cardDefinitions
            variantProfiles = testVariantProfiles()
        }
        val repository = MiniGamesRepository(
            progressRepository = progressGateway,
            catalogRepository = catalogGateway,
            settings = testGameSettings(now = now),
        )
        return Fixture(
            progressGateway = progressGateway,
            catalogGateway = catalogGateway,
            repository = repository,
        )
    }

    private data class Fixture(
        val progressGateway: FakeProgressGateway,
        val catalogGateway: FakeCatalogGateway,
        val repository: MiniGamesRepository,
    ) {
        fun newViewModel(): MiniGamesViewModel = MiniGamesViewModel(
            miniGamesRepository = repository,
            catalogRepository = catalogGateway,
            progressRepository = progressGateway,
        )

        suspend fun buildQuiz(difficulty: MiniGameDifficulty): QuizGame {
            val state = repository.loadMiniGamesState()
            val resolvedCards = repository.prepareResolvedCardsForToday(
                miniGameId = MiniGameId.Quiz,
                slotCount = 1,
            )
            val result = buildQuizGame(
                difficulty = difficulty,
                dateUtc = state.todayUtc,
                resolvedCards = resolvedCards,
                cards = catalogGateway.cards,
                extensions = catalogGateway.extensions,
                variantProfiles = catalogGateway.variantProfiles,
            )
            return when (result) {
                is QuizGameBuildResult.Ready -> result.game
                is QuizGameBuildResult.Unavailable -> error(result.message)
            }
        }

        suspend fun buildTimeline(difficulty: MiniGameDifficulty): TimelineGame {
            val state = repository.loadMiniGamesState()
            val criterion = selectPlayableTimelineCriterion(
                dateUtc = state.todayUtc,
                cards = catalogGateway.cards,
                ownedCardIds = progressGateway.progress.collection.cards.keys,
            ) ?: error("Expected playable timeline criterion")
            val eligibleCardIds = eligibleTimelineCardIds(
                criterion = criterion,
                cards = catalogGateway.cards,
            )
            val resolvedCards = repository.prepareResolvedCardsForToday(
                miniGameId = MiniGameId.Timeline,
                slotCount = timelineResolvedCardCountForDifficulty(difficulty),
                eligibleCardIds = eligibleCardIds,
                distinctOwnedCards = true,
            )
            val result = buildTimelineGame(
                difficulty = difficulty,
                criterion = criterion,
                dateUtc = state.todayUtc,
                resolvedCards = resolvedCards,
                cards = catalogGateway.cards,
                extensions = catalogGateway.extensions,
                variantProfiles = catalogGateway.variantProfiles,
            )
            return when (result) {
                is TimelineGameBuildResult.Ready -> result.game
                is TimelineGameBuildResult.Unavailable -> error(result.message)
            }
        }

        suspend fun buildObservatory(difficulty: MiniGameDifficulty): ObservatoryGame {
            val state = repository.loadMiniGamesState()
            val resolvedCards = repository.prepareResolvedCardsForToday(
                miniGameId = MiniGameId.Observatory,
                slotCount = difficulty.level,
                distinctOwnedCards = true,
            )
            val result = buildObservatoryGame(
                difficulty = difficulty,
                dateUtc = state.todayUtc,
                resolvedCards = resolvedCards,
                cards = catalogGateway.cards,
                extensions = catalogGateway.extensions,
                variantProfiles = catalogGateway.variantProfiles,
            )
            return when (result) {
                is ObservatoryGameBuildResult.Ready -> result.game
                is ObservatoryGameBuildResult.Unavailable -> error(result.message)
            }
        }
    }

    private fun quizCards(): List<CardDefinition> {
        val objectTypes = listOf(
            "Nébuleuse",
            "Galaxie",
            "Étoile",
            "Constellation",
            "Amas ouvert",
            "Planète",
        )
        val constellations = listOf("Orion", "Cygne", "Lyre", "Aigle", "Andromède", "Taureau")
        val seasons = listOf("Hiver", "Été", "Printemps", "Automne", "Toute l'année", "Fin d'été")
        val catalogs = listOf("Messier", "Bayer", "NGC", "IAU", "Caldwell", "Système solaire")
        return objectTypes.indices.map { index ->
            val id = "ALP-${(index + 1).toString().padStart(3, '0')}"
            val base = testCardDefinition(
                id = id,
                extensionId = "alpha",
                name = "Carte ${index + 1}",
            )
            base.copy(
                astronomy = base.astronomy.copy(
                    objectTypeLabel = objectTypes[index],
                    constellation = constellations[index],
                    mainSeason = seasons[index],
                    primaryCatalogName = catalogs[index],
                    catalogNumber = "REF-${index + 1}",
                ),
            )
        }
    }

    private fun timelineCards(): List<CardDefinition> {
        val deepSkyCards = (1..5).map { index ->
            timelineDeepSkyCard(
                id = "DS-${index.toString().padStart(3, '0')}",
                distance = 100.0 * index,
                realSize = 10.0 * index,
                visualWidth = index.toDouble(),
                visualHeight = 1.0,
                absoluteMagnitude = -10.0 + index,
            )
        }
        val solarSystemCards = (1..5).map { index ->
            timelineSolarSystemCard(
                id = "SOL-${index.toString().padStart(3, '0')}",
                diameter = 1_000.0 * index,
            )
        }
        return deepSkyCards + solarSystemCards
    }

    private fun timelineDeepSkyCard(
        id: String,
        distance: Double,
        realSize: Double = 10.0,
        visualWidth: Double = 1.0,
        visualHeight: Double = 1.0,
        absoluteMagnitude: Double = -1.0,
    ): CardDefinition {
        val base = testCardDefinition(
            id = id,
            extensionId = "alpha",
            name = id,
        )
        return base.copy(
            astronomy = base.astronomy.copy(
                objectFamily = "deep_sky",
                details = DeepSkyDetails(
                    distance = LightYearMeasurement(distance, "$distance années-lumière"),
                    realSize = LightYearMeasurement(realSize, "$realSize années-lumière"),
                    visualSize = VisualSize(
                        fullMoonWidth = visualWidth,
                        fullMoonHeight = visualHeight,
                        angularWidth = AngularMeasurement(0, 30, 0, "0°30′00″"),
                        angularHeight = AngularMeasurement(0, 30, 0, "0°30′00″"),
                        label = "$visualWidth × $visualHeight",
                    ),
                    absoluteMagnitude = AbsoluteMagnitudeMeasurement(
                        value = absoluteMagnitude,
                        label = absoluteMagnitude.toString(),
                    ),
                ),
            ),
        )
    }

    private fun timelineSolarSystemCard(
        id: String,
        diameter: Double,
    ): CardDefinition {
        val base = testCardDefinition(
            id = id,
            extensionId = "alpha",
            name = id,
        )
        return base.copy(
            astronomy = base.astronomy.copy(
                objectFamily = "solar_system",
                details = SolarSystemDetails(
                    realSize = LightYearMeasurement(diameter, "$diameter km"),
                ),
            ),
        )
    }
}
