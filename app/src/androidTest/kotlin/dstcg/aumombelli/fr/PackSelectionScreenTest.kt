package fr.aumombelli.dstcg

import android.os.SystemClock
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import fr.aumombelli.dstcg.app.NewPlayerOnboardingTarget
import fr.aumombelli.dstcg.model.DrawPackResponse
import fr.aumombelli.dstcg.model.ExtensionDefinition
import fr.aumombelli.dstcg.model.toDisplayCard
import fr.aumombelli.dstcg.model.toDisplayVariant
import fr.aumombelli.dstcg.testsupport.androidTestRechargeStateWithNextChargeAt
import fr.aumombelli.dstcg.ui.component.TRADING_CARD_WIDTH_OVER_HEIGHT
import fr.aumombelli.dstcg.ui.screen.PackOpeningScreen
import fr.aumombelli.dstcg.ui.screen.PackSelectionScreen
import fr.aumombelli.dstcg.ui.viewmodel.PackOpeningUiState
import fr.aumombelli.dstcg.ui.viewmodel.PackSelectionUiState
import java.time.Duration
import java.time.Instant
import kotlin.math.abs
import kotlin.math.absoluteValue
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
    fun booster_coachmark_target_waits_for_intro_completion_and_clears_on_click() {
        var latestBoosterCoachmarkBounds: Rect? = null
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
                    state.value = state.value.copy(
                        selectedBoosterIndex = boosterIndex,
                        isAwaitingPackResult = true,
                    )
                },
                onOpenPack = {},
                onPackRevealReady = {},
                onCoachmarkTargetBoundsChanged = { target, bounds ->
                    if (target == NewPlayerOnboardingTarget.PackSelectionBooster) {
                        latestBoosterCoachmarkBounds = bounds
                    }
                },
                packReadySignal = 0,
                showBackground = false,
            )
        }

        composeRule.mainClock.advanceTimeBy(1_200)
        composeRule.waitForIdle()
        assertEquals(null, latestBoosterCoachmarkBounds)

        composeRule.mainClock.advanceTimeBy(800)
        composeRule.waitForIdle()
        assertTrue(latestBoosterCoachmarkBounds != null)

        composeRule.onNodeWithTag("pack-booster-0").performClick()
        composeRule.waitForIdle()
        assertEquals(null, latestBoosterCoachmarkBounds)
    }

    @Test
    fun extension_coachmark_target_clears_on_click() {
        var latestExtensionCoachmarkBounds: Rect? = null
        val state = mutableStateOf(
            PackSelectionUiState(
                isLoading = false,
                extensions = listOf(
                    ExtensionDefinition("astronomes-en-herbe", "Astronomes en herbe", "cover"),
                ),
            ),
        )

        composeRule.setContent {
            PackSelectionScreen(
                state = state.value,
                onRefresh = {},
                onSelectExtension = { extensionId ->
                    state.value = state.value.copy(selectedExtensionId = extensionId)
                },
                onSelectBooster = {},
                onOpenPack = {},
                onPackRevealReady = {},
                onCoachmarkTargetBoundsChanged = { target, bounds ->
                    if (target == NewPlayerOnboardingTarget.PackSelectionExtension) {
                        latestExtensionCoachmarkBounds = bounds
                    }
                },
                packReadySignal = 0,
                showBackground = false,
            )
        }

        composeRule.waitForIdle()
        assertTrue(latestExtensionCoachmarkBounds != null)

        composeRule.onNodeWithTag("pack-extension-enter-astronomes-en-herbe").performClick()
        composeRule.waitForIdle()
        assertEquals(null, latestExtensionCoachmarkBounds)
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
    fun first_booster_keeps_the_same_node_when_coachmark_becomes_ready() {
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

        composeRule.mainClock.advanceTimeBy(1_050)
        composeRule.waitForIdle()

        val firstNodeId = composeRule.onNodeWithTag("pack-booster-0").fetchSemanticsNode().id

        composeRule.mainClock.advanceTimeBy(1_000)
        composeRule.waitForIdle()

        val secondNodeId = composeRule.onNodeWithTag("pack-booster-0").fetchSemanticsNode().id

        assertEquals(
            "Expected the first booster node to stay stable when the coachmark becomes ready.",
            firstNodeId,
            secondNodeId,
        )
    }

    @Test
    fun selected_extension_hero_expands_over_header_area() {
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

        composeRule.mainClock.advanceTimeBy(1_000)
        composeRule.waitForIdle()

        val screenBounds = composeRule.onNodeWithTag("pack-screen-root").fetchSemanticsNode().boundsInRoot
        val titleBounds = composeRule.onNodeWithTag("pack-title").fetchSemanticsNode().boundsInRoot
        val stageBounds = composeRule.onNodeWithTag("pack-extension-stage").fetchSemanticsNode().boundsInRoot
        val heroBounds = composeRule.onNodeWithTag("pack-extension-hero").fetchSemanticsNode().boundsInRoot

        assertTrue(
            "Expected hero top ${heroBounds.top} to be above title top ${titleBounds.top}.",
            heroBounds.top <= titleBounds.top,
        )
        assertTrue(
            "Expected hero left ${heroBounds.left} to align with screen left ${screenBounds.left}.",
            (heroBounds.left - screenBounds.left).absoluteValue <= 2f,
        )
        assertTrue(
            "Expected hero top ${heroBounds.top} to align with screen top ${screenBounds.top}.",
            (heroBounds.top - screenBounds.top).absoluteValue <= 2f,
        )
        assertTrue(
            "Expected hero height ${heroBounds.height} to exceed stage height ${stageBounds.height}.",
            heroBounds.height > stageBounds.height,
        )
        assertTrue(
            "Expected hero right ${heroBounds.right} to align with screen right ${screenBounds.right}.",
            (heroBounds.right - screenBounds.right).absoluteValue <= 2f,
        )
        assertTrue(
            "Expected hero bottom ${heroBounds.bottom} to align with screen bottom ${screenBounds.bottom}.",
            (heroBounds.bottom - screenBounds.bottom).absoluteValue <= 2f,
        )
    }

    @Test
    fun selected_extension_offsets_title_from_screen_top_and_keeps_compact_boosters_large() {
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
            Box(modifier = androidx.compose.ui.Modifier.size(width = 411.dp, height = 731.dp)) {
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
        }

        composeRule.mainClock.advanceTimeBy(1_800)
        composeRule.waitForIdle()

        val screenBounds = composeRule.onNodeWithTag("pack-screen-root").fetchSemanticsNode().boundsInRoot
        val titleBounds = composeRule.onNodeWithTag("pack-extension-title").fetchSemanticsNode().boundsInRoot
        val boosterBounds = composeRule.onNodeWithTag("pack-booster-0").fetchSemanticsNode().boundsInRoot

        assertTrue(
            "Expected extension title top ${titleBounds.top} to sit below screen top ${screenBounds.top}.",
            titleBounds.top >= screenBounds.top + 8f,
        )
        assertTrue(
            "Expected first booster top ${boosterBounds.top} to stay below extension title bottom ${titleBounds.bottom}.",
            boosterBounds.top >= titleBounds.bottom + 4f,
        )
        assertTrue(
            "Expected compact booster width ${boosterBounds.width} to keep a strong presence within screen width ${screenBounds.width}.",
            boosterBounds.width / screenBounds.width >= 0.40f,
        )
    }

    @Test
    fun selected_booster_finishes_in_the_same_slot_as_pack_opening_booster() {
        val firstCard = testCardDefinition("ALP-001", name = "Nebuleuse d'Orion")
        val packResult = DrawPackResponse.fromCards(
            extensionId = "astronomes-en-herbe",
            drawnAt = "2026-03-23T12:00:00Z",
            rechargeState = androidTestRechargeStateWithNextChargeAt(
                availableDrawCount = 9,
                nextChargeAt = "2026-03-24T18:00:00Z",
            ),
            cards = listOf(
                testPackCard("ALP-001", "Nebuleuse d'Orion", "Common", "spark_fox"),
            ),
        )
        val selectionState = mutableStateOf(
            PackSelectionUiState(
                isLoading = false,
                extensions = listOf(
                    ExtensionDefinition("astronomes-en-herbe", "Astronomes en herbe", "cover"),
                ),
                selectedExtensionId = "astronomes-en-herbe",
            ),
        )
        var showOpeningScreen by mutableStateOf(false)

        composeRule.mainClock.autoAdvance = false
        composeRule.setContent {
            Box(modifier = androidx.compose.ui.Modifier.size(width = 411.dp, height = 731.dp)) {
                if (!showOpeningScreen) {
                    PackSelectionScreen(
                        state = selectionState.value,
                        onRefresh = {},
                        onSelectExtension = {},
                        onSelectBooster = { boosterIndex ->
                            selectionState.value = selectionState.value.copy(
                                selectedBoosterIndex = boosterIndex,
                                isAwaitingPackResult = true,
                            )
                        },
                        onOpenPack = {},
                        onPackRevealReady = {},
                        packReadySignal = 0,
                        showBackground = false,
                    )
                } else {
                    PackOpeningScreen(
                        state = PackOpeningUiState(
                            packResult = packResult,
                            displayCards = listOf(
                                firstCard.toDisplayCard(
                                    extensionName = "Astronomes en herbe",
                                    activeVariant = packResult.cards[0].variant.toDisplayVariant(),
                                ),
                            ),
                            highestBurstRarity = "Common",
                            hasHolographicBurst = false,
                        ),
                        onDone = {},
                    )
                }
            }
        }

        composeRule.mainClock.advanceTimeBy(1_800)
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("pack-booster-0").performClick()
        composeRule.mainClock.advanceTimeBy(700)
        composeRule.waitForIdle()

        val selectedBoosterBounds = composeRule.onNodeWithTag("pack-booster-0").fetchSemanticsNode().boundsInRoot

        composeRule.runOnIdle { showOpeningScreen = true }

        composeRule.mainClock.advanceTimeBy(300)
        composeRule.waitForIdle()

        val openingBoosterBounds = composeRule
            .onNodeWithTag("pack-opening-booster", useUnmergedTree = true)
            .fetchSemanticsNode().boundsInRoot

        composeRule.assertBoundsClose(
            expected = openingBoosterBounds,
            actual = selectedBoosterBounds,
            tolerancePx = 3f,
        )
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
                    rechargeState = androidTestRechargeStateWithNextChargeAt(
                        availableDrawCount = 0,
                        nextChargeAt = "2999-01-01T00:00:00Z",
                    ),
                    availableDrawCount = 0,
                    nextChargeAt = "2999-01-01T00:00:00Z",
                    remainingDuration = Duration.ofHours(6),
                    rechargeProgress = 0f,
                    isDrawLocked = true,
                    trustedNow = Instant.parse("2026-03-24T12:00:00Z"),
                    trustedElapsedRealtimeMs = SystemClock.elapsedRealtime(),
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

        val titleBounds = composeRule.onNodeWithTag("pack-title").fetchSemanticsNode().boundsInRoot
        val forecastBounds = composeRule.onNodeWithTag("pack-weather-forecast").fetchSemanticsNode().boundsInRoot
        val statusBounds = composeRule.onNodeWithTag("pack-status").fetchSemanticsNode().boundsInRoot

        assertTrue(titleBounds.bottom <= forecastBounds.top)
        assertTrue(forecastBounds.bottom <= statusBounds.top)
        composeRule.onNodeWithTag("pack-weather-title").assertIsDisplayed()
        composeRule.onNodeWithTag("pack-weather-time").assertTextContains("12:00 UTC")
        composeRule.onNodeWithTag("pack-weather-forecast").assertIsDisplayed()
        composeRule.onNodeWithTag("pack-weather-day-0").assertIsDisplayed()
        composeRule.onNodeWithTag("pack-weather-day-6").assertIsDisplayed()
        composeRule.onNodeWithTag("pack-weather-day-multiplier-0").assertTextContains("x1")
        composeRule.onNodeWithTag("pack-weather-day-multiplier-1").assertTextContains("x0")
        composeRule.onNodeWithTag("pack-status-count").assertTextContains("0/10")
        composeRule.onAllNodesWithTag("pack-status-weather").assertCountEquals(0)
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

    private fun androidx.compose.ui.test.junit4.ComposeContentTestRule.assertBoundsClose(
        expected: Rect,
        actual: Rect,
        tolerancePx: Float,
    ) {
        assertTrue(
            "Expected bounds to stay aligned. Expected=$expected Actual=$actual",
            abs(expected.left - actual.left) <= tolerancePx &&
                abs(expected.top - actual.top) <= tolerancePx &&
                abs(expected.width - actual.width) <= tolerancePx &&
                abs(expected.height - actual.height) <= tolerancePx,
        )
    }
}
