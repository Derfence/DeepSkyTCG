package fr.aumombelli.dstcg.feature.minigames

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
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
        composeRule.onNodeWithText("1 cible - ±6% - 15min").assertIsDisplayed()
        composeRule.onNodeWithText("4 cibles - ±2% - 1h").assertIsDisplayed()

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
            screen = observatoryPlaying(
                step = ObservatoryStep.Capture,
                stepTitle = "Capture",
                stepInstruction = "La cible est prête.",
                captureProgress = 0.6f,
                canCapture = true,
            ),
        )

        composeRule.onNodeWithTag("observatory-playing").assertIsDisplayed()
        composeRule.onNodeWithTag("observatory-stage").assertIsDisplayed()
        composeRule.onNodeWithTag("observatory-illustration-stage").assertIsDisplayed()
        composeRule.onNodeWithTag("observatory-hud").assertIsDisplayed()
        composeRule.onNodeWithTag("observatory-control-tray").assertIsDisplayed()
        composeRule.onNodeWithTag("observatory-capture-progress").assertIsDisplayed()
        composeRule.onNodeWithTag("observatory-capture-card").assertIsDisplayed()
        composeRule.onNodeWithTag("observatory-capture").assertIsEnabled()
        composeRule.onAllNodesWithTag("observatory-target-card").assertCountEquals(0)
        composeRule.onAllNodesWithTag("observatory-dome-slider").assertCountEquals(0)
        composeRule.onAllNodesWithTag("observatory-azimuth-slider").assertCountEquals(0)
        composeRule.onAllNodesWithTag("observatory-altitude-slider").assertCountEquals(0)
        composeRule.onAllNodesWithTag("observatory-altitude-side-control").assertCountEquals(0)
        composeRule.onAllNodesWithTag("observatory-focus-slider").assertCountEquals(0)
        composeRule.onAllNodesWithTag("observatory-focus-wheels").assertCountEquals(0)
        composeRule.onAllNodesWithTag("observatory-clear-cloud").assertCountEquals(0)
    }

    @Test
    fun playing_screen_displays_only_dome_control_during_opening() {
        setObservatoryContent(
            screen = observatoryPlaying(
                step = ObservatoryStep.OpenDome,
                stepTitle = "Ouverture de la coupole",
                stepInstruction = "Glisse jusqu'au bout pour ouvrir le panneau.",
            ),
        )

        composeRule.onNodeWithTag("observatory-dome-slider").assertIsDisplayed()
        composeRule.onAllNodesWithTag("observatory-azimuth-slider").assertCountEquals(0)
        composeRule.onAllNodesWithTag("observatory-altitude-slider").assertCountEquals(0)
        composeRule.onAllNodesWithTag("observatory-altitude-side-control").assertCountEquals(0)
        composeRule.onAllNodesWithTag("observatory-focus-slider").assertCountEquals(0)
        composeRule.onAllNodesWithTag("observatory-focus-wheels").assertCountEquals(0)
        composeRule.onAllNodesWithTag("observatory-clear-cloud").assertCountEquals(0)
        composeRule.onAllNodesWithTag("observatory-capture").assertCountEquals(0)
        composeRule.onAllNodesWithText("Repère 100%").assertCountEquals(0)
    }

    @Test
    fun playing_screen_displays_only_alignment_controls_during_alignment() {
        setObservatoryContent(
            screen = observatoryPlaying(
                step = ObservatoryStep.Align,
                stepTitle = "Alignement du télescope",
                stepInstruction = "Aligne le réticule mobile sur la cible lumineuse.",
            ),
        )

        composeRule.onNodeWithTag("observatory-azimuth-slider").assertIsDisplayed()
        composeRule.onNodeWithTag("observatory-altitude-side-control").assertIsDisplayed()
        composeRule.onNodeWithTag("observatory-altitude-slider").assertIsDisplayed()
        composeRule.onAllNodesWithTag("observatory-dome-slider").assertCountEquals(0)
        composeRule.onAllNodesWithTag("observatory-focus-slider").assertCountEquals(0)
        composeRule.onAllNodesWithTag("observatory-focus-wheels").assertCountEquals(0)
        composeRule.onAllNodesWithTag("observatory-clear-cloud").assertCountEquals(0)
        composeRule.onAllNodesWithTag("observatory-capture").assertCountEquals(0)
        composeRule.onAllNodesWithText("Repère 42%").assertCountEquals(0)
        composeRule.onAllNodesWithText("Repère 56%").assertCountEquals(0)
    }

    @Test
    fun playing_screen_displays_only_focus_control_during_focus() {
        setObservatoryContent(
            screen = observatoryPlaying(
                step = ObservatoryStep.Focus,
                stepTitle = "Mise au point",
                stepInstruction = "Fais coïncider l'anneau du réticule avec la cible.",
            ),
        )

        composeRule.onNodeWithTag("observatory-focus-wheels").assertIsDisplayed()
        composeRule.onNodeWithTag("observatory-focus-coarse-wheel").assertIsDisplayed()
        composeRule.onNodeWithTag("observatory-focus-fine-wheel").assertIsDisplayed()
        composeRule.onAllNodesWithTag("observatory-dome-slider").assertCountEquals(0)
        composeRule.onAllNodesWithTag("observatory-azimuth-slider").assertCountEquals(0)
        composeRule.onAllNodesWithTag("observatory-altitude-slider").assertCountEquals(0)
        composeRule.onAllNodesWithTag("observatory-altitude-side-control").assertCountEquals(0)
        composeRule.onAllNodesWithTag("observatory-focus-slider").assertCountEquals(0)
        composeRule.onAllNodesWithTag("observatory-clear-cloud").assertCountEquals(0)
        composeRule.onAllNodesWithTag("observatory-capture").assertCountEquals(0)
        composeRule.onAllNodesWithText("Repère 61%").assertCountEquals(0)
    }

    @Test
    fun playing_screen_displays_cloud_scrub_zone_during_cloud_pause() {
        var scrubbedAmount = 0f
        setObservatoryContent(
            screen = observatoryPlaying(
                step = ObservatoryStep.ClearCloud,
                stepTitle = "Nuage de passage",
                stepInstruction = "Efface-le avec ton doigt",
            ),
            onScrubCloud = { scrubbedAmount += it },
        )

        composeRule.onNodeWithTag("observatory-cloud-scrub-zone")
            .assertIsDisplayed()
            .performClick()
        composeRule.runOnIdle {
            assertEquals(ObservatoryCloudTapScrubAmount, scrubbedAmount, 0.001f)
        }
        composeRule.onAllNodesWithTag("observatory-clear-cloud").assertCountEquals(0)
        composeRule.onAllNodesWithTag("observatory-dome-slider").assertCountEquals(0)
        composeRule.onAllNodesWithTag("observatory-azimuth-slider").assertCountEquals(0)
        composeRule.onAllNodesWithTag("observatory-altitude-slider").assertCountEquals(0)
        composeRule.onAllNodesWithTag("observatory-altitude-side-control").assertCountEquals(0)
        composeRule.onAllNodesWithTag("observatory-focus-slider").assertCountEquals(0)
        composeRule.onAllNodesWithTag("observatory-focus-wheels").assertCountEquals(0)
        composeRule.onAllNodesWithTag("observatory-capture").assertCountEquals(0)
    }

    @Test
    fun playing_screen_displays_only_close_dome_control_during_closing() {
        setObservatoryContent(
            screen = observatoryPlaying(
                step = ObservatoryStep.CloseDome,
                stepTitle = "Fermeture de la coupole",
                stepInstruction = "Referme la coupole pour terminer l'observation.",
                domeProgress = 1f,
                domeClosed = false,
            ),
        )

        composeRule.onNodeWithTag("observatory-close-dome-slider").assertIsDisplayed()
        composeRule.onAllNodesWithTag("observatory-dome-slider").assertCountEquals(0)
        composeRule.onAllNodesWithTag("observatory-azimuth-slider").assertCountEquals(0)
        composeRule.onAllNodesWithTag("observatory-altitude-slider").assertCountEquals(0)
        composeRule.onAllNodesWithTag("observatory-altitude-side-control").assertCountEquals(0)
        composeRule.onAllNodesWithTag("observatory-focus-slider").assertCountEquals(0)
        composeRule.onAllNodesWithTag("observatory-focus-wheels").assertCountEquals(0)
        composeRule.onAllNodesWithTag("observatory-clear-cloud").assertCountEquals(0)
        composeRule.onAllNodesWithTag("observatory-capture").assertCountEquals(0)
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
        onScrubCloud: (Float) -> Unit = {},
    ) {
        composeRule.setContent {
            DstcgTheme {
                ObservatoryGameScreen(
                    state = stateOverride.copy(screen = screen),
                    onBackToMenu = {},
                    onSelectDifficulty = onSelectDifficulty,
                    onSetDomeProgress = {},
                    onValidateDomeProgress = {},
                    onSetAzimuth = {},
                    onSetAltitude = {},
                    onValidateAlignment = {},
                    onSetFocus = {},
                    onValidateFocus = {},
                    onScrubCloud = onScrubCloud,
                    onCapture = {},
                )
            }
        }
    }

    private fun observatoryPlaying(
        step: ObservatoryStep,
        stepTitle: String,
        stepInstruction: String,
        domeProgress: Float = 1f,
        domeClosed: Boolean = false,
        captureProgress: Float = 0f,
        canClearCloud: Boolean = step == ObservatoryStep.ClearCloud,
        canCapture: Boolean = false,
    ): MiniGamesScreenUiState.ObservatoryPlaying =
        MiniGamesScreenUiState.ObservatoryPlaying(
            difficultyName = "Apprenti",
            rewardLabel = "15min",
            targetIndex = 0,
            targetCount = 1,
            targetCard = observatoryCard(),
            step = step,
            stepTitle = stepTitle,
            stepInstruction = stepInstruction,
            domeProgress = domeProgress,
            azimuth = 0.42f,
            altitude = 0.56f,
            focus = 0.61f,
            captureProgress = captureProgress,
            cloudProgress = if (step == ObservatoryStep.ClearCloud) 1f else 0f,
            targetAzimuth = 0.42f,
            targetAltitude = 0.56f,
            targetFocus = 0.61f,
            tolerance = 0.06f,
            toleranceLabel = "±6%",
            domeReady = true,
            domeClosed = domeClosed,
            alignmentReady = true,
            focusReady = true,
            canClearCloud = canClearCloud,
            canCapture = canCapture,
            feedbackEvent = null,
        )

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
