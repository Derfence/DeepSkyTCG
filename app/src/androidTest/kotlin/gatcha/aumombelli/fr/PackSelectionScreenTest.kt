package fr.aumombelli.gatcha

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import fr.aumombelli.gatcha.model.ExtensionDefinition
import fr.aumombelli.gatcha.ui.component.TRADING_CARD_WIDTH_OVER_HEIGHT
import fr.aumombelli.gatcha.ui.screen.PackSelectionScreen
import fr.aumombelli.gatcha.ui.viewmodel.PackSelectionUiState
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
                onBack = {},
                onRefresh = {},
                onSelectExtension = {},
                onBackToExtensions = {},
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
                onBack = {},
                onRefresh = {},
                onSelectExtension = {},
                onBackToExtensions = {},
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
    fun selected_extension_can_return_to_extension_list_cleanly() {
        var backToExtensionsCalls = 0
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
                onBack = {},
                onRefresh = {},
                onSelectExtension = {},
                onBackToExtensions = {
                    backToExtensionsCalls += 1
                    state.value = state.value.copy(selectedExtensionId = null)
                },
                onSelectBooster = {},
                onOpenPack = {},
                onPackRevealReady = {},
                packReadySignal = 0,
                showBackground = false,
            )
        }

        composeRule.mainClock.advanceTimeBy(1800)
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("pack-back").performClick()
        composeRule.mainClock.advanceTimeBy(2400)
        composeRule.mainClock.autoAdvance = true
        composeRule.waitForIdle()

        assertEquals(1, backToExtensionsCalls)
        composeRule.onNodeWithTag("pack-extension-enter-astronomes-en-herbe").assertIsDisplayed()
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
