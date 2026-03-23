package gatcha.aumombelli.fr

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import gatcha.aumombelli.fr.model.DrawPackResponse
import gatcha.aumombelli.fr.model.PackCard
import gatcha.aumombelli.fr.ui.screen.PackOpeningScreen
import gatcha.aumombelli.fr.ui.viewmodel.PackOpeningUiState
import org.junit.Rule
import org.junit.Test

class PackOpeningScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun pack_opening_reveals_cards_and_supports_swipe() {
        composeRule.mainClock.autoAdvance = false
        composeRule.setContent {
            PackOpeningScreen(
                state = PackOpeningUiState(
                    packResult = DrawPackResponse(
                        extensionId = "core-alpha",
                        drawnAt = "2026-03-23T12:00:00Z",
                        nextDrawAt = "2026-03-24T00:00:00Z",
                        cards = listOf(
                            PackCard("ALP-001", "Spark Fox", "Common", "spark_fox"),
                            PackCard("ALP-002", "Steam Golem", "Common", "steam_golem"),
                        ),
                    ),
                ),
                onDone = {},
            )
        }

        composeRule.mainClock.advanceTimeBy(1400)
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Spark Fox").assertIsDisplayed()
        composeRule.onRoot().performTouchInput { swipeLeft() }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Steam Golem").assertIsDisplayed()
    }
}
