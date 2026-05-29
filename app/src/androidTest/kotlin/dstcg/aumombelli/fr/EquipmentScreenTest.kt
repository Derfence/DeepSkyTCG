package fr.aumombelli.dstcg

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.unit.dp
import fr.aumombelli.dstcg.feature.equipment.EquipmentActiveSummaryItemUi
import fr.aumombelli.dstcg.feature.equipment.EquipmentCategoryIconUi
import fr.aumombelli.dstcg.feature.equipment.EquipmentCategoryVisualUi
import fr.aumombelli.dstcg.feature.equipment.EquipmentInventoryCardUi
import fr.aumombelli.dstcg.feature.equipment.EquipmentSectionUi
import fr.aumombelli.dstcg.feature.equipment.EquipmentUiState
import fr.aumombelli.dstcg.model.EquipmentType
import fr.aumombelli.dstcg.ui.screen.EquipmentScreen
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class EquipmentScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun back_button_is_visible_only_with_callback_and_invokes_it() {
        var backClicks = 0

        composeRule.setContent {
            EquipmentScreen(
                state = EquipmentUiState(isLoading = false),
                onRefresh = {},
                onActivateEquipment = {},
                onBack = { backClicks += 1 },
            )
        }

        composeRule.onNodeWithTag("equipment-back").assertIsDisplayed()
        composeRule.onNodeWithTag("equipment-back").performClick()

        assertEquals(1, backClicks)
    }

    @Test
    fun equipment_screen_renders_visual_sections_and_active_summary() {
        composeRule.setContent {
            EquipmentScreen(
                state = EquipmentUiState(
                    isLoading = false,
                    activeEffects = listOf(
                        EquipmentActiveSummaryItemUi(
                            type = EquipmentType.Observatory,
                            visual = testEquipmentCategoryVisual(EquipmentType.Observatory),
                            displayName = "Observatoire expert",
                            bonusLabel = "x1,5 recharge",
                            packsRemaining = 3,
                        ),
                    ),
                    sections = listOf(
                        testEquipmentSection(
                            type = EquipmentType.Observatory,
                            statusLabel = "2 cartes en reserve",
                            lastActivatedLabel = "Observatoire debutant",
                            cards = listOf(
                                testEquipmentInventoryCard(
                                    definition = testEquipmentCardDefinition(
                                        id = "observatory-1",
                                        type = EquipmentType.Observatory,
                                        displayName = "Observatoire debutant",
                                        level = 1,
                                    ),
                                    stockCount = 2,
                                    activationCount = 3,
                                    activationEnabled = true,
                                ),
                                testEquipmentInventoryCard(
                                    definition = testEquipmentCardDefinition(
                                        id = "observatory-2",
                                        type = EquipmentType.Observatory,
                                        displayName = "Observatoire expert",
                                        level = 2,
                                    ),
                                    stockCount = 1,
                                    activationCount = 1,
                                ),
                                testEquipmentInventoryCard(
                                    definition = testEquipmentCardDefinition(
                                        id = "observatory-3",
                                        type = EquipmentType.Observatory,
                                        displayName = "Observatoire legendaire",
                                        level = 3,
                                    ),
                                    stockCount = 0,
                                    activationCount = 0,
                                ),
                            ),
                        ),
                        testEquipmentSection(
                            type = EquipmentType.Telescope,
                            statusLabel = "1 carte en reserve",
                            cards = listOf(
                                testEquipmentInventoryCard(
                                    definition = testEquipmentCardDefinition(
                                        id = "telescope-1",
                                        type = EquipmentType.Telescope,
                                        displayName = "Telescope debutant",
                                        level = 1,
                                    ),
                                    stockCount = 1,
                                    activationCount = 2,
                                    activationEnabled = true,
                                ),
                                testEquipmentInventoryCard(
                                    definition = testEquipmentCardDefinition(
                                        id = "telescope-2",
                                        type = EquipmentType.Telescope,
                                        displayName = "Telescope expert",
                                        level = 2,
                                    ),
                                ),
                                testEquipmentInventoryCard(
                                    definition = testEquipmentCardDefinition(
                                        id = "telescope-3",
                                        type = EquipmentType.Telescope,
                                        displayName = "Telescope legendaire",
                                        level = 3,
                                    ),
                                ),
                            ),
                        ),
                        testEquipmentSection(
                            type = EquipmentType.Mount,
                            statusLabel = "Aucune carte en reserve",
                            cards = listOf(
                                testEquipmentInventoryCard(
                                    definition = testEquipmentCardDefinition(
                                        id = "mount-1",
                                        type = EquipmentType.Mount,
                                        displayName = "Monture debutante",
                                        level = 1,
                                    ),
                                ),
                                testEquipmentInventoryCard(
                                    definition = testEquipmentCardDefinition(
                                        id = "mount-2",
                                        type = EquipmentType.Mount,
                                        displayName = "Monture experte",
                                        level = 2,
                                    ),
                                ),
                                testEquipmentInventoryCard(
                                    definition = testEquipmentCardDefinition(
                                        id = "mount-3",
                                        type = EquipmentType.Mount,
                                        displayName = "Monture legendaire",
                                        level = 3,
                                    ),
                                ),
                            ),
                        ),
                    ),
                ),
                onRefresh = {},
                onActivateEquipment = {},
            )
        }

        composeRule.onNodeWithTag("equipment-active-observatory").assertIsDisplayed()
        composeRule.onNodeWithTag("equipment-icon-observatory").assertIsDisplayed()
        composeRule.onNodeWithTag("equipment-benefit-observatory").assertIsDisplayed()
        composeRule.onAllNodesWithTag("equipment-section-status-observatory").assertCountEquals(0)
        composeRule.onNodeWithTag("equipment-last-used-observatory").assertIsDisplayed()
        composeRule.onNodeWithTag("equipment-cards-observatory").assertIsDisplayed()
        composeRule.onNodeWithTag("equipment-card-observatory-1").assertIsDisplayed()
        assertTrue(
            composeRule.onAllNodesWithText("Utilisés")
                .fetchSemanticsNodes(atLeastOneRootRequired = false)
                .isNotEmpty(),
        )
        composeRule.onNodeWithTag("equipment-list").performScrollToNode(hasTestTag("equipment-section-telescope"))
        composeRule.onNodeWithTag("equipment-icon-telescope").assertIsDisplayed()
        composeRule.onNodeWithTag("equipment-list").performScrollToNode(hasTestTag("equipment-section-mount"))
        composeRule.onNodeWithTag("equipment-icon-mount").assertIsDisplayed()
        assertTrue(
            composeRule.onAllNodesWithText("Utilisées")
                .fetchSemanticsNodes(atLeastOneRootRequired = false)
                .isNotEmpty(),
        )
    }

    @Test
    fun equipment_category_row_scrolls_horizontally_on_compact_width() {
        composeRule.setContent {
            Box(modifier = Modifier.size(width = 320.dp, height = 640.dp)) {
                EquipmentScreen(
                    state = EquipmentUiState(
                        isLoading = false,
                        sections = listOf(
                            testEquipmentSection(
                                type = EquipmentType.Observatory,
                                statusLabel = "3 cartes en reserve",
                                cards = listOf(
                                    testEquipmentInventoryCard(
                                        definition = testEquipmentCardDefinition(
                                            id = "observatory-1",
                                            type = EquipmentType.Observatory,
                                            displayName = "Observatoire debutant",
                                            level = 1,
                                        ),
                                        stockCount = 2,
                                        activationCount = 3,
                                        activationEnabled = true,
                                    ),
                                    testEquipmentInventoryCard(
                                        definition = testEquipmentCardDefinition(
                                            id = "observatory-2",
                                            type = EquipmentType.Observatory,
                                            displayName = "Observatoire expert",
                                            level = 2,
                                        ),
                                        stockCount = 1,
                                        activationCount = 1,
                                        activationEnabled = true,
                                    ),
                                    testEquipmentInventoryCard(
                                        definition = testEquipmentCardDefinition(
                                            id = "observatory-3",
                                            type = EquipmentType.Observatory,
                                            displayName = "Observatoire legendaire",
                                            level = 3,
                                        ),
                                        stockCount = 1,
                                        activationCount = 0,
                                        activationEnabled = true,
                                    ),
                                ),
                            ),
                        ),
                    ),
                    onRefresh = {},
                    onActivateEquipment = {},
                )
            }
        }

        composeRule.onNodeWithTag("equipment-card-observatory-1").assertIsDisplayed()
        composeRule.onNodeWithTag("equipment-cards-observatory").performScrollToIndex(2)
        composeRule.onNodeWithTag("equipment-card-observatory-3").assertIsDisplayed()
    }

    @Test
    fun equipment_card_opens_detail_directly_and_returns_to_equipment() {
        composeRule.setContent {
            EquipmentScreen(
                state = EquipmentUiState(
                    isLoading = false,
                    sections = listOf(
                        testEquipmentSection(
                            type = EquipmentType.Observatory,
                            statusLabel = "1 carte en reserve",
                            cards = listOf(
                                testEquipmentInventoryCard(
                                    definition = testEquipmentCardDefinition(
                                        id = "observatory-1",
                                        type = EquipmentType.Observatory,
                                        displayName = "Observatoire debutant",
                                        level = 1,
                                        imageRef = "equipment_observatory_1",
                                    ),
                                    stockCount = 1,
                                    activationCount = 2,
                                    activationEnabled = true,
                                ),
                            ),
                        ),
                    ),
                ),
                onRefresh = {},
                onActivateEquipment = {},
            )
        }

        composeRule.onAllNodesWithTag(
            "equipment-card-art-observatory-1",
            useUnmergedTree = true,
        ).assertCountEquals(1)
        composeRule.onNodeWithTag("equipment-card-observatory-1").performClick()
        composeRule.onNodeWithTag("equipment-card-fullscreen").assertIsDisplayed()
        composeRule.onAllNodesWithTag(
            "equipment-card-fullscreen-art-observatory-1",
            useUnmergedTree = true,
        ).assertCountEquals(1)
        composeRule.onNodeWithTag("equipment-card-fullscreen-icon-observatory-1").assertIsDisplayed()
        composeRule.onNodeWithTag("equipment-card-fullscreen-close").performClick()
        composeRule.onAllNodesWithTag("equipment-card-fullscreen").assertCountEquals(0)
    }

    @Test
    fun equipment_card_uses_fallback_when_art_asset_is_missing() {
        composeRule.setContent {
            EquipmentScreen(
                state = EquipmentUiState(
                    isLoading = false,
                    sections = listOf(
                        testEquipmentSection(
                            type = EquipmentType.Telescope,
                            statusLabel = "1 carte en reserve",
                            cards = listOf(
                                testEquipmentInventoryCard(
                                    definition = testEquipmentCardDefinition(
                                        id = "telescope-missing-art",
                                        type = EquipmentType.Telescope,
                                        displayName = "Telescope sans image",
                                        level = 1,
                                        imageRef = "equipment_missing_art",
                                    ),
                                    stockCount = 1,
                                    activationCount = 0,
                                    activationEnabled = true,
                                ),
                            ),
                        ),
                    ),
                ),
                onRefresh = {},
                onActivateEquipment = {},
            )
        }

        composeRule.onAllNodesWithTag(
            "equipment-card-art-fallback-telescope-missing-art",
            useUnmergedTree = true,
        ).assertCountEquals(1)
        composeRule.onNodeWithTag("equipment-card-telescope-missing-art").performClick()
        composeRule.onAllNodesWithTag(
            "equipment-card-fullscreen-art-fallback-telescope-missing-art",
            useUnmergedTree = true,
        ).assertCountEquals(1)
        composeRule.onNodeWithTag("equipment-card-fullscreen-icon-telescope-missing-art").assertIsDisplayed()
    }

    @Test
    fun active_equipment_card_uses_color_indicator_on_card_frame() {
        composeRule.setContent {
            EquipmentScreen(
                state = EquipmentUiState(
                    isLoading = false,
                    sections = listOf(
                        testEquipmentSection(
                            type = EquipmentType.Observatory,
                            statusLabel = "3 packs actifs",
                            cards = listOf(
                                testEquipmentInventoryCard(
                                    definition = testEquipmentCardDefinition(
                                        id = "observatory-active",
                                        type = EquipmentType.Observatory,
                                        displayName = "Observatoire actif",
                                        level = 2,
                                    ),
                                    activationCount = 1,
                                    isActive = true,
                                    packsRemaining = 3,
                                ),
                            ),
                        ),
                    ),
                ),
                onRefresh = {},
                onActivateEquipment = {},
            )
        }

        composeRule.onNodeWithTag("equipment-list")
            .performScrollToNode(hasTestTag("equipment-section-observatory"))
        composeRule.onNodeWithTag("equipment-cards-observatory").performScrollToIndex(0)
        composeRule.onAllNodesWithTag("equipment-card-icon-observatory-active", useUnmergedTree = true)
            .assertCountEquals(1)
        composeRule.onAllNodesWithTag("equipment-card-active-indicator-observatory-active", useUnmergedTree = true)
            .assertCountEquals(1)
    }

    @Test
    fun equipment_activation_target_is_cleared_when_activation_starts() {
        var state by mutableStateOf(
            EquipmentUiState(
                isLoading = false,
                sections = listOf(
                    testEquipmentSection(
                        type = EquipmentType.Observatory,
                        statusLabel = "1 carte en reserve",
                        cards = listOf(
                            testEquipmentInventoryCard(
                                definition = testEquipmentCardDefinition(
                                    id = "observatory-1",
                                    type = EquipmentType.Observatory,
                                    displayName = "Observatoire debutant",
                                    level = 1,
                                ),
                                stockCount = 1,
                                activationCount = 0,
                                activationEnabled = true,
                            ),
                        ),
                    ),
                    testEquipmentSection(
                        type = EquipmentType.Telescope,
                        statusLabel = "1 carte en reserve",
                        cards = listOf(
                            testEquipmentInventoryCard(
                                definition = testEquipmentCardDefinition(
                                    id = "telescope-1",
                                    type = EquipmentType.Telescope,
                                    displayName = "Telescope debutant",
                                    level = 1,
                                ),
                                stockCount = 1,
                                activationCount = 0,
                                activationEnabled = true,
                            ),
                        ),
                    ),
                ),
            ),
        )
        var lastReportedBounds: Rect? = null

        composeRule.setContent {
            EquipmentScreen(
                state = state,
                onRefresh = {},
                onActivateEquipment = {},
                onOnboardingActivationBoundsChanged = { bounds ->
                    lastReportedBounds = bounds
                },
            )
        }

        composeRule.waitUntil(timeoutMillis = 5_000) { lastReportedBounds != null }

        composeRule.runOnIdle {
            state = state.copy(activatingCardId = "observatory-1")
        }

        composeRule.waitUntil(timeoutMillis = 5_000) { lastReportedBounds == null }

        composeRule.runOnIdle {
            state = state.copy(
                activatingCardId = null,
                activeEffects = listOf(
                    EquipmentActiveSummaryItemUi(
                        type = EquipmentType.Observatory,
                        visual = testEquipmentCategoryVisual(EquipmentType.Observatory),
                        displayName = "Observatoire debutant",
                        bonusLabel = "x1,5 recharge",
                        packsRemaining = 3,
                    ),
                ),
                sections = listOf(
                    testEquipmentSection(
                        type = EquipmentType.Observatory,
                        statusLabel = "3 packs actifs",
                        lastActivatedLabel = "Observatoire debutant",
                        cards = listOf(
                            testEquipmentInventoryCard(
                                definition = testEquipmentCardDefinition(
                                    id = "observatory-1",
                                    type = EquipmentType.Observatory,
                                    displayName = "Observatoire debutant",
                                    level = 1,
                                ),
                                stockCount = 0,
                                activationCount = 1,
                                isActive = true,
                                packsRemaining = 3,
                            ),
                        ),
                    ),
                    testEquipmentSection(
                        type = EquipmentType.Telescope,
                        statusLabel = "1 carte en reserve",
                        cards = listOf(
                            testEquipmentInventoryCard(
                                definition = testEquipmentCardDefinition(
                                    id = "telescope-1",
                                    type = EquipmentType.Telescope,
                                    displayName = "Telescope debutant",
                                    level = 1,
                                ),
                                stockCount = 1,
                                activationCount = 0,
                                activationEnabled = true,
                            ),
                        ),
                    ),
                ),
            )
        }

        composeRule.waitForIdle()
        assertNull(lastReportedBounds)
    }

    @Test
    fun equipment_activation_reports_scroll_hint_when_target_section_is_below_viewport() {
        var activationBounds: Rect? = null
        var scrollHintVisible by mutableStateOf(false)

        composeRule.setContent {
            Box(modifier = Modifier.size(width = 360.dp, height = 420.dp)) {
                EquipmentScreen(
                    state = EquipmentUiState(
                        isLoading = false,
                        sections = listOf(
                            testEquipmentSection(
                                type = EquipmentType.Observatory,
                                statusLabel = "Aucune carte en reserve",
                                cards = List(3) { index ->
                                    testEquipmentInventoryCard(
                                        definition = testEquipmentCardDefinition(
                                            id = "observatory-${index + 1}",
                                            type = EquipmentType.Observatory,
                                            displayName = "Observatoire ${index + 1}",
                                            level = index + 1,
                                        ),
                                    )
                                },
                            ),
                            testEquipmentSection(
                                type = EquipmentType.Telescope,
                                statusLabel = "Aucune carte en reserve",
                                cards = List(3) { index ->
                                    testEquipmentInventoryCard(
                                        definition = testEquipmentCardDefinition(
                                            id = "telescope-${index + 1}",
                                            type = EquipmentType.Telescope,
                                            displayName = "Telescope ${index + 1}",
                                            level = index + 1,
                                        ),
                                    )
                                },
                            ),
                            testEquipmentSection(
                                type = EquipmentType.Mount,
                                statusLabel = "1 carte en reserve",
                                cards = listOf(
                                    testEquipmentInventoryCard(
                                        definition = testEquipmentCardDefinition(
                                            id = "mount-activate",
                                            type = EquipmentType.Mount,
                                            displayName = "Monture a activer",
                                            level = 1,
                                        ),
                                        stockCount = 1,
                                        activationCount = 0,
                                        activationEnabled = true,
                                    ),
                                ),
                            ),
                        ),
                    ),
                    onRefresh = {},
                    onActivateEquipment = {},
                    onOnboardingActivationBoundsChanged = { bounds ->
                        activationBounds = bounds
                    },
                    onOnboardingActivationScrollHintChanged = { visible ->
                        scrollHintVisible = visible
                    },
                )
            }
        }

        composeRule.waitUntil(timeoutMillis = 5_000) { scrollHintVisible }
        assertNull(activationBounds)
    }

    @Test
    fun equipment_activation_reports_bounds_when_target_button_is_visible() {
        var activationBounds: Rect? = null
        var scrollHintVisible by mutableStateOf(true)

        composeRule.setContent {
            Box(modifier = Modifier.size(width = 360.dp, height = 900.dp)) {
                EquipmentScreen(
                    state = EquipmentUiState(
                        isLoading = false,
                        sections = listOf(
                            testEquipmentSection(
                                type = EquipmentType.Observatory,
                                statusLabel = "1 carte en reserve",
                                cards = listOf(
                                    testEquipmentInventoryCard(
                                        definition = testEquipmentCardDefinition(
                                            id = "observatory-activate",
                                            type = EquipmentType.Observatory,
                                            displayName = "Observatoire a activer",
                                            level = 1,
                                        ),
                                        stockCount = 1,
                                        activationCount = 0,
                                        activationEnabled = true,
                                    ),
                                ),
                            ),
                        ),
                    ),
                    onRefresh = {},
                    onActivateEquipment = {},
                    onOnboardingActivationBoundsChanged = { bounds ->
                        activationBounds = bounds
                    },
                    onOnboardingActivationScrollHintChanged = { visible ->
                        scrollHintVisible = visible
                    },
                )
            }
        }

        composeRule.waitUntil(timeoutMillis = 5_000) { !scrollHintVisible && activationBounds != null }
        composeRule.onAllNodesWithTag("equipment-activate-observatory-activate").assertCountEquals(1)
    }

    private fun testEquipmentSection(
        type: EquipmentType,
        statusLabel: String,
        lastActivatedLabel: String? = null,
        cards: List<EquipmentInventoryCardUi>,
    ): EquipmentSectionUi = EquipmentSectionUi(
        type = type,
        title = type.displayName,
        visual = testEquipmentCategoryVisual(type),
        statusLabel = statusLabel,
        lastActivatedLabel = lastActivatedLabel,
        cards = cards,
    )

    private fun testEquipmentInventoryCard(
        definition: fr.aumombelli.dstcg.model.EquipmentCardDefinition,
        stockCount: Int = 0,
        activationCount: Int = 0,
        isActive: Boolean = false,
        packsRemaining: Int? = null,
        activationEnabled: Boolean = false,
    ): EquipmentInventoryCardUi = EquipmentInventoryCardUi(
        definition = definition,
        stockCount = stockCount,
        activationCount = activationCount,
        isActive = isActive,
        packsRemaining = packsRemaining,
        activationEnabled = activationEnabled,
    )

    private fun testEquipmentCategoryVisual(type: EquipmentType): EquipmentCategoryVisualUi = when (type) {
        EquipmentType.Observatory -> EquipmentCategoryVisualUi(
            icon = EquipmentCategoryIconUi.Observatory,
            benefitLabel = "Recharge des packs",
        )

        EquipmentType.Telescope -> EquipmentCategoryVisualUi(
            icon = EquipmentCategoryIconUi.Telescope,
            benefitLabel = "Chance holographique",
        )

        EquipmentType.Mount -> EquipmentCategoryVisualUi(
            icon = EquipmentCategoryIconUi.Mount,
            benefitLabel = "Promotion de rareté",
        )
    }
}
