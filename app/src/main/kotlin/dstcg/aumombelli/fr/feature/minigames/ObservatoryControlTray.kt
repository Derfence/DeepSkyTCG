package fr.aumombelli.dstcg.feature.minigames

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
internal fun ObservatoryControlTray(
    playing: MiniGamesScreenUiState.ObservatoryPlaying,
    onSetDomeProgress: (Float) -> Unit,
    onValidateDomeProgress: () -> Unit,
    onSetAzimuth: (Float) -> Unit,
    onValidateAlignment: () -> Unit,
    onSetFocus: (Float) -> Unit,
    onValidateFocus: () -> Unit,
    onCapture: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = modifier.testTag("observatory-control-tray"),
    ) {
        when (playing.step) {
            ObservatoryStep.OpenDome -> ObservatoryControlSlider(
                label = "Coupole",
                value = playing.domeProgress,
                ready = playing.domeReady,
                onValueChange = onSetDomeProgress,
                onValueChangeFinished = onValidateDomeProgress,
                testTag = "observatory-dome-slider",
            )

            ObservatoryStep.CloseDome -> ObservatoryControlSlider(
                label = "Fermeture",
                value = playing.domeProgress,
                ready = playing.domeClosed,
                onValueChange = onSetDomeProgress,
                onValueChangeFinished = onValidateDomeProgress,
                testTag = "observatory-close-dome-slider",
            )

            ObservatoryStep.Align -> {
                ObservatoryControlSlider(
                    label = "Azimut",
                    value = playing.azimuth,
                    ready = playing.alignmentReady,
                    onValueChange = onSetAzimuth,
                    onValueChangeFinished = onValidateAlignment,
                    testTag = "observatory-azimuth-slider",
                )
            }

            ObservatoryStep.ClearCloud -> Unit

            ObservatoryStep.Focus -> ObservatoryFocusWheelsControl(
                value = playing.focus,
                ready = playing.focusReady,
                onValueChange = onSetFocus,
                onValueChangeFinished = onValidateFocus,
            )

            ObservatoryStep.Capture -> ObservatoryCaptureMashControl(
                progress = playing.captureProgress,
                enabled = playing.canCapture,
                onCapture = onCapture,
            )
        }
    }
}

@Composable
private fun ObservatoryCaptureMashControl(
    progress: Float,
    enabled: Boolean,
    onCapture: () -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("observatory-capture-mash"),
    ) {
        LinearProgressIndicator(
            progress = { progress.coerceIn(0f, 1f) },
            color = Color(0xFFFFD684),
            trackColor = Color(0x66334455),
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
                .testTag("observatory-capture-progress"),
        )
        Button(
            onClick = onCapture,
            enabled = enabled,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .testTag("observatory-capture"),
        ) {
            Text("Capturer")
        }
    }
}

@Composable
private fun ObservatoryControlSlider(
    label: String,
    value: Float,
    ready: Boolean,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: () -> Unit,
    testTag: String,
) {
    val readyColor = observatoryControlReadyColor(ready)
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("$testTag-row"),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 2.dp),
        ) {
            Text(
                text = label,
                color = readyColor,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
            )
            Canvas(
                modifier = Modifier
                    .size(12.dp)
                    .testTag("$testTag-ready-indicator"),
            ) {
                drawCircle(
                    color = observatoryControlIndicatorColor(ready),
                    radius = size.minDimension / 2f,
                )
            }
        }
        Slider(
            value = value.coerceIn(0f, 1f),
            onValueChange = onValueChange,
            onValueChangeFinished = onValueChangeFinished,
            modifier = Modifier.testTag(testTag),
        )
    }
}

internal fun observatoryControlReadyColor(ready: Boolean): Color =
    if (ready) Color(0xFF88E6D2) else Color.White

internal fun observatoryControlIndicatorColor(ready: Boolean): Color =
    if (ready) Color(0xFF88E6D2) else Color(0xFF9BA9B7)
