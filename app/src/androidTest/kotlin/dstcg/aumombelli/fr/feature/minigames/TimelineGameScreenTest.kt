package fr.aumombelli.dstcg.feature.minigames

import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import fr.aumombelli.dstcg.model.DisplayCard
import fr.aumombelli.dstcg.model.DisplayCardVariant
import fr.aumombelli.dstcg.model.MiniGameDifficulty
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
    fun difficulty_selection_displays_choices_and_requests_selection() {
        var selectedDifficulty: MiniGameDifficulty? = null

        setTimelineContent(
            screen = MiniGamesScreenUiState.TimelineDifficultySelection,
            timelineDifficultyChoices = listOf(
                TimelineDifficultyChoiceUi(
                    difficulty = MiniGameDifficulty.Apprentice,
                    title = "Apprenti",
                    comparisonLabel = "1 comparaison",
                    rewardLabel = "15min",
                    enabled = true,
                    locked = false,
                    statusLabel = "Disponible",
                ),
            ),
            onSelectDifficulty = { selectedDifficulty = it },
        )

        composeRule.onNodeWithTag("timeline-difficulty-selection").assertIsDisplayed()
        composeRule.onNodeWithTag("timeline-difficulty-apprentice")
            .assertIsDisplayed()
            .assertIsEnabled()
            .performClick()

        composeRule.runOnIdle {
            assertEquals(MiniGameDifficulty.Apprentice, selectedDifficulty)
        }
    }

    @Test
    fun dragging_card_to_slot_requests_placement() {
        val firstCard = timelineCard("ALP-001", "M42")
        val secondCard = timelineCard("ALP-002", "M31")
        var placedCardId: String? = null
        var placedSlotIndex: Int? = null

        setTimelineContent(
            screen = playingScreen(
                slots = listOf(
                    TimelineSlotUi(index = 0, placedCard = null, emptyLabel = "La plus proche"),
                    TimelineSlotUi(index = 1, placedCard = null, emptyLabel = "La plus lointaine"),
                ),
                handCards = listOf(firstCard, secondCard),
            ),
            onPlaceCard = { cardId, slotIndex ->
                placedCardId = cardId
                placedSlotIndex = slotIndex
            },
        )

        composeRule.onNodeWithTag("timeline-comparison-board").assertIsDisplayed()
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
            screen = playingScreen(
                slots = listOf(
                    TimelineSlotUi(index = 0, placedCard = null, emptyLabel = "La plus proche"),
                    TimelineSlotUi(index = 1, placedCard = null, emptyLabel = "La plus lointaine"),
                ),
                handCards = listOf(firstCard, secondCard),
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
    fun validate_button_is_hidden_until_both_slots_are_filled() {
        val firstCard = timelineCard("ALP-001", "M42")
        val secondCard = timelineCard("ALP-002", "M31")

        setTimelineContent(
            screen = playingScreen(
                slots = listOf(
                    TimelineSlotUi(index = 0, placedCard = firstCard),
                    TimelineSlotUi(index = 1, placedCard = null),
                ),
                handCards = listOf(secondCard),
                handSlots = listOf(null, secondCard),
                canValidate = false,
            ),
        )
        composeRule.onAllNodesWithTag("timeline-validate").assertCountEquals(0)
    }

    @Test
    fun validate_button_is_enabled_when_both_slots_are_filled() {
        val firstCard = timelineCard("ALP-001", "M42")
        val secondCard = timelineCard("ALP-002", "M31")

        setTimelineContent(
            screen = playingScreen(
                slots = listOf(
                    TimelineSlotUi(index = 0, placedCard = firstCard),
                    TimelineSlotUi(index = 1, placedCard = secondCard),
                ),
                handCards = emptyList(),
                handSlots = listOf(null, null),
                canValidate = true,
            ),
        )
        composeRule.onNodeWithTag("timeline-validate")
            .assertIsDisplayed()
            .assertIsEnabled()
    }

    @Test
    fun two_slots_and_two_cards_fit_the_available_width() {
        val firstCard = timelineCard("ALP-001", "M42")
        val secondCard = timelineCard("ALP-002", "M31")

        setTimelineContent(
            screen = playingScreen(
                slots = listOf(
                    TimelineSlotUi(index = 0, placedCard = null),
                    TimelineSlotUi(index = 1, placedCard = null),
                ),
                handCards = listOf(firstCard, secondCard),
            ),
            modifier = Modifier
                .width(360.dp)
                .height(760.dp),
        )

        val boardBounds = composeRule.onNodeWithTag("timeline-comparison-board")
            .fetchSemanticsNode()
            .boundsInRoot
        val firstSlotBounds = composeRule.onNodeWithTag("timeline-slot-0")
            .fetchSemanticsNode()
            .boundsInRoot
        val secondSlotBounds = composeRule.onNodeWithTag("timeline-slot-1")
            .fetchSemanticsNode()
            .boundsInRoot
        val firstHandBounds = composeRule.onNodeWithTag("timeline-hand-slot-0")
            .fetchSemanticsNode()
            .boundsInRoot
        val secondHandBounds = composeRule.onNodeWithTag("timeline-hand-slot-1")
            .fetchSemanticsNode()
            .boundsInRoot

        assertTrue(firstSlotBounds.left >= boardBounds.left)
        assertTrue(secondSlotBounds.right <= boardBounds.right)
        assertTrue(firstHandBounds.left >= boardBounds.left)
        assertTrue(secondHandBounds.right <= boardBounds.right)
        assertTrue(firstSlotBounds.right < secondSlotBounds.left)
        assertTrue(firstHandBounds.right < secondHandBounds.left)
    }

    @Test
    fun playable_cards_fill_their_slot_without_outer_frame() {
        val firstCard = timelineCard("ALP-001", "M42")
        val secondCard = timelineCard("ALP-002", "M31")

        setTimelineContent(
            screen = playingScreen(
                slots = listOf(
                    TimelineSlotUi(index = 0, placedCard = firstCard, emptyLabel = "La plus proche"),
                    TimelineSlotUi(index = 1, placedCard = null, emptyLabel = "La plus lointaine"),
                ),
                handCards = listOf(secondCard),
                handSlots = listOf(null, secondCard),
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
    fun dragging_placed_card_to_another_slot_requests_placement() {
        val firstCard = timelineCard("ALP-001", "M42")
        val secondCard = timelineCard("ALP-002", "M31")
        var placedCardId: String? = null
        var placedSlotIndex: Int? = null

        setTimelineContent(
            screen = playingScreen(
                slots = listOf(
                    TimelineSlotUi(index = 0, placedCard = firstCard, emptyLabel = "La plus proche"),
                    TimelineSlotUi(index = 1, placedCard = null, emptyLabel = "La plus lointaine"),
                ),
                handCards = listOf(secondCard),
                handSlots = listOf(null, secondCard),
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
    fun result_displays_score_reward_and_corrections() {
        val firstCard = timelineCard("ALP-001", "M42")
        val secondCard = timelineCard("ALP-002", "M31")

        setTimelineContent(
            screen = MiniGamesScreenUiState.TimelineResult(
                difficultyName = "Observateur",
                criterionTitle = "Distance",
                scoreLabel = "0/1",
                rewardLabel = "15min",
                corrections = listOf(
                    TimelineComparisonResultUi(
                        index = 0,
                        firstSlotLabel = "La plus proche",
                        lastSlotLabel = "La plus lointaine",
                        placedCards = listOf(secondCard, firstCard),
                        correctCards = listOf(firstCard, secondCard),
                        isCorrect = false,
                    ),
                ),
                nextDifficultyName = null,
            ),
        )

        composeRule.onNodeWithTag("timeline-result").assertIsDisplayed()
        composeRule.onNodeWithTag("timeline-result-comparison-0").assertIsDisplayed()
        composeRule.onNodeWithText("0/1").assertIsDisplayed()
        composeRule.onNodeWithText("15min").assertIsDisplayed()
    }

    private fun playingScreen(
        slots: List<TimelineSlotUi>,
        handCards: List<TimelineCardUi>,
        handSlots: List<TimelineCardUi?> = handCards,
        canValidate: Boolean = false,
    ): MiniGamesScreenUiState.TimelinePlaying =
        MiniGamesScreenUiState.TimelinePlaying(
            difficultyName = "Apprenti",
            criterionTitle = "Distance",
            instruction = "Compare les deux cartes.",
            rewardLabel = "15min",
            comparisonIndex = 0,
            comparisonCount = 1,
            score = 0,
            slots = slots,
            handCards = handCards,
            handSlots = handSlots,
            canValidate = canValidate,
            feedbackEvent = null,
        )

    private fun setTimelineContent(
        screen: MiniGamesScreenUiState,
        timelineDifficultyChoices: List<TimelineDifficultyChoiceUi> = emptyList(),
        onSelectDifficulty: (MiniGameDifficulty) -> Unit = {},
        onPlaceCard: (String, Int) -> Unit = { _, _ -> },
        onReturnCardToHand: (String, Int) -> Unit = { _, _ -> },
        modifier: Modifier = Modifier,
    ) {
        composeRule.setContent {
            DstcgTheme {
                TimelineGameScreen(
                    state = MiniGamesUiState(
                        isLoading = false,
                        timelineDifficultyChoices = timelineDifficultyChoices,
                        screen = screen,
                    ),
                    onBackToMenu = {},
                    onSelectDifficulty = onSelectDifficulty,
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
