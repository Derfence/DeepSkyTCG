package fr.aumombelli.dstcg

import androidx.compose.ui.geometry.Rect
import fr.aumombelli.dstcg.app.AppSceneUiState
import fr.aumombelli.dstcg.app.NewPlayerCoachmarkSpec
import fr.aumombelli.dstcg.app.NewPlayerOnboardingCoordinator
import fr.aumombelli.dstcg.app.NewPlayerOnboardingTarget
import fr.aumombelli.dstcg.model.NewPlayerOnboardingStep
import fr.aumombelli.dstcg.model.OwnedCollection
import fr.aumombelli.dstcg.model.StandaloneProgress
import fr.aumombelli.dstcg.ui.motion.AppScene
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NewPlayerOnboardingCoordinatorTest {
    @Test
    fun `coordinator persists the first journey sequence`() = runTest {
        val progressGateway = FakeProgressGateway().apply {
            progress = StandaloneProgress(
                collection = OwnedCollection(),
                availableDrawCount = 10,
                nextChargeAt = null,
                openedPackCount = 0,
                newPlayerOnboardingStep = NewPlayerOnboardingStep.OpenFirstPackMenu,
            )
        }
        val coordinator = NewPlayerOnboardingCoordinator(progressGateway)

        coordinator.syncFromProgress()
        coordinator.onHomeOpenPackSelected()
        coordinator.onExtensionSelected()
        val shouldDeferBadgeCelebration = coordinator.onFirstPackOpened()
        val shouldResumeBadgeCelebration = coordinator.onLibraryOpened()
        coordinator.onBadgeBookOpened()

        assertTrue(shouldDeferBadgeCelebration)
        assertTrue(shouldResumeBadgeCelebration)
        assertEquals(NewPlayerOnboardingStep.Completed, coordinator.uiState.currentStep)
        assertEquals(NewPlayerOnboardingStep.Completed, progressGateway.progress.newPlayerOnboardingStep)
        assertFalse(coordinator.uiState.libraryCardHintVisible)
    }

    @Test
    fun `library visit enables local hint and badges coachmark after step advance`() = runTest {
        val progressGateway = FakeProgressGateway().apply {
            progress = StandaloneProgress(
                collection = ownedCollectionOf("ALP-001" to 1),
                availableDrawCount = 9,
                nextChargeAt = null,
                openedPackCount = 1,
                newPlayerOnboardingStep = NewPlayerOnboardingStep.ViewLibrary,
            )
        }
        val coordinator = NewPlayerOnboardingCoordinator(progressGateway)

        coordinator.syncFromProgress()
        val shouldResumeBadgeCelebration = coordinator.onLibraryOpened()

        assertTrue(shouldResumeBadgeCelebration)
        assertEquals(NewPlayerOnboardingStep.ViewBadges, coordinator.uiState.currentStep)
        assertTrue(coordinator.uiState.libraryCardHintVisible)

        coordinator.onLibraryCardHintConsumed()

        assertFalse(coordinator.uiState.libraryCardHintVisible)
    }

    @Test
    fun `badge coachmark waits until badge celebration is no longer visible`() = runTest {
        val progressGateway = FakeProgressGateway().apply {
            progress = StandaloneProgress(
                collection = ownedCollectionOf("ALP-001" to 1),
                availableDrawCount = 9,
                nextChargeAt = null,
                openedPackCount = 1,
                newPlayerOnboardingStep = NewPlayerOnboardingStep.ViewBadges,
            )
        }
        val coordinator = NewPlayerOnboardingCoordinator(progressGateway)
        val sceneState = AppSceneUiState(
            currentScene = AppScene.Home,
            homeContentVisible = true,
            coachmarkTargetBounds = mapOf(
                NewPlayerOnboardingTarget.HomeBadges to Rect(10f, 20f, 120f, 72f),
            ),
        )

        coordinator.syncFromProgress()

        val hiddenCoachmark = coordinator.activeCoachmark(
            currentScene = AppScene.Home,
            sceneState = sceneState,
            badgeCelebrationVisible = true,
        )
        val visibleCoachmark = coordinator.activeCoachmark(
            currentScene = AppScene.Home,
            sceneState = sceneState,
            badgeCelebrationVisible = false,
        )

        assertNull(hiddenCoachmark)
        assertEquals(
            NewPlayerCoachmarkSpec(
                target = NewPlayerOnboardingTarget.HomeBadges,
                title = "Badges d'astronome",
                message = "Ton premier badge t'attend ici !",
            ),
            visibleCoachmark,
        )
    }

    @Test
    fun `coachmark waits until onboarding hints are visible after transitions`() = runTest {
        val progressGateway = FakeProgressGateway().apply {
            progress = StandaloneProgress(
                collection = OwnedCollection(),
                availableDrawCount = 10,
                nextChargeAt = null,
                openedPackCount = 0,
                newPlayerOnboardingStep = NewPlayerOnboardingStep.OpenFirstPackMenu,
            )
        }
        val coordinator = NewPlayerOnboardingCoordinator(progressGateway)
        val hiddenSceneState = AppSceneUiState(
            currentScene = AppScene.Home,
            homeContentVisible = true,
            onboardingHintsVisible = false,
            coachmarkTargetBounds = mapOf(
                NewPlayerOnboardingTarget.HomeOpenPack to Rect(10f, 20f, 120f, 72f),
            ),
        )
        val visibleSceneState = hiddenSceneState.copy(onboardingHintsVisible = true)

        coordinator.syncFromProgress()

        val hiddenCoachmark = coordinator.activeCoachmark(
            currentScene = AppScene.Home,
            sceneState = hiddenSceneState,
            badgeCelebrationVisible = false,
        )
        val visibleCoachmark = coordinator.activeCoachmark(
            currentScene = AppScene.Home,
            sceneState = visibleSceneState,
            badgeCelebrationVisible = false,
        )

        assertNull(hiddenCoachmark)
        assertEquals(
            NewPlayerCoachmarkSpec(
                target = NewPlayerOnboardingTarget.HomeOpenPack,
                title = "Premières cartes",
                message = "Commençons ta collection de cartes d'objets célestes !",
            ),
            visibleCoachmark,
        )
    }
}
