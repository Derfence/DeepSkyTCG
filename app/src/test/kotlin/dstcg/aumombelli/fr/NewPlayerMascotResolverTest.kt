package fr.aumombelli.dstcg

import androidx.compose.ui.geometry.Rect
import fr.aumombelli.dstcg.app.AppSceneUiState
import fr.aumombelli.dstcg.app.NewPlayerBlockingModalKind
import fr.aumombelli.dstcg.app.NewPlayerCoachmarkSpec
import fr.aumombelli.dstcg.app.NewPlayerOnboardingTarget
import fr.aumombelli.dstcg.app.resolveNewPlayerBlockingModalMascotSpec
import fr.aumombelli.dstcg.app.resolveNewPlayerSceneMascotSpec
import fr.aumombelli.dstcg.model.NewPlayerOnboardingStep
import fr.aumombelli.dstcg.ui.component.AsterAnchor
import fr.aumombelli.dstcg.ui.component.AsterFace
import fr.aumombelli.dstcg.ui.component.AsterHand
import fr.aumombelli.dstcg.ui.component.AsterHandSide
import fr.aumombelli.dstcg.ui.component.AsterMascotScale
import fr.aumombelli.dstcg.ui.motion.AppScene
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NewPlayerMascotResolverTest {
    @Test
    fun `blocking modal mascots are used for intro and conclusion`() {
        val welcome = resolveNewPlayerBlockingModalMascotSpec(NewPlayerBlockingModalKind.WelcomeIntro)
        val conclusion = resolveNewPlayerBlockingModalMascotSpec(NewPlayerBlockingModalKind.Conclusion)
        val libraryVariants = resolveNewPlayerBlockingModalMascotSpec(NewPlayerBlockingModalKind.LibraryVariants)
        val craftingTools = resolveNewPlayerBlockingModalMascotSpec(NewPlayerBlockingModalKind.CraftingTools)

        assertEquals(AsterFace.Smile, welcome?.face)
        assertEquals(AsterHand.Open, welcome?.hand)
        assertEquals(AsterAnchor.BottomCenter, welcome?.anchor)
        assertEquals(AsterMascotScale.Compact, welcome?.scale)
        assertEquals(true, welcome?.showBothHands)
        assertEquals(2f, welcome?.sizeMultiplier)
        assertEquals(AsterFace.BigSmile, conclusion?.face)
        assertEquals(AsterHand.Cards, conclusion?.hand)
        assertEquals(AsterHand.Telescope, conclusion?.mirroredHand)
        assertEquals(AsterAnchor.BottomCenter, conclusion?.anchor)
        assertEquals(AsterMascotScale.Compact, conclusion?.scale)
        assertEquals(true, conclusion?.showBothHands)
        assertEquals(2f, conclusion?.sizeMultiplier)
        assertNull(libraryVariants)
        assertNull(craftingTools)
    }

    @Test
    fun `silent completed and modal steps do not show scene Aster`() {
        val sceneState = homeState()

        listOf(
            null,
            NewPlayerOnboardingStep.ShowWelcomeIntro,
            NewPlayerOnboardingStep.LearnLibraryVariants,
            NewPlayerOnboardingStep.OpenSecondPackMenu,
            NewPlayerOnboardingStep.AwaitCraftingEligibility,
            NewPlayerOnboardingStep.ShowConclusion,
            NewPlayerOnboardingStep.Completed,
        ).forEach { step ->
            assertNull(
                resolveNewPlayerSceneMascotSpec(
                    currentStep = step,
                    currentScene = AppScene.Home,
                    sceneState = sceneState,
                    activeCoachmarkSpec = coachmark(NewPlayerOnboardingTarget.HomeOpenPack),
                    badgeCelebrationVisible = false,
                ),
            )
        }
        assertNull(
            resolveNewPlayerSceneMascotSpec(
                currentStep = NewPlayerOnboardingStep.OpenFirstPackMenu,
                currentScene = AppScene.Home,
                sceneState = sceneState.copy(coachmarkTargetBounds = emptyMap()),
                activeCoachmarkSpec = coachmark(NewPlayerOnboardingTarget.HomeOpenPack),
                badgeCelebrationVisible = false,
            ),
        )
        assertNull(
            resolveNewPlayerSceneMascotSpec(
                currentStep = NewPlayerOnboardingStep.ViewLibrary,
                currentScene = AppScene.PackOpening,
                sceneState = AppSceneUiState(
                    currentScene = AppScene.PackOpening,
                    packSceneVisible = true,
                    onboardingHintsVisible = true,
                    rootWidthPx = 360f,
                    rootHeightPx = 640f,
                ),
                activeCoachmarkSpec = null,
                badgeCelebrationVisible = false,
            ),
        )
    }

    @Test
    fun `guided home steps use the planned poses`() {
        val sceneState = homeState()

        val firstPack = resolveNewPlayerSceneMascotSpec(
            currentStep = NewPlayerOnboardingStep.OpenFirstPackMenu,
            currentScene = AppScene.Home,
            sceneState = sceneState,
            activeCoachmarkSpec = coachmark(NewPlayerOnboardingTarget.HomeOpenPack),
            badgeCelebrationVisible = false,
        )
        val equipmentMenu = resolveNewPlayerSceneMascotSpec(
            currentStep = NewPlayerOnboardingStep.ViewEquipmentMenu,
            currentScene = AppScene.Home,
            sceneState = sceneState.copy(
                coachmarkTargetBounds = mapOf(NewPlayerOnboardingTarget.HomeEquipment to Rect(16f, 180f, 112f, 280f)),
            ),
            activeCoachmarkSpec = coachmark(NewPlayerOnboardingTarget.HomeEquipment),
            badgeCelebrationVisible = false,
        )
        val libraryMenu = resolveNewPlayerSceneMascotSpec(
            currentStep = NewPlayerOnboardingStep.ViewLibrary,
            currentScene = AppScene.Home,
            sceneState = homeState(
                target = NewPlayerOnboardingTarget.HomeLibrary,
                targetBounds = Rect(120f, 180f, 240f, 360f),
            ),
            activeCoachmarkSpec = coachmark(NewPlayerOnboardingTarget.HomeLibrary),
            badgeCelebrationVisible = false,
        )
        val badges = resolveNewPlayerSceneMascotSpec(
            currentStep = NewPlayerOnboardingStep.ViewBadges,
            currentScene = AppScene.Home,
            sceneState = homeState(
                target = NewPlayerOnboardingTarget.HomeBadges,
                targetBounds = Rect(120f, 180f, 240f, 360f),
            ),
            activeCoachmarkSpec = coachmark(NewPlayerOnboardingTarget.HomeBadges),
            badgeCelebrationVisible = false,
        )

        assertEquals(AsterFace.BigSmile, firstPack?.face)
        assertEquals(AsterHand.Point, firstPack?.hand)
        assertEquals(AsterAnchor.BottomEnd, firstPack?.anchor)
        assertEquals(AsterHandSide.Left, firstPack?.handSide)
        assertEquals(AsterFace.Smile, equipmentMenu?.face)
        assertEquals(AsterHand.Telescope, equipmentMenu?.hand)
        assertEquals(AsterAnchor.BottomEnd, equipmentMenu?.anchor)
        assertEquals(AsterHandSide.Left, equipmentMenu?.handSide)
        assertEquals(AsterFace.Smile, libraryMenu?.face)
        assertEquals(AsterHand.Cards, libraryMenu?.hand)
        assertEquals(AsterAnchor.BottomEnd, libraryMenu?.anchor)
        assertEquals(AsterFace.BigSmile, badges?.face)
        assertEquals(AsterHand.Point, badges?.hand)
        assertEquals(AsterAnchor.BottomEnd, badges?.anchor)
    }

    @Test
    fun `pack selection mascots stay in bottom end corner`() {
        val extensionMascot = resolveNewPlayerSceneMascotSpec(
            currentStep = NewPlayerOnboardingStep.SelectFirstExtension,
            currentScene = AppScene.PackSelection,
            sceneState = AppSceneUiState(
                currentScene = AppScene.PackSelection,
                packSceneVisible = true,
                packExtensionListVisible = true,
                onboardingHintsVisible = true,
                rootWidthPx = 360f,
                rootHeightPx = 640f,
                coachmarkTargetBounds = mapOf(
                    NewPlayerOnboardingTarget.PackSelectionExtension to Rect(24f, 120f, 180f, 220f),
                ),
            ),
            activeCoachmarkSpec = coachmark(NewPlayerOnboardingTarget.PackSelectionExtension),
            badgeCelebrationVisible = false,
        )
        val boosterMascot = resolveNewPlayerSceneMascotSpec(
            currentStep = NewPlayerOnboardingStep.SelectFirstBooster,
            currentScene = AppScene.PackSelection,
            sceneState = AppSceneUiState(
                currentScene = AppScene.PackSelection,
                packSceneVisible = true,
                onboardingHintsVisible = true,
                rootWidthPx = 360f,
                rootHeightPx = 640f,
                coachmarkTargetBounds = mapOf(
                    NewPlayerOnboardingTarget.PackSelectionBooster to Rect(48f, 160f, 312f, 430f),
                ),
            ),
            activeCoachmarkSpec = coachmark(NewPlayerOnboardingTarget.PackSelectionBooster),
            badgeCelebrationVisible = false,
        )

        assertEquals(AsterAnchor.BottomEnd, extensionMascot?.anchor)
        assertEquals(AsterFace.HappyEyes, extensionMascot?.face)
        assertEquals(AsterHand.Point, extensionMascot?.hand)
        assertEquals(AsterHandSide.Left, extensionMascot?.handSide)
        assertEquals(AsterAnchor.BottomEnd, boosterMascot?.anchor)
        assertEquals(AsterFace.BigSmile, boosterMascot?.face)
        assertEquals(AsterHand.Point, boosterMascot?.hand)
        assertEquals(AsterHandSide.Left, boosterMascot?.handSide)
    }

    @Test
    fun `crafting chapter uses wrench hand`() {
        val homeMascot = resolveNewPlayerSceneMascotSpec(
            currentStep = NewPlayerOnboardingStep.ViewCraftingMenu,
            currentScene = AppScene.Home,
            sceneState = homeState(
                target = NewPlayerOnboardingTarget.HomeCrafting,
                targetBounds = Rect(280f, 560f, 336f, 616f),
            ),
            activeCoachmarkSpec = coachmark(NewPlayerOnboardingTarget.HomeCrafting),
            badgeCelebrationVisible = false,
        )
        val craftingMascot = resolveNewPlayerSceneMascotSpec(
            currentStep = NewPlayerOnboardingStep.UseSkyDarkening,
            currentScene = AppScene.Crafting,
            sceneState = AppSceneUiState(
                currentScene = AppScene.Crafting,
                craftingContentVisible = true,
                onboardingHintsVisible = true,
                rootWidthPx = 360f,
                rootHeightPx = 640f,
                coachmarkTargetBounds = mapOf(
                    NewPlayerOnboardingTarget.CraftingDarkenSkyMode to Rect(0f, 0f, 360f, 320f),
                ),
            ),
            activeCoachmarkSpec = coachmark(NewPlayerOnboardingTarget.CraftingDarkenSkyMode),
            badgeCelebrationVisible = false,
        )

        assertEquals(AsterHand.Wrench, homeMascot?.hand)
        assertEquals(AsterAnchor.BottomStart, homeMascot?.anchor)
        assertEquals(AsterHandSide.Right, homeMascot?.handSide)
        assertEquals(AsterHand.Wrench, craftingMascot?.hand)
        assertEquals(AsterAnchor.BottomEnd, craftingMascot?.anchor)
        assertEquals(AsterHandSide.Left, craftingMascot?.handSide)
    }

    @Test
    fun `equipment scene does not show Aster during activation coachmark`() {
        val mascot = resolveNewPlayerSceneMascotSpec(
            currentStep = NewPlayerOnboardingStep.ActivateFirstEquipment,
            currentScene = AppScene.Equipment,
            sceneState = AppSceneUiState(
                currentScene = AppScene.Equipment,
                equipmentContentVisible = true,
                onboardingHintsVisible = true,
                rootWidthPx = 360f,
                rootHeightPx = 640f,
                coachmarkTargetBounds = mapOf(
                    NewPlayerOnboardingTarget.EquipmentActivation to Rect(208f, 480f, 344f, 560f),
                ),
            ),
            activeCoachmarkSpec = coachmark(NewPlayerOnboardingTarget.EquipmentActivation),
            badgeCelebrationVisible = false,
        )

        assertNull(mascot)
    }

    @Test
    fun `mascot is hidden instead of moving when the preferred corner would cover the target`() {
        val sceneState = homeState(
            target = NewPlayerOnboardingTarget.HomeOpenPack,
            targetBounds = Rect(240f, 552f, 352f, 632f),
        )

        val mascot = resolveNewPlayerSceneMascotSpec(
            currentStep = NewPlayerOnboardingStep.OpenFirstPackMenu,
            currentScene = AppScene.Home,
            sceneState = sceneState,
            activeCoachmarkSpec = coachmark(NewPlayerOnboardingTarget.HomeOpenPack),
            badgeCelebrationVisible = false,
        )

        assertNull(mascot)
    }

    @Test
    fun `mascot is hidden when the fixed corner would cover the target`() {
        val sceneState = homeState(
            target = NewPlayerOnboardingTarget.HomeOpenPack,
            targetBounds = Rect(0f, 540f, 360f, 640f),
        )

        val mascot = resolveNewPlayerSceneMascotSpec(
            currentStep = NewPlayerOnboardingStep.OpenFirstPackMenu,
            currentScene = AppScene.Home,
            sceneState = sceneState,
            activeCoachmarkSpec = coachmark(NewPlayerOnboardingTarget.HomeOpenPack),
            badgeCelebrationVisible = false,
        )

        assertNull(mascot)
    }

    @Test
    fun `transition and badge celebration temporarily hide Aster`() {
        val mascotDuringTransition = resolveNewPlayerSceneMascotSpec(
            currentStep = NewPlayerOnboardingStep.ViewBadges,
            currentScene = AppScene.Home,
            sceneState = homeState().copy(transitionLocked = true),
            activeCoachmarkSpec = coachmark(NewPlayerOnboardingTarget.HomeBadges),
            badgeCelebrationVisible = false,
        )
        val mascotDuringBadgeCelebration = resolveNewPlayerSceneMascotSpec(
            currentStep = NewPlayerOnboardingStep.ViewBadges,
            currentScene = AppScene.Home,
            sceneState = homeState(),
            activeCoachmarkSpec = coachmark(NewPlayerOnboardingTarget.HomeBadges),
            badgeCelebrationVisible = true,
        )

        assertNull(mascotDuringTransition)
        assertNull(mascotDuringBadgeCelebration)
    }

    private fun homeState(
        target: NewPlayerOnboardingTarget = NewPlayerOnboardingTarget.HomeOpenPack,
        targetBounds: Rect = Rect(120f, 180f, 240f, 360f),
    ): AppSceneUiState = AppSceneUiState(
        currentScene = AppScene.Home,
        homeContentVisible = true,
        onboardingHintsVisible = true,
        rootWidthPx = 360f,
        rootHeightPx = 640f,
        coachmarkTargetBounds = mapOf(target to targetBounds),
    )

    private fun coachmark(target: NewPlayerOnboardingTarget): NewPlayerCoachmarkSpec =
        NewPlayerCoachmarkSpec(
            target = target,
            title = "Titre",
            message = "Message",
        )
}
