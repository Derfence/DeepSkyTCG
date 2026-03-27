package fr.aumombelli.gatcha

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
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
    fun thumbnail_shows_catalog_number_and_variant_under_common_name() {
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
        composeRule.onAllNodesWithText("Nebuleuse d'Orion").assertCountEquals(1)
        composeRule.onAllNodesWithText("M42").assertCountEquals(1)
        composeRule.onAllNodesWithText("Ville · Standard").assertCountEquals(1)
        composeRule.onAllNodesWithTag(CARD_CATALOG_NUMBER_TAG).assertCountEquals(1)
        composeRule.onAllNodesWithTag(CARD_VARIATION_TAG).assertCountEquals(1)
    }

    @Test
    fun thumbnail_falls_back_to_catalog_number_when_common_name_is_missing() {
        val item = LibraryCardItem(
            definition = testCardDefinition(
                id = "NGC7000",
                name = "Nebuleuse catalogue",
                commonName = null,
                catalogNumber = "NGC 7000",
            ),
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

        composeRule.onAllNodesWithText("NGC 7000").assertCountEquals(1)
        composeRule.onAllNodesWithTag(CARD_CATALOG_NUMBER_TAG).assertCountEquals(0)
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

    @Test
    fun thumbnail_footer_stays_at_the_same_relative_height_when_title_wraps() {
        val shortName = LibraryCardItem(
            definition = testCardDefinition("M42", name = "M42"),
            extensionName = "Astronomes en herbe",
            ownedCount = 1,
            availableVariants = listOf(
                DisplayCardVariant("city", "Ville", "standard", "Standard", false, 1),
            ),
        )
        val longName = LibraryCardItem(
            definition = testCardDefinition(
                id = "NGC7000",
                name = "Nebuleuse de l'Amerique du Nord",
                commonName = "Nebuleuse de l'Amerique du Nord",
                catalogNumber = "NGC 7000",
            ),
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

        val surfaceNodes = composeRule.onAllNodesWithTag("library-card-surface-M42", useUnmergedTree = true)
        val shortSurface = surfaceNodes[0].fetchSemanticsNode().boundsInRoot
        val longSurface = composeRule
            .onNodeWithTag("library-card-surface-NGC7000", useUnmergedTree = true)
            .fetchSemanticsNode().boundsInRoot
        val footerNodes = composeRule.onAllNodesWithTag(CARD_FOOTER_TAG, useUnmergedTree = true)
        val shortFooter = footerNodes[0].fetchSemanticsNode().boundsInRoot
        val longFooter = footerNodes[1].fetchSemanticsNode().boundsInRoot

        val shortBottomOffset = shortSurface.bottom - shortFooter.bottom
        val longBottomOffset = longSurface.bottom - longFooter.bottom

        assertTrue(
            "Expected footer bottom offset to stay aligned",
            abs(shortBottomOffset - longBottomOffset) <= 1f,
        )
    }

    @Test
    fun thumbnail_footer_shows_constellation_extension_logo_and_rarity_star() {
        val item = LibraryCardItem(
            definition = testCardDefinition("M57", name = "Nebuleuse de la Lyre", rarityLabel = "Rare"),
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

        composeRule.onAllNodesWithText("Orion").assertCountEquals(1)
        composeRule.onAllNodesWithText("Astronomes en herbe").assertCountEquals(0)
        composeRule.onAllNodesWithText("Rareté").assertCountEquals(0)
        composeRule.onAllNodesWithTag(CARD_EXTENSION_LOGO_TAG).assertCountEquals(1)
        composeRule.onAllNodesWithTag("astro-card-rarity-rare").assertCountEquals(1)
    }

    @Test
    fun thumbnail_uses_shared_fallback_asset_when_card_art_is_missing() {
        val item = LibraryCardItem(
            definition = testCardDefinition(
                id = "M13",
                name = "Grand amas d'Hercule",
                imageRef = "missing-art",
            ),
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

        composeRule.onAllNodesWithTag(CARD_BACKGROUND_FALLBACK_ASSET_TAG).assertCountEquals(1)
    }

    private companion object {
        const val CARD_BACKGROUND_FALLBACK_ASSET_TAG = "astro-card-background-fallback-asset"
        const val CARD_CATALOG_NUMBER_TAG = "astro-card-catalog-number"
        const val CARD_EXTENSION_LOGO_TAG = "astro-card-extension-logo"
        const val CARD_FOOTER_TAG = "astro-card-footer"
        const val CARD_VARIATION_TAG = "astro-card-variation"
    }
}
