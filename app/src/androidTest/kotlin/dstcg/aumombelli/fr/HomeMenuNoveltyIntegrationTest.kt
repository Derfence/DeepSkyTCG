package fr.aumombelli.dstcg

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import fr.aumombelli.dstcg.app.AppLaunchConfig
import fr.aumombelli.dstcg.app.AppLaunchScene
import fr.aumombelli.dstcg.data.requireUsableProgress
import fr.aumombelli.dstcg.model.HomeMenuNoveltyState
import fr.aumombelli.dstcg.model.LibraryCardNoveltyState
import fr.aumombelli.dstcg.testsupport.homeMenuNoveltyTestAppContainer
import fr.aumombelli.dstcg.ui.theme.DstcgTheme
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class HomeMenuNoveltyIntegrationTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun opening_library_clears_only_library_novelty_in_progress() {
        val appContainer = homeMenuNoveltyTestAppContainer()
        setAppContent(appContainer)

        composeRule.onNodeWithTag("home-library").performClick()
        advanceUntilTagExists("library-grid", timeoutMillis = 10_000)

        assertNoveltyState(
            appContainer = appContainer,
            expectedState = HomeMenuNoveltyState(
                library = false,
                equipment = true,
                badgeBook = true,
            ),
        )
        assertLibraryNoveltyState(
            appContainer = appContainer,
            expectedState = LibraryCardNoveltyState(),
        )
    }

    @Test
    fun opening_badges_clears_only_badge_novelty_in_progress() {
        val appContainer = homeMenuNoveltyTestAppContainer()
        setAppContent(appContainer)

        composeRule.onNodeWithTag("home-badges").performClick()
        advanceUntilTagExists("badge-book-scroll", timeoutMillis = 10_000)

        assertNoveltyState(
            appContainer = appContainer,
            expectedState = HomeMenuNoveltyState(
                library = true,
                equipment = true,
                badgeBook = false,
            ),
        )
    }

    @Test
    fun opening_equipment_clears_only_equipment_novelty_in_progress() {
        val appContainer = homeMenuNoveltyTestAppContainer()
        setAppContent(appContainer)

        composeRule.onNodeWithTag("home-equipment").performClick()
        advanceUntilTagExists("equipment-screen", timeoutMillis = 10_000)
        advanceUntilTagGone("app-transition-equipment", timeoutMillis = 10_000)

        assertNoveltyState(
            appContainer = appContainer,
            expectedState = HomeMenuNoveltyState(
                library = true,
                equipment = false,
                badgeBook = true,
            ),
        )
    }

    private fun assertNoveltyState(
        appContainer: AppContainer,
        expectedState: HomeMenuNoveltyState,
    ) {
        val progress = runBlocking {
            appContainer.progressRepository.loadProgress().requireUsableProgress().progress
        }

        assertEquals(expectedState, progress.homeMenuNoveltyState)
    }

    private fun assertLibraryNoveltyState(
        appContainer: AppContainer,
        expectedState: LibraryCardNoveltyState,
    ) {
        val progress = runBlocking {
            appContainer.progressRepository.loadProgress().requireUsableProgress().progress
        }

        assertEquals(expectedState, progress.libraryCardNoveltyState)
    }

    private fun setAppContent(appContainer: AppContainer) {
        composeRule.mainClock.autoAdvance = false
        composeRule.setContent {
            DstcgTheme {
                DstcgApp(
                    appContainer = appContainer,
                    launchConfig = AppLaunchConfig(scene = AppLaunchScene.Home),
                )
            }
        }
        advanceUntilTagExists("home-open-pack", timeoutMillis = 10_000)
    }

    private fun advanceBy(durationMillis: Long) {
        composeRule.mainClock.advanceTimeBy(durationMillis)
        composeRule.waitForIdle()
    }

    private fun advanceUntilTagExists(tag: String, timeoutMillis: Long) {
        advanceUntil(timeoutMillis) {
            composeRule.onAllNodesWithTag(tag, useUnmergedTree = true)
                .fetchSemanticsNodes(atLeastOneRootRequired = false)
                .isNotEmpty()
        }
    }

    private fun advanceUntilTagGone(tag: String, timeoutMillis: Long) {
        advanceUntil(timeoutMillis) {
            composeRule.onAllNodesWithTag(tag, useUnmergedTree = true)
                .fetchSemanticsNodes(atLeastOneRootRequired = false)
                .isEmpty()
        }
    }

    private fun advanceUntil(timeoutMillis: Long, condition: () -> Boolean) {
        var elapsed = 0L
        while (elapsed <= timeoutMillis) {
            if (condition()) {
                return
            }
            advanceBy(100)
            elapsed += 100
        }
        throw androidx.compose.ui.test.ComposeTimeoutException(
            "Condition still not satisfied after ${timeoutMillis} ms",
        )
    }
}
