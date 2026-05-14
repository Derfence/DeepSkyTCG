package fr.aumombelli.dstcg.feature.minigames

import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import fr.aumombelli.dstcg.model.DisplayCard
import fr.aumombelli.dstcg.model.DisplayCardVariant
import fr.aumombelli.dstcg.testCardDefinition
import fr.aumombelli.dstcg.ui.theme.DstcgTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import kotlin.math.abs

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
                    TimelineSlotUi(index = 0, placedCard = null, emptyLabel = "La plus proche"),
                    TimelineSlotUi(index = 1, placedCard = null, emptyLabel = "La plus lointaine"),
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

        composeRule.onNodeWithTag("timeline-horizontal-scroll").assertIsDisplayed()
        composeRule.onNodeWithTag("timeline-hand").assertIsDisplayed()
        composeRule.onNodeWithText("La plus proche").fetchSemanticsNode()
        composeRule.onNodeWithText("La plus lointaine").fetchSemanticsNode()

        val cardBounds = composeRule.onNodeWithTag(firstCard.testTag)
            .fetchSemanticsNode()
            .boundsInRoot
        val slotBounds = composeRule.onNodeWithTag("timeline-slot-0")
            .fetchSemanticsNode()
            .boundsInRoot
        val dragAnchor = Offset(
            x = cardBounds.width / 2f,
            y = cardBounds.height * 0.25f,
        )
        val delta = Offset(
            x = slotBounds.center.x - cardBounds.center.x,
            y = slotBounds.center.y - cardBounds.center.y,
        )

        composeRule.onNodeWithTag(firstCard.testTag)
            .performTouchInput {
                down(dragAnchor)
                moveBy(delta)
                up()
            }

        composeRule.runOnIdle {
            assertEquals(firstCard.id, placedCardId)
            assertEquals(0, placedSlotIndex)
        }
    }

    @Test
    fun dropping_card_center_below_slot_does_not_request_placement() {
        val firstCard = timelineCard("ALP-001", "M42")
        val secondCard = timelineCard("ALP-002", "M31")
        var placedCardId: String? = null

        setTimelineContent(
            screen = MiniGamesScreenUiState.TimelinePlaying(
                criterionTitle = "Distance",
                instruction = "Classe les cartes.",
                rewardLabel = "1h",
                slots = listOf(
                    TimelineSlotUi(index = 0, placedCard = null, emptyLabel = "La plus proche"),
                    TimelineSlotUi(index = 1, placedCard = null, emptyLabel = "La plus lointaine"),
                ),
                handCards = listOf(firstCard, secondCard),
                canValidate = false,
                feedbackEvent = null,
            ),
            onPlaceCard = { cardId, _ ->
                placedCardId = cardId
            },
        )

        val cardBounds = composeRule.onNodeWithTag(firstCard.testTag)
            .fetchSemanticsNode()
            .boundsInRoot
        val slotBounds = composeRule.onNodeWithTag("timeline-slot-0")
            .fetchSemanticsNode()
            .boundsInRoot
        val dragAnchor = Offset(
            x = cardBounds.width / 2f,
            y = cardBounds.height * 0.25f,
        )
        val delta = Offset(
            x = slotBounds.center.x - cardBounds.center.x,
            y = (slotBounds.bottom + (cardBounds.height * 0.25f)) - cardBounds.center.y,
        )

        composeRule.onNodeWithTag(firstCard.testTag)
            .performTouchInput {
                down(dragAnchor)
                moveBy(delta)
                up()
            }

        composeRule.runOnIdle {
            assertEquals(null, placedCardId)
        }
    }

    @Test
    fun validate_button_is_hidden_until_all_slots_are_filled() {
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
        composeRule.onAllNodesWithTag("timeline-validate").assertCountEquals(0)
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
        composeRule.onNodeWithTag("timeline-validate")
            .assertIsDisplayed()
            .assertIsEnabled()
    }

    @Test
    fun timeline_board_keeps_same_bounds_when_validate_button_appears() {
        val firstCard = timelineCard("ALP-001", "M42")
        val secondCard = timelineCard("ALP-002", "M31")
        var screen by mutableStateOf(
            MiniGamesScreenUiState.TimelinePlaying(
                criterionTitle = "Distance",
                instruction = "Classe les cartes.",
                rewardLabel = "1h",
                slots = listOf(
                    TimelineSlotUi(index = 0, placedCard = firstCard),
                    TimelineSlotUi(index = 1, placedCard = null),
                ),
                handCards = listOf(secondCard),
                handSlots = listOf(null, secondCard),
                canValidate = false,
                feedbackEvent = null,
            ),
        )

        composeRule.setContent {
            DstcgTheme {
                TimelineGameScreen(
                    state = MiniGamesUiState(
                        isLoading = false,
                        screen = screen,
                    ),
                    onBackToMenu = {},
                    onPlaceCard = { _, _ -> },
                    onReturnCardToHand = { _, _ -> },
                    onValidate = {},
                )
            }
        }

        val beforeBounds = composeRule.onNodeWithTag("timeline-horizontal-scroll")
            .fetchSemanticsNode()
            .boundsInRoot

        composeRule.runOnIdle {
            screen = MiniGamesScreenUiState.TimelinePlaying(
                criterionTitle = "Distance",
                instruction = "Classe les cartes.",
                rewardLabel = "1h",
                slots = listOf(
                    TimelineSlotUi(index = 0, placedCard = firstCard),
                    TimelineSlotUi(index = 1, placedCard = secondCard),
                ),
                handCards = emptyList(),
                handSlots = listOf(null, null),
                canValidate = true,
                feedbackEvent = null,
            )
        }
        composeRule.waitForIdle()

        val afterBounds = composeRule.onNodeWithTag("timeline-horizontal-scroll")
            .fetchSemanticsNode()
            .boundsInRoot

        assertTrue(abs(beforeBounds.top - afterBounds.top) < 1f)
        assertTrue(abs(beforeBounds.bottom - afterBounds.bottom) < 1f)
        composeRule.onNodeWithTag("timeline-validate").assertIsDisplayed()
    }

    @Test
    fun central_empty_slots_do_not_display_text() {
        setTimelineContent(
            screen = MiniGamesScreenUiState.TimelinePlaying(
                criterionTitle = "Distance",
                instruction = "Classe les cartes.",
                rewardLabel = "1h",
                slots = listOf(
                    TimelineSlotUi(index = 0, placedCard = null, emptyLabel = "La plus proche"),
                    TimelineSlotUi(index = 1, placedCard = null),
                    TimelineSlotUi(index = 2, placedCard = null),
                    TimelineSlotUi(index = 3, placedCard = null),
                    TimelineSlotUi(index = 4, placedCard = null, emptyLabel = "La plus lointaine"),
                ),
                handCards = emptyList(),
                canValidate = false,
                feedbackEvent = null,
            ),
        )

        composeRule.onNodeWithText("La plus proche").fetchSemanticsNode()
        composeRule.onNodeWithText("La plus lointaine").fetchSemanticsNode()
        composeRule.onAllNodesWithText("Emplacement 2").assertCountEquals(0)
        composeRule.onAllNodesWithText("Emplacement 3").assertCountEquals(0)
        composeRule.onAllNodesWithText("Emplacement 4").assertCountEquals(0)
        composeRule.onAllNodesWithText("1").assertCountEquals(0)
        composeRule.onAllNodesWithText("2").assertCountEquals(0)
        composeRule.onAllNodesWithText("3").assertCountEquals(0)
        composeRule.onAllNodesWithText("4").assertCountEquals(0)
        composeRule.onAllNodesWithText("5").assertCountEquals(0)
    }

    @Test
    fun playable_cards_fill_their_timeline_slot_without_outer_frame() {
        val firstCard = timelineCard("ALP-001", "M42")
        val secondCard = timelineCard("ALP-002", "M31")

        setTimelineContent(
            screen = MiniGamesScreenUiState.TimelinePlaying(
                criterionTitle = "Distance",
                instruction = "Classe les cartes.",
                rewardLabel = "1h",
                slots = listOf(
                    TimelineSlotUi(index = 0, placedCard = firstCard, emptyLabel = "La plus proche"),
                    TimelineSlotUi(index = 1, placedCard = null, emptyLabel = "La plus lointaine"),
                ),
                handCards = listOf(secondCard),
                handSlots = listOf(null, secondCard),
                canValidate = false,
                feedbackEvent = null,
            ),
        )

        val slotBounds = composeRule.onNodeWithTag("timeline-slot-0")
            .fetchSemanticsNode()
            .boundsInRoot
        val placedPreviewBounds = composeRule.onNodeWithTag("timeline-card-preview-${firstCard.id}")
            .fetchSemanticsNode()
            .boundsInRoot
        val handSlotBounds = composeRule.onNodeWithTag("timeline-hand-slot-1")
            .fetchSemanticsNode()
            .boundsInRoot
        val handPreviewBounds = composeRule.onNodeWithTag("timeline-card-preview-${secondCard.id}")
            .fetchSemanticsNode()
            .boundsInRoot

        assertBoundsMatch(slotBounds, placedPreviewBounds)
        assertBoundsMatch(handSlotBounds, handPreviewBounds)
    }

    @Test
    fun hand_card_positions_stay_stable_when_a_card_is_already_placed() {
        val firstCard = timelineCard("ALP-001", "M42")
        val secondCard = timelineCard("ALP-002", "M31")

        setTimelineContent(
            screen = MiniGamesScreenUiState.TimelinePlaying(
                criterionTitle = "Distance",
                instruction = "Classe les cartes.",
                rewardLabel = "1h",
                slots = listOf(
                    TimelineSlotUi(index = 0, placedCard = firstCard, emptyLabel = "La plus proche"),
                    TimelineSlotUi(index = 1, placedCard = null, emptyLabel = "La plus lointaine"),
                ),
                handCards = listOf(secondCard),
                handSlots = listOf(null, secondCard),
                canValidate = false,
                feedbackEvent = null,
            ),
        )

        val firstSlotBounds = composeRule.onNodeWithTag("timeline-slot-0")
            .fetchSemanticsNode()
            .boundsInRoot
        val secondHandCardBounds = composeRule.onNodeWithTag(secondCard.testTag)
            .fetchSemanticsNode()
            .boundsInRoot

        assertTrue(secondHandCardBounds.left > firstSlotBounds.right)
    }

    @Test
    fun dragging_placed_card_to_another_slot_requests_placement() {
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
                    TimelineSlotUi(index = 0, placedCard = firstCard, emptyLabel = "La plus proche"),
                    TimelineSlotUi(index = 1, placedCard = null, emptyLabel = "La plus lointaine"),
                ),
                handCards = listOf(secondCard),
                handSlots = listOf(null, secondCard),
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
        val targetSlotBounds = composeRule.onNodeWithTag("timeline-slot-1")
            .fetchSemanticsNode()
            .boundsInRoot
        val delta = Offset(
            x = targetSlotBounds.center.x - cardBounds.center.x,
            y = targetSlotBounds.center.y - cardBounds.center.y,
        )

        composeRule.onNodeWithTag(firstCard.testTag)
            .performTouchInput {
                down(center)
                moveBy(delta)
                up()
            }

        composeRule.runOnIdle {
            assertEquals(firstCard.id, placedCardId)
            assertEquals(1, placedSlotIndex)
        }
    }

    @Test
    fun dragging_placed_card_to_empty_hand_slot_requests_return_to_hand() {
        val firstCard = timelineCard("ALP-001", "M42")
        val secondCard = timelineCard("ALP-002", "M31")
        var returnedCardId: String? = null
        var returnedHandSlotIndex: Int? = null

        setTimelineContent(
            screen = MiniGamesScreenUiState.TimelinePlaying(
                criterionTitle = "Distance",
                instruction = "Classe les cartes.",
                rewardLabel = "1h",
                slots = listOf(
                    TimelineSlotUi(index = 0, placedCard = firstCard, emptyLabel = "La plus proche"),
                    TimelineSlotUi(index = 1, placedCard = null, emptyLabel = "La plus lointaine"),
                ),
                handCards = listOf(secondCard),
                handSlots = listOf(null, secondCard),
                canValidate = false,
                feedbackEvent = null,
            ),
            onReturnCardToHand = { cardId, handSlotIndex ->
                returnedCardId = cardId
                returnedHandSlotIndex = handSlotIndex
            },
        )

        val cardBounds = composeRule.onNodeWithTag(firstCard.testTag)
            .fetchSemanticsNode()
            .boundsInRoot
        val handSlotBounds = composeRule.onNodeWithTag("timeline-hand-slot-0")
            .fetchSemanticsNode()
            .boundsInRoot
        val delta = Offset(
            x = handSlotBounds.center.x - cardBounds.center.x,
            y = handSlotBounds.center.y - cardBounds.center.y,
        )

        composeRule.onNodeWithTag(firstCard.testTag)
            .performTouchInput {
                down(center)
                moveBy(delta)
                up()
            }

        composeRule.runOnIdle {
            assertEquals(firstCard.id, returnedCardId)
            assertEquals(0, returnedHandSlotIndex)
        }
    }

    @Test
    fun timeline_content_is_centered_when_full_cards_fit_vertically() {
        val firstCard = timelineCard("ALP-001", "M42")
        val secondCard = timelineCard("ALP-002", "M31")

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
            modifier = Modifier
                .width(900.dp)
                .height(1300.dp),
        )

        val railBounds = composeRule.onNodeWithTag("timeline-horizontal-scroll")
            .fetchSemanticsNode()
            .boundsInRoot
        val slotBounds = composeRule.onNodeWithTag("timeline-slot-0")
            .fetchSemanticsNode()
            .boundsInRoot
        val handCardBounds = composeRule.onNodeWithTag(firstCard.testTag)
            .fetchSemanticsNode()
            .boundsInRoot
        val contentCenterY = (slotBounds.top + handCardBounds.bottom) / 2f

        assertTrue(handCardBounds.bottom <= railBounds.bottom)
        assertTrue(abs(contentCenterY - railBounds.center.y) < 24f)
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
        onReturnCardToHand: (String, Int) -> Unit = { _, _ -> },
        modifier: Modifier = Modifier,
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
                    onReturnCardToHand = onReturnCardToHand,
                    onValidate = {},
                    modifier = modifier,
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

    private fun assertBoundsMatch(
        expected: Rect,
        actual: Rect,
    ) {
        val tolerancePx = 8f
        assertTrue("left differs by ${abs(expected.left - actual.left)}px", abs(expected.left - actual.left) <= tolerancePx)
        assertTrue("top differs by ${abs(expected.top - actual.top)}px", abs(expected.top - actual.top) <= tolerancePx)
        assertTrue("right differs by ${abs(expected.right - actual.right)}px", abs(expected.right - actual.right) <= tolerancePx)
        assertTrue("bottom differs by ${abs(expected.bottom - actual.bottom)}px", abs(expected.bottom - actual.bottom) <= tolerancePx)
    }
}
