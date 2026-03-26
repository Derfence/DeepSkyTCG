package fr.aumombelli.gatcha.ui.motion

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

internal fun quadPath(
    topLeft: Offset,
    topRight: Offset,
    bottomRight: Offset,
    bottomLeft: Offset,
): Path = Path().apply {
    moveTo(topLeft.x, topLeft.y)
    lineTo(topRight.x, topRight.y)
    lineTo(bottomRight.x, bottomRight.y)
    lineTo(bottomLeft.x, bottomLeft.y)
    close()
}

internal fun lerpOffset(
    start: Offset,
    stop: Offset,
    fraction: Float,
): Offset = Offset(
    x = scalarLerp(start.x, stop.x, fraction),
    y = scalarLerp(start.y, stop.y, fraction),
)

internal fun scalarLerp(
    start: Float,
    stop: Float,
    fraction: Float,
): Float = start + (stop - start) * fraction

internal fun normalizedBurstProgress(
    progress: Float,
    delayFraction: Float,
): Float = ((progress - delayFraction) / (1f - delayFraction).coerceAtLeast(0.0001f)).coerceIn(0f, 1f)

internal fun easeInOutBurst(progress: Float): Float {
    val clamped = progress.coerceIn(0f, 1f)
    return clamped * clamped * (3f - 2f * clamped)
}

internal fun starPath(
    center: Offset,
    points: Int,
    outerRadius: Float,
    innerRadius: Float,
): Path {
    val path = Path()
    val angleStep = PI / points

    repeat(points * 2) { index ->
        val radius = if (index % 2 == 0) outerRadius else innerRadius
        val angle = -PI / 2 + angleStep * index
        val x = center.x + radius * cos(angle).toFloat()
        val y = center.y + radius * sin(angle).toFloat()
        if (index == 0) {
            path.moveTo(x, y)
        } else {
            path.lineTo(x, y)
        }
    }
    path.close()
    return path
}
