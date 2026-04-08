package fr.aumombelli.dstcg

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import fr.aumombelli.dstcg.testsupport.offlineMainActivityTestAppContainer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class HomeScreenResponsiveLaunchTest {
    init {
        MainActivity.appContainerFactory = { context ->
            offlineMainActivityTestAppContainer(
                context = context,
                dataStoreFileName = "home-responsive-launch.preferences_pb",
            )
        }
    }

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun home_screen_centers_pack_card_between_logo_and_menu_buttons_after_launch_transition() {
        waitForHomeScreen()

        val logoBounds = composeRule.onNodeWithTag("home-logo-lockup").fetchSemanticsNode().boundsInRoot
        val packBounds = composeRule.onNodeWithTag("home-open-pack").fetchSemanticsNode().boundsInRoot
        val libraryBounds = composeRule.onNodeWithTag("home-library").fetchSemanticsNode().boundsInRoot

        val expectedPackCenterY = (logoBounds.bottom + libraryBounds.top) / 2f
        val actualPackCenterY = (packBounds.top + packBounds.bottom) / 2f

        assertEquals(expectedPackCenterY, actualPackCenterY, 4f)
        composeRule.onNodeWithTag("home-open-pack").assertIsDisplayed()
    }

    private fun waitForHomeScreen() {
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodesWithTag("app-launch-logo")
                .fetchSemanticsNodes(atLeastOneRootRequired = false)
                .isNotEmpty()
        }
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodesWithTag("home-open-pack")
                .fetchSemanticsNodes(atLeastOneRootRequired = false)
                .isNotEmpty()
        }
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
