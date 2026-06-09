package fr.aumombelli.dstcg.ui.motion

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import fr.aumombelli.dstcg.ui.theme.rarityBadgeStyle

@Composable
fun RarityBurstOverlay(
    rarityLabel: String?,
    hasHolographicBurst: Boolean,
    progress: Float,
    originBounds: PackRevealBounds? = null,
    modifier: Modifier = Modifier,
) {
    val burstStars = remember(rarityLabel, hasHolographicBurst) {
        buildBurstParticleSpecs(
            highestRarityLabel = rarityLabel,
            hasHolographicBurst = hasHolographicBurst,
        )
    }
    val visibleProgress = progress.coerceIn(0f, 1f)
    if (visibleProgress <= 0f || visibleProgress >= 1f || burstStars.isEmpty()) return

    val radialStars = remember(burstStars) { burstStars.filter { it.motion == BurstParticleMotion.Radial } }
    val fallingStars = remember(burstStars) { burstStars.filter { it.motion == BurstParticleMotion.Falling } }
    val orbitalStars = remember(burstStars) { burstStars.filter { it.motion == BurstParticleMotion.Orbital } }

    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag("pack-opening-burst"),
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val burstOrigin = packOpeningBurstOrigin(
                originBounds = originBounds,
                canvasWidth = size.width,
                canvasHeight = size.height,
                hasHolographicBurst = hasHolographicBurst,
            )
            val center = Offset(burstOrigin.x, burstOrigin.y)
            radialStars.forEach { star ->
                val particleProgress = normalizedBurstProgress(visibleProgress, star.delayFraction)
                if (particleProgress <= 0f) return@forEach

                val easedProgress = easeInOutBurst(particleProgress)
                val style = rarityBadgeStyle(star.rarityLabel)
                val horizontalDirection = kotlin.math.cos(star.angle).toFloat()
                val launchArcFactor = 1.72f + kotlin.math.abs(kotlin.math.sin(star.angle).toFloat()) * 0.28f
                val horizontalTravel =
                    size.maxDimension * (0.42f + star.travelFactor * 0.52f) * horizontalDirection
                val lateralWobble =
                    size.maxDimension *
                        star.horizontalDrift *
                        kotlin.math.sin(particleProgress * Math.PI * star.spinTurns + star.angle).toFloat()
                val launchLift =
                    size.maxDimension * (0.24f + star.travelFactor * 0.2f) * launchArcFactor
                val gravityDrop = size.maxDimension * (0.74f + star.travelFactor * 0.9f)
                val starCenter = Offset(
                    x = center.x + horizontalTravel * easedProgress + lateralWobble,
                    y = center.y -
                        launchLift * easedProgress +
                        gravityDrop * easedProgress * easedProgress -
                        lateralWobble * 0.08f,
                )
                val alpha = ((1f - particleProgress) * 1.16f).coerceIn(0f, 1f) * star.alpha
                drawPath(
                    path = starPath(
                        center = starCenter,
                        points = style.branchCount,
                        outerRadius = size.minDimension * star.radius,
                        innerRadius = size.minDimension * star.radius * 0.42f,
                    ),
                    color = style.color.copy(alpha = alpha),
                    style = Fill,
                )
                drawPath(
                    path = starPath(
                        center = starCenter,
                        points = style.branchCount,
                        outerRadius = size.minDimension * star.radius,
                        innerRadius = size.minDimension * star.radius * 0.42f,
                    ),
                    color = Color.White.copy(alpha = alpha * 0.68f),
                    style = Stroke(width = size.minDimension * 0.0035f),
                )
            }
        }

        if (fallingStars.isNotEmpty()) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("pack-opening-burst-rain"),
            ) {
                fallingStars.forEach { star ->
                    val particleProgress = normalizedBurstProgress(visibleProgress, star.delayFraction)
                    if (particleProgress <= 0f) return@forEach

                    val easedProgress = easeInOutBurst(particleProgress)
                    val style = rarityBadgeStyle(star.rarityLabel)
                    val startY = size.height * star.startYFraction
                    val endY = size.height * (1f + star.travelFactor)
                    val lateralSway =
                        size.width *
                            0.18f *
                            star.horizontalDrift *
                            kotlin.math.sin(particleProgress * Math.PI * star.spinTurns + star.angle).toFloat()
                    val starCenter = Offset(
                        x = size.width * (star.xFraction + star.horizontalDrift * easedProgress) + lateralSway,
                        y = scalarLerp(startY, endY, easedProgress),
                    )
                    val tailCenter = Offset(
                        x = starCenter.x - lateralSway * 0.72f,
                        y = starCenter.y - size.height * 0.05f,
                    )
                    val alpha = ((1f - particleProgress) * 1.14f).coerceIn(0f, 1f) * star.alpha
                    drawLine(
                        color = style.color.copy(alpha = alpha * 0.28f),
                        start = tailCenter,
                        end = starCenter,
                        strokeWidth = size.minDimension * 0.0048f,
                    )
                    drawPath(
                        path = starPath(
                            center = starCenter,
                            points = style.branchCount,
                            outerRadius = size.minDimension * star.radius,
                            innerRadius = size.minDimension * star.radius * 0.42f,
                        ),
                        color = style.color.copy(alpha = alpha),
                        style = Fill,
                    )
                    drawPath(
                        path = starPath(
                            center = starCenter,
                            points = style.branchCount,
                            outerRadius = size.minDimension * star.radius,
                            innerRadius = size.minDimension * star.radius * 0.42f,
                        ),
                        color = Color.White.copy(alpha = alpha * 0.58f),
                        style = Stroke(width = size.minDimension * 0.003f),
                    )
                }
            }
        }

        if (orbitalStars.isNotEmpty()) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("pack-opening-burst-orbit"),
            ) {
                val burstOrigin = packOpeningBurstOrbitOrigin(
                    originBounds = originBounds,
                    canvasWidth = size.width,
                    canvasHeight = size.height,
                    hasHolographicBurst = hasHolographicBurst,
                )
                val center = Offset(burstOrigin.x, burstOrigin.y)
                orbitalStars.forEach { star ->
                    val particleProgress = normalizedBurstProgress(visibleProgress, star.delayFraction)
                    if (particleProgress <= 0f) return@forEach

                    val easedProgress = easeInOutBurst(particleProgress)
                    val style = rarityBadgeStyle(star.rarityLabel)
                    val orbitRadius =
                        size.minDimension *
                            (0.09f + star.travelFactor * star.horizontalDrift) *
                            (0.42f + easedProgress * 0.82f)
                    val travelAngle = star.angle + easedProgress * Math.PI * 2 * star.spinTurns
                    val verticalLift = size.height * (0.04f + star.travelFactor * 0.16f) * easedProgress
                    val starCenter = Offset(
                        x = center.x + kotlin.math.cos(travelAngle).toFloat() * orbitRadius,
                        y = center.y -
                            verticalLift +
                            kotlin.math.sin(travelAngle).toFloat() * orbitRadius * 0.46f,
                    )
                    val trailProgress = (particleProgress - 0.08f).coerceAtLeast(0f)
                    val trailEased = easeInOutBurst(trailProgress)
                    val trailAngle = star.angle + trailEased * Math.PI * 2 * star.spinTurns
                    val trailRadius =
                        size.minDimension *
                            (0.09f + star.travelFactor * star.horizontalDrift) *
                            (0.42f + trailEased * 0.82f)
                    val trailCenter = Offset(
                        x = center.x + kotlin.math.cos(trailAngle).toFloat() * trailRadius,
                        y = center.y -
                            size.height * (0.04f + star.travelFactor * 0.16f) * trailEased +
                            kotlin.math.sin(trailAngle).toFloat() * trailRadius * 0.46f,
                    )
                    val alpha = ((1f - particleProgress) * 1.12f).coerceIn(0f, 1f) * star.alpha
                    drawLine(
                        color = style.color.copy(alpha = alpha * 0.34f),
                        start = trailCenter,
                        end = starCenter,
                        strokeWidth = size.minDimension * 0.0042f,
                    )
                    drawPath(
                        path = starPath(
                            center = starCenter,
                            points = style.branchCount,
                            outerRadius = size.minDimension * star.radius,
                            innerRadius = size.minDimension * star.radius * 0.42f,
                        ),
                        color = style.color.copy(alpha = alpha),
                        style = Fill,
                    )
                    drawPath(
                        path = starPath(
                            center = starCenter,
                            points = style.branchCount,
                            outerRadius = size.minDimension * star.radius,
                            innerRadius = size.minDimension * star.radius * 0.42f,
                        ),
                        color = Color.White.copy(alpha = alpha * 0.62f),
                        style = Stroke(width = size.minDimension * 0.0032f),
                    )
                }
            }
        }
    }
}
