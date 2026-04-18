package fr.aumombelli.dstcg.ui.component

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import fr.aumombelli.dstcg.ui.motion.HolographicCardMotion
import fr.aumombelli.dstcg.ui.theme.SkyQualityPalette
import fr.aumombelli.dstcg.ui.theme.rarityBadgeStyle
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
internal fun QuantityPill(
    text: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        color = Color.Black.copy(alpha = 0.35f),
        shape = androidx.compose.foundation.shape.CircleShape,
        modifier = modifier,
    ) {
        Text(
            text = text,
            color = Color.White,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
        )
    }
}

@Composable
internal fun HeroAtmosphere(
    palette: SkyQualityPalette,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        palette.glow,
                        Color.Transparent,
                    ),
                    center = Offset(280f, 120f),
                    radius = 520f,
                ),
            ),
    )
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        palette.mist,
                        Color.Transparent,
                    ),
                    center = Offset(120f, 340f),
                    radius = 420f,
                ),
            ),
    )
}

@Composable
internal fun RarityStarBadge(
    rarityLabel: String,
    modifier: Modifier = Modifier,
) {
    val style = rarityBadgeStyle(rarityLabel)

    Canvas(modifier = modifier) {
        val center = Offset(size.width / 2f, size.height / 2f)
        drawCircle(
            color = style.glowColor,
            radius = size.minDimension * 0.44f,
            center = center,
        )
        drawPath(
            path = starPath(
                center = center,
                points = style.branchCount,
                outerRadius = size.minDimension * 0.34f,
                innerRadius = size.minDimension * 0.16f,
            ),
            color = style.color,
            style = Fill,
        )
        drawPath(
            path = starPath(
                center = center,
                points = style.branchCount,
                outerRadius = size.minDimension * 0.34f,
                innerRadius = size.minDimension * 0.16f,
            ),
            color = Color.White.copy(alpha = 0.75f),
            style = Stroke(width = size.minDimension * 0.03f),
        )
    }
}

@Composable
internal fun TwinklingStarsOverlay(
    animated: Boolean = true,
    sparkleBoost: Float = 0f,
    modifier: Modifier = Modifier,
) {
    val progress = if (animated) {
        val transition = rememberInfiniteTransition(label = "twinkle")
        val animatedProgress by transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 3600, easing = LinearEasing),
                repeatMode = RepeatMode.Restart,
            ),
            label = "twinkle-progress",
        )
        animatedProgress
    } else {
        0.27f
    }

    Canvas(modifier = modifier) {
        val clampedBoost = sparkleBoost.coerceIn(0f, 1f)
        TwinkleStars.forEach { star ->
            val twinkle = ((sin((progress + star.phase) * PI * 2).toFloat() + 1f) / 2f)
            val alpha = (
                0.18f +
                    0.82f * twinkle +
                    clampedBoost * (0.12f + twinkle * 0.14f)
                ).coerceIn(0f, 1f)
            val center = Offset(size.width * star.x, size.height * star.y)
            val starRadius = size.minDimension * star.radius * (1f + clampedBoost * 0.24f)
            drawPath(
                path = starPath(
                    center = center,
                    points = 4,
                    outerRadius = starRadius,
                    innerRadius = starRadius * 0.42f,
                ),
                color = Color.White.copy(alpha = alpha),
            )
            drawCircle(
                color = Color(0xFFFFF8D6).copy(alpha = alpha * 0.7f),
                radius = starRadius * 0.12f,
                center = center,
            )
        }

        if (clampedBoost > 0.05f) {
            BoostedTwinkleStars.forEach { star ->
                val twinkle = ((sin((progress * 1.35f + star.phase) * PI * 2).toFloat() + 1f) / 2f)
                val alpha = (clampedBoost * (0.24f + 0.76f * twinkle)).coerceIn(0f, 1f)
                val center = Offset(size.width * star.x, size.height * star.y)
                val starRadius = size.minDimension * star.radius * (1f + clampedBoost * 0.32f)
                drawPath(
                    path = starPath(
                        center = center,
                        points = 4,
                        outerRadius = starRadius,
                        innerRadius = starRadius * 0.4f,
                    ),
                    color = Color(0xFFFFF6DA).copy(alpha = alpha),
                )
                drawCircle(
                    color = Color.White.copy(alpha = alpha * 0.58f),
                    radius = starRadius * 0.1f,
                    center = center,
                )
            }
        }
    }
}

@Composable
internal fun HolographicFoilOverlay(
    motion: HolographicCardMotion,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        val sweepCenter = size.width * motion.sweepFraction.coerceIn(0f, 1f)
        val highlightAlpha = motion.highlightAlpha.coerceIn(0f, 1f)
        val outerStroke = size.minDimension * 0.028f
        val innerInset = outerStroke * 0.62f
        val innerStroke = size.minDimension * 0.011f
        val cornerRadius = size.minDimension * 0.12f
        val iridescentBrush = Brush.linearGradient(
            colors = listOf(
                Color(0xFF2DEBFF).copy(alpha = 0.84f + highlightAlpha * 0.14f),
                Color(0xFFFF3FCB).copy(alpha = 0.82f + highlightAlpha * 0.16f),
                Color(0xFFFFD84D).copy(alpha = 0.88f + highlightAlpha * 0.10f),
                Color(0xFF41A2FF).copy(alpha = 0.84f + highlightAlpha * 0.14f),
            ),
            start = Offset(sweepCenter - size.width * 0.82f, size.height * 0.06f),
            end = Offset(sweepCenter + size.width * 0.42f, size.height * 0.96f),
        )
        drawRoundRect(
            brush = iridescentBrush,
            cornerRadius = CornerRadius(cornerRadius),
            style = Stroke(width = outerStroke),
        )
        drawRoundRect(
            brush = Brush.linearGradient(
                colors = listOf(
                    Color(0xFFA5FCFF).copy(alpha = 0.62f + highlightAlpha * 0.18f),
                    Color(0xFFFFB3EB).copy(alpha = 0.56f + highlightAlpha * 0.20f),
                    Color(0xFFFFF1A0).copy(alpha = 0.66f + highlightAlpha * 0.16f),
                    Color(0xFFABD8FF).copy(alpha = 0.58f + highlightAlpha * 0.18f),
                ),
                start = Offset(sweepCenter - size.width * 0.54f, size.height * 0.02f),
                end = Offset(sweepCenter + size.width * 0.18f, size.height * 0.98f),
            ),
            topLeft = Offset(innerInset, innerInset),
            size = Size(
                width = (size.width - innerInset * 2f).coerceAtLeast(0f),
                height = (size.height - innerInset * 2f).coerceAtLeast(0f),
            ),
            cornerRadius = CornerRadius((cornerRadius - innerInset).coerceAtLeast(0f)),
            style = Stroke(width = innerStroke),
        )
    }
}

@Composable
internal fun HolographicGlareOverlay(
    motion: HolographicCardMotion,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        val sweepCenter = size.width * motion.sweepFraction.coerceIn(0f, 1f)
        val glareAlpha = (0.14f + motion.highlightAlpha * 0.58f).coerceIn(0f, 0.9f)
        val glareInset = size.minDimension * 0.012f
        drawRoundRect(
            brush = Brush.linearGradient(
                colors = listOf(
                    Color.White.copy(alpha = glareAlpha * 0.12f),
                    Color.White.copy(alpha = glareAlpha * 0.48f),
                    Color.White.copy(alpha = glareAlpha),
                    Color.White.copy(alpha = glareAlpha * 0.34f),
                    Color.White.copy(alpha = glareAlpha * 0.1f),
                ),
                start = Offset(sweepCenter - size.width * 0.34f, size.height * 0.02f),
                end = Offset(sweepCenter + size.width * 0.04f, size.height * 0.98f),
            ),
            topLeft = Offset(glareInset, glareInset),
            size = Size(
                width = (size.width - glareInset * 2f).coerceAtLeast(0f),
                height = (size.height - glareInset * 2f).coerceAtLeast(0f),
            ),
            cornerRadius = CornerRadius((size.minDimension * 0.12f - glareInset).coerceAtLeast(0f)),
            style = Stroke(width = size.minDimension * 0.0105f),
        )
    }
}

@Composable
internal fun HolographicRimLightOverlay(
    motion: HolographicCardMotion,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        val edgeAlpha = motion.edgeGlowAlpha.coerceIn(0f, 1f)
        drawRoundRect(
            brush = Brush.linearGradient(
                colors = listOf(
                    Color(0xFF66F2FF).copy(alpha = 0.74f + edgeAlpha * 0.26f),
                    Color(0xFFFF84E0).copy(alpha = 0.72f + edgeAlpha * 0.28f),
                    Color(0xFFFFE15B).copy(alpha = 0.78f + edgeAlpha * 0.22f),
                    Color(0xFF7CC2FF).copy(alpha = 0.72f + edgeAlpha * 0.26f),
                ),
                start = Offset.Zero,
                end = Offset(size.width, size.height),
            ),
            cornerRadius = CornerRadius(size.minDimension * 0.12f),
            style = Stroke(width = size.minDimension * 0.0145f),
        )
    }
}

@Composable
internal fun StampedSealOverlay(
    modifier: Modifier = Modifier,
) {
    AssetSvgImage(
        assetPath = StampedSealAssetPath,
        modifier = modifier
            .graphicsLayer(rotationZ = -45f),
    )
}

private const val StampedSealAssetPath = "branding/22-tampon.svg"

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

private data class TwinkleStar(
    val x: Float,
    val y: Float,
    val radius: Float,
    val phase: Float,
)

private val TwinkleStars = listOf(
    TwinkleStar(0.16f, 0.14f, 0.024f, 0.0f),
    TwinkleStar(0.78f, 0.18f, 0.019f, 0.17f),
    TwinkleStar(0.64f, 0.34f, 0.016f, 0.39f),
    TwinkleStar(0.24f, 0.44f, 0.021f, 0.56f),
    TwinkleStar(0.83f, 0.58f, 0.018f, 0.73f),
    TwinkleStar(0.14f, 0.72f, 0.02f, 0.88f),
    TwinkleStar(0.52f, 0.78f, 0.014f, 0.28f),
    TwinkleStar(0.72f, 0.86f, 0.022f, 0.49f),
)

private val BoostedTwinkleStars = listOf(
    TwinkleStar(0.11f, 0.22f, 0.012f, 0.08f),
    TwinkleStar(0.34f, 0.18f, 0.011f, 0.42f),
    TwinkleStar(0.59f, 0.24f, 0.01f, 0.71f),
    TwinkleStar(0.86f, 0.29f, 0.012f, 0.13f),
    TwinkleStar(0.23f, 0.61f, 0.011f, 0.54f),
    TwinkleStar(0.47f, 0.67f, 0.01f, 0.33f),
    TwinkleStar(0.68f, 0.73f, 0.012f, 0.82f),
    TwinkleStar(0.82f, 0.49f, 0.011f, 0.26f),
)
