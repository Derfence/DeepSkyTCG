package fr.aumombelli.dstcg

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import androidx.compose.ui.test.swipeUp
import fr.aumombelli.dstcg.feature.start.StartScreen
import fr.aumombelli.dstcg.feature.start.StartUiState
import fr.aumombelli.dstcg.ui.theme.DstcgTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class StartScreenStateTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun loading_and_transitioning_states_disable_begin_action() {
        val state = setStartScreenContent(
            StartUiState(
                isLoading = true,
            ),
        )

        composeRule.onNodeWithTag("start-begin").assertIsNotEnabled()

        composeRule.runOnIdle {
            state.value = StartUiState(
                isLoading = false,
                isTransitioningToMenu = true,
            )
        }

        composeRule.onNodeWithTag("start-begin").assertIsNotEnabled()
    }

    @Test
    fun error_state_is_displayed_and_prevents_begin() {
        setStartScreenContent(
            StartUiState(
                isLoading = false,
                errorMessage = "Saved progression could not be read.",
            ),
        )

        composeRule.onNodeWithTag("start-error").assertIsDisplayed()
        composeRule.onNodeWithTag("start-begin").assertIsNotEnabled()
    }

    @Test
    fun reset_action_is_always_visible() {
        setStartScreenContent(
            StartUiState(
                isLoading = false,
            ),
        )

        composeRule.onNodeWithTag("start-reset-progress").assertIsDisplayed().assertIsEnabled()
    }

    @Test
    fun about_sheet_opens_with_swipe_up_on_about_trigger() {
        setStartScreenContent(
            StartUiState(
                isLoading = false,
            ),
        )

        composeRule.onNodeWithTag("start-about-trigger").performTouchInput { swipeUp() }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("start-about-sheet").assertIsDisplayed()
    }

    @Test
    fun about_sheet_opens_with_tap_on_about_trigger() {
        setStartScreenContent(
            StartUiState(
                isLoading = false,
            ),
        )

        composeRule.onNodeWithTag("start-about-trigger").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("start-about-sheet").assertIsDisplayed()
    }

    @Test
    fun about_sheet_closes_with_swipe_down_on_header() {
        setStartScreenContent(
            StartUiState(
                isLoading = false,
            ),
        )

        composeRule.onNodeWithTag("start-about-trigger").performTouchInput { swipeUp() }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("start-about-sheet").assertIsDisplayed()

        composeRule.onNodeWithTag("start-about-sheet-header").performTouchInput { swipeDown() }
        composeRule.waitForIdle()
        composeRule.onAllNodesWithTag("start-about-sheet").assertCountEquals(0)
    }

    @Test
    fun reset_confirmation_requires_delay_before_validation() {
        composeRule.mainClock.autoAdvance = false
        setStartScreenContent(
            StartUiState(
                isLoading = false,
            ),
        )

        composeRule.onNodeWithTag("start-reset-progress").performClick()
        composeRule.mainClock.advanceTimeBy(1)
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("start-reset-confirmation").assertIsDisplayed()
        composeRule.onNodeWithTag("start-reset-confirmation-confirm").assertIsNotEnabled()

        composeRule.mainClock.advanceTimeBy(1_999)
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("start-reset-confirmation-confirm").assertIsNotEnabled()

        composeRule.mainClock.advanceTimeBy(1)
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("start-reset-confirmation-confirm").assertIsEnabled()
        composeRule.mainClock.autoAdvance = true
    }

    @Test
    fun cancelling_reset_confirmation_keeps_callback_uninvoked() {
        var resetCount = 0
        setStartScreenContent(
            initialState = StartUiState(
                isLoading = false,
            ),
            onResetProgress = { resetCount += 1 },
        )

        composeRule.onNodeWithTag("start-reset-progress").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("start-reset-confirmation-cancel").performClick()
        composeRule.waitForIdle()

        composeRule.onAllNodesWithTag("start-reset-confirmation").assertCountEquals(0)
        composeRule.runOnIdle {
            assertEquals(0, resetCount)
        }
    }

    @Test
    fun confirming_reset_calls_callback_once() {
        var resetCount = 0
        composeRule.mainClock.autoAdvance = false
        setStartScreenContent(
            initialState = StartUiState(
                isLoading = false,
            ),
            onResetProgress = { resetCount += 1 },
        )

        composeRule.onNodeWithTag("start-reset-progress").performClick()
        composeRule.mainClock.advanceTimeBy(1)
        composeRule.waitForIdle()
        composeRule.mainClock.advanceTimeBy(2_000)
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("start-reset-confirmation-confirm").performClick()
        composeRule.mainClock.advanceTimeBy(1)
        composeRule.waitForIdle()
        composeRule.mainClock.autoAdvance = true

        composeRule.onAllNodesWithTag("start-reset-confirmation").assertCountEquals(0)
        composeRule.runOnIdle {
            assertEquals(1, resetCount)
        }
    }

    private fun setStartScreenContent(
        initialState: StartUiState,
        onResetProgress: () -> Unit = {},
    ): MutableState<StartUiState> {
        val state = mutableStateOf(initialState)
        composeRule.setContent {
            DstcgTheme {
                StartScreen(
                    state = state.value,
                    onBegin = {},
                    onResetProgress = onResetProgress,
                    showBackground = false,
                    contentVisible = true,
                )
            }
        }
        return state
    }
}
