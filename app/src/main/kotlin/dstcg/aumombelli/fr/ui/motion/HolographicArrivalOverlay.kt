package fr.aumombelli.dstcg.ui.motion

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun HolographicArrivalOverlay(
    progress: Float,
    originBounds: PackRevealBounds? = null,
    centerXFraction: Float = 0.5f,
    centerYFraction: Float = 0.5f,
    modifier: Modifier = Modifier,
) {
    val visibleProgress = progress.coerceIn(0f, 1f)
    if (visibleProgress <= 0f || visibleProgress >= 1f) return

    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag("pack-opening-holo-arrival"),
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val impactProgress = easeInOutBurst((visibleProgress / 0.22f).coerceIn(0f, 1f))
            val explosionProgress = easeInOutBurst(((visibleProgress - 0.04f) / 0.96f).coerceIn(0f, 1f))
            val fade = (1f - visibleProgress).coerceIn(0f, 1f)
            val center = if (originBounds != null) {
                Offset(
                    x = originBounds.leftPx + originBounds.widthPx / 2f,
                    y = originBounds.topPx + originBounds.heightPx / 2f,
                )
            } else {
                Offset(
                    size.width * centerXFraction.coerceIn(0f, 1f),
                    size.height * centerYFraction.coerceIn(0f, 1f),
                )
            }
            val baseGlowRadius = size.minDimension * scalarLerp(0.08f, 0.42f, explosionProgress)

            drawCircle(
                color = Color.White.copy(alpha = fade * 0.12f),
                radius = size.maxDimension * scalarLerp(0.1f, 0.54f, impactProgress),
                center = center,
            )
            drawCircle(
                color = Color(0xFFFFF2C9).copy(alpha = fade * 0.34f),
                radius = baseGlowRadius,
                center = center,
            )
            drawCircle(
                color = Color(0xFF86E8FF).copy(alpha = fade * 0.24f),
                radius = baseGlowRadius * 1.34f,
                center = center,
            )
            drawCircle(
                color = Color(0xFFFFBEEB).copy(alpha = fade * 0.16f),
                radius = size.maxDimension * scalarLerp(0.16f, 0.72f, explosionProgress),
                center = center,
            )

            HolographicArrivalRays.forEach { ray ->
                val reveal = normalizedBurstProgress(visibleProgress, ray.delayFraction)
                if (reveal <= 0f) return@forEach

                val easedReveal = easeInOutBurst(reveal)
                val angle = ray.angleRadians +
                    sin((visibleProgress * PI * 2 * ray.swayTurns) + ray.angleRadians).toFloat() * 0.09f
                val startDistance = size.minDimension * scalarLerp(0.04f, 0.12f, impactProgress)
                val endDistance = size.maxDimension * scalarLerp(0.16f, ray.lengthFactor, easedReveal)
                val start = Offset(
                    x = center.x + cos(angle).toFloat() * startDistance,
                    y = center.y + sin(angle).toFloat() * startDistance * 0.72f,
                )
                val end = Offset(
                    x = center.x + cos(angle).toFloat() * endDistance,
                    y = center.y + sin(angle).toFloat() * endDistance * 0.72f,
                )
                drawLine(
                    color = ray.color.copy(alpha = fade * ray.alpha * (0.36f + 0.64f * easedReveal)),
                    start = start,
                    end = end,
                    strokeWidth = size.minDimension * scalarLerp(ray.widthFactor * 1.2f, ray.widthFactor, easedReveal),
                )
            }

            drawCircle(
                color = Color.White.copy(alpha = fade * 0.62f),
                radius = size.minDimension * scalarLerp(0.16f, 0.58f, explosionProgress),
                center = center,
                style = Stroke(width = size.minDimension * scalarLerp(0.016f, 0.004f, visibleProgress)),
            )
            drawCircle(
                color = Color(0xFF8DE8FF).copy(alpha = fade * 0.46f),
                radius = size.minDimension * scalarLerp(0.12f, 0.7f, explosionProgress),
                center = center,
                style = Stroke(width = size.minDimension * scalarLerp(0.01f, 0.0032f, visibleProgress)),
            )
            drawCircle(
                color = Color(0xFFFFD6F3).copy(alpha = fade * 0.32f),
                radius = size.minDimension * scalarLerp(0.1f, 0.82f, explosionProgress),
                center = center,
                style = Stroke(width = size.minDimension * scalarLerp(0.007f, 0.0026f, visibleProgress)),
            )

            HolographicArrivalStars.forEach { star ->
                val reveal = normalizedBurstProgress(visibleProgress, star.delayFraction)
                if (reveal <= 0f) return@forEach

                val easedReveal = easeInOutBurst(reveal)
                val angle = star.angleRadians +
                    sin((visibleProgress * PI * 2 * star.spinTurns) + star.angleRadians).toFloat() * 0.1f
                val distance =
                    size.minDimension * scalarLerp(0.08f, star.distanceFactor, easedReveal)
                val lift = size.height * star.liftFactor * explosionProgress
                val starCenter = Offset(
                    x = center.x + cos(angle).toFloat() * distance,
                    y = center.y + sin(angle).toFloat() * distance * 0.72f - lift,
                )
                val trailStart = Offset(
                    x = center.x + cos(angle).toFloat() * distance * 0.28f,
                    y = center.y + sin(angle).toFloat() * distance * 0.18f - lift * 0.18f,
                )
                val alpha = fade * star.alpha * (0.32f + 0.68f * easedReveal)
                drawLine(
                    color = star.color.copy(alpha = alpha * 0.34f),
                    start = trailStart,
                    end = starCenter,
                    strokeWidth = size.minDimension * star.trailWidthFactor,
                )
                drawPath(
                    path = starPath(
                        center = starCenter,
                        points = star.points,
                        outerRadius = size.minDimension *
                            scalarLerp(star.radiusFactor * 0.5f, star.radiusFactor, explosionProgress),
                        innerRadius = size.minDimension *
                            scalarLerp(star.radiusFactor * 0.22f, star.radiusFactor * 0.42f, explosionProgress),
                    ),
                    color = star.color.copy(alpha = alpha),
                    style = Fill,
                )
                drawPath(
                    path = starPath(
                        center = starCenter,
                        points = star.points,
                        outerRadius = size.minDimension *
                            scalarLerp(star.radiusFactor * 0.5f, star.radiusFactor, explosionProgress),
                        innerRadius = size.minDimension *
                            scalarLerp(star.radiusFactor * 0.22f, star.radiusFactor * 0.42f, explosionProgress),
                    ),
                    color = Color.White.copy(alpha = alpha * 0.52f),
                    style = Stroke(width = size.minDimension * 0.0032f),
                )
            }

            HolographicArrivalComets.forEach { comet ->
                val reveal = normalizedBurstProgress(visibleProgress, comet.delayFraction)
                if (reveal <= 0f) return@forEach

                val easedReveal = easeInOutBurst(reveal)
                val angle = comet.angleRadians
                val distance = size.maxDimension * scalarLerp(0.14f, comet.distanceFactor, easedReveal)
                val cometCenter = Offset(
                    x = center.x + cos(angle).toFloat() * distance,
                    y = center.y + sin(angle).toFloat() * distance * 0.7f - size.height * comet.liftFactor * easedReveal,
                )
                val tailLength = size.minDimension * scalarLerp(0.06f, comet.tailFactor, easedReveal)
                val tailStart = Offset(
                    x = cometCenter.x - cos(angle).toFloat() * tailLength,
                    y = cometCenter.y - sin(angle).toFloat() * tailLength * 0.68f,
                )
                val alpha = fade * comet.alpha
                drawLine(
                    color = comet.color.copy(alpha = alpha * 0.42f),
                    start = tailStart,
                    end = cometCenter,
                    strokeWidth = size.minDimension * comet.widthFactor,
                )
                drawCircle(
                    color = comet.color.copy(alpha = alpha),
                    radius = size.minDimension * scalarLerp(comet.radiusFactor * 0.42f, comet.radiusFactor, easedReveal),
                    center = cometCenter,
                )
                drawCircle(
                    color = Color.White.copy(alpha = alpha * 0.44f),
                    radius = size.minDimension * scalarLerp(comet.radiusFactor * 0.18f, comet.radiusFactor * 0.46f, easedReveal),
                    center = cometCenter,
                )
            }
        }
    }
}

@Composable
fun HolographicArrivalCelebrationOverlay(
    progress: Float,
    originBounds: PackRevealBounds? = null,
    modifier: Modifier = Modifier,
) {
    val visibleProgress = progress.coerceIn(0f, 1f)
    if (visibleProgress <= 0f || visibleProgress >= 1f) return

    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag("pack-opening-holo-celebration"),
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = if (originBounds != null) {
                Offset(
                    x = originBounds.leftPx + originBounds.widthPx / 2f,
                    y = originBounds.topPx + originBounds.heightPx / 2f,
                )
            } else {
                Offset(size.width / 2f, size.height * 0.56f)
            }
            val flashProgress = easeInOutBurst((visibleProgress / 0.18f).coerceIn(0f, 1f))
            val expansionProgress = easeInOutBurst(visibleProgress)
            val fade = (1f - visibleProgress).coerceIn(0f, 1f)

            drawRect(
                brush = androidx.compose.ui.graphics.Brush.radialGradient(
                    colors = listOf(
                        Color(0xFFFFF6D8).copy(alpha = fade * 0.16f),
                        Color(0xFF8DEBFF).copy(alpha = fade * 0.1f),
                        Color.Transparent,
                    ),
                    center = center,
                    radius = size.maxDimension * scalarLerp(0.12f, 0.92f, expansionProgress),
                ),
            )

            if (originBounds != null) {
                val pulseInset = size.minDimension * scalarLerp(0.02f, 0.18f, expansionProgress)
                val pulseStroke = size.minDimension * scalarLerp(0.018f, 0.004f, visibleProgress)
                val pulseTopLeft = Offset(
                    x = originBounds.leftPx - pulseInset,
                    y = originBounds.topPx - pulseInset,
                )
                val pulseSize = androidx.compose.ui.geometry.Size(
                    width = originBounds.widthPx + pulseInset * 2f,
                    height = originBounds.heightPx + pulseInset * 2f,
                )
                drawRoundRect(
                    color = Color.White.copy(alpha = fade * 0.62f),
                    topLeft = pulseTopLeft,
                    size = pulseSize,
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(size.minDimension * 0.08f),
                    style = Stroke(width = pulseStroke),
                )
                drawRoundRect(
                    color = Color(0xFFFFD5F3).copy(alpha = fade * 0.4f),
                    topLeft = Offset(
                        x = originBounds.leftPx - pulseInset * 1.7f,
                        y = originBounds.topPx - pulseInset * 1.7f,
                    ),
                    size = androidx.compose.ui.geometry.Size(
                        width = originBounds.widthPx + pulseInset * 3.4f,
                        height = originBounds.heightPx + pulseInset * 3.4f,
                    ),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(size.minDimension * 0.12f),
                    style = Stroke(width = pulseStroke * 0.72f),
                )
            }

            repeat(22) { index ->
                val rayProgress = normalizedBurstProgress(visibleProgress, index / 48f)
                if (rayProgress <= 0f) return@repeat
                val easedRay = easeInOutBurst(rayProgress)
                val angle = (PI * 2 * index / 22.0) + visibleProgress * PI * 0.24
                val start = Offset(
                    x = center.x + cos(angle).toFloat() * size.minDimension * 0.08f,
                    y = center.y + sin(angle).toFloat() * size.minDimension * 0.06f,
                )
                val end = Offset(
                    x = center.x + cos(angle).toFloat() * size.maxDimension * scalarLerp(0.14f, 0.54f, easedRay),
                    y = center.y + sin(angle).toFloat() * size.maxDimension * scalarLerp(0.1f, 0.38f, easedRay),
                )
                drawLine(
                    color = if (index % 2 == 0) {
                        Color(0xFFFFF1B8).copy(alpha = fade * 0.38f * (1f - index / 32f))
                    } else {
                        Color(0xFF90E7FF).copy(alpha = fade * 0.34f * (1f - index / 32f))
                    },
                    start = start,
                    end = end,
                    strokeWidth = size.minDimension * scalarLerp(0.012f, 0.0035f, easedRay),
                )
            }

            repeat(18) { index ->
                val cometProgress = normalizedBurstProgress(visibleProgress, 0.05f + index / 60f)
                if (cometProgress <= 0f) return@repeat
                val easedComet = easeInOutBurst(cometProgress)
                val angle = (PI * 2 * index / 18.0) - PI / 2 + visibleProgress * PI * 0.18
                val distance = size.maxDimension * scalarLerp(0.22f, 0.72f, easedComet)
                val cometCenter = Offset(
                    x = center.x + cos(angle).toFloat() * distance,
                    y = center.y + sin(angle).toFloat() * distance * 0.72f,
                )
                val tail = size.minDimension * scalarLerp(0.06f, 0.18f, easedComet)
                val tailStart = Offset(
                    x = cometCenter.x - cos(angle).toFloat() * tail,
                    y = cometCenter.y - sin(angle).toFloat() * tail * 0.68f,
                )
                val cometColor = if (index % 3 == 0) {
                    Color(0xFFFFD4EE)
                } else if (index % 3 == 1) {
                    Color(0xFF8DE8FF)
                } else {
                    Color(0xFFFFF3C0)
                }
                drawLine(
                    color = cometColor.copy(alpha = fade * 0.44f),
                    start = tailStart,
                    end = cometCenter,
                    strokeWidth = size.minDimension * 0.0062f,
                )
                drawCircle(
                    color = cometColor.copy(alpha = fade * 0.84f),
                    radius = size.minDimension * scalarLerp(0.008f, 0.016f, flashProgress),
                    center = cometCenter,
                )
            }
        }

        HolographicArrivalOverlay(
            progress = visibleProgress,
            originBounds = originBounds,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

private data class HolographicArrivalStar(
    val angleRadians: Double,
    val delayFraction: Float,
    val distanceFactor: Float,
    val liftFactor: Float,
    val radiusFactor: Float,
    val alpha: Float,
    val trailWidthFactor: Float,
    val points: Int,
    val color: Color,
    val spinTurns: Float,
)

private data class HolographicArrivalRay(
    val angleRadians: Double,
    val delayFraction: Float,
    val lengthFactor: Float,
    val widthFactor: Float,
    val alpha: Float,
    val swayTurns: Float,
    val color: Color,
)

private data class HolographicArrivalComet(
    val angleRadians: Double,
    val delayFraction: Float,
    val distanceFactor: Float,
    val tailFactor: Float,
    val radiusFactor: Float,
    val widthFactor: Float,
    val alpha: Float,
    val liftFactor: Float,
    val color: Color,
)

private val HolographicArrivalStars = listOf(
    HolographicArrivalStar(-PI / 2, 0.00f, 0.44f, 0.06f, 0.026f, 0.94f, 0.0062f, 4, Color(0xFFFFF0B8), 1.1f),
    HolographicArrivalStar(-1.18, 0.02f, 0.4f, 0.04f, 0.021f, 0.88f, 0.0054f, 4, Color(0xFF8AEAFF), 1.35f),
    HolographicArrivalStar(-0.82, 0.03f, 0.5f, 0.05f, 0.025f, 0.9f, 0.0060f, 5, Color(0xFFFFBFEF), 1.15f),
    HolographicArrivalStar(-0.42, 0.05f, 0.46f, 0.03f, 0.022f, 0.82f, 0.0052f, 4, Color(0xFFFFF2C7), 1.25f),
    HolographicArrivalStar(-0.04, 0.06f, 0.42f, 0.02f, 0.021f, 0.8f, 0.0050f, 4, Color(0xFF9DD9FF), 1.1f),
    HolographicArrivalStar(0.38, 0.04f, 0.46f, 0.05f, 0.024f, 0.9f, 0.0058f, 5, Color(0xFFFFD5F4), 1.32f),
    HolographicArrivalStar(0.84, 0.08f, 0.42f, 0.04f, 0.02f, 0.76f, 0.0048f, 4, Color(0xFF87EAFF), 1.14f),
    HolographicArrivalStar(1.22, 0.09f, 0.36f, 0.06f, 0.019f, 0.74f, 0.0046f, 4, Color(0xFFFFF1BA), 1.22f),
    HolographicArrivalStar(1.72, 0.1f, 0.44f, 0.04f, 0.022f, 0.82f, 0.0051f, 4, Color(0xFFFFC9EB), 1.28f),
    HolographicArrivalStar(2.22, 0.12f, 0.5f, 0.02f, 0.024f, 0.86f, 0.0056f, 5, Color(0xFF8BD7FF), 1.16f),
    HolographicArrivalStar(2.7, 0.13f, 0.42f, 0.03f, 0.02f, 0.78f, 0.0049f, 4, Color(0xFFFFF0C4), 1.2f),
    HolographicArrivalStar(3.12, 0.14f, 0.36f, 0.02f, 0.018f, 0.72f, 0.0044f, 4, Color(0xFFFFD2F2), 1.08f),
    HolographicArrivalStar(3.52, 0.07f, 0.48f, 0.03f, 0.022f, 0.8f, 0.0051f, 4, Color(0xFF9FE4FF), 1.3f),
    HolographicArrivalStar(4.02, 0.05f, 0.46f, 0.04f, 0.024f, 0.86f, 0.0057f, 5, Color(0xFFFFD6EE), 1.18f),
)

private val HolographicArrivalRays = listOf(
    HolographicArrivalRay(-1.56, 0.00f, 0.34f, 0.008f, 0.72f, 1.1f, Color(0xFFFFF0B8)),
    HolographicArrivalRay(-1.18, 0.02f, 0.42f, 0.007f, 0.66f, 1.25f, Color(0xFF83E7FF)),
    HolographicArrivalRay(-0.78, 0.03f, 0.5f, 0.0086f, 0.74f, 1.18f, Color(0xFFFFC7EC)),
    HolographicArrivalRay(-0.38, 0.05f, 0.44f, 0.0066f, 0.58f, 1.34f, Color(0xFFFFF2C4)),
    HolographicArrivalRay(0.04, 0.06f, 0.38f, 0.0062f, 0.54f, 1.12f, Color(0xFF9EDCFF)),
    HolographicArrivalRay(0.48, 0.08f, 0.46f, 0.0076f, 0.68f, 1.28f, Color(0xFFFFDAF1)),
    HolographicArrivalRay(0.92, 0.09f, 0.42f, 0.0066f, 0.56f, 1.16f, Color(0xFF83EAFF)),
    HolographicArrivalRay(1.38, 0.11f, 0.36f, 0.006f, 0.5f, 1.18f, Color(0xFFFFF3C1)),
    HolographicArrivalRay(1.84, 0.13f, 0.42f, 0.0072f, 0.62f, 1.22f, Color(0xFFFFCAE9)),
    HolographicArrivalRay(2.3, 0.12f, 0.48f, 0.008f, 0.7f, 1.1f, Color(0xFF8BD8FF)),
    HolographicArrivalRay(2.76, 0.09f, 0.42f, 0.0064f, 0.56f, 1.26f, Color(0xFFFFF1B8)),
    HolographicArrivalRay(3.18, 0.07f, 0.34f, 0.0058f, 0.48f, 1.1f, Color(0xFFFFD9F3)),
    HolographicArrivalRay(3.62, 0.04f, 0.4f, 0.0068f, 0.6f, 1.2f, Color(0xFF85E8FF)),
    HolographicArrivalRay(4.02, 0.02f, 0.48f, 0.0082f, 0.72f, 1.16f, Color(0xFFFFC8EB)),
)

private val HolographicArrivalComets = listOf(
    HolographicArrivalComet(-0.96, 0.04f, 0.7f, 0.18f, 0.018f, 0.007f, 0.82f, 0.05f, Color(0xFF89EAFF)),
    HolographicArrivalComet(-0.32, 0.1f, 0.82f, 0.2f, 0.019f, 0.0074f, 0.86f, 0.03f, Color(0xFFFFF2C4)),
    HolographicArrivalComet(0.44, 0.06f, 0.78f, 0.18f, 0.018f, 0.007f, 0.82f, 0.04f, Color(0xFFFFD0F0)),
    HolographicArrivalComet(1.08, 0.14f, 0.68f, 0.16f, 0.016f, 0.0066f, 0.76f, 0.06f, Color(0xFF97D9FF)),
    HolographicArrivalComet(2.18, 0.12f, 0.76f, 0.19f, 0.017f, 0.007f, 0.8f, 0.04f, Color(0xFFFFE4A9)),
    HolographicArrivalComet(3.02, 0.08f, 0.72f, 0.17f, 0.016f, 0.0064f, 0.74f, 0.03f, Color(0xFFFFC9E9)),
)
