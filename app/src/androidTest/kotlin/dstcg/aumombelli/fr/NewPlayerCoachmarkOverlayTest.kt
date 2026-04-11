package fr.aumombelli.dstcg

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import fr.aumombelli.dstcg.app.NewPlayerCoachmarkOverlay
import fr.aumombelli.dstcg.app.NewPlayerCoachmarkSpec
import fr.aumombelli.dstcg.app.NewPlayerOnboardingTarget
import org.junit.Rule
import org.junit.Test

class NewPlayerCoachmarkOverlayTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun equipment_activation_shows_scroll_arrow_when_forced() {
        composeRule.setContent {
            Box(modifier = Modifier.size(width = 360.dp, height = 640.dp)) {
                NewPlayerCoachmarkOverlay(
                    spec = NewPlayerCoachmarkSpec(
                        target = NewPlayerOnboardingTarget.EquipmentActivation,
                        title = "Active ta carte",
                        message = "Active cet equipement pour ameliorer les prochains packs.",
                    ),
                    targetBounds = null,
                    forceScrollDownHint = true,
                    modifier = Modifier.size(width = 360.dp, height = 640.dp),
                )
            }
        }

        composeRule.onNodeWithTag("new-player-coachmark-scroll-down").assertIsDisplayed()
        composeRule.onAllNodesWithTag("new-player-coachmark-EquipmentActivation").assertCountEquals(0)
    }

    @Test
    fun equipment_activation_shows_text_bubble_when_target_is_visible() {
        composeRule.setContent {
            Box(modifier = Modifier.size(width = 360.dp, height = 640.dp)) {
                NewPlayerCoachmarkOverlay(
                    spec = NewPlayerCoachmarkSpec(
                        target = NewPlayerOnboardingTarget.EquipmentActivation,
                        title = "Active ta carte",
                        message = "Active cet equipement pour ameliorer les prochains packs.",
                    ),
                    targetBounds = Rect(
                        left = 72f,
                        top = 280f,
                        right = 288f,
                        bottom = 336f,
                    ),
                    modifier = Modifier.size(width = 360.dp, height = 640.dp),
                )
            }
        }

        composeRule.onNodeWithTag("new-player-coachmark-EquipmentActivation").assertIsDisplayed()
        composeRule.onAllNodesWithTag("new-player-coachmark-scroll-down").assertCountEquals(0)
    }
}
