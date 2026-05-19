package fr.aumombelli.dstcg.feature.minigames

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import fr.aumombelli.dstcg.model.DisplayCard

@Composable
internal fun ObservatoryIllustrationScene(
    targetCard: DisplayCard,
    step: ObservatoryStep,
    domeProgress: Float,
    azimuth: Float,
    altitude: Float,
    focus: Float,
    captureProgress: Float,
    cloudProgress: Float,
    domeReady: Boolean,
    targetAzimuth: Float,
    targetAltitude: Float,
    targetFocus: Float,
    tolerance: Float,
    modifier: Modifier = Modifier,
) {
    val animatedDomeProgress by animateFloatAsState(
        targetValue = domeProgress.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 360, easing = FastOutSlowInEasing),
        label = "observatory-dome-progress",
    )
    val animatedAzimuth by animateFloatAsState(
        targetValue = azimuth.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing),
        label = "observatory-azimuth",
    )
    val animatedAltitude by animateFloatAsState(
        targetValue = altitude.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing),
        label = "observatory-altitude",
    )
    val visualFocus = focus.coerceIn(0f, 1f)
    val animatedCaptureProgress by animateFloatAsState(
        targetValue = if (step == ObservatoryStep.Capture) captureProgress.coerceIn(0f, 1f) else 0f,
        animationSpec = tween(durationMillis = 320, easing = FastOutSlowInEasing),
        label = "observatory-capture-progress",
    )
    val visualReadiness = observatoryVisualReadiness(
        azimuth = animatedAzimuth,
        altitude = animatedAltitude,
        focus = visualFocus,
        targetAzimuth = targetAzimuth,
        targetAltitude = targetAltitude,
        targetFocus = targetFocus,
        tolerance = tolerance,
    )

    val illustrationState = ObservatoryIllustrationState(
        step = step,
        domeProgress = animatedDomeProgress,
        azimuth = animatedAzimuth,
        altitude = animatedAltitude,
        focus = visualFocus,
        domeReady = domeReady,
        alignmentReady = visualReadiness.alignmentReady,
        focusReady = visualReadiness.focusReady,
        targetAzimuth = targetAzimuth.coerceIn(0f, 1f),
        targetAltitude = targetAltitude.coerceIn(0f, 1f),
        targetFocus = targetFocus.coerceIn(0f, 1f),
        cloudAlpha = cloudProgress.coerceIn(0f, 1f),
        captureProgress = animatedCaptureProgress,
    )

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF04080F))
            .testTag("observatory-illustration-stage"),
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawObservatoryIllustration(state = illustrationState)
        }
        ObservatoryCaptureCardOverlay(
            targetCard = targetCard,
            state = illustrationState,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

internal data class ObservatoryIllustrationState(
    val step: ObservatoryStep,
    val domeProgress: Float,
    val azimuth: Float,
    val altitude: Float,
    val focus: Float,
    val domeReady: Boolean,
    val alignmentReady: Boolean,
    val focusReady: Boolean,
    val targetAzimuth: Float,
    val targetAltitude: Float,
    val targetFocus: Float,
    val cloudAlpha: Float,
    val captureProgress: Float,
)
