package fr.aumombelli.dstcg

import fr.aumombelli.dstcg.data.MiniGamesRepository
import fr.aumombelli.dstcg.feature.minigames.MemoryCardRole
import fr.aumombelli.dstcg.feature.minigames.MemoryCellState
import fr.aumombelli.dstcg.feature.minigames.MiniGameFeedbackTone
import fr.aumombelli.dstcg.feature.minigames.MiniGamesScreenUiState
import fr.aumombelli.dstcg.feature.minigames.MiniGamesViewModel
import fr.aumombelli.dstcg.feature.minigames.QuizAnswerState
import fr.aumombelli.dstcg.feature.minigames.QuizGame
import fr.aumombelli.dstcg.feature.minigames.QuizGameBuildResult
import fr.aumombelli.dstcg.feature.minigames.TimelineGame
import fr.aumombelli.dstcg.feature.minigames.TimelineGameBuildResult
import fr.aumombelli.dstcg.feature.minigames.TimelinePreferredCardCount
import fr.aumombelli.dstcg.feature.minigames.buildQuizGame
import fr.aumombelli.dstcg.feature.minigames.buildTimelineGame
import fr.aumombelli.dstcg.feature.minigames.eligibleTimelineCardIds
import fr.aumombelli.dstcg.feature.minigames.selectTimelineCriterion
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
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
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
    fun `opening timeline consumes attempt without reward`() = runTest {
        val fixture = newTimelineFixture()
        val viewModel = fixture.newViewModel()
        advanceUntilIdle()

        viewModel.openTimeline()
        advanceUntilIdle()

        val dailyState = fixture.progressGateway.progress.miniGamesProgress
            .dailyStateFor(MiniGameId.Timeline, "2026-05-10")
        val playing = viewModel.uiState.value.screen as MiniGamesScreenUiState.TimelinePlaying
        assertTrue(dailyState.hasPlayed)
        assertNull(dailyState.reward)
        assertEquals(playing.slots.size, playing.handCards.size)
        assertTrue(!playing.canValidate)
    }

    @Test
    fun `leaving timeline after launch consumes attempt without reward`() = runTest {
        val fixture = newTimelineFixture()
        val viewModel = fixture.newViewModel()
        advanceUntilIdle()

        viewModel.openTimeline()
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
    fun `perfect timeline grants one hour reward`() = runTest {
        val fixture = newTimelineFixture()
        val viewModel = fixture.newViewModel()
        advanceUntilIdle()

        viewModel.openTimeline()
        advanceUntilIdle()
        val timeline = fixture.buildTimeline()
        timeline.correctOrder.forEachIndexed { index, card ->
            viewModel.placeTimelineCard(card.id, index)
        }
        val ready = viewModel.uiState.value.screen as MiniGamesScreenUiState.TimelinePlaying
        assertTrue(ready.canValidate)

        viewModel.validateTimeline()
        advanceUntilIdle()

        val progress = fixture.progressGateway.progress.miniGamesProgress
        val dailyState = progress.dailyStateFor(MiniGameId.Timeline, "2026-05-10")
        val result = viewModel.uiState.value.screen as MiniGamesScreenUiState.TimelineResult
        assertEquals(MiniGameReward.fromMinutes(60L), dailyState.reward)
        assertEquals("${timeline.correctOrder.size}/${timeline.correctOrder.size}", result.scoreLabel)
        assertEquals("1h", result.rewardLabel)
        assertTrue(!result.showCorrectOrder)
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
    fun `holographic singleton selected first is immediately matched`() = runTest {
        val fixture = newFixture(
            cardCount = 5,
            unlockedDifficulty = MiniGameDifficulty.Observer,
            variants = mapOf(
                "ALP-001" to listOf(OwnedVariantCount("city", "standard", 1)),
                "ALP-002" to listOf(OwnedVariantCount("city", "standard", 1)),
                "ALP-003" to listOf(OwnedVariantCount("city", "standard", 1)),
                "ALP-004" to listOf(OwnedVariantCount("city", "standard", 1)),
                "ALP-005" to listOf(OwnedVariantCount("holographic", "standard", 1)),
            ),
        )
        val viewModel = fixture.newViewModel()
        advanceUntilIdle()

        viewModel.openMemory()
        viewModel.selectMemoryDifficulty(MiniGameDifficulty.Observer)
        advanceUntilIdle()
        val playing = viewModel.uiState.value.screen as MiniGamesScreenUiState.MemoryPlaying
        val singletonIndex = playing.cells.single { it.face.role == MemoryCardRole.HolographicSingleton }.index

        viewModel.selectMemoryCell(singletonIndex)
        advanceUntilIdle()

        val updated = viewModel.uiState.value.screen as MiniGamesScreenUiState.MemoryPlaying
        assertEquals(MemoryCellState.Matched, updated.cells.single { it.index == singletonIndex }.state)
        assertEquals(1, updated.moves)
        assertEquals(1, updated.currentStreak)
        assertEquals(1, updated.bestStreak)
        assertEquals(MiniGameFeedbackTone.Special, updated.feedbackEvent?.tone)
        assertEquals(setOf(singletonIndex), updated.feedbackEvent?.sourceIndexes)
    }

    @Test
    fun `holographic singleton selected second is a mismatch`() = runTest {
        val fixture = newFixture(
            cardCount = 5,
            unlockedDifficulty = MiniGameDifficulty.Observer,
            variants = mapOf(
                "ALP-001" to listOf(OwnedVariantCount("city", "standard", 1)),
                "ALP-002" to listOf(OwnedVariantCount("city", "standard", 1)),
                "ALP-003" to listOf(OwnedVariantCount("city", "standard", 1)),
                "ALP-004" to listOf(OwnedVariantCount("city", "standard", 1)),
                "ALP-005" to listOf(OwnedVariantCount("holographic", "standard", 1)),
            ),
        )
        val viewModel = fixture.newViewModel()
        advanceUntilIdle()

        viewModel.openMemory()
        viewModel.selectMemoryDifficulty(MiniGameDifficulty.Observer)
        advanceUntilIdle()
        val playing = viewModel.uiState.value.screen as MiniGamesScreenUiState.MemoryPlaying
        val singletonIndex = playing.cells.single { it.face.role == MemoryCardRole.HolographicSingleton }.index
        val firstPairCell = playing.cells.first { it.face.role == MemoryCardRole.Pair }

        viewModel.selectMemoryCell(firstPairCell.index)
        viewModel.selectMemoryCell(singletonIndex)

        val mismatch = viewModel.uiState.value.screen as MiniGamesScreenUiState.MemoryPlaying
        assertEquals(MemoryCellState.Mismatch, mismatch.cells.single { it.index == firstPairCell.index }.state)
        assertEquals(MemoryCellState.Mismatch, mismatch.cells.single { it.index == singletonIndex }.state)
        assertEquals(1, mismatch.moves)
        assertEquals(0, mismatch.currentStreak)
        assertEquals(0, mismatch.bestStreak)
        assertEquals(MiniGameFeedbackTone.Error, mismatch.feedbackEvent?.tone)
        assertEquals(setOf(firstPairCell.index, singletonIndex), mismatch.feedbackEvent?.sourceIndexes)

        advanceTimeBy(650L)
        advanceUntilIdle()

        val reset = viewModel.uiState.value.screen as MiniGamesScreenUiState.MemoryPlaying
        assertEquals(MemoryCellState.Hidden, reset.cells.single { it.index == firstPairCell.index }.state)
        assertEquals(MemoryCellState.Hidden, reset.cells.single { it.index == singletonIndex }.state)
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
            .filter { it.face.role == MemoryCardRole.Pair }
            .groupBy { it.face.identity.key }
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
            .filter { it.face.role == MemoryCardRole.Pair }
            .groupBy { it.face.identity.key }
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
            .filter { it.face.role == MemoryCardRole.Pair }
            .groupBy { it.face.identity.key }
            .values
        pairGroups.forEach { pair ->
            require(pair.size == 2)
            viewModel.selectMemoryCell(pair[0].index)
            viewModel.selectMemoryCell(pair[1].index)
        }
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

        suspend fun buildTimeline(): TimelineGame {
            val state = repository.loadMiniGamesState()
            val criterion = selectTimelineCriterion(state.todayUtc)
            val eligibleCardIds = eligibleTimelineCardIds(
                criterion = criterion,
                cards = catalogGateway.cards,
            )
            val resolvedCards = repository.prepareResolvedCardsForToday(
                miniGameId = MiniGameId.Timeline,
                slotCount = TimelinePreferredCardCount,
                eligibleCardIds = eligibleCardIds,
                distinctOwnedCards = true,
            )
            val result = buildTimelineGame(
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
