package fr.aumombelli.dstcg

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import fr.aumombelli.dstcg.model.DisplayCardVariant
import fr.aumombelli.dstcg.model.LibraryCardItem
import fr.aumombelli.dstcg.model.toDisplayCard
import fr.aumombelli.dstcg.ui.component.AstroCardPreviewSurface
import fr.aumombelli.dstcg.ui.component.AstroCardSurfaceMode
import fr.aumombelli.dstcg.ui.component.AstroCardThumbnail
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
        composeRule.onAllNodesWithTag(CARD_CATALOG_NUMBER_TAG, useUnmergedTree = true).assertCountEquals(1)
        composeRule.onAllNodesWithTag(CARD_VARIATION_TAG, useUnmergedTree = true).assertCountEquals(1)
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
        composeRule.onAllNodesWithTag(CARD_CATALOG_NUMBER_TAG, useUnmergedTree = true).assertCountEquals(0)
    }

    @Test
    fun thumbnail_hides_art_for_unowned_cards_without_removing_card_chrome() {
        val item = LibraryCardItem(
            definition = testCardDefinition("M31", name = "Galaxie d'Andromede"),
            extensionName = "Astronomes en herbe",
            ownedCount = 0,
        )

        composeRule.setContent {
            AstroCardThumbnail(
                item = item,
                onClick = {},
            )
        }

        composeRule.onNodeWithTag("library-card-M31").assertHasNoClickAction()
        composeRule.onAllNodesWithTag(CARD_BACKGROUND_HIDDEN_PLACEHOLDER_TAG, useUnmergedTree = true).assertCountEquals(1)
        composeRule.onAllNodesWithTag(CARD_BACKGROUND_ART_TAG, useUnmergedTree = true).assertCountEquals(0)
        composeRule.onAllNodesWithTag(CARD_BACKGROUND_FALLBACK_ASSET_TAG, useUnmergedTree = true).assertCountEquals(0)
        composeRule.onAllNodesWithText("Galaxie d'Andromede").assertCountEquals(1)
        composeRule.onAllNodesWithText("M31").assertCountEquals(1)
        composeRule.onAllNodesWithTag(CARD_EXTENSION_LOGO_TAG, useUnmergedTree = true).assertCountEquals(1)
        composeRule.onAllNodesWithTag("astro-card-rarity-common", useUnmergedTree = true).assertCountEquals(1)
    }

    @Test
    fun thumbnail_shows_new_indicator_when_card_is_marked_as_new() {
        val item = LibraryCardItem(
            definition = testCardDefinition("M42", name = "Nebuleuse d'Orion"),
            extensionName = "Astronomes en herbe",
            ownedCount = 1,
            showNewIndicator = true,
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

        composeRule.onNodeWithTag("library-card-new-indicator-M42", useUnmergedTree = true).assertIsDisplayed()
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
        composeRule.onAllNodesWithTag(CARD_EXTENSION_LOGO_TAG, useUnmergedTree = true).assertCountEquals(1)
        composeRule.onAllNodesWithTag("astro-card-rarity-rare", useUnmergedTree = true).assertCountEquals(1)
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

        composeRule.onAllNodesWithTag(CARD_BACKGROUND_FALLBACK_ASSET_TAG, useUnmergedTree = true).assertCountEquals(1)
        composeRule.onAllNodesWithTag(CARD_BACKGROUND_HIDDEN_PLACEHOLDER_TAG, useUnmergedTree = true).assertCountEquals(0)
    }

    @Test
    fun thumbnail_background_art_leaves_visible_sky_quality_frame() {
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
                modifier = Modifier.fillMaxWidth(),
                onClick = {},
            )
        }

        val surfaceBounds = composeRule
            .onNodeWithTag("library-card-surface-M42", useUnmergedTree = true)
            .fetchSemanticsNode().boundsInRoot
        val artBounds = composeRule
            .onNodeWithTag(CARD_BACKGROUND_ART_TAG, useUnmergedTree = true)
            .fetchSemanticsNode().boundsInRoot

        assertTrue("Expected thumbnail art width to leave a visible border", artBounds.width < surfaceBounds.width - 1f)
        assertTrue("Expected thumbnail art height to leave a visible border", artBounds.height < surfaceBounds.height - 1f)
    }

    @Test
    fun preview_background_art_leaves_visible_sky_quality_frame() {
        val item = LibraryCardItem(
            definition = testCardDefinition("M42", name = "Nebuleuse d'Orion"),
            extensionName = "Astronomes en herbe",
            ownedCount = 1,
            availableVariants = listOf(
                DisplayCardVariant("city", "Ville", "standard", "Standard", false, 1),
            ),
        )
        val displayCard = item.toDisplayCard() ?: error("Expected display card")

        composeRule.setContent {
            AstroCardPreviewSurface(
                displayCard = displayCard,
                mode = AstroCardSurfaceMode.Preview,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("preview-card-surface"),
            )
        }

        val surfaceBounds = composeRule
            .onNodeWithTag("preview-card-surface", useUnmergedTree = true)
            .fetchSemanticsNode().boundsInRoot
        val artBounds = composeRule
            .onNodeWithTag(CARD_BACKGROUND_ART_TAG, useUnmergedTree = true)
            .fetchSemanticsNode().boundsInRoot

        assertTrue("Expected preview art width to leave a visible border", artBounds.width < surfaceBounds.width - 1f)
        assertTrue("Expected preview art height to leave a visible border", artBounds.height < surfaceBounds.height - 1f)
    }

    @Test
    fun holographic_thumbnail_and_preview_keep_foil_border_without_explicit_motion() {
        val holographicVariant = DisplayCardVariant(
            skyQuality = "holographic",
            skyQualityLabel = "Holographique",
            finish = "standard",
            finishLabel = "Standard",
            isHolographic = true,
            count = 1,
        )
        val item = LibraryCardItem(
            definition = testCardDefinition("M42", name = "Nebuleuse d'Orion"),
            extensionName = "Astronomes en herbe",
            ownedCount = 1,
            availableVariants = listOf(holographicVariant),
        )
        val displayCard = item.toDisplayCard() ?: error("Expected display card")

        composeRule.setContent {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                AstroCardThumbnail(
                    item = item,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {},
                )
                AstroCardPreviewSurface(
                    displayCard = displayCard,
                    mode = AstroCardSurfaceMode.Preview,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("preview-card-surface"),
                )
            }
        }

        composeRule.onAllNodesWithTag("astro-card-holo-foil", useUnmergedTree = true).assertCountEquals(2)
        composeRule.onAllNodesWithTag("astro-card-holo-glare", useUnmergedTree = true).assertCountEquals(2)
    }

    private companion object {
        const val CARD_BACKGROUND_ART_TAG = "astro-card-background-art"
        const val CARD_BACKGROUND_FALLBACK_ASSET_TAG = "astro-card-background-fallback-asset"
        const val CARD_BACKGROUND_HIDDEN_PLACEHOLDER_TAG = "astro-card-background-hidden-placeholder"
        const val CARD_CATALOG_NUMBER_TAG = "astro-card-catalog-number"
        const val CARD_EXTENSION_LOGO_TAG = "astro-card-extension-logo"
        const val CARD_FOOTER_TAG = "astro-card-footer"
        const val CARD_VARIATION_TAG = "astro-card-variation"
    }
}
