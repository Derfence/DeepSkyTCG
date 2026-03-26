package fr.aumombelli.gatcha

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import fr.aumombelli.gatcha.testsupport.blockedTestAppContainer
import org.junit.Assert.assertEquals
import org.junit.After
import org.junit.Rule
import org.junit.Test

class AppCompatibilityBlockTest {
    init {
        MainActivity.appContainerFactory = { blockedAppContainer() }
    }

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun incompatible_startup_blocks_the_app() {
        composeRule.onNodeWithTag("app-bootstrap-message").assertIsDisplayed()
        composeRule.onNodeWithTag("app-bootstrap-retry").assertIsDisplayed()
        assertEquals(
            0,
            composeRule.onAllNodesWithTag("login-submit")
                .fetchSemanticsNodes(atLeastOneRootRequired = false)
                .size,
        )
    }

    @After
    fun tearDown() {
        MainActivity.appContainerFactory = null
    }

    private fun blockedAppContainer(): AppContainer {
        return blockedTestAppContainer()
    }
}
