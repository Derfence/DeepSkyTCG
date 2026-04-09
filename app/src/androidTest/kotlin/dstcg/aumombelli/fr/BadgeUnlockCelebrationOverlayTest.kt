package fr.aumombelli.dstcg

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
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
import fr.aumombelli.dstcg.feature.badges.BadgeItem
import fr.aumombelli.dstcg.feature.badges.BadgeProgress
import fr.aumombelli.dstcg.feature.badges.BadgeRequirementType
import fr.aumombelli.dstcg.feature.badges.BadgeUnlockCelebrationOverlay
import fr.aumombelli.dstcg.ui.theme.DstcgTheme
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue

class BadgeUnlockCelebrationOverlayTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun celebration_stays_visible_until_badge_reaches_its_destination() {
        composeRule.mainClock.autoAdvance = false
        var celebrationVisible by mutableStateOf(true)
        composeRule.setContent {
            DstcgTheme {
                Box(modifier = Modifier.fillMaxSize()) {
                    BadgeUnlockCelebrationOverlay(
                        badges = listOf(sampleBadge()),
                        targetBounds = Rect(left = 900f, top = 2100f, right = 1044f, bottom = 2244f),
                        visible = celebrationVisible,
                        onFinished = { celebrationVisible = false },
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }

        composeRule.mainClock.advanceTimeBy(950)
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("badge-unlock-celebration").assertIsDisplayed()
        composeRule.onNodeWithTag("badge-unlock-celebration-coin-astro::sky::city").assertIsDisplayed()

        composeRule.mainClock.advanceTimeBy(500)
        composeRule.waitForIdle()
        composeRule.onAllNodesWithTag("badge-unlock-celebration").assertCountEquals(0)
    }

    @Test
    fun celebration_lands_relative_to_target_bounds_while_title_remains_visible() {
        composeRule.mainClock.autoAdvance = false
        var celebrationVisible by mutableStateOf(true)
        lateinit var density: Density
        val targetBounds = Rect(left = 900f, top = 2100f, right = 1044f, bottom = 2244f)

        composeRule.setContent {
            density = LocalDensity.current
            DstcgTheme {
                Box(modifier = Modifier.fillMaxSize()) {
                    BadgeUnlockCelebrationOverlay(
                        badges = listOf(sampleBadge()),
                        targetBounds = targetBounds,
                        visible = celebrationVisible,
                        onFinished = { celebrationVisible = false },
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }

        composeRule.mainClock.advanceTimeBy(300)
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("badge-unlock-celebration-title").assertIsDisplayed()

        composeRule.mainClock.advanceTimeBy(780)
        composeRule.waitForIdle()

        val finalBadgeBounds = composeRule
            .onNodeWithTag("badge-unlock-celebration-coin-astro::sky::city")
            .fetchSemanticsNode()
            .boundsInRoot
        val finalTitleBounds = composeRule
            .onNodeWithTag("badge-unlock-celebration-title")
            .fetchSemanticsNode()
            .boundsInRoot
        val landingGapPx = with(density) { 10.dp.toPx() }

        assertEquals((targetBounds.left + targetBounds.right) / 2f, finalBadgeBounds.center.x, 4f)
        assertEquals(targetBounds.top + landingGapPx, finalBadgeBounds.top, 8f)
        composeRule.onNodeWithTag("badge-unlock-celebration-title").assertIsDisplayed()
        assertTrue(finalTitleBounds.width > 0f)
        assertTrue(finalTitleBounds.height > 0f)
        assertTrue(finalBadgeBounds.top > 100f)
    }

    private fun sampleBadge(): BadgeItem = BadgeItem(
        id = "astro::sky::city",
        extensionId = "astro",
        extensionName = "Astro",
        title = "Ville",
        description = "Description",
        requirementType = BadgeRequirementType.SkyQuality,
        progress = BadgeProgress(matchedCards = 1, totalCards = 1),
        skyQualityCode = "city",
    )
}
