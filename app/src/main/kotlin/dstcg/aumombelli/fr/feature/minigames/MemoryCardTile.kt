package fr.aumombelli.dstcg.feature.minigames

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fr.aumombelli.dstcg.performance.LocalAppPerformanceProfile
import fr.aumombelli.dstcg.ui.component.AstroCardSurfaceMode
import fr.aumombelli.dstcg.ui.component.CardArtBackground
import fr.aumombelli.dstcg.ui.component.CardArtVisibility
import fr.aumombelli.dstcg.ui.component.HolographicFoilOverlay
import fr.aumombelli.dstcg.ui.component.HolographicGlareOverlay
import fr.aumombelli.dstcg.ui.component.HolographicRimLightOverlay
import fr.aumombelli.dstcg.ui.component.TwinklingStarsOverlay
import fr.aumombelli.dstcg.ui.motion.autoplayHolographicMotion
import fr.aumombelli.dstcg.ui.theme.skyQualityPalette
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
internal fun MemoryAnimatedCardTile(
    cell: MemoryCellUi,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(10.dp)
    val borderColor by animateColorAsState(
        targetValue = when (cell.state) {
            MemoryCellState.Hidden -> Color(0x6688A8C6)
            MemoryCellState.Revealed -> Color(0xFFB8D9FF)
            MemoryCellState.Matched -> Color(0xFF75E0C2)
            MemoryCellState.Mismatch -> Color(0xFFE67C73)
        },
        animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing),
        label = "memory-card-border",
    )
    val revealProgress by animateFloatAsState(
        targetValue = if (cell.isVisible) 1f else 0f,
        animationSpec = tween(durationMillis = 320, easing = FastOutSlowInEasing),
        label = "memory-card-flip",
    )
    val matchedScale by animateFloatAsState(
        targetValue = if (cell.state == MemoryCellState.Matched) 1.035f else 1f,
        animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing),
        label = "memory-card-match-scale",
    )
    val shake = remember { Animatable(0f) }
    LaunchedEffect(cell.state) {
        if (cell.state == MemoryCellState.Mismatch) {
            shake.snapTo(0f)
            shake.animateTo(1f, tween(50, easing = LinearEasing))
            shake.animateTo(-1f, tween(70, easing = LinearEasing))
            shake.animateTo(0.75f, tween(70, easing = LinearEasing))
            shake.animateTo(0f, tween(90, easing = FastOutSlowInEasing))
        } else {
            shake.animateTo(0f, tween(80))
        }
    }
    val shakeOffsetPx = with(LocalDensity.current) { 6.dp.toPx() }
    val contentRotation = revealProgress * 180f
    val showFront = revealProgress >= 0.5f

    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = shape,
        color = Color.Transparent,
        modifier = modifier
            .graphicsLayer {
                translationX = shake.value * shakeOffsetPx
                scaleX = matchedScale
                scaleY = matchedScale
            }
            .border(width = 1.dp, color = borderColor, shape = shape)
            .semantics {
                contentDescription = if (cell.isVisible) {
                    "Carte révélée ${cell.face.displayCard.definition.name}"
                } else {
                    "Carte cachée"
                }
            }
            .testTag(cell.testTag),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(shape)
                .background(Color(0xFF0A1524))
                .graphicsLayer {
                    rotationY = if (showFront) contentRotation - 180f else contentRotation
                    cameraDistance = 18f * density * 72f
                },
        ) {
            if (showFront) {
                MemoryCardFront(
                    cell = cell,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                MemoryCardBack(modifier = Modifier.fillMaxSize())
            }
        }
    }
}

@Composable
private fun MemoryCardFront(
    cell: MemoryCellUi,
    modifier: Modifier = Modifier,
) {
    val displayCard = cell.face.displayCard
    val palette = skyQualityPalette(displayCard.activeVariant.skyQuality)
    val isSpecialHolographic = cell.face.role == MemoryCardRole.HolographicSingleton ||
        displayCard.activeVariant.isHolographic

    Box(modifier = modifier) {
        CardArtBackground(
            definition = displayCard.definition,
            mode = AstroCardSurfaceMode.Thumbnail,
            palette = palette,
            artVisibility = CardArtVisibility.Visible,
            modifier = Modifier.fillMaxSize(),
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colorStops = arrayOf(
                            0.0f to Color.Black.copy(alpha = 0.2f),
                            0.5f to Color.Transparent,
                            1.0f to Color.Black.copy(alpha = 0.56f),
                        ),
                    ),
                ),
        )
        if (isSpecialHolographic) {
            MemoryHolographicTileOverlay(modifier = Modifier.fillMaxSize())
        }
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.58f))
                .padding(horizontal = 5.dp, vertical = 3.dp),
        ) {
            Text(
                text = displayCard.definition.name,
                color = Color.White,
                fontSize = 9.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (cell.face.role == MemoryCardRole.HolographicSingleton) {
            Text(
                text = "H",
                color = Color(0xFF061016),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(5.dp)
                    .size(18.dp)
                    .background(Color(0xFFE6F7FF), RoundedCornerShape(9.dp))
                    .padding(top = 1.dp),
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun MemoryHolographicTileOverlay(
    modifier: Modifier = Modifier,
) {
    val performanceProfile = LocalAppPerformanceProfile.current
    val loopProgress = if (performanceProfile.enableInteractiveHolographicEffects) {
        val transition = rememberInfiniteTransition(label = "memory-holographic-loop")
        val animatedProgress by transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 2600, easing = LinearEasing),
                repeatMode = RepeatMode.Restart,
            ),
            label = "memory-holographic-progress",
        )
        animatedProgress
    } else {
        0.28f
    }
    val motion = autoplayHolographicMotion(
        loopProgress = loopProgress,
        interactiveEffectsEnabled = performanceProfile.enableInteractiveHolographicEffects,
    )
    Box(modifier = modifier) {
        HolographicFoilOverlay(motion = motion, modifier = Modifier.fillMaxSize())
        HolographicGlareOverlay(motion = motion, modifier = Modifier.fillMaxSize())
        HolographicRimLightOverlay(motion = motion, modifier = Modifier.fillMaxSize())
        TwinklingStarsOverlay(
            animated = performanceProfile.enableAnimatedThumbnailTwinkles,
            sparkleBoost = motion.sparkleBoost,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@Composable
private fun MemoryCardBack(
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.background(
            Brush.linearGradient(
                listOf(
                    Color(0xFF10243A),
                    Color(0xFF1E4C5B),
                    Color(0xFF0A1423),
                ),
            ),
        ),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            drawRoundRect(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0x33F6C75D),
                        Color(0x118DEBFF),
                        Color.Transparent,
                    ),
                    center = center,
                    radius = size.minDimension * 0.72f,
                ),
                cornerRadius = CornerRadius(size.minDimension * 0.12f),
            )
            drawCircle(
                color = Color(0x228DEBFF),
                radius = size.minDimension * 0.28f,
                center = center,
                style = Stroke(width = size.minDimension * 0.02f),
            )
            drawPath(
                path = memoryBackStarPath(
                    center = center,
                    points = 6,
                    outerRadius = size.minDimension * 0.17f,
                    innerRadius = size.minDimension * 0.07f,
                ),
                color = Color(0xFFE9F4FF).copy(alpha = 0.82f),
                style = Fill,
            )
            drawPath(
                path = memoryBackStarPath(
                    center = center,
                    points = 6,
                    outerRadius = size.minDimension * 0.17f,
                    innerRadius = size.minDimension * 0.07f,
                ),
                color = Color(0xFFF6C75D).copy(alpha = 0.48f),
                style = Stroke(width = size.minDimension * 0.01f),
            )
            MemoryBackConstellationPoints.zipWithNext().forEach { (start, end) ->
                drawLine(
                    color = Color(0x668DEBFF),
                    start = Offset(size.width * start.x, size.height * start.y),
                    end = Offset(size.width * end.x, size.height * end.y),
                    strokeWidth = size.minDimension * 0.01f,
                )
            }
            MemoryBackConstellationPoints.forEach { point ->
                drawCircle(
                    color = Color.White.copy(alpha = 0.78f),
                    radius = size.minDimension * 0.018f,
                    center = Offset(size.width * point.x, size.height * point.y),
                )
            }
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.12f),
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.22f),
                        ),
                    ),
                ),
        )
    }
}

private data class MemoryBackPoint(
    val x: Float,
    val y: Float,
)

private fun memoryBackStarPath(
    center: Offset,
    points: Int,
    outerRadius: Float,
    innerRadius: Float,
): Path {
    val path = Path()
    val angleStep = PI / points
    repeat(points * 2) { index ->
        val radius = if (index % 2 == 0) outerRadius else innerRadius
        val angle = -PI / 2 + index * angleStep
        val point = Offset(
            x = center.x + cos(angle).toFloat() * radius,
            y = center.y + sin(angle).toFloat() * radius,
        )
        if (index == 0) {
            path.moveTo(point.x, point.y)
        } else {
            path.lineTo(point.x, point.y)
        }
    }
    path.close()
    return path
}

private val MemoryBackConstellationPoints = listOf(
    MemoryBackPoint(0.23f, 0.34f),
    MemoryBackPoint(0.38f, 0.27f),
    MemoryBackPoint(0.56f, 0.39f),
    MemoryBackPoint(0.72f, 0.3f),
    MemoryBackPoint(0.64f, 0.58f),
    MemoryBackPoint(0.42f, 0.66f),
)
