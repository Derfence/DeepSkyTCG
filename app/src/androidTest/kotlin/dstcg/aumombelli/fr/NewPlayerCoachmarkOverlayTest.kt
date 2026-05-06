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
import fr.aumombelli.dstcg.app.NewPlayerCoachmarkTargetEffect
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
                        targetEffect = NewPlayerCoachmarkTargetEffect.None,
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
    fun below_target_coachmark_places_text_under_the_target_when_space_allows() {
        lateinit var targetBounds: Rect

        composeRule.setContent {
            val density = LocalDensity.current
            targetBounds = with(density) {
                Rect(
                    left = 72.dp.toPx(),
                    top = 384.dp.toPx(),
                    right = 288.dp.toPx(),
                    bottom = 432.dp.toPx(),
                )
            }
            Box(modifier = Modifier.size(width = 360.dp, height = 720.dp)) {
                NewPlayerCoachmarkOverlay(
                    spec = NewPlayerCoachmarkSpec(
                        target = NewPlayerOnboardingTarget.PackSelectionExtension,
                        title = "Choisis une extension",
                        message = "Choisissons cette collection pour commencer.",
                        placement = NewPlayerCoachmarkPlacement.BelowTarget,
                    ),
                    targetBounds = targetBounds,
                    modifier = Modifier.size(width = 360.dp, height = 720.dp),
                )
            }
        }

        composeRule.onNodeWithTag("new-player-coachmark-PackSelectionExtension").assertIsDisplayed()
        composeRule.waitForIdle()

        val bubbleBounds = composeRule
            .onNodeWithTag("new-player-coachmark-PackSelectionExtension", useUnmergedTree = true)
            .fetchSemanticsNode()
            .boundsInRoot
        assertTrue(
            "Expected extension coachmark bubble top ${bubbleBounds.top} to stay below target bottom ${targetBounds.bottom}.",
            bubbleBounds.top > targetBounds.bottom,
        )
    }

    @Test
    fun home_open_pack_coachmark_sits_over_card_text_area() {
        lateinit var density: Density
        lateinit var targetBounds: Rect

        composeRule.setContent {
            density = LocalDensity.current
            targetBounds = with(density) {
                Rect(
                    left = 64.dp.toPx(),
                    top = 120.dp.toPx(),
                    right = 296.dp.toPx(),
                    bottom = 520.dp.toPx(),
                )
            }
            Box(modifier = Modifier.size(width = 360.dp, height = 640.dp)) {
                NewPlayerCoachmarkOverlay(
                    spec = NewPlayerCoachmarkSpec(
                        target = NewPlayerOnboardingTarget.HomeOpenPack,
                        title = "Premières cartes",
                        message = "Commençons ta collection de cartes d'objets célestes !",
                        placement = NewPlayerCoachmarkPlacement.OverHomeCardText,
                    ),
                    targetBounds = targetBounds,
                    modifier = Modifier.size(width = 360.dp, height = 640.dp),
                )
            }
        }

        composeRule.onNodeWithTag("new-player-coachmark-HomeOpenPack").assertIsDisplayed()
        composeRule.waitForIdle()

        val bubbleBounds = composeRule
            .onNodeWithTag("new-player-coachmark-HomeOpenPack", useUnmergedTree = true)
            .fetchSemanticsNode()
            .boundsInRoot
        val expectedBubbleBottomPx = targetBounds.bottom - with(density) { 20.dp.toPx() }

        assertTrue(
            "Expected HomeOpenPack bubble bottom ${bubbleBounds.bottom} to align over the card text area at $expectedBubbleBottomPx.",
            abs(bubbleBounds.bottom - expectedBubbleBottomPx) <= with(density) { 2.dp.toPx() },
        )
        assertTrue(
            "Expected HomeOpenPack bubble top ${bubbleBounds.top} to stay inside the card bounds starting at ${targetBounds.top}.",
            bubbleBounds.top > targetBounds.top,
        )
    }

    @Test
    fun touch_zone_coachmark_draws_tap_hint_and_places_text_slightly_over_target_bottom() {
        lateinit var density: Density
        lateinit var targetBounds: Rect

        composeRule.setContent {
            density = LocalDensity.current
            targetBounds = with(density) {
                Rect(
                    left = 0.dp.toPx(),
                    top = 0.dp.toPx(),
                    right = 360.dp.toPx(),
                    bottom = 320.dp.toPx(),
                )
            }
            Box(modifier = Modifier.size(width = 360.dp, height = 640.dp)) {
                NewPlayerCoachmarkOverlay(
                    spec = NewPlayerCoachmarkSpec(
                        target = NewPlayerOnboardingTarget.CraftingDarkenSkyMode,
                        title = "Assombrir le ciel",
                        message = "L'une des deux améliorations permet d'assombrir le ciel d'une carte.",
                        placement = NewPlayerCoachmarkPlacement.OverlapTargetBottom,
                        targetEffect = NewPlayerCoachmarkTargetEffect.TouchZone,
                    ),
                    targetBounds = targetBounds,
                    modifier = Modifier.size(width = 360.dp, height = 640.dp),
                )
            }
        }

        composeRule.onNodeWithTag("new-player-coachmark-CraftingDarkenSkyMode").assertIsDisplayed()
        composeRule.onNodeWithTag("new-player-coachmark-touch-zone-CraftingDarkenSkyMode").assertIsDisplayed()
        composeRule.onNodeWithTag("new-player-coachmark-muted-zone-CraftingDarkenSkyMode").assertIsDisplayed()
        composeRule.onAllNodesWithTag("new-player-coachmark-target-CraftingDarkenSkyMode").assertCountEquals(0)
        composeRule.waitForIdle()

        val bubbleBounds = composeRule
            .onNodeWithTag("new-player-coachmark-CraftingDarkenSkyMode", useUnmergedTree = true)
            .fetchSemanticsNode()
            .boundsInRoot
        val targetCenterX = targetBounds.left + targetBounds.width / 2f
        val bubbleCenterX = bubbleBounds.left + bubbleBounds.width / 2f
        assertTrue(
            "Expected touch-zone coachmark bubble to stay horizontally centered on target.",
            abs(bubbleCenterX - targetCenterX) <= 2f,
        )
        assertTrue(
            "Expected touch-zone coachmark bubble top ${bubbleBounds.top} to overlap target bottom ${targetBounds.bottom} very slightly.",
            bubbleBounds.top < targetBounds.bottom &&
                targetBounds.bottom - bubbleBounds.top <= with(density) { 10.dp.toPx() },
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
