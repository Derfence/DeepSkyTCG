package fr.aumombelli.dstcg

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeRight
import fr.aumombelli.dstcg.feature.library.buildLibraryOnboardingVariantWalkthroughPages
import fr.aumombelli.dstcg.model.CardFinishDefinition
import fr.aumombelli.dstcg.model.DisplayCardVariant
import fr.aumombelli.dstcg.model.ExtensionDefinition
import fr.aumombelli.dstcg.model.LibraryCardItem
import fr.aumombelli.dstcg.model.LibrarySection
import fr.aumombelli.dstcg.model.SkyQualityDefinition
import fr.aumombelli.dstcg.model.VariantProfile
import fr.aumombelli.dstcg.ui.screen.LibraryScreen
import fr.aumombelli.dstcg.ui.component.TRADING_CARD_WIDTH_OVER_HEIGHT
import fr.aumombelli.dstcg.ui.viewmodel.LibraryUiState
import kotlin.math.abs
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
        composeRule.onAllNodesWithTag(CARD_BACKGROUND_HIDDEN_PLACEHOLDER_TAG, useUnmergedTree = true).assertCountEquals(1)
        composeRule.onNodeWithTag("library-card-M42").performClick()
        composeRule.onNodeWithTag("library-card-preview").assertIsDisplayed()
        composeRule.onNodeWithTag("astro-card-variant-city-standard").performClick()
        composeRule.onNodeWithTag("library-card-preview-surface").performClick()
        composeRule.onNodeWithTag("astro-card-fullscreen").assertIsDisplayed()
        composeRule.onNodeWithTag("astro-card-fullscreen-close").performClick()
        composeRule.onAllNodesWithTag("library-card-preview").assertCountEquals(0)
        composeRule.onAllNodesWithTag("library-back").assertCountEquals(0)
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

    private companion object {
        const val CARD_BACKGROUND_HIDDEN_PLACEHOLDER_TAG = "astro-card-background-hidden-placeholder"
    }
}

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
