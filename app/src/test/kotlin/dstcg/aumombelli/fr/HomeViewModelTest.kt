package fr.aumombelli.dstcg

import fr.aumombelli.dstcg.app.NewPlayerOnboardingCoordinator
import fr.aumombelli.dstcg.data.ProgressGateway
import fr.aumombelli.dstcg.data.ProgressLoadResult
import fr.aumombelli.dstcg.feature.home.HomeViewModel
import fr.aumombelli.dstcg.model.CraftingCardCandidate
import fr.aumombelli.dstcg.model.CraftingMode
import fr.aumombelli.dstcg.model.DisplayCardVariant
import fr.aumombelli.dstcg.model.HomeMenuNoveltyState
import fr.aumombelli.dstcg.model.NewPlayerOnboardingStep
import fr.aumombelli.dstcg.model.StandaloneProgress
import fr.aumombelli.dstcg.model.OwnedEquipmentCardEntry
import fr.aumombelli.dstcg.model.OwnedEquipmentInventory
import java.time.Instant
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `init loads local progress and clears loading state`() = runTest {
        val progressGateway = FakeProgressGateway().apply {
            progress = progress.copy(collection = ownedCollectionOf("ALP-001" to 1))
        }

        val viewModel = HomeViewModel(progressGateway)
        advanceUntilIdle()

        assertEquals(1, progressGateway.loadCallCount.get())
        assertEquals(false, viewModel.uiState.value.isLoading)
        assertNull(viewModel.uiState.value.errorMessage)
        assertFalse(viewModel.uiState.value.isLibraryMenuVisible)
        assertFalse(viewModel.uiState.value.isEquipmentMenuVisible)
        assertFalse(viewModel.uiState.value.isBadgeBookMenuVisible)
        assertFalse(viewModel.uiState.value.isCraftingMenuAvailable)
    }

    @Test
    fun `reset is ignored while loading`() = runTest {
        val progressGateway = FakeProgressGateway()
        val viewModel = HomeViewModel(progressGateway)

        viewModel.resetProgress()
        advanceUntilIdle()

        assertEquals(0, progressGateway.resetCallCount.get())
        assertEquals(false, viewModel.uiState.value.isLoading)
    }

    @Test
    fun `init surfaces progress loading failure`() = runTest {
        val progressGateway = FakeProgressGateway().apply {
            loadFailure = IllegalStateException("Saved progression could not be read.")
        }

        val viewModel = HomeViewModel(progressGateway)
        advanceUntilIdle()

        assertEquals(false, viewModel.uiState.value.isLoading)
        assertEquals("Saved progression could not be read.", viewModel.uiState.value.errorMessage)
    }

    @Test
    fun `init surfaces recovered progress silently`() = runTest {
        val progressGateway = FakeProgressGateway().apply {
            recoveryNotice = "La progression locale a été sécurisée."
        }

        val viewModel = HomeViewModel(progressGateway)
        advanceUntilIdle()

        assertEquals(false, viewModel.uiState.value.isLoading)
        assertNull(viewModel.uiState.value.errorMessage)
    }

    @Test
    fun `equipment menu becomes visible after first equipment is stored`() = runTest {
        val progressGateway = FakeProgressGateway().apply {
            progress = progress.copy(
                equipmentInventory = OwnedEquipmentInventory(
                    cards = mapOf(
                        "observatory-1" to OwnedEquipmentCardEntry(countOwned = 1),
                    ),
                ),
            )
        }

        val viewModel = HomeViewModel(progressGateway)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isEquipmentMenuVisible)
    }

    @Test
    fun `library and badge menus become visible after the first opened pack`() = runTest {
        val progressGateway = FakeProgressGateway().apply {
            progress = progress.copy(openedPackCount = 1)
        }

        val viewModel = HomeViewModel(progressGateway)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isLibraryMenuVisible)
        assertTrue(viewModel.uiState.value.isBadgeBookMenuVisible)
    }

    @Test
    fun `crafting menu unlocks from the third pack when darken sky candidate exists`() = runTest {
        val progressGateway = FakeProgressGateway().apply {
            progress = progress.copy(
                openedPackCount = 3,
                newPlayerOnboardingStep = NewPlayerOnboardingStep.AwaitCraftingEligibility,
                collection = ownedCollectionOf("ALP-001" to 2),
            )
        }
        val craftingGateway = FakeCraftingGateway().apply {
            candidatesByMode = mapOf(CraftingMode.DarkenSky to listOf(testDarkenSkyCandidate()))
        }

        val viewModel = HomeViewModel(progressGateway, craftingGateway)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isCraftingMenuAvailable)
    }

    @Test
    fun `crafting menu stays available during crafting walkthrough mini games discovery and conclusion`() = runTest {
        val progressGateway = FakeProgressGateway().apply {
            progress = progress.copy(
                openedPackCount = 3,
                newPlayerOnboardingStep = NewPlayerOnboardingStep.LearnCraftingTools,
            )
        }

        val viewModel = HomeViewModel(progressGateway)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isCraftingMenuAvailable)

        progressGateway.progress = progressGateway.progress.copy(
            newPlayerOnboardingStep = NewPlayerOnboardingStep.DiscoverMiniGames,
        )

        viewModel.refresh()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isCraftingMenuAvailable)

        progressGateway.progress = progressGateway.progress.copy(
            newPlayerOnboardingStep = NewPlayerOnboardingStep.ShowConclusion,
        )

        viewModel.refresh()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isCraftingMenuAvailable)
    }

    @Test
    fun `crafting menu stays locked before third pack even with darken sky candidate`() = runTest {
        val progressGateway = FakeProgressGateway().apply {
            progress = progress.copy(
                openedPackCount = 2,
                newPlayerOnboardingStep = NewPlayerOnboardingStep.AwaitCraftingEligibility,
                collection = ownedCollectionOf("ALP-001" to 2),
            )
        }
        val craftingGateway = FakeCraftingGateway().apply {
            candidatesByMode = mapOf(CraftingMode.DarkenSky to listOf(testDarkenSkyCandidate()))
        }

        val viewModel = HomeViewModel(progressGateway, craftingGateway)
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isCraftingMenuAvailable)
    }

    @Test
    fun `init exposes persisted home menu novelty flags`() = runTest {
        val progressGateway = FakeProgressGateway().apply {
            progress = progress.copy(
                miniGamesMenuUnlocked = true,
                homeMenuNoveltyState = HomeMenuNoveltyState(
                    library = true,
                    equipment = true,
                    badgeBook = true,
                    miniGames = true,
                ),
                equipmentInventory = OwnedEquipmentInventory(
                    cards = mapOf(
                        "observatory-1" to OwnedEquipmentCardEntry(countOwned = 1),
                    ),
                ),
            )
        }

        val viewModel = HomeViewModel(progressGateway)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.showLibraryNewIndicator)
        assertTrue(viewModel.uiState.value.showEquipmentNewIndicator)
        assertTrue(viewModel.uiState.value.showBadgeBookNewIndicator)
        assertTrue(viewModel.uiState.value.showMiniGamesNewIndicator)
        assertTrue(viewModel.uiState.value.isMiniGamesMenuVisible)
    }

    @Test
    fun `mini games menu is visible only after persisted unlock`() = runTest {
        val progressGateway = FakeProgressGateway()

        val viewModel = HomeViewModel(progressGateway)
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isMiniGamesMenuVisible)

        progressGateway.progress = progressGateway.progress.copy(miniGamesMenuUnlocked = true)
        viewModel.refresh()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isMiniGamesMenuVisible)
    }

    @Test
    fun `mark library seen only clears library novelty`() = runTest {
        val progressGateway = FakeProgressGateway().apply {
            progress = progress.copy(
                homeMenuNoveltyState = HomeMenuNoveltyState(
                    library = true,
                    equipment = true,
                    badgeBook = true,
                ),
                equipmentInventory = OwnedEquipmentInventory(
                    cards = mapOf(
                        "observatory-1" to OwnedEquipmentCardEntry(countOwned = 1),
                    ),
                ),
            )
        }

        val viewModel = HomeViewModel(progressGateway)
        advanceUntilIdle()

        viewModel.markLibrarySeen()
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.showLibraryNewIndicator)
        assertTrue(viewModel.uiState.value.showEquipmentNewIndicator)
        assertTrue(viewModel.uiState.value.showBadgeBookNewIndicator)
        assertEquals(
            HomeMenuNoveltyState(
                library = false,
                equipment = true,
                badgeBook = true,
            ),
            progressGateway.progress.homeMenuNoveltyState,
        )
    }

    @Test
    fun `mark seen is idempotent when the indicator is already false`() = runTest {
        val progressGateway = FakeProgressGateway().apply {
            progress = progress.copy(
                homeMenuNoveltyState = HomeMenuNoveltyState(
                    equipment = true,
                ),
            )
        }

        val viewModel = HomeViewModel(progressGateway)
        advanceUntilIdle()

        viewModel.markLibrarySeen()
        advanceUntilIdle()

        assertEquals(0, progressGateway.savedProgress.size)
        assertFalse(viewModel.uiState.value.showLibraryNewIndicator)
        assertTrue(viewModel.uiState.value.showEquipmentNewIndicator)
    }

    @Test
    fun `mark mini games seen only clears mini games novelty`() = runTest {
        val progressGateway = FakeProgressGateway().apply {
            progress = progress.copy(
                miniGamesMenuUnlocked = true,
                homeMenuNoveltyState = HomeMenuNoveltyState(
                    library = true,
                    miniGames = true,
                ),
            )
        }

        val viewModel = HomeViewModel(progressGateway)
        advanceUntilIdle()

        viewModel.markMiniGamesSeen()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.showLibraryNewIndicator)
        assertFalse(viewModel.uiState.value.showMiniGamesNewIndicator)
        assertEquals(
            HomeMenuNoveltyState(
                library = true,
                miniGames = false,
            ),
            progressGateway.progress.homeMenuNoveltyState,
        )
    }

    @Test
    fun `opening badge book keeps the second pack onboarding step while consuming novelty`() = runTest {
        val progressGateway = AtomicProgressGateway(
            initialProgress = StandaloneProgress(
                collection = ownedCollectionOf("ALP-001" to 1),
                rechargeState = testRechargeState(availableDrawCount = 9),
                openedPackCount = 1,
                newPlayerOnboardingStep = NewPlayerOnboardingStep.ViewBadges,
                homeMenuNoveltyState = HomeMenuNoveltyState(
                    badgeBook = true,
                ),
            ),
        )
        val viewModel = HomeViewModel(progressGateway)
        val coordinator = NewPlayerOnboardingCoordinator(progressGateway)

        advanceUntilIdle()
        coordinator.syncFromProgress()

        viewModel.markBadgeBookSeen()
        coordinator.onBadgeBookOpened()
        advanceUntilIdle()

        assertEquals(NewPlayerOnboardingStep.OpenSecondPackMenu, progressGateway.progress.newPlayerOnboardingStep)
        assertFalse(progressGateway.progress.homeMenuNoveltyState.badgeBook)
        assertFalse(viewModel.uiState.value.showBadgeBookNewIndicator)
    }

    @Test
    fun `reset is allowed even when an error is present`() = runTest {
        val progressGateway = FakeProgressGateway().apply {
            loadFailure = IllegalStateException("Saved progression could not be read.")
        }

        val viewModel = HomeViewModel(progressGateway)
        advanceUntilIdle()

        viewModel.resetProgress()
        advanceUntilIdle()

        assertEquals(1, progressGateway.resetCallCount.get())
        assertEquals("Saved progression could not be read.", viewModel.uiState.value.errorMessage)
    }

    @Test
    fun `healthy progress can be reset and reloads clean state`() = runTest {
        val progressGateway = FakeProgressGateway().apply {
            progress = progress.copy(collection = ownedCollectionOf("ALP-001" to 3))
        }

        val viewModel = HomeViewModel(progressGateway)
        advanceUntilIdle()

        viewModel.resetProgress()
        advanceUntilIdle()

        assertEquals(1, progressGateway.resetCallCount.get())
        assertNull(viewModel.uiState.value.errorMessage)
        assertEquals(false, viewModel.uiState.value.isLoading)
    }

    @Test
    fun `compromised progress can be reset and reloads clean state`() = runTest {
        val progressGateway = FakeProgressGateway().apply {
            compromisedMessage = "La progression locale semble corrompue."
        }

        val viewModel = HomeViewModel(progressGateway)
        advanceUntilIdle()

        viewModel.resetProgress()
        advanceUntilIdle()

        assertEquals(1, progressGateway.resetCallCount.get())
        assertNull(viewModel.uiState.value.errorMessage)
    }

    private class AtomicProgressGateway(
        initialProgress: StandaloneProgress,
    ) : ProgressGateway {
        var progress: StandaloneProgress = initialProgress
        private val trustedNow: Instant = Instant.parse("2026-03-24T12:00:00Z")

        override suspend fun loadProgress(): ProgressLoadResult = ProgressLoadResult.Ok(
            progress = progress,
            trustedNow = trustedNow,
        )

        override suspend fun saveProgress(progress: StandaloneProgress) {
            error("This regression test requires atomic progress updates.")
        }

        override suspend fun updateProgress(transform: (StandaloneProgress) -> StandaloneProgress) {
            progress = transform(progress)
        }

        override suspend fun resetProgress() {
            progress = StandaloneProgress(
                collection = ownedCollectionOf(),
                rechargeState = testRechargeState(),
            )
        }
    }

    private fun testDarkenSkyCandidate(): CraftingCardCandidate =
        CraftingCardCandidate(
            card = testCardDefinition("ALP-001"),
            extensionName = "Astronomes en herbe",
            mode = CraftingMode.DarkenSky,
            sourceVariant = DisplayCardVariant(
                skyQuality = "city",
                skyQualityLabel = "Ville",
                finish = "standard",
                finishLabel = "Standard",
                isHolographic = false,
                count = 2,
            ),
            targetVariant = DisplayCardVariant(
                skyQuality = "suburban",
                skyQualityLabel = "Periurbain",
                finish = "standard",
                finishLabel = "Standard",
                isHolographic = false,
                count = 0,
            ),
            consumedCount = 2,
        )
}
