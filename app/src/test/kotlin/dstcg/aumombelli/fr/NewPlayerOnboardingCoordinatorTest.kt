package fr.aumombelli.dstcg

import androidx.compose.ui.geometry.Rect
import fr.aumombelli.dstcg.app.AppSceneUiState
import fr.aumombelli.dstcg.app.NewPlayerBlockingModalKind
import fr.aumombelli.dstcg.app.NewPlayerBlockingModalSpec
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
    fun `coordinator persists the full onboarding sequence through equipment activation`() = runTest {
        val progressGateway = FakeProgressGateway().apply {
            progress = StandaloneProgress(
                collection = OwnedCollection(),
                rechargeState = testRechargeState(),
                openedPackCount = 0,
                newPlayerOnboardingStep = NewPlayerOnboardingStep.ShowWelcomeIntro,
            )
        }
        val coordinator = NewPlayerOnboardingCoordinator(progressGateway)

        coordinator.syncFromProgress()
        coordinator.onWelcomeIntroAcknowledged()
        coordinator.onHomeOpenPackSelected()
        coordinator.onExtensionSelected()
        val shouldDeferBadgeCelebration = coordinator.onPackOpened()
        val shouldResumeBadgeCelebrationOnOpen = coordinator.onLibraryOpened()
        val shouldResumeBadgeCelebrationOnCompletion = coordinator.onLibraryVariantWalkthroughCompleted()
        coordinator.onBadgeBookOpened()
        val shouldDeferEquipmentChapterBadges = coordinator.onPackOpened()
        coordinator.onEquipmentOpened()
        coordinator.onEquipmentActivated()

        assertTrue(shouldDeferBadgeCelebration)
        assertFalse(shouldResumeBadgeCelebrationOnOpen)
        assertTrue(shouldResumeBadgeCelebrationOnCompletion)
        assertTrue(shouldDeferEquipmentChapterBadges)
        assertEquals(NewPlayerOnboardingStep.Completed, coordinator.uiState.currentStep)
        assertEquals(NewPlayerOnboardingStep.Completed, progressGateway.progress.newPlayerOnboardingStep)
        assertFalse(coordinator.uiState.libraryCardHintVisible)
    }

    @Test
    fun `library walkthrough completion enables local hint and badges coachmark after step advance`() = runTest {
        val progressGateway = FakeProgressGateway().apply {
            progress = StandaloneProgress(
                collection = ownedCollectionOf("ALP-001" to 1),
                rechargeState = testRechargeState(availableDrawCount = 9),
                openedPackCount = 1,
                newPlayerOnboardingStep = NewPlayerOnboardingStep.ViewLibrary,
            )
        }
        val coordinator = NewPlayerOnboardingCoordinator(progressGateway)

        coordinator.syncFromProgress()
        val shouldResumeBadgeCelebrationOnOpen = coordinator.onLibraryOpened()

        assertFalse(shouldResumeBadgeCelebrationOnOpen)
        assertEquals(NewPlayerOnboardingStep.LearnLibraryVariants, coordinator.uiState.currentStep)
        assertFalse(coordinator.uiState.libraryCardHintVisible)

        val shouldResumeBadgeCelebrationOnCompletion = coordinator.onLibraryVariantWalkthroughCompleted()

        assertTrue(shouldResumeBadgeCelebrationOnCompletion)
        assertEquals(NewPlayerOnboardingStep.ViewBadges, coordinator.uiState.currentStep)
        assertTrue(coordinator.uiState.libraryCardHintVisible)

        coordinator.onLibraryCardHintConsumed()

        assertFalse(coordinator.uiState.libraryCardHintVisible)
    }

    @Test
    fun `welcome intro stays active until acknowledged and becomes blocking modal on home`() = runTest {
        val progressGateway = FakeProgressGateway().apply {
            progress = StandaloneProgress(
                collection = OwnedCollection(),
                rechargeState = testRechargeState(),
                openedPackCount = 0,
                newPlayerOnboardingStep = NewPlayerOnboardingStep.ShowWelcomeIntro,
            )
        }
        val coordinator = NewPlayerOnboardingCoordinator(progressGateway)
        val sceneState = AppSceneUiState(
            currentScene = AppScene.Home,
            homeContentVisible = true,
            onboardingHintsVisible = true,
        )

        coordinator.syncFromProgress()

        assertEquals(
            NewPlayerBlockingModalSpec(NewPlayerBlockingModalKind.WelcomeIntro),
            coordinator.activeBlockingModal(
                currentScene = AppScene.Home,
                sceneState = sceneState,
            ),
        )
        assertNull(
            coordinator.activeCoachmark(
                currentScene = AppScene.Home,
                sceneState = sceneState,
                badgeCelebrationVisible = false,
            ),
        )

        coordinator.onWelcomeIntroAcknowledged()

        assertEquals(NewPlayerOnboardingStep.OpenFirstPackMenu, coordinator.uiState.currentStep)
        assertEquals(NewPlayerOnboardingStep.OpenFirstPackMenu, progressGateway.progress.newPlayerOnboardingStep)
        assertNull(
            coordinator.activeBlockingModal(
                currentScene = AppScene.Home,
                sceneState = sceneState,
            ),
        )
    }

    @Test
    fun `badge coachmark waits until badge celebration is no longer visible`() = runTest {
        val progressGateway = FakeProgressGateway().apply {
            progress = StandaloneProgress(
                collection = ownedCollectionOf("ALP-001" to 1),
                rechargeState = testRechargeState(availableDrawCount = 9),
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
    fun `badge book visit opens second pack chapter and equipment activation completes onboarding`() = runTest {
        val progressGateway = FakeProgressGateway().apply {
            progress = StandaloneProgress(
                collection = ownedCollectionOf("ALP-001" to 1),
                rechargeState = testRechargeState(availableDrawCount = 9),
                openedPackCount = 1,
                newPlayerOnboardingStep = NewPlayerOnboardingStep.ViewBadges,
            )
        }
        val coordinator = NewPlayerOnboardingCoordinator(progressGateway)

        coordinator.syncFromProgress()
        coordinator.onBadgeBookOpened()

        assertEquals(NewPlayerOnboardingStep.OpenSecondPackMenu, coordinator.uiState.currentStep)

        val shouldDeferBadgeCelebration = coordinator.onPackOpened()

        assertTrue(shouldDeferBadgeCelebration)
        assertEquals(NewPlayerOnboardingStep.ViewEquipmentMenu, coordinator.uiState.currentStep)

        coordinator.onEquipmentOpened()
        assertEquals(NewPlayerOnboardingStep.ActivateFirstEquipment, coordinator.uiState.currentStep)

        coordinator.onEquipmentActivated()
        assertEquals(NewPlayerOnboardingStep.Completed, coordinator.uiState.currentStep)
        assertEquals(NewPlayerOnboardingStep.Completed, progressGateway.progress.newPlayerOnboardingStep)
    }

    @Test
    fun `open second pack chapter stays silent on home and equipment coachmarks still appear afterwards`() = runTest {
        val progressGateway = FakeProgressGateway().apply {
            progress = StandaloneProgress(
                collection = ownedCollectionOf("ALP-001" to 1),
                rechargeState = testRechargeState(availableDrawCount = 9),
                openedPackCount = 1,
                newPlayerOnboardingStep = NewPlayerOnboardingStep.OpenSecondPackMenu,
            )
        }
        val coordinator = NewPlayerOnboardingCoordinator(progressGateway)
        val homeSceneState = AppSceneUiState(
            currentScene = AppScene.Home,
            homeContentVisible = true,
            coachmarkTargetBounds = mapOf(
                NewPlayerOnboardingTarget.HomeOpenPack to Rect(10f, 20f, 120f, 72f),
            ),
        )
        val equipmentSceneState = AppSceneUiState(
            currentScene = AppScene.Equipment,
            equipmentContentVisible = true,
            coachmarkTargetBounds = mapOf(
                NewPlayerOnboardingTarget.EquipmentActivation to Rect(20f, 40f, 160f, 96f),
            ),
        )

        coordinator.syncFromProgress()

        assertNull(
            coordinator.activeCoachmark(
                currentScene = AppScene.Home,
                sceneState = homeSceneState,
                badgeCelebrationVisible = false,
            ),
        )
        assertFalse(coordinator.isBlockingStep())

        coordinator.onPackOpened()
        coordinator.onEquipmentOpened()

        assertEquals(
            NewPlayerCoachmarkSpec(
                target = NewPlayerOnboardingTarget.EquipmentActivation,
                title = "Active ta carte",
                message = "Active cet equipement pour ameliorer les prochains packs.",
            ),
            coordinator.activeCoachmark(
                currentScene = AppScene.Equipment,
                sceneState = equipmentSceneState,
                badgeCelebrationVisible = false,
            ),
        )
    }

    @Test
    fun `coachmark waits until onboarding hints are visible after transitions`() = runTest {
        val progressGateway = FakeProgressGateway().apply {
            progress = StandaloneProgress(
                collection = OwnedCollection(),
                rechargeState = testRechargeState(),
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
