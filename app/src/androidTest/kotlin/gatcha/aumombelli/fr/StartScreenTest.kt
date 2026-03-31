package fr.aumombelli.gatcha

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import fr.aumombelli.gatcha.testsupport.offlineMainActivityTestAppContainer
import org.junit.After
import org.junit.Rule
import org.junit.Test

class StartScreenTest {
    init {
        MainActivity.appContainerFactory = { context ->
            offlineMainActivityTestAppContainer(
                context = context,
                dataStoreFileName = "start-screen.preferences_pb",
            )
        }
    }

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun start_screen_shows_footer_on_launch() {
        waitForStartScreen()

        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodesWithTag("start-footer-version")
                .fetchSemanticsNodes(atLeastOneRootRequired = false)
                .isNotEmpty()
        }
        composeRule.onNodeWithTag("start-footer").assertIsDisplayed()
        composeRule.onNodeWithTag("start-footer-version").assertTextContains("v0.9.0")
        composeRule.onNodeWithTag("start-about-trigger").assertIsDisplayed()
    }

    private fun waitForStartScreen() {
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodesWithTag("app-launch-logo")
                .fetchSemanticsNodes(atLeastOneRootRequired = false)
                .isNotEmpty()
        }
        composeRule.onNodeWithTag("app-launch-logo").assertIsDisplayed()
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodesWithTag("app-launch-start-card")
                .fetchSemanticsNodes(atLeastOneRootRequired = false)
                .isNotEmpty()
        }
        composeRule.onNodeWithTag("app-launch-start-card").assertIsDisplayed()
        composeRule.onNodeWithTag("start-begin").assertIsDisplayed()
        composeRule.onNodeWithTag("start-title").assertIsDisplayed()
    }

    @After
    fun tearDown() {
        MainActivity.appContainerFactory = null
    }
}
