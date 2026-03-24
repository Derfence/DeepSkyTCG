package fr.aumombelli.gatcha

import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import fr.aumombelli.gatcha.model.DisplayCardVariant
import fr.aumombelli.gatcha.model.ExtensionDefinition
import fr.aumombelli.gatcha.model.LibraryCardItem
import fr.aumombelli.gatcha.model.LibrarySection
import fr.aumombelli.gatcha.ui.screen.LibraryScreen
import fr.aumombelli.gatcha.ui.viewmodel.LibraryUiState
import org.junit.Rule
import org.junit.Test

class LibraryScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun owned_card_opens_preview_then_fullscreen_while_unowned_is_passive() {
        val ownedItem = LibraryCardItem(
            definition = testCardDefinition("M42", name = "Nebuleuse d'Orion"),
            extensionName = "Astronomes en herbe",
            ownedCount = 2,
            availableVariants = listOf(
                DisplayCardVariant("mountain", "Montagne", "holographic", "Holographique", true, 1),
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
                onBack = {},
                onRefresh = {},
            )
        }

        composeRule.onNodeWithTag("library-card-M31").assertHasNoClickAction()
        composeRule.onNodeWithTag("library-card-M42").performClick()
        composeRule.onNodeWithTag("library-card-preview").assertIsDisplayed()
        composeRule.onNodeWithTag("astro-card-variant-city-standard").performClick()
        composeRule.onNodeWithTag("library-card-preview-surface").performClick()
        composeRule.onNodeWithTag("astro-card-fullscreen").assertIsDisplayed()
        composeRule.onNodeWithTag("astro-card-fullscreen-close").performClick()
        composeRule.onNodeWithTag("library-card-preview").assertIsDisplayed()
        composeRule.onNodeWithTag("library-card-preview-close").performClick()
    }
}
