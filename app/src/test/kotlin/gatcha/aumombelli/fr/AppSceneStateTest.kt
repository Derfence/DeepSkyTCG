package fr.aumombelli.gatcha

import fr.aumombelli.gatcha.app.AppSceneUiState
import fr.aumombelli.gatcha.app.enterBadgeBook
import fr.aumombelli.gatcha.app.enterPackOpening
import fr.aumombelli.gatcha.app.finishPackOpeningToMenu
import fr.aumombelli.gatcha.app.prepareBadgeBookEntry
import fr.aumombelli.gatcha.app.preparePackSelection
import fr.aumombelli.gatcha.app.resetLaunchSequence
import fr.aumombelli.gatcha.app.showStartCard
import fr.aumombelli.gatcha.ui.motion.AppScene
import fr.aumombelli.gatcha.ui.motion.PackRevealBounds
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AppSceneStateTest {
    @Test
    fun `prepare pack selection resets transient flow state`() {
        val initialState = AppSceneUiState(
            currentScene = AppScene.MainMenu,
            menuContentVisible = true,
            packFlowKey = 2,
            packReadySignal = 4,
            selectedPackRevealBounds = PackRevealBounds(
                leftPx = 10f,
                topPx = 20f,
                widthPx = 30f,
                heightPx = 40f,
            ),
        )

        val nextState = initialState.preparePackSelection(nextPackFlowKey = 3)

        assertEquals(AppScene.PackSelection, nextState.currentScene)
        assertEquals(false, nextState.menuContentVisible)
        assertEquals(false, nextState.packSceneVisible)
        assertEquals(false, nextState.packExtensionListVisible)
        assertEquals(3, nextState.packFlowKey)
        assertEquals(0, nextState.packReadySignal)
        assertNull(nextState.selectedPackRevealBounds)
    }

    @Test
    fun `finish pack opening returns to menu and clears reveal state`() {
        val openedState = AppSceneUiState(
            currentScene = AppScene.PackSelection,
            packSceneVisible = true,
            packExtensionListVisible = true,
            selectedPackRevealBounds = PackRevealBounds(1f, 2f, 3f, 4f),
        ).enterPackOpening()

        val nextState = openedState.finishPackOpeningToMenu()

        assertEquals(AppScene.MainMenu, nextState.currentScene)
        assertEquals(true, nextState.menuContentVisible)
        assertEquals(false, nextState.packSceneVisible)
        assertEquals(false, nextState.packExtensionListVisible)
        assertNull(nextState.selectedPackRevealBounds)
    }

    @Test
    fun `reset launch sequence hides start card and logo lift`() {
        val initialState = AppSceneUiState(
            currentScene = AppScene.Start,
            launchLogoVisible = true,
            launchLogoRaised = true,
            startCardVisible = true,
        )

        val nextState = initialState.resetLaunchSequence()

        assertEquals(AppScene.Start, nextState.currentScene)
        assertEquals(false, nextState.launchLogoVisible)
        assertEquals(false, nextState.launchLogoRaised)
        assertEquals(false, nextState.startCardVisible)
    }

    @Test
    fun `show start card only reveals the start overlay`() {
        val state = AppSceneUiState(startCardVisible = false)

        val nextState = state.showStartCard()

        assertEquals(true, nextState.startCardVisible)
        assertEquals(state.currentScene, nextState.currentScene)
    }

    @Test
    fun `prepare badge book entry resets badge content and increments key`() {
        val initialState = AppSceneUiState(
            currentScene = AppScene.MainMenu,
            menuContentVisible = true,
            badgeBookContentVisible = true,
            badgeBookViewModelKey = 2,
        )

        val nextState = initialState.prepareBadgeBookEntry(nextBadgeBookViewModelKey = 3)

        assertEquals(AppScene.MainMenu, nextState.currentScene)
        assertEquals(false, nextState.menuContentVisible)
        assertEquals(false, nextState.badgeBookContentVisible)
        assertEquals(3, nextState.badgeBookViewModelKey)
    }

    @Test
    fun `enter badge book switches current scene only`() {
        val state = AppSceneUiState(currentScene = AppScene.MainMenu)

        val nextState = state.enterBadgeBook()

        assertEquals(AppScene.BadgeBook, nextState.currentScene)
        assertEquals(state.menuContentVisible, nextState.menuContentVisible)
    }
}
