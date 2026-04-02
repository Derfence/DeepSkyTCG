package fr.aumombelli.dstcg

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import fr.aumombelli.dstcg.model.ExtensionDefinition
import fr.aumombelli.dstcg.ui.component.TRADING_CARD_WIDTH_OVER_HEIGHT
import fr.aumombelli.dstcg.ui.screen.PackSelectionScreen
import fr.aumombelli.dstcg.ui.viewmodel.PackSelectionUiState
import java.time.Duration
import kotlin.math.abs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class PackSelectionScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun selected_extension_draws_constellation_and_centers_chosen_booster_before_reveal() {
        var packReadySignal by mutableIntStateOf(0)
        var revealReady = false
        var openedExtensionId: String? = null
        var selectedBooster: Int? = null
        val state = mutableStateOf(
            PackSelectionUiState(
                isLoading = false,
                extensions = listOf(
                    ExtensionDefinition("astronomes-en-herbe", "Astronomes en herbe", "cover"),
                ),
                selectedExtensionId = "astronomes-en-herbe",
            ),
        )

        composeRule.mainClock.autoAdvance = false
        composeRule.setContent {
            PackSelectionScreen(
                state = state.value,
                onRefresh = {},
                onSelectExtension = {},
                onSelectBooster = { boosterIndex ->
                    selectedBooster = boosterIndex
                    state.value = state.value.copy(
                        selectedBoosterIndex = boosterIndex,
                        isAwaitingPackResult = true,
                    )
                },
                onOpenPack = { extensionId ->
                    openedExtensionId = extensionId
                },
                onPackRevealReady = { revealReady = true },
                packReadySignal = packReadySignal,
                showBackground = false,
            )
        }

        composeRule.mainClock.advanceTimeBy(1800)
        composeRule.mainClock.autoAdvance = true
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("pack-booster-0").assertIsDisplayed()
        composeRule.onNodeWithTag("pack-booster-3").assertIsDisplayed()
        composeRule.onAllNodesWithTag("app-launch-logo")
            .assertCountEquals(0)
        composeRule.onAllNodesWithTag("pack-extension-constellation")
            .assertCountEquals(0)
        composeRule.onAllNodesWithTag("pack-back").assertCountEquals(0)
        composeRule.onAllNodesWithText("Booster")
            .assertCountEquals(0)

        composeRule.onNodeWithTag("pack-booster-0").performClick()
        composeRule.mainClock.autoAdvance = false
        composeRule.mainClock.advanceTimeBy(700)
        composeRule.mainClock.autoAdvance = true
        composeRule.waitForIdle()

        assertEquals("astronomes-en-herbe", openedExtensionId)
        assertEquals(0, selectedBooster)
        assertTrue(
            composeRule.onAllNodesWithTag("pack-booster-1")
                .fetchSemanticsNodes(atLeastOneRootRequired = false)
                .isEmpty(),
        )
        composeRule.assertApproxCardRatio("pack-booster-0")

        packReadySignal += 1
        composeRule.mainClock.autoAdvance = false
        composeRule.mainClock.advanceTimeBy(700)
        composeRule.mainClock.autoAdvance = true
        composeRule.waitForIdle()

        assertTrue(revealReady)
    }

    @Test
    fun selected_extension_shows_packs_one_by_one_without_autonomous_logo() {
        val state = mutableStateOf(
            PackSelectionUiState(
                isLoading = false,
                extensions = listOf(
                    ExtensionDefinition("astronomes-en-herbe", "Astronomes en herbe", "cover"),
                ),
                selectedExtensionId = "astronomes-en-herbe",
            ),
        )

        composeRule.mainClock.autoAdvance = false
        composeRule.setContent {
            PackSelectionScreen(
                state = state.value,
                onRefresh = {},
                onSelectExtension = {},
                onSelectBooster = {},
                onOpenPack = {},
                onPackRevealReady = {},
                packReadySignal = 0,
                showBackground = false,
            )
        }

        composeRule.mainClock.advanceTimeBy(1050)
        composeRule.waitForIdle()

        composeRule.onAllNodesWithTag("app-launch-logo").assertCountEquals(0)
        composeRule.onAllNodesWithTag("pack-extension-constellation").assertCountEquals(0)
        assertTrue(
            composeRule.onAllNodesWithTag("pack-booster-0")
                .fetchSemanticsNodes(atLeastOneRootRequired = false)
                .isNotEmpty(),
        )
        assertTrue(
            composeRule.onAllNodesWithTag("pack-booster-3")
                .fetchSemanticsNodes(atLeastOneRootRequired = false)
                .isEmpty(),
        )

        composeRule.mainClock.advanceTimeBy(1000)
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("pack-booster-3").assertIsDisplayed()
    }

    @Test
    fun selected_extension_returns_to_extension_list_when_selection_is_cleared() {
        val state = mutableStateOf(
            PackSelectionUiState(
                isLoading = false,
                extensions = listOf(
                    ExtensionDefinition("astronomes-en-herbe", "Astronomes en herbe", "cover"),
                ),
                selectedExtensionId = "astronomes-en-herbe",
            ),
        )

        composeRule.mainClock.autoAdvance = false
        composeRule.setContent {
            PackSelectionScreen(
                state = state.value,
                onRefresh = {},
                onSelectExtension = {},
                onSelectBooster = {},
                onOpenPack = {},
                onPackRevealReady = {},
                packReadySignal = 0,
                showBackground = false,
            )
        }

        composeRule.mainClock.advanceTimeBy(1800)
        composeRule.waitForIdle()
        state.value = state.value.copy(selectedExtensionId = null)
        composeRule.mainClock.advanceTimeBy(1200)
        composeRule.mainClock.autoAdvance = true
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("pack-extension-enter-astronomes-en-herbe").assertIsDisplayed()
        composeRule.onAllNodesWithTag("pack-back").assertCountEquals(0)
    }

    @Test
    fun cooldown_state_disables_extension_entry_and_updates_status_text() {
        composeRule.setContent {
            PackSelectionScreen(
                state = PackSelectionUiState(
                    isLoading = false,
                    extensions = listOf(
                        ExtensionDefinition("astronomes-en-herbe", "Astronomes en herbe", "cover"),
                    ),
                    availableDrawCount = 0,
                    nextChargeAt = "2999-01-01T00:00:00Z",
                    remainingDuration = Duration.ofHours(6),
                    rechargeProgress = 0f,
                    isDrawLocked = true,
                ),
                onRefresh = {},
                onSelectExtension = {},
                onSelectBooster = {},
                onOpenPack = {},
                onPackRevealReady = {},
                packReadySignal = 0,
                showBackground = false,
            )
        }

        composeRule.onNodeWithTag("pack-status-count").assertTextContains("0/10")
        composeRule.onNodeWithTag("pack-status-remaining")
            .assertTextContains("Prochaine charge dans", substring = true)
        composeRule.onNodeWithTag("pack-status-progress").assertIsDisplayed()
        composeRule.onNodeWithTag("pack-extension-enter-astronomes-en-herbe").assertIsNotEnabled()
    }

    @Test
    fun error_card_exposes_retry_action() {
        var refreshCalls = 0

        composeRule.setContent {
            PackSelectionScreen(
                state = PackSelectionUiState(
                    isLoading = false,
                    extensions = listOf(
                        ExtensionDefinition("astronomes-en-herbe", "Astronomes en herbe", "cover"),
                    ),
                    errorMessage = "Draw failed.",
                ),
                onRefresh = { refreshCalls += 1 },
                onSelectExtension = {},
                onSelectBooster = {},
                onOpenPack = {},
                onPackRevealReady = {},
                packReadySignal = 0,
                showBackground = false,
            )
        }

        composeRule.onNodeWithTag("pack-error").assertTextContains("Draw failed.")
        composeRule.onNodeWithTag("pack-refresh").performClick()

        assertEquals(1, refreshCalls)
    }

    private fun androidx.compose.ui.test.junit4.ComposeContentTestRule.assertApproxCardRatio(
        tag: String,
        tolerance: Float = 0.03f,
    ) {
        val bounds = onNodeWithTag(tag, useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        val actualRatio = bounds.width / bounds.height
        assertTrue(
            "Expected $tag width/height ratio near $TRADING_CARD_WIDTH_OVER_HEIGHT but was $actualRatio",
            abs(actualRatio - TRADING_CARD_WIDTH_OVER_HEIGHT) <= tolerance,
        )
    }
}
