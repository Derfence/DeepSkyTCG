package fr.aumombelli.gatcha

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import fr.aumombelli.gatcha.testsupport.backNavigationTestAppContainer
import fr.aumombelli.gatcha.ui.theme.GatchaTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class GatchaAppBackNavigationTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun android_back_from_library_returns_to_main_menu() {
        setAppContent(backNavigationTestAppContainer())
        loginAndReachMainMenu()

        composeRule.onNodeWithTag("menu-library").performClick()
        advanceBy(2_700)
        composeRule.onNodeWithTag("library-grid").assertIsDisplayed()

        pressAndroidBack()
        advanceBy(2_100)
        composeRule.onNodeWithTag("menu-open-pack").assertIsDisplayed()
    }

    @Test
    fun android_back_from_pack_selection_returns_to_extension_list_then_menu() {
        setAppContent(backNavigationTestAppContainer())
        loginAndReachMainMenu()

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
        loginAndReachMainMenu()

        composeRule.onNodeWithTag("menu-open-pack").performClick()
        advanceBy(1_900)
        composeRule.onNodeWithTag("pack-extension-enter-astronomes-en-herbe").performClick()
        advanceBy(1_900)
        composeRule.onNodeWithTag("pack-booster-0").performClick()
        advanceBy(6_400)
        composeRule.onNodeWithTag("pack-opening-card-id").assertIsDisplayed()

        pressAndroidBack()
        advanceBy(700)
        composeRule.onNodeWithTag("menu-open-pack").assertIsDisplayed()
    }

    @Test
    fun android_back_from_main_menu_finishes_activity() {
        setAppContent(backNavigationTestAppContainer())
        loginAndReachMainMenu()

        pressAndroidBack()
        composeRule.waitUntil(timeoutMillis = 5_000) { composeRule.activity.isFinishing }
        assertTrue(composeRule.activity.isFinishing)
    }

    private fun setAppContent(appContainer: AppContainer) {
        composeRule.mainClock.autoAdvance = false
        composeRule.setContent {
            GatchaTheme {
                GatchaApp(appContainer = appContainer)
            }
        }
        advanceBy(2_200)
    }

    private fun loginAndReachMainMenu() {
        composeRule.onNodeWithTag("login-username").performTextInput("alice")
        composeRule.onNodeWithTag("login-password").performTextInput("password")
        composeRule.onNodeWithTag("login-submit").performClick()
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
