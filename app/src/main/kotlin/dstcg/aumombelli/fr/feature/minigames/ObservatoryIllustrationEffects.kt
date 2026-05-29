package fr.aumombelli.dstcg.feature.minigames

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke

internal fun DrawScope.drawObservationBeam(
    geometry: ObservatoryGeometry,
    state: ObservatoryIllustrationState,
) {
    if (!shouldDrawObservationBeam(state.step, state.domeProgress)) return
    val activeAlpha = when (state.step) {
        ObservatoryStep.OpenDome -> 0.10f
        ObservatoryStep.Align -> 0.28f
        ObservatoryStep.ClearCloud -> 0.18f
        ObservatoryStep.Focus -> 0.34f
        ObservatoryStep.Capture -> 0.54f
        ObservatoryStep.CloseDome -> 0f
    }
    val readyBoost = if (state.alignmentReady || state.focusReady) 0.12f else 0f
    val beamAlpha = (activeAlpha + readyBoost + state.captureProgress * 0.22f).coerceIn(0f, 0.82f)
    val width = observatoryLerp(geometry.unit * 0.010f, geometry.unit * 0.004f, state.focus.coerceIn(0f, 1f))

    drawLine(
        color = Color(0xFF8BE8E0).copy(alpha = beamAlpha),
        start = geometry.lens,
        end = geometry.target,
        strokeWidth = width,
        cap = StrokeCap.Round,
    )
    drawLine(
        color = Color(0xFFFFD684).copy(alpha = state.captureProgress * 0.42f),
        start = geometry.lens,
        end = geometry.target,
        strokeWidth = width * 0.45f,
        cap = StrokeCap.Round,
    )
}

internal fun shouldDrawObservationBeam(
    step: ObservatoryStep,
    domeProgress: Float,
): Boolean = domeProgress >= 0.32f && step != ObservatoryStep.CloseDome

internal fun DrawScope.drawTargetReticle(
    geometry: ObservatoryGeometry,
    state: ObservatoryIllustrationState,
) {
    if (state.domeProgress < 0.40f || state.step == ObservatoryStep.CloseDome) return
    val focusTightness = if (state.step == ObservatoryStep.OpenDome) 0f else state.focus.coerceIn(0f, 1f)
    val radius = reticleRadius(geometry.unit, focusTightness)
    val targetColor = when {
        state.captureProgress > 0f -> Color(0xFFFFD684)
        state.focusReady -> Color(0xFF88E6D2)
        state.alignmentReady -> Color(0xFF9AEAFF)
        else -> Color(0xCCD6E7F7)
    }
    val alpha = if (state.step == ObservatoryStep.OpenDome) 0.20f else 0.80f

    drawCircle(
        color = targetColor.copy(alpha = 0.12f + state.captureProgress * 0.20f),
        radius = radius * (2.0f + state.captureProgress),
        center = geometry.target,
    )
    drawCircle(
        color = targetColor.copy(alpha = alpha),
        radius = radius,
        center = geometry.target,
        style = Stroke(width = geometry.unit * 0.005f),
    )
    drawLine(
        color = targetColor.copy(alpha = alpha * 0.72f),
        start = Offset(geometry.target.x - radius * 1.45f, geometry.target.y),
        end = Offset(geometry.target.x - radius * 0.42f, geometry.target.y),
        strokeWidth = geometry.unit * 0.004f,
        cap = StrokeCap.Round,
    )
    drawLine(
        color = targetColor.copy(alpha = alpha * 0.72f),
        start = Offset(geometry.target.x + radius * 0.42f, geometry.target.y),
        end = Offset(geometry.target.x + radius * 1.45f, geometry.target.y),
        strokeWidth = geometry.unit * 0.004f,
        cap = StrokeCap.Round,
    )
    drawLine(
        color = targetColor.copy(alpha = alpha * 0.72f),
        start = Offset(geometry.target.x, geometry.target.y - radius * 1.45f),
        end = Offset(geometry.target.x, geometry.target.y - radius * 0.42f),
        strokeWidth = geometry.unit * 0.004f,
        cap = StrokeCap.Round,
    )
    drawLine(
        color = targetColor.copy(alpha = alpha * 0.72f),
        start = Offset(geometry.target.x, geometry.target.y + radius * 0.42f),
        end = Offset(geometry.target.x, geometry.target.y + radius * 1.45f),
        strokeWidth = geometry.unit * 0.004f,
        cap = StrokeCap.Round,
    )
    drawCircle(
        color = Color.White.copy(alpha = 0.60f + state.captureProgress * 0.30f),
        radius = geometry.unit * 0.006f,
        center = geometry.target,
    )
}

internal fun DrawScope.drawObjectiveMarker(
    geometry: ObservatoryGeometry,
    state: ObservatoryIllustrationState,
) {
    if (
        state.domeProgress < 0.40f ||
        state.step == ObservatoryStep.OpenDome ||
        state.step == ObservatoryStep.CloseDome
    ) {
        return
    }
    val focusTarget = if (state.step == ObservatoryStep.Align) 0.5f else state.targetFocus
    val radius = reticleRadius(geometry.unit, focusTarget)
    val color = if (state.alignmentReady && state.focusReady) {
        Color(0xFF88E6D2)
    } else {
        Color(0xFFFFD684)
    }

    drawCircle(
        color = color.copy(alpha = 0.16f),
        radius = radius * 2.25f,
        center = geometry.objective,
    )
    drawCircle(
        color = color.copy(alpha = 0.92f),
        radius = radius,
        center = geometry.objective,
        style = Stroke(width = geometry.unit * 0.006f),
    )
    drawCircle(
        color = color.copy(alpha = 0.42f),
        radius = radius * 0.52f,
        center = geometry.objective,
        style = Stroke(width = geometry.unit * 0.004f),
    )
    drawLine(
        color = color.copy(alpha = 0.80f),
        start = Offset(geometry.objective.x - radius * 1.65f, geometry.objective.y),
        end = Offset(geometry.objective.x + radius * 1.65f, geometry.objective.y),
        strokeWidth = geometry.unit * 0.003f,
        cap = StrokeCap.Round,
    )
    drawLine(
        color = color.copy(alpha = 0.80f),
        start = Offset(geometry.objective.x, geometry.objective.y - radius * 1.65f),
        end = Offset(geometry.objective.x, geometry.objective.y + radius * 1.65f),
        strokeWidth = geometry.unit * 0.003f,
        cap = StrokeCap.Round,
    )
}

private fun reticleRadius(unit: Float, focus: Float): Float =
    observatoryLerp(unit * 0.060f, unit * 0.024f, focus.coerceIn(0f, 1f))
