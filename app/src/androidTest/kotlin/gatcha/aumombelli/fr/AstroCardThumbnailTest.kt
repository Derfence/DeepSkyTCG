package fr.aumombelli.gatcha

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import fr.aumombelli.gatcha.model.DisplayCardVariant
import fr.aumombelli.gatcha.model.LibraryCardItem
import fr.aumombelli.gatcha.ui.component.AstroCardThumbnail
import org.junit.Rule
import org.junit.Test

class AstroCardThumbnailTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun thumbnail_hides_center_catalog_number_and_variant_label() {
        val item = LibraryCardItem(
            definition = testCardDefinition("M42", name = "Nebuleuse d'Orion"),
            extensionName = "Astronomes en herbe",
            ownedCount = 1,
            availableVariants = listOf(
                DisplayCardVariant("city", "Ville", "standard", "Standard", false, 1),
            ),
        )

        composeRule.setContent {
            AstroCardThumbnail(
                item = item,
                onClick = {},
            )
        }

        composeRule.onNodeWithTag("library-card-M42").assertIsDisplayed()
        composeRule.onAllNodesWithText("M42").assertCountEquals(0)
        composeRule.onAllNodesWithText("Ville · Standard").assertCountEquals(0)
    }
}
