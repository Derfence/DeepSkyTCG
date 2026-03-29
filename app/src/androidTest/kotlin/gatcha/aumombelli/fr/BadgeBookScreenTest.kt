package fr.aumombelli.gatcha

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import fr.aumombelli.gatcha.feature.badges.BadgeBookScreen
import fr.aumombelli.gatcha.feature.badges.BadgeBookUiState
import fr.aumombelli.gatcha.feature.badges.BadgeItem
import fr.aumombelli.gatcha.feature.badges.BadgeProgress
import fr.aumombelli.gatcha.feature.badges.BadgeRequirementType
import fr.aumombelli.gatcha.feature.badges.BadgeSection
import fr.aumombelli.gatcha.ui.theme.GatchaTheme
import org.junit.Rule
import org.junit.Test

class BadgeBookScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun clicking_a_badge_opens_detail_with_progress_and_description() {
        composeRule.mainClock.autoAdvance = false
        composeRule.setContent {
            GatchaTheme {
                BadgeBookScreen(
                    state = BadgeBookUiState(
                        isLoading = false,
                        sections = listOf(
                            BadgeSection(
                                extensionId = "astronomes-en-herbe",
                                extensionName = "Astronomes en herbe",
                                badges = listOf(
                                    BadgeItem(
                                        id = "astronomes-en-herbe::finish::holographic",
                                        extensionId = "astronomes-en-herbe",
                                        extensionName = "Astronomes en herbe",
                                        title = "Holographique",
                                        description = "Obtiens chaque carte de Astronomes en herbe en holographique, quelle que soit la qualite du ciel.",
                                        requirementType = BadgeRequirementType.Holographic,
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
        composeRule.onNodeWithTag("badge-coin-astronomes-en-herbe::finish::holographic").performClick()
        composeRule.mainClock.advanceTimeBy(700)
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("badge-detail").assertIsDisplayed()
        composeRule.onNodeWithTag("badge-detail-progress", useUnmergedTree = true).assertTextContains(
            "1 / 3",
            substring = true,
        )
        composeRule.onNodeWithTag("badge-detail-description").assertTextContains(
            "holographique",
            substring = true,
        )
    }
}
