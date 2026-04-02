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
    if (visibleProgress <= 0f || burstStars.isEmpty()) return

    val radialStars = remember(burstStars) { burstStars.filter { it.motion == BurstParticleMotion.Radial } }
    val fallingStars = remember(burstStars) { burstStars.filter { it.motion == BurstParticleMotion.Falling } }

    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag("pack-opening-burst"),
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = if (originBounds != null) {
                Offset(
                    x = originBounds.leftPx + originBounds.widthPx / 2f,
                    y = originBounds.topPx + originBounds.heightPx / 2f,
                )
            } else {
                Offset(size.width / 2f, size.height / 2f)
            }
            radialStars.forEach { star ->
                val particleProgress = normalizedBurstProgress(visibleProgress, star.delayFraction)
                if (particleProgress <= 0f) return@forEach

                val easedProgress = easeInOutBurst(particleProgress)
                val style = rarityBadgeStyle(star.rarityLabel)
                val horizontalDirection = kotlin.math.cos(star.angle).toFloat()
                val launchArcFactor = 1.72f + kotlin.math.abs(kotlin.math.sin(star.angle).toFloat()) * 0.28f
                val horizontalTravel =
                    size.maxDimension * (0.42f + star.travelFactor * 0.52f) * horizontalDirection
                val launchLift =
                    size.maxDimension * (0.24f + star.travelFactor * 0.2f) * launchArcFactor
                val gravityDrop = size.maxDimension * (0.74f + star.travelFactor * 0.9f)
                val starCenter = Offset(
                    x = center.x + horizontalTravel * easedProgress,
                    y = center.y - launchLift * easedProgress + gravityDrop * easedProgress * easedProgress,
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
                    val starCenter = Offset(
                        x = size.width * (star.xFraction + star.horizontalDrift * easedProgress),
                        y = scalarLerp(startY, endY, easedProgress),
                    )
                    val alpha = ((1f - particleProgress) * 1.14f).coerceIn(0f, 1f) * star.alpha
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
    }
}
