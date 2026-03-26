package fr.aumombelli.gatcha

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import fr.aumombelli.gatcha.testsupport.compatibleTestAppContainer
import org.junit.After
import org.junit.Rule
import org.junit.Test

class LoginScreenTest {
    init {
        MainActivity.appContainerFactory = { compatibleAppContainer() }
    }

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun login_screen_is_shown_on_launch() {
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodesWithTag("app-launch-logo")
                .fetchSemanticsNodes(atLeastOneRootRequired = false)
                .isNotEmpty()
        }
        composeRule.onNodeWithTag("app-launch-logo").assertIsDisplayed()
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodesWithTag("app-launch-login-form")
                .fetchSemanticsNodes(atLeastOneRootRequired = false)
                .isNotEmpty()
        }
        composeRule.onNodeWithTag("app-launch-login-form").assertIsDisplayed()
        composeRule.onNodeWithTag("login-submit").assertIsDisplayed()
        composeRule.onNodeWithTag("login-title").assertIsDisplayed()
    }

    @After
    fun tearDown() {
        MainActivity.appContainerFactory = null
    }

    private fun compatibleAppContainer(): AppContainer {
        return compatibleTestAppContainer()
    }
}
