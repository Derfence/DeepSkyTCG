package fr.aumombelli.dstcg

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import fr.aumombelli.dstcg.app.NewPlayerCoachmarkOverlay
import fr.aumombelli.dstcg.app.NewPlayerCoachmarkPlacement
import fr.aumombelli.dstcg.app.NewPlayerCoachmarkSpec
import fr.aumombelli.dstcg.app.NewPlayerOnboardingTarget
import kotlin.math.abs
import org.junit.Assert.assertTrue
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

    @Test
    fun text_bubble_wraps_shorter_messages_without_previous_minimum_height() {
        lateinit var density: Density

        composeRule.setContent {
            density = LocalDensity.current
            Box(modifier = Modifier.size(width = 360.dp, height = 640.dp)) {
                NewPlayerCoachmarkOverlay(
                    spec = NewPlayerCoachmarkSpec(
                        target = NewPlayerOnboardingTarget.HomeBadges,
                        title = "Badges",
                        message = "Ouvre ici.",
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

        val bubbleBounds = composeRule
            .onNodeWithTag("new-player-coachmark-HomeBadges", useUnmergedTree = true)
            .fetchSemanticsNode()
            .boundsInRoot
        val previousFixedHeightPx = with(density) { 108.dp.toPx() }
        assertTrue(
            "Expected short coachmark bubble height to stay below the previous fixed $previousFixedHeightPx px height, but was ${bubbleBounds.height}",
            bubbleBounds.height < previousFixedHeightPx,
        )
    }

    @Test
    fun centered_text_only_coachmark_does_not_draw_target_highlight() {
        lateinit var targetBounds: Rect

        composeRule.setContent {
            val density = LocalDensity.current
            targetBounds = with(density) {
                Rect(
                    left = 64.dp.toPx(),
                    top = 176.dp.toPx(),
                    right = 296.dp.toPx(),
                    bottom = 420.dp.toPx(),
                )
            }
            Box(modifier = Modifier.size(width = 360.dp, height = 640.dp)) {
                NewPlayerCoachmarkOverlay(
                    spec = NewPlayerCoachmarkSpec(
                        target = NewPlayerOnboardingTarget.PackSelectionBooster,
                        title = "Choisis un booster",
                        message = "Touche le booster de ton choix pour en révéler le contenu ⭐",
                        placement = NewPlayerCoachmarkPlacement.CenteredOnTarget,
                        showTargetHighlight = false,
                    ),
                    targetBounds = targetBounds,
                    modifier = Modifier.size(width = 360.dp, height = 640.dp),
                )
            }
        }

        composeRule.onNodeWithTag("new-player-coachmark-PackSelectionBooster").assertIsDisplayed()
        composeRule.onAllNodesWithTag("new-player-coachmark-target-PackSelectionBooster").assertCountEquals(0)
        composeRule.waitForIdle()

        val bubbleBounds = composeRule
            .onNodeWithTag("new-player-coachmark-PackSelectionBooster", useUnmergedTree = true)
            .fetchSemanticsNode()
            .boundsInRoot
        val targetCenterX = targetBounds.left + targetBounds.width / 2f
        val targetCenterY = targetBounds.top + targetBounds.height / 2f
        val bubbleCenterX = bubbleBounds.left + bubbleBounds.width / 2f
        val bubbleCenterY = bubbleBounds.top + bubbleBounds.height / 2f
        assertTrue(
            "Expected text-only coachmark bubble center ($bubbleCenterX, $bubbleCenterY) to align with target center ($targetCenterX, $targetCenterY).",
            abs(bubbleCenterX - targetCenterX) <= 2f &&
                abs(bubbleCenterY - targetCenterY) <= 2f,
        )
    }

    @Test
    fun text_bubble_grows_to_fit_longer_messages() {
        lateinit var density: Density

        composeRule.setContent {
            density = LocalDensity.current
            Box(modifier = Modifier.size(width = 360.dp, height = 640.dp)) {
                NewPlayerCoachmarkOverlay(
                    spec = NewPlayerCoachmarkSpec(
                        target = NewPlayerOnboardingTarget.HomeCrafting,
                        title = "Atelier de fabrication",
                        message = "Une carte peut être améliorée grâce à ses doublons. Ouvre l'atelier pour découvrir comment faire sans perdre le fil de l'onboarding.",
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

        val bubbleBounds = composeRule
            .onNodeWithTag("new-player-coachmark-HomeCrafting", useUnmergedTree = true)
            .fetchSemanticsNode()
            .boundsInRoot
        val previousFixedHeightPx = with(density) { 108.dp.toPx() }
        assertTrue(
            "Expected coachmark bubble height to grow beyond the previous fixed $previousFixedHeightPx px height, but was ${bubbleBounds.height}",
            bubbleBounds.height > previousFixedHeightPx,
        )
    }
}
