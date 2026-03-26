package fr.aumombelli.gatcha

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeUp
import fr.aumombelli.gatcha.model.DrawPackResponse
import fr.aumombelli.gatcha.model.toDisplayCard
import fr.aumombelli.gatcha.model.toDisplayVariant
import fr.aumombelli.gatcha.ui.component.TRADING_CARD_WIDTH_OVER_HEIGHT
import fr.aumombelli.gatcha.ui.screen.PackOpeningScreen
import fr.aumombelli.gatcha.ui.viewmodel.PackOpeningUiState
import kotlin.math.abs
import org.junit.Assert.assertTrue
import org.junit.Assert.assertEquals
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
            availableDrawCount = 9,
            nextChargeAt = "2026-03-24T18:00:00Z",
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

        var doneCallCount = 0
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
                    highestBurstRarity = "Rare",
                    hasHolographicBurst = false,
                ),
                onDone = { doneCallCount += 1 },
            )
        }

        composeRule.mainClock.advanceTimeBy(1200)
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("pack-opening-booster").assertIsDisplayed()
        composeRule.assertApproxCardRatio("pack-opening-booster")
        composeRule.onAllNodesWithText("Booster").assertCountEquals(0)

        composeRule.mainClock.advanceTimeBy(1800)
        composeRule.mainClock.autoAdvance = true
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("pack-opening-burst").assertIsDisplayed()
        composeRule.onNodeWithTag("pack-opening-progress").assertIsDisplayed()
        composeRule.onNodeWithTag("pack-opening-card-id").assertTextContains("ALP-001")
        composeRule.onAllNodesWithTag("pack-opening-card-name").assertCountEquals(0)
        composeRule.assertApproxCardRatio("pack-opening-card-surface")
        composeRule.firstNodeWithTag("pack-opening-card-surface").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("astro-card-fullscreen-close").assertIsDisplayed()
        composeRule.onNodeWithTag("astro-card-fullscreen-close").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("pack-opening-card-id").assertTextContains("ALP-001")
        composeRule.firstNodeWithTag("pack-opening-card-surface").performTouchInput { swipeLeft() }
        composeRule.waitUntil(timeoutMillis = 5_000) {
            runCatching {
                composeRule.onNodeWithTag("pack-opening-card-id").assertTextContains("ALP-002")
                true
            }.getOrDefault(false)
        }
        composeRule.onNodeWithTag("pack-opening-card-id").assertTextContains("ALP-002")
        composeRule.firstNodeWithTag("pack-opening-card-surface").performTouchInput { swipeUp() }
        composeRule.waitUntil(timeoutMillis = 5_000) { doneCallCount == 1 }
        assertEquals(1, doneCallCount)
    }

    @Test
    fun pack_opening_holographic_burst_adds_falling_stars() {
        val holoCard = testCardDefinition("ALP-777", rarityLabel = "Epic")
        val packResult = DrawPackResponse(
            extensionId = "astronomes-en-herbe",
            drawnAt = "2026-03-23T12:00:00Z",
            availableDrawCount = 9,
            nextChargeAt = "2026-03-24T18:00:00Z",
            cards = listOf(
                testPackCard(
                    "ALP-777",
                    "Comete",
                    "Epic",
                    "comet",
                    finish = "holographic",
                    finishLabel = "Holographique",
                    isHolographic = true,
                ),
            ),
        )

        composeRule.mainClock.autoAdvance = false
        composeRule.setContent {
            PackOpeningScreen(
                state = PackOpeningUiState(
                    packResult = packResult,
                    displayCards = listOf(
                        holoCard.toDisplayCard(
                            extensionName = "Astronomes en herbe",
                            activeVariant = packResult.cards[0].variant.toDisplayVariant(),
                        ),
                    ),
                    highestBurstRarity = "Epic",
                    hasHolographicBurst = true,
                ),
                onDone = {},
            )
        }

        composeRule.mainClock.advanceTimeBy(700)
        composeRule.mainClock.autoAdvance = true
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("pack-opening-burst").assertIsDisplayed()
        composeRule.onNodeWithTag("pack-opening-burst-rain").assertIsDisplayed()
    }

    private fun androidx.compose.ui.test.junit4.ComposeContentTestRule.assertApproxCardRatio(
        tag: String,
        tolerance: Float = 0.03f,
    ) {
        val bounds = firstNodeWithTag(tag, useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        val actualRatio = bounds.width / bounds.height
        assertTrue(
            "Expected $tag width/height ratio near $TRADING_CARD_WIDTH_OVER_HEIGHT but was $actualRatio",
            abs(actualRatio - TRADING_CARD_WIDTH_OVER_HEIGHT) <= tolerance,
        )
    }

    private fun androidx.compose.ui.test.junit4.ComposeContentTestRule.firstNodeWithTag(
        tag: String,
        useUnmergedTree: Boolean = false,
    ) = onAllNodesWithTag(tag, useUnmergedTree = useUnmergedTree)[0]
}
