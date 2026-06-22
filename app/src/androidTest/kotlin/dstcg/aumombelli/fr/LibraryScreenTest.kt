package fr.aumombelli.dstcg

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeRight
import androidx.compose.ui.unit.dp
import fr.aumombelli.dstcg.audio.AmbientTrack
import fr.aumombelli.dstcg.audio.AudioController
import fr.aumombelli.dstcg.audio.AudioSettings
import fr.aumombelli.dstcg.audio.LocalAudioController
import fr.aumombelli.dstcg.audio.SoundCue
import fr.aumombelli.dstcg.feature.library.buildLibraryOnboardingVariantWalkthroughPages
import fr.aumombelli.dstcg.feature.library.LibraryFilterOption
import fr.aumombelli.dstcg.feature.library.LibraryFilterOptions
import fr.aumombelli.dstcg.model.CardFinishDefinition
import fr.aumombelli.dstcg.model.DisplayCardVariant
import fr.aumombelli.dstcg.model.ExtensionDefinition
import fr.aumombelli.dstcg.model.LibraryCardItem
import fr.aumombelli.dstcg.model.LibrarySection
import fr.aumombelli.dstcg.model.SkyQualityDefinition
import fr.aumombelli.dstcg.model.VariantProfile
import fr.aumombelli.dstcg.ui.screen.LibraryScreen
import fr.aumombelli.dstcg.ui.component.AstroCardDetailsPreviewTag
import fr.aumombelli.dstcg.ui.component.TRADING_CARD_WIDTH_OVER_HEIGHT
import fr.aumombelli.dstcg.ui.viewmodel.LibraryUiState
import kotlin.math.abs
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class LibraryScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun back_button_is_visible_only_with_callback_and_invokes_it() {
        var backClicks = 0

        composeRule.setContent {
            LibraryScreen(
                state = LibraryUiState(isLoading = false),
                onRefresh = {},
                onBack = { backClicks += 1 },
            )
        }

        composeRule.onNodeWithTag("library-back").assertIsDisplayed()
        composeRule.onNodeWithTag("library-back").performClick()

        assertEquals(1, backClicks)
    }

    @Test
    fun owned_card_opens_preview_then_fullscreen_and_returns_to_library() {
        val ownedItem = LibraryCardItem(
            definition = testCardDefinition("M42", name = "Nebuleuse d'Orion"),
            extensionName = "Astronomes en herbe",
            ownedCount = 2,
            availableVariants = listOf(
                DisplayCardVariant("mountain", "Montagne", "standard", "Standard", false, 1),
                DisplayCardVariant("city", "Ville", "standard", "Standard", false, 1),
            ),
        )
        val unownedItem = LibraryCardItem(
            definition = testCardDefinition("M31", name = "Galaxie d'Andromede"),
            extensionName = "Astronomes en herbe",
            ownedCount = 0,
        )

        composeRule.setContent {
            LibraryScreen(
                state = LibraryUiState(
                    isLoading = false,
                    sections = listOf(
                        LibrarySection(
                            extension = ExtensionDefinition("astronomes-en-herbe", "Astronomes en herbe", "cover"),
                            cards = listOf(ownedItem, unownedItem),
                        ),
                    ),
                ),
                onRefresh = {},
            )
        }

        composeRule.onNodeWithTag("library-card-M31").assertHasNoClickAction()
        composeRule.onNodeWithTag("library-section-count-astronomes-en-herbe").assertTextEquals("1/2")
        composeRule.onNodeWithTag("library-rarity-section-astronomes-en-herbe-Common").assertIsDisplayed()
        composeRule.onAllNodesWithTag(
            "library-rarity-section-star-astronomes-en-herbe-Common",
            useUnmergedTree = true,
        ).assertCountEquals(1)
        composeRule.onAllNodesWithTag("library-owned-M42").assertCountEquals(0)
        composeRule.onAllNodesWithTag("library-owned-M31").assertCountEquals(0)
        composeRule.onAllNodesWithTag(CARD_BACKGROUND_HIDDEN_PLACEHOLDER_TAG, useUnmergedTree = true).assertCountEquals(1)
        composeRule.onNodeWithTag("library-card-M42").performClick()
        composeRule.onNodeWithTag("library-card-preview").assertIsDisplayed()
        composeRule.onNodeWithTag("library-card-preview-close").assertIsDisplayed()
        val closeBounds = composeRule.onNodeWithTag("library-card-preview-close").fetchSemanticsNode().boundsInRoot
        val cardBounds = composeRule.onNodeWithTag("library-card-preview-surface").fetchSemanticsNode().boundsInRoot
        assertTrue(closeBounds.left <= cardBounds.left + 4f)
        composeRule.onNodeWithTag("astro-card-variant-city-standard").performClick()
        composeRule.onNodeWithTag("library-card-preview-surface").performClick()
        composeRule.onNodeWithTag("astro-card-fullscreen").assertIsDisplayed()
        composeRule.onNodeWithTag(AstroCardDetailsPreviewTag).assertIsDisplayed()
        composeRule.onNodeWithTag("astro-card-fullscreen-close").performClick()
        composeRule.onAllNodesWithTag("library-card-preview").assertCountEquals(0)
        composeRule.onAllNodesWithTag("library-back").assertCountEquals(0)
    }

    @Test
    fun preview_swipes_between_visible_owned_cards_and_skips_unowned_cards() {
        val firstOwned = libraryCardItem(
            id = "M42",
            name = "Nébuleuse d'Orion",
            rarityLabel = "Common",
        )
        val unowned = libraryCardItem(
            id = "M31",
            name = "Galaxie d'Andromède",
            rarityLabel = "Common",
            variants = emptyList(),
        )
        val secondOwned = libraryCardItem(
            id = "M57",
            name = "Nébuleuse de la Lyre",
            rarityLabel = "Rare",
        )

        composeRule.setContent {
            LibraryScreen(
                state = LibraryUiState(
                    isLoading = false,
                    sections = listOf(
                        LibrarySection(
                            extension = ExtensionDefinition("astronomes-en-herbe", "Astronomes en herbe", "cover"),
                            cards = listOf(firstOwned, unowned, secondOwned),
                        ),
                    ),
                ),
                onRefresh = {},
            )
        }

        composeRule.onNodeWithTag("library-card-M42").performClick()
        composeRule.assertPreviewCurrentCard("M42")
        composeRule.onAllNodesWithTag("library-card-preview-arrow-left", useUnmergedTree = true)
            .assertCountEquals(0)
        composeRule.onAllNodesWithTag("library-card-preview-arrow-right", useUnmergedTree = true)
            .assertCountEquals(1)

        composeRule.onNodeWithTag("library-card-preview-pager").performTouchInput { swipeLeft() }
        composeRule.assertPreviewCurrentCard("M57")
        composeRule.onAllNodesWithTag("library-card-preview-arrow-left", useUnmergedTree = true)
            .assertCountEquals(1)
        composeRule.onAllNodesWithTag("library-card-preview-arrow-right", useUnmergedTree = true)
            .assertCountEquals(0)

        composeRule.onNodeWithTag("library-card-preview-pager").performTouchInput { swipeRight() }
        composeRule.assertPreviewCurrentCard("M42")
        composeRule.onNodeWithTag("library-card-preview-close").performClick()
        composeRule.onAllNodesWithTag("library-card-preview").assertCountEquals(0)
    }

    @Test
    fun fullscreen_swipes_between_cards_without_returning_to_library() {
        val firstOwned = libraryCardItem(id = "M42", name = "Nébuleuse d'Orion")
        val secondOwned = libraryCardItem(id = "M57", name = "Nébuleuse de la Lyre", rarityLabel = "Rare")

        composeRule.setContent {
            LibraryScreen(
                state = LibraryUiState(
                    isLoading = false,
                    sections = listOf(
                        LibrarySection(
                            extension = ExtensionDefinition("astronomes-en-herbe", "Astronomes en herbe", "cover"),
                            cards = listOf(firstOwned, secondOwned),
                        ),
                    ),
                ),
                onRefresh = {},
            )
        }

        composeRule.onNodeWithTag("library-card-M42").performClick()
        composeRule.onNodeWithTag("library-card-preview-surface").performClick()
        composeRule.onNodeWithTag("astro-card-fullscreen").assertIsDisplayed()
        composeRule.assertFullscreenCurrentCard("M42")

        composeRule.onNodeWithTag("astro-card-fullscreen-pager").performTouchInput { swipeLeft() }
        composeRule.assertFullscreenCurrentCard("M57")
        composeRule.onNodeWithTag("astro-card-fullscreen").assertIsDisplayed()

        composeRule.onNodeWithTag("astro-card-fullscreen-pager").performTouchInput { swipeRight() }
        composeRule.assertFullscreenCurrentCard("M42")
        composeRule.onNodeWithTag("astro-card-fullscreen-close").performClick()
        composeRule.onAllNodesWithTag("astro-card-fullscreen").assertCountEquals(0)
        composeRule.onAllNodesWithTag("library-card-preview").assertCountEquals(0)
    }

    @Test
    fun preview_swipe_navigation_is_limited_to_filtered_visible_cards() {
        val cityAlpha = libraryCardItem(
            id = "ALP-CITY",
            extensionId = "astronomes-en-herbe",
            name = "Carte ville alpha",
            variants = listOf(cityVariant(count = 1)),
        )
        val holographicBeta = libraryCardItem(
            id = "BET-HOLO",
            extensionId = "systeme-solaire",
            name = "Carte holographique bêta",
            variants = listOf(holographicVariant(count = 1)),
        )
        val cityBeta = libraryCardItem(
            id = "BET-CITY",
            extensionId = "systeme-solaire",
            name = "Carte ville beta",
            variants = listOf(cityVariant(count = 1)),
        )

        composeRule.setContent {
            LibraryScreen(
                state = LibraryUiState(
                    isLoading = false,
                    filterOptions = libraryFilterOptions(),
                    sections = listOf(
                        LibrarySection(
                            extension = ExtensionDefinition("astronomes-en-herbe", "Astronomes en herbe", "cover"),
                            cards = listOf(cityAlpha),
                        ),
                        LibrarySection(
                            extension = ExtensionDefinition("systeme-solaire", "Système solaire", "cover"),
                            cards = listOf(holographicBeta, cityBeta),
                        ),
                    ),
                ),
                onRefresh = {},
            )
        }

        composeRule.clickLibraryFilter("library-filter-extension-systeme-solaire")
        composeRule.clickLibraryFilter("library-filter-sky-city")
        composeRule.onAllNodesWithTag("library-card-BET-HOLO").assertCountEquals(0)
        composeRule.onNodeWithTag("library-card-BET-CITY").performClick()

        composeRule.assertPreviewCurrentCard("BET-CITY")
        composeRule.onAllNodesWithTag("library-card-preview-arrow-left", useUnmergedTree = true)
            .assertCountEquals(0)
        composeRule.onAllNodesWithTag("library-card-preview-arrow-right", useUnmergedTree = true)
            .assertCountEquals(0)
        composeRule.onNodeWithTag("library-card-preview-pager").performTouchInput { swipeLeft() }
        composeRule.assertPreviewCurrentCard("BET-CITY")
    }

    @Test
    fun library_sections_show_rarity_subsections_in_sort_order() {
        val commonItem = LibraryCardItem(
            definition = testCardDefinition("ALP-001", name = "Amas ouvert", rarityLabel = "Common"),
            extensionName = "Astronomes en herbe",
            ownedCount = 1,
            availableVariants = listOf(
                DisplayCardVariant("city", "Ville", "standard", "Standard", false, 1),
            ),
        )
        val rareItem = LibraryCardItem(
            definition = testCardDefinition("ALP-002", name = "Nebuleuse rare", rarityLabel = "Rare"),
            extensionName = "Astronomes en herbe",
            ownedCount = 1,
            availableVariants = listOf(
                DisplayCardVariant("city", "Ville", "standard", "Standard", false, 1),
            ),
        )

        composeRule.setContent {
            LibraryScreen(
                state = LibraryUiState(
                    isLoading = false,
                    sections = listOf(
                        LibrarySection(
                            extension = ExtensionDefinition("astronomes-en-herbe", "Astronomes en herbe", "cover"),
                            cards = listOf(rareItem, commonItem),
                        ),
                    ),
                ),
                onRefresh = {},
            )
        }

        val commonHeaderBounds = composeRule.onNodeWithTag("library-rarity-section-astronomes-en-herbe-Common")
            .fetchSemanticsNode()
            .boundsInRoot
        val rareHeaderBounds = composeRule.onNodeWithTag("library-rarity-section-astronomes-en-herbe-Rare")
            .fetchSemanticsNode()
            .boundsInRoot

        assertTrue(commonHeaderBounds.top < rareHeaderBounds.top)
    }

    @Test
    fun library_card_click_and_return_play_navigation_sound() {
        val ownedItem = LibraryCardItem(
            definition = testCardDefinition("M42", name = "Nebuleuse d'Orion"),
            extensionName = "Astronomes en herbe",
            ownedCount = 1,
            availableVariants = listOf(
                DisplayCardVariant("city", "Ville", "standard", "Standard", false, 1),
            ),
        )
        val audioController = RecordingAudioController()

        composeRule.setContent {
            CompositionLocalProvider(LocalAudioController provides audioController) {
                LibraryScreen(
                    state = LibraryUiState(
                        isLoading = false,
                        sections = listOf(
                            LibrarySection(
                                extension = ExtensionDefinition("astronomes-en-herbe", "Astronomes en herbe", "cover"),
                                cards = listOf(ownedItem),
                            ),
                        ),
                    ),
                    onRefresh = {},
                )
            }
        }

        composeRule.onNodeWithTag("library-card-M42").performClick()
        composeRule.onNodeWithTag("library-card-preview").assertIsDisplayed()
        assertEquals(listOf(SoundCue.UiNavigate), audioController.playedCues)

        audioController.clearPlayedCues()
        composeRule.onNodeWithTag("library-card-preview-close").performClick()
        composeRule.onAllNodesWithTag("library-card-preview").assertCountEquals(0)
        assertEquals(listOf(SoundCue.UiNavigate), audioController.playedCues)

        audioController.clearPlayedCues()
        composeRule.onNodeWithTag("library-card-M42").performClick()
        composeRule.onNodeWithTag("library-card-preview").assertIsDisplayed()
        audioController.clearPlayedCues()

        composeRule.onNodeWithTag("library-card-preview-surface").performClick()
        composeRule.onNodeWithTag("astro-card-fullscreen").assertIsDisplayed()
        assertEquals(listOf(SoundCue.UiNavigate), audioController.playedCues)

        audioController.clearPlayedCues()
        composeRule.onNodeWithTag("astro-card-fullscreen-close").performClick()
        composeRule.onAllNodesWithTag("astro-card-fullscreen").assertCountEquals(0)
        composeRule.onAllNodesWithTag("library-card-preview").assertCountEquals(0)
        assertEquals(listOf(SoundCue.UiNavigate), audioController.playedCues)
    }

    @Test
    fun preview_card_can_close_with_close_button() {
        val ownedItem = LibraryCardItem(
            definition = testCardDefinition("M42", name = "Nebuleuse d'Orion"),
            extensionName = "Astronomes en herbe",
            ownedCount = 1,
            availableVariants = listOf(
                DisplayCardVariant("city", "Ville", "standard", "Standard", false, 1),
            ),
        )

        composeRule.setContent {
            LibraryScreen(
                state = LibraryUiState(
                    isLoading = false,
                    sections = listOf(
                        LibrarySection(
                            extension = ExtensionDefinition("astronomes-en-herbe", "Astronomes en herbe", "cover"),
                            cards = listOf(ownedItem),
                        ),
                    ),
                ),
                onRefresh = {},
            )
        }

        composeRule.onNodeWithTag("library-card-M42").performClick()
        composeRule.onNodeWithTag("library-card-preview-close").performClick()

        composeRule.onAllNodesWithTag("library-card-preview").assertCountEquals(0)
    }

    @Test
    fun preview_close_button_matches_library_back_button_position() {
        val ownedItem = LibraryCardItem(
            definition = testCardDefinition("M42", name = "Nebuleuse d'Orion"),
            extensionName = "Astronomes en herbe",
            ownedCount = 1,
            availableVariants = listOf(
                DisplayCardVariant("city", "Ville", "standard", "Standard", false, 1),
            ),
        )

        composeRule.setContent {
            LibraryScreen(
                state = LibraryUiState(
                    isLoading = false,
                    sections = listOf(
                        LibrarySection(
                            extension = ExtensionDefinition("astronomes-en-herbe", "Astronomes en herbe", "cover"),
                            cards = listOf(ownedItem),
                        ),
                    ),
                ),
                onRefresh = {},
                onBack = {},
            )
        }

        val libraryBackBounds = composeRule.onNodeWithTag("library-back").fetchSemanticsNode().boundsInRoot

        composeRule.onNodeWithTag("library-card-M42").performClick()

        val previewCloseBounds = composeRule.onNodeWithTag("library-card-preview-close")
            .fetchSemanticsNode()
            .boundsInRoot
        assertTrue(abs(previewCloseBounds.left - libraryBackBounds.left) <= 1f)
        assertTrue(abs(previewCloseBounds.top - libraryBackBounds.top) <= 1f)
    }

    @Test
    fun preview_card_can_close_with_tap_outside_card() {
        val ownedItem = LibraryCardItem(
            definition = testCardDefinition("M42", name = "Nebuleuse d'Orion"),
            extensionName = "Astronomes en herbe",
            ownedCount = 1,
            availableVariants = listOf(
                DisplayCardVariant("city", "Ville", "standard", "Standard", false, 1),
            ),
        )

        composeRule.setContent {
            LibraryScreen(
                state = LibraryUiState(
                    isLoading = false,
                    sections = listOf(
                        LibrarySection(
                            extension = ExtensionDefinition("astronomes-en-herbe", "Astronomes en herbe", "cover"),
                            cards = listOf(ownedItem),
                        ),
                    ),
                ),
                onRefresh = {},
            )
        }

        composeRule.onNodeWithTag("library-card-M42").performClick()
        composeRule.onNodeWithTag("library-card-preview").performTouchInput {
            click(Offset(1f, 1f))
        }

        composeRule.onAllNodesWithTag("library-card-preview").assertCountEquals(0)
    }

    @Test
    fun preview_card_shrinks_to_keep_variants_and_trade_visible_on_compact_height() {
        val ownedItem = LibraryCardItem(
            definition = testCardDefinition("M42", name = "Nebuleuse d'Orion"),
            extensionName = "Astronomes en herbe",
            ownedCount = 3,
            availableVariants = listOf(
                DisplayCardVariant("city", "Ville", "standard", "Standard", false, 2),
                DisplayCardVariant("mountain", "Montagne", "standard", "Standard", false, 1),
            ),
        )

        composeRule.setContent {
            Box(modifier = Modifier.size(width = 360.dp, height = 520.dp)) {
                LibraryScreen(
                    state = LibraryUiState(
                        isLoading = false,
                        sections = listOf(
                            LibrarySection(
                                extension = ExtensionDefinition(
                                    "astronomes-en-herbe",
                                    "Astronomes en herbe",
                                    "cover",
                                ),
                                cards = listOf(ownedItem),
                            ),
                        ),
                    ),
                    onRefresh = {},
                )
            }
        }

        composeRule.onNodeWithTag("library-card-M42").performClick()
        val previewBounds = composeRule.onNodeWithTag("library-card-preview-surface").fetchSemanticsNode().boundsInRoot
        val variantBounds = composeRule.onNodeWithTag("astro-card-variant-city-standard").fetchSemanticsNode().boundsInRoot
        val tradeBounds = composeRule.onNodeWithTag("library-card-trade").fetchSemanticsNode().boundsInRoot

        composeRule.assertApproxCardRatio("library-card-preview-surface")
        assertTrue(previewBounds.bottom <= variantBounds.top)
        assertTrue(variantBounds.bottom <= tradeBounds.top)
    }

    @Test
    fun filter_chips_show_extension_logo_and_rarity_star() {
        composeRule.setContent {
            LibraryScreen(
                state = LibraryUiState(
                    isLoading = false,
                    filterOptions = LibraryFilterOptions(
                        extensions = listOf(
                            LibraryFilterOption("systeme-solaire", "Système solaire"),
                        ),
                        rarities = listOf(
                            LibraryFilterOption("Rare", "Rare"),
                        ),
                    ),
                ),
                onRefresh = {},
            )
        }

        composeRule.onNodeWithTag("library-filter-extension-systeme-solaire").assertIsDisplayed()
        composeRule.onNodeWithTag("library-filter-rarity-Rare").assertIsDisplayed()
        composeRule.onAllNodesWithTag("library-filter-extension-logo-systeme-solaire", useUnmergedTree = true)
            .assertCountEquals(1)
        composeRule.onAllNodesWithTag("library-filter-rarity-star-Rare", useUnmergedTree = true)
            .assertCountEquals(1)
    }

    @Test
    fun filters_combine_with_tradeable_filter() {
        val alphaCommon = LibraryCardItem(
            definition = testCardDefinition("ALP-001", extensionId = "astronomes-en-herbe", rarityLabel = "Common"),
            extensionName = "Astronomes en herbe",
            ownedCount = 2,
            availableVariants = listOf(
                DisplayCardVariant("city", "Ville", "standard", "Standard", false, 2),
            ),
        )
        val betaHolographicTradeable = LibraryCardItem(
            definition = testCardDefinition("BET-001", extensionId = "systeme-solaire", rarityLabel = "Rare"),
            extensionName = "Système solaire",
            ownedCount = 2,
            availableVariants = listOf(
                DisplayCardVariant("holographic", "Holographique", "standard", "Standard", true, 2),
            ),
        )
        val betaHolographicSingle = LibraryCardItem(
            definition = testCardDefinition("BET-002", extensionId = "systeme-solaire", rarityLabel = "Rare"),
            extensionName = "Système solaire",
            ownedCount = 3,
            availableVariants = listOf(
                DisplayCardVariant("holographic", "Holographique", "standard", "Standard", true, 1),
                DisplayCardVariant("city", "Ville", "standard", "Standard", false, 2),
            ),
        )
        val betaUnowned = LibraryCardItem(
            definition = testCardDefinition("BET-003", extensionId = "systeme-solaire", rarityLabel = "Rare"),
            extensionName = "Système solaire",
            ownedCount = 0,
        )

        composeRule.setContent {
            LibraryScreen(
                state = LibraryUiState(
                    isLoading = false,
                    filterOptions = libraryFilterOptions(),
                    sections = listOf(
                        LibrarySection(
                            extension = ExtensionDefinition(
                                "astronomes-en-herbe",
                                "Astronomes en herbe",
                                "cover",
                            ),
                            cards = listOf(alphaCommon),
                        ),
                        LibrarySection(
                            extension = ExtensionDefinition("systeme-solaire", "Système solaire", "cover"),
                            cards = listOf(betaHolographicTradeable, betaHolographicSingle, betaUnowned),
                        ),
                    ),
                ),
                onRefresh = {},
            )
        }

        composeRule.onNodeWithTag("library-filter-panel").assertIsDisplayed()
        composeRule.assertLibrarySectionCount("astronomes-en-herbe", "1/1")
        composeRule.assertLibrarySectionCount("systeme-solaire", "2/3")
        composeRule.clickLibraryFilter("library-filter-extension-systeme-solaire")
        composeRule.clickLibraryFilter("library-filter-rarity-Rare")

        composeRule.onAllNodesWithTag("library-card-ALP-001").assertCountEquals(0)
        composeRule.assertLibrarySectionCount("systeme-solaire", "2/3")
        composeRule.assertLibraryNodeDisplayed("library-card-BET-001")
        composeRule.assertLibraryNodeDisplayed("library-card-BET-002")
        composeRule.assertLibraryNodeDisplayed("library-card-BET-003")

        composeRule.clickLibraryFilter("library-filter-sky-city")

        composeRule.onAllNodesWithTag("library-card-BET-001").assertCountEquals(0)
        composeRule.assertLibrarySectionCount("systeme-solaire", "1/3")
        composeRule.assertLibraryNodeDisplayed("library-card-BET-002")
        composeRule.onAllNodesWithTag("library-card-BET-003").assertCountEquals(0)
        composeRule.onAllNodesWithText("Ville · Standard").assertCountEquals(1)
        composeRule.onAllNodesWithText("Holographique · Standard").assertCountEquals(0)

        composeRule.clickLibraryFilter("library-filter-sky-city")
        composeRule.clickLibraryFilter("library-filter-sky-holographic")

        composeRule.assertLibrarySectionCount("systeme-solaire", "2/3")
        composeRule.assertLibraryNodeDisplayed("library-card-BET-001")
        composeRule.assertLibraryNodeDisplayed("library-card-BET-002")
        composeRule.onAllNodesWithTag("library-card-BET-003").assertCountEquals(0)

        composeRule.clickLibraryFilter("library-filter-tradeable")

        composeRule.assertLibraryFilterSelected("library-filter-extension-systeme-solaire")
        composeRule.assertLibraryFilterSelected("library-filter-rarity-Rare")
        composeRule.assertLibraryFilterSelected("library-filter-sky-holographic")
        composeRule.assertLibraryFilterSelected("library-filter-tradeable")
        composeRule.assertLibrarySectionCount("systeme-solaire", "1/3")
        composeRule.assertLibraryNodeDisplayed("library-card-BET-001")
        composeRule.onAllNodesWithTag("library-card-BET-002").assertCountEquals(0)
        composeRule.onAllNodesWithTag("library-card-BET-003").assertCountEquals(0)
    }

    @Test
    fun tradeable_filter_displays_best_tradeable_variant() {
        val ownedItem = LibraryCardItem(
            definition = testCardDefinition("M42", name = "Nebuleuse d'Orion"),
            extensionName = "Astronomes en herbe",
            ownedCount = 4,
            availableVariants = listOf(
                DisplayCardVariant("city", "Ville", "standard", "Standard", false, 2),
                DisplayCardVariant("holographic", "Holographique", "standard", "Standard", true, 2),
            ),
        )

        composeRule.setContent {
            LibraryScreen(
                state = LibraryUiState(
                    isLoading = false,
                    filterOptions = libraryFilterOptions(),
                    sections = listOf(
                        LibrarySection(
                            extension = ExtensionDefinition("astronomes-en-herbe", "Astronomes en herbe", "cover"),
                            cards = listOf(ownedItem),
                        ),
                    ),
                ),
                onRefresh = {},
            )
        }

        composeRule.onAllNodesWithText("Ville · Standard").assertCountEquals(1)

        composeRule.clickLibraryFilter("library-filter-tradeable")

        composeRule.onAllNodesWithText("Ville · Standard").assertCountEquals(0)
        composeRule.onAllNodesWithText("Holographique · Standard").assertCountEquals(1)
        composeRule.onNodeWithTag("library-card-M42").performClick()
        composeRule.onNodeWithTag("astro-card-variant-holographic-standard").assertIsSelected()
        composeRule.onNodeWithTag("library-card-trade").assertIsDisplayed()
    }

    @Test
    fun preview_card_uses_trading_card_ratio() {
        val ownedItem = LibraryCardItem(
            definition = testCardDefinition("M42", name = "Nebuleuse d'Orion"),
            extensionName = "Astronomes en herbe",
            ownedCount = 1,
            availableVariants = listOf(
                DisplayCardVariant("city", "Ville", "standard", "Standard", false, 1),
            ),
        )

        composeRule.setContent {
            LibraryScreen(
                state = LibraryUiState(
                    isLoading = false,
                    sections = listOf(
                        LibrarySection(
                            extension = ExtensionDefinition("astronomes-en-herbe", "Astronomes en herbe", "cover"),
                            cards = listOf(ownedItem),
                        ),
                    ),
                ),
                onRefresh = {},
            )
        }

        composeRule.onNodeWithTag("library-card-M42").performClick()
        composeRule.onNodeWithTag("library-card-preview-surface").assertIsDisplayed()
        composeRule.assertApproxCardRatio("library-card-preview-surface")
    }

    @Test
    fun onboarding_hint_is_displayed_when_requested() {
        val ownedItem = LibraryCardItem(
            definition = testCardDefinition("M42", name = "Nebuleuse d'Orion"),
            extensionName = "Astronomes en herbe",
            ownedCount = 1,
            availableVariants = listOf(
                DisplayCardVariant("city", "Ville", "standard", "Standard", false, 1),
            ),
        )

        composeRule.setContent {
            LibraryScreen(
                state = LibraryUiState(
                    isLoading = false,
                    sections = listOf(
                        LibrarySection(
                            extension = ExtensionDefinition("astronomes-en-herbe", "Astronomes en herbe", "cover"),
                            cards = listOf(ownedItem),
                        ),
                    ),
                ),
                onRefresh = {},
                showOnboardingHint = true,
            )
        }

        composeRule.onNodeWithTag("library-onboarding-hint").assertIsDisplayed()
    }

    @Test
    fun variant_walkthrough_pages_block_library_until_finished() {
        val extensions = listOf(
            ExtensionDefinition("astronomes-en-herbe", "Astronomes en herbe", "cover"),
        )
        val walkthroughPages = buildLibraryOnboardingVariantWalkthroughPages(
            extensions = extensions,
            cards = listOf(testCardDefinition("ALP-001", name = "Nebuleuse d'Orion")),
            variantProfiles = testLibraryVariantProfiles(),
        )
        val ownedItem = LibraryCardItem(
            definition = testCardDefinition("ALP-001", name = "Nebuleuse d'Orion"),
            extensionName = "Astronomes en herbe",
            ownedCount = 1,
            availableVariants = listOf(
                DisplayCardVariant("city", "Ville", "standard", "Standard", false, 1),
            ),
        )
        var walkthroughCompleted = false

        composeRule.setContent {
            LibraryScreen(
                state = LibraryUiState(
                    isLoading = false,
                    sections = listOf(
                        LibrarySection(
                            extension = extensions.single(),
                            cards = listOf(ownedItem),
                        ),
                    ),
                    onboardingVariantWalkthroughPages = walkthroughPages,
                ),
                onRefresh = {},
                interactionsEnabled = false,
                showOnboardingVariantWalkthrough = true,
                onOnboardingVariantWalkthroughCompleted = { walkthroughCompleted = true },
            )
        }

        composeRule.onNodeWithTag("new-player-modal-library-variants").assertIsDisplayed()
        composeRule.onAllNodesWithTag("aster-mascot").assertCountEquals(0)
        composeRule.onNodeWithTag("new-player-modal-page-0").assertIsDisplayed()
        composeRule.assertNodeInsideModalVerticalBounds(
            nodeTag = "new-player-modal-next",
            modalTag = "new-player-modal-library-variants",
        )
        composeRule.onAllNodesWithTag("library-card-preview").assertCountEquals(0)

        composeRule.onNodeWithTag("library-card-ALP-001").performClick()
        composeRule.onAllNodesWithTag("library-card-preview").assertCountEquals(0)

        composeRule.onNodeWithTag("new-player-modal-page-0").performTouchInput { swipeLeft() }
        composeRule.onNodeWithTag("new-player-modal-page-1").assertIsDisplayed()
        composeRule.onNodeWithTag("new-player-modal-page-1").performTouchInput { swipeRight() }
        composeRule.onNodeWithTag("new-player-modal-page-0").assertIsDisplayed()
        composeRule.onNodeWithTag("new-player-modal-next").performClick()
        composeRule.onNodeWithTag("new-player-modal-page-1").assertIsDisplayed()
        composeRule.onNodeWithTag("new-player-modal-page-1").performTouchInput { swipeLeft() }
        composeRule.onNodeWithTag("new-player-modal-page-2").assertIsDisplayed()
        composeRule.onNodeWithTag("new-player-modal-next").performClick()
        composeRule.onNodeWithTag("new-player-modal-page-3").assertIsDisplayed()
        composeRule.onNodeWithTag("new-player-modal-page-3").performTouchInput { swipeLeft() }
        composeRule.onNodeWithTag("new-player-modal-page-3").assertIsDisplayed()
        composeRule.onNodeWithTag("new-player-modal-finish").performClick()

        assertTrue(walkthroughCompleted)
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

    private fun androidx.compose.ui.test.junit4.ComposeContentTestRule.clickLibraryFilter(tag: String) {
        onNodeWithTag(LIBRARY_GRID_TAG).performScrollToIndex(0)
        onNodeWithTag(tag).performClick()
    }

    private fun androidx.compose.ui.test.junit4.ComposeContentTestRule.assertLibraryFilterSelected(tag: String) {
        onNodeWithTag(LIBRARY_GRID_TAG).performScrollToIndex(0)
        onNodeWithTag(tag).assertIsSelected()
    }

    private fun androidx.compose.ui.test.junit4.ComposeContentTestRule.assertLibrarySectionCount(
        extensionId: String,
        expectedText: String,
    ) {
        val tag = "library-section-count-$extensionId"
        assertLibraryNodeHasText(tag, expectedText)
    }

    private fun androidx.compose.ui.test.junit4.ComposeContentTestRule.assertLibraryNodeHasText(
        tag: String,
        expectedText: String,
    ) {
        onNodeWithTag(LIBRARY_GRID_TAG).performScrollToNode(hasTestTag(tag))
        onNodeWithTag(tag).assertTextEquals(expectedText)
    }

    private fun androidx.compose.ui.test.junit4.ComposeContentTestRule.assertLibraryNodeDisplayed(tag: String) {
        onNodeWithTag(LIBRARY_GRID_TAG).performScrollToNode(hasTestTag(tag))
        onNodeWithTag(tag).assertIsDisplayed()
    }

    private fun androidx.compose.ui.test.junit4.ComposeContentTestRule.assertNodeInsideModalVerticalBounds(
        nodeTag: String,
        modalTag: String,
    ) {
        val modalBounds = onNodeWithTag(modalTag, useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        val nodeBounds = onNodeWithTag(nodeTag, useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        assertTrue(
            "Expected $nodeTag vertical bounds $nodeBounds to stay inside modal bounds $modalBounds",
            nodeBounds.top >= modalBounds.top && nodeBounds.bottom <= modalBounds.bottom,
        )
    }

    private fun androidx.compose.ui.test.junit4.ComposeContentTestRule.assertPreviewCurrentCard(cardId: String) {
        waitForIdle()
        onNodeWithTag("library-card-preview-current-id", useUnmergedTree = true).assertTextEquals(cardId)
    }

    private fun androidx.compose.ui.test.junit4.ComposeContentTestRule.assertFullscreenCurrentCard(cardId: String) {
        waitForIdle()
        onNodeWithTag("astro-card-fullscreen-current-id", useUnmergedTree = true).assertTextEquals(cardId)
    }

    private fun libraryCardItem(
        id: String,
        extensionId: String = "astronomes-en-herbe",
        name: String,
        rarityLabel: String = "Common",
        variants: List<DisplayCardVariant> = listOf(cityVariant(count = 1)),
    ): LibraryCardItem =
        LibraryCardItem(
            definition = testCardDefinition(
                id = id,
                extensionId = extensionId,
                name = name,
                rarityLabel = rarityLabel,
            ),
            extensionName = extensionId,
            ownedCount = variants.sumOf { it.count },
            availableVariants = variants,
        )

    private fun cityVariant(count: Int): DisplayCardVariant =
        DisplayCardVariant("city", "Ville", "standard", "Standard", false, count)

    private fun holographicVariant(count: Int): DisplayCardVariant =
        DisplayCardVariant("holographic", "Holographique", "standard", "Standard", true, count)

    private class RecordingAudioController : AudioController {
        private val mutableSettings = MutableStateFlow(AudioSettings())
        private val playedCueStorage = mutableListOf<SoundCue>()

        override val settings: StateFlow<AudioSettings> = mutableSettings.asStateFlow()

        val playedCues: List<SoundCue>
            get() = synchronized(playedCueStorage) { playedCueStorage.toList() }

        override fun play(cue: SoundCue) {
            synchronized(playedCueStorage) {
                playedCueStorage += cue
            }
        }

        override fun setAmbient(track: AmbientTrack?) = Unit

        override suspend fun setEnabled(enabled: Boolean) {
            mutableSettings.value = mutableSettings.value.copy(enabled = enabled)
        }

        override fun onAppForegrounded() = Unit

        override fun onAppBackgrounded() = Unit

        override fun release() = Unit

        fun clearPlayedCues() {
            synchronized(playedCueStorage) {
                playedCueStorage.clear()
            }
        }
    }

    private companion object {
        const val CARD_BACKGROUND_HIDDEN_PLACEHOLDER_TAG = "astro-card-background-hidden-placeholder"
        const val LIBRARY_GRID_TAG = "library-grid"
    }
}

private fun libraryFilterOptions(): LibraryFilterOptions =
    LibraryFilterOptions(
        extensions = listOf(
            LibraryFilterOption("systeme-solaire", "Système solaire"),
            LibraryFilterOption("astronomes-en-herbe", "Astronomes en herbe"),
        ),
        rarities = listOf(
            LibraryFilterOption("Common", "Common"),
            LibraryFilterOption("Rare", "Rare"),
        ),
        skyQualities = listOf(
            LibraryFilterOption("city", "Ville"),
            LibraryFilterOption("holographic", "Holographique"),
        ),
    )

private fun testLibraryVariantProfiles(): List<VariantProfile> = listOf(
    VariantProfile(
        id = "observation-default",
        skyQualities = listOf(
            SkyQualityDefinition("city", "Ville"),
            SkyQualityDefinition("suburban", "Periurbain"),
            SkyQualityDefinition("rural", "Campagne"),
            SkyQualityDefinition("mountain", "Montagne"),
            SkyQualityDefinition("holographic", "Holographique", isHolographic = true),
        ),
        finishes = listOf(
            CardFinishDefinition("standard", "Standard"),
            CardFinishDefinition("stamped", "Tamponnee", isStamped = true),
        ),
    ),
)
