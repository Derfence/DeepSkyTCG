package fr.aumombelli.dstcg

import fr.aumombelli.dstcg.data.MiniGamesRepository
import fr.aumombelli.dstcg.feature.minigames.MemoryCardRole
import fr.aumombelli.dstcg.feature.minigames.MemoryCellState
import fr.aumombelli.dstcg.feature.minigames.MiniGameFeedbackTone
import fr.aumombelli.dstcg.feature.minigames.MiniGamesScreenUiState
import fr.aumombelli.dstcg.feature.minigames.MiniGamesViewModel
import fr.aumombelli.dstcg.model.ExtensionDefinition
import fr.aumombelli.dstcg.model.MiniGameDifficulty
import fr.aumombelli.dstcg.model.MiniGameId
import fr.aumombelli.dstcg.model.MiniGameReward
import fr.aumombelli.dstcg.model.MiniGamesProgress
import fr.aumombelli.dstcg.model.OwnedCardEntry
import fr.aumombelli.dstcg.model.OwnedCollection
import fr.aumombelli.dstcg.model.OwnedVariantCount
import fr.aumombelli.dstcg.model.PackRechargeState
import fr.aumombelli.dstcg.model.StandaloneProgress
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
        assertEquals(MiniGameReward(reductionMinutes = 15L), dailyState.reward)
        assertEquals(MiniGameDifficulty.Observer, progress.unlockedDifficultyFor(MiniGameId.Memory))
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

    private fun newFixture(
        cardCount: Int,
        unlockedDifficulty: MiniGameDifficulty = MiniGameDifficulty.Apprentice,
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
                    unlockedDifficulties = mapOf(MiniGameId.Memory to unlockedDifficulty),
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
            cards = (1..cardCount).map { index ->
                testCardDefinition(
                    id = "ALP-${index.toString().padStart(3, '0')}",
                    extensionId = "alpha",
                    name = "Carte $index",
                )
            }
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
    }
}
