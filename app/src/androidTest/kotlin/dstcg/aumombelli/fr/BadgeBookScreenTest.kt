package fr.aumombelli.dstcg

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import fr.aumombelli.dstcg.feature.badges.BadgeBookScreen
import fr.aumombelli.dstcg.feature.badges.BadgeBookUiState
import fr.aumombelli.dstcg.feature.badges.BadgeItem
import fr.aumombelli.dstcg.feature.badges.BadgeProgress
import fr.aumombelli.dstcg.feature.badges.BadgeRequirementType
import fr.aumombelli.dstcg.feature.badges.BadgeSection
import fr.aumombelli.dstcg.feature.badges.BadgeSectionType
import fr.aumombelli.dstcg.ui.theme.DstcgTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class BadgeBookScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun clicking_a_badge_opens_detail_with_progress_and_description() {
        composeRule.mainClock.autoAdvance = false
        composeRule.setContent {
            DstcgTheme {
                BadgeBookScreen(
                    state = BadgeBookUiState(
                        isLoading = false,
                        sections = listOf(
                            BadgeSection(
                                extensionId = "astronomes-en-herbe",
                                extensionName = "Astronomes en herbe",
                                badges = listOf(
                                    BadgeItem(
                                        id = "astronomes-en-herbe::finish::stamped",
                                        extensionId = "astronomes-en-herbe",
                                        extensionName = "Astronomes en herbe",
                                        title = "Tamponnee",
                                        description = "Obtiens chaque carte de Astronomes en herbe en finition tamponnee, quelle que soit la qualite du ciel.",
                                        requirementType = BadgeRequirementType.Stamped,
                                        progress = BadgeProgress(
                                            matchedCards = 1,
                                            totalCards = 3,
                                        ),
                                    ),
                                ),
                            ),
                        ),
                    ),
                    onRefresh = {},
                )
            }
        }

        composeRule.waitForIdle()
        composeRule.onNodeWithTag("badge-coin-astronomes-en-herbe::finish::stamped").performClick()
        composeRule.mainClock.advanceTimeBy(700)
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("badge-detail").assertIsDisplayed()
        composeRule.onNodeWithTag("badge-detail-progress", useUnmergedTree = true).assertTextContains(
            "1 / 3",
            substring = true,
        )
        composeRule.onNodeWithTag("badge-detail-description").assertTextContains(
            "tamponnee",
            substring = true,
        )
    }

    @Test
    fun general_badge_logo_keeps_same_ratio_when_detail_coin_expands() {
        composeRule.mainClock.autoAdvance = false
        composeRule.setContent {
            DstcgTheme {
                BadgeBookScreen(
                    state = BadgeBookUiState(
                        isLoading = false,
                        sections = listOf(
                            BadgeSection(
                                extensionId = "general",
                                extensionName = "Badges generaux",
                                sectionType = BadgeSectionType.General,
                                badges = listOf(
                                    BadgeItem(
                                        id = "general::pack::first-opened",
                                        extensionId = "general",
                                        extensionName = "Badges generaux",
                                        title = "Premier pack",
                                        description = "Ouvre ton premier pack.",
                                        requirementType = BadgeRequirementType.FirstPackOpened,
                                        progress = BadgeProgress(
                                            matchedCards = 1,
                                            totalCards = 1,
                                            unitLabel = "packs",
                                        ),
                                    ),
                                ),
                            ),
                        ),
                    ),
                    onRefresh = {},
                )
            }
        }

        composeRule.waitForIdle()
        val gridCoinBounds = composeRule
            .onNodeWithTag("badge-coin-general::pack::first-opened")
            .fetchSemanticsNode()
            .boundsInRoot
        val gridMarkBounds = composeRule
            .onNodeWithTag("badge-center-mark-general::pack::first-opened", useUnmergedTree = true)
            .fetchSemanticsNode()
            .boundsInRoot
        val gridRatio = gridMarkBounds.width / gridCoinBounds.width

        composeRule.onNodeWithTag("badge-coin-general::pack::first-opened").performClick()
        composeRule.mainClock.advanceTimeBy(700)
        composeRule.waitForIdle()

        val detailCoinBounds = composeRule
            .onNodeWithTag("badge-detail-coin")
            .fetchSemanticsNode()
            .boundsInRoot
        val detailMarkBounds = composeRule
            .onNodeWithTag("badge-detail-center-mark", useUnmergedTree = true)
            .fetchSemanticsNode()
            .boundsInRoot
        val detailRatio = detailMarkBounds.width / detailCoinBounds.width

        assertEquals(gridRatio, detailRatio, 0.02f)
    }
}
