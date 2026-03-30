package fr.aumombelli.gatcha

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import fr.aumombelli.gatcha.feature.start.StartScreen
import fr.aumombelli.gatcha.feature.start.StartUiState
import fr.aumombelli.gatcha.ui.theme.GatchaTheme
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
    fun warning_state_is_displayed_without_disabling_begin() {
        setStartScreenContent(
            StartUiState(
                isLoading = false,
                warningMessage = "La progression locale a été sécurisée.",
            ),
        )

        composeRule.onNodeWithTag("start-warning").assertIsDisplayed()
    }

    @Test
    fun compromised_state_shows_reset_action() {
        var resetCount = 0
        composeRule.setContent {
            GatchaTheme {
                StartScreen(
                    state = StartUiState(
                        isLoading = false,
                        errorMessage = "La progression locale semble corrompue.",
                        canResetProgress = true,
                    ),
                    onBegin = {},
                    onResetProgress = { resetCount += 1 },
                    showBackground = false,
                    contentVisible = true,
                )
            }
        }

        composeRule.onNodeWithTag("start-reset-progress").assertIsDisplayed().performClick()
        composeRule.runOnIdle {
            assertEquals(1, resetCount)
        }
    }

    private fun setStartScreenContent(initialState: StartUiState): MutableState<StartUiState> {
        val state = mutableStateOf(initialState)
        composeRule.setContent {
            GatchaTheme {
                StartScreen(
                    state = state.value,
                    onBegin = {},
                    onResetProgress = {},
                    showBackground = false,
                    contentVisible = true,
                )
            }
        }
        return state
    }
}
