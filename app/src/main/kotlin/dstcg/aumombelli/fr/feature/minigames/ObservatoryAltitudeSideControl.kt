package fr.aumombelli.dstcg.feature.minigames

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

private const val ObservatoryAltitudeSliderLengthRatio = 0.92f

@Composable
internal fun ObservatoryAltitudeSideControl(
    value: Float,
    ready: Boolean,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color(0xD407111A),
        contentColor = Color.White,
        shadowElevation = 10.dp,
        modifier = modifier
            .width(48.dp)
            .testTag("observatory-altitude-side-control"),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 12.dp),
        ) {
            Text(
                text = "Alt",
                color = observatoryControlReadyColor(ready),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
            )
            BoxWithConstraints(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            ) {
                Slider(
                    value = value.coerceIn(0f, 1f),
                    onValueChange = onValueChange,
                    onValueChangeFinished = onValueChangeFinished,
                    modifier = Modifier
                        .requiredWidth(observatoryAltitudeSliderLength(maxHeight))
                        .rotate(-90f)
                        .testTag("observatory-altitude-slider"),
                )
            }
            Canvas(
                modifier = Modifier
                    .size(12.dp)
                    .testTag("observatory-altitude-slider-ready-indicator"),
            ) {
                drawCircle(
                    color = observatoryControlIndicatorColor(ready),
                    radius = size.minDimension / 2f,
                )
            }
        }
    }
}

internal fun observatoryAltitudeSliderLength(containerHeight: Dp): Dp =
    containerHeight * ObservatoryAltitudeSliderLengthRatio
