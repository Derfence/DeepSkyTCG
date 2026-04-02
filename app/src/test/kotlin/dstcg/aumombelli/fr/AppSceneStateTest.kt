package fr.aumombelli.dstcg

import fr.aumombelli.dstcg.app.AppSceneUiState
import fr.aumombelli.dstcg.app.clearPendingBadgeCelebration
import fr.aumombelli.dstcg.app.enterBadgeBook
import fr.aumombelli.dstcg.app.enterPackOpening
import fr.aumombelli.dstcg.app.finishPackOpeningToMenu
import fr.aumombelli.dstcg.app.prepareBadgeBookEntry
import fr.aumombelli.dstcg.app.preparePackOpeningReturnToMenu
import fr.aumombelli.dstcg.app.preparePackSelection
import fr.aumombelli.dstcg.app.registerPackReady
import fr.aumombelli.dstcg.app.resetLaunchSequence
import fr.aumombelli.dstcg.app.requestPackOpeningExit
import fr.aumombelli.dstcg.app.showStartCard
import fr.aumombelli.dstcg.feature.badges.BadgeItem
import fr.aumombelli.dstcg.feature.badges.BadgeProgress
import fr.aumombelli.dstcg.feature.badges.BadgeRequirementType
import fr.aumombelli.dstcg.ui.motion.AppScene
import fr.aumombelli.dstcg.ui.motion.PackRevealBounds
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AppSceneStateTest {
    @Test
    fun `prepare pack selection resets transient flow state`() {
        val initialState = AppSceneUiState(
            currentScene = AppScene.MainMenu,
            menuContentVisible = true,
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
        assertEquals(false, nextState.menuContentVisible)
        assertEquals(false, nextState.packSceneVisible)
        assertEquals(false, nextState.packExtensionListVisible)
        assertEquals(3, nextState.packRefreshSignal)
        assertEquals(0, nextState.packReadySignal)
        assertEquals(0, nextState.packOpeningExitSignal)
        assertEquals(emptyList<BadgeItem>(), nextState.pendingBadgeCelebration)
        assertNull(nextState.selectedPackRevealBounds)
    }

    @Test
    fun `finish pack opening returns to menu and keeps pending celebration until cleared`() {
        val openedState = AppSceneUiState(
            currentScene = AppScene.PackSelection,
            packSceneVisible = true,
            packExtensionListVisible = true,
            selectedPackRevealBounds = PackRevealBounds(1f, 2f, 3f, 4f),
        ).enterPackOpening().registerPackReady(listOf(sampleBadge()))

        val nextState = openedState.finishPackOpeningToMenu()

        assertEquals(AppScene.MainMenu, nextState.currentScene)
        assertEquals(true, nextState.menuContentVisible)
        assertEquals(false, nextState.packSceneVisible)
        assertEquals(false, nextState.packExtensionListVisible)
        assertEquals(listOf(sampleBadge()), nextState.pendingBadgeCelebration)
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
    fun `prepare badge book entry resets badge content and bumps refresh signal`() {
        val initialState = AppSceneUiState(
            currentScene = AppScene.MainMenu,
            menuContentVisible = true,
            badgeBookContentVisible = true,
            badgeBookRefreshSignal = 2,
        )

        val nextState = initialState.prepareBadgeBookEntry(nextBadgeBookRefreshSignal = 3)

        assertEquals(AppScene.MainMenu, nextState.currentScene)
        assertEquals(false, nextState.menuContentVisible)
        assertEquals(false, nextState.badgeBookContentVisible)
        assertEquals(3, nextState.badgeBookRefreshSignal)
    }

    @Test
    fun `enter badge book switches current scene only`() {
        val state = AppSceneUiState(currentScene = AppScene.MainMenu)

        val nextState = state.enterBadgeBook()

        assertEquals(AppScene.BadgeBook, nextState.currentScene)
        assertEquals(state.menuContentVisible, nextState.menuContentVisible)
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
    fun `prepare return to menu hides menu until the controller reveals it`() {
        val state = AppSceneUiState(
            currentScene = AppScene.PackOpening,
            menuContentVisible = true,
            selectedPackRevealBounds = PackRevealBounds(1f, 2f, 3f, 4f),
            packOpeningExitSignal = 1,
        )

        val nextState = state.preparePackOpeningReturnToMenu()

        assertEquals(AppScene.MainMenu, nextState.currentScene)
        assertEquals(false, nextState.menuContentVisible)
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
