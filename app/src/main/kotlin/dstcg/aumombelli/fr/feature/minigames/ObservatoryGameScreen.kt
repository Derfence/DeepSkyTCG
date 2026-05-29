package fr.aumombelli.dstcg.feature.minigames

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
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
    onValidateDomeProgress: () -> Unit,
    onSetAzimuth: (Float) -> Unit,
    onSetAltitude: (Float) -> Unit,
    onValidateAlignment: () -> Unit,
    onSetFocus: (Float) -> Unit,
    onValidateFocus: () -> Unit,
    onScrubCloud: (Float) -> Unit,
    onCapture: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val resultScreen = state.screen as? MiniGamesScreenUiState.ObservatoryResult
    val playingScreen = state.screen as? MiniGamesScreenUiState.ObservatoryPlaying
    val playingCue = playingScreen?.feedbackEvent

    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag("observatory-screen"),
    ) {
        if (playingScreen != null) {
            ObservatoryPlayingScene(
                playing = playingScreen,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            MiniGameSceneBackdrop(
                variant = SkyBackdropVariant.Mountain,
                mountainBlendProgress = 1f,
                sparkleBoost = 0.24f,
                modifier = Modifier.fillMaxSize(),
            )
        }
        MiniGameFeedbackOverlay(
            cue = playingCue ?: resultScreen?.feedbackEvent,
            modifier = Modifier.fillMaxSize(),
        )
        if (playingScreen?.step == ObservatoryStep.ClearCloud) {
            ObservatoryCloudScrubOverlay(
                onScrubCloud = onScrubCloud,
                modifier = Modifier.fillMaxSize(),
            )
        }
        if (playingScreen != null) {
            ObservatoryPlayingForeground(
                playing = playingScreen,
                onSetDomeProgress = onSetDomeProgress,
                onValidateDomeProgress = onValidateDomeProgress,
                onSetAzimuth = onSetAzimuth,
                onSetAltitude = onSetAltitude,
                onValidateAlignment = onValidateAlignment,
                onSetFocus = onSetFocus,
                onValidateFocus = onValidateFocus,
                onCapture = onCapture,
                modifier = Modifier.fillMaxSize(),
            )
        }
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

                is MiniGamesScreenUiState.ObservatoryPlaying -> Unit

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
private fun ObservatoryPlayingScene(
    playing: MiniGamesScreenUiState.ObservatoryPlaying,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag("observatory-playing"),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .testTag("observatory-stage"),
        ) {
            ObservatoryIllustrationScene(
                targetCard = playing.targetCard,
                step = playing.step,
                domeProgress = playing.domeProgress,
                azimuth = playing.azimuth,
                altitude = playing.altitude,
                focus = playing.focus,
                captureProgress = playing.captureProgress,
                cloudProgress = playing.cloudProgress,
                domeReady = playing.domeReady,
                targetAzimuth = playing.targetAzimuth,
                targetAltitude = playing.targetAltitude,
                targetFocus = playing.targetFocus,
                tolerance = playing.tolerance,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Composable
private fun ObservatoryPlayingForeground(
    playing: MiniGamesScreenUiState.ObservatoryPlaying,
    onSetDomeProgress: (Float) -> Unit,
    onValidateDomeProgress: () -> Unit,
    onSetAzimuth: (Float) -> Unit,
    onSetAltitude: (Float) -> Unit,
    onValidateAlignment: () -> Unit,
    onSetFocus: (Float) -> Unit,
    onValidateFocus: () -> Unit,
    onCapture: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .dstcgContentInsetsPadding(includeBottom = true)
            .padding(horizontal = 18.dp, vertical = 18.dp)
            .testTag("observatory-board"),
    ) {
        Column(
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxSize(),
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 58.dp),
            ) {
                ObservatoryHud(
                    playing = playing,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color(0xD407111A),
                contentColor = Color.White,
                shadowElevation = 10.dp,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                ) {
                    ObservatoryStageHeader(
                        title = playing.stepTitle,
                        instruction = playing.stepInstruction,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    ObservatoryControlTray(
                        playing = playing,
                        onSetDomeProgress = onSetDomeProgress,
                        onValidateDomeProgress = onValidateDomeProgress,
                        onSetAzimuth = onSetAzimuth,
                        onValidateAlignment = onValidateAlignment,
                        onSetFocus = onSetFocus,
                        onValidateFocus = onValidateFocus,
                        onCapture = onCapture,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
        if (playing.step == ObservatoryStep.Align) {
            ObservatoryAltitudeSideControl(
                value = playing.altitude,
                ready = playing.alignmentReady,
                onValueChange = onSetAltitude,
                onValueChangeFinished = onValidateAlignment,
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .fillMaxHeight(0.65f)
                    .offset(x = -15.dp, y = -30.dp),
            )
        }
    }
}

@Composable
private fun ObservatoryStageHeader(
    title: String,
    instruction: String,
    modifier: Modifier = Modifier,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
        modifier = modifier,
    ) {
        Text(
            text = title,
            color = Color.White,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
        Text(
            text = instruction,
            color = Color(0xFFD6E7F7),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
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
