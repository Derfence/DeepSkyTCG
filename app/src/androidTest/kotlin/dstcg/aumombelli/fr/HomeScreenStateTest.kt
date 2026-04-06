package fr.aumombelli.dstcg

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import fr.aumombelli.dstcg.feature.home.HomeScreen
import fr.aumombelli.dstcg.feature.home.HomeUiState
import fr.aumombelli.dstcg.ui.theme.DstcgTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class HomeScreenStateTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun loading_and_resetting_states_disable_pack_action() {
        val state = setHomeScreenContent(
            HomeUiState(
                isLoading = true,
            ),
        )

        composeRule.onNodeWithTag("home-open-pack").assertIsNotEnabled()

        composeRule.runOnIdle {
            state.value = HomeUiState(
                isLoading = false,
                isResettingProgress = true,
            )
        }

        composeRule.onNodeWithTag("home-open-pack").assertIsNotEnabled()
    }

    @Test
    fun error_state_is_displayed_and_prevents_pack_opening() {
        setHomeScreenContent(
            HomeUiState(
                isLoading = false,
                errorMessage = "Saved progression could not be read.",
            ),
        )

        composeRule.onNodeWithTag("home-error-card").assertIsDisplayed()
        composeRule.onNodeWithTag("home-open-pack").assertIsNotEnabled()
    }

    @Test
    fun equipment_action_is_hidden_until_it_is_unlocked() {
        val state = setHomeScreenContent(
            HomeUiState(
                isLoading = false,
                isEquipmentMenuVisible = false,
            ),
        )

        composeRule.onAllNodesWithTag("home-equipment").assertCountEquals(0)

        composeRule.runOnIdle {
            state.value = HomeUiState(
                isLoading = false,
                isEquipmentMenuVisible = true,
            )
        }

        composeRule.onNodeWithTag("home-equipment").assertIsDisplayed()
    }

    @Test
    fun settings_menu_is_visible_and_can_open_about_sheet() {
        setHomeScreenContent(
            HomeUiState(
                isLoading = false,
            ),
        )

        composeRule.onNodeWithTag("home-settings").assertIsDisplayed().performClick()
        composeRule.onNodeWithTag("home-settings-about").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("home-about-sheet").assertIsDisplayed()
        composeRule.onNodeWithTag("home-about-sheet-version").assertTextContains("v1.2.0")
    }

    @Test
    fun about_sheet_closes_with_swipe_down_on_header() {
        setHomeScreenContent(
            HomeUiState(
                isLoading = false,
            ),
        )

        composeRule.onNodeWithTag("home-settings").performClick()
        composeRule.onNodeWithTag("home-settings-about").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("home-about-sheet").assertIsDisplayed()

        composeRule.onNodeWithTag("home-about-sheet-header").performTouchInput { swipeDown() }
        composeRule.waitForIdle()
        composeRule.onAllNodesWithTag("home-about-sheet").assertCountEquals(0)
    }

    @Test
    fun reset_confirmation_requires_delay_before_validation() {
        composeRule.mainClock.autoAdvance = false
        setHomeScreenContent(
            HomeUiState(
                isLoading = false,
            ),
        )

        composeRule.onNodeWithTag("home-settings").performClick()
        composeRule.mainClock.advanceTimeBy(1)
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("home-settings-reset").performClick()
        composeRule.mainClock.advanceTimeBy(1)
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("home-reset-confirmation").assertIsDisplayed()
        composeRule.onNodeWithTag("home-reset-confirmation-confirm").assertIsNotEnabled()

        composeRule.mainClock.advanceTimeBy(1_999)
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("home-reset-confirmation-confirm").assertIsNotEnabled()

        composeRule.mainClock.advanceTimeBy(1)
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("home-reset-confirmation-confirm").assertIsEnabled()
        composeRule.mainClock.autoAdvance = true
    }

    @Test
    fun cancelling_reset_confirmation_keeps_callback_uninvoked() {
        var resetCount = 0
        setHomeScreenContent(
            initialState = HomeUiState(
                isLoading = false,
            ),
            onResetProgress = { resetCount += 1 },
        )

        composeRule.onNodeWithTag("home-settings").performClick()
        composeRule.onNodeWithTag("home-settings-reset").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("home-reset-confirmation-cancel").performClick()
        composeRule.waitForIdle()

        composeRule.onAllNodesWithTag("home-reset-confirmation").assertCountEquals(0)
        composeRule.runOnIdle {
            assertEquals(0, resetCount)
        }
    }

    @Test
    fun confirming_reset_calls_callback_once() {
        var resetCount = 0
        composeRule.mainClock.autoAdvance = false
        setHomeScreenContent(
            initialState = HomeUiState(
                isLoading = false,
            ),
            onResetProgress = { resetCount += 1 },
        )

        composeRule.onNodeWithTag("home-settings").performClick()
        composeRule.mainClock.advanceTimeBy(1)
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("home-settings-reset").performClick()
        composeRule.mainClock.advanceTimeBy(1)
        composeRule.waitForIdle()
        composeRule.mainClock.advanceTimeBy(2_000)
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("home-reset-confirmation-confirm").performClick()
        composeRule.mainClock.advanceTimeBy(1)
        composeRule.waitForIdle()
        composeRule.mainClock.autoAdvance = true

        composeRule.onAllNodesWithTag("home-reset-confirmation").assertCountEquals(0)
        composeRule.runOnIdle {
            assertEquals(1, resetCount)
        }
    }

    private fun setHomeScreenContent(
        initialState: HomeUiState,
        onResetProgress: () -> Unit = {},
    ): MutableState<HomeUiState> {
        val state = mutableStateOf(initialState)
        composeRule.setContent {
            DstcgTheme {
                HomeScreen(
                    state = state.value,
                    onOpenPack = {},
                    onOpenLibrary = {},
                    onOpenEquipment = {},
                    onOpenBadgeBook = {},
                    onResetProgress = onResetProgress,
                    showBackground = false,
                    contentVisible = true,
                )
            }
        }
        return state
    }
}
