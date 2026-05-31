package fr.aumombelli.dstcg

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.unit.dp
import fr.aumombelli.dstcg.model.DisplayCardVariant
import fr.aumombelli.dstcg.model.toDisplayCard
import fr.aumombelli.dstcg.ui.component.AstroCardDetailsPreviewTag
import fr.aumombelli.dstcg.ui.component.AstroCardDetailsSurface
import fr.aumombelli.dstcg.ui.component.TRADING_CARD_WIDTH_OVER_HEIGHT
import kotlin.math.abs
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class AstroCardDetailsSurfaceTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun details_preview_shrinks_to_fit_compact_height() {
        composeRule.setContent {
            Box(modifier = Modifier.size(width = 320.dp, height = 420.dp)) {
                AstroCardDetailsSurface(
                    displayCard = testCardDefinition(
                        id = "M42",
                        name = "Nebuleuse d'Orion",
                    ).toDisplayCard(
                        extensionName = "Astronomes en herbe",
                        activeVariant = DisplayCardVariant(
                            skyQuality = "city",
                            skyQualityLabel = "Ville",
                            finish = "standard",
                            finishLabel = "Standard",
                            isHolographic = false,
                        ),
                    ),
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }

        val previewBounds = composeRule.onNodeWithTag(AstroCardDetailsPreviewTag)
            .fetchSemanticsNode()
            .boundsInRoot
        val rootBounds = composeRule.onRoot().fetchSemanticsNode().boundsInRoot

        assertTrue(previewBounds.height < rootBounds.height)
        assertTrue(previewBounds.width < rootBounds.width)
        assertTrue(abs(previewBounds.width / previewBounds.height - TRADING_CARD_WIDTH_OVER_HEIGHT) <= 0.03f)
    }

    @Test
    fun sky_event_with_visual_size_shows_measurements_section() {
        composeRule.setContent {
            AstroCardDetailsSurface(
                displayCard = testSkyEventCardDefinition("evt-perseides", withVisualSize = true).toDisplayCard(
                    extensionName = "Astronomes en herbe",
                    activeVariant = DisplayCardVariant(
                        skyQuality = "rural",
                        skyQualityLabel = "Campagne",
                        finish = "standard",
                        finishLabel = "Standard",
                        isHolographic = false,
                    ),
                ),
            )
        }

        composeRule.onAllNodesWithText("Mesures").assertCountEquals(1)
        composeRule.onAllNodesWithText("Taille visuelle").assertCountEquals(1)
    }

    @Test
    fun sky_event_without_visual_size_hides_measurements_section() {
        composeRule.setContent {
            AstroCardDetailsSurface(
                displayCard = testSkyEventCardDefinition("evt-halley", name = "Comete de Halley", withVisualSize = false).toDisplayCard(
                    extensionName = "Astronomes en herbe",
                    activeVariant = DisplayCardVariant(
                        skyQuality = "city",
                        skyQualityLabel = "Ville",
                        finish = "standard",
                        finishLabel = "Standard",
                        isHolographic = false,
                    ),
                ),
            )
        }

        composeRule.onAllNodesWithText("Mesures").assertCountEquals(0)
    }

    @Test
    fun details_surface_shows_known_image_credit_artist() {
        composeRule.setContent {
            AstroCardDetailsSurface(
                displayCard = testCardDefinition(
                    id = "M42",
                    name = "Nebuleuse d'Orion",
                    imageRef = "m42_orion_nebula",
                ).toDisplayCard(
                    extensionName = "Astronomes en herbe",
                    activeVariant = DisplayCardVariant(
                        skyQuality = "rural",
                        skyQualityLabel = "Campagne",
                        finish = "standard",
                        finishLabel = "Standard",
                        isHolographic = false,
                    ),
                ),
            )
        }

        composeRule.onNodeWithTag("astro-card-image-credit").assertTextContains(
            "Min Xie",
            substring = true,
        )
        composeRule.onNodeWithTag("astro-card-image-license").assertTextContains(
            "Licence : CC BY-NC",
            substring = true,
        )
    }

    @Test
    fun details_surface_falls_back_to_inconnu_when_image_credit_is_missing() {
        composeRule.setContent {
            AstroCardDetailsSurface(
                displayCard = testCardDefinition(
                    id = "M13",
                    name = "Grand amas d'Hercule",
                    imageRef = "missing-credit",
                ).toDisplayCard(
                    extensionName = "Astronomes en herbe",
                    activeVariant = DisplayCardVariant(
                        skyQuality = "city",
                        skyQualityLabel = "Ville",
                        finish = "standard",
                        finishLabel = "Standard",
                        isHolographic = false,
                    ),
                ),
            )
        }

        composeRule.onNodeWithTag("astro-card-image-credit").assertTextContains(
            "Inconnu",
            substring = true,
        )
        composeRule.onNodeWithTag("astro-card-image-license").assertTextContains(
            "Licence : Inconnue",
            substring = true,
        )
    }
}
