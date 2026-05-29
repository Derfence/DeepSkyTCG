package fr.aumombelli.dstcg

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import fr.aumombelli.dstcg.testsupport.offlineMainActivityTestAppContainer
import org.junit.After
import org.junit.Rule
import org.junit.Test

class HomeScreenLaunchTest {
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
    fun home_screen_shows_logo_and_primary_actions_on_launch() {
        waitForHomeScreen()

        composeRule.onNodeWithTag("new-player-modal-welcome").assertIsDisplayed()
        composeRule.onNodeWithTag("home-logo-lockup").assertIsDisplayed()
        composeRule.onNodeWithTag("home-open-pack").assertIsDisplayed()
        composeRule.onAllNodesWithTag("home-library").assertCountEquals(0)
        composeRule.onAllNodesWithTag("home-equipment").assertCountEquals(0)
        composeRule.onAllNodesWithTag("home-badges").assertCountEquals(0)
        composeRule.onAllNodesWithTag("home-crafting").assertCountEquals(0)
        composeRule.onNodeWithTag("home-settings").assertIsDisplayed()
        composeRule.onAllNodesWithTag("start-footer-version").assertCountEquals(0)
        composeRule.onNodeWithTag("new-player-modal-finish").assertIsDisplayed()
    }

    private fun waitForHomeScreen() {
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodesWithTag("app-launch-logo")
                .fetchSemanticsNodes(atLeastOneRootRequired = false)
                .isNotEmpty()
        }
        composeRule.onNodeWithTag("app-launch-logo").assertIsDisplayed()
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodesWithTag("home-open-pack")
                .fetchSemanticsNodes(atLeastOneRootRequired = false)
                .isNotEmpty()
        }
        composeRule.onNodeWithTag("home-open-pack").assertIsDisplayed()
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodesWithTag("home-logo-lockup")
                .fetchSemanticsNodes(atLeastOneRootRequired = false)
                .isNotEmpty()
        }
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodesWithTag("app-launch-logo")
                .fetchSemanticsNodes(atLeastOneRootRequired = false)
                .isEmpty()
        }
    }

    @After
    fun tearDown() {
        MainActivity.appContainerFactory = null
    }
}
