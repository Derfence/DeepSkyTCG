package fr.aumombelli.dstcg

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.espresso.Espresso
import fr.aumombelli.dstcg.app.NewPlayerBlockingModal
import fr.aumombelli.dstcg.app.NewPlayerBlockingModalPage
import org.junit.Rule
import org.junit.Test

class NewPlayerBlockingModalTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun android_back_returns_to_previous_page_in_multi_page_modal() {
        composeRule.setContent {
            Box(modifier = Modifier.fillMaxSize()) {
                NewPlayerBlockingModal(
                    testTag = "new-player-modal-library-variants",
                    pages = listOf(
                        NewPlayerBlockingModalPage("Page 1", "Message 1"),
                        NewPlayerBlockingModalPage("Page 2", "Message 2"),
                        NewPlayerBlockingModalPage("Page 3", "Message 3"),
                        NewPlayerBlockingModalPage("Page 4", "Message 4"),
                    ),
                    finishButtonLabel = "Terminer",
                    onFinished = {},
                )
            }
        }

        composeRule.onNodeWithTag("new-player-modal-page-0").assertIsDisplayed()
        composeRule.onNodeWithTag("new-player-modal-next").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("new-player-modal-page-1").assertIsDisplayed()

        pressAndroidBack()

        composeRule.onNodeWithTag("new-player-modal-page-0").assertIsDisplayed()
        composeRule.onAllNodesWithTag("new-player-modal-page-1").assertCountEquals(0)
    }

    private fun pressAndroidBack() {
        Espresso.pressBack()
        composeRule.waitForIdle()
    }
}
