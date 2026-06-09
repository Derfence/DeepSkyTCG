package fr.aumombelli.dstcg

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
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
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeRight
import fr.aumombelli.dstcg.app.NewPlayerOnboardingTarget
import fr.aumombelli.dstcg.feature.home.HomeScreen
import fr.aumombelli.dstcg.feature.home.HomeUiState
import fr.aumombelli.dstcg.ui.component.TRADING_CARD_WIDTH_OVER_HEIGHT
import fr.aumombelli.dstcg.ui.theme.DstcgTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
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
    fun novelty_indicators_are_hidden_by_default() {
        setHomeScreenContent(
            HomeUiState(
                isLoading = false,
                isLibraryMenuVisible = true,
                isBadgeBookMenuVisible = true,
            ),
        )

        composeRule.onAllNodesWithTag("home-library-new-indicator", useUnmergedTree = true).assertCountEquals(0)
        composeRule.onAllNodesWithTag("home-equipment-new-indicator", useUnmergedTree = true).assertCountEquals(0)
        composeRule.onAllNodesWithTag("home-badges-new-indicator", useUnmergedTree = true).assertCountEquals(0)
    }

    @Test
    fun crafting_action_is_visible_and_opens_callback() {
        var openCraftingCount = 0
        setHomeScreenContent(
            initialState = HomeUiState(
                isLoading = false,
                isCraftingMenuAvailable = true,
            ),
            onOpenCrafting = { openCraftingCount += 1 },
        )

        composeRule.onNodeWithTag("home-crafting").assertIsDisplayed().performClick()

        composeRule.runOnIdle {
            assertEquals(1, openCraftingCount)
        }
    }

    @Test
    fun badges_move_to_top_start_and_crafting_replaces_bottom_end() {
        setHomeScreenContent(
            HomeUiState(
                isLoading = false,
                isLibraryMenuVisible = true,
                isBadgeBookMenuVisible = true,
                isCraftingMenuAvailable = true,
            ),
        )

        val badgeBounds = composeRule.onNodeWithTag("home-badges").fetchSemanticsNode().boundsInRoot
        val libraryBounds = composeRule.onNodeWithTag("home-library").fetchSemanticsNode().boundsInRoot
        val craftingBounds = composeRule.onNodeWithTag("home-crafting").fetchSemanticsNode().boundsInRoot

        assertTrue(badgeBounds.top < libraryBounds.top)
        assertTrue(badgeBounds.left <= libraryBounds.left + 1f)
        assertEquals(libraryBounds.top, craftingBounds.top, 2f)
        assertTrue(craftingBounds.left > libraryBounds.left)
    }

    @Test
    fun locked_menus_are_hidden_until_their_unlock_conditions_are_met() {
        setHomeScreenContent(
            HomeUiState(
                isLoading = false,
                isCraftingMenuAvailable = false,
            ),
        )

        composeRule.onAllNodesWithTag("home-library").assertCountEquals(0)
        composeRule.onAllNodesWithTag("home-badges").assertCountEquals(0)
        composeRule.onAllNodesWithTag("home-crafting").assertCountEquals(0)
        composeRule.onAllNodesWithTag("home-card-flip").assertCountEquals(0)
        composeRule.onAllNodesWithTag("home-mini-games-card").assertCountEquals(0)
    }

    @Test
    fun mini_games_card_is_front_side_by_default_after_unlock() {
        setHomeScreenContent(
            HomeUiState(
                isLoading = false,
                isMiniGamesMenuVisible = true,
            ),
        )

        composeRule.onNodeWithTag("home-open-pack").assertIsDisplayed()
        composeRule.onNodeWithTag("home-card-flip").assertIsDisplayed()
        composeRule.onAllNodesWithTag("home-mini-games-card").assertCountEquals(0)
    }

    @Test
    fun flip_button_and_swipes_toggle_the_home_card_back() {
        setHomeScreenContent(
            HomeUiState(
                isLoading = false,
                isMiniGamesMenuVisible = true,
            ),
        )

        composeRule.onNodeWithTag("home-card-flip").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("home-mini-games-card").assertIsDisplayed()

        composeRule.onNodeWithTag("home-mini-games-card").performTouchInput { swipeRight() }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("home-open-pack").assertIsDisplayed()

        composeRule.onNodeWithTag("home-open-pack").performTouchInput { swipeLeft() }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("home-mini-games-card").assertIsDisplayed()
    }

    @Test
    fun swipe_outside_home_card_does_not_flip() {
        setHomeScreenContent(
            HomeUiState(
                isLoading = false,
                isMiniGamesMenuVisible = true,
            ),
        )

        composeRule.onNodeWithTag("home-settings").performTouchInput { swipeLeft() }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("home-open-pack").assertIsDisplayed()
        composeRule.onAllNodesWithTag("home-mini-games-card").assertCountEquals(0)
    }

    @Test
    fun mini_games_card_button_opens_menu_callback() {
        var openMiniGamesCount = 0
        setHomeScreenContent(
            initialState = HomeUiState(
                isLoading = false,
                isMiniGamesMenuVisible = true,
            ),
            onOpenMiniGamesMenu = { openMiniGamesCount += 1 },
        )

        composeRule.onNodeWithTag("home-card-flip").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("home-mini-games-open-menu").performClick()
        composeRule.waitForIdle()

        composeRule.runOnIdle {
            assertEquals(1, openMiniGamesCount)
        }
    }

    @Test
    fun mini_games_card_click_opens_menu_callback() {
        var openMiniGamesCount = 0
        setHomeScreenContent(
            initialState = HomeUiState(
                isLoading = false,
                isMiniGamesMenuVisible = true,
            ),
            onOpenMiniGamesMenu = { openMiniGamesCount += 1 },
        )

        composeRule.onNodeWithTag("home-card-flip").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("home-mini-games-card").performClick()
        composeRule.waitForIdle()

        composeRule.runOnIdle {
            assertEquals(1, openMiniGamesCount)
        }
    }

    @Test
    fun mini_games_home_target_uses_central_card_bounds() {
        val reportedTargets = mutableSetOf<NewPlayerOnboardingTarget>()
        setHomeScreenContent(
            initialState = HomeUiState(
                isLoading = false,
                isMiniGamesMenuVisible = true,
            ),
            onCoachmarkTargetBoundsChanged = { target, bounds ->
                if (bounds != null) {
                    reportedTargets += target
                }
            },
        )

        composeRule.waitForIdle()

        assertTrue(NewPlayerOnboardingTarget.HomeOpenPack in reportedTargets)
        assertTrue(NewPlayerOnboardingTarget.HomeMiniGames in reportedTargets)
    }

    @Test
    fun novelty_indicators_are_shown_only_for_matching_buttons() {
        setHomeScreenContent(
            HomeUiState(
                isLoading = false,
                isLibraryMenuVisible = true,
                isEquipmentMenuVisible = true,
                isBadgeBookMenuVisible = true,
                showLibraryNewIndicator = true,
                showEquipmentNewIndicator = true,
                showBadgeBookNewIndicator = true,
            ),
        )

        composeRule.onNodeWithTag("home-library-new-indicator", useUnmergedTree = true).assertIsDisplayed()
        composeRule.onNodeWithTag("home-equipment-new-indicator", useUnmergedTree = true).assertIsDisplayed()
        composeRule.onNodeWithTag("home-badges-new-indicator", useUnmergedTree = true).assertIsDisplayed()
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
        composeRule.onNodeWithTag("home-about-sheet-version").assertTextContains("v2.6.1")
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

    @Test
    fun pack_card_size_adapts_to_viewport_height_while_preserving_ratio() {
        val viewportWidthFraction = mutableStateOf(1f)
        val viewportHeightFraction = mutableStateOf(0.86f)
        setHomeScreenContentInViewport(
            widthFraction = viewportWidthFraction,
            heightFraction = viewportHeightFraction,
            state = HomeUiState(isLoading = false),
        )
        composeRule.waitForIdle()
        val shortViewportBounds = composeRule.onNodeWithTag("home-open-pack").fetchSemanticsNode().boundsInRoot

        composeRule.runOnIdle {
            viewportWidthFraction.value = 0.96f
            viewportHeightFraction.value = 1f
        }
        composeRule.waitForIdle()
        val tallViewportBounds = composeRule.onNodeWithTag("home-open-pack").fetchSemanticsNode().boundsInRoot

        assertTrue(
            "Expected the pack card to grow when the viewport gets taller.",
            tallViewportBounds.width > shortViewportBounds.width,
        )
        assertEquals(
            TRADING_CARD_WIDTH_OVER_HEIGHT,
            shortViewportBounds.width / shortViewportBounds.height,
            0.01f,
        )
        assertEquals(
            TRADING_CARD_WIDTH_OVER_HEIGHT,
            tallViewportBounds.width / tallViewportBounds.height,
            0.01f,
        )
    }

    private fun setHomeScreenContent(
        initialState: HomeUiState,
        onOpenCrafting: () -> Unit = {},
        onOpenMiniGamesMenu: () -> Unit = {},
        onResetProgress: () -> Unit = {},
        onCoachmarkTargetBoundsChanged: (
            NewPlayerOnboardingTarget,
            Rect?,
        ) -> Unit = { _, _ -> },
    ): MutableState<HomeUiState> {
        val state = mutableStateOf(initialState)
        composeRule.setContent {
            DstcgTheme {
                HomeScreen(
                    state = state.value,
                    onOpenPack = {},
                    onOpenLibrary = {},
                    onOpenCrafting = onOpenCrafting,
                    onOpenEquipment = {},
                    onOpenBadgeBook = {},
                    onOpenMiniGamesMenu = onOpenMiniGamesMenu,
                    onResetProgress = onResetProgress,
                    showBackground = false,
                    contentVisible = true,
                    onCoachmarkTargetBoundsChanged = onCoachmarkTargetBoundsChanged,
                )
            }
        }
        return state
    }

    private fun setHomeScreenContentInViewport(
        widthFraction: MutableState<Float>,
        heightFraction: MutableState<Float>,
        state: HomeUiState,
    ) {
        composeRule.setContent {
            DstcgTheme {
                BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                    Box(
                        modifier = Modifier.size(
                            width = maxWidth * widthFraction.value.coerceIn(0f, 1f),
                            height = maxHeight * heightFraction.value.coerceIn(0f, 1f),
                        ),
                    ) {
                        HomeScreen(
                            state = state,
                            onOpenPack = {},
                            onOpenLibrary = {},
                            onOpenCrafting = {},
                            onOpenEquipment = {},
                            onOpenBadgeBook = {},
                            onOpenMiniGamesMenu = {},
                            onResetProgress = {},
                            showBackground = false,
                            contentVisible = true,
                        )
                    }
                }
            }
        }
    }
}
