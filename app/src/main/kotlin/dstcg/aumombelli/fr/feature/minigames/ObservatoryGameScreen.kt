package fr.aumombelli.dstcg.feature.minigames

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import fr.aumombelli.dstcg.model.MiniGameDifficulty
import fr.aumombelli.dstcg.ui.component.AstroCardPreviewSurface
import fr.aumombelli.dstcg.ui.component.AstroCardSurfaceMode
import fr.aumombelli.dstcg.ui.component.SceneNavigationButton
import fr.aumombelli.dstcg.ui.component.SceneNavigationIcon
import fr.aumombelli.dstcg.ui.motion.SkyBackdropVariant
import fr.aumombelli.dstcg.ui.screen.dstcgContentInsetsPadding

@Composable
internal fun ObservatoryGameScreen(
    state: MiniGamesUiState,
    onBackToMenu: () -> Unit,
    onSelectDifficulty: (MiniGameDifficulty) -> Unit,
    onSetDomeProgress: (Float) -> Unit,
    onSetAzimuth: (Float) -> Unit,
    onSetAltitude: (Float) -> Unit,
    onSetFocus: (Float) -> Unit,
    onClearCloud: () -> Unit,
    onCapture: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val resultScreen = state.screen as? MiniGamesScreenUiState.ObservatoryResult
    val playingCue = (state.screen as? MiniGamesScreenUiState.ObservatoryPlaying)?.feedbackEvent

    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag("observatory-screen"),
    ) {
        MiniGameSceneBackdrop(
            variant = SkyBackdropVariant.Mountain,
            mountainBlendProgress = 1f,
            sparkleBoost = if (state.screen is MiniGamesScreenUiState.ObservatoryPlaying) 0.36f else 0.24f,
            modifier = Modifier.fillMaxSize(),
        )
        MiniGameFeedbackOverlay(
            cue = playingCue ?: resultScreen?.feedbackEvent,
            modifier = Modifier.fillMaxSize(),
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .dstcgContentInsetsPadding(includeBottom = true)
                .padding(horizontal = 18.dp, vertical = 18.dp),
        ) {
            SceneNavigationButton(
                icon = SceneNavigationIcon.Back,
                onClick = onBackToMenu,
                contentDescription = "Retour au menu des mini-jeux",
                testTag = "observatory-back",
                modifier = Modifier.align(Alignment.TopStart),
            )

            Text(
                text = "Observatoire",
                color = Color.White,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 4.dp),
            )

            when (val screen = state.screen) {
                MiniGamesScreenUiState.ObservatoryDifficultySelection -> ObservatoryDifficultySelection(
                    choices = state.observatoryDifficultyChoices,
                    isLoading = state.isLoading,
                    onSelectDifficulty = onSelectDifficulty,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .fillMaxWidth(),
                )

                is MiniGamesScreenUiState.ObservatoryPlaying -> ObservatoryPlayingPanel(
                    playing = screen,
                    onSetDomeProgress = onSetDomeProgress,
                    onSetAzimuth = onSetAzimuth,
                    onSetAltitude = onSetAltitude,
                    onSetFocus = onSetFocus,
                    onClearCloud = onClearCloud,
                    onCapture = onCapture,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .fillMaxSize(),
                )

                is MiniGamesScreenUiState.ObservatoryResult -> ObservatoryResultPanel(
                    result = screen,
                    onBackToMenu = onBackToMenu,
                    modifier = Modifier.align(Alignment.Center),
                )

                is MiniGamesScreenUiState.ObservatoryUnavailable -> ObservatoryUnavailablePanel(
                    message = screen.message,
                    onBackToMenu = onBackToMenu,
                    modifier = Modifier.align(Alignment.Center),
                )

                else -> Unit
            }
        }
    }
}

@Composable
private fun ObservatoryDifficultySelection(
    choices: List<ObservatoryDifficultyChoiceUi>,
    isLoading: Boolean,
    onSelectDifficulty: (MiniGameDifficulty) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .testTag("observatory-difficulty-selection"),
    ) {
        Spacer(modifier = Modifier.height(56.dp))
        Text(
            text = "Choisis une session",
            color = Color.White,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        choices.forEach { choice ->
            ObservatoryDifficultyRow(
                choice = choice,
                enabled = choice.enabled && !isLoading,
                onClick = { onSelectDifficulty(choice.difficulty) },
            )
        }
    }
}

@Composable
private fun ObservatoryDifficultyRow(
    choice: ObservatoryDifficultyChoiceUi,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(14.dp),
        color = if (enabled) Color(0xDD10283A) else Color(0x99101A25),
        contentColor = Color.White,
        modifier = Modifier
            .fillMaxWidth()
            .testTag(choice.testTag),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = choice.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "${choice.targetLabel} - ${choice.precisionLabel} - ${choice.rewardLabel}",
                    color = Color(0xFFD6E7F7),
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                text = choice.statusLabel,
                color = if (enabled) Color(0xFF88E6D2) else Color(0xFF9BA9B7),
                style = MaterialTheme.typography.labelLarge,
            )
        }
    }
}

@Composable
private fun ObservatoryPlayingPanel(
    playing: MiniGamesScreenUiState.ObservatoryPlaying,
    onSetDomeProgress: (Float) -> Unit,
    onSetAzimuth: (Float) -> Unit,
    onSetAltitude: (Float) -> Unit,
    onSetFocus: (Float) -> Unit,
    onClearCloud: () -> Unit,
    onCapture: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .fillMaxSize()
            .testTag("observatory-playing"),
    ) {
        Spacer(modifier = Modifier.height(58.dp))
        ObservatoryHud(
            playing = playing,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 10.dp),
        )
        MiniGameBoardSurface(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .testTag("observatory-board"),
            ) {
                AstroCardPreviewSurface(
                    displayCard = playing.targetCard,
                    mode = AstroCardSurfaceMode.Thumbnail,
                    modifier = Modifier
                        .widthIn(max = 150.dp)
                        .testTag("observatory-target-card"),
                )
                Text(
                    text = playing.stepTitle,
                    color = Color.White,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = playing.stepInstruction,
                    color = Color(0xFFD6E7F7),
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
                ObservatoryControlSlider(
                    label = "Coupole",
                    value = playing.domeProgress,
                    targetLabel = "100%",
                    enabled = playing.step == ObservatoryStep.OpenDome,
                    ready = playing.domeReady,
                    onValueChange = onSetDomeProgress,
                    testTag = "observatory-dome-slider",
                )
                ObservatoryControlSlider(
                    label = "Azimut",
                    value = playing.azimuth,
                    targetLabel = playing.targetAzimuthLabel,
                    enabled = playing.step == ObservatoryStep.Align,
                    ready = playing.alignmentReady,
                    onValueChange = onSetAzimuth,
                    testTag = "observatory-azimuth-slider",
                )
                ObservatoryControlSlider(
                    label = "Altitude",
                    value = playing.altitude,
                    targetLabel = playing.targetAltitudeLabel,
                    enabled = playing.step == ObservatoryStep.Align,
                    ready = playing.alignmentReady,
                    onValueChange = onSetAltitude,
                    testTag = "observatory-altitude-slider",
                )
                Button(
                    onClick = onClearCloud,
                    enabled = playing.canClearCloud,
                    modifier = Modifier.testTag("observatory-clear-cloud"),
                ) {
                    Text("Dégager le nuage")
                }
                ObservatoryControlSlider(
                    label = "Netteté",
                    value = playing.focus,
                    targetLabel = playing.targetFocusLabel,
                    enabled = playing.step == ObservatoryStep.Focus,
                    ready = playing.focusReady,
                    onValueChange = onSetFocus,
                    testTag = "observatory-focus-slider",
                )
                Button(
                    onClick = onCapture,
                    enabled = playing.canCapture,
                    modifier = Modifier.testTag("observatory-capture"),
                ) {
                    Text("Capturer")
                }
            }
        }
    }
}

@Composable
private fun ObservatoryHud(
    playing: MiniGamesScreenUiState.ObservatoryPlaying,
    modifier: Modifier = Modifier,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier.testTag("observatory-hud"),
    ) {
        MiniGameHudPill(
            label = "Gain",
            value = playing.rewardLabel,
            tint = Color(0xFFF6C75D),
            modifier = Modifier.weight(1f),
        )
        MiniGameHudPill(
            label = "Cible",
            value = "${playing.targetIndex + 1}/${playing.targetCount}",
            tint = Color(0xFF75E0C2),
            modifier = Modifier.weight(1f),
        )
        MiniGameHudPill(
            label = "Précision",
            value = playing.toleranceLabel,
            tint = Color(0xFF9AEAFF),
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun ObservatoryControlSlider(
    label: String,
    value: Float,
    targetLabel: String,
    enabled: Boolean,
    ready: Boolean,
    onValueChange: (Float) -> Unit,
    testTag: String,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("$testTag-row"),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = label,
                color = Color.White,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = "Repère $targetLabel",
                color = if (ready) Color(0xFF88E6D2) else Color(0xFFD6E7F7),
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1,
            )
        }
        Slider(
            value = value.coerceIn(0f, 1f),
            onValueChange = onValueChange,
            enabled = enabled,
            modifier = Modifier.testTag(testTag),
        )
    }
}

@Composable
private fun ObservatoryResultPanel(
    result: MiniGamesScreenUiState.ObservatoryResult,
    onBackToMenu: () -> Unit,
    modifier: Modifier = Modifier,
) {
    MiniGameBoardSurface(
        modifier = modifier
            .widthIn(max = 460.dp)
            .fillMaxWidth()
            .testTag("observatory-result"),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = "Observation terminée",
                color = Color.White,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
            MiniGameHudPill(
                label = result.difficultyName,
                value = result.rewardLabel,
                tint = Color(0xFFF6C75D),
            )
            if (result.targetCount > 0) {
                val targetSummary = if (result.targetCount == 1) {
                    "1 cible archivée"
                } else {
                    "${result.targetCount} cibles archivées"
                }
                Text(
                    text = targetSummary,
                    color = Color(0xFFD6E7F7),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            result.nextDifficultyName?.let { next ->
                Text(
                    text = "$next débloqué",
                    color = Color(0xFF88E6D2),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Button(
                onClick = onBackToMenu,
                modifier = Modifier.testTag("observatory-result-back"),
            ) {
                Text("Retour au menu")
            }
        }
    }
}

@Composable
private fun ObservatoryUnavailablePanel(
    message: String,
    onBackToMenu: () -> Unit,
    modifier: Modifier = Modifier,
) {
    MiniGameBoardSurface(
        modifier = modifier
            .widthIn(max = 460.dp)
            .fillMaxWidth()
            .testTag("observatory-unavailable"),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = message,
                color = Color.White,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.titleMedium,
            )
            Button(
                onClick = onBackToMenu,
                modifier = Modifier.testTag("observatory-unavailable-back"),
            ) {
                Text("Retour au menu")
            }
        }
    }
}
