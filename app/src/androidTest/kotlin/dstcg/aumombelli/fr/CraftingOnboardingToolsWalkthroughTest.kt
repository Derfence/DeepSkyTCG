package fr.aumombelli.dstcg

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import fr.aumombelli.dstcg.feature.crafting.CraftingOnboardingToolsWalkthrough
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class CraftingOnboardingToolsWalkthroughTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun walkthrough_displays_two_tool_pages_with_explicit_costs() {
        var completed = false

        composeRule.setContent {
            CraftingOnboardingToolsWalkthrough(
                onCompleted = { completed = true },
            )
        }

        composeRule.onNodeWithTag("new-player-modal-crafting-tools").assertIsDisplayed()
        composeRule.onNodeWithTag("new-player-modal-page-0").assertIsDisplayed()
        composeRule.onNodeWithText("Assombrir le ciel").assertIsDisplayed()
        composeRule.onNodeWithText("Ville -> Périurbain").assertIsDisplayed()
        composeRule.onNodeWithText("2 exemplaires Ville").assertIsDisplayed()
        composeRule.onNodeWithText("Montagne -> Holographique").assertIsDisplayed()
        composeRule.onNodeWithText("6 exemplaires Montagne").assertIsDisplayed()

        composeRule.onNodeWithTag("new-player-modal-next").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("new-player-modal-page-1").assertIsDisplayed()
        composeRule.onNodeWithText("Agence spatiale").assertIsDisplayed()
        composeRule.onNodeWithText("Standard -> Tamponnée").assertIsDisplayed()
        composeRule.onNodeWithText("10 exemplaires standard").assertIsDisplayed()

        composeRule.onNodeWithTag("new-player-modal-finish").performClick()
        composeRule.waitForIdle()

        assertTrue(completed)
    }
}
