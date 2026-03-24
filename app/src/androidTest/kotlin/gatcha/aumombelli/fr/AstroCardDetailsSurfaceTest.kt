package fr.aumombelli.gatcha

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import fr.aumombelli.gatcha.model.DisplayCardVariant
import fr.aumombelli.gatcha.model.toDisplayCard
import fr.aumombelli.gatcha.ui.component.AstroCardDetailsSurface
import org.junit.Rule
import org.junit.Test

class AstroCardDetailsSurfaceTest {
    @get:Rule
    val composeRule = createComposeRule()

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
}
