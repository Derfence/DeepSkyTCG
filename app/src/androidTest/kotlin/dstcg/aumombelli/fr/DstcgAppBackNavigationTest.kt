package fr.aumombelli.dstcg

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import fr.aumombelli.dstcg.testsupport.badgeCelebrationBackNavigationTestAppContainer
import fr.aumombelli.dstcg.testsupport.backNavigationTestAppContainer
import fr.aumombelli.dstcg.ui.theme.DstcgTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class DstcgAppBackNavigationTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun android_back_from_start_reset_confirmation_closes_dialog_before_finishing_activity() {
        setAppContent(backNavigationTestAppContainer())

        composeRule.onNodeWithTag("start-reset-progress").performClick()
        advanceBy(1)
        composeRule.onNodeWithTag("start-reset-confirmation").assertIsDisplayed()

        pressAndroidBack()
        advanceBy(1)
        composeRule.onAllNodesWithTag("start-reset-confirmation").assertCountEquals(0)
        composeRule.onNodeWithTag("start-begin").assertIsDisplayed()
        assertTrue(!composeRule.activity.isFinishing)
    }

    @Test
    fun android_back_from_start_about_sheet_closes_sheet_before_finishing_activity() {
        setAppContent(backNavigationTestAppContainer())

        composeRule.onNodeWithTag("start-about-trigger").performTouchInput { swipeUp() }
        advanceBy(400)
        composeRule.onNodeWithTag("start-about-sheet").assertIsDisplayed()

        pressAndroidBack()
        advanceBy(400)
        composeRule.onAllNodesWithTag("start-about-sheet").assertCountEquals(0)
        composeRule.onNodeWithTag("start-begin").assertIsDisplayed()
        assertTrue(!composeRule.activity.isFinishing)
    }

    @Test
    fun android_back_from_library_returns_to_main_menu() {
        setAppContent(backNavigationTestAppContainer())
        startAndReachMainMenu()

        composeRule.onNodeWithTag("menu-library").performClick()
        advanceBy(2_700)
        composeRule.onNodeWithTag("library-grid").assertIsDisplayed()

        pressAndroidBack()
        advanceBy(2_100)
        composeRule.onNodeWithTag("menu-open-pack").assertIsDisplayed()
    }

    @Test
    fun android_back_from_badge_book_closes_detail_then_returns_to_main_menu() {
        setAppContent(backNavigationTestAppContainer())
        startAndReachMainMenu()

        composeRule.onNodeWithTag("menu-badges").performClick()
        advanceBy(800)
        composeRule.onNodeWithTag("app-transition-chest").assertIsDisplayed()
        advanceBy(2_000)
        composeRule.onNodeWithTag("badge-book-scroll").assertIsDisplayed()

        composeRule.onNodeWithTag("badge-coin-astronomes-en-herbe::sky::city").performClick()
        advanceBy(700)
        composeRule.onNodeWithTag("badge-detail").assertIsDisplayed()

        pressAndroidBack()
        advanceBy(700)
        composeRule.onNodeWithTag("badge-book-scroll").assertIsDisplayed()

        pressAndroidBack()
        advanceBy(2_100)
        composeRule.onNodeWithTag("menu-open-pack").assertIsDisplayed()
    }

    @Test
    fun android_back_from_pack_selection_returns_to_extension_list_then_menu() {
        setAppContent(backNavigationTestAppContainer())
        startAndReachMainMenu()

        composeRule.onNodeWithTag("menu-open-pack").performClick()
        advanceBy(1_900)
        composeRule.onNodeWithTag("pack-extension-enter-astronomes-en-herbe").performClick()
        advanceBy(1_900)
        composeRule.onNodeWithTag("pack-booster-0").assertIsDisplayed()

        pressAndroidBack()
        advanceBy(900)
        composeRule.onNodeWithTag("pack-extension-enter-astronomes-en-herbe").assertIsDisplayed()

        pressAndroidBack()
        advanceBy(1_900)
        composeRule.onNodeWithTag("menu-open-pack").assertIsDisplayed()
    }

    @Test
    fun android_back_from_pack_opening_returns_to_main_menu() {
        setAppContent(backNavigationTestAppContainer())
        startAndReachMainMenu()

        composeRule.onNodeWithTag("menu-open-pack").performClick()
        advanceBy(1_900)
        composeRule.onNodeWithTag("pack-extension-enter-astronomes-en-herbe").performClick()
        advanceBy(1_900)
        composeRule.onNodeWithTag("pack-booster-0").performClick()
        advanceBy(6_400)
        composeRule.onNodeWithTag("pack-opening-current-card-surface").assertIsDisplayed()

        pressAndroidBack()
        advanceBy(700)
        composeRule.onNodeWithTag("menu-open-pack").assertIsDisplayed()
    }

    @Test
    fun android_back_from_pack_opening_with_new_badge_defers_celebration_until_library_is_seen() {
        setAppContent(badgeCelebrationBackNavigationTestAppContainer())
        startAndReachMainMenu()

        composeRule.onNodeWithTag("menu-open-pack").performClick()
        advanceBy(1_900)
        composeRule.onNodeWithTag("pack-extension-enter-astronomes-en-herbe").performClick()
        advanceBy(1_900)
        composeRule.onNodeWithTag("pack-booster-0").performClick()
        advanceBy(6_400)
        composeRule.onNodeWithTag("pack-opening-current-card-surface").assertIsDisplayed()

        pressAndroidBack()
        advanceBy(1_000)
        composeRule.onNodeWithTag("new-player-coachmark-MenuLibrary").assertIsDisplayed()
        composeRule.onAllNodesWithTag("badge-unlock-celebration").assertCountEquals(0)

        composeRule.onNodeWithTag("menu-library").performClick()
        advanceBy(3_200)
        composeRule.onNodeWithTag("library-grid").assertIsDisplayed()
        pressAndroidBack()
        advanceBy(2_400)
        composeRule.onNodeWithTag("badge-unlock-celebration").assertIsDisplayed()
        composeRule.onNodeWithTag("badge-unlock-celebration-coin-astronomes-en-herbe::sky::city").assertIsDisplayed()

        advanceBy(1_800)
        composeRule.onAllNodesWithTag("badge-unlock-celebration").assertCountEquals(0)
        composeRule.onNodeWithTag("menu-open-pack").assertIsEnabled()
        composeRule.onNodeWithTag("menu-open-pack").assertIsDisplayed()
    }

    @Test
    fun android_back_from_main_menu_finishes_activity() {
        setAppContent(backNavigationTestAppContainer())
        startAndReachMainMenu()

        pressAndroidBack()
        composeRule.waitUntil(timeoutMillis = 5_000) { composeRule.activity.isFinishing }
        assertTrue(composeRule.activity.isFinishing)
    }

    private fun setAppContent(appContainer: AppContainer) {
        composeRule.mainClock.autoAdvance = false
        composeRule.setContent {
            DstcgTheme {
                DstcgApp(appContainer = appContainer)
            }
        }
        advanceBy(3_000)
    }

    private fun startAndReachMainMenu() {
        composeRule.onNodeWithTag("start-begin").performClick()
        advanceBy(1_600)
        composeRule.onNodeWithTag("menu-open-pack").assertIsDisplayed()
    }

    private fun advanceBy(durationMillis: Long) {
        composeRule.mainClock.advanceTimeBy(durationMillis)
        composeRule.waitForIdle()
    }

    private fun pressAndroidBack() {
        composeRule.activity.runOnUiThread {
            composeRule.activity.onBackPressedDispatcher.onBackPressed()
        }
        composeRule.waitForIdle()
    }
}
