package fr.aumombelli.dstcg.app

import fr.aumombelli.dstcg.feature.minigames.MiniGamesScreenUiState
import fr.aumombelli.dstcg.feature.minigames.ObservatoryStep
import fr.aumombelli.dstcg.model.DisplayCard
import fr.aumombelli.dstcg.model.DisplayCardVariant
import fr.aumombelli.dstcg.testCardDefinition
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MiniGamesSceneNavigationPolicyTest {
    @Test
    fun `native back is blocked only while clearing observatory clouds`() {
        assertTrue(
            shouldBlockMiniGamesNativeBackNavigation(
                observatoryPlaying(step = ObservatoryStep.ClearCloud),
            ),
        )

        assertFalse(
            shouldBlockMiniGamesNativeBackNavigation(
                observatoryPlaying(step = ObservatoryStep.Focus),
            ),
        )
        assertFalse(shouldBlockMiniGamesNativeBackNavigation(MiniGamesScreenUiState.Menu))
    }

    private fun observatoryPlaying(step: ObservatoryStep): MiniGamesScreenUiState.ObservatoryPlaying =
        MiniGamesScreenUiState.ObservatoryPlaying(
            difficultyName = "Apprenti",
            rewardLabel = "15min",
            targetIndex = 0,
            targetCount = 1,
            targetCard = displayCard(),
            step = step,
            stepTitle = step.name,
            stepInstruction = "",
            domeProgress = 1f,
            azimuth = 0.5f,
            altitude = 0.5f,
            focus = 0.5f,
            captureProgress = 0f,
            cloudProgress = if (step == ObservatoryStep.ClearCloud) 1f else 0f,
            targetAzimuth = 0.5f,
            targetAltitude = 0.5f,
            targetFocus = 0.5f,
            tolerance = 0.06f,
            toleranceLabel = "+/-6%",
            domeReady = true,
            domeClosed = false,
            alignmentReady = true,
            focusReady = true,
            canClearCloud = step == ObservatoryStep.ClearCloud,
            canCapture = step == ObservatoryStep.Capture,
            feedbackEvent = null,
        )

    private fun displayCard(): DisplayCard =
        DisplayCard(
            definition = testCardDefinition("ALP-001"),
            extensionName = "Astronomes en herbe",
            activeVariant = DisplayCardVariant(
                skyQuality = "city",
                skyQualityLabel = "Ville",
                finish = "standard",
                finishLabel = "Standard",
                isHolographic = false,
            ),
        )
}
