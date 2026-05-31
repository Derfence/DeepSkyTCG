package fr.aumombelli.dstcg

import fr.aumombelli.dstcg.app.AppSceneUiState
import fr.aumombelli.dstcg.app.PackOpeningExitDestination
import fr.aumombelli.dstcg.app.clearPendingBadgeCelebration
import fr.aumombelli.dstcg.app.enterBadgeBook
import fr.aumombelli.dstcg.app.enterCrafting
import fr.aumombelli.dstcg.app.enterPackOpening
import fr.aumombelli.dstcg.app.finishPackOpeningToPackSelection
import fr.aumombelli.dstcg.app.finishPackOpeningToHome
import fr.aumombelli.dstcg.app.packOpeningExitDestination
import fr.aumombelli.dstcg.app.lockTransitions
import fr.aumombelli.dstcg.app.prepareBadgeBookEntry
import fr.aumombelli.dstcg.app.prepareCraftingEntry
import fr.aumombelli.dstcg.app.preparePackOpeningReturnToPackSelection
import fr.aumombelli.dstcg.app.preparePackOpeningReturnToHome
import fr.aumombelli.dstcg.app.preparePackSelection
import fr.aumombelli.dstcg.app.registerPackReady
import fr.aumombelli.dstcg.app.resetLaunchSequence
import fr.aumombelli.dstcg.app.requestPackOpeningExit
import fr.aumombelli.dstcg.app.showHomeContent
import fr.aumombelli.dstcg.app.showOnboardingHints
import fr.aumombelli.dstcg.app.unlockTransitions
import fr.aumombelli.dstcg.feature.badges.BadgeItem
import fr.aumombelli.dstcg.feature.badges.BadgeProgress
import fr.aumombelli.dstcg.feature.badges.BadgeRequirementType
import fr.aumombelli.dstcg.model.NewPlayerOnboardingStep
import fr.aumombelli.dstcg.ui.motion.AppScene
import fr.aumombelli.dstcg.ui.motion.PackRevealBounds
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AppSceneStateTest {
    @Test
    fun `prepare pack selection resets transient flow state`() {
        val initialState = AppSceneUiState(
            currentScene = AppScene.Home,
            homeContentVisible = true,
            packRefreshSignal = 2,
            packReadySignal = 4,
            selectedPackRevealBounds = PackRevealBounds(
                leftPx = 10f,
                topPx = 20f,
                widthPx = 30f,
                heightPx = 40f,
            ),
            packOpeningExitSignal = 2,
            pendingBadgeCelebration = listOf(sampleBadge()),
        )

        val nextState = initialState.preparePackSelection(nextPackRefreshSignal = 3)

        assertEquals(AppScene.PackSelection, nextState.currentScene)
        assertEquals(false, nextState.homeContentVisible)
        assertEquals(false, nextState.packSceneVisible)
        assertEquals(false, nextState.packExtensionListVisible)
        assertEquals(3, nextState.packRefreshSignal)
        assertEquals(0, nextState.packReadySignal)
        assertEquals(0, nextState.packOpeningExitSignal)
        assertEquals(listOf(sampleBadge()), nextState.pendingBadgeCelebration)
        assertNull(nextState.selectedPackRevealBounds)
    }

    @Test
    fun `finish pack opening returns to home and keeps pending celebration until cleared`() {
        val openedState = AppSceneUiState(
            currentScene = AppScene.PackSelection,
            packSceneVisible = true,
            packExtensionListVisible = true,
            selectedPackRevealBounds = PackRevealBounds(1f, 2f, 3f, 4f),
        ).enterPackOpening().registerPackReady(
            newlyUnlockedBadges = listOf(sampleBadge()),
            deferBadgeCelebration = false,
        )

        val nextState = openedState.finishPackOpeningToHome()

        assertEquals(AppScene.Home, nextState.currentScene)
        assertEquals(true, nextState.homeContentVisible)
        assertEquals(false, nextState.packSceneVisible)
        assertEquals(false, nextState.packExtensionListVisible)
        assertEquals(listOf(sampleBadge()), nextState.pendingBadgeCelebration)
        assertNull(nextState.selectedPackRevealBounds)
    }

    @Test
    fun `finish pack opening can return to visible pack selection`() {
        val openedState = AppSceneUiState(
            currentScene = AppScene.PackSelection,
            packSceneVisible = true,
            packExtensionListVisible = true,
            selectedPackRevealBounds = PackRevealBounds(1f, 2f, 3f, 4f),
            packOpeningExitSignal = 1,
        ).enterPackOpening()

        val nextState = openedState.finishPackOpeningToPackSelection()

        assertEquals(AppScene.PackSelection, nextState.currentScene)
        assertEquals(false, nextState.homeContentVisible)
        assertEquals(true, nextState.packSceneVisible)
        assertEquals(true, nextState.packExtensionListVisible)
        assertEquals(0, nextState.packOpeningExitSignal)
        assertNull(nextState.selectedPackRevealBounds)
    }

    @Test
    fun `pack opening exits to home during active onboarding`() {
        val state = AppSceneUiState(currentScene = AppScene.PackOpening)

        val destination = state.packOpeningExitDestination(NewPlayerOnboardingStep.ViewLibrary)

        assertEquals(PackOpeningExitDestination.Home, destination)
    }

    @Test
    fun `pack opening exits to pack selection while crafting eligibility is pending`() {
        val state = AppSceneUiState(currentScene = AppScene.PackOpening)

        val destination = state.packOpeningExitDestination(NewPlayerOnboardingStep.AwaitCraftingEligibility)

        assertEquals(PackOpeningExitDestination.PackSelection, destination)
    }

    @Test
    fun `pack opening exits to home when crafting onboarding becomes available`() {
        val state = AppSceneUiState(currentScene = AppScene.PackOpening)

        val destination = state.packOpeningExitDestination(NewPlayerOnboardingStep.ViewCraftingMenu)

        assertEquals(PackOpeningExitDestination.Home, destination)
    }

    @Test
    fun `pack opening exits to home for pending badges while crafting eligibility is pending`() {
        val state = AppSceneUiState(
            currentScene = AppScene.PackOpening,
            pendingBadgeCelebration = listOf(sampleBadge()),
        )

        val destination = state.packOpeningExitDestination(NewPlayerOnboardingStep.AwaitCraftingEligibility)

        assertEquals(PackOpeningExitDestination.Home, destination)
    }

    @Test
    fun `pack opening exits to home when badges were unlocked`() {
        val state = AppSceneUiState(
            currentScene = AppScene.PackOpening,
            pendingBadgeCelebration = listOf(sampleBadge()),
        )

        val destination = state.packOpeningExitDestination(NewPlayerOnboardingStep.Completed)

        assertEquals(PackOpeningExitDestination.Home, destination)
    }

    @Test
    fun `pack opening exits to pack selection when onboarding is completed and no badge was unlocked`() {
        val state = AppSceneUiState(currentScene = AppScene.PackOpening)

        val destination = state.packOpeningExitDestination(NewPlayerOnboardingStep.Completed)

        assertEquals(PackOpeningExitDestination.PackSelection, destination)
    }

    @Test
    fun `pack opening exits to pack selection without onboarding and badge`() {
        val state = AppSceneUiState(currentScene = AppScene.PackOpening)

        val destination = state.packOpeningExitDestination(null)

        assertEquals(PackOpeningExitDestination.PackSelection, destination)
    }

    @Test
    fun `reset launch sequence hides home content and logo lift`() {
        val initialState = AppSceneUiState(
            currentScene = AppScene.Home,
            launchLogoVisible = true,
            launchLogoRaised = true,
            homeContentVisible = true,
            onboardingHintsVisible = true,
        )

        val nextState = initialState.resetLaunchSequence()

        assertEquals(AppScene.Home, nextState.currentScene)
        assertEquals(false, nextState.launchLogoVisible)
        assertEquals(false, nextState.launchLogoRaised)
        assertEquals(false, nextState.homeContentVisible)
        assertEquals(false, nextState.onboardingHintsVisible)
    }

    @Test
    fun `show home content only reveals the home overlay`() {
        val state = AppSceneUiState(homeContentVisible = false)

        val nextState = state.showHomeContent()

        assertEquals(true, nextState.homeContentVisible)
        assertEquals(state.currentScene, nextState.currentScene)
    }

    @Test
    fun `prepare badge book entry resets badge content and bumps refresh signal`() {
        val initialState = AppSceneUiState(
            currentScene = AppScene.Home,
            homeContentVisible = true,
            badgeBookContentVisible = true,
            badgeBookRefreshSignal = 2,
        )

        val nextState = initialState.prepareBadgeBookEntry(nextBadgeBookRefreshSignal = 3)

        assertEquals(AppScene.Home, nextState.currentScene)
        assertEquals(false, nextState.homeContentVisible)
        assertEquals(false, nextState.badgeBookContentVisible)
        assertEquals(3, nextState.badgeBookRefreshSignal)
    }

    @Test
    fun `prepare crafting entry resets crafting content and bumps refresh signal`() {
        val initialState = AppSceneUiState(
            currentScene = AppScene.Home,
            homeContentVisible = true,
            craftingContentVisible = true,
            craftingRefreshSignal = 2,
        )

        val nextState = initialState.prepareCraftingEntry(nextCraftingRefreshSignal = 3)

        assertEquals(AppScene.Home, nextState.currentScene)
        assertEquals(false, nextState.homeContentVisible)
        assertEquals(false, nextState.craftingContentVisible)
        assertEquals(3, nextState.craftingRefreshSignal)
    }

    @Test
    fun `enter crafting switches current scene only`() {
        val state = AppSceneUiState(currentScene = AppScene.Home)

        val nextState = state.enterCrafting()

        assertEquals(AppScene.Crafting, nextState.currentScene)
        assertEquals(state.homeContentVisible, nextState.homeContentVisible)
    }

    @Test
    fun `enter badge book switches current scene only`() {
        val state = AppSceneUiState(currentScene = AppScene.Home)

        val nextState = state.enterBadgeBook()

        assertEquals(AppScene.BadgeBook, nextState.currentScene)
        assertEquals(state.homeContentVisible, nextState.homeContentVisible)
    }

    @Test
    fun `request pack opening exit increments dismiss signal`() {
        val state = AppSceneUiState(
            currentScene = AppScene.PackOpening,
            packOpeningExitSignal = 1,
        )

        val nextState = state.requestPackOpeningExit()

        assertEquals(2, nextState.packOpeningExitSignal)
    }

    @Test
    fun `prepare return to home hides home until the controller reveals it`() {
        val state = AppSceneUiState(
            currentScene = AppScene.PackOpening,
            homeContentVisible = true,
            selectedPackRevealBounds = PackRevealBounds(1f, 2f, 3f, 4f),
            packOpeningExitSignal = 1,
        )

        val nextState = state.preparePackOpeningReturnToHome()

        assertEquals(AppScene.Home, nextState.currentScene)
        assertEquals(false, nextState.homeContentVisible)
        assertEquals(0, nextState.packOpeningExitSignal)
        assertNull(nextState.selectedPackRevealBounds)
    }

    @Test
    fun `prepare return to pack selection hides the pack scene until the controller reveals it`() {
        val state = AppSceneUiState(
            currentScene = AppScene.PackOpening,
            packSceneVisible = true,
            packExtensionListVisible = true,
            selectedPackRevealBounds = PackRevealBounds(1f, 2f, 3f, 4f),
            packOpeningExitSignal = 1,
        )

        val nextState = state.preparePackOpeningReturnToPackSelection()

        assertEquals(AppScene.PackSelection, nextState.currentScene)
        assertEquals(false, nextState.packSceneVisible)
        assertEquals(false, nextState.packExtensionListVisible)
        assertEquals(0, nextState.packOpeningExitSignal)
        assertNull(nextState.selectedPackRevealBounds)
    }

    @Test
    fun `clear pending celebration removes transient badge payload`() {
        val state = AppSceneUiState(
            pendingBadgeCelebration = listOf(sampleBadge()),
        )

        val nextState = state.clearPendingBadgeCelebration()

        assertEquals(emptyList<BadgeItem>(), nextState.pendingBadgeCelebration)
    }

    @Test
    fun `locking transitions hides onboarding hints until they are restored`() {
        val state = AppSceneUiState(
            currentScene = AppScene.Home,
            onboardingHintsVisible = true,
        )

        val lockedState = state.lockTransitions()
        val restoredState = lockedState.unlockTransitions().showOnboardingHints()

        assertEquals(true, lockedState.transitionLocked)
        assertEquals(false, lockedState.onboardingHintsVisible)
        assertEquals(false, restoredState.transitionLocked)
        assertEquals(true, restoredState.onboardingHintsVisible)
    }

    private fun sampleBadge(): BadgeItem = BadgeItem(
        id = "astro::sky::city",
        extensionId = "astro",
        extensionName = "Astro",
        title = "Ville",
        description = "Description",
        requirementType = BadgeRequirementType.SkyQuality,
        progress = BadgeProgress(matchedCards = 1, totalCards = 1),
        skyQualityCode = "city",
    )
}
