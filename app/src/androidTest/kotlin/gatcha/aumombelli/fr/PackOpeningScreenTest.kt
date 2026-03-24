package fr.aumombelli.gatcha

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import fr.aumombelli.gatcha.model.DrawPackResponse
import fr.aumombelli.gatcha.model.toDisplayCard
import fr.aumombelli.gatcha.model.toDisplayVariant
import fr.aumombelli.gatcha.ui.screen.PackOpeningScreen
import fr.aumombelli.gatcha.ui.viewmodel.PackOpeningUiState
import org.junit.Rule
import org.junit.Test

class PackOpeningScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun pack_opening_reveals_cards_supports_swipe_and_fullscreen() {
        val firstCard = testCardDefinition("ALP-001", name = "Nebuleuse d'Orion")
        val secondCard = testCardDefinition("ALP-002", name = "Galaxie d'Andromede")
        val packResult = DrawPackResponse(
            extensionId = "astronomes-en-herbe",
            drawnAt = "2026-03-23T12:00:00Z",
            nextDrawAt = "2026-03-24T00:00:00Z",
            cards = listOf(
                testPackCard("ALP-001", "Nebuleuse d'Orion", "Common", "spark_fox"),
                testPackCard(
                    "ALP-002",
                    "Galaxie d'Andromede",
                    "Rare",
                    "steam_golem",
                    skyQuality = "rural",
                    skyQualityLabel = "Campagne",
                ),
            ),
        )

        composeRule.mainClock.autoAdvance = false
        composeRule.setContent {
            PackOpeningScreen(
                state = PackOpeningUiState(
                    packResult = packResult,
                    displayCards = listOf(
                        firstCard.toDisplayCard(
                            extensionName = "Astronomes en herbe",
                            activeVariant = packResult.cards[0].variant.toDisplayVariant(),
                        ),
                        secondCard.toDisplayCard(
                            extensionName = "Astronomes en herbe",
                            activeVariant = packResult.cards[1].variant.toDisplayVariant(),
                        ),
                    ),
                ),
                onDone = {},
            )
        }

        composeRule.mainClock.advanceTimeBy(1400)
        composeRule.mainClock.autoAdvance = true
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("pack-opening-card-name").assertTextContains("Nebuleuse d'Orion")
        composeRule.onNodeWithTag("pack-opening-card-surface").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("astro-card-fullscreen-close").assertIsDisplayed()
        composeRule.onNodeWithTag("astro-card-fullscreen-close").performClick()
        composeRule.waitForIdle()
        composeRule.onRoot().performTouchInput { swipeLeft() }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("pack-opening-card-name").assertTextContains("Galaxie d'Andromede")
    }
}
