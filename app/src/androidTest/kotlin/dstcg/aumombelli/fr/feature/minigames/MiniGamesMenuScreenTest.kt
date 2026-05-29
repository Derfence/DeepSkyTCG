package fr.aumombelli.dstcg.feature.minigames

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import fr.aumombelli.dstcg.ui.theme.DstcgTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class MiniGamesMenuScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun game_buttons_expose_daily_availability_states() {
        setMenuContent(
            MiniGamesUiState(
                isLoading = false,
                quizStatusLabel = "Disponible aujourd'hui",
                quizPlayedToday = false,
                memoryStatusLabel = "Essai utilisé aujourd'hui",
                memoryPlayedToday = true,
                observatoryStatusLabel = "Disponible Observatoire",
                observatoryPlayedToday = false,
            ),
        )

        composeRule.onNodeWithTag("mini-games-quiz")
            .assertIsDisplayed()
            .assertIsEnabled()
            .assert(hasStateDescription("Disponible"))
        composeRule.onNodeWithTag("mini-games-memory")
            .assertIsDisplayed()
            .assertIsEnabled()
            .assert(hasStateDescription("Essai quotidien consommé"))
        composeRule.onNodeWithTag("mini-games-timeline")
            .assertIsDisplayed()
            .assertIsEnabled()
            .assert(hasStateDescription("Disponible"))
        composeRule.onNodeWithTag("mini-games-observatory")
            .assertIsDisplayed()
            .assertIsEnabled()
            .assert(hasStateDescription("Disponible"))

        composeRule.onNodeWithText("Quiz universitaire").assertIsDisplayed()
        composeRule.onNodeWithText("Disponible aujourd'hui").assertIsDisplayed()
        composeRule.onNodeWithText("Memory amateur").assertIsDisplayed()
        composeRule.onNodeWithText("Essai utilisé aujourd'hui").assertIsDisplayed()
        composeRule.onNodeWithText("Comparaison").assertIsDisplayed()
        composeRule.onNodeWithText("Observatoire").assertIsDisplayed()
        composeRule.onNodeWithText("Disponible Observatoire").assertIsDisplayed()
    }

    @Test
    fun game_information_is_placed_on_the_side_with_more_available_space() {
        setMenuContent(
            MiniGamesUiState(
                isLoading = false,
                quizStatusLabel = "Disponible",
                memoryStatusLabel = "Disponible",
            ),
        )

        val quizButton = composeRule.onNodeWithTag("mini-games-quiz").fetchSemanticsNode().boundsInRoot
        val quizInfo = composeRule.onNodeWithTag("mini-games-quiz-info").fetchSemanticsNode().boundsInRoot
        val memoryButton = composeRule.onNodeWithTag("mini-games-memory").fetchSemanticsNode().boundsInRoot
        val memoryInfo = composeRule.onNodeWithTag("mini-games-memory-info").fetchSemanticsNode().boundsInRoot
        val timelineButton = composeRule.onNodeWithTag("mini-games-timeline").fetchSemanticsNode().boundsInRoot
        val timelineInfo = composeRule.onNodeWithTag("mini-games-timeline-info").fetchSemanticsNode().boundsInRoot
        val observatoryButton = composeRule.onNodeWithTag("mini-games-observatory").fetchSemanticsNode().boundsInRoot
        val observatoryInfo = composeRule.onNodeWithTag("mini-games-observatory-info").fetchSemanticsNode().boundsInRoot

        assertTrue(quizInfo.left >= quizButton.right)
        assertTrue(memoryInfo.left >= memoryButton.right)
        assertTrue(timelineInfo.right <= timelineButton.left)
        assertTrue(observatoryInfo.right <= observatoryButton.left)
    }

    @Test
    fun game_buttons_keep_a_wide_bottom_left_to_top_right_diagonal() {
        setMenuContent(
            MiniGamesUiState(
                isLoading = false,
                quizStatusLabel = "Disponible",
                memoryStatusLabel = "Disponible",
            ),
        )

        val root = composeRule.onNodeWithTag("mini-games-menu-screen").fetchSemanticsNode().boundsInRoot
        val quizButton = composeRule.onNodeWithTag("mini-games-quiz").fetchSemanticsNode().boundsInRoot
        val memoryButton = composeRule.onNodeWithTag("mini-games-memory").fetchSemanticsNode().boundsInRoot
        val timelineButton = composeRule.onNodeWithTag("mini-games-timeline").fetchSemanticsNode().boundsInRoot
        val observatoryButton = composeRule.onNodeWithTag("mini-games-observatory").fetchSemanticsNode().boundsInRoot

        val quizCenterX = centerX(quizButton)
        val quizCenterY = centerY(quizButton)
        val memoryCenterX = centerX(memoryButton)
        val memoryCenterY = centerY(memoryButton)
        val timelineCenterX = centerX(timelineButton)
        val timelineCenterY = centerY(timelineButton)
        val observatoryCenterX = centerX(observatoryButton)
        val observatoryCenterY = centerY(observatoryButton)

        assertTrue(quizCenterX < memoryCenterX)
        assertTrue(memoryCenterX < timelineCenterX)
        assertTrue(timelineCenterX < observatoryCenterX)
        assertTrue(quizCenterY > memoryCenterY)
        assertTrue(memoryCenterY > timelineCenterY)
        assertTrue(timelineCenterY > observatoryCenterY)
        assertTrue(observatoryCenterX - quizCenterX >= (root.right - root.left) * 0.55f)
        assertTrue(quizCenterY - observatoryCenterY >= (root.bottom - root.top) * 0.55f)
    }

    private fun setMenuContent(state: MiniGamesUiState) {
        composeRule.setContent {
            DstcgTheme {
                MiniGamesMenuScreen(
                    state = state,
                    onBack = {},
                    onOpenQuiz = {},
                    onOpenMemory = {},
                    onOpenTimeline = {},
                    onOpenObservatory = {},
                )
            }
        }
    }

    private fun hasStateDescription(value: String): SemanticsMatcher =
        SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, value)

    private fun centerX(bounds: androidx.compose.ui.geometry.Rect): Float =
        (bounds.left + bounds.right) / 2f

    private fun centerY(bounds: androidx.compose.ui.geometry.Rect): Float =
        (bounds.top + bounds.bottom) / 2f
}
