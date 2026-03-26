package fr.aumombelli.gatcha

import fr.aumombelli.gatcha.app.AppSceneUiState
import fr.aumombelli.gatcha.app.enterPackOpening
import fr.aumombelli.gatcha.app.finishPackOpeningToMenu
import fr.aumombelli.gatcha.app.preparePackSelection
import fr.aumombelli.gatcha.app.prepareReturnToLogin
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
    fun `prepare return to login increments key and resets scene overlays`() {
        val initialState = AppSceneUiState(
            currentScene = AppScene.MainMenu,
            launchLogoVisible = false,
            launchLogoRaised = false,
            menuContentVisible = true,
            libraryContentVisible = true,
            packSceneVisible = true,
            packExtensionListVisible = true,
            loginViewModelKey = 6,
            selectedPackRevealBounds = PackRevealBounds(1f, 2f, 3f, 4f),
        )

        val nextState = initialState.prepareReturnToLogin(nextLoginViewModelKey = 7)

        assertEquals(AppScene.Login, nextState.currentScene)
        assertEquals(true, nextState.launchLogoVisible)
        assertEquals(true, nextState.launchLogoRaised)
        assertEquals(false, nextState.menuContentVisible)
        assertEquals(false, nextState.libraryContentVisible)
        assertEquals(false, nextState.packSceneVisible)
        assertEquals(false, nextState.packExtensionListVisible)
        assertEquals(7, nextState.loginViewModelKey)
        assertNull(nextState.selectedPackRevealBounds)
    }
}
