package fr.aumombelli.dstcg.feature.minigames

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

private const val ObservatoryFocusCoarseReductionRatio = 1f
internal const val ObservatoryFocusFineReductionRatio = 5f

@Composable
internal fun ObservatoryFocusWheelsControl(
    value: Float,
    ready: Boolean,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(18.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .testTag("observatory-focus-wheels"),
    ) {
        ObservatoryFocusWheel(
            value = value,
            ready = ready,
            rotationDegrees = value * 360f,
            reductionRatio = ObservatoryFocusCoarseReductionRatio,
            toothCount = 18,
            onValueChange = onValueChange,
            onValueChangeFinished = onValueChangeFinished,
            modifier = Modifier
                .size(92.dp)
                .testTag("observatory-focus-coarse-wheel"),
        )
        ObservatoryFocusWheel(
            value = value,
            ready = ready,
            rotationDegrees = value * 360f * ObservatoryFocusFineReductionRatio,
            reductionRatio = ObservatoryFocusFineReductionRatio,
            toothCount = 16,
            onValueChange = onValueChange,
            onValueChangeFinished = onValueChangeFinished,
            modifier = Modifier
                .size(76.dp)
                .testTag("observatory-focus-fine-wheel"),
        )
    }
}

@Composable
private fun ObservatoryFocusWheel(
    value: Float,
    ready: Boolean,
    rotationDegrees: Float,
    reductionRatio: Float,
    toothCount: Int,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val latestValue by rememberUpdatedState(value.coerceIn(0f, 1f))
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.pointerInput(reductionRatio) {
            var previousAngle = 0f
            var gestureValue = latestValue
            detectDragGestures(
                onDragStart = { offset ->
                    previousAngle = observatoryFocusWheelAngleDegrees(
                        offset = offset,
                        center = Offset(size.width / 2f, size.height / 2f),
                    )
                    gestureValue = latestValue
                },
                onDragEnd = onValueChangeFinished,
                onDragCancel = onValueChangeFinished,
                onDrag = { change, _ ->
                    val currentAngle = observatoryFocusWheelAngleDegrees(
                        offset = change.position,
                        center = Offset(size.width / 2f, size.height / 2f),
                    )
                    val deltaDegrees = observatoryFocusWheelDeltaDegrees(
                        previousAngle = previousAngle,
                        currentAngle = currentAngle,
                    )
                    previousAngle = currentAngle
                    gestureValue = observatoryFocusWheelValue(
                        value = gestureValue,
                        deltaDegrees = deltaDegrees,
                        reductionRatio = reductionRatio,
                    )
                    onValueChange(gestureValue)
                    change.consume()
                },
            )
        },
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            drawFocusGear(
                rotationDegrees = rotationDegrees,
                toothCount = toothCount,
                ready = ready,
            )
        }
    }
}

internal fun observatoryFocusWheelValue(
    value: Float,
    deltaDegrees: Float,
    reductionRatio: Float,
): Float = (value + deltaDegrees / 360f / reductionRatio).coerceIn(0f, 1f)

internal fun observatoryFocusWheelDeltaDegrees(
    previousAngle: Float,
    currentAngle: Float,
): Float = ((currentAngle - previousAngle + 540f) % 360f) - 180f

private fun observatoryFocusWheelAngleDegrees(
    offset: Offset,
    center: Offset,
): Float = (atan2(
    y = offset.y - center.y,
    x = offset.x - center.x,
) * 180f / Math.PI).toFloat()

private fun DrawScope.drawFocusGear(
    rotationDegrees: Float,
    toothCount: Int,
    ready: Boolean,
) {
    val radius = min(size.width, size.height) / 2f
    val center = Offset(size.width / 2f, size.height / 2f)
    val accent = observatoryControlIndicatorColor(ready)
    val steel = if (ready) Color(0xFF88E6D2) else Color(0xFFD6E7F7)
    val body = Color(0xFF1A2B3B)
    val toothWidth = radius * 0.18f
    val toothHeight = radius * 0.20f

    withTransform({
        rotate(rotationDegrees, pivot = center)
    }) {
        repeat(toothCount) { index ->
            withTransform({
                rotate(360f / toothCount * index, pivot = center)
            }) {
                drawRoundRect(
                    color = steel.copy(alpha = 0.90f),
                    topLeft = Offset(center.x - toothWidth / 2f, center.y - radius),
                    size = Size(toothWidth, toothHeight),
                    cornerRadius = CornerRadius(toothWidth * 0.28f, toothHeight * 0.28f),
                )
            }
        }
        drawCircle(
            color = body,
            radius = radius * 0.78f,
            center = center,
        )
        drawCircle(
            color = steel.copy(alpha = 0.95f),
            radius = radius * 0.66f,
            center = center,
            style = Stroke(width = radius * 0.10f),
        )
        repeat(6) { index ->
            val angle = (index * 60f).toRadians()
            drawLine(
                color = steel.copy(alpha = 0.60f),
                start = Offset(
                    x = center.x + cos(angle) * radius * 0.24f,
                    y = center.y + sin(angle) * radius * 0.24f,
                ),
                end = Offset(
                    x = center.x + cos(angle) * radius * 0.58f,
                    y = center.y + sin(angle) * radius * 0.58f,
                ),
                strokeWidth = radius * 0.055f,
                cap = StrokeCap.Round,
            )
        }
        drawCircle(
            color = accent.copy(alpha = if (ready) 0.95f else 0.42f),
            radius = radius * 0.20f,
            center = center,
        )
        drawCircle(
            color = Color(0xFF06111A),
            radius = radius * 0.09f,
            center = center,
        )
    }
}
