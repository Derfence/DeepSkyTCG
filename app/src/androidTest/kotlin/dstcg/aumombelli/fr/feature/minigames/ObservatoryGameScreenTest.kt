package fr.aumombelli.dstcg.feature.minigames

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import fr.aumombelli.dstcg.model.DisplayCard
import fr.aumombelli.dstcg.model.DisplayCardVariant
import fr.aumombelli.dstcg.model.MiniGameDifficulty
import fr.aumombelli.dstcg.testCardDefinition
import fr.aumombelli.dstcg.ui.theme.DstcgTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class ObservatoryGameScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun difficulty_selection_displays_targets_and_requests_choice() {
        var selectedDifficulty: MiniGameDifficulty? = null

        setObservatoryContent(
            screen = MiniGamesScreenUiState.ObservatoryDifficultySelection,
            stateOverride = MiniGamesUiState(
                isLoading = false,
                observatoryDifficultyChoices = MiniGameDifficulty.entries.map { difficulty ->
                    val spec = ObservatoryDifficultySpec.forDifficulty(difficulty)
                    ObservatoryDifficultyChoiceUi(
                        difficulty = difficulty,
                        title = difficulty.displayName,
                        targetLabel = spec.targetLabel,
                        precisionLabel = spec.precisionLabel,
                        rewardLabel = formatReward(difficulty.reward),
                        enabled = difficulty == MiniGameDifficulty.Apprentice,
                        locked = difficulty != MiniGameDifficulty.Apprentice,
                        statusLabel = if (difficulty == MiniGameDifficulty.Apprentice) {
                            "Disponible"
                        } else {
                            "À débloquer"
                        },
                    )
                },
            ),
            onSelectDifficulty = { selectedDifficulty = it },
        )

        composeRule.onNodeWithTag("observatory-difficulty-selection").assertIsDisplayed()
        composeRule.onNodeWithText("1 cible - ±12% - 15min").assertIsDisplayed()
        composeRule.onNodeWithText("4 cibles - ±4% - 1h").assertIsDisplayed()

        composeRule.onNodeWithTag("observatory-difficulty-apprentice")
            .assertIsEnabled()
            .performClick()

        composeRule.runOnIdle {
            assertEquals(MiniGameDifficulty.Apprentice, selectedDifficulty)
        }
    }

    @Test
    fun playing_screen_displays_controls_and_capture_state() {
        setObservatoryContent(
            screen = MiniGamesScreenUiState.ObservatoryPlaying(
                difficultyName = "Apprenti",
                rewardLabel = "15min",
                targetIndex = 0,
                targetCount = 1,
                targetCard = observatoryCard(),
                step = ObservatoryStep.Capture,
                stepTitle = "Capture",
                stepInstruction = "La cible est prête.",
                domeProgress = 1f,
                azimuth = 0.42f,
                altitude = 0.56f,
                focus = 0.61f,
                targetAzimuthLabel = "42%",
                targetAltitudeLabel = "56%",
                targetFocusLabel = "61%",
                toleranceLabel = "±12%",
                domeReady = true,
                alignmentReady = true,
                focusReady = true,
                canClearCloud = false,
                canCapture = true,
                feedbackEvent = null,
            ),
        )

        composeRule.onNodeWithTag("observatory-playing").assertIsDisplayed()
        composeRule.onNodeWithTag("observatory-target-card").assertIsDisplayed()
        composeRule.onNodeWithTag("observatory-dome-slider").assertIsDisplayed()
        composeRule.onNodeWithTag("observatory-azimuth-slider").assertIsDisplayed()
        composeRule.onNodeWithTag("observatory-altitude-slider").assertIsDisplayed()
        composeRule.onNodeWithTag("observatory-focus-slider").assertIsDisplayed()
        composeRule.onNodeWithTag("observatory-clear-cloud").assertIsNotEnabled()
        composeRule.onNodeWithTag("observatory-capture").assertIsEnabled()
    }

    @Test
    fun result_displays_reward_and_next_unlock() {
        setObservatoryContent(
            screen = MiniGamesScreenUiState.ObservatoryResult(
                difficultyName = "Apprenti",
                rewardLabel = "15min",
                targetCount = 1,
                nextDifficultyName = "Observateur",
            ),
        )

        composeRule.onNodeWithTag("observatory-result").assertIsDisplayed()
        composeRule.onNodeWithText("Observation terminée").assertIsDisplayed()
        composeRule.onNodeWithText("Observateur débloqué").assertIsDisplayed()
    }

    private fun setObservatoryContent(
        screen: MiniGamesScreenUiState,
        stateOverride: MiniGamesUiState = MiniGamesUiState(isLoading = false),
        onSelectDifficulty: (MiniGameDifficulty) -> Unit = {},
    ) {
        composeRule.setContent {
            DstcgTheme {
                ObservatoryGameScreen(
                    state = stateOverride.copy(screen = screen),
                    onBackToMenu = {},
                    onSelectDifficulty = onSelectDifficulty,
                    onSetDomeProgress = {},
                    onSetAzimuth = {},
                    onSetAltitude = {},
                    onSetFocus = {},
                    onClearCloud = {},
                    onCapture = {},
                )
            }
        }
    }

    private fun observatoryCard(): DisplayCard {
        val variant = DisplayCardVariant(
            skyQuality = "city",
            skyQualityLabel = "Ville",
            finish = "standard",
            finishLabel = "Standard",
            isHolographic = false,
        )
        return DisplayCard(
            definition = testCardDefinition(
                id = "ALP-001",
                name = "M42",
            ),
            extensionName = "Alpha",
            activeVariant = variant,
        )
    }
}
