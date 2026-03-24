package fr.aumombelli.gatcha

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import fr.aumombelli.gatcha.model.CardVariant
import fr.aumombelli.gatcha.model.DrawPackResponse
import fr.aumombelli.gatcha.model.PackCard
import fr.aumombelli.gatcha.ui.screen.PackOpeningScreen
import fr.aumombelli.gatcha.ui.viewmodel.PackOpeningUiState
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
                            PackCard(
                                cardId = "ALP-001",
                                name = "Nebuleuse d'Orion",
                                rarityLabel = "Common",
                                imageRef = "spark_fox",
                                variant = CardVariant("city", "Ville", "standard", "Standard", false),
                            ),
                            PackCard(
                                cardId = "ALP-002",
                                name = "Galaxie d'Andromede",
                                rarityLabel = "Common",
                                imageRef = "steam_golem",
                                variant = CardVariant("city", "Ville", "standard", "Standard", false),
                            ),
                        ),
                    ),
                ),
                onDone = {},
            )
        }

        composeRule.mainClock.advanceTimeBy(1400)
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Nebuleuse d'Orion").assertIsDisplayed()
        composeRule.onRoot().performTouchInput { swipeLeft() }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Galaxie d'Andromede").assertIsDisplayed()
    }
}
