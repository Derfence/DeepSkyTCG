package fr.aumombelli.gatcha

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import fr.aumombelli.gatcha.ui.screen.LoginScreen
import fr.aumombelli.gatcha.ui.theme.GatchaTheme
import fr.aumombelli.gatcha.ui.viewmodel.LoginUiState
import org.junit.Rule
import org.junit.Test

class LoginScreenStateTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun toggle_mode_shows_and_hides_email_field() {
        setLoginScreenContent(LoginUiState())

        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTag("login-email")
                .fetchSemanticsNodes(atLeastOneRootRequired = false)
                .isEmpty()
        }

        composeRule.onNodeWithTag("login-toggle-mode").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTag("login-email")
                .fetchSemanticsNodes(atLeastOneRootRequired = false)
                .isNotEmpty()
        }
        composeRule.onNodeWithTag("login-email").assertIsDisplayed()

        composeRule.onNodeWithTag("login-toggle-mode").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTag("login-email")
                .fetchSemanticsNodes(atLeastOneRootRequired = false)
                .isEmpty()
        }
    }

    @Test
    fun loading_and_transitioning_states_disable_fields_and_actions() {
        val state = setLoginScreenContent(
            LoginUiState(
                username = "alice",
                email = "alice@example.com",
                password = "secret",
                isCreateMode = true,
                isLoading = true,
            ),
        )

        composeRule.onNodeWithTag("login-username").assertIsNotEnabled()
        composeRule.onNodeWithTag("login-email").assertIsNotEnabled()
        composeRule.onNodeWithTag("login-password").assertIsNotEnabled()
        composeRule.onNodeWithTag("login-submit").assertIsNotEnabled()
        composeRule.onNodeWithTag("login-toggle-mode").assertIsNotEnabled()

        composeRule.runOnIdle {
            state.value = LoginUiState(
                username = "alice",
                password = "secret",
                isTransitioningToMenu = true,
            )
        }

        composeRule.onNodeWithTag("login-username").assertIsNotEnabled()
        composeRule.onNodeWithTag("login-password").assertIsNotEnabled()
        composeRule.onNodeWithTag("login-submit").assertIsNotEnabled()
        composeRule.onNodeWithTag("login-toggle-mode").assertIsNotEnabled()
    }

    private fun setLoginScreenContent(initialState: LoginUiState): MutableState<LoginUiState> {
        val state = mutableStateOf(initialState)
        composeRule.setContent {
            GatchaTheme {
                LoginScreen(
                    state = state.value,
                    onUsernameChange = { state.value = state.value.copy(username = it) },
                    onEmailChange = { state.value = state.value.copy(email = it) },
                    onPasswordChange = { state.value = state.value.copy(password = it) },
                    onModeToggle = { state.value = state.value.copy(isCreateMode = !state.value.isCreateMode) },
                    onSubmit = {},
                    showBackground = false,
                    contentVisible = true,
                )
            }
        }
        return state
    }
}
