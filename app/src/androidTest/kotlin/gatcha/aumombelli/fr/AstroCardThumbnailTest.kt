package fr.aumombelli.gatcha

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import fr.aumombelli.gatcha.model.DisplayCardVariant
import fr.aumombelli.gatcha.model.LibraryCardItem
import fr.aumombelli.gatcha.ui.component.AstroCardThumbnail
import kotlin.math.abs
import org.junit.Assert.assertTrue
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

    @Test
    fun thumbnails_keep_constant_surface_size_when_title_wraps() {
        val shortName = LibraryCardItem(
            definition = testCardDefinition("M42", name = "M42"),
            extensionName = "Astronomes en herbe",
            ownedCount = 1,
            availableVariants = listOf(
                DisplayCardVariant("city", "Ville", "standard", "Standard", false, 1),
            ),
        )
        val longName = LibraryCardItem(
            definition = testCardDefinition("NGC7000", name = "Nebuleuse de l'Amerique du Nord"),
            extensionName = "Astronomes en herbe",
            ownedCount = 1,
            availableVariants = listOf(
                DisplayCardVariant("city", "Ville", "standard", "Standard", false, 1),
            ),
        )

        composeRule.setContent {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp),
            ) {
                AstroCardThumbnail(
                    item = shortName,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {},
                )
                AstroCardThumbnail(
                    item = longName,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {},
                )
            }
        }

        val shortBounds = composeRule
            .onNodeWithTag("library-card-surface-M42", useUnmergedTree = true)
            .fetchSemanticsNode().boundsInRoot
        val longBounds = composeRule
            .onNodeWithTag("library-card-surface-NGC7000", useUnmergedTree = true)
            .fetchSemanticsNode().boundsInRoot

        assertTrue("Expected thumbnail widths to stay aligned", abs(shortBounds.width - longBounds.width) <= 1f)
        assertTrue("Expected thumbnail heights to stay aligned", abs(shortBounds.height - longBounds.height) <= 1f)
    }
}
