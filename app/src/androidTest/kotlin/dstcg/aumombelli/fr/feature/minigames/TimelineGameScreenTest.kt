package fr.aumombelli.dstcg.feature.minigames

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import fr.aumombelli.dstcg.model.DisplayCard
import fr.aumombelli.dstcg.model.DisplayCardVariant
import fr.aumombelli.dstcg.testCardDefinition
import fr.aumombelli.dstcg.ui.theme.DstcgTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class TimelineGameScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun dragging_card_to_slot_requests_placement() {
        val firstCard = timelineCard("ALP-001", "M42")
        val secondCard = timelineCard("ALP-002", "M31")
        var placedCardId: String? = null
        var placedSlotIndex: Int? = null

        setTimelineContent(
            screen = MiniGamesScreenUiState.TimelinePlaying(
                criterionTitle = "Distance",
                instruction = "Classe les cartes.",
                rewardLabel = "1h",
                slots = listOf(
                    TimelineSlotUi(index = 0, placedCard = null),
                    TimelineSlotUi(index = 1, placedCard = null),
                ),
                handCards = listOf(firstCard, secondCard),
                canValidate = false,
                feedbackEvent = null,
            ),
            onPlaceCard = { cardId, slotIndex ->
                placedCardId = cardId
                placedSlotIndex = slotIndex
            },
        )

        val cardBounds = composeRule.onNodeWithTag(firstCard.testTag)
            .fetchSemanticsNode()
            .boundsInRoot
        val slotBounds = composeRule.onNodeWithTag("timeline-slot-0")
            .fetchSemanticsNode()
            .boundsInRoot
        val delta = Offset(
            x = slotBounds.center.x - cardBounds.center.x,
            y = slotBounds.center.y - cardBounds.center.y,
        )

        composeRule.onNodeWithTag(firstCard.testTag)
            .performTouchInput {
                down(center)
                moveBy(delta)
                up()
            }

        composeRule.runOnIdle {
            assertEquals(firstCard.id, placedCardId)
            assertEquals(0, placedSlotIndex)
        }
    }

    @Test
    fun validate_button_is_disabled_until_all_slots_are_filled() {
        val firstCard = timelineCard("ALP-001", "M42")
        val secondCard = timelineCard("ALP-002", "M31")

        setTimelineContent(
            screen = MiniGamesScreenUiState.TimelinePlaying(
                criterionTitle = "Distance",
                instruction = "Classe les cartes.",
                rewardLabel = "1h",
                slots = listOf(
                    TimelineSlotUi(index = 0, placedCard = firstCard),
                    TimelineSlotUi(index = 1, placedCard = null),
                ),
                handCards = listOf(secondCard),
                canValidate = false,
                feedbackEvent = null,
            ),
        )
        composeRule.onNodeWithTag("timeline-validate").assertIsNotEnabled()
    }

    @Test
    fun validate_button_is_enabled_when_all_slots_are_filled() {
        val firstCard = timelineCard("ALP-001", "M42")
        val secondCard = timelineCard("ALP-002", "M31")

        setTimelineContent(
            screen = MiniGamesScreenUiState.TimelinePlaying(
                criterionTitle = "Distance",
                instruction = "Classe les cartes.",
                rewardLabel = "1h",
                slots = listOf(
                    TimelineSlotUi(index = 0, placedCard = firstCard),
                    TimelineSlotUi(index = 1, placedCard = secondCard),
                ),
                handCards = emptyList(),
                canValidate = true,
                feedbackEvent = null,
            ),
        )
        composeRule.onNodeWithTag("timeline-validate").assertIsEnabled()
    }

    @Test
    fun result_displays_correction_and_correct_order() {
        val firstCard = timelineCard("ALP-001", "M42")
        val secondCard = timelineCard("ALP-002", "M31")

        setTimelineContent(
            screen = MiniGamesScreenUiState.TimelineResult(
                criterionTitle = "Distance",
                scoreLabel = "0/2",
                rewardLabel = "30min",
                slotResults = listOf(
                    TimelineSlotResultUi(
                        index = 0,
                        placedCard = secondCard,
                        correctCard = firstCard,
                        isCorrect = false,
                    ),
                    TimelineSlotResultUi(
                        index = 1,
                        placedCard = firstCard,
                        correctCard = secondCard,
                        isCorrect = false,
                    ),
                ),
                correctOrder = listOf(firstCard, secondCard),
                showCorrectOrder = true,
            ),
        )

        composeRule.onNodeWithTag("timeline-result").assertIsDisplayed()
        composeRule.onNodeWithTag("timeline-result-slot-0").assertIsDisplayed()
        composeRule.onNodeWithTag("timeline-result-slot-1").assertIsDisplayed()
        composeRule.onNodeWithTag("timeline-correct-order-title").assertIsDisplayed()
        composeRule.onNodeWithTag("timeline-correct-order-0").assertIsDisplayed()
    }

    private fun setTimelineContent(
        screen: MiniGamesScreenUiState,
        onPlaceCard: (String, Int) -> Unit = { _, _ -> },
    ) {
        composeRule.setContent {
            DstcgTheme {
                TimelineGameScreen(
                    state = MiniGamesUiState(
                        isLoading = false,
                        screen = screen,
                    ),
                    onBackToMenu = {},
                    onPlaceCard = onPlaceCard,
                    onValidate = {},
                )
            }
        }
    }

    private fun timelineCard(
        id: String,
        name: String,
    ): TimelineCardUi {
        val variant = DisplayCardVariant(
            skyQuality = "city",
            skyQualityLabel = "Ville",
            finish = "standard",
            finishLabel = "Standard",
            isHolographic = false,
        )
        return TimelineCardUi(
            id = id,
            displayCard = DisplayCard(
                definition = testCardDefinition(
                    id = id,
                    name = name,
                ),
                extensionName = "Alpha",
                activeVariant = variant,
            ),
            valueLabel = "$id valeur",
        )
    }
}
