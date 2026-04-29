package fr.aumombelli.dstcg

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import fr.aumombelli.dstcg.app.NewPlayerOnboardingTarget
import fr.aumombelli.dstcg.feature.crafting.CraftingCardGroup
import fr.aumombelli.dstcg.feature.crafting.CraftingCompletion
import fr.aumombelli.dstcg.feature.crafting.CraftingScreen
import fr.aumombelli.dstcg.feature.crafting.CraftingSection
import fr.aumombelli.dstcg.feature.crafting.CraftingUiState
import fr.aumombelli.dstcg.model.CraftingCardCandidate
import fr.aumombelli.dstcg.model.CraftingMode
import fr.aumombelli.dstcg.model.CraftingRecipe
import fr.aumombelli.dstcg.model.DisplayCardVariant
import fr.aumombelli.dstcg.ui.theme.DstcgTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class CraftingScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun mode_menu_exposes_both_workshops() {
        var selectedMode: CraftingMode? = null

        composeRule.setContent {
            DstcgTheme {
                CraftingScreen(
                    state = CraftingUiState(),
                    onRefresh = {},
                    onSelectMode = { selectedMode = it },
                    onBackHome = {},
                    onBackToModes = {},
                    onApplyCrafting = {},
                )
            }
        }

        composeRule.onNodeWithTag("crafting-mode-darken-sky").assertIsDisplayed()
        composeRule.onNodeWithTag("crafting-mode-space-agency").assertIsDisplayed().performClick()

        composeRule.runOnIdle {
            assertEquals(CraftingMode.SpaceAgency, selectedMode)
        }
    }

    @Test
    fun mode_menu_uses_two_half_screen_buttons_with_svg_slots_and_edge_copy() {
        var reportedDarkenTargetBounds: Rect? = null

        composeRule.setContent {
            DstcgTheme {
                CraftingScreen(
                    state = CraftingUiState(),
                    onRefresh = {},
                    onSelectMode = {},
                    onBackHome = {},
                    onBackToModes = {},
                    onApplyCrafting = {},
                    onCoachmarkTargetBoundsChanged = { target, bounds ->
                        if (target == NewPlayerOnboardingTarget.CraftingDarkenSkyMode) {
                            reportedDarkenTargetBounds = bounds
                        }
                    },
                )
            }
        }

        val screenBounds = composeRule.onNodeWithTag("crafting-screen").fetchSemanticsNode().boundsInRoot
        val darkenBounds = composeRule.onNodeWithTag("crafting-mode-darken-sky").fetchSemanticsNode().boundsInRoot
        val agencyBounds = composeRule.onNodeWithTag("crafting-mode-space-agency").fetchSemanticsNode().boundsInRoot
        val darkenCopyBounds = composeRule.onNodeWithTag(
            "crafting-mode-copy-DarkenSky",
            useUnmergedTree = true,
        ).fetchSemanticsNode().boundsInRoot
        val agencyCopyBounds = composeRule.onNodeWithTag(
            "crafting-mode-copy-SpaceAgency",
            useUnmergedTree = true,
        ).fetchSemanticsNode().boundsInRoot
        val darkenGraphicBounds = composeRule.onNodeWithTag(
            "crafting-mode-darken-sky-svg-slot",
            useUnmergedTree = true,
        ).fetchSemanticsNode().boundsInRoot
        val agencyGraphicBounds = composeRule.onNodeWithTag(
            "crafting-mode-space-agency-svg-slot",
            useUnmergedTree = true,
        ).fetchSemanticsNode().boundsInRoot

        composeRule.onNodeWithTag("crafting-mode-darken-sky-svg-slot", useUnmergedTree = true).assertIsDisplayed()
        composeRule.onNodeWithTag("crafting-mode-space-agency-svg-slot", useUnmergedTree = true).assertIsDisplayed()
        assertEquals(screenBounds.height / 2f, darkenBounds.height, 2f)
        assertEquals(screenBounds.height / 2f, agencyBounds.height, 2f)
        assertEquals(darkenBounds.bottom, agencyBounds.top, 1f)
        assertEquals(darkenBounds, darkenGraphicBounds)
        assertEquals(agencyBounds, agencyGraphicBounds)
        assertTrue(darkenCopyBounds.top < darkenBounds.center.y)
        assertTrue(agencyCopyBounds.bottom > agencyBounds.center.y)
        composeRule.waitUntil(timeoutMillis = 5_000) {
            reportedDarkenTargetBounds != null
        }
        val darkenTargetBounds = reportedDarkenTargetBounds ?: error("Missing darken sky onboarding target bounds.")
        assertEquals(darkenBounds.left, darkenTargetBounds.left, 1f)
        assertEquals(darkenBounds.right, darkenTargetBounds.right, 1f)
        assertEquals(darkenBounds.bottom, darkenTargetBounds.bottom, 1f)
        assertTrue(
            "Expected darken sky onboarding target to start below the copy top and before the copy bottom.",
            darkenTargetBounds.top > darkenCopyBounds.top &&
                darkenTargetBounds.top < darkenCopyBounds.bottom,
        )
    }

    @Test
    fun space_agency_candidate_opens_confirmation_and_runs_stamp_animation() {
        val candidate = testCraftingCandidate(CraftingMode.SpaceAgency)
        val state = mutableStateOf(
            CraftingUiState(
                selectedMode = CraftingMode.SpaceAgency,
                sections = listOf(testCraftingSection(candidate)),
            ),
        )
        var appliedCandidate: CraftingCardCandidate? = null

        composeRule.setContent {
            DstcgTheme {
                CraftingScreen(
                    state = state.value,
                    onRefresh = {},
                    onSelectMode = {},
                    onBackHome = {},
                    onBackToModes = {},
                    onApplyCrafting = { appliedCandidate = it },
                )
            }
        }

        composeRule.onNodeWithTag("library-card-ALP-001").performClick()
        composeRule.onNodeWithTag("crafting-fullscreen").assertIsDisplayed()
        composeRule.onNodeWithTag("crafting-consumed-text").assertTextContains("10 x Ville", substring = true)
        composeRule.onNodeWithTag("crafting-created-text").assertTextContains("Tamponnee", substring = true)
        composeRule.mainClock.autoAdvance = false
        composeRule.onNodeWithTag("crafting-confirm").performClick()

        composeRule.runOnIdle {
            val applied = checkNotNull(appliedCandidate)
            state.value = state.value.copy(
                completion = CraftingCompletion(
                    id = 1,
                    mode = CraftingMode.SpaceAgency,
                    recipe = CraftingRecipe(
                        mode = CraftingMode.SpaceAgency,
                        source = applied.sourceRef,
                        target = applied.targetRef,
                        consumedCount = applied.consumedCount,
                    ),
                ),
            )
        }
        composeRule.mainClock.advanceTimeByFrame()
        composeRule.mainClock.advanceTimeBy(240)
        composeRule.onNodeWithTag("crafting-stamp-animation").assertIsDisplayed()
        composeRule.mainClock.autoAdvance = true
    }

    @Test
    fun applying_darken_sky_disables_confirmation_and_shows_pending_label() {
        val candidate = testCraftingCandidate(CraftingMode.DarkenSky)
        val state = mutableStateOf(
            CraftingUiState(
                selectedMode = CraftingMode.DarkenSky,
                sections = listOf(testCraftingSection(candidate)),
            ),
        )

        composeRule.setContent {
            DstcgTheme {
                CraftingScreen(
                    state = state.value,
                    onRefresh = {},
                    onSelectMode = {},
                    onBackHome = {},
                    onBackToModes = {},
                    onApplyCrafting = {
                        state.value = state.value.copy(isApplying = true)
                    },
                )
            }
        }

        composeRule.onNodeWithTag("library-card-ALP-001").performClick()
        composeRule.onNodeWithTag("crafting-confirm").performClick()

        composeRule.onNodeWithTag("crafting-confirm").assertIsNotEnabled()
        composeRule.onNodeWithTag("crafting-confirm").assertTextContains("En cours", substring = true)
    }

    @Test
    fun darken_sky_keeps_pending_label_until_color_animation_finishes() {
        val candidate = testCraftingCandidate(CraftingMode.DarkenSky)
        val state = mutableStateOf(
            CraftingUiState(
                selectedMode = CraftingMode.DarkenSky,
                sections = listOf(testCraftingSection(candidate)),
            ),
        )
        var appliedCandidate: CraftingCardCandidate? = null

        composeRule.setContent {
            DstcgTheme {
                CraftingScreen(
                    state = state.value,
                    onRefresh = {},
                    onSelectMode = {},
                    onBackHome = {},
                    onBackToModes = {},
                    onApplyCrafting = { appliedCandidate = it },
                )
            }
        }

        composeRule.onNodeWithTag("library-card-ALP-001").performClick()
        composeRule.onNodeWithTag("crafting-fullscreen").assertIsDisplayed()
        composeRule.onNodeWithTag("crafting-confirm").assertIsDisplayed()
        composeRule.mainClock.autoAdvance = false
        composeRule.onNodeWithTag("crafting-confirm").performClick()

        composeRule.runOnIdle {
            val applied = checkNotNull(appliedCandidate)
            state.value = state.value.copy(
                completion = CraftingCompletion(
                    id = 1,
                    mode = CraftingMode.DarkenSky,
                    recipe = CraftingRecipe(
                        mode = CraftingMode.DarkenSky,
                        source = applied.sourceRef,
                        target = applied.targetRef,
                        consumedCount = applied.consumedCount,
                    ),
                ),
            )
        }

        composeRule.mainClock.advanceTimeByFrame()
        composeRule.onNodeWithTag("crafting-confirm").assertIsNotEnabled()
        composeRule.onNodeWithTag("crafting-confirm").assertTextContains("En cours", substring = true)

        composeRule.mainClock.advanceTimeBy(1_100)
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("crafting-confirm").assertTextContains("Termine", substring = true)
        composeRule.mainClock.autoAdvance = true
    }

    @Test
    fun empty_filtered_library_keeps_filter_locked() {
        composeRule.setContent {
            DstcgTheme {
                CraftingScreen(
                    state = CraftingUiState(
                        selectedMode = CraftingMode.DarkenSky,
                        sections = emptyList(),
                    ),
                    onRefresh = {},
                    onSelectMode = {},
                    onBackHome = {},
                    onBackToModes = {},
                    onApplyCrafting = {},
                )
            }
        }

        composeRule.onNodeWithTag("crafting-empty").assertIsDisplayed()
        composeRule.onAllNodesWithTag("library-filter-tradeable").assertCountEquals(0)
    }

    @Test
    fun darken_sky_flow_reports_onboarding_targets() {
        val reportedTargets = mutableSetOf<NewPlayerOnboardingTarget>()
        val candidate = testCraftingCandidate(CraftingMode.DarkenSky)

        composeRule.setContent {
            DstcgTheme {
                CraftingScreen(
                    state = CraftingUiState(
                        selectedMode = CraftingMode.DarkenSky,
                        sections = listOf(testCraftingSection(candidate)),
                    ),
                    onRefresh = {},
                    onSelectMode = {},
                    onBackHome = {},
                    onBackToModes = {},
                    onApplyCrafting = {},
                    onCoachmarkTargetBoundsChanged = { target, bounds ->
                        if (bounds != null) {
                            reportedTargets += target
                        }
                    },
                )
            }
        }

        composeRule.waitUntil(timeoutMillis = 5_000) {
            NewPlayerOnboardingTarget.CraftingCandidate in reportedTargets
        }
        composeRule.onNodeWithTag("library-card-ALP-001").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            NewPlayerOnboardingTarget.CraftingConfirm in reportedTargets
        }

        assertTrue(NewPlayerOnboardingTarget.CraftingCandidate in reportedTargets)
        assertTrue(NewPlayerOnboardingTarget.CraftingConfirm in reportedTargets)
    }

    private fun testCraftingSection(candidate: CraftingCardCandidate): CraftingSection =
        CraftingSection(
            extensionId = "astronomes-en-herbe",
            extensionName = "Astronomes en herbe",
            cards = listOf(
                CraftingCardGroup(
                    cardId = candidate.card.id,
                    cardName = candidate.card.name,
                    candidates = listOf(candidate),
                ),
            ),
        )

    private fun testCraftingCandidate(mode: CraftingMode): CraftingCardCandidate {
        val isDarkenSky = mode == CraftingMode.DarkenSky
        return CraftingCardCandidate(
            card = testCardDefinition("ALP-001", name = "Nebuleuse d'Orion"),
            extensionName = "Astronomes en herbe",
            mode = mode,
            sourceVariant = DisplayCardVariant(
                skyQuality = "city",
                skyQualityLabel = "Ville",
                finish = "standard",
                finishLabel = "Standard",
                isHolographic = false,
                count = if (isDarkenSky) 2 else 10,
            ),
            targetVariant = DisplayCardVariant(
                skyQuality = if (isDarkenSky) "suburban" else "city",
                skyQualityLabel = if (isDarkenSky) "Periurbain" else "Ville",
                finish = if (isDarkenSky) "standard" else "stamped",
                finishLabel = if (isDarkenSky) "Standard" else "Tamponnee",
                isHolographic = false,
                count = 0,
                isStamped = !isDarkenSky,
            ),
            consumedCount = if (isDarkenSky) 2 else 10,
        )
    }
}
